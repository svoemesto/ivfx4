# ivfx4 — Модель данных и фильтры

Исследование каталогов (прочитано полностью, ~3918 строк):
`models/`, `modelsext/`, `repos/`, `enums/`.
Проект: Kotlin + Spring Boot (Spring Data JPA) + Hibernate + JavaFX (desktop), БД MySQL/PostgreSQL
(`spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.generate-ddl=true` — схема генерится Hibernate'ом,
SQL-скриптов в репозитории нет).

Термины домена (по коду): **Project** = папка исходников, **File** = один видеофайл, **Track** = дорожка
внутри контейнера (видео/аудио/текст), **Frame** = кадр (один JPEG на диске), **Shot** = «план» — диапазон
кадров, **Scene** и **Event** — смысловые группы планов (границы задаются только номерами кадров),
**Face** = обнаруженное лицо в кадре, **Person** = персонаж проекта, **Filter** — сохраняемый запрос
«дай мне планы, где …».

---

## 1. Полная таблица сущностей JPA

Все 18 сущностей объявлены **одинаково**: `@Component @Entity @Table(name = "tbl_…") @Transactional`
(то есть каждая сущность заодно является Spring-бином — характерная черта этого кода).
Все ID — одинаково:

```kotlin
@Id
@Column(name = "id", nullable = false)
@GeneratedValue(strategy = GenerationType.IDENTITY)
var id: Long = 0
```

`GenerationType.IDENTITY` + `Long` — MySQL/Postgres. Исключений нет ни у одной сущности.

### 1.1. Project — `tbl_projects` ([Project.kt](../../src/main/kotlin/com/svoemesto/ivfx/models/Project.kt))

| Поле (Kotlin) | Тип | Колонка | Default |
|---|---|---|---|
| `id` | `Long` | `id` | IDENTITY |
| `order` | `Int` | `order_project` | 0 |
| `name` | `String` | `name` | `""` |
| `shortName` | `String` | `short_name` | `""` |
| `lossLessCodec` | `String` | `lossless_codec` | `LosslessVideoCodecs` default → `"RAW"` |
| `lossLessContainer` | `String` | `lossless_container` | `LosslessContainers` default → `"MKV"` |
| `container` | `String` | `container` | `VideoContainers` default → `"MP4"` |
| `videoCodec` | `String` | `video_codec` | `VideoCodecs` default → `"X264"` |
| `audioCodec` | `String` | `audio_codec` | `AudioCodecs` default → `"AAC"` |
| `width` / `height` | `Int` | `width` / `height` | 1920 / 1080 |
| `fps` | `Double` | `fps` | 23.976 |
| `videoBitrate` | `Int` | `video_bitrate` | 10 000 000 |
| `audioBitrate` | `Int` | `audio_bitrate` | 320 000 |
| `audioFrequency` | `Int` | `audio_frequency` | 48 000 |

Связи (все — «владелец → дети», LAZY + `@Fetch(FetchMode.SUBSELECT)`):

```kotlin
@OneToMany(mappedBy = "project", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
@Fetch(value = FetchMode.SUBSELECT)
var files:   MutableSet<File>       = mutableSetOf()
var persons: MutableSet<Person>     = mutableSetOf()
var filters: MutableSet<Filter>     = mutableSetOf()
var cdfs:    MutableSet<ProjectCdf> = mutableSetOf()
```

Отдельное **не-персистентное** свойство `folder: String` (getter/setter, строки 97–112) — «виртуальное»
поле: читает `cdfs.firstOrNull { it.computerId == Main.ccid }?.folder`, а при записи находит свою
`ProjectCdf` для `Main.ccid` либо создаёт новую и добавляет в `cdfs`. Т.е. физический путь к папке проекта
живёт в `tbl_projects_cdf`, а класс `Project` его скрывает.

### 1.2. ProjectCdf — `tbl_projects_cdf`

| Поле | Тип | Колонка |
|---|---|---|
| `id` | `Long` | `id` (IDENTITY) |
| `project` | `Project` | `project_id` — `@ManyToOne(LAZY) @JoinColumn`, `lateinit` |
| `computerId` | `Int` | `computer_id` default 0 |
| `folder` | `String` | `folder` default `""` |

### 1.3. File — `tbl_files` ([File.kt](../../src/main/kotlin/com/svoemesto/ivfx/models/File.kt))

`class File : Comparable<File>` — `compareTo` = `this.order - other.order`.

| Поле | Тип | Колонка | Default |
|---|---|---|---|
| `id` | `Long` | `id` | IDENTITY |
| `project` | `Project` | `project_id` — `@ManyToOne(LAZY)`, `lateinit` | — |
| `order` | `Int` | `order_file` | 0 |
| `name` | `String` | `name` | `""` |
| `shortName` | `String` | `short_name` | `""` |

Связи: `tracks: MutableSet<Track>`, `frames: MutableSet<Frame>`, `faces: MutableSet<Face>`,
`cdfs: MutableSet<FileCdf>`, `shots: MutableSet<Shot>` — все `@OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = LAZY)` + `@Fetch(SUBSELECT)`.

Виртуальное свойство `path: String` (строки 72–87) — работает ровно как `Project.folder`, но через
`FileCdf.path` и `Main.ccid`.

### 1.4. FileCdf — `tbl_files_cdf`

`id: Long` (IDENTITY), `file: File` (`file_id`, `@ManyToOne(LAZY) lateinit`), `computerId: Int`
(`computer_id`), `path: String` (`path`).

### 1.5. Frame — `tbl_frames`

| Поле | Тип | Колонка | Комментарий |
|---|---|---|---|
| `id` | `Long` | `id` | IDENTITY |
| `file` | `File` | `file_id` | `@ManyToOne(LAZY) lateinit` |
| `frameNumber` | `Int` | `frame_number` | номер кадра в файле |
| `isIFrame` | `Boolean` | `is_iframe` | ключевой кадр |
| `isFind` | `Boolean` | `is_find` | найден детектором сцены |
| `isManualAdd` | `Boolean` | `is_manual_add` | добавлен вручную |
| `isManualCancel` | `Boolean` | `is_manual_cancel` | снят вручную |
| `isFinalFind` | `Boolean` | `is_final_find` | финальная граница |
| `simScoreNext1/2/3`, `simScorePrev1/2/3` | `Double` | `sim_score_next_1..3`, `sim_score_prev_1..3` | «сходство» с соседями (детектор) |
| `diffNext1/2`, `diffPrev1/2` | `Double` | `diff_next_1/2`, `diff_prev_1/2` | разности |

Обратите внимание: у Frame **нет** колонки с путём к картинке — путь вычисляется в
`modelsext/FrameExt.kt` как `"${fileExt.folderFramesSmall}/…/${fileExt.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"`.
Сама картинка лежит на диске (папки `Frames_Small` / `Frames_Medium` / `Frames_Full`), в БД — только
признаки детектора.

Кадры не создаются по одному: `FrameRepo.createFrames(fileId, countFrames)` — нативный `INSERT … SELECT`
с кросс-джойном шести таблиц цифр 0..9, генерирующий номера 1..N одной командой (см. §4).

### 1.6. Shot — `tbl_shots`

`class Shot : Comparable<Shot>` — `compareTo` = `(this.file.order - other.file.order) * 1000000 + (firstFrameNumber - other.firstFrameNumber)`.

| Поле | Тип | Колонка | Default |
|---|---|---|---|
| `id` | `Long` | `id` | IDENTITY |
| `file` | `File` | `file_id` — `@ManyToOne(LAZY) lateinit` | — |
| `typePerson` | `ShotTypePerson` | `shot_type_person` | `NONE` |
| `firstFrameNumber` | `Int` | `first_frame_number` | 0 |
| `lastFrameNumber` | `Int` | `last_frame_number` | 0 |
| `nearestIFrame` | `Int` | `nearest_i_frame` | 0 |

**Классификации размера кадра в БД нет** — `ShotTypeSize` импортирован в `Shot.kt`, но поле не
объявлено (см. §8). Shot↔Person связи в модели **нет** вообще: «персонажи в плане» получаются
join'ом `tbl_faces` по диапазону кадров (см. `PersonRepo.findByShotId`, `FaceRepo.findByShotIdAndPersonId`).

### 1.7. Scene — `tbl_scenes`

`Comparable<Scene>`, `compareTo` = `firstFrameNumber - other.firstFrameNumber`.

`id: Long` (id), `file: File` (`file_id`), `parentId: Long` (`parent_id`, default 0),
`name: String` (`name`), `firstFrameNumber: Int`, `lastFrameNumber: Int`.
Связи «сцены→планы» в модели нет — вычисляется на лету: `ShotRepo.getShotsForScenes(sceneId)` ищет
`tsh.first_frame_number >= tsc.first_frame_number and tsh.last_frame_number <= tsc.last_frame_number`
при равенстве `file_id` (планы должны **целиком** лежать внутри сцены).

### 1.8. Event — `tbl_events`

Полный аналог Scene, ровно те же поля и та же семантика (`parentId`, `name`, `firstFrameNumber`,
`lastFrameNumber`, `file`), таблица `tbl_events`. Различия только в намерении: Scene — «сцена»
(монтажная единица), Event — «событие» (драматургическая единица, обычно создаётся из сцены —
`EventController.createEventExt(sceneExt)` переиспользует `sceneExt.sceneName` как имя события).

### 1.9. Track — `tbl_files_tracks`

`Comparable<Track>` (`order`).

| Поле | Тип | Колонка |
|---|---|---|
| `id` | `Long` | `id` |
| `file` | `File` | `file_id` (`@ManyToOne(LAZY) lateinit`) |
| `order` | `Int` | `order_file_track` |
| `name` | `String` | `name` |
| `type` | `String` | `type` |
| `use` | `Boolean` | `use_track`, default `true` |

### 1.10. Face — `tbl_faces`

`Comparable<Face>` — трёхуровневая: `file.order`, затем `frameNumber`, затем `faceNumberInFrame`.

| Поле | Тип | Колонка | Комментарий |
|---|---|---|---|
| `id` | `Long` | `id` | |
| `file` | `File` | `file_id` | |
| `person` | `Person` | `person_id` | `@ManyToOne(LAZY) lateinit` — **не nullable**, у лица всегда есть «владелец-персонаж» |
| `faceNumberInFrame` | `Int` | `face_number_in_frame` | какое лицо по счёту в кадре |
| `frameNumber` | `Int` | `frame_number` | денормализовано (дублирует связь с Frame, но Frame в БД может отсутствовать) |
| `personRecognizedName` | `String` | `person_recognized_name` | что выдал распознаватель |
| `recognizeProbability` | `Double` | `recognize_probability` | уверенность |
| `startX`, `startY`, `endX`, `endY` | `Int` | `start_x`, `start_y`, `end_x`, `end_y` | bounding box |
| `isExample` | `Boolean` | `is_example` | эталон для обучения |
| `isManual` | `Boolean` | `is_manual` | размечено вручную |
| `vectorText` | `String` | `vector` (`@Lob`) | эмбеддинг, **хранится строкой** |

Вектор-фичер хранится как `@Lob`-строка «a|b|c» и разворачивается виртуальным свойством:

```kotlin
var vector: DoubleArray
    get() = vectorText.split("\\|".toRegex()).map { it.toDouble() }.toDoubleArray()
    set(value) { vectorText = if (vector.isEmpty()) "" else value.joinToString("|") }
```

### 1.11. Person — `tbl_persons`

`Comparable<Person>` — по `name`.

| Поле | Тип | Колонка | Комментарий |
|---|---|---|---|
| `id` | `Long` | `id` | |
| `project` | `Project` | `project_id` (`@ManyToOne(LAZY) lateinit`) | |
| `faces` | `MutableSet<Face>` | — | `@OneToMany(mappedBy = "person", cascade = [CascadeType.REMOVE], fetch = LAZY)` + `@Fetch(SUBSELECT)` |
| `personType` | `PersonType` | `person_type` | default `PERSON` |
| `name` | `String` | `name` | |
| `nameInRecognizer` | `String` | `name_in_recognizer` | имя в распознавателе лиц |
| `fileIdForPreview` | `Long` | `file_id_for_preview` | откуда брать аватар |
| `frameNumberForPreview` | `Int` | `frame_number_for_preview` | |
| `faceNumberForPreview` | `Int` | `face_number_for_preview` | 0 = брать кадр целиком, ≠0 = вырезать лицо |
| `uuid` | `String` | `uuid` | инициализируется `UUID.randomUUID().toString()`; имя файла аватара: `${uuid}.small.jpg` |

Персоны — «глобальные» для проекта (в т.ч. `PersonType.UNDEFINDED` и `NONPERSON` — служебные
«персонажи-заглушки», см. `PersonController.getUndefindedExt/getNonpersonExt` и
`FaceController.createOrUpdate(..., undefindedPerson, nonPerson)`, где лицо с отношением сторон > 4
автоматически приписывается `nonPerson`).

### 1.12. Property — `tbl_properties` (EAV-свойства)

| Поле | Тип | Колонка |
|---|---|---|
| `id` | `Long` | `id` |
| `order` | `Int` | `order_property` |
| `parentClass` | `String` | `parent_class` — **строка**, напр. `"Person"`, `"Shot"`, `"Scene"`, `"Event"`, `"Track"`, `"File"`, `"FilterCondition"` |
| `parentId` | `Long` | `parent_id` — **без FK** |
| `key` | `String` | `property_key` |
| `value` | `String` | `property_value` (`@Lob`) |

Полиморфный EAV без ссылки на таблицу-владельца. Из-за этого удаление свойств не каскадное, а
явное в контроллерах: `PropertyController.deleteAll(entity::class.java.simpleName, entity.id)`.

### 1.13. PropertyCdf — `tbl_properties_cdf`

Отличие от `Property` ровно одно — добавлено поле `computerId: Int` (`computer_id`), и оно участвует
во **всех** finder-методах (`findByParentClassAndParentIdAndComputerIdAnd…`, `getKeys(parentClass, computerId)`).
Именно в `tbl_properties_cdf` хранятся пути к рабочим папкам (см. §2).

### 1.14. ShotTmpCdf — `tbl_shots_tmp_cdf`

`id: Long` (IDENTITY), `computerId: Int` (`computer_id`), `shotId: Long` (`shot_id`) — и всё.
Связи `@ManyToOne` на `Shot` **нет** (только число). Это временная таблица «шоты выбранных для
фильтрации файлов на этой машине».

### 1.15. ShotTmp2Cdf — `tbl_shots_tmp2_cdf`

`id: Long`, `computerId: Int`, `shotId: Long`, `fileId: Long`, `projectId: Long` — тоже без связей.
Временная таблица результата фильтра: в неё `INSERT … SELECT` материализуется набор id'шотов,
после чего `ShotController.convertSetShotsIdsToListShotsExt` читает `findByComputerId` и по
`fileId`/`projectId` собирает `FileExt`/`ProjectExt`.

### 1.16. Filter — `tbl_filters`

`Comparable<Filter>` (`order`): `id`, `project: Project` (`project_id`, `@ManyToOne(LAZY) lateinit`),
`name: String`, `order: Int` (`order_filter`), `isAnd: Boolean` (`is_and`, default `true`),
`filterGroups: MutableSet<FilterGroup>` (`@OneToMany(mappedBy = "filter", cascade = [REMOVE], LAZY)` + `@Fetch(SUBSELECT)`).

### 1.17. FilterGroup — `tbl_filters_groups`

`id`, `filter: Filter` (`filter_id`), `name`, `order` (`order_filter_group`), `isAnd` (`is_and`),
`filterConditions: MutableSet<FilterCondition>` (`@OneToMany(mappedBy = "filterGroup", cascade = [REMOVE], LAZY)`).

### 1.18. FilterCondition — `tbl_filters_conditions`

`id`, `filterGroup: FilterGroup` (`filter_group_id`), `name`, `order` (`order_filter_condition`),
`objectId: Long` (`object_id`), `objectName: String` (`object_name`), `objectValue: String`
(`object_value`), `objectClass: String` (`object_class`), `isIncluded: Boolean` (`is_included`,
default true), `subjectClass: String` (`subject_class`).

### Сводка связей

```
tbl_projects ──1:N──► tbl_files, tbl_persons, tbl_filters, tbl_projects_cdf
tbl_files    ──1:N──► tbl_files_tracks, tbl_frames, tbl_faces, tbl_files_cdf, tbl_shots
tbl_files    ──1:N──► tbl_scenes, tbl_events              (связь есть, mappedBy в модели НЕ объявлена)
tbl_persons  ──1:N──► tbl_faces
tbl_filters  ──1:N──► tbl_filters_groups ──1:N──► tbl_filters_conditions
tbl_shots    ◄──(без FK)── tbl_shots_tmp_cdf.shot_id
tbl_shots    ◄──(без FK)── tbl_shots_tmp2_cdf.shot_id
tbl_properties / tbl_properties_cdf — полиморфны: (parent_class, parent_id) без FK
```

Все `@OneToMany` — с `cascade = [CascadeType.REMOVE]` (только REMOVE; ни PERSIST, ни MERGE нет).
`@ManyToMany`, `@OneToOne`, `@ElementCollection`, `@Inheritance` в проекте **нет нигде** (проверено
grep'ом по `src/main`) — связь Scene↔Shot и Event↔Shot когда-то была `@ManyToMany` через
`@JoinTable` и join-сущности `SceneShot`/`EventShot`, от неё остались только импорты (см. §8).

Все дочерние коллекции помечены `@Fetch(FetchMode.SUBSELECT)` — N+1 на графе проект→файлы→шоты
вызывается массово.

---

## 2. Что такое сущности «*Cdf» — точно по коду

`Cdf` **не** = «copy for display». По коду это — **Computer-dependent (per-machine) данные**.
Доказательства из кода:

1. Во всех `*Cdf`-сущностях есть поле `computerId: Int` (`computer_id`), которого нет у обычных.
2. `Main.ccid = getCurrentComputerId()` ([H2db.kt:153](../../src/main/kotlin/com/svoemesto/ivfx/H2db.kt))
   → `ComputerIdentifier.getComputerId()` = `hashCode()` строки
   `"<manufacturer>#<processorID>#<processorIdentifier>#<logicalProcessorCount>"` из `oshi.SystemInfo`
   ([ComputerIdentifier.kt](../../src/main/kotlin/com/svoemesto/ivfx/utils/ComputerIdentifier.kt)).
3. `Project.folder` / `File.path` — не колонки, а «проекции» на строку `tbl_projects_cdf.folder` /
   `tbl_files_cdf.path` с фильтром `it.computerId == Main.ccid`.

Итого смысл: **проект/файл — общие для всех машин, а абсолютные пути — нет**. Одна и та же папка на
разных ПК лежит по разным путям, поэтому путь хранится в расходной таблице, по строке на
компьютер. Свойства (`PropertyCdf`) — по той же логике: ключи вида `folder_preview` имеют разные
значения на разных машинах, и `PropertyCdfRepo` требует `computerId` в каждом finder'е
(`PropertyCdfController.getOrCreate(parentClass, parentId, Folders.PREVIEW.propertyCdfKey)`).

Отдельно от «машинозависимости» стоят `ShotTmpCdf` и `ShotTmp2Cdf`: они тоже несут `computerId`,
но смысл у них другой — **временная рабочая область (scratch) одного пользователя на одной машине**
для раскладки фильтра. Их наполняют нативными `INSERT … SELECT` и целиком чистят по
`DELETE … WHERE computer_id = ?` (`ShotTmpCdfRepo.deleteAll(computerId)`,
`ShotTmp2CdfRepo.deleteAll(computerId)`), вызываемыми в `FilterEditFXController.doFilter()` и
`ShotController.convertSetShotsIdsToListShotsExt()`.

Итого 6 «Cdf»-типов по назначению:

| Cdf-тип | Назначение | Ключ выбора строки |
|---|---|---|
| `ProjectCdf` | корневая папка проекта на конкретной машине | `project_id` + `computer_id` |
| `FileCdf` | путь к видеофайлу на конкретной машине | `file_id` + `computer_id` |
| `PropertyCdf` | машинозависимые свойства (в т.ч. все рабочие папки) | `parent_class` + `parent_id` + `computer_id` |
| `ShotTmpCdf` | scratch: шоты файлов, отобранных для фильтра | `shot_id` + `computer_id` |
| `ShotTmp2Cdf` | scratch: результат фильтра (shot/file/project id) | `shot_id` + `computer_id` |

---

## 3. Схема связей, каскады, что удаляется каскадно

Объявленные в коде каскады (все — только REMOVE):

| Владелец | Дети | Cascade |
|---|---|---|
| `Project` | `files`, `persons`, `filters`, `cdfs` | `CascadeType.REMOVE` |
| `File` | `tracks`, `frames`, `faces`, `cdfs`, `shots` | `CascadeType.REMOVE` |
| `Person` | `faces` | `CascadeType.REMOVE` |
| `Filter` | `filterGroups` | `CascadeType.REMOVE` |
| `FilterGroup` | `filterConditions` | `CascadeType.REMOVE` |

Каскадно (при `repo.delete(entity)`) удаляется: проект → файлы/персоны/фильтры/`tbl_projects_cdf`;
файл → треки/кадры/лица/`tbl_files_cdf`/шоты; персона → лица; фильтр → группы → условия.

**Чего каскад НЕ покрывает (и это важно):**

1. `Scene` и `Event` в `File` как `@OneToMany` **не объявлены** вообще (в `File.kt` нет полей
   `scenes`/`events`), хотя `file_id` в `tbl_scenes`/`tbl_events` есть. Связь односторонняя,
   JPA-каскада нет; удаление файла оставит сиротские сцены/события — их чистят вручную
   (`SceneRepo.deleteAll(fileId)`, `EventRepo.deleteAll(fileId)` — нативные `DELETE`).
2. `Property`/`PropertyCdf` — без FK, каскад невозможен; удаление вручную
   (`PropertyController.deleteAll(parentClass, parentId)` вызывается в `ShotController.delete`,
   `SceneController.delete`, `TrackController.delete`, `FilterConditionController.delete` и др.).
3. `ShotTmpCdf`/`ShotTmp2Cdf` — без FK на `Shot`; при удалении шотов остаются висящие `shot_id`.

**Отдельно про нативные `@Modifying @Query("DELETE FROM …")`:** они выполняются в обход Hibernate,
поэтому JPA-каскады при них **не работают**. Например `FileRepo.deleteAll(projectId)` удалит строки
`tbl_files`, но не `tbl_frames`/`tbl_shots`/`tbl_faces`/`tbl_files_cdf` — если вызывающий код не
предварительно прошёл по детям (в контроллерах это сделано явно: `ShotController.deleteAll(file)`
сначала чистит свойства каждого шота, затем `shotRepo.deleteAll(file.id)`).

Отдельная мина: у `Face` две ссылки на одну сущность (`file_id` и `person_id`), а `Face` находится
под каскадом и файла, и персоны. Удаление персоны снесёт лица, которые одновременно принадлежат
файлу; удаление файла снесёт лица «чужих» персон.

---

## 4. Система фильтров

### 4.1. Структура: три уровня вложенности

```
Filter (project_id, name, order, isAnd)
  └─ FilterGroup (filter_id, name, order, isAnd)          — «скобки»
       └─ FilterCondition (filter_group_id, name, order,
            objectClass, subjectClass, objectId,
            objectName, objectValue, isIncluded)           — «условие»
```

`isAnd` на каждом уровне выбирает, как комбинировать **своих детей**: `true` → `&&` (пересечение,
реализовано через `retainAll`), `false` → `||` (объединение, `addAll`).

Смысл полей `FilterCondition` (по вызовам в `FilterConditionExt` и `FilterConditionCreateFXController`):

* `objectClass` — **что** проверяем. Возможные значения задаются радиокнопками и строятся из
  `simpleName` + (для свойств) `" Property"`:
  `"Person"`, `"Person Property"`, `"Shot Property"`, `"Scene Property"`, `"Event Property"`.
* `subjectClass` — **где** ищем. Значения: `"Shot"`, `"Scene"`, `"Event"`, `"File"`.
* `objectId` — id персоны (для `objectClass = "Person"`); для `* Property` используется
  `objectName` (= `property_key`) и `objectValue` (= `property_value`), а `objectId` в этом случае
  формально не значим (UI всё равно требует непустой `currentObjectId`, чтобы разрешить «OK»).
* `isIncluded` — `true` → включать найденное, `false` → исключать (инверсия множества).
* `name` — человекочитаемая формула, собирается в UI как
  `"$objectClass «$objectName»${" = «$value»"} is[ NOT]included in $subjectClass"`.

### 4.2. Поддерживаемые пары (что реально реализовано)

В `FilterConditionExt.shotsIds()` реализовано ровно 5 комбинаций `objectClass` × `subjectClass`
(все прочие ветки пустые `{}` / `else -> {}`):

| objectClass | subjectClass | вызываемый метод `ShotRepo` |
|---|---|---|
| `Person` | `Shot` | `getShotsIdsForShotsTmpAndPerson(ccid, objectId)` |
| `Person` | `Scene` | `getShotsIdsForScenesTmpAndPerson(ccid, objectId)` |
| `Person` | `Event` | `getShotsIdsForEventsTmpAndPerson(ccid, objectId)` |
| `Person Property` | `Shot/Scene/Event` | `…ForShotsTmpAndPersonProperty(ccid, objectName, objectValue)` |
| `Shot Property` | `Shot/Scene/Event` | `…ForShotsTmpAndShotProperty(ccid, objectName, objectValue)` |
| `Scene Property` | `Scene` | `getShotsIdsForScenesTmpAndSceneProperty(ccid, …)` |
| `Event Property` | `Event` | `getShotsIdsForEventsTmpAndEventProperty(ccid, …)` |

Отсутствует: `Shot Property` × `File` (в коде пустые ветки `File::class.java.simpleName -> {}`),
`Scene Property` × `Shot/Event`, `Event Property` × `Shot/Scene`, а также `objectClass = "File"`,
`"Shot"` целиком — UI их и не предлагает.

### 4.3. Как выполняется поиск

**Не** Specification и **не** Criteria API. Смешанная схема:

1. **Каждое условие → один нативный `@Query` в `ShotRepo`** (обычно `select distinct tsh.id`),
   возвращает `Set<Long>`.
2. **Комбинация условий/групп/фильтров — в памяти**, в `modelsext`: `FilterConditionExt` →
   `FilterGroupExt` → `FilterExt`. Каждый уровень рекурсивно сводит множества (`retainAll` для AND,
   `addAll` для OR). Есть три параллельных API: `shotsIds(): Set<Long>` (id), `shots(): Set<Shot>`
   (только для `objectClass == "Person"`!) и `shotsExt(setOfShotsExt): Set<ShotExt>`
   (тоже только для `Person`, поверх уже загруженного `ShotExt`-графа).
3. Отдельный scratch-проход: `tbl_shots_tmp_cdf` ограничивает выборку файлами, выбранными в UI
   (`FilterEditFXController.doFilter()`: `ShotTmpCdfController.deleteAll()` затем
   `Main.shotTmpCdfRepo.addAllByFileId(Main.ccid, fileExt.file.id)` для каждого выбранного файла).

Пример конкретного запроса — «шоты выбранных файлов, в которых встречается персонаж №7»
([ShotRepo.kt:48–53](../../src/main/kotlin/com/svoemesto/ivfx/repos/ShotRepo.kt)):

```sql
select distinct tsh.* from tbl_shots as tsh
inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id
inner join tbl_frames as tf on (tsh.file_id = tf.file_id
        and tsh.first_frame_number <= tf.frame_number
        and tsh.last_frame_number  >= tf.frame_number)
inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number)
where tstc.computer_id = ?1 and tfc.person_id = ?2
```

Пример запроса по свойству сцены ([ShotRepo.kt:200–212](../../src/main/kotlin/com/svoemesto/ivfx/repos/ShotRepo.kt)):

```sql
select distinct tsh.id from tbl_shots as tsh
inner join (
  select distinct tsc.* from tbl_scenes as tsc
  inner join tbl_properties as tpsc on (tpsc.parent_class = 'Scene' and tpsc.parent_id = tsc.id)
  inner join (
    select distinct tsh.* from tbl_shots as tsh
    inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id
    where tstc.computer_id = ?1
  ) as sssh on (sssh.file_id = tsc.file_id
        and sssh.first_frame_number >= tsc.first_frame_number
        and sssh.last_frame_number  <= tsc.last_frame_number)
  where tpsc.property_key = ?2 and tpsc.property_value = ?3
) as sssc on (tsh.file_id = sssc.file_id …)
```

То есть «свойство сцены/события/персонажа/шота» — это JOIN на `tbl_properties` по строковым
`'Person'`/`'Shot'`/`'Scene'`/`'Event'` (никогда не по `propertyCdf` — фильтр работает только с
общими, не машинозависимыми свойствами).

Полный путь исполнения (UI → БД → UI):
`doFilter()` → очистка/заполнение `tbl_shots_tmp_cdf` → `FilterController.getFilterExt(projectExt, filterId)`
(подтягивает `filterGroups` через `FilterGroupController.getSetFilterGroups`, там же условия) →
`FilterExt.shotsIds()` → нативные запросы + свёртка в памяти →
`ShotController.convertSetShotsIdsToListShotsExt(ids, projectExt)` (заполняет `tbl_shots_tmp2_cdf`
нативным `INSERT … SELECT`, затем `findByComputerId` и `shotRepo.findByIds`) → `tblShots.items`.

Прочие derived-запросы, на которые опираются контроллеры (примеры):
`FilterRepo.findByProjectIdAndOrderGreaterThanOrderByOrder`, `findByProjectIdAndName`,
`FilterGroupRepo.findByFilterId`, `FilterConditionRepo.findByFilterGroupIdAndOrderLessThanOrderByOrderDesc`
(используются для переупорядочивания), `FilterConditionRepo.getEntityWithGreaterOrder(filterGroupId)`
(для присвоения следующего `order`). Реордер — `ReorderTypes` (`MOVE_UP/DOWN/TO_FIRST/TO_LAST`),
реализован в `FilterConditionController.reOrder` (и аналогично в `TrackController`, `FilterController`,
`FilterGroupController`, `PropertyController`, `PropertyCdfController`) — перенумерация `order`
сдвигами ±1 по соседям.

---

## 5. Что хранится в Track и Event

### Track (`tbl_files_tracks`)

Хранит **описание дорожек медиа-контейнера, полученное из MediaInfo**:

* `name` — при импорте пишется равным `type` (`TrackController.createTracksFromMediaInfo`:
  `trackType = jsonTrack["@type"].toString(); trackType -> track.type и track.name`);
  при ручном создании — `"New file track N to file M"`.
* `type` — строка `@type` из MediaInfo (`"Video"`, `"Audio"`, `"Text"`, …). Своиго enum'а нет.
* `use` / `use_track` — флаг «использовать при финальной сборке», default `true`.
* `order` — порядок в списке.
* **Вся остальная информация дорожки** (битрейт, разрешение, число каналов, язык, SampleRate, …)
  хранится **в `tbl_properties` как EAV** с `parent_class = "Track"`, `parent_id = track.id`:
  `TrackController.createTracksFromMediaInfo` разворачивает JSON MediaInfo в плоский набор
  `PropertyController.editOrCreate("Track", track.id, key, value)` (с рекурсивным спуском во
  вложенные объекты `LinkedTreeMap`, с потерей иерархии — ключи становятся плоскими).
* Файла на диске трек не имеет — работа с ними идёт через ffmpeg по номеру дорожки.

### Event (`tbl_events`)

Event — «событие», смысловая группа планов, **без ссылок на конкретные планы в БД**:

* `file: File` (`file_id`) — файл-владелец;
* `parentId: Long` (`parent_id`, default 0) — заготовка под иерархию событий; **нигде не заполняется**
  (в `controllers` и `fxcontrollers` нет ни одного присваивания `scene.parentId`/`event.parentId`);
* `name: String` — имя события (переименовывается из UI через `ContextMenu` → `TextInputDialog`,
  `EventExt.eventNameLabel`, затем `EventController.save`);
* `firstFrameNumber` / `lastFrameNumber: Int` — границы в кадрах.

Связь «планы внутри события» — **вычисляемая**: `ShotRepo.getShotsForEvents(eventId)` =
`tsh.first_frame_number >= tev.first_frame_number and tsh.last_frame_number <= tev.last_frame_number`
при равенстве `file_id`; на стороне UI — `EventExt.shotsExt` фильтрует `fileExt.shotsExt` по тем же
границам. События могут **пересекаться** (границы задаются независимо), а `Shot` может попасть в
несколько событий; поэтому в `ShotRepo` для событий и сцен используется трёхуровневый подзапрос
(`shots of shots → events of shots → shots of events`), возвращающий также «планы, полностью
накрытые событием, которое само накрыто шотом с нужным признаком» — т.е. считаются и «вложенные»
случаи.

---

## 6. Enum-ы (полный список)

Все enum-ы проекта (10 классов enum в 5 файлах) — в `enums/`. Ниже полные значения.

### `ShotTypePerson` ([ShotTypePerson.kt](../../src/main/kotlin/com/svoemesto/ivfx/enums/ShotTypePerson.kt)) — `enum class ShotTypePerson(order, description, comment, pathToPicture)`; **персистится** как `Shot.typePerson` (`shot_type_person`, int)

| order | name | description | comment (RU) |
|---|---|---|---|
| 0 | `NONE` | `N/A` | Не определено |
| 1 | `SGN` | `Single` | Один |
| 2 | `OTS` | `Over The Shoulder` | Один через плечо |
| 3 | `TWO` | `Two Shot` | Двое |
| 4 | `GRP` | `Group Shot` | Трое и более |
| 5 | `MASS` | `Massive Shot` | Очень много |

`pathToPicture` = `…/shot_type_person_<NAME>.png` (`getResource(...).file.substring(1)`), используется
в `ShotExt.previewType`/`labelType` и `ShotExt.buttonGetType`.

### `ShotTypeSize` ([ShotTypeSize.kt](../../src/main/kotlin/com/svoemesto/ivfx/enums/ShotTypeSize.kt)) — тот же шаблон `(order, description, comment, pathToPicture)`; **НЕ персистится ни в одной таблице**

| order | name | description | comment (RU) |
|---|---|---|---|
| 0 | `NONE` | `N/A` | Не определено |
| 1 | `ECU` | `Extreame Close-Up` *(опечатка в исходнике)* | Деталь |
| 2 | `BCU` | `Big Close-Up` | Крупный |
| 3 | `CU` | `Close-Up` | Крупный (по плечи) |
| 4 | `MCU` | `Medium Close-Up` | Крупный (по грудь) |
| 5 | `MS` | `Medium Shot` | Средний (по пояс) |
| 6 | `MLS` | `Medium Long Shot` | Средний (по колено) |
| 7 | `LS` | `Long Shot` | Общий (видны ноги) |
| 8 | `VLS` | `Very Long Shot` | Общий |
| 9 | `XLS` | `Extreme Long Shot` | Дальний |

Единственное упоминание вне самого файла — **неиспользуемый `import` в `Shot.kt:4`**. Поля
`typeSize`/`shotTypeSize` в модели нет; UI-классификации размера в проекте тоже нет. То есть
функциональность «тип плана по размеру» заготовлена, но не подключена (см. §8).

### `PersonType` ([PersonType.kt](../../src/main/kotlin/com/svoemesto/ivfx/enums/PersonType.kt)) — `enum class PersonType(order, description, comment)`; персистится как `Person.personType` (`person_type`, int)

| order | name | description | comment (RU) | назначение в коде |
|---|---|---|---|---|
| 0 | `PERSON` | `Person` | Персонаж | обычный персонаж (default) |
| 1 | `NONPERSON` | `NonPerson` | Не персонаж | служебный: лица «нелицачные» (аспект > 4) — `FaceController.createOrUpdate` |
| 2 | `EXTRAS` | `Extras` | Массовка | (в коде не используется) |
| 3 | `UNDEFINDED` | `Undefinded` | Неопределенный персонаж | служебный «неопознанный» — `PersonController.getUndefindedExt` |

### `Folders` ([Folders.kt](../../src/main/kotlin/com/svoemesto/ivfx/enums/Folders.kt)) — `enum class Folders(propertyCdfKey, folderName, forProject, forFile)`; не персистится, но задаёт ключи в `tbl_properties_cdf`

| name | `propertyCdfKey` | `folderName` | forProject | forFile |
|---|---|---|---|---|
| `LOSSLESS` | `folder_lossless` | `Lossless` | ✔ | ✔ |
| `PREVIEW` | `folder_preview` | `Preview` | ✔ | ✔ |
| `SHOTS` | `folder_shots` | `Shots` | ✔ | ✔ |
| `FAVORITES` | `folder_favorites` | `Favorites` | ✔ | ✔ |
| `FRAMES_SMALL` | `folder_frames_small` | `Frames_Small` | ✔ | ✔ |
| `FRAMES_MEDIUM` | `folder_frames_medium` | `Frames_Medium` | ✔ | ✔ |
| `FRAMES_FULL` | `folder_frames_full` | `Frames_Full` | ✔ | ✔ |
| `FACES_FULL` | `folder_faces_full` | `Faces_Full` | ✔ | ✔ |
| `FACES_PREVIEW` | `folder_faces_preview` | `Faces_Preview` | ✔ | ✔ |
| `PERSONS` | `folder_persons` | `Persons` | ✔ | ✘ |
| `SHOTS_COMPRESSED_WITH_AUDIO` | `folder_shots_compressed_with_audio` | `Shots` | ✔ | ✔ |
| `SHOTS_LOSSLESS_WITH_AUDIO` | `folder_shots_lossless_with_audio` | `Shots_LL_audioYES` | ✔ | ✔ |
| `SHOTS_LOSSLESS_WITHOUT_AUDIO` | `folder_shots_lossless_without_audio` | `Shots_LL_audioNO` | ✔ | ✔ |
| `CONCAT` | `folder_concat` | `Concat` | ✔ | ✔ |
| `FILTERS` | `folder_filters` | `Filters` | ✔ | ✘ |

Использование: `FileController.getFolderX(fileExt)` / `ProjectController.getFolderX(project)` →
`PropertyCdfController.getOrCreate(parentClass, parentId, Folders.X.propertyCdfKey)`; если значение
пустое — используется `project.folder + File.separator + Folders.X.folderName`. Обратите внимание на
коллизию: `SHOTS` и `SHOTS_COMPRESSED_WITH_AUDIO` имеют одинаковый `folderName = "Shots"`, но
разные ключи — переопределение одного не влияет на другой. `forFile=false` → папка только
проектного уровня (`PERSONS`, `FILTERS`).

### `ReorderTypes` ([ReorderTypes.kt](../../src/main/kotlin/com/svoemesto/ivfx/enums/ReorderTypes.kt)) — `enum class ReorderTypes { MOVE_UP, MOVE_DOWN, MOVE_TO_FIRST, MOVE_TO_LAST }`

Не персистится; используется в `reOrder(reorderType, entity)` контроллеров `Filter`,
`FilterGroup`, `FilterCondition`, `Track`, `Property`, `PropertyCdf` (сдвиг `order` соседей на ±1).
Обратите внимание на несогласованность направления: в `FilterConditionController.reOrder`
`MOVE_DOWN` означает «вниз по списку» (увеличить `order`), а `MOVE_UP` — уменьшить.

### `VideoContainers.kt` — пять enum'ов, все с шаблоном `(…)`; значения сохраняются в `tbl_projects` строками (enum'ами не являются)

| enum | name | extention/codec | default |
|---|---|---|---|
| `VideoContainers` | `MP4` | `mp4` | **✔** |
| | `MKV` | `mkv` | |
| | `MXF` | `mxf` | |
| `LosslessContainers` | `MP4` | `mp4` | |
| | `MKV` | `mkv` | **✔** |
| | `MXF` | `mxf` | |
| `VideoCodecs` | `X264` | `libx264` | **✔** |
| | `DNX` | `dnxhd` | |
| `LosslessVideoCodecs` | `RAW` | `rawvideo` | **✔** |
| | `DNX` | `dnxhd` | |
| `AudioCodecs` | `AAC` | `aac` | **✔** |
| | `AC3` | `ac3` | |
| | `MP3` | `mp3` | |
| | `PMC` | `pcm_s16le` | (опечатка: должно быть `PCM`) |

Значения по умолчанию — единственные значения с `default = true`; `Project` инициализирует свои
поля выражениями `…values().firstOrNull{it.default}?.name`. Обратите внимание: в `Project` хранится
`name` контейнера/кодека (например `"X264"`), а `extention`/`codec` используется в коде как
`VideoContainers.valueOf(fileExt.projectExt.project.container).extention` (в `ShotExt`,
`FilterEditFXController`) — то есть связь по `name`, а не по enum-объекту. У `LosslessContainers`
и `VideoContainers` одинаковые `name`, но разные значения по умолчанию — при `valueOf` по имени
контейнера из lossless-поля может «подхватиться» не то расширение.

---

## 7. `modelsext` — что это за слой

`models/` — чистый JPA, `modelsext/` — «расширенные» DTO/витрины для UI (JavaFX), безPersistence.
Общий принцип: `*Ext` оборачивает сущность, лениво кэширует вычисленные пути/картинки
(`private var _x: … ?: null` + `get()`), умеет `resetPreview()`/`resetFieldsLinkedShortName()`/
`resetFieldsLinkedPath()` при переименовании файла или смене пути.

| Ext | Что добавляет |
|---|---|
| `ProjectExt` | 15 вычисленных папок проекта (`folderPreview`, `folderLossless`, …, `folderFilters`), всё через `ProjectController.getFolderX` |
| `FileExt` | `fps`, `framesCount`, 13 папок, 15 «существует/готово» флагов (`hasPreview`…`hasConcat`, каждый со строкой `hasXString: "✓"/"✗"`), Observable-списки `framesExt/shotsExt/scenesExt/eventsExt/facesExt`, `framesWithFaces: MutableSet<Int>` |
| `ShotExt` | `start`/`end`/`duration` (через `IvfxFFmpegUtils`), `sceneExt`/`eventsExt` (вычисляемые связи!), `filenameWithoutExt`, 3 пути к вырезанным видео (`pathToCompressedWithAudio`, `pathToLosslessWithAudio`, `pathToLosslessWithoutAudio`) + флаги `has…`, `personsExt`, 6 превью-меток (`labelFirst1..3`, `labelLast1..3`), иконка типа плана |
| `SceneExt` / `EventExt` | `shotsExt` (из `fileExt.shotsExt` по границам), `personsExt`, `start/end/duration`, `sceneNameLabel`/`eventNameLabel` с `ContextMenu` переименования, по 6 превью-меток |
| `FrameExt` | `pathToSmall/Medium/Full` (`%06d`), `biSmall/biMedium/biFull`, `previewX`/`labelX`, стабы `blank_frame_{small,medium,full}.jpg`, `facesExt()` |
| `FaceExt` | вычисляемые пути к кадру/лицу (`%06d`, `%02d`), по `@SerializedName`-полям для экспорта в JSON (`toSerialize*`), ленивая генерация превью лица с оверлеями (треугольник зелёный = `isExample`, красный = `isManual`), `vector` с авто-`save` |
| `PersonExt` | `pathToSmall/Medium` по `person.uuid`, генерация аватара из кадра или вырезанного лица, стабы `blank_person_{small,medium}.jpg` |
| `FilterExt`/`FilterGroupExt`/`FilterConditionExt` | вычисление выборки шотов (см. §4) |
| `MatrixFrame`/`MatrixPageFrames`/`MatrixFace`/`MatrixPageFaces` | разбивка кадров/лиц на «страницы-сетки» для JavaFX; логика переноса строки по `Frame.isFinalFind` (конец сцены → новая строка) |

---

## 8. Признаки незавершённости, заглушек и мёртвого кода

Прямых маркеров `TODO`/`FIXME`/`XXX`/`HACK` **нет ни в одном из четырёх каталогов** (проверено).
Но незавершённость выражена структурно:

1. **Большой закомментированный блок в `Shot.kt` (строки 58–120, 63 строки).** Закомментированы
   `@OneToMany var eventsShots: MutableSet<EventShot>` и вычисляемые `isBodyScene`, `isStartScene`,
   `isEndScene`, `isBodyEvent`, `isStartEvent`, `isEndEvent`. Логика этих свойств фактически
   переехала в UI в виде оверлеев на превью (`OverlayImage.setOverlayIsStartScene/IsEndScene/
   IsBodyScene/IsStartEvent/IsEndEvent/IsBodyEvent`, вызываются в `ShotExt.previewsFirst/previewsLast`).
   То есть связь Scene↔Shot/Event↔Shot была заменена на диапазоны кадров, а старый код оставлен.
2. **Неиспользуемые импорты во всех «связевых» моделях** — следы удалённых `@ManyToMany`:
   * `Shot.kt`: `ManyToMany`, `JoinTable`, `OneToMany`, `CascadeType`, `Fetch`, `FetchMode`;
   * `Scene.kt`, `Event.kt`: те же (кроме `FetchMode` используется 0 раз в теле);
   * `Filter.kt`, `FilterGroup.kt`: `ManyToMany`, `JoinTable`, `OneToMany`, `CascadeType`, `Fetch*`;
   * `FilterCondition.kt`: `ManyToMany`, `OneToMany`, `CascadeType`, `Fetch*`;
   * `File.kt`: `JoinTable`, `org.hibernate.Hibernate`; `Project.kt`: `org.hibernate.Hibernate`.
3. **`ShotTypeSize` — полностью не подключённый enum**: импортирован в `Shot.kt`, но поля нет;
   ни одного использования в `fxcontrollers`. При этом PNG-ресурсы `shot_type_size_*.png` есть.
   Рядом `PersonType.EXTRAS` тоже нигде не используется.
4. **`ShotExt`**: закомментированные дубли полей `private var _fileExt/_firstFrameExt/_lastFrameExt`
   (строки 38–40) — после переноса в конструктор; поле `buttonGetType: Button = Button()`
   и то же в `SceneExt`/`EventExt` — пустые кнопки без обработчика.
5. **`ShotTmp2Cdf`** — вторая версия временной таблицы; в `ShotRepo` нет ни одного запроса,
   читающего `tbl_shots_tmp2_cdf` (только `INSERT`/`DELETE` в `ShotTmp2CdfRepo` и
   `findByComputerId`). Аналогично в `ShotController` закомментирована построчная загрузка
   (`// Main.shotTmp2CdfRepo.addByShotId(Main.ccid, it)`) в пользу пакетной `addByShotIds`.
6. **`Scene.parentId` и `Event.parentId`** — поля объявлены, но нигде не заполняются и не читаются
   (иерархия сцен/событий не реализована).
7. **`FaceExtJson.frameId`** — присутствует в DTO, но при `frameId != 0` код ищет лицо по
   `faceRepo.findById(faceExtJson.frameId)` (**по `frameId`, а не `faceId`**) —
   `FaceController.createOrUpdate`; похоже на копипаст-баг.
8. **Асимметрия API фильтра**: `FilterConditionExt.shots()` и `shotsExt()` обрабатывают **только**
   `objectClass == "Person"` (остальные ветки — `else -> {}`), тогда как `shotsIds()` поддерживает
   все 5 типов. То есть часть функциональности существует только в одном из трёх параллельных API.
9. **Пустые ветки `File::class.java.simpleName -> {}`** во всех `when(subjectClass)` в
   `FilterConditionExt` — заготовка под фильтр «файл целиком».
10. **`ShotTypeSize`/`ShotTypePerson` — хрупкая загрузка ресурсов**: `getResource(...)!!.file.substring(1)`
    (и во всех `Ext`-классах) — упадёт при запуске из JAR, а не из распакованной папки. Тот же приём
    в `FaceExtJson`/`PersonExt` для `blank_person_*.jpg`.
11. **`FilterConditionRepo`/`FilterGroupRepo`/`PropertyRepo`/`PropertyCdfRepo`/`SceneRepo`/
    `EventRepo`/`PersonRepo`/`ShotTmp2CdfRepo`/`FilterConditionRepo`** — неиспользуемые импорты
    (`File`, `Filter`, `Scene`, `Shot`, `ShotTmpCdf`, `FilterGroupExt`, `FilterConditionExt`),
    оставшиеся от удалённых сигнатур.
12. **Дублирование `getSceneForShot`/`getEventForShot` и «сцены/события по шоту» без кеша** —
    на каждый `ShotExt.sceneExt` делается запрос (`ShotExt.sceneExt` → `fileExt.scenesExt`,
    который заполняется один, но `getShotsForScenes` — нативный запрос на каждую связку).
13. **`Face` требует `person` (не-null `lateinit`)**, а `FrameExt.facesExt()` вручную читает
    `person_id` сырым JDBC `Main.connection.prepareStatement("select * from tbl_faces as tf where tf.id = ?")`
    вместо репозитория — обход ORM, чтобы «пережить» прокси.
14. **Отсутствие `@Version` (optimistic locking) и уникальных ограничений/индексов** ни на одной
    таблице; `@Column(nullable=false)` в `@Lob`-полях (`Face.vector`, `Property.value`) в сочетании
    с default `""`/`"0.0"`.
15. **`Person.uuid` инициализируется в поле** (`UUID.randomUUID().toString()`). Формально корректно
    (аннотации стоят на полях → Hibernate использует field access и при загрузке перезаписывает поле
    значением из колонки), но это «магическое» поведение: два несохранённых объекта получают разные
    uuid, а значит, опереться на `uuid` до `save()` нельзя.

---

## 9. Сводка: 18 таблиц

`tbl_projects`, `tbl_projects_cdf`, `tbl_files`, `tbl_files_cdf`, `tbl_files_tracks`, `tbl_frames`,
`tbl_faces`, `tbl_shots`, `tbl_scenes`, `tbl_events`, `tbl_persons`, `tbl_properties`,
`tbl_properties_cdf`, `tbl_shots_tmp_cdf`, `tbl_shots_tmp2_cdf`, `tbl_filters`, `tbl_filters_groups`,
`tbl_filters_conditions`.
