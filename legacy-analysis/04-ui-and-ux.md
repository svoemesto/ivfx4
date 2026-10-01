# ivfx4 — Пользовательский интерфейс и сценарии работы

Исследование выполнено по исходникам `/home/nsa/ivfx4` (Kotlin + JavaFX + Spring, desktop, 2021–2022).
Прочитаны полностью: `apps/ProjectFXApp.kt`, все 11 `fxcontrollers/*.kt`, все 11 `fxcontrollers/*.fxml`,
`SpringConfig.kt`, `SomeUtils.kt`, `H2db.kt`, `Main.kt`, а также (для понимания отрисовки)
`modelsext/{MatrixFrame,MatrixPageFrames,MatrixFace,MatrixPageFaces,FrameExt,FaceExt,ShotExt,PersonExt}.kt`,
`utils/OverlayImage.kt`, `enums/{PersonType,ShotTypePerson,ReorderTypes}.kt`.

Ничего не изменялось — только чтение.

---

## 1. Полная карта экранов

Приложение **не имеет ни одного `Stage` из `Application.launch`** как самостоятельного экрана: `ProjectFXApp.start()`
вообще не показывает переданный `stage`. Единственное окно верхнего уровня создаётся вручную внутри
`ProjectEditFXController.editProject()`. Все окна — **`Modality.WINDOW_MODAL`** или `APPLICATION_MODAL`,
открываются через `FXMLLoader.load(...).getResource("<name>-view.fxml")` + `showAndWait()`.

### Дерево экранов (кто кого открывает)

```
ProjectFXApp.start(stage, hostServices)                    [apps/ProjectFXApp.kt:17]
│
├─ initializeH2db()                                        [H2db.kt:292]  — создаёт/настраивает служебную БД h2db
│
└─ ProjectEditFXController().editProject(null, hostServices)
   │                                                        ← ГЛАВНОЕ ОКНО, project-edit-view.fxml
   │                                                          заголовок: "Проект: <name>" или "Откройте или создайте проект."
   │
   ├─[Project ▸ New]        doMenuNewProject()  → ProjectController.create(), initialize()
   ├─[Project ▸ Open...]    doMenuOpen()        → ProjectSelectFXController.getProject()
   │                                             └── project-select-view.fxml ("Выбор проекта.")
   ├─[Project ▸ Delete]     doMenuDeleteProject()→ Alert + ProjectController.delete()
   ├─[Project ▸ Close]      doMenuExit()         → mainStage.close()
   │
   ├─[Database ▸ <имя БД>]  doSelectDatabase()   → DatabaseSelectFXController.getDatabase()
   │                                             └── database-select-view.fxml ("Выбор базы данных")
   │                                                 ├──[Редактировать базу данных] → DatabaseEditFXController.editH2database()
   │                                                 │                                └── database-edit-view.fxml
   │                                                 │                                    ("Редактирование базы данных" / "Добавление базы данных")
   │                                                 ├──[Добавить новую базу данных] → DatabaseEditFXController.editH2database(H2database())
   │                                                 ├──[Удалить выбранную базу данных]
   │                                                 ├──[OK] / [Отмена]
   │                                                 └── двойной клик по строке = OK
   │
   ├─[Actions ▸ Project Actions...]  doMenuProjectActions() → ProjectActionsFXController.actionsProject(project, listFilesExt, hostServices)
   │                                                 └── project-actions-view.fxml  (Modality.NONE — можно работать параллельно)
   │                                                     таблица файлов + 15 чекбоксов операций + [Do actions]
   │                                                     + [Train face model], pb1/pb2 + lblPb1/lblPb2
   │
   ├─[Actions ▸ Edit shots...]      doMenuEditShots()  → ShotsEditFXController.editShots(FileExt, hostServices)
   │                                                 └── shots-edit-view.fxml  "Редактор планов. Файл: <file.name>"
   │                                                     ┌ вкладка "Frames"     → paneFrames + tblPagesFrames
   │                                                     ├ вкладка "Persons"    → tblPersonsAllForFile + paneFaces + tblPagesFaces
   │                                                     ├ вкладка "Scenes"     → tblScenes + tblShotsForScenes + tblPersonsAllForScenes + tblSceneProperties
   │                                                     └ вкладка "Events"     → tblEvents + tblShotsForEvents + tblPersonsAllForEvents + tblEventProperties
   │                                                     │
   │                                                     ├─[ПКМ на миниатюре кадра / на lblFrameFull → "Edit frame faces"]
   │                                                     │     → FrameFacesEditFXController.editFrame(frameExt)   [staticmethod, companion object]
   │                                                     │        └── frame-faces-edit-view.fxml  (APPLICATION_MODAL)
   │                                                     │            tblFaces (Face | Person | M) + lblFrame (1920×1080) + [Create new face] + [ОК]
   │                                                     │            └──[Create new face] → PersonSelectFXController.getPersonExt()
   │                                                     │                                   └── person-select-view.fxml (APPLICATION_MODAL)
   │                                                     │                                          fldFind + tblPersons(PERSON) + [➕][✖] + [OK][Отмена]
   │                                                     │                                          └──[➕ doPersonAdd] → PersonEditFXController.editPerson()
   │                                                     │                                                                    └── person-edit-view.fxml
   │                                                     │
   │                                                     ├─[двойной клик по строке в tblPersonsAllForFile / tblPersonsAllForShot]
   │                                                     │     → PersonEditFXController.editPerson(projectExt, personExt, hostServices)
   │                                                     │        └── person-edit-view.fxml
   │                                                     │
   │                                                     └─[ПКМ на миниатюре лица → "CREATE NEW PERSON"] → PersonEditFXController.editPerson()
   │
   ├─[Actions ▸ Edit filters...]    doMenuEditFilters()  → FilterEditFXController.editFilters(projectExt, hostServices)
   │                                                 └── filter-edit-view.fxml
   │                                                     3 уровня: tblFilters → tblFiltersGroups → tblFiltersConditions
   │                                                     + tblFiles (мультивыбор) + btnFilter [>>] + tblShots
   │                                                     + [Create Video File] / [Create Video File for all ended persons]
   │                                                     + pb/lblPb
   │                                                     └──[➕ btnFilterConditionAdd] / двойной клик по условию
   │                                                           → FilterConditionCreateFXController.createFilterCondition()
   │                                                              └── filter-condition-create-view.fxml ("Create new filter condition" / "Edit filter condition")
   │                                                                  └──[btnSelectObject "Select Person"] → PersonSelectFXController.getPersonExt()
   │
   └─[Actions ▸ Edit persons...]    doMenuEditPersons()  → PersonEditFXController.editPerson(projectExt, null, hostServices)
                                                     └── person-edit-view.fxml
```

**Ключевые особенности навигации**

* Единственная точка входа — `ProjectEditFXController`; остальные экраны модальны поверх него.
* `ProjectActionsFXController` — единственное окно с `Modality.NONE` (форма вроде утилиты/мастера).
* `PersonSelectFXController` и `FrameFacesEditFXController` используют `Modality.APPLICATION_MODAL` (блокируют всё приложение).
* Обратной навигации нет: из любого дочернего окна пользователь возвращается только кнопками OK/Отмена/крестик.
* Ни в одном контроллере нет нижнего меню-бара — только `MenuBar` в `project-edit-view.fxml`.
* Переходы между таблицами внутри одного окна (`tblShots` ↔ `tblPagesFrames` ↔ `tblScenes` ↔ `tblEvents`) реализованы через «умный скролл» — взаимная подсветка выделения, чтобы оператор не терялся. Реализовано через рефлексию в `TableViewSkin.children[1]` (`VirtualFlow`) и сравнение `firstVisibleCell`/`lastVisibleCell` с `selectedIndex` — см. `tblShotsSmartScroll()`, `tblPagesFramesSmartScroll()` и т.п.

---

## 2. Поэкранный разбор FXML-контроллеров

### 2.1 `project-edit-view.fxml` — `ProjectEditFXController` (2017 строк, `@Transactional`)

FXML: 771 строка. Класс аннотирован `@org.springframework.transaction.annotation.Transactional`.

**MenuBar** (три меню)

| fx:id | MenuItem | text | onAction |
|---|---|---|---|
| `menuProject` | `menuNewProject` | `New` | `#doMenuNewProject` |
| | `menuOpenProject` | `Open...` | `#doMenuOpen` |
| | `menuDeleteProject` | `Delete` | `#doMenuDeleteProject` |
| | `menuExit` | `Close` | `#doMenuExit` |
| `menuActions` | `menuProjectActions` | `Project Actions...` | `#doMenuProjectActions` |
| | `menuEditShots` | `Edit shots...` | `#doMenuEditShots` |
| | `menuEditFilters` | `Edit filters...` | `#doMenuEditFilters` |
| | `menuEditPersons` | `Edit persons...` | `#doMenuEditPersons` |
| `menuDatabase` | `menuSelectDatabase` | `Выбрать базу данных` | `#doSelectDatabase` |

Текст `menuDatabase` подменяется в `initialize()` на имя текущей БД (`menuDatabase?.text = getCurrentDatabase()?.name`), т.е. это одновременно индикатор и вход в выбор БД.

**`paneMain` → `SplitPane` (divider 0.441)** — левая половина, панель проекта

*Поля проекта (все сохраняются по потере фокуса или на закрытии окна — `saveCurrentProject()`):*

| fx:id | Label | Тип |
|---|---|---|
| `fldProjectName` | `Name:` | TextField |
| `fldProjectShortName` | `Short:` | TextField |
| `fldProjectFolder` | `Folder:` | TextField + `btnSelectProjectFolder` `[...]` → `DirectoryChooser` |
| `fldProjectWidth` | `W (px):` | TextField → `project.width` (Int) |
| `fldProjectHeight` | `H (px):` | TextField → `project.height` (Int) |
| `fldProjectFps` | `FPS:` | TextField → `project.fps` (Double) |
| `fldProjectVideoBitrate` | `Bitrate:` (Video) | TextField |
| `cbProjectVideoCodec` | `Codec:` (Video) | ComboBox ← `VideoCodecs.values()` |
| `fldProjectAudioBitrate` | `Bitrate:` (Audio) | TextField |
| `fldProjectAudioFrequency` | `Frequncy:` [опечатка в FXML] | TextField |
| `cbProjectAudioCodec` | `Codec:` (Audio) | ComboBox ← `AudioCodecs.values()` |
| `cbProjectContainer` | `Container:` | ComboBox ← `VideoContainers.values()` |
| `cbProjectLosslessCodec` | `Lossless Codec:` | ComboBox ← `LosslessVideoCodecs.values()` |
| `cbProjectLosslessContainer` | `Lossless Container:` | ComboBox ← `LosslessContainers.values()` |

*Таблица файлов проекта:*

| Элемент | Значение |
|---|---|
| `tblFiles` | `TableView<FileExt>`, style `-fx-selection-bar: red` (красная подсветка выделения — фирменный приём проекта) |
| `colFileOrder` | `#`, `PropertyValueFactory("fileOrder")` |
| `colFileName` | `Файл`, `PropertyValueFactory("fileName")` |
| `btnFileMoveToFirst` `btnFileMoveUp` `btnFileMoveDown` `btnFileMoveToLast` | `⟰ ⇧ ⇩ ⟱`, тултипы `В начало списка` / `На один уровень вверх` / `На один уровень вниз` / `В конец списка` → `FileController.reOrder(ReorderTypes,…)` |
| `btnFileAdd` | `➕` тултип `Добавить файл` → `FileChooser` («Добавить файл к проекту») → `FileController.create()`; если файл уже есть — просто выделяет его |
| `btnFileAddFilesFromFolder` | `📂` тултип `Добавить файлы из папки` → `DirectoryChooser` → `FileController.create()` для каждого файла |
| `btnFileDelete` | `✖` тултип `Удалить файл из проекта` → Alert → `FileController.delete()` |
| `pbFiles` / `lblPbFiles` | Прогресс загрузки списка файлов (`LoadListFilesExt`) |

*4 таблицы свойств (каждая: TableView + 6 кнопок ⟰⇧⇩⟱➕✖ + TextField key + TextArea value):*

| Назначение | TableView | Колонки | Поля |
|---|---|---|---|
| Project Properties | `tblProjectProperties` | `colProjectPropertyKey`(`key`), `colProjectPropertyValue`(`value`) | `fldProjectPropertyKey`, `fldProjectPropertyValue` |
| Computer-Depened-Properties проекта [опечатка в FXML] | `tblProjectPropertiesCdf` | `colProjectPropertyCdfKey`, `colProjectPropertyCdfValue` | `fldProjectPropertyCdfKey`, `fldProjectPropertyCdfValue` + `btnBrowseProjectPropertyCdfValue` `📂` |
| File Properties | `tblFileProperties` | `colFilePropertyKey`, `colFilePropertyValue` | `fldFilePropertyKey`, `fldFilePropertyValue` |
| Computer-Depened-Properties файла | `tblFilePropertiesCdf` | `colFilePropertyCdfKey`, `colFilePropertyCdfValue` | `fldFilePropertyCdfKey`, `fldFilePropertyCdfValue` + `btnBrowseFilePropertyCdfValue` `📂` |

Тултипы у всех `➕` ошибочно скопированы из шаблона файлов: `Добавить файл`; у `✖` — `Удалить файл из проекта` (даже для свойств).
`➕` открывает `ContextMenu` (не диалог!): пункт `Добавить новое свойство проекта/файла` (с Alert-подтверждением «Имя и значение свойства будут сгенерированы автоматически.») + подменю по всем ключам из `PropertyController.getMapKeyValuesByParentClass()` / `getKeys()` + пункт `Добавить все свойства для …`.
Ключ CDF-свойства блокируется к редактированию, если он служебный: `fldProjectPropertyCdfKey?.isDisable = … || Folders.values().any { it.propertyCdfKey == … }`.
Колонка Value во всех 5 таблицах имеет кастомный `cellFactory` с `Text` + `wrappingWidthProperty().bind(col.widthProperty())` — перенос по словам и авто-высота строки.

**`paneFile` — правая половина SplitPane**

| fx:id | Label |
|---|---|
| `fldFileName` | `Name:` |
| `fldFileShortName` | `Short name:` (при изменении `resetFieldsLinkedShortName()` — сброс всех папок, производных от shortName) |
| `fldFilePath` | `Path:` + `btnSelectFilePath` `[...]` → `FileChooser` («Выберите файл») + автоматически `TrackController.createTracksFromMediaInfo()` |
| `btnGetFileTracksFromMediaInfo` | `Get tracks from MediaInfo` |
| `tblTracks` | `TableView<Track>`: `colTrackUse`(`Use`, Boolean), `colTrackType`(`Type`, String). **Двойной клик** по строке (кроме `General`/`Video`) переключает `track.use` → `TrackController.save()` — т.е. оператор выбирает, какие дорожки попадут в результат |
| `tblTrackProperties` | `TableView<Property>`: `colTrackPropertyKey`(`Key`), `colTrackPropertyValue`(`Value`) |

Двойной клик по свойству с ключом `url_*` открывает ссылку через `hostServices.showDocument(...)`; по CDF-свойству с ключом `folder_*` — открывает папку (или вычисленную `FileController.getCdfFolder(...)`).

Placeholder-тексты (показываются, когда таблица пуста):
`"Project not selected or don't have any files."`, `"Project not selected or don't have any properties."`, `"File not selected or don't have any properties."`, `"Project not selected or don't have any computer-depended properties."`, `"File not selected or don't have any computer-depended properties."`, `"File not selected or don't have any tracks."`, `"Track not selected or don't have any properties.."`.

Автосохранение при закрытии окна (`setOnCloseRequest`): `saveCurrentFileProperty()`, `saveCurrentFilePropertyCdf()`, `saveCurrentFile()`, `saveCurrentProject()` + `interrupt()` фонового потока загрузки файлов.

---

### 2.2 `project-select-view.fxml` — `ProjectSelectFXController`

* `lblDb` — `Label`, живой текст `"БД: ${getCurrentDatabase()?.name}"`, жирный 10pt.
* `tblProjects` (`TableView<Project>`): `colOrder` (`#`, `order`), `colName` (`Проект`, `name`).
* 4 кнопки `⟰ ⇧ ⇩ ⟱` (`btnMoveToFirst`, `btnMoveUp`, `btnMoveDown`, `btnMoveToLast`) → `doMove(ReorderTypes.*)` → `ProjectController.reOrder()`. Все 4 заблокированы, пока не выбрана строка.
* `btnOk` `OK`, `btnCancel` `Отмена` (возвращает `incomingProject`).
* Двойной клик по строке = OK.
* Заголовок окна: `"Выбор проекта."`

---

### 2.3 `project-actions-view.fxml` — `ProjectActionsFXController` (458 строк)

См. раздел 4 (полный список операций).

---

### 2.4 `shots-edit-view.fxml` — `ShotsEditFXController` (3179 строк, `@Transactional`)

FXML 533 строки. Размер окна `prefWidth=2120, prefHeight=1200`. Корневой `HBox` из трёх частей, разделённых вертикальными `Separator`.

**Левая часть (VBox, maxWidth 730) — постоянно видимая**

| Элемент | Описание |
|---|---|
| `tblShots` (`TableView<ShotExt>`, MULTIPLE, placeholder = `ProgressIndicator(-1)`) | `colShotFrom`(`FROM`)←`labelFirst1` (Label с картинкой!), `colShotTo`(`TO`)←`labelLast1`, `colShotType`(`TYPE`)←`labelType` (Label с пиктограммой типа), `colButtonGetType` (без текста) ← `buttonGetType` (Button) |
| `pbShots` | Прогресс `LoadListShotsExt` |
| `tblShotProperties` | Label `Shot properties`; `colShotPropertyKey`(`Key`), `colShotPropertyValue`(`Value`); кнопки `btnShotPropertyMoveToFirst/MoveUp/MoveDown/MoveToLast/Add/Delete`; `fldShotPropertyKey` (TextField), `fldShotPropertyValue` (TextArea) |
| `tblPersonsAllForShot` | `colTblPersonsAllForShotName`(`Name`)←`labelSmall` — **персоны, попавшие в выбранный шот**; `pbPersonsForShot` |
| Радиокнопки `rbPersonAll`(`All`) / `rbPersonFile`(`File`, selected) | `grpPersons` — область персон для фрейма: все по проекту или только по файлу |
| Радиокнопки `rbFaceAll`(`All`) / `rbFaceFile`(`File`, selected) | `grpFaces` — область лиц |
| Чекбоксы `cbFacesNotExample`(`Not example`), `cbFacesExample`(`Example`), `cbFacesNotManual`(`Not manual`), `cbFacesManual`(`Manual`) | Все 4 по умолчанию `selected="true"` — фильтр типов лиц |
| `lblFrameFull` | `Label` 720×400, `style="-fx-background-color: black;"`, имеет пустой `ContextMenu` (`contextMenuFrameFull`), который **полностью переопределяется в коде**: туда добавляется единственный пункт `MenuItem("Edit frame faces")` |
| `btnOK` | `OK` → `doOK()` → `mainStage.close()` |

**Правая часть (VBox, minWidth 920) — `TabPane` с 4 вкладками**

**Вкладка `Frames`**
* `paneFrames` (`Pane`, `style="-fx-background-color: black;"`) — холст матрицы миниатюр кадров. Размер холста отслеживается через `widthProperty()/heightProperty()` → `listenToChangePaneSize()` переразбивает матрицу.
* `tblPagesFrames` (`TableView<MatrixPageFrames>`) — список «страниц» кадров; `colDurationStart`(`Время: с`)←`start`, `colDurationEnd`(`Время: по`)←`end`, `colFrameStart`(`Кадры: с`)←`firstFrameNumber`, `colFrameEnd`(`Кадры: по`)←`lastFrameNumber`.
* `pbPagesFrames`.

**Вкладка `Persons`**
* `tblPersonsAllForFile` — `colTblPersonsAllForFileName`(`Name`)←`labelSmall`; `pbPersonsForFile`. Таблица принимает drag&drop (`dragboard.string == "labelFace"`).
* `paneFaces` (`Pane`, чёрный фон) — холст матрицы миниатюр лиц; `pbFaces`.
* `tblPagesFaces` (`TableView<MatrixPageFaces>`) — `colTblPagesFacesNumber`(`#`)←`pageNumber`.

**Вкладка `Scenes`**
* `tblScenes` (MULTIPLE): `colSceneName`(`NAME`)←`sceneNameLabel`, `colSceneFrom`(`FROM`)←`labelFirst1`, `colSceneTo`(`TO`)←`labelLast1`; `pbScenes`.
* `btnCreateNewSceneBySelectedShots` → `Create new scene by selected shots`
* `btnDeleteSelectedScenes` → `Delete selected scenes` (**метод `doDeleteSelectedScenes()` пустой — кнопка не работает**)
* `btnCreateEventBasedScene` → `Create Event Based Scene` (**по коду создаёт Event из сцены** `EventController.createEventExt(currentSceneExt)`, `doCreateEventBasedScene()` — явный баг именования)
* `tblSceneProperties` + `colScenePropertyKey/Value` + 6 кнопок `btnSceneProperty*` + `fldScenePropertyKey`/`fldScenePropertyValue`; Label `Scene properties`
* `tblShotsForScenes`: `colShotForSceneFrom`(`FROM`)←`labelFirst2`, `colShotForSceneTo`(`TO`)←`labelLast2`; `pbShotsForScenes`
* `tblPersonsAllForScenes`: `colTblPersonsAllForSceneName`(`Name`)←`labelSmall`; `pbPersonsForScenes`

**Вкладка `Events`** — полный аналог Scenes:
* `tblEvents` (MULTIPLE): `colEventName`(`NAME`)←`eventNameLabel`, `colEventFrom`(`FROM`)←`labelFirst1`, `colEventTo`(`TO`)←`labelLast1`; `pbEvents`
* `btnCreateNewEventBySelectedShots` → `Create new event by selected shots`
* `btnDeleteSelectedEvents` → `Delete selected events` (реализован)
* `tblEventProperties` + `colEventPropertyKey/Value` + 6 кнопок `btnEventProperty*` + `fldEventPropertyKey`/`fldEventPropertyValue`; Label `Event properties`
* `tblShotsForEvents`: `colShotForEventFrom`(`FROM`)←`labelFirst3`, `colShotForEventTo`(`TO`)←`labelLast3` (**FXML-id прогресса ошибочно `pbShotsForScenes1`, а в Kotlin поле называется `pbShotsForEvents` → это поле не инжектится, всегда `null`; аналогично `pbPersonsForScenes1` vs `pbPersonsForEvents`**)
* `tblPersonsAllForEvents`: `colTblPersonsAllForEventName`(`Name`)←`labelSmall`

**Подвал окна**: `pb` (`ProgressBar`) + `lblPb` (`Label`) — общий индикатор.

Заголовок окна: `"Редактор планов. Файл: ${currentFileExt!!.file.name}"`.
При закрытии — `clearOnExit()`: очистка `shotsExt/scenesExt/eventsExt/framesExt` + `System.gc()`.

---

### 2.5 `frame-faces-edit-view.fxml` — `FrameFacesEditFXController` (239 строк)

Окно 2200×1200. Метод входа — **`companion object fun editFrame(frameExt: FrameExt)`** (не экземпляр!).

* `tblFaces` (`TableView<FaceExt>`): `colFace`(`Face`, 75px)←`labelSmall` (**изображение лица 75×75**), `colPerson`(`Person`, 135px)←`labelPersonSmall` (**изображение персоны, а не текст!**), `colIsManual`(`M`, 20px)←`isManualText` → строка `"✓"` если `face.isManual`, иначе `""`.
* `lblFrame` — `Label` 1920×1080, `style="-fx-background-color: black;"`, `alignment = Pos.CENTER`. Показывает полный кадр с наложенными прямоугольниками лиц (`FaceController.getOverlayedFrame(currentFrameExt, null, true)`).
* `btnCreateNewFace` → `Create new face` (англ. в русском UI — несогласованность)
* `btnOk` → `ОК`

Мышь: `onMousePressed` запоминает `startX/startY` и заново строит оверлей; `onMouseDragged` рисует синий (`Color.BLUE`) прямоугольник по `drawRect`; `onMouseReleased` → автоматически `onCreateNewFace(null)`. То есть оператор **просто рисует мышью прямоугольник вокруг лица** — отдельная кнопка «создать» не обязательна (хотя есть).

---

### 2.6 `person-select-view.fxml` — `PersonSelectFXController` (225 строк)

Узкая панель 210×1000, `APPLICATION_MODAL`.
* `fldFind` — поиск без `promptText` (пустое TextField, без подсказки)
* `tblPersons` (`TableView<PersonExt>`): `colPersonName`(`PERSON`)←`labelSmall` (**изображение 135×75 с подписью-подчёркиванием имени**)
* `btnPersonAdd` `➕` тултип `Добавить файл` (текст/тултип скопированы из экрана файлов) → `doPersonAdd()`: `PersonController.create(project)` + `PersonEditFXController().editPerson(...)` + перезагрузка списка
* `btnPersonDelete` `✖` тултип `Удалить файл из проекта` → **`doPersonDelete()` пустой**
* `btnOk` `OK` → добавляет персону в MRU-список `listLastSelectedPersons` (макс. 11) — «недавние»
* `btnCancel` `Отмена` → возвращает `null`
* Enter в `fldFind` → фокус на таблицу и выбор первой строки; Enter в таблице → фокус на `btnOk`; Enter на `btnOk` → `doOk`
* Двойной клик по строке → `doOk`
* Поиск живой: `fldFind.onKeyReleased` → `FilteredList` + `SortedList`, сравнение `personExt.person.name.lowercase().contains(filter)`, автовыбор первого результата.

---

### 2.7 `person-edit-view.fxml` — `PersonEditFXController` (540 строк)

Окно 730×900 (в FXML `maxWidth=730, minWidth=950` — противоречие, minWidth сильнее).

* `fldFind` + `tblPersons` (`colPersonName`, `PERSON`) + `btnPersonAdd` `➕` (**метод `doPersonAdd()` пустой**) + `btnPersonDelete` `✖` (**пустой**)
* `lblMediumPreview` — `Label` 720×400, `contentDisplay="GRAPHIC_ONLY"`, `style="-fx-background-color: black;"`, `textFill="#ddff00"` → `currentPersonExt.labelMedium` (крупное фото персоны 720×400)
* `fldName` + Label `Name:` — **редактируемое имя персоны**, сохраняется по `doOk()`/закрытию окна
* Label `Properties`, `tblProperties`: `colPropertyKey`(`Key`), `colPropertyValue`(`Value`) + 6 кнопок `btnPropertyMoveToFirst/MoveUp/MoveDown/MoveToLast/Add/Delete` + `fldPropertyKey` + `fldPropertyValue` (TextArea)
* `btnTagAdd` / `btnTagDelete` — **объявлены в FXML? Нет.** В контроллере есть `@FXML btnTagAdd/btnTagDelete`, но в FXML отсутствуют → всегда `null`. Мёртвый код.
* `btnOk` `OK`

Особенность: при смене выбранной персоны сначала `saveCurrentProperty()` + `saveCurrentPerson()`, затем перезагрузка свойств. Двойной клик по свойству с ключом `url_*` → `hostServices.showDocument(value)`.

---

### 2.8 `filter-edit-view.fxml` — `FilterEditFXController` (667 строк)

Окно 1920×1080. Трёхуровневая структура «Фильтр → Группа → Условие».

**Уровень 1 — Фильтры** (`tblFilters`): `colFilterOrder`(`#`)←`order`, `colFilterName`(`Filter`)←`name`, `colFilterIsAnd`(`&|`)←`andText`.
Под таблицей: `fldFilterName` (TextField, живое сохранение в `textProperty().addListener`), `rbFilterIsAnd`(`AND`) / `rbFilterIsOr`(`OR`) в `tgFilter`.
Кнопки: `btnFilterMoveToFirst/MoveUp/MoveDown/MoveToLast` (`⟰⇧⇩⟱`), `Separator`, `btnFilterAdd` `➕`, `btnFilterDelete` `✖`.

**Уровень 2 — Группы** (`tblFiltersGroups`): `colFilterGroupOrder`(`#`), `colFilterGroupName`(`Group`)←`name`, `colFilterGroupIsAnd`(`&|`)←`andText`; `fldFilterGroupName`, `rbFilterGroupIsAnd`(`AND`)/`rbFilterGroupIsOr`(`OR`) в `tgFilterGroup`; 6 кнопок `btnFilterGroup*`.

**Уровень 3 — Условия** (`tblFiltersConditions`): `colFilterConditionOrder`(`#`), `colFilterConditionName`(`Condition`)←`name`; 6 кнопок `btnFilterCondition*`.

Все три колонки Name имеют cellFactory с переносом по словам.

**Правая часть:**
* `tblFiles` (`TableView<FileExt>`, MULTIPLE): `colFileOrder`(`#`)←`fileOrder`, `colFileName`(`Файл`)←`fileName`
* `btnFilter` → `>>` — «применить фильтр»: `ShotTmpCdfController.deleteAll()` + `Main.shotTmpCdfRepo.addAllByFileId(ccid, fileId)` для выбранных файлов + `FilterController.getFilterExt(project, filterId).shotsIds()` + `ShotController.convertSetShotsIdsToListShotsExt(...)` → `tblShots`
* `tblShots` (`TableView<ShotExt>`): `colShotFileName`(`FILE`)←`fileName`, `colShotFrom`(`FROM`)←`labelFirst1` (картинка!), `colShotTo`(`TO`)←`labelLast1` (картинка!)
* `btnCreateVideo` → `Create Video File` → `CreateFilterResult(filterExt, projectExt, shotsExt, fileExt).run()`
* `btnCreateVideoForAllPersons` → `Create Video File for all ended persons`
* `pb` + `lblPb` (прогресс `LoadListFilesExt`)

---

### 2.9 `filter-condition-create-view.fxml` — `FilterConditionCreateFXController` (327 строк)

Панель 200×340, узкая и текстовая.

```
Label  lblHeader        "Create new filter condition"  (System Bold 12) / при редактировании "Edit filter condition"
── объект (ToggleGroup tgObject) ──
RadioButton rbPerson           "Person"            selected=true
RadioButton rbPersonProperty   "Person property"
RadioButton rbShotProperty     "Shot property"
RadioButton rbSceneProperty    "Scene property"
RadioButton rbEventProperty    "Event property"
Button      btnSelectObject    "Select Person"   (текст меняется по типу объекта)
── Separator ──
── включённость (ToggleGroup tgIncluded) ──
RadioButton rbIsIncluded       "is included"      selected=true
RadioButton rbIsNotIncluded    "is NOT included"
Label                          "in"
── субъект (ToggleGroup tgSubjectClass) ──
RadioButton rbShot             "Shot"             selected=true
RadioButton rbScene            "Scene"
RadioButton rbEvent            "Event"
Label    lblName               "NAME"  (RED, System Bold 12, wrapText, minHeight 50) — живая формулировка
Button   btnOk                 "Create new filter condition" / "Edit filter condition"
Button   btnCancel             "Cancel"
```

Логика ограничений: если выбрано `Scene property` — `rbShot` блокируется и автоматически выбирается `rbScene`; если `Event property` — блокируется `rbScene`, выбирается `rbEvent`.
`btnOk.isDisable = currentObjectId == null` — пока объект не выбран, создать условие нельзя.
Живая формулировка (`getCurrentName()`), например:
`Person «Иван» is included in Shot`, `Person property «Пол» = «мужской» is NOT included in Scene`.

Загрузка FXML в этом контроллере идёт через `ShotsEditFXController::class.java.getResource(...)` — копипаста, работает из-за того же пакета.

---

### 2.10 `database-select-view.fxml` — `DatabaseSelectFXController` (140 строк)

Панель 300×400. `lblDb` отсутствует (в отличие от project-select).
* `tblDatabases` (`TableView<H2database>`): `colDbName`(`База данных`)←`name`
* `btnSelectDb` `OK`, `btnEditDb` `Редактировать базу данных`, `btnCreateNewDb` `Добавить новую базу данных`, `btnDeleteDb` `Удалить выбранную базу данных`, `btnCancel` `Отмена`
* Заголовок окна `"Выбор базы данных"`, модальность `WINDOW_MODAL`, двойной клик = OK
* `doCancel` возвращает `incomingDatabase` (откат)

### 2.11 `database-edit-view.fxml` — `DatabaseEditFXController` (99 строк)

Форма 400×246. 6 полей: `fldId` (Label `ID:`, **задизейблен**), `fldName` (`Name:`), `fldDriver` (`Driver:`), `fldUrl` (`Url:`), `fldUser` (`User:`), `fldPassword` (`Password:` — обычный `TextField`, не `PasswordField`).
Кнопки `btlOk` `OK` → `saveH2database()`, `btnCancel` `Cancel`.
Заголовок окна зависит от режима: `"Редактирование базы данных"` или `"Добавление базы данных"`.

---

## 3. Полный пользовательский сценарий (user journey)

### Шаг 0. Запуск

`main()` в `apps/ProjectFXApp.kt:29` → `Application.launch(ProjectFXApp::class.java)`.
`ProjectFXApp` помечен `@SpringBootApplication`, но **Spring Boot не запускается** — ни `SpringApplication.run`, ни `AnnotationConfigApplicationContext` в `start()`.
`start(stage, hostServices)`:
1. `initializeH2db()` — открывает фиксированную `jdbc:h2:./h2db` (`getH2Connection()`), создаёт `tbl_databases` и `tbl_properties`, если их нет, и назначает текущую рабочую БД (`CURRENTDB_ID`). При 0 баз создаёт embedded `«<Встроенная база данных>»`, driver `org.h2.Driver`, url `jdbc:h2:./ivfxdb`, user `sa`, пароль пустой.
2. `ProjectEditFXController().editProject(null, hostServices)` — если в БД есть проекты, берётся **первый** (`ProjectController.getListProjects().firstOrNull()`), иначе `currentProjectExt == null` → окно открывается с заголовком «Откройте или создайте проект.», `paneMain.isVisible = false`, пункты `Actions`/`Edit filters`/`Edit persons`/`Delete` — задизейблены.

Spring-контекст инициализируется лениво при первом обращении к любому `Main.*`: `companion object` класса `Main` — это Kotlin `val`-ы в статическом инициализаторе, `context = AnnotationConfigApplicationContext(SpringConfig::class.java)`, затем 17 `context.getBean("xxxRepo", …)` и `connection = getConnection()`.
`SpringConfig` — `@Configuration @ComponentScan("com.svoemesto.ivfx") @PropertySource("/application.properties")`, два бина: `dataSource(): DataSource` (DriverManagerDataSource, параметры берутся из `getCurrentDatabase()`) и `jdbcTemplate(): JdbcTemplate`.

⚠️ Связка Spring↔JavaFX минимальна: контроллеры создаются как обычные Kotlin-объекты (`ProjectEditFXController()`), а не как бины Spring; аннотации `@Transactional` на классах `ProjectEditFXController`/`ShotsEditFXController` не работают (нет Spring-прокси). Spring используется только как фабрика репозиториев.

### Шаг 1. Создание проекта

`Project ▸ New` → `doMenuNewProject()` → `ProjectController.create()` → `ProjectExt` → `initialize()` (полная переинициализация формы).
Далее оператор заполняет:
* `fldProjectName` — имя (попадает в заголовок окна);
* `fldProjectShortName` — короткое имя (используется как префикс имён папок и файлов: `${shortName}_frame_000123.jpg`, `${shortName}_shot_[…]`);
* `fldProjectFolder` + `btnSelectProjectFolder` — рабочая папка. **Важно:** без папки у всех файлов будет пустой `folderPreview/folderLossless/…`;
* технические параметры: `W (px)`, `H (px)`, `FPS`, video/audio bitrate, audio frequency, кодеки `VideoCodecs`/`AudioCodecs`, контейнер `VideoContainers`, lossless-кодек `LosslessVideoCodecs` и lossless-контейнер `LosslessContainers`.
Всё сохраняется автоматически: по потере фокуса `fldProjectName` и по закрытию окна (`saveCurrentProject()` → `ProjectCdfController.save(project.cdfs.first())` + `ProjectController.save(project)`).
Произвольные свойства проекта — через `btnProjectPropertyAdd` (`➕`): «Добавить новое свойство проекта», конкретный ключ из списка, либо «Добавить все свойства для проекта».

### Шаг 2. Добавление файлов

Три способа:
1. `btnFileAdd` (`➕`) → `FileChooser` → `FileController.create(project, absolutePath)`. Если файл уже в проекте — он просто выделяется (повторно не добавляется).
2. `btnFileAddFilesFromFolder` (`📂`) → `DirectoryChooser` → для каждого `listFiles()` файла, которого ещё нет, `FileController.create()`. Фильтра по расширению нет — берётся всё, включая не-видео.
3. Вставка/правка пути вручную: `fldFilePath` + `btnSelectFilePath`.

После добавления автоматически вызывается `TrackController.createTracksFromMediaInfo(file)` → заполняется `tblTracks`. Оператор может двойным кликом снять/поставить галочку `Use` у дорожек (кроме `General`/`Video`).

Порядок файлов задаётся кнопками `⟰ ⇧ ⇩ ⟱` → `FileController.reOrder()`, после чего `updateOrderListFiles()` перечитывает порядок из `Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder()`.

### Шаг 3. Запуск обработки — экран project-actions

Выделить файл(ы) в `tblFiles` → `Actions ▸ Project Actions...` → `project-actions-view.fxml`. Подробно — раздел 4.

### Шаг 4. Просмотр результатов

Выделить файл в `tblFiles` → `Actions ▸ Edit shots...` → `shots-edit-view.fxml`. Подробно — раздел 5.

### Шаг 5. Редактирование

**а) Границы шотов (планов)** — двойной клик по миниатюре кадра в `paneFrames`:
* если кадр `isFind` и **не** `isManualCancel` → `isManualCancel = true`, `isFinalFind = false`, `FrameController.save()`, `splitOrUnionShots()`;
* если кадр `isFind` и `isManualCancel` → отметка снимается, `isFinalFind = true`;
* если кадр **не** `isFind` и **не** `isManualAdd` → `isManualAdd = true`, `isFinalFind = true`;
* если `isManualAdd` → снимается, `isFinalFind = false`.

`splitOrUnionShots()` — ядро ручного монтажа:
* **split** (кадр внутри шота, `isFinalFind`): `shot.lastFrameNumber = frameNumber - 1`, `ShotController.save()`, `ShotController.getOrCreate(file, frameNumber, lastFrameNumber)` создаёт второй шот;
* **union** (кадр — начало шота, `!isFinalFind`): предыдущий шот продлевается до `shot.lastFrameNumber`, текущий шот удаляется через `ShotController.delete()`;
* после любого изменения матрица и списки перестраиваются заново.

**б) Тип шота** — кнопка `colButtonGetType` в `tblShots` → `onActionButtonGetShotType(shotExt)` → `ContextMenu` из значений `ShotTypePerson` (`NONE`, `SGN`, `OTS`, `TWO`, `GRP`, `MASS`), каждое — `MenuItem(null, imageView)` с пиктограммой `shot_type_person_*.png`. Выбор → `shot.typePerson = …` → `ShotController.save()` → `resetPreview()` → `tblShots.refresh()`. Значок типа виден в колонке `TYPE`.

**в) Свойства шота** — `tblShotProperties` (Key/Value), `fldShotPropertyKey`/`fldShotPropertyValue`. Сохранение по потере фокуса (`PropertyController.save()`). Добавление: `btnShotPropertyAdd` (`➕`) → меню «Добавить новое свойство плана» / конкретные ключи из `PropertyController.getMapKeyValuesByParentClass(Shot::class.java.simpleName)`. Удаление — `✖` с Alert.

**г) Персоны и лица** — вкладка `Persons`:
* выбрать персону в `tblPersonsAllForFile` → `doSelectFacesCb(null)` перезагружает `paneFaces`;
* фильтры: `rbFaceFile`/`rbFaceAll`, `cbFacesNotExample`/`cbFacesExample`/`cbFacesNotManual`/`cbFacesManual`;
* миниатюра лица: ЛКМ — выбрать (Ctrl — добавить к выделению, Shift — выделить диапазон), ПКМ → контекстное меню:

| Пункт меню | Действие |
|---|---|
| `UNDEFINDED` | все выделенные лица → персона `PersonType.UNDEFINDED`; `face.personRecognizedName = ""` |
| `NONPERSON` | → персона `PersonType.NONPERSON` |
| `EXTRAS` | → персона `PersonType.EXTRAS` |
| `SELECT PERSON` | → `PersonSelectFXController.getPersonExt()` |
| `CREATE NEW PERSON` | `TextInputDialog("New person")` (title `Create new person`, header `Enter person name:`, content `Name:`) → `PersonController.create(project, name, PersonType.PERSON, "", fileId, frameNumber, faceNumberInFrame)` → сразу открывается `PersonEditFXController.editPerson()` |
| `Set as person picture` | удаляет `<uuid>.small.jpg`/`<uuid>.medium.jpg`, ставит `person.fileIdForPreview/frameNumberForPreview/faceNumberForPreview`, `PersonController.save()` |
| `Set as EXAMPLE` | `face.isExample = true`, превью перегенерируется, зелёный треугольник `OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.GREEN, 1.0F)` |
| `Remove from EXAMPLE` | обратно |

Drag&drop: перетаскивание миниатюры лица на строку `tblPersonsAllForFile` (rowFactory + `hoverProperty`) присваивает персону всем перетащенным лицам; если персоны ещё нет в списке файла — она добавляется.

Создание нового лица вручную: ПКМ на кадре (миниатюре или `lblFrameFull`) → `Edit frame faces` → в `frame-faces-edit-view.fxml` нарисовать прямоугольник мышью → отпустить → `PersonSelectFXController` выбрать персону (или `➕` создать новую) → `Face` сохраняется с `isManual = true`, `faceNumberInFrame = list.size + 1`, превью 75×75 с **красным** треугольником.

**д) Сцены** — вкладка `Scenes`:
1. Выделить **непрерывный** набор шотов в `tblShots` (проверяется `shotExt.shot.lastFrameNumber + 1 == next.firstFrameNumber`, при разрыве — `break`).
2. `Create new scene by selected shots` → `SceneController.createSceneExt(listShotsExt)` → `TextInputDialog` (title `Rename scene`, header `Enter new scene name:`, content `Name:`) → `SceneController.save()`.
3. `tblScenes` показывает `NAME/FROM/TO` (картинки с подписями времени).
4. Множественный выбор сцен → в `tblShotsForScenes` объединённый список шотов всех выбранных сцен, в `tblPersonsAllForScenes` — объединённый список персон.
5. `Create Event Based Scene` — создаёт **Event** из выбранной сцены.
6. `Delete selected scenes` — кнопка есть, обработчик пуст.
7. Свойства сцены — `tblSceneProperties` + `btnScenePropertyAdd` («Добавить новое свойство сцены»).

**е) События** — вкладка `Events` — полный аналог, но `doDeleteSelectedEvents()` **реализован** (`EventController.delete()` для каждого выбранного + пересчёт превью затронутых шотов).

**ж) Персоны** — `Actions ▸ Edit persons...` (или двойной клик по персоне в `tblPersonsAllForFile` / `tblPersonsAllForShot`):
* `fldFind` — поиск; `tblPersons` — список;
* `lblMediumPreview` — крупное фото 720×400;
* `fldName` — редактирование имени;
* `tblProperties` + `fldPropertyKey`/`fldPropertyValue` — произвольные свойства персоны (аналогично шоту/сцене/событию);
* `btnOk` `OK` → `saveCurrentPerson()` → `PersonController.save()`.

### Шаг 6. Фильтры и экспорт

`Actions ▸ Edit filters...` → `filter-edit-view.fxml`:
1. `btnFilterAdd` (`➕`) → `FilterController.create(project)` → строка в `tblFilters`.
2. `fldFilterName` — имя (живое сохранение). `AND`/`OR` — как объединяются **группы** внутри фильтра (`doFilterIsAnd` → `FilterController.save`).
3. Выбрать фильтр → `btnFilterGroupAdd` (`➕`) → `FilterGroupController.create(filter)`. `fldFilterGroupName` — имя группы; `AND`/`OR` — как объединяются **условия** внутри группы.
4. Выбрать группу → `btnFilterConditionAdd` (`➕`) → `filter-condition-create-view.fxml` (см. 2.9). Двойной клик по существующему условию открывает его на редактирование (заголовок меняется на `Edit filter condition`).
5. Выбрать файлы в `tblFiles` (MULTIPLE) → `btnFilter` `>>` → вычисляются `shotsIds()` → `tblShots` показывает результат.
6. **Экспорт:** `Create Video File` → `CreateFilterResult(filterExt, projectExt, shotsExt, fileExt).run()` — выходной файл в `projectExt.folderFilters`.
   `Create Video File for all ended persons` — для каждой персоны сproperty `end`: создаётся/переиспользуется фильтр `AllEventsPerson`, имя файла `AllEventsPerson «<имя персоны>».<ext контейнера проекта>`; если файл уже существует — пропускается.
7. Дополнительный экспорт без интерфейса: `Actions ▸ Project Actions...` → чекбоксы `[SLA]`/`[SLN]`/`[SCA]`/`[CC]` вырезают видеофайлы шотов/конкатенацию; `Train face model` обучает распознаватель лиц (пишет `embeddings.json` и зовёт `train_model_json.py`).

---

## 4. Экран project-actions: полный список операций

`project-actions-view.fxml` (1280×720) → `ProjectActionsFXController`, вход `actionsProject(project, listFilesExt, hostServices)`, модальность `NONE`.

### 4.1 Таблица файлов `tblFilesExt` (MULTIPLE) — 17 колонок-индикаторов

| fx:id | text | `PropertyValueFactory` | Смысл |
|---|---|---|---|
| `colFileExtOrder` | `#` | `fileOrder` | порядок файла |
| `colFileExtName` | `Файл` | `fileName` | имя |
| `colFileExtPW` | `PW` | `hasPreviewString` | preview создан |
| `colFileExtLL` | `LL` | `hasLosslessString` | lossless-копия создана |
| `colFileExtFS` | `FS` | `hasFramesSmallString` | кадры 175×35 |
| `colFileExtFM` | `FM` | `hasFramesMediumString` | кадры 720×400 |
| `colFileExtFF` | `FF` | `hasFramesFullString` | кадры 1920×1080 |
| `colFileExtAF` | `AF` | `hasAnalyzedFramesString` | кадры проанализированы (обнаружены границы планов) |
| `colFileExtCS` | `CS` | `hasCreatedShotsString` | планы созданы |
| `colFileExtDF` | `DF` | `hasDetectedFacesString` | лица обнаружены |
| `colFileExtCF` | `CF` | `hasCreatedFacesString` | лица вырезаны в файлы |
| `colFileExtCFP` | `CFP` | `hasCreatedFacesPreviewString` | превью лиц созданы |
| `colFileExtRF` | `RF` | `hasRecognizedFacesString` | лица распознаны |
| `colFileExtSCA` | `SCA` | `hasShotsCompressedWithAudioString` | видео планов сжатое + звук |
| `colFileExtSLA` | `SLA` | `hasShotsLosslessWithAudioString` | видео планов lossless mxf + звук |
| `colFileExtSLN` | `SLN` | `hasShotsLosslessWithoutAudioString` | видео планов lossless mxf без звука |
| `colFileExtCC` | `CC` | `hasConcatString` | сконкатенированный файл |

Под таблицей: `btnTrainFaceModel` → `Train face model`.

### 4.2 Чекбоксы операций (правый VBox, в порядке FXML)

| fx:id | Текст в UI | Класс потока | Сообщение прогресса |
|---|---|---|---|
| `checkReCreateIfExists` | `RECREATE IF EXISTS` | — (глобальный переключатель) | — |
| `btnDoActions` | `Do actions` (Button) | — | — |
| `checkCreatePreview` | `[PW] Create preview` | `CreatePreview` | `Action: Create Preview` |
| `checkCreateLossless` | `[LL] Create lossless` | `CreateLossless` | `Action: Create Lossless` |
| `checkCreateFramesSmall` | `[FS] Create frames small` | `CreateFramesSmall` | `Create Frames (small size 175x35)` |
| `checkCreateFramesMedium` | `[FM] Create frames medium` | `CreateFramesMedium` | `Create Frames (medium size 720x400)` |
| `checkCreateFramesFull` | `[FF] Create frames full` | `CreateFramesFull` | `Create Frames (full size 1920x1080)` |
| `checkAnalyzeFrames` | `[AF] Analyze frames` | `AnalyzeFrames` | `Action: Analyze Frames` |
| `checkCreateShots` | `[CS] Create shots` | `CreateShots` | `Action: Create shots` |
| `checkDetectFaces` | `[DF] Detect faces` | `DetectFaces` | `Action: Detect Faces` |
| `checkCreateFaces` | `[CF] Create faces` | `CreateFaces` | `Action: Create Faces` |
| `checkCreateFacesPreview` | `[CFP] Create faces preview` | `CreateFacesPreview` | `Action: Create Faces Preview` |
| `checkRecognizeFaces` | `[RF] Recognize faces` | `RecognizeFaces` | `Action: Recognize Faces` |
| `checkCreateShotsCompressedWithAudio` | `[SCA] Create shots video files (compressed, with audio) - need LL!!!` | `CreateShotsCompressedWithAudio` | `Create Shots video files (compressed, with audio)` |
| `checkCreateShotsLosslessWithAudio` | `[SLA] Create shots video files (lossless mxf, with audio) - need LL!!!` | `CreateShotsLosslessWithAudio` | `Create Shots video files (lossless, with audio)` |
| `checkCreateShotsLosslessWithoutAudio` | `[SLN] Create shots video files (lossless mxf, without audio) - need LL!!!` | `CreateShotsLosslessWithoutAudio` | `Create Shots video files (lossless, without audio)` |
| `checkCreateConcat` | `[CC] Create concatinated video file - need SLA!!!` | `CreateConcat` | `Create concatinated video file` |

Все классы — `com.svoemesto.ivfx.threads.projectactions.*`. Зависимости от предусловий отражены прямо в тексте чекбоксов (`need LL!!!`, `need SLA!!!`) — но **программно не проверяются**: если `LL` не создан, `[SCA]/[SLA]/[SLN]` упадут.

### 4.3 Механика `doActions()`

1. Первый проход: для каждого выделенного файла и каждой отмеченной операции проверяется условие
   `флажок && (!fileExt.hasXxx!! || (fileExt.hasXxx!! && checkReCreateIfExists.isSelected))`
   и увеличивается `countActions`. Флаг «уже сделано» + `RECREATE IF EXISTS` сняты → операция пропускается (идемпотентность по умолчанию).
2. Второй проход: для каждой подходящей пары (файл, операция) создаётся поток
   `XxxAction(fileExt, tblFilesExt, "File: ${fileExt.file.name}, Action: …, Issue: [${counterPb1}/${countActions}]", counterPb1, countActions, lblPb1, pb1, lblPb2, pb2)`
   — передаются оба `ProgressBar` и оба `Label`, т.к. ffmpeg- и python-фазы внутри одного действия используют разные индикаторы.
3. `RunListThreads(listThreads).start()` — параллельный запуск всего списка.

### 4.4 Прогресс и обучение модели

`pb1`, `lblPb1`, `pb2`, `lblPb2` изначально `isVisible = false`.
`doTrainFaceModel()`: `FaceController.getListFacesToTrain(project)` → внутренний `class Embeddings(@SerializedName("embeddings") vectors, @SerializedName("names") tags)` → Gson → `<project.folder>/embeddings.json` → `RunCmd` запускает
`cd <FaceDetection.FACE_DETECTOR_PATH> && py train_model_json.py -e embeddings.json -r recognizer.pickle -l le.pickle`
(все слэши заменяются на обратные — Windows-only).

---

## 5. Как отображаются результаты

### 5.1 Константы размеров (`Main`)

```kotlin
FULL_FRAME_W = 1920.0 ; FULL_FRAME_H = 1080.0
MEDIUM_FRAME_W = 720.0 ; MEDIUM_FRAME_H = 400.0
PREVIEW_FRAME_W = 135.0 ; PREVIEW_FRAME_H = 75.0
PREVIEW_FACE_W  = 75.0  ; PREVIEW_FACE_H  = 75.0
PREVIEW_FACE_EXPAND_FACTOR = 1.4
PREVIEW_FACE_CROPPING = false
PREVIEW_PERSON_CROPPING = false
```

### 5.2 Матрица кадров (`MatrixPageFrames` / `MatrixFrame`) — вкладка «Frames»

**Что это.** `MatrixPageFrames` — «страница» (полный экран миниатюр). `MatrixFrame` — одна ячейка: `{frameExt, matrixPageFrames, column, row}`.

**Разбиение на страницы** (`MatrixPageFrames.createPages(listFramesExt, paneW, paneH, picW, picH)`):
```
countColumns = ((paneW  - ((picW+2)*2 + 20)) / (picW+2)).toInt()
countRows    = ((paneH  - ((picH+2)*2 + 20)) / (picH+2)).toInt()
```
Ключевая идея: `isFinalFind` (кадр — окончательная граница плана) **разрывает строку**, а не просто занимает ячейку. Если следующий кадр `isFinalFind`, `currentRow++` (перенос строки) вместо `currentColumn++`. При переходе на новую страницу крайние «половинки» плана дублируются: `omegaMatrixFrame` (кадр перед разрывом, с координатой последней ячейки предыдущей страницы + 1 по столбцу или +1 по строке) и `alfaMatrixFrame` (кадр после разрыва, с координатой `column=1,row=0` либо `column=0,row=1` — за левым/над верхним краем). Так **один и тот же план виден на стыке двух страниц** — оператор видит, что шот продолжается.
Колонки таблицы страниц (`tblPagesFrames`): `Время: с` (`convertDurationToString(getDurationByFrameNumber(...))`), `Время: по`, `Кадры: с` (`firstFrameNumber`), `Кадры: по` (`lastFrameNumber`).

**Отрисовка** (`showMatrixPageFrames()`, `ShotsEditFXController.kt:1961`). Каждый `MatrixFrame` — это переиспользуемый `Label` из `frameExt.labelSmall`, позиционированный вручную внутри `paneFrames`:
```kotlin
lbl.translateX = 10 + matrixFrame.column * (Main.PREVIEW_FRAME_W + 2)
lbl.translateY = 10 + matrixFrame.row    * (Main.PREVIEW_FRAME_H + 2)
lbl.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
```
Отступы: `heightPadding = 10`, `widthPadding = 10`, «+2» — зазор под рамку.

**Важно:** если кадр «интересный» (граница плана у него или у соседей, он I-кадр, или в кадре есть лица) — рисуется `frameExt.biSmall` (135×75 JPEG). Иначе `resultImage == null` → на `Label` **пусто** (Pane чёрный). Это и есть «визуализация только релевантных кадров» — экономия места.

**Оверлеи на миниатюрах кадра** (`utils/OverlayImage.kt`, порядок применения в коде):

| Условие | Вызов | Что видно |
|---|---|---|
| `framesWithFaces.contains(frameNumber)` | `setOverlayTriangle(bi, 4, 0.2, Color.BLUE, 1.0F)` | синий треугольник в правом верхнем углу — «в кадре есть лица» |
| `frame.isIFrame` | `setOverlayIFrame(bi)` | голубая буква **`I`** по центру, `Font.MONOSPACED BOLD 26`, opacity 0.9 |
| `frame.isFind` (начало плана) | `setOverlayFirstFrameFound(bi)` | **красная** строка `╚════╝` сверху по центру, 24pt |
| `frame.isFind && isManualCancel` | `cancelOverlayFirstFrameManual(bi)` | **оранжевая** `[` слева по центру, 48pt, opacity 0.2 — «начало отменено вручную» |
| следующий кадр `isFind` (конец плана) | `setOverlayLastFrameFound(bi)` | **красная** строка `╔════╗` снизу по центру |
| следующий кадр `isFind && isManualCancel` | `cancelOverlayLastFrameManual(bi)` | **оранжевая** `]` справа по центру |
| `frame.isManualAdd` | `setOverlayFirstFrameManual(bi)` | **зелёная** `╚════╝` сверху — граница добавлена оператором |
| следующий кадр `isManualAdd` | `setOverlayLastFrameManual(bi)` | **зелёная** `╔════╗` снизу |

Итого семантика цвета: **красный = найдено алгоритмом**, **зелёный = сделано оператором**, **оранжевый = отменено оператором**, **голубой = I-кадр**, **синий треугольник = есть лица**.

**Рамки состояния** (CSS на `Label`):
```kotlin
fxBorderDefault        = "-fx-border-color:#0f0f0f;-fx-border-width:1"  // обычное
fxBorderFocused        = "-fx-border-color:YELLOW;-fx-border-width:1" // наведение + toFront()
fxBorderSelected       = "-fx-border-color:RED;-fx-border-width:1"    // текущий кадр
fxBorderSelectedFocused= "-fx-border-color:ORANGE;-fx-border-width:1" // выделено + наведение
```

**Крупный кадр** — `lblFrameFull` (720×400, `contentDisplay` по умолчанию) показывает `FaceController.getOverlayedFrame(matrixFrame.frameExt, null)` — полный кадр с наложенными прямоугольниками лиц; при выборе лица — `getOverlayedFrame(frameExt, faceExt)` с выделением конкретного лица. Загрузка в отдельном потоке, результат через `Platform.runLater`.

### 5.3 Матрица лиц (`MatrixPageFaces` / `MatrixFace`) — вкладка «Persons»

То же самое, но:
* размер ячейки `Main.PREVIEW_FACE_W × PREVIEW_FACE_H` = 75×75 (квадрат);
* нет логики разрыва по `isFinalFind` — лица идут подряд по списку;
* при переходе на новую страницу добавляется только **один** дубликат крайнего лица с `column=0, row=1`;
* `MatrixPageFaces` — data-класс с `val pageNumber/countColumns/countRows/matrixFaces`, реализует `Comparable<MatrixFace>` через `FaceExt.compareTo`;
* таблица страниц `tblPagesFaces` — одна колонка `#` ← `pageNumber`.

**Содержимое миниатюры лица** (`showMatrixPageFaces()`, `ShotsEditFXController.kt:2116`): `matrixFace.faceExt.previewSmall` — кэшированный `ImageView` 75×75.
`FaceExt.previewSmall` лениво: если `pathToPreviewFile` существует — читает его; иначе вырезает область из **полного** кадра (`FrameExt.pathToFull`) через `OverlayImage.extractRegion(biSource, startX, startY, endX, endY, 75, 75, PREVIEW_FACE_EXPAND_FACTOR=1.4, PREVIEW_FACE_CROPPING=false)`, помечает `isExample` **зелёным** треугольником, `isManual` **красным**, сохраняет в `folderFacesPreview/<short>_frame_%06d_face_%02d.jpg` и кэширует.
Т.е. файл превью — это кэш, который можно удалить и он пересоздастся.

**Внутри ячейки лица находится именно лицо, а не фото персоны**; фото персоны используется в других местах:
* `FaceExt.labelPersonSmall` — Label с `personExt.previewSmall` → колонка `Person` в `frame-faces-edit-view.fxml`;
* `PersonExt.labelSmall` (135×75 с именем подчёркиванием) → колонка `PERSON` в `person-select-view.fxml`, колонки `Name` в `shots-edit-view.fxml`, `labelSmall` в `person-edit-view.fxml`;
* `PersonExt.labelMedium` (720×400, `previewMedium`, кроп из лица или кадра по `fileIdForPreview/frameNumberForPreview/faceNumberForPreview`) → `lblMediumPreview`.
`PersonExt.previewSmall` всегда накладывает имя подчёркиванием: `OverlayImage.setOverlayUnderlineText(bi, person.name)`.

**Перетаскивание и мультивыбор:** `lbl.onDragDetected` кладёт в `ClipboardContent` строку `"labelFace"`, `tblPersonsAllForFile.onDragOver/onDragDropped` её принимают (`TransferMode.COPY_OR_MOVE`), обработка — в `setRowFactory` по `hoverProperty`.

### 5.4 Оверлеи на кадрах (сцены/события) — через `ShotExt`

Ячейки `FROM`/`TO` в `tblShots`, `tblShotsForScenes`, `tblShotsForEvents` — это **не текст**, а `Label` с картинкой (`ShotExt.labelFirst1/2/3`, `labelLast1/2/3`). Три копии каждой (`labelFirst1`, `labelFirst2`, `labelFirst3`) используются в трёх разных таблицах, чтобы JavaFX-нода не переехала между таблицами.

`ShotExt.previewsFirst[i]` (135×75, из `firstFrameExt.pathToSmall`, иначе `FrameExt.pathToStubSmall`):
* `setOverlayUnderlineText(bi, start)` — подпись времени (`ЧЧ:ММ:СС`) под миниатюрой;
* если план попадает в сцену: `setOverlayIsBodyScene(bi)` — **оранжевая** вертикальная полоса 10×H слева; `setOverlayIsStartScene(bi)` — **оранжевый** прямоугольник 20×10 в левом верхнем углу; `setOverlayIsEndScene(bi)` — **оранжевый** прямоугольник 20×10 в левом нижнем.

`ShotExt.previewsLast[i]` — то же, но:
* `setOverlayIsBodyEvent(bi)` — **зелёная** вертикальная полоса 10×H справа;
* `setOverlayIsStartEvent(bi)` — **зелёный** прямоугольник 20×10 в правом верхнем;
* `setOverlayIsEndEvent(bi)` — **зелёный** прямоугольник 20×10 в правом нижнем.

`ShotExt.labelType` / `previewType` — Label/ImageView 135×75 с пиктограммой `ShotTypePerson`.

`SceneExt`/`EventExt` используют `sceneNameLabel`/`eventNameLabel` в колонке `NAME` (текстовые).
Пересчёт оверлеев при изменении сцен/событий: `shotExt.resetPreview()` (сбрасывает кэш `_previewsFirst/_previewsLast/_previewType/_labelsFirst/_labelsLast/_labelType`) + обращение к `it.labelsFirst` / `it.labelsLast` для пересоздания нод + `tblShots.refresh()`. Пересчёт запускается только для шотов, пересекающихся с затронутым диапазоном через `isPairIntersected()` (`min(b1,b2) - max(a1,a2) >= 0`).

### 5.5 Ресайз

`paneFrames.widthProperty()/heightProperty()` и `paneFaces…` → `listenToChangePaneSize()`. Пересчитываются `countColumnsInPageFrames/countRowsInPageFrames` (и аналоги для лиц), и **только если** число строк/столбцов реально изменилось — пересобираются страницы, восстанавливается текущий кадр/лицо (`getMatrixFrameByFrameNumber` / `getMatrixPageFacesByMatrixFace`) и перерисовывается страница. Позиция оператора не теряется.

### 5.6 Клавиатура и мышь (`shots-edit-view.fxml`)

`onStart()` (companion) навешивает на `Scene`:
* `CONTROL` → `isPressedControl`, `SHIFT` → `isPressedShift` (по `onKeyPressed`/`onKeyReleased`);
* `Z` → `isPressedPlayBackward`, `X` → `isPressedPlayForward` — **эти два флага нигде не читаются**, код «playback» не реализован (задел под видеоплеер). Также мёртвы `isPlayingForward` и `isWorking`.
* `Ctrl + колесо мыши` над `paneFrames` или `lblFrameFull` — шаг `±1` кадр **внутри текущего плана** (`currentShotExt.firstFrameExt/lastFrameExt`);
* колесо без Ctrl над `paneFrames` — шаг `±1` **страница** (`getNextMatrixFrame(last)`, `getPrevMatrixFrame(first)`);
* колесо над `paneFaces` — шаг `±1` страница лиц;
* ЛКМ по миниатюре кадра — выбрать кадр (`goToFrame`): снимает выделение в `tblShots`, выбирает план, содержащий кадр, ставит красную рамку, грузит крупный кадр;
* **двойной** ЛКМ по миниатюре кадра — переключение границы плана (см. §3 шаг 5а);
* наведение на миниатюру кадра — жёлтая рамка + `toFront()` (приподнимает над соседями);
* ЛКМ по миниатюре лица — выбрать; `Ctrl` — мультивыбор; `Shift` — выделение диапазона от `lastClickedMatrixFace` до текущего;
* ПКМ по миниатюре лица — контекстное меню персон (§3 шаг 5г);
* ПКМ по миниатюре кадра или по `lblFrameFull` — `Edit frame faces`.

---

## 6. Запуск приложения: Spring ↔ JavaFX

```
ProjectFXApp (@SpringBootApplication, extends javafx.application.Application)
  main() → Application.launch(ProjectFXApp::class.java)
    ↓ JavaFX Toolkit init
  start(stage, hostServices):
      initializeH2db()                                   // H2db.kt — только сырой JDBC, без Spring
      ProjectEditFXController().editProject(null, hostServices)
         → FXMLLoader.load(ProjectEditFXController::class.java.getResource("project-edit-view.fxml"))
         → mainStage = Stage(); scene = Scene(root); initModality(WINDOW_MODAL); showAndWait()
         → [при вызове любого Main.*] class Main.Companion static init:
              context = AnnotationConfigApplicationContext(SpringConfig::class.java)
              connection = getConnection()
              17 × context.getBean("<name>Repo", XxxRepo::class.java)
```

Ключевые наблюдения:

1. **`stage` из `Application.launch` игнорируется.** `ProjectFXApp.start()` имеет параметр `stage`, но ни разу к нему не обращается. Единственное создаваемое окно — вручную созданный `Stage` внутри `ProjectEditFXController`.
2. **Spring Boot не используется.** `@SpringBootApplication` на `ProjectFXApp` и на `Main` — дань моде; нет `SpringApplication.run(...)`, нет `SpringApplicationBuilder`, нет `application.properties` с boot-настройками (только `@PropertySource("/application.properties")` в `SpringConfig`). Контекст поднимается вручную: `AnnotationConfigApplicationContext(SpringConfig::class.java)`.
3. **Ленивая инициализация.** `Main` — `class Main { companion object { val context = ...; val propertyRepo = ...; … } }`. Kotlin-`val` в companion = статическое поле, инициализируемое при первом обращении к любому из них. То есть `Application.launch` стартует без БД, а БД/`DataSource` поднимаются в момент первого касания `Main.*` внутри уже показанного окна (обычно при `LoadListFilesExt`).
4. **`@PropertySource("/application.properties")` + `@ComponentScan("com.svoemesto.ivfx")`** — сканируются репозитории (`com.svoemesto.ivfx.repos.*`, помечены Spring Data `@Repository`/JPA-репозиториями). `@Transactional` на FX-контроллерах не даёт эффекта: контроллеры создаются вручную (`ProjectEditFXController()`), минуя контейнер, поэтому Spring-прокси не создаётся, а транзакции открываются только внутри `JdbcTemplate`/репозиториев.
5. **Два независимых подключения к БД:**
   * служебная `jdbc:h2:./h2db` (`getH2Connection()`) — только для `tbl_databases` и `tbl_properties` (какая рабочая БД текущая);
   * рабочая `jdbc:<driver>` из `getCurrentDatabase()` (`getConnection()`, `SpringConfig.dataSource()`) — сами данные.
   Именно поэтому `initializeH2db()` обязан выполняться **до** первого касания `Main.*`.
6. **Выбор БД требует перезапуска.** `doSelectDatabase()` при смене БД пишет `CURRENTDB_ID`, показывает Alert «Для вступления измненения в силу перезапустите приложение.» (опечатка «измженения») и **закрывает главное окно**.
7. **Есть альтернативные точки входа** (закомментированы в `ProjectFXApp` и живут отдельно): `H2db.kt:21 main()` → `initializeH2db()` + `DatabaseSelectFXController().getDatabase(null)`; `SomeUtils.kt:10 main()` → `AnnotationConfigApplicationContext` + `propertyRepo.count()` (smoke-тест).
8. **Хост-сервисы прокидываются вниз вручную.** `hostServices` из `Application.start` сохраняется в `companion object` каждого контроллера и передаётся дальше: `editProject → actionsProject/editShots/editFilters/editPerson → PersonSelectFXController`. Он нужен только для `showDocument(...)` (открытие `url_*`-ссылок и `folder_*`-папок двойным кликом). Там, где контроллер создаётся без `hostServices` (например `doPersonAdd()` в `PersonSelectFXController`), открытие ссылок не работает.

---

## 7. Ручное редактирование результатов анализа

Да, ручное редактирование — центральная функция приложения. Ниже полный перечень того, что оператор может изменить, и куда это попадает.

### 7.1 Кадры и границы планов (shots)

| Что | Где в UI | Механика | Персистенс |
|---|---|---|---|
| Граница плана (начало/конец) | Двойной ЛКМ по миниатюре кадра в `paneFrames` | Переключение `frame.isManualAdd` / `frame.isManualCancel` / `frame.isFinalFind` + `splitOrUnionShots()` | `FrameController.save(frame)`; `ShotController.save(shot)` / `ShotController.getOrCreate(...)` / `ShotController.delete(...)` |
| Тип плана | `colButtonGetType` → ContextMenu с пиктограммами | 6 значений `ShotTypePerson` (NONE/SGN/OTS/TWO/GRP/MASS) | `ShotController.save(shot)` (`shot.typePerson`) |
| Произвольные свойства плана | `tblShotProperties` + `fldShotPropertyKey` (TextField) + `fldShotPropertyValue` (TextArea) | Список ключей из `PropertyController.getMapKeyValuesByParentClass("Shot")`; добавление, удаление (с Alert), переупорядочивание ⟰⇧⇩⟱ | `PropertyController.save(property)` / `.delete(property)` / `.reOrder(ReorderTypes, property)` |

**Чего оператор НЕ может** редактировать: `xmin/xmax/ymin/ymax` шота (в модели `Shot` границы — это `firstFrameNumber`/`lastFrameNumber`, они меняются только двойным кликом по кадру); нет отдельных полей «имя плана» и «комментарий» — используется универсальный механизм свойств (key/value).

### 7.2 Сцены

| Что | Механика | Персистенс |
|---|---|---|
| Создать сцену из выбранных шотов | `Create new scene by selected shots`; требует **непрерывности** выбранных шотов (проверка смежности); затем `TextInputDialog` для имени | `SceneController.createSceneExt(listShotsExt)`, `SceneController.save(scene)` |
| Переименовать сцену | Тот же `TextInputDialog` сразу при создании; далее — только через свойства | — |
| Удалить сцену | Кнопка есть (`Delete selected scenes`), **обработчик пуст** | ничего |
| Свойства сцены | `tblSceneProperties` + 6 кнопок + `fldScenePropertyKey/Value` | `PropertyController.save/delete/reOrder` |
| Создать событие из сцены | `Create Event Based Scene` (название вводит в заблуждение) | `EventController.createEventExt(currentSceneExt)` |

### 7.3 События

| Что | Механика | Персистенс |
|---|---|---|
| Создать событие из выбранных шотов | `Create new event by selected shots` + `TextInputDialog` | `EventController.createEventExt(...)`, `.save(event)` |
| Удалить события | `Delete selected events` — **реализован**, удаляет все выбранные, пересчитывает превью затронутых шотов | `EventController.delete(event)` |
| Свойства события | `tblEventProperties` + 6 кнопок + `fldEventPropertyKey/Value` | `PropertyController.save/delete/reOrder` |

### 7.4 Лица и персоны

| Что | Механика | Персистенс |
|---|---|---|
| Привязать лицо к персоне | ПКМ по миниатюре лица: `SELECT PERSON` / `UNDEFINDED` / `NONPERSON` / `EXTRAS` / `CREATE NEW PERSON`; либо drag&drop миниатюры на строку `tblPersonsAllForFile` | `face.person = …`, `face.personRecognizedName = (если UNDEFINDED то "" иначе person.nameInRecognizer)`, `FaceController.save(face)` |
| Создать лицо вручную | `Edit frame faces` → нарисовать прямоугольник мышью по кадру → `Create new face` → выбрать/создать персону | `face.isManual = true`, `faceNumberInFrame = list.size+1`, `face.frameNumber`, `startX/startY/endX/endY`; `FaceController.save(face)`; превью с красным треугольником |
| Пометить лицо как эталон | `Set as EXAMPLE` (только для не-ручных и не-эталонных) | `face.isExample = true`, `FaceController.save(face)`, пересоздание превью |
| Снять эталон | `Remove from EXAMPLE` | `face.isExample = false` |
| Фото персоны | `Set as person picture` | `person.fileIdForPreview/frameNumberForPreview/faceNumberForPreview`, `PersonController.save(person)`, удаление старых `*.small.jpg`/`*.medium.jpg` |
| Имя персоны | `fldName` в `person-edit-view.fxml` | `PersonController.save(person)` |
| Свойства персоны | `tblProperties` + `fldPropertyKey`/`fldPropertyValue` | `PropertyController.save/delete/reOrder` |
| Создать персону | `➕` в `person-select-view.fxml`; или `CREATE NEW PERSON` из контекстного меню лица | `PersonController.create(project, name, PersonType.PERSON, …)` |
| Удалить персону | `✖ btnPersonDelete` — **обработчик пуст** | ничего |

### 7.5 Проект и файлы

| Что | Персистенс |
|---|---|
| Имя, короткое имя, папка, W/H/FPS, битрейты, частота, кодеки, контейнеры | `ProjectController.save(project)` + `ProjectCdfController.save(project.cdfs.first())` |
| Имя файла, короткое имя, путь | `FileController.save(file)` (короткое имя/путь → `resetFieldsLinkedShortName()`/`resetFieldsLinkedPath()`) |
| Свойства проекта / файла (+ CDF) | `PropertyController` / `PropertyCdfController` |
| Используемые дорожки | Двойной ЛКМ по `tblTracks` → `track.use` → `TrackController.save(track)` |
| Порядок файлов / проектов / свойств / персон в списках | `XxxController.reOrder(ReorderTypes, …)` |
| Пути к рабочим папкам (CDF) | `btnBrowseProjectPropertyCdfValue` / `btnBrowseFilePropertyCdfValue` → `DirectoryChooser`. **Смена пути сбрасывает флаги**: например смена `Folders.FRAMES_FULL.propertyCdfKey` обнуляет `folderFramesFull`, `hasFramesFull`, `hasDetectedFaces`, `hasCreatedFaces` |

### 7.6 Фильтры

| Что | Персистенс |
|---|---|
| Имя фильтра | `fldFilterName` (live) → `FilterController.save(filter)` |
| AND/OR между группами | `rbFilterIsAnd`/`rbFilterIsOr` → `FilterController.save(filter)` |
| Имя группы | `fldFilterGroupName` (live) → `FilterGroupController.save(filterGroup)` |
| AND/OR между условиями | `rbFilterGroupIsAnd`/`rbFilterGroupIsOr` → `FilterGroupController.save(filterGroup)` |
| Условие: объект (персона / персонное свойство / свойство плана / свойство сцены / свойство события), включённость (is included / is NOT included), субъект (Shot / Scene / Event) | `FilterConditionController.create(...)` / `.save(filterCondition)` |
| Удаление / порядок фильтров, групп, условий | `FilterController.delete` / `FilterGroupController.delete` / `FilterConditionController.delete`; `…reOrder(ReorderTypes, …)` |

### 7.7 Единый механизм свойств (сквозная черта)

Все редактируемые сущности — `Project`, `File`, `Shot`, `Scene`, `Event`, `Person`, `Track`, а также CDF-вариант для `Project`/`File` — используют **одни и те же** 6 кнопок (⟰⇧⇩⟱➕✖) и пару «Key = TextField / Value = TextArea». Шаблон кода повторяется в 6 контроллерах почти дословно. Ключи предлагаются из `PropertyController.getMapKeyValuesByParentClass(<SimpleClassName>)`; CDF-ключи — из `PropertyCdfRepo.getKeys(<SimpleClassName>, ccid)`. Пункт «Добавить все свойства для …» создаёт сразу весь набор отсутствующих ключей (есть только у `Project`/`File`/`Person`, но не у `Shot`/`Scene`/`Event`).

Сохранение происходит **по потере фокуса** поля (`focusedProperty().addListener { if (!newPropertyValue) saveCurrent…() }`) либо при закрытии окна, либо при смене выделения. Явной кнопки «Сохранить» в приложении нет вообще.

### 7.8 Мёртвый код и дефекты UI (сводка)

| Что | Где | Последствие |
|---|---|---|
| `doDeleteSelectedScenes()` пуст | `ShotsEditFXController.kt:2747` | Кнопка `Delete selected scenes` ничего не делает |
| `doPersonDelete()` пуст | `PersonSelectFXController.kt:223`, `PersonEditFXController.kt:533` | `✖` удаления персоны ничего не делает |
| `doPersonAdd()` пуст | `PersonEditFXController.kt:537` | `➕` в `person-edit-view.fxml` ничего не делает |
| `btnTagAdd`/`btnTagDelete` объявлены в Kotlin, но отсутствуют в FXML | `PersonEditFXController.kt:111-116` | всегда `null` |
| `pbShotsForScenes1`/`pbPersonsForScenes1` в FXML ≠ `pbShotsForEvents`/`pbPersonsForEvents` в Kotlin | `shots-edit-view.fxml:501,511` | инжект не происходит, поля всегда `null` |
| `Create Event Based Scene` создаёт Event | `doCreateEventBasedScene()`, `ShotsEditFXController.kt:3155` | вводит в заблуждение |
| `isPressedPlayForward`/`isPressedPlayBackward`/`isPlayingForward`/`isWorking` | `ShotsEditFXController.kt:432-437` | клавиши Z/X задуманы как видеоплеер, но не читаются нигде |
| «Время: с» → «Время: по», «Кадры: с» → «Кадры: по» | `shots-edit-view.fxml:210-213` | обрезаны подписи колонок |
| Тултипы «Добавить файл» / «Удалить файл из проекта» на кнопках свойств/персон | практически все FXML | скопированы из шаблона списка файлов |
| `pbPersonsAllForEvents`, `pbShotsForEvents` объявлены в Kotlin как отдельные поля, но в FXML другие id | `shots-edit-view.fxml` | прогресс на вкладке Events не отображается |
| `pbEvents` используется в Kotlin, в FXML для Events — `pbEvents` ✔; но `LoadListEventsExt(…, null, null, …)` передаёт `null` вместо прогресс-баров | `ShotsEditFXController.kt:682` | на вкладке Events индикатор не заполняется при загрузке |
| Смешение языков UI | `Create new face`, `Train face model`, `Do actions`, `Cancel`, `PERSON`, `M`, `&|` на русском экране | — |
| Сравнение строк в БД без параметризации | `H2db.kt` (везде через строковую конкатенацию) | риск SQL-инъекции и поломок на апострофах |
| `@Transactional` на FX-контроллерах | `ProjectEditFXController.kt:71`, `ShotsEditFXController.kt:100` | не работает — контроллеры не Spring-бины |
| Доступ к внутренностям JavaFX | `import com.sun.javafx.scene.control.skin.TableViewSkin/VirtualFlow` | сломается на стандартном (не-SDK) JavaFX; требует Oracle JDK/OpenJFX с internal API |
