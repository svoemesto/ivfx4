# ivfx4 — что это за проект, что он умеет и как

> Детальные отчёты: `01-data-model.md`, `02-video-pipeline.md`, `03-faces-and-api.md`,
> `04-ui-and-ux.md` (этот файл — сводка + карта миграции на стек Karaoke).
> HEAD изучен: `a3003b7 Фильтры` (2022-05-11). Разработка 2021-11-30 → 2022-05-11, 75 коммитов.

---

## 0. Главное в одном абзаце

**ivfx4** — десктопное Windows-приложение на JavaFX для **кинематографического анализа
видеоархива**: оно раскладывает видеофайлы на кадры, автоматически находит в них границы
**планов (шотов)** и **сцен**, детектирует и **распознаёт лица** (OpenFace + SVM), сводит
всё в реляционную БД, даёт оператору интерфейс для ручной правки результатов и позволяет
**собрать экспортную выборку** (все планы с актрисой X по всем сценам) в один видеофайл.
Это инструмент видеопродакшена/архивирования, а не плеер и не видеоредактор.

Разработчик — `svoemesto`, пакет `com.svoemesto.ivfx`. Тот же владелец, что и у Karaoke.

---

## 1. Стек (2022) и чем он отличается от Karaoke

| Слой | ivfx4 (2022) | Karaoke (цель) |
|---|---|---|
| Сборка | Maven, `kotlin-maven-plugin` | Gradle multi-module (`build.gradle.kts`) |
| Kotlin | 1.5.31 | 2.2.20 |
| Spring Boot | 2.5.6 | 3.5.6, JDK 17 |
| UI | **JavaFX 11 + FXML**, desktop-окно | **Vue 3 + Vite + Bootstrap 5**, браузер |
| БД | JPA/Hibernate, H2 / MySQL / Postgres, `ddl-auto=update` | **сырой JDBC** (`KaraokeDbTable`), Postgres 16, нумерованные миграции. JPA **запрещён** (`check-no-jpa-imports.sh`) |
| Сетевой доступ | **HTTP-сервера нет вообще** | REST `/api/*` + SSE + nginx |
| Запуск | `mvn javafx:run`, один .jar | docker-compose: `karaoke-app` + `karaoke-web` + `webvue3` + `postgres` |
| ffmpeg | портативный `ffmpeg.exe` **внутри JAR** (Windows) | системный `ffmpeg` в образе Ubuntu + `melt`/MLT для рендера |
| ML | Python + OpenCV + OpenFace (Caffe/Torch), вызов через `.cmd` | Python/Demucs/Sheetsage + Ollama; процессы через очередь |
| Потоки | `Thread()+Runnable`, пула нет | `KaraokeProcessWorker` — очередь заданий в БД со статусами и прогрессом |
| Линтеры/доки | нет | ktlint, ESLint, KDoc 100 %, `knowledge/` SSoT, CI |

**Объём старого кода:** 133 Kotlin-файла, ~18 900 строк.

| Подсистема | Файлов | Строк |
|---|---:|---:|
| `fxcontrollers` (JavaFX UI) | 11 | 8 056 |
| `controllers` (бизнес-логика) | 18 | 2 723 |
| `threads` (пайплайн + загрузка) | 33 | 2 985 |
| `modelsext` (ext-модели, матрицы) | 16 | 1 879 |
| `models` (JPA-сущности) | 18 | 1 102 |
| `repos` | 18 | 852 |
| `utils` | 7 | 737 |
| `enums` | 6 | 85 |

---

## 2. Архитектура (как было устроено)

```
ProjectFXApp (JavaFX Application)
  └─ Main.kt (companion object)
       └─ AnnotationConfigApplicationContext(SpringConfig)   ← Spring Boot НЕ используется
            ├─ dataSource() / jdbcTemplate()  ← HikariCP/DriverManager
            ├─ *Repo  (Spring Data JPA, @Component @Entity)
            ├─ *Controller (@Controller — просто стереотип, HTTP-нет)
            └─ H2db.kt — служебная БД jdbc:h2:./h2db (список баз данных)
  └─ FX-контроллеры создаются вручную: ProjectEditFXController() …
       └─ Main.<repo>.<метод> — прямые вызовы, ObservableList<Tab>
```

Ключевые архитектурные факты (проверены по коду):

1. **HTTP API нет.** Ни одного `@RestController/@RequestMapping/@GetMapping`; в `pom.xml`
   нет `spring-boot-starter-web`. «Контроллеры» — объекты со статическими методами над
   репозиториями, вызываемые из UI в том же процессе.
2. **`@Transactional` на FX-контроллерах не работает** — они создаются `new`, минуя контейнер,
   Spring-прокси не создаётся. Транзакции работают только там, где бин берётся из контекста.
3. Spring Boot как фреймворк **не используется**: нет `SpringApplication.run`, контекст
   поднимается вручную; `@SpringBootApplication` на `Main` — мёртвая аннотация.
4. **Два независимых соединения с БД**: служебная H2 (`./h2db`) и рабочая (H2/MySQL/Postgres).
   Смена рабочей БД требует перезапуска приложения.
5. **Cdf = Computer-Dependent File.** Во всех `*Cdf` сущностях есть `computerId`;
   `Main.ccid` = hash строки из OSHI SystemInfo. `Project.folder` и `File.path` — не колонки,
   а проекции на `ProjectCdf`/`FileCdf` по `computerId == Main.ccid`. Это костыль «общая БД,
   разные машины», потому что абсолютные пути к рабочим папкам у каждой машины свои.
6. **Потоки — руками.** Ни `ExecutorService`, ни пула: `Thread()+Runnable` + последовательный
   оркестратор `RunListThreads` с `sleep(100)` и `isAlive`.
7. **Отмена операций не работает**: `projectactions` не проверяют `isInterrupted` и не убивают
   дочерние ffmpeg; `RunListThreads` при прерывании не выставляет `flagIsDone` → UI зависает.

---

## 3. Модель данных

18 JPA-сущностей, ID везде `@Id @GeneratedValue(IDENTITY) Long`. Таблицы:
`tbl_projects`, `tbl_projects_cdf`, `tbl_files`, `tbl_files_cdf`, `tbl_files_tracks`,
`tbl_frames`, `tbl_faces`, `tbl_shots`, `tbl_scenes`, `tbl_events`, `tbl_persons`,
`tbl_properties`, `tbl_properties_cdf`, `tbl_shots_tmp_cdf`, `tbl_shots_tmp2_cdf`,
`tbl_filters`, `tbl_filters_groups`, `tbl_filters_conditions`.

```
Project ─┬─> File ─┬─> Frame (frameNumber + 6 флагов is* + 10 метрик simScore*/diff*)
         │         ├─> Shot (firstFrameNumber, lastFrameNumber, nearestIFrame, typePerson)
         │         ├─> Face (bbox, vector[128] как @Lob "a|b|c", person, isExample, isManual)
         │         ├─> Track (дорожки MediaInfo: type/name/use/order)
         │         ├─> Scene (firstFrameNumber..lastFrameNumber)   ← связь с Shot НЕТ в модели
         │         ├─> Event (firstFrameNumber..lastFrameNumber)   ← связь с Shot НЕТ в модели
         │         └─> FileCdf (path per computer)
         ├─> Person (name, nameInRecognizer, uuid, personType, faces, photo-ref)
         ├─> Filter ─> FilterGroup ─> FilterCondition
         ├─> ProjectCdf (folder per computer)
         └─> Property / PropertyCdf  ← EAV без FK: parentClass("Shot"/"Scene"/…) + parentId + key + value
```

Особенности, которые надо знать при переносе:

- **Связи Shot↔Scene↔Event вычисляются диапазонами кадров**, а не FK. «План входит в сцену»
  = `shot.first >= scene.first && shot.last <= scene.last`.
- **Каскады только `CascadeType.REMOVE`**, и только у Project/File/Person/Filter.
  Scene/Event (в `File` таких полей вообще нет) и Property (EAV без FK) удаляются
  нативными `DELETE` в обход Hibernate → при удалении файла остаются сироты.
- **`ShotTypeSize` (ECU/BCU/CU/MCU/MS/MLS/LS/VLS/XLS) объявлен, но не подключён к БД** —
  размер плана так и не стал полем.
- `Scene.parentId` / `Event.parentId` объявлены и никогда не заполняются.
- `tbl_shots_tmp_cdf` / `tbl_shots_tmp2_cdf` — временный scratch для раскладки выборки
  фильтра (per-computer), не доменная модель.
- Вектор лица (128 float) хранится **строкой с разделителем `|`**, не BLOB и не 128 колонок.

### Фильтры

Три уровня: `Filter(isAnd)` → `FilterGroup(isAnd)` → `FilterCondition(objectClass, subjectClass,
objectId, objectName, objectValue, isIncluded)`. Свёртка **в памяти**: `isAnd=true` →
`retainAll` (И), `false` → `addAll` (ИЛИ). Каждый тип условия = отдельный нативный `@Query`,
возвращающий `Set<Long>`; пересечение с областью выборки делается предварительным
заполнением `tbl_shots_tmp_cdf`.

Поддерживаемые `objectClass`: `Person`, `Person Property`, `Shot Property`, `Scene Property`,
`Event Property`. `subjectClass`: `Shot`/`Scene`/`Event` (ветки `File` — пустые заглушки).
Свойства берутся из общей `tbl_properties` (не из PropertyCdf).

---

## 4. Видео-пайплайн (16 «project actions»)

Запуск с одного экрана `project-actions-view`: таблица файлов с 17 колонками-индикаторами
(`PW LL FS FM FF AF CS DF CF CFP RF SCA SLA SLN CC`) + 15 чекбоксов операций + флаг
`RECREATE IF EXISTS` (даёт идемпотентность: операция выполняется, если артефакта нет).

| Операция | Что делает | Инструмент |
|---|---|---|
| `Create preview` | 720×400 h264+aac 48000 2ch, letterbox `scale,pad` | ffmpeg (bramp) |
| `Create lossless` | мастер-копия 1920×1080: `dnxhd -b:v 36M` или rawvideo, звук `pcm_s16le 48k` | ffmpeg |
| `Create frames small/medium/full` | кадры 135×75 / 720×400 / 1920×1080, шаг задаётся fps | ffmpeg, `-vframes N -qscale:v 1` |
| `Analyze frames` | расчёт `simScore*`/`diff*` для соседних кадров | **Sikuli** (`Finder`+`Pattern`, `MinSimilarity=0.0`) на 135×75 |
| `Create shots` | нарезка `tbl_shots` по `frame.isFinalFind` | пороги `diff1=0.4`, `diff2=0.42`; `nearestIFrame` из ffprobe |
| `Detect faces` | детекция лиц на полных кадрах | Python + **Caffe SSD** res10_300x300 + **OpenFace** nn4 |
| `Create faces` | разбор `faces.json` → `tbl_faces` | Kotlin + Gson |
| `Create faces preview` | вырезка 75×75 превью лиц | Java2D `OverlayImage` |
| `Recognize faces` | SVM-классификация векторов | Python: `SVC(C=1.0, kernel="linear", probability=True)` |
| `Train face model` | обучение на `is_example = true` | Python: `LabelEncoder` + SVC → `recognizer.pickle`, `le.pickle` |
| `Create shots …` (3 варианта) | вырезка планов в отдельные файлы | ffmpeg **из lossless**, не из исходника |
| `Create concat` | склейка планов файла | `ffmpeg -f concat -safe 0 -c copy` |
| `Create filter result` | **экспорт выборки фильтра** в один файл `Filters/<filter> [<f1>-<f2>].ext` | `ffmpeg -f concat -c copy` |

**Алгоритмы:**

- **I-кадры** (единственный прямой вызов ffprobe):
  `ffprobe -skip_frame nokey -select_streams v -show_frames <file>`,
  парсинг вывода regex'ом с **захардкоженным `\r\n`** → только Windows.
- **Границы планов** — **не** `ffmpeg scene-detect`, а сравнение соседних кадров:
  `simScoreNext1..3 / simScorePrev1..3` → `diffNext1/2`, `diffPrev1` → порог → `isFinalFind`.
  (Известный дефект: `diffPrev2` никогда не вычисляется — вторая ветка детекта мертва.)
- **Сцены — только ручные**: оператор выделяет непрерывный набор планов в UI →
  `SceneController.getOrCreate(file, first, last)`, пересекающиеся сцены разрезаются.

**Иерархия папок на диске:**

```
<project.folder>/
  Lossless/<short>/<short>_lossless.mkv        ← мастер-копия, источник всех нарезок
  Preview/<short>/<short>_preview.mp4
  Frames_Small/<short>/<short>_frame_%06d.jpg  (135×75)
  Frames_Medium/<short>/…                      (720×400)
  Frames_Full/<short>/… + frames.json + faces.json
  Faces_Full/<short>/<short>_frame_%06d_face_%02d.jpg
  Faces_Preview/<short>/…                      (75×75)
  Shots/<short>/<short>_shot_[h.mm.sss-h.mm.sss]-(f-l).mp4
  Shots_LL_audioYES/<short>/…_audioON.mxf
  Shots_LL_audioNO/<short>/…_audioOFF.mxf
  Concat/<short>.txt + <short>_concat.<ext>   ← без подпапки shortName (дефект)
  Filters/<filterName> [<f1>-<f2>].<ext>
  Persons/ , recognizer.pickle , le.pickle , embeddings.json
```

Каждый путь переопределяем per-computer через `PropertyCdf`.

**Вызов внешних программ — три разных механизма** (все Windows-only):
bramp `FFmpegWrapper 0.6.2` с бинарём `ffmpeg.exe` из ресурсов JAR; сырой `ProcessBuilder`
для `MediaInfo.exe`; и `RunCmd`, который пишет временный `.cmd` и запускает **сам .cmd-файл**
(так вызываются Python-скрипты OpenFace). Код возврата подпроцесса **игнорируется** —
сбой Python не виден в UI.

---

## 5. Лица

- **Детекция**: Caffe SSD `res10_300x300` (opencv dnn), фильтр `confidence > 0.3`,
  отбрасываются лица < 20×20.
- **Эмбеддинг**: OpenFace `openface_nn4.small2.v1.t7` → **128-мерный** вектор.
- **Классификация**: sklearn `SVC(kernel="linear", probability=True)`, метки — `Person.nameInRecognizer`.
- **Порог принятия распознавания зашит в Kotlin: `prob > 0.3`** (в Python аргумент `-c` мёртвый).
- Плюс фильтр «не лицо»: если `max(w,h)/min(w,h) > 4` → `PersonType.NONPERSON`.
- Выбор кадров для детекции адаптивный: шаг 3/5/10/20 в зависимости от длины плана.
- Протокол обмена Kotlin↔Python — плоский JSON `faces.json` через Gson; Python
  **перезаписывает тот же файл**, добавив `personRecognizedName`/`recognizeProbability`.

Дефекты: `recognize_faces.py` грузит детектор и эмбеддер (40 МБ) и **не использует их**;
в `detect_faces_in_folder.py` внутренний цикл затирает внешний счётчик `i`;
`FaceController` ищет лицо по `findById(frameId)` вместо `faceId` (баг-копипаст, 4 места).

---

## 6. Пользовательский интерфейс и сценарий

**Карта окон:**

```
project-edit-view (главное 1502×1100)
 ├─ Open… → project-select-view
 ├─ Database → database-select-view → database-edit-view (id/name/driver/url/user/password)
 ├─ Actions ▸ Project Actions… → project-actions-view (Modality.NONE)
 ├─ Actions ▸ Edit shots… → shots-edit-view (2120×1200), TabPane {Frames, Persons, Scenes, Events}
 │    ├─ ПКМ по кадру → «Edit frame faces» → frame-faces-edit-view → person-select-view → person-edit-view
 │    └─ (сцены/события создаются в тех же вкладках)
 ├─ Actions ▸ Edit filters… → filter-edit-view → filter-condition-create-view → person-select-view
 └─ Actions ▸ Edit persons… → person-edit-view
```

**Сценарий работы:**

1. Создать проект: имя, shortName, папка, разрешение/fps/битрейты, кодеки и контейнеры.
2. Добавить файлы (файл или целой папкой). Автоматически читаются дорожки MediaInfo →
   `tbl_files_tracks`; двойной клик переключает `use`.
3. Открыть **Project Actions** → отметить операции → «Do actions» → ждать (две полосы прогресса:
   фаза ffmpeg и фаза python).
4. **Edit shots** → вкладка Frames: матрица кадров 135×75, страницы по ширине окна.
   - **двойной клик по кадру** = задать/снять границу плана (split/union);
   - кнопка в колонке → тип плана из 6 пиктограмм (Один / Один через плечо / Двое / Трое и более / Очень много);
   - цветовая семантика оверлеев: **красный** = нашёл алгоритм, **зелёный** = сделал оператор,
     **оранжевый** = отменил, **голубой I** = I-кадр, **синий треугольник** = есть лица.
   - `isFinalFind` **разрывает строку** матрицы, а не занимает ячейку; на стыке страниц
     крайний план дублируется.
5. Вкладка Persons: матрица лиц 75×75; привязка лица к персоне — контекстное меню или
   drag&drop миниатюры на строку персоны; Ctrl/Shift — мультивыбор.
6. **Ручное создание лица**: ПКМ по кадру → Edit frame faces → **нарисовать прямоугольник
   мышью** по кадру → выбрать персону → `isManual = true`.
7. Вкладки Scenes/Events: выделить непрерывный набор планов → «Create new scene/event» →
   ввести имя.
8. **Edit filters** → задать 3-уровневое условие → выбрать файлы → `>>` → получить шоты →
   **Create Video File** → `ffmpeg -f concat -c copy` в `Filters/`.
9. **Экспорт без фильтра** — операции `[SLA]/[SLN]/[SCA]/[CC]` в project-actions.

**Ключевая особенность UX**: кнопки «Сохранить» нет **нигде**. Сохранение происходит
по потере фокуса, по смене выделения и при закрытии окна. Универсальный механизм
свойств (key/value + кнопки ⟰⇧⇩⟱➕✖) продублирован в шести контроллерах почти дословно.

---

## 7. Что сделано плохо (долги, найденные при изучении)

Ни одного TODO/FIXME, но:

- **Отмена и прогресс**: нет пула потоков, последовательный оркестратор, отмена не убивает
  ffmpeg, `CreateFilterResult.run()` блокирует FX-тред, прерывание вешает UI.
- **Целостность данных**: сцены/события/свойства не каскадят, при удалении файла остаются сироты;
  `AnalyzeFrames` затирает ручные правки `isManualAdd/isManualCancel`;
  нативные `DELETE` в обход Hibernate.
- **Ошибки**: ни `@ExceptionHandler`, ни `@ControllerAdvice`; только `printStackTrace()`;
  необработанные `.get()/.first()` по репозиториям.
- **Баги**: баг bramp 0.6.2 (`setVideoFilter()` эмитит `-af`, `setAudioFilter()` → `-vf`);
  `diffPrev2` мёртв; `_lossless.mkv` захардкожен вопреки `lossLessContainer`;
  `CreateShotsLosslessWithoutAudio` всё равно мапит аудио; FXML-id не совпадают с
  Kotlin-полями (`pbShotsForScenes1`); «Create Event Based Scene» создаёт событие;
  пустые обработчики «Delete selected scenes» и удаления персоны.
- **Платформа**: Windows-only — `.exe` внутри JAR, `\r\n` в парсере ffprobe, `.cmd`-запуск
  Python, `getResource(...)!!.file.substring(1)` (упадёт при запуске из JAR),
  `com.sun.javafx...TableViewSkin` (internal API, требует Oracle JDK).
- **Приватность**: пароль БД — обычный `TextField`; `H2db.kt` — SQL строковой конкатенацией.
- **Производительность**: `FrameController.createFrames` генерирует 10⁶ строк через 6 `cross JOIN`
  ради `limit N`; `sourceIterable.count()` в цикле во всех `LoadList*`;
  поиск персоны по распознавателю — O(n) в памяти на каждое лицо.
- **История**: фича иерархических Tags (Tag/TagNode/TagChild + UI) была реализована
  в декабре 2021 и **удалена** в январе 2022, заменена системой Filter/Group/Condition.
- Неиспользуемое: `ShotTypeSize` (10 значений), `PersonType.EXTRAS`, `btnTagAdd/Delete`
  (объявлены в Kotlin, отсутствуют в FXML), клавиши Z/X видеоплеера (флаги нигде не читаются).

---

## 8. Карта соответствий old → new (стек Karaoke)

| Было (ivfx4) | Стало (стек Karaoke) | Комментарий |
|---|---|---|
| JavaFX + FXML, 11 окон | Vue 3 SPA в `webvue3` + nginx | Матрица кадров/лиц → компонент на CSS-grid/canvas |
| Нет HTTP | `karaoke-app` REST `/api/*` + DTO + SSE | `controllers/*.kt` становятся `@RestController` + `@Service` |
| JPA/Hibernate, 18 `@Entity` | **сырой JDBC** + `KaraokeDbTable` (ADR-0001, R-07) | Переписывать персистентность полностью; JPA-импорты запрещены guard'ом |
| `ddl-auto=update`, H2 | Postgres 16 + `deploy/karaoke-db/NN_*.sql` | Миграции нумерованные, append-only |
| `Thread()+Runnable` + `sleep(100)` | `KaraokeProcessWorker` + `tbl_processes` | Уже готовая очередь: `CREATING/WAITING/WORKING/DONE/ERROR`, `percentage`, forceStop, лейны приоритета |
| `project-actions` 15 чекбоксов | Экран «Processes» + очередь заданий | Типы операций → `KaraokeProcessTypes` |
| `bramp FFmpegWrapper` + `ffmpeg.exe` в JAR | ffmpeg из образа + `ProcessBuilder` + парсинг stdout | Или `melt`/MLT, как в Karaoke (ADR-0002) — но для анализа ffmpeg уместен |
| Python `.cmd` + OpenFace | Контейнер/сервис, вызов из Kotlin-воркера | Скрипты переносятся как есть, меняется транспорт |
| `Main.ccid` / `*Cdf` (per-machine пути) | Только один хост → **CDF-схему можно убрать** | Либо хранить пути в `Property` без computerId |
| `Property`/`PropertyCdf` (EAV) | `tbl_properties` (KaraokeDbTable) | Механизм универсальных свойств сохраняется |
| `Filter`/`Group`/`Condition` (свёртка в памяти) | То же + SQL Criteria | Логику можно перенести почти 1:1 |
| Секрет БД в `application.properties` | env-переменные (`DB_*`) | Constitution VIII: секреты только через env |
| Прямые `ObservableList` | SSE `recordChange` + Vue-стор | Как в Karaoke: `SseNotificationService`, `tabId` |

**Что стоит взять как есть:** 3-уровневая модель фильтров; матрица кадров с разрывом строки
по границе плана; цветовая семантика оверлеев (красный/зелёный/оранжевый); идея мастер-копии
lossless как единственного источника нарезок; ручная доводка результата алгоритма
(split/union плана, тип плана, рисование лица мышью).

**Что стоит переписать:** персистентность (JPA→JDBC), очередь заданий, отмена операций,
REST-слой, перенос бинарей в образ, кроссплатформенность.

---

## 9. Открытые вопросы к владельцу (для нового проекта)

1. **Сценарий доступа**: приложение остаётся однопользовательским (одна машина, одна БД)
   или становится веб-сервисом с несколькими операторами?
2. **Видеоархив**: файлы остаются локальными на диске с иерархией папок, или уходят в
   объектное хранилище (в Karaoke это MinIO)?
3. **Объём обработки**: один оператор на 1–2 файла, или массовая обработка каталога
   (сотни/тысячи файлов) — от этого зависит, нужна ли очередь с лейнами приоритета.
4. **Каталог лиц**: обучать SVM заново на каждом проекте (как сейчас), или единая
   модель на все проекты + домены/кластеры «неизвестных»?
5. **Экспорт**: насколько нужен конкат выбранных планов в один файл, или достаточно
   экспорта списка/выборки метаданных (CSV/JSON)?
6. **Сцены/события**: оставить только ручную разметку или добавить автоматическую
   (средние I-кадры)?
7. **ShotTypeSize**: размер плана в старом коде не был доведён — вводить в новой модели?
