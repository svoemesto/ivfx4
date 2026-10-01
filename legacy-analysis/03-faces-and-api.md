# 03 — Распознавание лиц и «REST API»/контроллеры (ivfx4)

Дата разбора: 2026-09. Прочитанные файлы (полностью):

- `src/main/kotlin/com/svoemesto/ivfx/utils/FaceDetection.kt`
- `src/main/resources/com/svoemesto/ivfx/utils/FaceDetector/detect_faces_in_folder.py`
- `src/main/resources/com/svoemesto/ivfx/utils/FaceDetector/recognize_faces.py`
- `src/main/resources/com/svoemesto/ivfx/utils/FaceDetector/train_model_json.py`
- `src/main/kotlin/com/svoemesto/ivfx/controllers/*.kt` (17 файлов)
- `src/main/kotlin/com/svoemesto/ivfx/HelloController.kt`
- плюс (для контекста) `threads/loadlists/*.kt`, `threads/updatelists/*.kt`,
  `threads/projectactions/{DetectFaces,RecognizeFaces,CreateFaces}.kt`, `threads/{RunCmd,RunListThreads}.kt`,
  `models/Face.kt`, `modelsext/{FaceExt,FaceExtJson}.kt`, `repos/FaceRepo.kt`, `Main.kt`, `pom.xml`.

---

## 0. ГЛАВНЫЙ ВЫВОД, КОТОРЫЙ МЕНЯЕТ ЗАПРОС

**REST API в проекте НЕТ.** Ни в одном файле `src/` нет ни `@RestController`,
ни `@RequestMapping`, ни `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`,
ни `ResponseEntity`, ни `@RequestBody`/`@RequestParam`/`@CrossOrigin`.
В `pom.xml` (Spring Boot 2.5.6) подключены `spring-boot-starter-data-jpa`,
`-data-jdbc`, `-jdbc`, `-validation` — но **нет** `spring-boot-starter-web` (grep: 0 совпадений).

Приложение — **десктопное JavaFX + Spring Context (без HTTP-сервера)**.
`Main.kt` поднимает контекст вручную:

```kotlin
@SpringBootApplication
class Main {
    companion object {
        val ccid = getCurrentComputerId()
        val context = AnnotationConfigApplicationContext(SpringConfig::class.java)
        val connection = getConnection()
        val projectRepo = context.getBean("projectRepo", ProjectRepo::class.java)
        ...
    }
}
```

Поэтому «контроллеры» — это **не HTTP-эндпоинты**, а singleton-классы с чисто
статическими (Kotlin `companion object`) сервисными методами над репозиториями.
Аннотация `@Controller` (Spring stereotype) на них повешена **по инерции**
(по всей видимости, чтобы работал component-scan и DI), но ни одного
mapped-handler-метода в них нет. Фактическая «API» проекта — это:

1. **внутрипроцессный Kotlin/Java API** — `ProjectController.getProjectExt(id)` и т.п.;
2. **JSON-протокол обмена с Python** (Gson ↔ `json`) — единственный реальный
   «сетевой» интерфейс приложения;
3. **JavaFX `ObservableList`** — «выдача» данных в UI.

Ниже раздел 2 честно описывает это как «API проекта», а не как REST.

---

## 1. РАСПОЗНАВАНИЕ ЛИЦ

### 1.1. Где лежат скрипты и модели

Всё в ресурсах, распаковывается в classpath-каталог:

```
src/main/resources/com/svoemesto/ivfx/utils/FaceDetector/
├── detect_faces_in_folder.py     (7 636 б)
├── recognize_faces.py            ( 2 890 б)
├── train_model_json.py           ( 1 427 б)
├── openface_nn4.small2.v1.t7     (31 510 785 б — 30 МБ, OpenFace nn4.small2)
└── face_detection_model/
    ├── deploy.prototxt                         (28 092 б)
    └── res10_300x300_ssd_iter_140000.caffemodel (10 666 211 б — Caffe SSD Res10 300x300)
```

Путь к папке получается в `utils/FaceDetection.kt` — весь файл 5 строк:

```kotlin
package com.svoemesto.ivfx.utils

object FaceDetection {
    val FACE_DETECTOR_PATH = FaceDetection::class.java.getResource("FaceDetector")?.path?.substring(1)?:""
}
```

`.substring(1)` срезает ведущий `/` из `file:/C:/.../FaceDetector` (т.е. рассчитано на Windows-подобный URL ресурса; на Linux classpath это даст путь от корня ФС — известная хрупкость).

### 1.2. Модели — то, что реально используется

| Задача | Модель | Файл | Как грузится |
|---|---|---|---|
| **Детекция лиц** | Caffe SSD Res10 300×300 | `face_detection_model/deploy.prototxt` + `res10_300x300_ssd_iter_140000.caffemodel` | `cv2.dnn.readNetFromCaffe(protoPath, modelPath)` |
| **Эмбеддинг лица** | OpenFace nn4.small2 (PyTorch .t7) | `openface_nn4.small2.v1.t7` | `cv2.dnn.readNetFromTorch(...)` |
| **Классификация лица** | SVC(sklearn), linear kernel | `<project>/recognizer.pickle` | `pickle.loads(open(...))` |
| **Кодировщик имён** | `sklearn.preprocessing.LabelEncoder` | `<project>/le.pickle` | `pickle.loads(open(...))` |

### 1.3. Оркестрация: три Python-скрипта, три Kotlin-потока

```
[UI] ProjectActionsFXController.doProjectActions()
      │  собирает список потоков по чекбоксам
      ▼
RunListThreads(listThreads).start()      // последовательно, по одному
      ├─ DetectFaces      (detect_faces_in_folder.py)  → faces.json + папка лиц
      ├─ CreateFaces      (без python; читает faces.json → пишет в БД)
      ├─ CreateFacesPreview (без python; нарезка превью)
      └─ RecognizeFaces   (recognize_faces.py)         → faces.json → БД
[UI кнопка] ProjectActionsFXController.doTrainFaceModel()
      └─ RunCmd("py train_model_json.py -e embeddings.json -r recognizer.pickle -l le.pickle")
```

Все внешние вызовы идут через один механизм — `threads/RunCmd.kt`: текст команды
пишется во временный `.cmd`-файл (`IOFile.createTempFile("ivfx", ".cmd")`) и
запускается `ProcessBuilder(cmdFile.absolutePath)`; `redirectErrorStream(true)`,
вывод читается в `ByteArrayOutputStream` и печатается, **только если код возврата ≠ 0**:

```kotlin
private fun exec(cmd: String): Int {
    val pb = ProcessBuilder(cmd)
    pb.redirectErrorStream(true)
    val p = pb.start()
    p.outputStream.close()
    val baos = ByteArrayOutputStream()
    copy(p.inputStream, baos)
    val r = p.waitFor()
    if (r != 0) println(cmd + " cmd: output:\n" + baos)
    return r
}
```

То есть при ошибке Python-кода приложение **молча продолжает** работать — код возврата нигде не проверяется вызывающим кодом (`runCmd.run()` результат игнорируется). Это источник «призрачных» сбоев.

Командная строка строится характерным приёмом: список фрагментов → `joinToString(" ")` →
`.replace("/", "\\")`, с принудительным `cd` в папку скриптов и префиксом `C:\n`:

```kotlin
param.add("${faceDetectorPath.first()}:\n")   // привод диска, напр. "C:\n"
param.add("cd \"${faceDetectorPath}\"\n")
param.add("py")                                // launcher Python
param.add("\"${faceDetectorPath}/detect_faces_in_folder.py\"")
...
val cmdText = param.joinToString(separator=" ").replace("/","\\")
```

### 1.4. `detect_faces_in_folder.py` — пошагово

Аргументы (`argparse`):

```python
ap.add_argument("-i", "--dataset", required=True, help="путь к папке с изображениями")
ap.add_argument("-o", "--output", required=True, help="путь к папке, в которую будут складываться лица")
ap.add_argument("-d", "--detector", required=True, help="path to OpenCV's deep learning face detector")
ap.add_argument("-m", "--embedding-model", required=True, help="path to OpenCV's deep learning face embedding model")
ap.add_argument("-c", "--confidence", type=float, default=0.5, ...)
```

Реальный вызов из `threads/projectactions/DetectFaces.kt:57-70`:

```
py "<FACE_DETECTOR_PATH>\detect_faces_in_folder.py"
   -i "<fileExt.folderFramesFull>"
   -o "<fileExt.folderFacesFull>"
   -d "<FACE_DETECTOR_PATH>\face_detection_model"
   -m "<FACE_DETECTOR_PATH>\openface_nn4.small2.v1.t7"
   -c 0.3
```

Обратите внимание: **`-c 0.3`**, а не дефолт 0.5.

Шаги:

1. **Загрузка детектора** (строки 29-32):
   `protoPath = os.path.sep.join([args["detector"], "deploy.prototxt"])`,
   `modelPath = ... "res10_300x300_ssd_iter_140000.caffemodel"`, `cv2.dnn.readNetFromCaffe`.
2. **Загрузка эмбеддера** (строка 36): `cv2.dnn.readNetFromTorch(args["embedding_model"])`.
3. **Чтение входного списка** — НЕ сканирование папки (`paths.list_images` закомментирован на строках 39-40), а чтение `frames.json`:
   ```python
   listframes_file = os.path.sep.join([args["dataset"], "frames.json"])
   listframes = json.loads(open(listframes_file, "rb").read())
   ```
   `frames.json` пишет Kotlin перед запуском: `DetectFaces.kt:41,47`
   ```kotlin
   val pathToFileJSON: String = fileExt.folderFramesFull + IOFile.separator + "frames.json"
   val arrFrameToDetectFaces: Array<FaceController.Companion.FrameToDetectFaces> =
       FaceController.getArrayFramesToDetectFaces(fileExt)
   FileWriter(pathToFileJSON).use { gson.toJson(arrFrameToDetectFaces, it) }
   ```
   Структура элемента — `FaceController.FrameToDetectFaces` (data class, строки 344-348):
   `projectId`, `fileId`, `frameNumber`, `pathToFrameFile`.
4. **Цикл по кадрам** (`for i in range(0, len(listframes))`), имя файла кадра без расширения: `name_file_wo_ext = imagePath.split(sep)[-1]` минус 4 символа.
5. **Ресайз до ширины 1920** — совпадает с `Main.FULL_FRAME_W = 1920.0`:
   ```python
   image = cv2.imread(imagePath)
   image = imutils.resize(image, width=1920)
   (h, w) = image.shape[:2]
   ```
6. **Blob для SSD 300×300** (без `swapRB`, mean `(104,177,123)`):
   ```python
   imageBlob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0, (300, 300),
                                      (104.0, 177.0, 123.0), swapRB=False, crop=False)
   detector.setInput(imageBlob); detections = detector.forward()
   ```
7. **Цикл по всем детекциям** `for i in range(0, detections.shape[2])`, фильтр по confidence:
   ```python
   confidence = detections[0, 0, i, 2]
   if confidence > args["confidence"]:
       box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
       (startX, startY, endX, endY) = box.astype("int")
       face = image[startY:endY, startX:endX]
       (fH, fW) = face.shape[:2]
       if fW < 20 or fH < 20:      # мелкие лица отбрасываются
           continue
   ```
8. **Сохранение вырезки** под именем `<shortName>_frame_000123_face_01.jpg`:
   ```python
   face_file_name = name_file_wo_ext + "_face_" + "{:02d}".format(face_count_in_image) + ".jpg"
   cv2.imwrite(face_file_name_and_path, face)
   ```
   Этот же шаблон имени потом проверяется в `FileController.hasDetectedFaces` / `hasCreatedFacesPreview`:
   `Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg$")`.
9. **Эмбеддинг 96×96, 128-мерный вектор**:
   ```python
   faceBlob = cv2.dnn.blobFromImage(face, 1.0/255, (96, 96), (0, 0, 0), swapRB=True, crop=False)
   embedder.setInput(faceBlob)
   vec = embedder.forward()
   ```
10. **Запись элемента в список** (строки 134-149):
    ```python
    face_data = {'projectid': projectid, 'frameId': 0, 'fileId': fileid, 'personId': 0,
                 'frameNumber': frameNumber, 'faceNumberInFrame': face_count_in_image,
                 'pathToFrameFile': imagePath, 'pathToFaceFile': face_file_name_and_path,
                 'personRecognizedName': '', 'recognizeProbability': 0.0,
                 'startX': int(startX), 'startY': int(startY),
                 'endX': int(endX), 'endY': int(endY),
                 'vector': vec.flatten().tolist()}
    ```
11. **Выгрузка всего результата** в `<dataset>/faces.json`:
    `json.dump(data_faces, file)` (строки 152-153). Заметьте: `data_file` = `<dataset>/faces.json`, т.е. **в папку кадров**, а не в папку лиц.

### 1.5. `recognize_faces.py` — пошагово

Аргументы: `-i/--inputjson`, `-d/--detector`, `-m/--embedding-model`, `-r/--recognizer`, `-l/--le`, `-c/--confidence`.

Реальный вызов (`threads/projectactions/RecognizeFaces.kt:67-82`):

```
py "<FACE_DETECTOR_PATH>\recognize_faces.py"
   -i "<fileExt.folderFramesFull>\faces.json"
   -d "<FACE_DETECTOR_PATH>\face_detection_model"
   -m "<FACE_DETECTOR_PATH>\openface_nn4.small2.v1.t7"
   -r "<project.folder>\recognizer.pickle"
   -l "<project.folder>\le.pickle"
   -c 0.3
```

Шаги:

1. Загрузка детектора и эмбеддера (**абсолютно та же пара вызовов**, строки 27-34) — хотя в этом скрипте они **не используются**: ни детекции, ни пересчёта эмбеддингов в нём нет. Мёртвый, но платный по времени код (загрузка 30 МБ .t7 + 10 МБ caffemodel ради нуля).
2. Загрузка обученной модели и кодировщика имён:
   ```python
   recognizer = pickle.loads(open(args["recognizer"], "rb").read())
   le = pickle.loads(open(args["le"], "rb").read())
   ```
3. Чтение `faces.json` целиком: `data_of_images = json.loads(open(file_json_images, "rb").read())`.
4. Цикл по лицам, **только для нераспознанных**:
   ```python
   person_type = face_data['personType']
   if person_type == 'UNDEFINDED':
       print(face_data['pathToFaceFile'])
       vec_flat = np.array(face_data['vector'])
       vec = vec_flat.reshape(-1, 128)
       vec_flat2 = vec.flatten()          # <- вычисляется и НЕ используется
       preds = recognizer.predict_proba(vec)[0]
       j = np.argmax(preds)
       proba = preds[j]
       name = le.classes_[j]
       face_data['personId'] = 0
       face_data['personRecognizedName'] = name
       face_data['recognizeProbability'] = proba
   ```
   Важно: `face_data['personId'] = 0` присваивается **всем** обработанным, но фактическую привязку к персоне делает уже Kotlin.
5. Перезапись **того же самого** `faces.json` обратно: `json.dump(data_of_images, file)` — файл служит и входом, и выходом.

### 1.6. `train_model_json.py` — обучение

Аргументы: `-e/--embeddings`, `-r/--recognizer`, `-l/--le`.

Реальный вызов (`fxcontrollers/ProjectActionsFXController.kt:105-125`, метод `doTrainFaceModel(event: ActionEvent?)`):

```kotlin
val listFacesToTrain = FaceController.getListFacesToTrain(currentProject)
val embeddings = Embeddings(arrayOfNulls(listFacesToTrain.size), arrayOfNulls(listFacesToTrain.size))
for ((i, face) in listFacesToTrain.withIndex()) {
    embeddings.vectors[i] = face.vector
    embeddings.tags[i] = face.personRecognizedName
}
gson.toJson(embeddings, FileWriter(pathToFileJSON))   // <project.folder>\embeddings.json
...
param.add("\"${faceDetectorPath}/train_model_json.py\"")
param.add("-e"); param.add("\"${pathToFileJSON}\"")
param.add("-r"); param.add("\"${currentProject.folder}\\recognizer.pickle\"")
param.add("-l"); param.add("\"${currentProject.folder}\\le.pickle\"")
```

`Embeddings` — вложенный класс с `@SerializedName`:
```kotlin
class Embeddings(
    @SerializedName("embeddings") var vectors: Array<DoubleArray?>,
    @SerializedName("names")     var tags:    Array<String?>
)
```

Сама обучающая часть:
```python
from sklearn.preprocessing import LabelEncoder
from sklearn.svm import SVC

data = json.loads(open(args["embeddings"], "rb").read())
le = LabelEncoder()
labels = le.fit_transform(data["names"])
recognizer = SVC(C=1.0, kernel="linear", probability=True)
recognizer.fit(data["embeddings"], labels)
pickle.dump(recognizer, open(args["recognizer"], "wb"))
pickle.dump(le, open(args["le"], "wb"))
```

Ключевые факты:
- **SVM `SVC(C=1.0, kernel="linear", probability=True)`** — вероятности доступны благодаря `probability=True` (внутренний 5-fold Platt scaling).
- Обучающая выборка — **только `is_example = true`** (`FaceRepo.getListFacesToTrain`):
  `SELECT * FROM tbl_faces INNER JOIN tbl_files ... WHERE tbl_files.project_id = ?1 AND tbl_faces.is_example = true`.
- Метка — `face.personRecognizedName` (строка), а не `personId`.

### 1.7. Порог похожести — где он на самом деле

Порогов **в Python нет вообще**. `recognize_faces.py` принимает `-c/--confidence`, но **ни разу его не читает** (мёртвый аргумент). Реальный порог применяется на стороне Kotlin — **жёстко зашитый `0.3`**, в четырёх местах `FaceController.createOrUpdate`:

```kotlin
if (faceExtJson.personRecognizedName != "") {
    if (faceExtJson.recognizeProbability > 0.3) {
        face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(
            fileExt.projectExt.project, faceExtJson.personRecognizedName,
            faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
    } else {
        face.person = undefindedPerson.person
    }
} else {
    face.person = undefindedPerson.person
}
```

То есть: вероятность SVM ≥ 0.3 → персона создаётся/берётся; иначе → персональный «UNDEFINDED»-placeholder.

### 1.8. Как хранятся 128-мерные дескрипторы

**В БД — как `@Lob`-строка с разделителем `|`** (не как 128 колонок, не как BLOB-числа). `models/Face.kt:73-88`:

```kotlin
@Lob
@Column(name = "vector")
var vectorText: String = "0.0"

var vector: DoubleArray
    get() {
        val textVector: Array<String> = vectorText.split("\\|".toRegex()).toTypedArray()
        val result = DoubleArray(textVector.size)
        for (i in textVector.indices) result[i] = textVector[i].toDouble()
        return result
    }
    set(value) {
        vectorText = if (vector.isEmpty()) "" else value.joinToString(separator = "|", prefix = "", postfix = "")
    }
```

Запись при создании лица (`FaceController.createOrUpdate`, строка 150):
```kotlin
face.vectorText = faceExtJson.vector.joinToString(separator = "|", prefix = "", postfix = "")
```
Сравнение при обновлении — `contentEquals` по `DoubleArray` (`if (!faceExt.vector.contentEquals(faceExtJson.vector))`), т.е. точное покомпонентное, без epsilon.

Таблица `tbl_faces` (`@Table(name = "tbl_faces")`), связи:
```kotlin
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "file_id")   lateinit var file: File
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "person_id") lateinit var person: Person
```
Поля: `face_number_in_frame`, `frame_number`, `person_recognized_name`, `recognize_probability`,
`start_x/start_y/end_x/end_y`, `is_example`, `is_manual`, `vector`.

**Смежный приём**: `FaceController.getListFacesExt(shotExt|projectExt, ...)` в двух местах
(строки 279-286 и 320-327) **дёргает сырой JDBC в обход JPA**, чтобы вытащить `file_id`:
```kotlin
val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
val stFaces = Main.connection.prepareStatement(sqlFaces)
stFaces.setLong(1, face.id)
val rsFaces = stFaces.executeQuery()
while (rsFaces.next()) { fileId = rsFaces.getLong("file_id"); break }
```
и кэширует `FileExt` в `setFilesExt`. Тот же приём в `FileController.getFramesWithFaces` и в `FrameController.createFrames`.

### 1.9. Обратный путь: `faces.json` → БД

Два потока читают `faces.json` и дёргают `FaceController.createOrUpdate`:

- `threads/projectactions/CreateFaces.kt` — после детекции (заполняет координаты, вектор, `person = UNDEFINDED`);
- `threads/projectactions/RecognizeFaces.kt` — после распознавания (заполняет `personRecognizedName`).

`RecognizeFaces.kt` перед запуском Python **обнуляет** всё неопознанное:
```kotlin
val arrFrameFaces: Array<FaceExt> = FaceController.getListFacesExtToRecognize(fileExt).toTypedArray()
arrFrameFaces.forEach {
    it.personId = 0
    it.personType = PersonType.UNDEFINDED.name
    it.personRecognizedName = ""
}
gson.toJson(arrFrameFaces, FileWriter(pathToFileJSON))
```
`getListFacesExtToRecognize` → `FaceRepo.findFacesToRecognize`:
`SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id = ?2` (где `?2` = id UNDEFINDED-персоны).

Затем обратное чтение:
```kotlin
FileReader(pathToFileJSON).use {
    val facesExtJsonArray: Array<FaceExtJson> = gson.fromJson(it, Array<FaceExtJson>::class.java)
    ...
    FaceController.createOrUpdate(faceExtJson, fileExt, undefindedPerson, nonPerson)
}
```
DTO — `modelsext/FaceExtJson.kt` (обычный mutable-класс, 17 полей, `vector: DoubleArray`).

Ключевой нюанс `createOrUpdate` (строки 36-44) — **логика «найти или создать»**:
```kotlin
var face = if (faceExtJson.frameId == 0L) {
    Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(
        faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame).firstOrNull()
} else {
    if (faceExtJson.faceId == 0L) null
    else Main.faceRepo.findById(faceExtJson.frameId).orElse(null)   // <- баг: ищет по frameId, а не faceId
}
```
Естественный ключ лица = тройка **(fileId, frameNumber, faceNumberInFrame)**.

Ещё один фильтр в `createOrUpdate` — «не лицо» по пропорциям bbox:
```kotlin
val w = faceExtJson.endX - faceExtJson.startX
val h = faceEndJson.endY - faceExtJson.startY
val d = if (w > h) w/h.toDouble() else h/w.toDouble()
...
if (d > 4) face.person = nonPerson.person   // слишком вытянутый bbox → NONPERSON
```

### 1.10. Выбор кадров для детекции

`FaceController.getFramesToRecognize(fileExt)` (строки 362-382) — адаптивный шаг по длине плана:

```kotlin
val stepFrames = if (shot.lastFrameNumber - shot.firstFrameNumber < 15) 3
                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 30) 5
                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 60) 10 else 20
var i: Int = shot.firstFrameNumber
while (i < shot.lastFrameNumber) { curr = i; if (curr <= countFrames) listFrames.add(curr); i += stepFrames }
if (curr < shot.lastFrameNumber) curr = shot.lastFrameNumber
if (curr <= countFrames) listFrames.add(curr)
```
И путь кадра формируется шаблоном `%06d`:
```kotlin
"${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", n)}.jpg"
```

### 1.11. Наложение рамок на кадр

`FaceController.getOverlayedFrame(frameExt, faceExt, fullFrame)` — чисто JavaFX/AWT-рисование поверх `BufferedImage`:
масштаб `val scaleFactor = frameWidth / Main.FULL_FRAME_W`; цвет подписи `YELLOW` (авто), `RED` (isManual), `ORANGE` (выбранное ручное), `GREEN` (выбранное авто); под чёрным фоном рисуется `faceExtInFrame.personExt.person.name`.

### 1.12. Мёртвый/подозрительный код в Python (итог)

| Место | Проблема |
|---|---|
| `detect_faces_in_folder.py:2` | `sys.path.append("../imutils")` — относительный путь от CWD, работает только потому, что `cd` делается в папку скрипта |
| `detect_faces_in_folder.py:40` | `from imutils import paths` + `imagePaths = list(paths.list_images(...))` закомментированы и не нужны |
| `detect_faces_in_folder.py:77-78` | ручное удаление 4 символов вместо `os.path.splitext` |
| `recognize_faces.py:27-34` | детектор + эмбеддер грузятся, но не используются (30+10 МБ впустую) |
| `recognize_faces.py:23` | аргумент `-c/--confidence` не используется |
| `recognize_faces.py:54` | `vec_flat2 = vec.flatten()` — вычисляется и не используется |
| `recognize_faces.py:49` | сравнение строкой `person_type == 'UNDEFINDED'` вместо enum |
| `detect_faces_in_folder.py:98` | внутренний цикл переиспользует имя `i`, затирая внешний счётчик прогресса |

---

## 2. «REST API» → фактический API проекта

### 2.1. Формальная сводка по всем контроллерам

Ниже — **полный** перечень публичных методов каждого из файлов в `controllers/`
(все — `companion object`, все статические; ни одного HTTP-метода).

#### ProjectController.kt (`@Controller @Transactional`)
`getCdfFolder(project, folder: Folders, createIfNotExist=false): String` ·
`getListProjects(): List<Project>` · `getProject(projectId: Long): Project` ·
`getProjectExt(projectId: Long): ProjectExt` · `getProjectForFileId(fileId: Long): Project` ·
`create(): Project` · `delete(project)` · `reOrder(reorderType: ReorderTypes, project)` ·
`save(project)` / `saveAll(projects)` ·
`getProperties(project)` / `getPropertyValue(project,key)` / `isPropertyPresent(project,key)` /
`getPropertyCdfValue(project,key)` / `isPropertyCdfPresent(project,key)` ·
15 функций-полупутей: `getFolderPersons`, `getFolderFilters`, `getFolderLossless`, `getFolderPreview`,
`getFolderFavorites`, `getFolderShots`, `getFolderFramesSmall/Medium/Full`,
`getFolderFacesFull`, `getFolderFacesPreview`, `getFolderShotsCompressedWithAudio`,
`getFolderShotsLosslessWithAudio`, `getFolderShotsLosslessWithoutAudio`, `getFolderConcat` ·
`val LOG: Logger`.
Каждая `getFolderX(project)` = `PropertyCdfController.getOrCreate("Project", id, Folders.X.propertyCdfKey)`,
и если пусто — `project.folder + sep + Folders.X.folderName`.

#### FileController.kt (`@Controller`)
`getCdfFolder(file, folder, createFolderIfNotExist=false)` ·
`getListFilesExt(project): List<FileExt>` · `getSetFiles(project): MutableSet<File>` ·
`getFile(fileId, project)` · `getFileExt(fileId, project): FileExt` · `getFileForShotId(shotId)` ·
`getFramesWithFaces(file): MutableSet<Int>` (сырой SQL `select distinct tf.frame_number from tbl_faces`) ·
`create(project, path)` · `delete(file)` · `deleteAll(project)` · `reOrder(type, file)` ·
`save`/`saveAll` · `getProperties`/`getPropertyValue`/`isPropertyPresent`/`getPropertyCdfValue`/`isPropertyCdfPresent` ·
`getFFmpegProbeResult(file): FFmpegProbeResult` (FFprobe) · `getFps(file): Double` · `getFramesCount(file): Int` ·
`getLossless`/`getPreview`/`getConcat` (+ `hasLossless/hasPreview/hasConcat`) ·
`hasFramesSmall/Medium/Full(fileExt)` (сверка по regex-имени и `fileExt.framesCount`) ·
`hasAnalyzedFrames(file)` · `hasCreatedShots(file)` ·
`hasDetectedFaces(fileExt)` / `hasCreatedFaces(file)` / `hasRecognizedFaces(file)` / `hasCreatedFacesPreview(fileExt)` ·
`hasShotsCompressedWithAudio/hasShotsLosslessWithAudio/hasShotsLosslessWithoutAudio` — **все три жёстко `return false`**.

#### FrameController.kt (`@Controller`)
`getOrCreate(file, frameNumber): Frame` · `getListFrames(file)` · `getListFramesExt(fileExt)` ·
`getFrameExt(fileId, frameNumber, project)` · `getFrameExt(fileExt, frameNumber)` ·
`createFrames(fileExt)` · `create(file, frameNumber)` · `save(frame)` · `delete(frame)` · `deleteAll(file)` ·
`getPropertyValue(frame,key)` / `isPropertyPresent(frame,key)`.
`createFrames` — чистый SQL: 6 `cross JOIN` на 0..9 дают 10⁶ строк, `SELECT distinct (a1 + a2*10 + ... + a6*100000)+1 ... limit <framesCount>` — генерация кадров 1..N на стороне БД.

#### ShotController.kt (`@Controller`)
`getSetShots(file)` · `getProperties(shot)` / `getPropertyValue(shot,key)` / `isPropertyPresent(shot,key)` ·
`save`/`saveAll`/`delete`/`deleteAll(file)` · `getOrCreate(file, firstFrameNumber, lastFrameNumber, nearestIFrame=0)` ·
`convertSetShotsToListShotsExt(shots)` · `convertSetShotsIdsToListShotsExt(shotsIds, projectExt)`.
Второй метод — интересный обход: кладёт id в **временную таблицу `shotTmp2Cdf`**, сортированную по `computer_id`:
```kotlin
Main.shotTmp2CdfRepo.deleteAll(Main.ccid)
Main.shotTmp2CdfRepo.addByShotIds(Main.ccid, shotsIds)
val shotTmp2Cdf = Main.shotTmp2CdfRepo.findByComputerId(Main.ccid)
val tmp = Main.shotRepo.findByIds(shotsIds)
val shots = tmp.associateBy { it.id }.toMutableMap()
```

#### SceneController.kt (`@Controller`)
`getSetScenes(file)` · `getProperties`/`getPropertyValue`/`isPropertyPresent` · `save`/`saveAll`/`delete`/`deleteAll(file)` ·
`getOrCreate(file, firstFrameNumber, lastFrameNumber)` · `createSceneExt(listShotsExt): SceneExt?`.
`getOrCreate` — самая нетривиальная логика: перед вставкой ищет «пересекающиеся» сцены
(`getCrossingScenes`) и **расщепляет** их на 1/2/3 части либо удаляет:
```kotlin
if (currentScene.firstFrameNumber < firstFrameNumber && currentScene.lastFrameNumber > lastFrameNumber) {
    // разрезать на две: [lastFrameNumber+1 .. currentScene.lastFrameNumber]
} else if (... lastFrameNumber >= firstFrameNumber) { currentScene.lastFrameNumber = firstFrameNumber - 1 }
  else if (... firstFrameNumber <= lastFrameNumber && ... > lastFrameNumber) { currentScene.firstFrameNumber = lastFrameNumber + 1 }
  else { delete(currentScene) }
```

#### FaceController.kt (`@Controller`) — 534 строки
`createOrUpdate(faceExtJson, fileExt, undefindedPerson, nonPerson): FaceExt` · `save(face)` ·
`getListFaces(file)` · `getListFacesExt(fileExt)` · `getListFacesExtToRecognize(fileExt)` ·
`getListFacesToTrain(project)` · `getListFacesExt(frameExt)` ·
`getListFacesExt(fileExt|shotExt|projectExt, personExt, loadNotExample=true, loadExample=true, loadNotManual=true, loadManual=true)` ·
`getFace(fileId, frameNumber, faceNumber)` · `getFaceExt(fileId, frameNumber, faceNumber, project)` ·
`getArrayFramesToDetectFaces(fileExt): Array<FrameToDetectFaces>` · `getFramesToRecognize(fileExt): List<Int>` ·
`deleteAll(file)` · `getOverlayedFrame(frameExt, faceExt=null, fullFrame=false): BufferedImage?` ·
`data class FrameToDetectFaces(projectId, fileId, frameNumber, pathToFrameFile)`.

#### PersonController.kt (`@Controller`)
`getSetPersons(project)` · `getListPersonsExt(projectExt)` · `getById(personId)` ·
`getUndefinded/getNonperson/getExtras(project): Person` (+ `...Ext(projectExt): PersonExt`) ·
`create(project, name="", personType=PersonType.PERSON, nameInRecognizer="", fileIdForPreview=0, frameNumberForPreview=0, faceNumberForPreview=0): Person` ·
`getPersonByProjectIdAndNameInRecognizer(project, personRecognizedName, fileIdToCreate, frameNumberToCreate, faceNumberInFrameToCreate)` ·
`delete(person)` / `deleteAll(project)` / `save`/`saveAll` / `isPropertyPresent(person,key)`.
Ключевая идентичность персоны: `uuid = UUID.randomUUID().toString()`, `nameInRecognizer = nameInRecognizer.ifEmpty { uuid }` — то есть метка SVM = «имя в распознавателе», а не отображаемое `name`.

#### EventController.kt (`@Controller`)
`getSetEvents(file)` · `getProperties`/`getPropertyValue`/`isPropertyPresent` · `save`/`saveAll`/`delete`/`deleteAll(file)` ·
`getOrCreate(file, firstFrameNumber, lastFrameNumber, eventName: String? = null)` ·
`createEventExt(listShotsExt): EventExt?` · `createEventExt(sceneExt): EventExt?` (берёт имя сцены).

#### TrackController.kt (`@Controller @Transactional`)
`getSetTracks(file)` · `getProperties`/`getPropertyValue`/`isPropertyPresent` · `createTracksFromMediaInfo(file)` ·
`create(file)` · `delete(track)` / `deleteAll(file)` · `save`/`saveAll` · `reOrder(type, track)`.
`createTracksFromMediaInfo` — распарсинг JSON от MediaInfo (`MediaInfo.executeMediaInfo(file.path, "--Output=JSON")` + `getFromMediaInfoTracks`), создаёт `Track` на каждую дорожку и **заливает каждый JSON-поле как Property**; вложенные объекты (`LinkedTreeMap`) разворачиваются в плоские ключи.

#### FilterController.kt (`@Controller`)
`getSetFilters(project)` · `getList(projectExt): MutableList<FilterExt>` ·
`getFilterExt(projectExt, filterId: Long)` · `getFilterExt(projectExt, filterName: String)` (создаёт при отсутствии) ·
`deleteFilterExt(projectExt, filterName)` · `create(project, isAnd=true)` · `delete(filter)` / `deleteAll(project)` ·
`save`/`saveAll` · `reOrder(type, filter)`.

#### FilterGroupController.kt (`@Controller`)
`getSetFilterGroups(filter)` · `getList(filterExt): MutableList<FilterGroupExt>` ·
`create(filter, isAnd=true)` · `save`/`saveAll`/`delete(filterGroup)`/`deleteAll(filter)` · `reOrder(type, filterGroup)`.

#### FilterConditionController.kt (`@Controller`)
`getSetFilterConditions(filterGroup)` · `getList(filterGroupExt): MutableList<FilterConditionExt>` ·
`create(filterGroup, name, objectId, objectName, objectValue, objectClass, subjectClass, isIncluded)` ·
`save`/`saveAll`/`delete(filterCondition)`/`deleteAll(filterGroup)` · `reOrder(type, filterCondition)`.

#### FileCdfController.kt (`@Controller`)
`getFileCdf(file): FileCdf` (по `Main.ccid`) · `create(file)` · `save(fileCdf)` · `delete(fileCdf)` · `deleteAll(file)`.

#### ProjectCdfController.kt (`@Controller`)
`getProjectCdf(project): ProjectCdf` · `create(project)` · `save(projectCdf)` · `delete(projectCdf)` · `deleteAll(project)`.

#### PropertyController.kt (`@Controller`)
`getListProperties(parentClass, parentId)` · `getMapKeyValuesByParentClass(parentClass): MutableMap<String, MutableList<String>>` ·
`getOrCreate(parentClass, parentId, key): String` · `editOrCreate(parentClass, parentId, key="", value=""): Property` ·
`delete(property)` / `deleteAll(parentClass, parentId)` · `save`/`saveAll` · `reOrder(type, property)` · `getKeys(parentClass)`.
Это **универсальная EAV-таблица**: `parentClass` — имя класса-владельца, `parentId` — его id. Используется для произвольных свойств Project/File/Shot/Scene/Event/Person/Track/Filter*/Cdf.

#### PropertyCdfController.kt (`@Controller`)
То же, но с `computerId = Main.ccid` во всех запросах:
`getListProperties` · `getOrCreate` · `editOrCreate` · `delete` / `deleteAll(parentClass, parentId)` (заметим: `deleteAll` **не фильтрует по ccid**) · `save`/`saveAll` · `reOrder` · `getKeys(parentClass, computerId)`.

#### ShotTmpCdfController.kt (`@Controller`)
`save(shotTmpCdf)` · `create(shot): ShotTmpCdf` · `deleteAll()` → `Main.shotTmpCdfRepo.deleteAll(Main.ccid)`.

#### ShotTmp2CdfController.kt (`@Controller`)
`save(shotTmp2Cdf)` · `create(shot): ShotTmp2Cdf` (дополнительно `fileId`, `projectId`) · `deleteAll()`.

#### HelloController.kt — `src/main/kotlin/com/svoemesto/ivfx/HelloController.kt`
**Не в папке `controllers/`** (то есть всего там 17 файлов, а не 18). Это шаблонный JavaFX-контроллер из IntelliJ, к проекту отношения не имеет:
```kotlin
class HelloController {
    @FXML private lateinit var welcomeText: Label
    @FXML private fun onHelloButtonClick() { welcomeText.text = "Welcome to JavaFX Application!" }
}
```

### 2.2. Что такое CDF (Computer-Dependent File) — «контроллерная» часть

Идея: путь к рабочим папкам **зависит от машины**, поэтому хранится пара «объект + computer_id».
`ComputerIdentifier.getComputerId()` (utils/ComputerIdentifier.kt) = `hashCode()` от
`OS.manufacturer # CPU.processorID # CPU.identifier # logicalProcessorCount` (через OSHI).

Отсюда `Folders` enum + `PropertyCdfController.getOrCreate(...)` в каждом `getFolderXxx()`.
И временные таблицы `tbl_shot_tmp_cdf` / `tbl_shot_tmp2_cdf` — «одноразовые» списки
выборок, живущие только в пределах текущей машины.

### 2.3. Формат «отдачи» — что реально сериализуется

Три разных формата:

1. **JSON ↔ Python — Gson**, через `FaceExtJson` (вход) и `FaceExt`/`FrameToDetectFaces` (вход/выход).
   `FaceExt` — гибрид: поля-транзиенты + поля с `@SerializedName`:
   ```kotlin
   @Transient var face: Face; @Transient var fileExt: FileExt; @Transient var personExt: PersonExt
   val frameNumber: Int get() = face.frameNumber
   @SerializedName("frameNumber") var toSerializeFrameNumber = frameNumber
   ...
   var vector: DoubleArray
       get() { ... face.vectorText.split("\\|".toRegex()) ... }
       set(value) { face.vectorText = value.joinToString(separator="|"); FaceController.save(face) }
   @SerializedName("vector") var toSerializeVector = vector
   ```
   (т.е. Gson сериализует `toSerialize*`-поля под «настоящими» именами).
2. **JavaFX `ObservableList<…Ext>`** — куда «отдаётся» всё в UI (`table.items = list`).
3. **Entity напрямую** — Spring Data репозитории возвращают `Iterable<Entity>`; наружу
   (в UI/JSON) отдаются «Ext»-обёртки.

### 2.4. Обработка ошибок — её фактически нет

Ни `@ControllerAdvice`, ни `@ExceptionHandler`, ни `ResponseEntity`, ни статусов.
Что есть на месте:

- `try/catch (e: IOException) { e.printStackTrace() }` — в `DetectFaces`, `RecognizeFaces`,
  `CreateFaces`, `doTrainFaceModel` (запись JSON);
- `try/catch (e: IOException) { e.printStackTrace() }` — в `getCdfFolder` (`ProjectController:35`, `FileController:36`) — создание папки;
- `catch (e: NoSuchElementException)`-подобное — `Main.projectRepo.findById(projectId).get()`,
  `Main.fileRepo.findById(fileId).get()`, `Main.personRepo.findById(personId).get()` — **необработанный** `.get()` на пустом Optional (в `getProject`, `getFile`, `getById`);
- `FileController.getFileForShotId` / `getProjectForFileId` — `.first()` без проверки;
- `UpdateListFramesExt` — единственное осмысленное: `try { frameExt.labelSmall } catch (e: IllegalStateException) { break }` (преждевременный выход, если файл кадра удалён);
- `RunCmd` печатает вывод процесса только при ненулевом коде возврата, но вызывающий код результат **игнорирует**.

Итог: сбой Python/БД приводит к тихому «ничего не произошло» в UI.

---

## 3. Механизм отложенной загрузки — `loadlists`

### 3.1. Что это

`src/main/kotlin/com/svoemesto/ivfx/threads/loadlists/` — 13 классов:

```
LoadListProjectsExt, LoadListFilesExt, LoadListFramesExt, LoadListShotsExt,
LoadListScenesExt, LoadListEventsExt, LoadListPersonsExtForProject,
LoadListPersonsExtForFile, LoadListPersonsExtForShot,
LoadListFileFacesExt, LoadListPersonFacesExtForFile, LoadListPersonFacesExtForShot,
LoadListPersonFacesExtForAll
```

### 3.2. Единый шаблон (13 копий одного класса)

```kotlin
class LoadListShotsExt(
    private var list: ObservableList<ShotExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListShotsExt"
        loadList()
        flagIsDone.set(true)
    }

    private fun loadList() {
        Platform.runLater {
            if (pb != null) { pb!!.progress = -1.0; pb!!.isVisible = true }
            if (lbl != null) { lbl!!.text = ...; lbl!!.isVisible = true }
        }
        val sourceIterable = ShotController.getSetShots(fileExt.file).toMutableList()
        sourceIterable.sort()
        list.clear()                                     // ← LazyListObservableList: список обновляется НА МЕСТЕ
        for ((i, shot) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {        // ← кооперативная отмена
                Platform.runLater { ... pb!!.progress = i.toDouble()/sourceIterable.count() ... }
                val shotExt = ShotExt(shot, fileExt,
                    fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber },
                    fileExt.framesExt.first { it.frame.frameNumber == shot.lastFrameNumber })
                shotExt.previewsFirst                    // ← ЛЕНИВАЯ загрузка превью
                shotExt.previewsLast
                list.add(shotExt)
            } else { return }
        }
        Platform.runLater { if (pb!=null) pb!!.isVisible = false; if (lbl!=null) lbl!!.isVisible = false }
    }
}
```

### 3.3. Приём — это НЕ «ленивая порция постранично», а три отдельных механизма

1. **Фоновый поток + `Platform.runLater`.** Запрос к БД и построение объектов — в
   фоновом `Thread`; каждый элемент добавляется в `ObservableList` через `runLater`,
   т.е. **поэлементно в FX-потоке**, список наполняется инкрементально (пользователь
   видит прогресс, а не «зависшее окно»).
2. **Отмена.** Единственная точка проверки — `if (!currentThread().isInterrupted) return`;
   при переключении пользователя поток прерывается, цикл выходит. Никакой
   `@Transactional`-обвязки нет.
3. **Согласованность «список ⇄ БД» через флаг.** Каждому списку соответствует
   `SimpleBooleanProperty flagIsDone`. UI вешает слушателя, который **сбрасывает флаг и
   переприсваивает `table.items = list`**:

   ```kotlin
   LoadListShotsExt(currentFileExt!!.shotsExt, currentFileExt!!, pbShots, null, isDoneLoadListShotsExt).start()
   isDoneLoadListShotsExt.addListener { _, _, newValue ->
       if (newValue == true) { isDoneLoadListShotsExt.set(false); tblShots!!.items = currentFileExt!!.shotsExt }
   }
   ```

### 3.4. Каскадный запуск — «список загрузки» как DAG

`loadlists` образуют цепочку зависимостей, а не набор независимых кнопок. Из
`fxcontrollers/ShotsEditFXController.kt:658-684`:

```kotlin
LoadListPersonsExtForFile(listPersonsExtForFile, currentFileExt!!, pbPersonsForFile, null,
                          isDoneLoadListPersonsExtForFile, false).start()   // без NONPERSON

LoadListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb, isDoneLoadListFramesExt).start()
isDoneLoadListFramesExt.addListener { _, _, newValue ->
    if (newValue == true) {
        isDoneLoadListFramesExt.set(false)
        listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt,
            paneFrames!!.width, paneFrames!!.height, Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
        tblPagesFrames!!.items = listMatrixPageFrames
        UpdateListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb, isDoneUpdateListFramesExt).start()

        LoadListShotsExt(...).start()
        LoadListScenesExt(...).start()
        LoadListEventsExt(...).start()
    }
}
```

То есть: **кадры → (планы, сцены, события)**. Причина прямо в коде `LoadListShotsExt`:
```kotlin
fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber }
```
— если кадры ещё не загружены, будет `NoSuchElementException`. То же в
`LoadListScenesExt` и `LoadListEventsExt`. Т.е. «список загрузки» — это
**упорядоченный асинхронный конвейер** с `flagIsDone` в роли join-точки.

### 3.5. Где ещё

- `LoadListPersonsExtForProject` — в `PersonEditFXController`, `PersonSelectFXController`.
- `LoadListFilesExt` — в `FilterEditFXController`, `ProjectEditFXController`.
- `LoadListFramesExt`/`LoadListShotsExt` — в `CreateShotsCompressedWithAudio`,
  `CreateShotsLosslessWithAudio`, `CreateShotsLosslessWithoutAudio`, `CreateFilterResult`,
  `CreateConcat`, `CreateFacesPreview` — то есть **и потоки обработки тоже грузят списки**
  перед генерацией видео.

---

## 4. Механизм `updatelists`

`src/main/kotlin/com/svoemesto/ivfx/threads/updatelists/` — всего **2 класса**:
`UpdateListFilesExt`, `UpdateListFramesExt`.

Отличие от loadlist — принципиальное: **список не перечитывается из БД**. Он уже в
памяти; обновляются только **ленивые вычисляемые свойства** (`Ext`-геттеры), которые
кэшируются в `_xxx`-полях и при первом обращении делают дисковую/сетевую работу.

`UpdateListFilesExt` — инвалидация кэшей `FileExt` по каждому элементу существующего списка:
```kotlin
for ((i, fileExt) in list.withIndex()) {
    if (!currentThread().isInterrupted) {
        Platform.runLater { ... lbl!!.text = "[%.0f%%] Updating: ${fileExt.file.name} ($i/${list.count()})" }
        fileExt.fps
        fileExt.framesCount
        fileExt.folderPreview
        fileExt.folderLossless
        fileExt.folderFavorites
        fileExt.folderShots
        fileExt.folderFramesSmall
        fileExt.folderFramesMedium
        fileExt.folderFramesFull
        fileExt.pathToLosslessFile
        fileExt.pathToPreviewFile
        fileExt.hasPreview
        fileExt.hasLossless
        fileExt.hasFramesSmall
        fileExt.hasFramesMedium
        fileExt.hasFramesFull
        fileExt.hasAnalyzedFrames
        fileExt.hasCreatedShots
        fileExt.hasDetectedFaces
        fileExt.hasCreatedFaces
        fileExt.hasCreatedFacesPreview
        fileExt.hasRecognizedFaces
    } else { return }
}
```

`UpdateListFramesExt` — то же для кадров, но с защитой от отсутствующего файла:
```kotlin
for ((i, frameExt) in list.withIndex()) {
    if (!currentThread().isInterrupted) {
        Platform.runLater { ... }
        try { frameExt.labelSmall } catch (e: IllegalStateException) { break }
    } else { return }
}
```

### 4.1. Почему это «инкрементально»

Именно ленивые геттеры. `FaceExt.previewSmall` — образец:
```kotlin
val previewSmall: ImageView get() {
    if (_previewSmall == null) {
        var bi: BufferedImage
        if (IOFile(pathToPreviewFile).exists()) {
            bi = ImageIO.read(IOFile(pathToPreviewFile))
        } else {
            if (!IOFile(pathToPreviewFile).parentFile.exists()) IOFile(pathToPreviewFile).parentFile.mkdir()
            val biSource = ImageIO.read(IOFile(FrameController.getFrameExt(...).pathToFull))
            bi = OverlayImage.extractRegion(biSource, startX, startY, endX, endY,
                Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(),
                Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
            if (face.isExample) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.GREEN, 1.0F)
            if (face.isManual)  bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.RED,   1.0F)
            ImageIO.write(bi, "jpg", IOFile(pathToPreviewFile))
        }
        _previewSmall = ImageView(ConvertToFxImage.convertToFxImage(bi))
    }
    return _previewSmall!!
}
```
`resetPreviewSmall() { _previewSmall = null }` — явный сброс кэша.

Обновление инкрементально ещё и в смысле **файловой системы**: `hasDetectedFaces`,
`hasFramesSmall/Medium/Full`, `hasCreatedFacesPreview` считают наличие артефактов
по **количеству файлов, совпавших с regex**:
```kotlin
val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg$")
return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
```
Т.е. «инкрементальность» здесь двойная: (а) не перечитывать БД, а пересчитать
вычисляемые свойства; (б) проверять наличие результата по файловой системе, а не по флагу.

### 4.2. Кто дёргает updatelists

Прямо из колбэка `loadlist` (см. 3.4): `UpdateListFramesExt` стартует сразу после
`LoadListFramesExt`. `UpdateListFilesExt` — по кнопке обновления панели файлов.

---

## 5. Итоговая карта потоков и артефактов

```
frames.json  ←── DetectFaces.kt (Gson, FaceController.getArrayFramesToDetectFaces)
                 │
                 ├──► py detect_faces_in_folder.py -i folderFramesFull -o folderFacesFull
                 │        -d face_detection_model -m openface_nn4.small2.v1.t7 -c 0.3
                 │           ├─▶ <folderFacesFull>\<short>_frame_%06d_face_%02d.jpg   (вырезки)
                 │           └─▶ <folderFramesFull>\faces.json  (128-мерные векторы)
                 │
                 ├──► CreateFaces.kt ──► FaceController.createOrUpdate ──► tbl_faces.vector (TEXT "v|v|v|…")
                 │
tbl_faces(is_example=true) ──► doTrainFaceModel → embeddings.json
                 └──► py train_model_json.py -e embeddings.json -r recognizer.pickle -l le.pickle
                          └─▶ SVC(C=1.0, kernel="linear", probability=True) + LabelEncoder
                                    │
faces.json (personType=UNDEFINDED) ──► RecognizeFaces.kt ──► py recognize_faces.py
                          -i faces.json -r recognizer.pickle -l le.pickle -c 0.3
                                    └─▶ faces.json (personRecognizedName, recognizeProbability)
                                              └──► FaceController.createOrUpdate
                                                       proba > 0.3 ? Person : UNDEFINDED
```

---

## 6. Сводка технических долгов (по прочитанному)

1. Нет REST API вообще; `@Controller` на статических сервисах — misleading.
2. `RunCmd` игнорирует код возврата; ошибка Python = тишина.
3. `recognize_faces.py` грузит 40 МБ моделей и не использует их.
4. Порог `0.3` зашит в Kotlin (4 места), аргумент `-c` в `recognize_faces.py` мёртвый.
5. `detect_faces_in_folder.py:98` — внутренний цикл затирает `i` внешнего счётчика.
6. `FaceController.createOrUpdate` при `frameId != 0L` ищет `findById(faceExtJson.frameId)` вместо `faceId`.
7. Дескрипторы — `@Lob` строка с разделителем: нельзя сравнивать/фильтровать в SQL, полный `SELECT *` всех лиц.
8. Сырой JDBC (`Main.connection.prepareStatement`) в трёх местах мимо JPA.
9. `.get()` / `.first()` на репозиториях без обработки (`getProject`, `getFile`, `getById`, `getFileForShotId`, `getProjectForFileId`).
10. `hasShots*` всегда `false` — заглушки.
11. 13 копий `LoadList*` и 2 копии `UpdateList*` — чистый copy-paste шаблон.
12. `FrameController.createFrames` генерирует 10⁶ строк через 6 `cross JOIN` ради `limit N`.
13. `PersonController.getPersonByProjectIdAndNameInRecognizer` ищет в `project.persons` в памяти (закомментированный SQL) — O(n) на каждое лицо.
14. `FrameController.hasFrames*` использует `name.contains(regex)` вместо `name.matches(regex)` — регулярка без якорей ведёт себя как подстрока.
15. `FaceDetection.FACE_DETECTOR_PATH` через `.substring(1)` — Windows-специфично.
