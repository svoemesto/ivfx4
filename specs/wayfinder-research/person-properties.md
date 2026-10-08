# Свойства персон (`tbl_properties`) и порядок файлов — разведка

> **Статус**: временная заметка (Ephemeral), вне `docs/`. Не является частью
> Living Documentation.
>
> **Дата**: 2026-10-08. **Задача**: разведка для «свойств границ участия персоны
> в сериях».
>
> **Прочитано по протоколу (Hard Gate)**: `docs/README.md` →
> `docs/domains/properties/domain.md` → `docs/domains/faces/domain.md` → код.
> Домен properties в карте README называется
> [properties](../docs/domains/properties/domain.md) — так и искал.

---

## 1. Модель и чтение

**Сущность** — [models/Property.kt](../src/main/kotlin/com/svoemesto/ivfx/models/Property.kt),
таблица `tbl_properties` (`@Table(name = "tbl_properties")`, строка 16).
Это ровно то имя, что предполагалось в задании. Поля Kotlin → колонки:

| Kotlin | Колонка | Модель | Живая схема | Совпало |
|---|---|---|---|---|
| `id: Long` | `id` | `nullable = false`, IDENTITY | `bigint not null`, identity | ✅ |
| `order: Int` | `order_property` | `nullable = false`, default 0 | `integer not null default 0` | ✅ |
| `parentClass: String` | `parent_class` | `varchar(255) default ''` | `varchar(255) default ''`, nullable | ✅ |
| `parentId: Long` | `parent_id` | `nullable = false`, default 0 | `bigint not null default 0` | ✅ |
| `key: String` | `property_key` | `varchar(255) default ''` | `varchar(255) default ''`, nullable | ✅ |
| `value: String` | `property_value` | `text` | `text`, **без default**, nullable | ✅ |

Живая схема снята так:

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c "\d tbl_properties"
```

Ключевое: **внешних ключей нет**, связь только парой
(`parent_class` — строковое имя класса, `parent_id` — id). Индекс есть один
(`idx_props_class_parent` на `(parent_class, parent_id)`, из
[tools/add-indexes.sql](../tools/add-indexes.sql):54). **Уникального ограничения
на `(parent_class, parent_id, property_key)` нет** — см. п. 2.

### Как читаются свойства по родителю

Готового метода, возвращающего **`Map<String, String>` (ключ → значение) по
одному родителю, в проекте нет** — проверено `grep -rn "Map<String, *String>"
src/main/kotlin/` (пусто). Есть четыре других формы:

| Что нужно | Что есть | Где |
|---|---|---|
| список свойств одного родителя | `PropertyController.getListProperties(parentClass, parentId): List<Property>` | [controllers/PropertyController.kt:27](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt) |
| все ключи класса со значениями | `PropertyController.getMapKeyValuesByParentClass(parentClass): MutableMap<String, MutableList<String>>` | [PropertyController.kt:15](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt) |
| наличие одного ключа у персоны | `PersonController.isPropertyPresent(person, key): Boolean` | [controllers/PersonController.kt:25](../src/main/kotlin/com/svoemesto/ivfx/controllers/PersonController.kt) |
| список уникальных ключей класса | `PropertyController.getKeys(parentClass): List<String>` | [PropertyController.kt:120](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt) |
| сырой список из репозитория | `Main.propertyRepo.findByParentClassAndParentId(cls, id)` | [repos/PropertyRepo.kt:32](../src/main/kotlin/com/svoemesto/ivfx/repos/PropertyRepo.kt) |

**Асимметрия, важная для работы:** `getPropertyValue(entity, key)` есть у
семи классов — `Track`, `File`, `Scene`, `Project`, `Frame`, `Event`, `Shot`
(например [TrackController.kt:46](../src/main/kotlin/com/svoemesto/ivfx/controllers/TrackController.kt)) —
а **у `Person` его нет**. У `PersonController` есть только `isPropertyPresent`.
Значение свойства персоны нигде в коде не читается одним вызовом; читатели
(п. 6) идут в обход, через `PropertyController.getListProperties` или через SQL.

Тонкость `getListProperties`: он зовёт
`findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(parentClass,
parentId, 0)` — то есть **свойства с `order_property = 0` не показываются
никогда**. Новое свойство, созданное в обход `editOrCreate`, с нулевым
порядком будет невидимым.

Тонкость `getMapKeyValuesByParentClass`: значение `""` **добавляется
принудительно** в список значений каждого ключа (`vals.add("")`,
[PropertyController.kt:20](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt)),
поэтому размер списка значений всегда ≥ 1 и «пустое» значение нельзя отличить
от отсутствующего.

---

## 2. Запись

**Пишет** [controllers/PropertyController.kt](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt)
(статические методы в `companion object`). Два рабочих входа:

- `editOrCreate(parentClass, parentId, key = "", value = "")` —
  строки 46–63. Ищет `findByParentClassAndParentIdAndKey(...).firstOrNull()`.
  **Нашёл → обновляет значение существующей строки** (то есть перезапись ключа
  = UPDATE той же строки, новая строка не создаётся). **Не нашёл → создаёт**
  новую: `order` = (максимальный `order_property` у родителя) + 1
  ([PropertyController.kt:55-56](../src/main/kotlin/com/svoemesto/ivfx/controllers/PropertyController.kt)),
  а пустой ключ заменяется на автоимя `"Key # <order>"` (строка 57).
- `getOrCreate(parentClass, parentId, key)` — строки 31–44, то же, но значение
  всегда `""` и **только создаёт**, ничего не обновляет.
- `save(property)` — строки 74–76, голый `propertyRepo.save`. Именно этим
  пользуется UI при правке полей.

### Защита от дублей по ключу

**Только на уровне приложения, и только в `editOrCreate`/`getOrCreate`.**

1. **В базе защиты нет.** Уникального индекса на
   `(parent_class, parent_id, property_key)` не существует — это видно в
   `\d tbl_properties`: только `tbl_properties_pkey` и
   `idx_props_class_parent`. На живой базе дублей сейчас тоже нет:

   ```
   docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
     "SELECT count(*) FROM (SELECT parent_class,parent_id,property_key
      FROM tbl_properties GROUP BY 1,2,3 HAVING count(*)>1) d;"
    dup_key_groups
   ----------------
               0
   ```

2. **UI защиты не имеет.** `saveCurrentProperty()`
   ([PersonEditFXController.kt:339](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/PersonEditFXController.kt))
   берёт ключ из текстового поля и делает `PropertyController.save(currentProperty!!)`
   — без всякой проверки «а нет ли уже такого ключа». Оператор, переименовав
   свойство в имя, уже занятое другим свойством той же персоны, получит **две
   строки с одинаковым ключом**, и база это пропустит. Дальше
   `editOrCreate` будет читать только `firstOrNull()` — то есть одну из двух
   строк, а вторая станет невидимой для всех, кто читает через
   `editOrCreate`, и останется в UI отдельной строкой таблицы.

3. `delete(property)` (строка 65) сначала делает `MOVE_TO_LAST`, чтобы
   перенумеровать хвост, и только затем удаляет.

Итог: **перезапись значения по тому же ключу безопасна; переименование ключа в
совпадающий — нет.**

---

## 3. Интерфейс: редактор свойств персоны **есть**

Это центральный вопрос, и ответ однозначный: **UI-редактор произвольных
свойств персоны существует, отдельным окном**. Строить ничего не нужно.

**Контроллер** — [fxcontrollers/PersonEditFXController.kt](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/PersonEditFXController.kt),
**форма** — [resources/.../person-edit-view.fxml](../src/main/resources/com/svoemesto/ivfx/fxcontrollers/person-edit-view.fxml).

Окно открывается из пяти мест: `ShotsEditFXController.kt:1588`, `:1650`,
`:2754`; `PersonSelectFXController.kt:239`; `ProjectEditFXController.kt:1367`
(последнее — создание персоны, `editPerson(..., null, ...)`).

Что в окне есть:

| Элемент | `fx:id` | Назначение |
|---|---|---|
| Таблица свойств | `tblProperties` (fxml:83) | список ключ/значение |
| Поле ключа | `fldPropertyKey` (fxml:158) | **правится вручную, произвольный текст** |
| Поле значения | `fldPropertyValue` (fxml:159) | `TextArea`, произвольный текст |
| Кнопка «+» | `btnPropertyAdd` → `doPropertyAdd` (fxml:135) | контекстное меню |
| Кнопка «✖» | `btnPropertyDelete` → `doPropertyDelete` (fxml:143) | удаление |
| 4 кнопки порядка | `doPropertyMoveToFirst/Up/Down/ToLast` (fxml:94,102,114,122) | перестановка |

**Связка FXML ↔ контроллер проверена полностью:** все 19 `fx:id` имеют
соответствующие поля в контроллере, все 9 `onAction` имеют методы
(проверено скриптом по аналогии с `tools/check-fx-wiring.sh`; ни одного
`MISSING`). Это важно — ловушка «кнопка нарисована, обработчик есть, но
`onAction` в форме нет» здесь **не сработала**.

Механика добавления (`doPropertyAdd`, строки 386–527) даёт три способа:

1. «Добавить новое свойство персонажа» — создаёт пустое свойство с
   автоименем `Key # N` (строки 394–421).
2. Меню **существующих** ключей класса Person (`getMapKeyValuesByParentClass`,
   строка 426) — оператор выбирает ключ и значение из уже существующих.
3. «Добавить все свойства для персонажа» (строки 498–520) — добивает
   отсутствующие ключи, не трогая имеющиеся.

Сохранение происходит по событию снятия фокуса с полей
(`focusedProperty` listeners, строки 316–325) и при закрытии окна.

**Чего в этом окне нет:** кнопок «добавить персону» и «удалить персону» —
обработчики `doPersonAdd` и `doPersonDelete` есть (строки 606–613), но оба
**пустые**, содержат только `Trace.action(...)`. Так что персон из этого окна
не создать — но свойства завести можно в полном объёме.

**Важно для п. 6:** двойной клик по свойству, чей ключ начинается с `url_`
(без учёта регистра), открывает значение в системном браузере
([PersonEditFXController.kt:290](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/PersonEditFXController.kt)).
Тот же приём есть для свойств файла и проекта
(`ProjectEditFXController.kt:949`, `:987`). Это единственное **специальное
толкование имени ключа** в UI.

**Редактора свойств персоны нет НИ в одном другом экране.** В
`ShotsEditFXController` таблицы свойств есть, но все три — для `Shot`
(строка 1119), `Scene` (1225) и `Event` (1405), персон там нет.

---

## 4. Откуда взялись `name` и `url` — импорта в проекте **нет**

Значения указывают на вики по «Игре престолов»:

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
  "SELECT property_key, order_property, property_value
   FROM tbl_properties WHERE parent_class='Person' AND parent_id IN (9,10);"

 property_key | order_property |             property_value
--------------+----------------+--------------------------------------
 name         |              1 | Eddard Stark
 url          |              2 | https://gameofthrones.fandom.com/ru/wiki/Эддард_Старк
 name         |              1 | Catelyn Stark
 url          |              2 | https://gameofthrones.fandom.com/ru/wiki/Кейтилин_Старк
```

**Кода импорта в репозитории нет.** Проверено:

- `grep -rn '"url"' src/main/kotlin/` — единственные совпадения это
  `H2db.kt:176` и `H2db.kt:211`, где `url` — JDBC-адрес базы из
  `tbl_databases`, к персонам отношения не имеет;
- `grep -rn "editOrCreate\|getOrCreate"` — ни один вызов не содержит
  литерала `name` или `url` в контексте Person;
- `grep -rni "вики\|wiki\|fandom\|импорт персон"` по всему репозиторию —
  совпадений нет (кроме `Ephemeral` в README/AGENTS);
- `grep -rl "gameofthrones\|fandom" .` вне `backups/` — пусто;
- `git log --diff-filter=D --name-only` — удалённых импортёров не было.

То есть `name`/`url` **не зашиты в код ни под два ключа, ни под сколько-либо**.
Они попали в базу извне репозитория (одноразовый скрипт или ручной ввод/SQL),
и в дампе [backups/ivfx-full-20261007-1930.sql](../backups/ivfx-full-20261007-1930.sql)
лежат уже как обычные данные:

```
INSERT INTO "tbl_properties" (...) VALUES (256,'name',1,'Person',9,'Eddard Stark');
INSERT INTO "tbl_properties" (...) VALUES (257,'url',2,'Person',9,'https://gameofthrones.../Эддард_Старк');
```

**Ответ на вопрос «можно ли тем же механизмом записать произвольное свойство»:**
да, и даже проще — отдельного «механизма импорта» не существует вовсе.
Единственный путь записи свойства — `PropertyController.editOrCreate` /
`save`, и он **полностью генеричен по ключу**: ни одна его строка не упоминает
`name` или `url`. В том числе и путь «через UI» — тот же
`editOrCreate`, вызванный из `doPropertyAdd`. Специфика ключей `name`/`url`
живёт только в данных, а не в коде.

Смежный факт: распознавание лиц **свойства не использует**. Галерея строится по
колонке `nameInRecognizer` (обычная колонка `tbl_persons`, а не EAV):
галерея отбирается по `personType` ([RecognizeFaces.kt:99-100](../src/main/kotlin/com/svoemesto/ivfx/threads/projectactions/RecognizeFaces.kt)),
а имя сверяется по `face.personRecognizedName`
([RecognizeFaces.kt:108](../src/main/kotlin/com/svoemesto/ivfx/threads/projectactions/RecognizeFaces.kt))
и `Person.nameInRecognizer` ([PersonController.kt:122](../src/main/kotlin/com/svoemesto/ivfx/controllers/PersonController.kt)).
То есть добавление свойства не заденет распознавание.

---

## 5. Живые данные (`ivfx-db`, только SELECT)

Всего свойств по классам:

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
  "SELECT parent_class, count(*) FROM tbl_properties GROUP BY 1 ORDER BY 2 DESC;"

 parent_class | count
--------------+-------
 Track        |  2526
 Person       |   137
 File         |     4
```

### Свойства Person

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
  "SELECT property_key, count(*) FROM tbl_properties
   WHERE parent_class='Person' GROUP BY 1 ORDER BY 2 DESC;"

 property_key | count
--------------+-------
 url          |    69
 name         |    68
```

- **Персон всего: 73** (`SELECT count(*) FROM tbl_persons`).
- **Персон со свойствами: 69** (`count(DISTINCT parent_id) WHERE parent_class='Person'`).
- **Персон с ключом вне `name`/`url`: ноль** — таких ключей в базе нет вообще.
- Персон без свойств: 5 — три служебные (`UNDEFINDED` id=1, `NONPERSON` id=2,
  `EXTRAS` id=3), обычная `Лютоволк` (id=41) и `Янос Слинт` (id=67).
- Янос Слинт (id=67) — единственная персона с **одним** свойством: только
  `url`, `order_property = 1`. То есть у него `name` не заведён, и это
  подтверждает, что порядок свойств выдаётся слоями и не обязан быть
  плотным.
- `property_value` пустых (`NULL`) среди свойств Person нет: 0.

### Свойства File — утверждение правил **устарело**

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
  "SELECT id, parent_id, property_key, property_value
   FROM tbl_properties WHERE parent_class='File' ORDER BY id;"

  id | parent_id | property_key | property_value
-----+-----------+--------------+----------------
 2662|         1 | Прогресс     | Персонажи
 2663|         2 | Прогресс     | Персонажи
 2664|         3 | Прогресс     | Персонажи
 2665|         4 | Прогресс     | Персонажи
```

**Свойства у `File` есть, их ровно 4, ключ один — `Прогресс`, значение
`Персонажи`, по одной на файлы id 1–4 (`GOT.S01E01`…`S01E04`).**
Утверждение `AGENTS.md` (Tier-1 «Индикаторов выполнения операций в базе нет»):
«`tbl_properties` — последняя содержит только свойства `Track` и `Person`»
**опровергнуто**. Само свойство, впрочем, к флагам выполнения отношения не
имеет — это ручная пометка оператора, а не признак состояния пайплайна, так
что вывод про «индикаторов нет» остаётся верным, а вот перечисление классов —
нет.

Дополнительно: ключ `Прогресс` **не встречается нигде в коде**
(`grep -rn "Прогресс" src/main/` — пусто). Это лучшее в базе доказательство
того, что произвольный ключ можно завести руками через существующий UI и
он спокойно живёт: никакой whitelist, никакой enum, никакой валидации.

Для сравнения, `Track` забит 77 ключами, но это **другое население** —
они пишутся программно из MediaInfo (`TrackController.createTracksFromMediaInfo`),
а не оператором. Пересечения с Person/File нет: множества ключей не
пересекаются.

---

## 6. Кто читает свойства Person

Полный перечень мест, где код обращается к свойствам Person.

| # | Место | Что читает | Какие ключи ожидает |
|---|---|---|---|
| 1 | `PersonController.isPropertyPresent` ([PersonController.kt:25](../src/main/kotlin/com/svoemesto/ivfx/controllers/PersonController.kt)) | наличие ключа у персоны | **любой, передаётся параметром** |
| 2 | `FilterEditFXController.doCreateVideoForAllPersons` ([FilterEditFXController.kt:678](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/FilterEditFXController.kt)) | `getListPersonsExt(...).filter { isPropertyPresent(it.person, "end") }` | **жёстко `"end"`** — персон без этого свойства в выборку не попадает |
| 3 | `FilterConditionExt.shotsIds()` / `shots()` / `shotsExt()` ([FilterConditionExt.kt:51,142,180](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FilterConditionExt.kt)) | ветка `objectClass == "Person Property"`, уходит в SQL `ShotRepo.getShotsIdsFor*TmpAndPersonProperty(ccid, objectName, objectValue)` | **любой**, но всегда по паре ключ+значение |
| 4 | `FilterConditionCreateFXController` ([строки 230, 232, 256, 258, 284, 286, 326](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/FilterConditionCreateFXController.kt)) | `getMapKeyValuesByParentClass("Person")` → меню ключей и значений | **любой**, ключи берутся из данных |
| 5 | `PersonEditFXController` (строки 184, 222, 412, 448, 480, 545, 595) | `getListProperties("Person", personId)` — заполнение и перечитывание таблицы | **любой** |
| 6 | `PersonEditFXController` ([строка 290](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/PersonEditFXController.kt)) | `key.startsWith("url_", ignoreCase = true)` → открыть в браузере | **префикс `url_`** |
| 7 | `PersonController.delete` ([PersonController.kt:107](../src/main/kotlin/com/svoemesto/ivfx/controllers/PersonController.kt)) | `PropertyController.deleteAll("Person", person.id)` при удалении персоны | все |

**Три места, где ключ трактуется особым образом** — именно то, о чём
предупреждает задание:

1. **`"end"`** (№ 2) — единственный ключ, который читается как **признак
   принадлежности к чему-то**. Персона без свойства `end` не попадает в
   фильтр «видео со всеми событиями по персонам». Это семантический ключ,
   и **новое свойство с именем `end` пересечётся с ним немедленно**.
2. **префикс `url_`** (№ 6) — двойной клик по такому ключу не редактирует, а
   открывает браузер. Ключ `url` (без подчёркивания) под это правило **не
   попадает** — в живых данных он ровно `url`, что согласуется.
3. **`objectClass == "Person Property"`** (№ 3) — специальная ветка в SQL.
   Здесь новое свойство не пересекается, а наоборот **подхватывается
   автоматически**: запросы в [ShotRepo.kt:199-200, 222-223](../src/main/kotlin/com/svoemesto/ivfx/repos/ShotRepo.kt)
   фильтруют по `tpp.parent_class = 'Person' AND tpp.property_key = ?2 AND
   tpp.property_value = ?3` — то есть **любой** ключ, и значения берутся
   строго, без LIKE и без диапазонов.

Про формат `objectClass` стоит сказать отдельно: он склеен **пробелом** —
`"Person Property"` ([FilterConditionExt.kt:51](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FilterConditionExt.kt),
[FilterConditionCreateFXController.kt:232](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/FilterConditionCreateFXController.kt)).
Это не схема в базе, а соглашение строкового enum-подобного поля.

Ещё одно наблюдение: SQL в `ShotRepo` идёт от лица к персоне через
`tfc.person_id`, поэтому **любое** свойство Person автоматически становится
доступным для отбора в выборках — отдельной регистрации не требуется.

---

## 7. Порядок файлов

**`tbl_files.order_file`** — единственный источник порядка серий в проекте.
Колонка описана как `@Column(name = "order_file", nullable = false,
columnDefinition = "int default 0") var order: Int = 0`
([models/File.kt:43-44](../src/main/kotlin/com/svoemesto/ivfx/models/File.kt)),
в БД — `integer not null default 0`, индекс `idx_files_proj (project_id,
order_file)` присутствует.

Живые данные — порядок плотный и осмысленный:

```
docker exec -e PGPASSWORD=123 ivfx-db psql -U postgres -d ivfx -c \
  "SELECT id, order_file, name FROM tbl_files ORDER BY order_file;"

 id | order_file |          name
----+------------+------------------------
  1 |          1 | GOT.S01E01.BDRip.1080p
  ...
 10 |         10 | GOT.S01E10.BDRip.1080p
```

### Кто задаёт

**Оператор, кнопками — не перетаскиванием.** Четыре кнопки в
[project-edit-view.fxml:424, 432, 444, 452](../src/main/resources/com/svoemesto/ivfx/fxcontrollers/project-edit-view.fxml)
(`btnFileMoveToFirst/Up/Down/ToLast` → `doFileMoveToFirst/Up/Down/ToLast`,
[ProjectEditFXController.kt:1515-1535](../src/main/kotlin/com/svoemesto/ivfx/fxcontrollers/ProjectEditFXController.kt))
вызывают `doMoveFile(ReorderTypes)` →
`FileController.reOrder` ([FileController.kt:573](../src/main/kotlin/com/svoemesto/ivfx/controllers/FileController.kt),
тело 573–620).

Drag-and-drop для файлов **не реализован вовсе**:
`grep -rn "setOnDragOver\|setOnDragDropped\|DragEvent\|onDrag"
ProjectEditFXController.kt` — пусто.

При создании файла порядок автоматический: `order = max(order_file) + 1`
([FileController.kt:507-508](../src/main/kotlin/com/svoemesto/ivfx/controllers/FileController.kt)),
где максимум берётся запросом `ORDER BY order_file DESC LIMIT 1`
([FileRepo.kt:22](../src/main/kotlin/com/svoemesto/ivfx/repos/FileRepo.kt)).
Перестановка сдвигает соседей: `MOVE_UP` уменьшает `order` на 1 и
соответственно увеличивает у предыдущего, `MOVE_DOWN` — наоборот,
`MOVE_TO_LAST` перенумеровывает хвост.

### Отдельного номера эпизода/серии в коде нет

Колонки `season`/`episode` не существует ни в `tbl_files` (`\d tbl_files`
показывает 6 колонок: `id`, `name`, `order_file`, `short_name`, `project_id`
и служебные), ни в модели. Номер эпизода живёт **только в имени файла**
(`GOT.S01E07`), и код его нигде не разбирает — `grep` по `S01E`,
`substringBefore`, `split(".")` в контексте имён файлов даёт только
комментарий в `IvfxUtils.kt:59`. Единственный разбор имён — это регулярка
для кадров `_frame_\d{6}\.jpg` ([FileController.kt:237](../src/main/kotlin/com/svoemesto/ivfx/controllers/FileController.kt)).

### Как код определяет «позднее/раннее»

Везде по `order_file`, через сравнение в самом объекте:

- `File.compareTo` = `this.order - other.order`
  ([File.kt:29-31](../src/main/kotlin/com/svoemesto/ivfx/models/File.kt));
- `FileExt.compareTo` = `this.fileOrder - other.fileOrder`
  ([FileExt.kt:229-230](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FileExt.kt)),
  где `fileOrder` — просто `file.order` ([FileExt.kt:11](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FileExt.kt));
- `FaceExt.compareTo` начинается с проверки файла:
  `if (this.face.file.order != other.face.file.order) return this.face.file.order - other.face.file.order`
  ([FaceExt.kt:39-40](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FaceExt.kt)) —
  то есть «файл раньше/позже» это **первый** уровень сравнения лиц.

Значения `order_file` показываются оператору колонкой
`colFileOrder` с `PropertyValueFactory("fileOrder")` в трёх экранах:
`ProjectEditFXController.kt:577`, `ProjectActionsFXController.kt:213`,
`FilterEditFXController.kt:265`.

**Важное замечание в духе уже известного правила** (см. Changelog 2.6.1 и
`AGENTS.md`): `FileRepo.findByProjectId` — **без `ORDER BY`**
([FileRepo.kt:25](../src/main/kotlin/com/svoemesto/ivfx/repos/FileRepo.kt)),
ровно как `FaceRepo`. Порядок держится только на сравнении объекта. Список
файлов в UI загружается запросом **с** сортировкой
(`findByProjectIdAndOrderGreaterThanOrderByOrder(projectId, 0)`,
[LoadListFilesExt.kt:31](../src/main/kotlin/com/svoemesto/ivfx/threads/loadlists/LoadListFilesExt.kt)),
но любой другой потребитель `findByProjectId` полагаться на выдачу не может.
Места, где список обходится без сортировки:
`RecognizeFaces.kt:102` (`for (galleryFile in Main.fileRepo.findByProjectId(projectId))`)
и [FilterConditionExt.kt:169](../src/main/kotlin/com/svoemesto/ivfx/modelsext/FilterConditionExt.kt)
(`files.firstOrNull { file -> file.shots.map{it.id}.contains(shot.id) }` —
перебор множества накопленных файлов).

---

## Что это значит для добавления свойств «границ участия персоны в сериях»

Без проектирования решения — только то, что следует из разведки:

1. **Механизм записи и интерфейс уже есть, ничего строить не надо.**
   `PropertyController.editOrCreate("Person", id, key, value)` — единственная
   точка записи, она генерична по ключу; произвольный ключ и значение
   заводится либо одной строкой кода, либо оператором в существующем окне
   `PersonEditFXController` (`fldPropertyKey` / `fldPropertyValue`).

2. **Структура хранения подходит, но не упорядочена и не защищена.**
   Порядок задаётся только через `editOrCreate` (`max(order)+1`), в БД нет
   уникальности на тройку (класс, id, ключ), а UI при переименовании ключа
   дубль не проверяет. Значит «границы» придётся либо хранить в значении
   одного свойства (тогда порядок и поиск — на разборе строки в коде), либо
   заводить по строке на границу (тогда нужна своя дисциплина порядка, и
   риск дублей реален).

3. **Есть три семантически занятых места, которые нельзя задевать.**
   Ключ `end` уже читается как признак принадлежности к выборке;
   префикс `url_` уже перехватывается двойным кликом;
   значения в SQL-фильтрах сравниваются строго, без диапазонов.

4. **Данные для «границ по сериям» придётся добывать отдельно.**
   Свойства Person — это только `name` и `url` у 69 персон из 73; факта
   «персона участвует в серии N» в EAV нет ни в одном виде, а связи
   «персона → серия» в базе нет — она выводится через
   `tbl_faces.person_id` внутри конкретного файла. Порядок серий при этом
   известен только как `order_file`, отдельного номера эпизода нет.