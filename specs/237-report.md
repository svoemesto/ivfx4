# 237 — Hibernate без стратегии именования колонок

**Найдено владельцем** при живом запуске после #236: приложение стартует,
открывает проект и файл, но на переходе к планам падает.

---

## Симптом

При открытии редактора планов:

```
Инициализация ShotsEditFXController.
...
Exception in thread "JavaFX Application Thread"
  org.springframework.dao.InvalidDataAccessResourceUsageException: could not extract ResultSet
  ...
Caused by: org.postgresql.util.PSQLException: ERROR: column facetrack0_.facecount does not exist
  Подсказка: Perhaps you meant to reference the column "facetrack0_.face_count".
```

Падение происходит в `ShotsEditFXController.reloadTracks` (`ShotsEditFXController.kt:3301`)
на вызове `Main.faceTrackRepo.findByShotId(...)` — вкладка «Треки» на экране планов.

Обратите внимание на стек: `QueryTranslatorImpl` и
`CriteriaQueryTypeQueryAdapter` — это **criteria-запрос**, собранный Spring Data.
Выборка сущности целиком тянет все её колонки, и первая несовпавшая роняет всё.

---

## Причина

В таблице `tbl_faces_tracks` колонки такие:

```
id, face_count, first_frame_number, last_frame_number, use_track, person_id, shot_id
```

В модели `FaceTrack` поля объявлены без `@Column`:

```kotlin
var firstFrameNumber: Int = 0
var lastFrameNumber: Int = 0
var faceCount: Int = 0
var useTrack: Boolean = true
```

Hibernate вне Spring Boot не подставляет стратегию физического именования и берёт
имена дословно из полей: `faceCount` → `faceCount` → Postgres видит `facecount`,
а в таблице `face_count`.

Схему этот же Hibernate когда-то создал — но **под Spring Boot**, где
`SpringPhysicalNamingStrategy` подставляется автоматически. Контекст в проекте
поднимается вручную через `AnnotationConfigApplicationContext` (ADR-0001),
автоконфигурация не участвует, и стратегия теряется.

---

## Масштаб

Затрагиваются **все** модели с полями без `@Column`, а не одна. В `FaceTrack`
это четыре поля: `faceCount`, `firstFrameNumber`, `lastFrameNumber`, `useTrack`.
Hibernate сообщает только о первом попавшемся — после починки первого вылезет
второй, если бы стратегия не закрыла класс целиком.

---

## Правка

Одна настройка в `SpringConfig.entityManagerFactory`:

```kotlin
setProperty("hibernate.physical_naming_strategy",
    "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy")
```

Класс проверен: лежит в `spring-boot-2.5.6.jar`, который и так в classpath.
Альтернатива `CamelCaseToUnderscoresNamingStrategy` в Hibernate 5.4.32
отсутствует — она появилась в Hibernate 6.

Имена таблиц стратегия не трогает: они заданы явно через `@Table(name = "tbl_…")`.

---

## Что проверено и как

Проверка шла на живой базе, но **без `h2db`**: она занята запущенным
приложением (embedded H2 не даёт второму процессу открыть файл). Контекст
собирался отдельно, с `DataSource` прямо на Postgres.

| Проверка | Результат |
|---|---|
| Тот же запрос, что упал — `findByShotId(shotId=3096)` | ✅ треков 0 за 34 мс |
| Тесты | `mvn -B test` → `Tests run: 28, Failures: 0` |
| Сборка | `mvn -B -q clean compile` → exit 0 |
| Стиль | `bash tools/check-ktlint.sh` → новых нарушений нет, отпечаток без изменений |
| Связь FXML | `bash tools/check-fx-wiring.sh` → форма и контроллер связаны |
| Документация | `python3 docs/scripts/lint-docs.py` → exit 0 |

Запущенное у владельца приложение не перезапускалось и не прерывалось:
оно держит старые классы в памяти и правку ещё не подхватило.

---

## Побочно найдено: приложение не запускается дважды

`h2db.mv.db` — встроенная H2, она держит файл монопольно. Второй процесс
падает на инициализации:

```
org.h2.jdbc.JdbcSQLNonTransientConnectionException: База данных уже используется
Database may be already in use: null. Possible solutions: close all other
connection(s); use the server mode [90020-200]
```

То есть параллельно два экземпляра не поднять, и проверять правки приходится
либо после закрытия приложения, либо контекстом в обход `h2db`. Это не баг,
но помешало проверке: она выполнена вторым способом.

---

## Что осталось

1. **Перезапуск у владельца** — правка в памяти работающего процесса ещё не
   подхвачена.
2. **Другие экраны не проверены.** Найденное — первая ошибка, всплывшая на
   первом же экране после планов. Что покажет следующий — неизвестно.
3. **Пробел со свойствами сцен и событий** из #236 не тронут.

## Чего не сделано и почему

- **Не перезапускал приложение владельца.** Он с ним работает.
- **Не добавлял `@Column` в модели.** 30 связей и множество полей; стратегия
  именования закрывает весь класс разом и в одном месте, а размазывать по
  моделям — значит забыть часть.
