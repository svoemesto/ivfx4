# ivfx4 — Видео-пайплайн (часть 2 исследования)

Исследованная зона: `src/main/kotlin/com/svoemesto/ivfx/threads/**` и `src/main/kotlin/com/svoemesto/ivfx/utils/**`
плюс то, что эти пакеты вызывают (`controllers/*`, `modelsext/*`, `models/*`, `enums/*`, `fxcontrollers/ProjectActionsFXController.kt`).

Проект: JavaFX + Spring Boot Data JPA + H2 (десктоп), Windows-ориентированный (ffmpeg.exe, `oshi`, `.cmd`, `\`-разделители, Sikuli).

Корень: `/home/nsa/ivfx4`. HEAD = `a3003b7 Фильтры`.

---

## 0. Карта зоны

```
threads/
├── RunCmd.kt                      — запуск .cmd-файла (ProcessBuilder по ассоциации Windows)
├── RunListThreads.kt              — последовательный оркестратор списка Thread
├── projectactions/                — 17 «долгих операций»
│   ├── CreateFramesSmall.kt / CreateFramesMedium.kt / CreateFramesFull.kt
│   ├── CreatePreview.kt
│   ├── CreateLossless.kt
│   ├── AnalyzeFrames.kt
│   ├── CreateShots.kt
│   ├── CreateShotsCompressedWithAudio.kt
│   ├── CreateShotsLosslessWithAudio.kt
│   ├── CreateShotsLosslessWithoutAudio.kt
│   ├── DetectFaces.kt
│   ├── CreateFaces.kt
│   ├── CreateFacesPreview.kt
│   ├── RecognizeFaces.kt
│   ├── CreateConcat.kt
│   └── CreateFilterResult.kt      — единственный, кто НЕ наследует Thread (сигнатура другая)
├── loadlists/                     — 13 «загрузчиков списков» в фоновый поток
│   ├── LoadListProjectsExt, LoadListFilesExt, LoadListFramesExt, LoadListShotsExt,
│   │   LoadListScenesExt, LoadListEventsExt, LoadListFileFacesExt,
│   │   LoadListPersonsExtForProject / ForFile / ForShot,
│   │   LoadListPersonFacesExtForAll / ForFile / ForShot
└── updatelists/
    ├── UpdateListFilesExt.kt      — «прогрев» ленивых полей FileExt
    └── UpdateListFramesExt.kt
```

Все классы в `projectactions/` объявлены как `class X(...) : Thread(), Runnable` и переопределяют `run()`.
`CreateFilterResult` — единственное исключение: `class CreateFilterResult(filterExt, projectExt, shotsExt, fileExt, filename="") : Thread(), Runnable`, сигнатура принципиально другая (не per-file).

---

## 1. Полный список project actions

Порядок в таблице `ProjectActionsFXController` (окно `project-actions-view.fxml`) и порядок в `doActions()`:

| Файл | Класс | Внешние программы | Что создаёт | Таблицы |
|---|---|---|---|---|
| `CreatePreview.kt` | `CreatePreview` | ffmpeg (через bramp) | `Preview/<shortName>/<shortName>_preview.mp4` | — |
| `CreateLossless.kt` | `CreateLossless` | ffmpeg | `Lossless/<shortName>/<shortName>_lossless.mkv` | `tbl_properties` (`@typeorder`) |
| `CreateFramesSmall.kt` | `CreateFramesSmall` | ffmpeg | `Frames_Small/<shortName>/<shortName>_frame_%06d.jpg` | — |
| `CreateFramesMedium.kt` | `CreateFramesMedium` | ffmpeg | `Frames_Medium/<shortName>/..._%06d.jpg` | — |
| `CreateFramesFull.kt` | `CreateFramesFull` | ffmpeg | `Frames_Full/<shortName>/..._%06d.jpg` | — |
| `AnalyzeFrames.kt` | `AnalyzeFrames` | ffprobe (сырой вывод), Sikuli `Finder` | — | `tbl_frames` (через `FrameController.createFrames` = чистый SQL INSERT, и `Main.frameRepo.saveAll`) |
| `CreateShots.kt` | `CreateShots` | — | — | `tbl_shots` (`ShotController.deleteAll` + `save` в цикле) |
| `DetectFaces.kt` | `DetectFaces` | `py …/detect_faces_in_folder.py` (OpenFace) | `<Faces_Full>/<shortName>/*.jpg`, `Frames_Full/<shortName>/frames.json` | — |
| `CreateFaces.kt` | `CreateFaces` | — | — | `tbl_faces` (`FaceController.createOrUpdate`) |
| `CreateFacesPreview.kt` | `CreateFacesPreview` | — | `<Faces_Preview>/<shortName>/..._%06d_face_%02d.jpg` | — |
| `RecognizeFaces.kt` | `RecognizeFaces` | `py …/recognize_faces.py` | правит `Faces_Full/…/faces.json` на месте | `tbl_faces` |
| `CreateShotsCompressedWithAudio.kt` | `CreateShotsCompressedWithAudio` | ffmpeg (по 1 процессу на шот) | `Shots/<shortName>/<shortName>_shot_[…].mp4|mkv|mxf` | `tbl_properties` |
| `CreateShotsLosslessWithAudio.kt` | `CreateShotsLosslessWithAudio` | ffmpeg | `Shots_LL_audioYES/<shortName>/…_audioON.mxf` | `tbl_properties` |
| `CreateShotsLosslessWithoutAudio.kt` | `CreateShotsLosslessWithoutAudio` | ffmpeg | `Shots_LL_audioNO/<shortName>/…_audioOFF.mxf` | `tbl_properties` |
| `CreateConcat.kt` | `CreateConcat` | ffmpeg (`-f concat`) | `Concat/<shortName>.txt` (временно) + `Concat/<shortName>_concat.<ext>` | — |
| `CreateFilterResult.kt` | `CreateFilterResult` | ffmpeg (`-f concat`) | `Filters/<filter> [<f1>-<f2>].<ext>` + временный `.txt` | — |
| — (в `ProjectActionsFXController`, не в projectactions) | `doTrainFaceModel()` | `py …/train_model_json.py` | `<project>/embeddings.json`, `recognizer.pickle`, `le.pickle` | — |

Флаги готовности хранятся в `FileExt` (`hasPreview`, `hasLossless`, `hasFramesSmall/Medium/Full`, `hasAnalyzedFrames`, `hasCreatedShots`, `hasDetectedFaces`, `hasCreatedFaces`, `hasCreatedFacesPreview`, `hasRecognizedFaces`, `hasShotsCompressedWithAudio`, `hasShotsLosslessWithAudio`, `hasShotsLosslessWithoutAudio`, `hasConcat`) — все ленивые геттеры на `FileController.hasXxx(...)`.

---

### 1.1 CreateFramesSmall / Medium / Full

Три почти идентичных файла, отличаются разрешением и `-qscale:v`:

| | Small | Medium | Full |
|---|---|---|---|
| `w × h` | `135 × 75` | `720 × 400` | `1920 × 1080` |
| `-qscale:v` | `1` | `2` | `2` |
| video filter | **нет** | `scale=…,pad=…:black` | `scale=…,pad=…:black` |
| лейбл в UI | «small size 175x35» (опечатка в коде) | «medium size 720x400» | «full size 1920x1080» |

Код (Small):
```kotlin
val fileInput  = fileExt.file.path
if (!IOFile(fileExt.folderFramesSmall).exists()) IOFile(fileExt.folderFramesSmall).mkdir()
val fileOutput = fileExt.folderFramesSmall + IOFile.separator + fileExt.file.shortName + "_frame_%06d.jpg"

val ffmpeg  = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
val ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)
val fFmpegProbeResult = ffprobe.probe(fileInput)
val countFrames = fFmpegProbeResult.streams.firstOrNull { it.codec_type == VIDEO }?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()

val builder = FFmpegBuilder()
    .setInput(fileInput)
    .overrideOutputFiles(true)
    .addOutput(fileOutput)
    .setFrames(countFrames ?: 1)
    .addExtraArgs("-qscale:v", "1")
    .setVideoResolution(135, 75)
    .done()
val job = FFmpegExecutor(ffmpeg, ffprobe).createJob(builder, progressListener)
job.run()
fileExt.hasFramesSmall = true
table.refresh()
```

Фильтр Medium/Full (letterbox-подгонка под целевой кадр):
```kotlin
val filter = if (fileAspect > frameAspect) {
    val frameHeight = (w.toDouble() / fileAspect).toInt()
    "\"scale=" + w + ":" + frameHeight + ",pad=" + w + ":" + h + ":0:" + ((h - frameHeight) / 2.0).toInt() + ":black\""
} else {
    val frameWidth = (h.toDouble() * fileAspect).toInt()
    "\"scale=" + frameWidth + ":" + h + ",pad=" + w + ":" + h + ":" + ((w - frameWidth) / 2.0).toInt() + ":0:black\""
}
```

Прогресс — через `net.bramp.ffmpeg.progress.ProgressListener` (см. §4).

Замечание: `setFrames(countFrames?:1)` ставит `-vframes N` — это ограничение числа кадров, дублирующее «все кадры».

---

### 1.2 CreatePreview

Цель: рабочее превью 720×400, libx264 + AAC 48 кГц стерео, 500 кбит/с.

```kotlin
val fileInput  = fileExt.file.path
val fileOutput = fileExt.pathToPreviewFile          // Preview/<shortName>/<shortName>_preview.mp4
val w = 720; val h = 400
val filter = … scale/pad …

val builder = FFmpegBuilder()
    .setInput(fileInput)
    .overrideOutputFiles(true)
    .addOutput(fileOutput)
    .setVideoResolution(w, h)
    .setVideoBitRate(500000)
    .setVideoCodec("libx264")
    .setAudioCodec("aac")
    .setAudioBitRate(196608)
    .setAudioSampleRate(48000)
    .setAudioChannels(2)
    .setVideoFilter(filter)
    .done()
```

Т.е. превью — единственная операция, жёстко зашитая на libx264/aac/500k (в отличие от CreateShotsCompressedWithAudio, которая берёт кодеки из настроек проекта).

---

### 1.3 CreateLossless

Промежуточное lossless-видео всего файла — **база для всех операций нарезки шотов** (`fileExt.pathToLosslessFile`).

```kotlin
val fileInput  = fileExt.file.path
val fileOutput = fileExt.pathToLosslessFile          // Lossless/<shortName>/<shortName>_lossless.mkv
val w = fileExt.file.project.width   // по умолчанию 1920
val h = fileExt.file.project.height  // по умолчанию 1080

builderOutput.setFilename(fileOutput)
builderOutput.addExtraArgs("-map", "0:v:0")
fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track ->
    var typeOrder = PropertyController.getOrCreate(track::class.java.simpleName, track.id, "@typeorder")
    if (typeOrder == "") typeOrder = "1"
    builderOutput.addExtraArgs("-map", "0:a:${(typeOrder.toInt()) - 1}")
}
builderOutput.setVideoResolution(w, h)

if (project.lossLessContainer == LosslessContainers.MXF.name) {
    builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)      // dnxhd
            .addExtraArgs("-b:v", "36M")
            .setAudioCodec(AudioCodecs.PMC.codec)                  // pcm_s16le
            .setAudioSampleRate(48000)
} else {
    if (project.lossLessCodec == LosslessVideoCodecs.RAW.name) {
        builderOutput.setVideoCodec(LosslessVideoCodecs.RAW.codec)  // rawvideo
                .addExtraArgs("-pix_fmt", "yuv420p")
    } else if (project.lossLessCodec == LosslessVideoCodecs.DNX.name) {
        builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)   // dnxhd
                .addExtraArgs("-b:v", "36M")
    }
    builderOutput.setAudioCodec(AudioCodecs.PMC.codec)
            .setAudioSampleRate(48000)
}
builderOutput.setVideoFilter(filter)   // scale+pad под project.width×height
```

**Замкнутые константы:** `LosslessVideoCodecs.RAW("rawvideo")`, `DNX("dnxhd")`, `AudioCodecs.PMC("pcm_s16le")`, `VideoCodecs.X264("libx264")`, `DNX("dnxhd")`, `AudioCodecs.AAC("aac")/AC3/MP3/PMC`.
`Containers`: `VideoContainers` MP4/MKV/MXF, `LosslessContainers` MP4/MKV/MXF (по умолчанию MKV).

**Найденное расхождение:** контейнер выбирается в `project.lossLessContainer` (и влияет только на ветку MXF vs MKV), но имя выходного файла **всегда** `_lossless.mkv` — захардкожено в `FileController.getLossless()`. Если выбран MXF-контейнер, ffmpeg получает `-f matroska`-эмуляцию по расширению, а `dnxhd`+`pcm_s16le` в MKV технически не пройдёт.

---

### 1.4 CreateShots (чистая БД, без видео)

```kotlin
val listFrames = FrameController.getListFrames(fileExt.file)
ShotController.deleteAll(fileExt.file)          // снос tbl_shots по файлу + properties
for ((i, frame) in listFrames.withIndex()) {
    if (frame.isIFrame) currentIFrame = frame.frameNumber
    currentFrameNumber = frame.frameNumber
    if (frame.isFinalFind || frame == listFrames.last()) {
        lastFrameNumber = currentFrameNumber - if (frame == listFrames.last()) 0 else 1
        val shot = Shot()
        shot.file = fileExt.file
        shot.firstFrameNumber = firstFrameNumber
        shot.lastFrameNumber = lastFrameNumber
        shot.nearestIFrame = previousIFrame
        listShots.add(shot); ShotController.save(shot)
        firstFrameNumber = currentFrameNumber
        previousIFrame = currentIFrame
    }
}
fileExt.hasCreatedShots = true
```

`Shot` (`tbl_shots`): `file_id`, `shot_type_person` (ShotTypePerson: NONE/SGN/OTS/TWO/GRP/MASS), `first_frame_number`, `last_frame_number`, `nearest_i_frame`.

---

### 1.5 CreateShotsCompressedWithAudio

Первый «нарезочный» пайплайн: режет **lossless-файл** на файлы шотов в сжатии, кодеки/битрейты — из проекта.

```kotlin
if (fileExt.framesExt.isEmpty()) LoadListFramesExt(fileExt.framesExt, fileExt, null, null).run()
if (fileExt.shotsExt.isEmpty()) LoadListShotsExt(fileExt.shotsExt, fileExt, null, null).run()

val fileInput = fileExt.pathToLosslessFile            // ← Lossless, не исходник!
val w = fileExt.file.project.width
val h = fileExt.file.project.height
val filter = … scale/pad …

for ((i, shotExt) in fileExt.shotsExt.withIndex()) {
    val fileOutput = shotExt.pathToCompressedWithAudio
    if (!File(fileOutput).exists()) {                       // ← идемпотентность по факту файла
        val firstFrame = shotExt.shot.firstFrameNumber
        val lastFrame  = shotExt.shot.lastFrameNumber
        val framesToCode = lastFrame - firstFrame + 1
        val start = (IvfxFFmpegUtils.getDurationByFrameNumber(firstFrame - 1, frameRate).toDouble() / 1000).toString()

        val builderOutput = FFmpegOutputBuilder()
        val builder = FFmpegBuilder()
        if (firstFrame != 0) builder.addExtraArgs("-ss", start)     // входной seek, до -i
        builder.setInput(fileInput); builder.overrideOutputFiles(true); builder.addOutput(builderOutput)

        builderOutput.setFilename(fileOutput)
        builderOutput.addExtraArgs("-map", "0:v:0")
        fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track -> … "-map","0:a:${typeOrder-1}" }
        builderOutput.addExtraArgs("-vframes", framesToCode.toString())
        builderOutput.setVideoResolution(w, h)
        builderOutput.setVideoCodec(VideoCodecs.valueOf(project.videoCodec).codec)
        builderOutput.setVideoBitRate(project.videoBitrate.toLong())
        builderOutput.setAudioCodec(AudioCodecs.valueOf(project.audioCodec).codec)
        builderOutput.setAudioBitRate(project.audioBitrate.toLong())
        builderOutput.setAudioSampleRate(project.audioFrequency)
        builderOutput.setVideoFilter(filter)

        FFmpegExecutor(ffmpeg).createJob(builder).run()          // БЕЗ ProgressListener
    }
}
fileExt.hasShotsCompressedWithAudio = true
```

`start` = секунды от начала файла до кадра `firstFrame-1`:
```kotlin
fun getDurationByFrameNumber(frameNumber: Int, fps: Double): Int {
    val dur1fr = 1000 / fps
    var durDouble = (frameNumber - 0) * dur1fr
    if (durDouble < 0) durDouble = 0.0
    return floor(durDouble).toInt()
}
```

Имя файла шота (`ShotExt`):
```kotlin
val start  = convertDurationToString(getDurationByFrameNumber(firstFrameNumber - 1, fps))   // "0:00:03.125"
val end    = convertDurationToString(getDurationByFrameNumber(lastFrameNumber, fps))
val filenameWithoutExt = "${fileExt.file.shortName}_shot_[${start.replace(":",".")}-${end.replace(":",".")}]-(${firstFrameNumber}-${lastFrameNumber})"
val pathToCompressedWithAudio = "${folderShotsCompressedWithAudio}\\${filenameWithoutExt}.${VideoContainers.valueOf(project.container).extention}"
```

**Важные особенности:**
- Здесь `start` = `…toDouble()/1000` → в ffmpeg уходит **секунды** (`"-ss", "3.125"`), в отображаемом имени — `мм:сс.ммм`.
- Прогресса ffmpeg нет: `createJob(builder)` без слушателя; прогресс = `((i+1)/shotsExt.size)` по шотам.
- Пропуск уже существующих файлов → повторный запуск дёшево «ничего не делает».
- Порядок прогресса: `pb1` = глобальный, `pb2` = «шот i из N».

---

### 1.6 CreateShotsLosslessWithAudio / CreateShotsLosslessWithoutAudio

Отличия от сжатого варианта (всё остальное идентично — тот же цикл, тот же `-ss`, тот же `-vframes`, та же map-логика):

**WithAudio** (`Shots_LL_audioYES/<shortName>/…_audioON.mxf`):
```kotlin
builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)   // dnxhd
builderOutput.addExtraArgs("-b:v", "36M")
builderOutput.setAudioCodec(AudioCodecs.PMC.codec)          // pcm_s16le
builderOutput.setAudioSampleRate(48000)
// builderOutput.setVideoFilter(filter)   ← ЗАКОММЕНТИРОВАНО: без ресайза!
```
Контейнер MXF зашит (`"_audioON.mxf"`).

**WithoutAudio** (`Shots_LL_audioNO/<shortName>/…_audioOFF.mxf`):
```kotlin
builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)
builderOutput.addExtraArgs("-b:v", "36M")
// builderOutput.setAudioCodec(AudioCodecs.PMC.codec)
// builderOutput.setAudioSampleRate(48000)
// builderOutput.setVideoFilter(filter)
```
**Баг/несоответствие имени:** блок `-map 0:a:<n>` в этой версии **не закомментирован** — аудиодорожки всё равно мапятся в mxf-файл, просто без заданного аудиокодека. То есть «без аудио» не гарантировано.

**Баг/несоответствие в обоих:** `setVideoResolution(w, h)` вызывается (`-s 1920x1080`), но `setVideoFilter` закомментирован — если lossless-файл уже 1920×1080, всё ок; иначе ffmpeg будет масштабировать сам (а не pad-ить в чёрные поля), т.е. геометрия кадра «поедет» относительно CreateLossless.

---

### 1.7 DetectFaces → CreateFaces → RecognizeFaces (конвейер OpenFace)

Три класса образуют цепочку через два JSON-файла в папке `Frames_Full`.

**DetectFaces** (`DetectFaces.kt`):
```kotlin
val pathToFileJSON = fileExt.folderFramesFull + IOFile.separator + "frames.json"
fileExt.file.shots = ShotController.getSetShots(fileExt.file)
val arrFrameToDetectFaces: Array<FaceController.FrameToDetectFaces> =
        FaceController.getArrayFramesToDetectFaces(fileExt)
FileWriter(pathToFileJSON).use { gson.toJson(arrFrameToDetectFaces, it) }

val param = mutableListOf(
    "${faceDetectorPath.first()}:\n", "cd \"${faceDetectorPath}\"\n", "py",
    "\"${faceDetectorPath}/detect_faces_in_folder.py\"", "-i", "\"${fileExt.folderFramesFull}\"",
    "-o", "\"${fileExt.folderFacesFull}\"", "-d", "\"${faceDetectorPath}/face_detection_model\"",
    "-m", "\"${faceDetectorPath}/openface_nn4.small2.v1.t7\"", "-c", "0.3")
val cmdText = param.joinToString(" ").replace("/", "\\")
val runCmd = RunCmd(cmdText); runCmd.run()
```

Дискретизация кадров для детекции (`FaceController.getFramesToRecognize`): шаг зависит от длины шота — **3 / 5 / 10 / 20** кадров для шотов короче 15 / 30 / 60 / длиннее 60.

`frames.json` пишется, но в команду **не передаётся** — скрипту передаётся каталог. Файл мёртвый (видимо, наследие).

**CreateFaces** (`CreateFaces.kt`): читает `Frames_Full/<shortName>/faces.json` (его пишет Python-скрипт детекции) как `Array<FaceExtJson>` и для каждого вызывает
`FaceController.createOrUpdate(faceExtJson, fileExt, undefindedPerson, nonPerson)` → строки в `tbl_faces`.
На этом шаге `personRecognizedName == ""` → всё уходит персоне «UNDEFINDED».

**RecognizeFaces** (`RecognizeFaces.kt`): собирает `FaceController.getListFacesExtToRecognize(fileExt)` (только неопознанные), обнуляет персон, пишет `Faces_Full/…/faces.json`, запускает распознавание, читает файл обратно и снова `createOrUpdate`:
```
cd "<faceDetectorPath>"
py "<faceDetectorPath>/recognize_faces.py" -i "<Faces_Full…/faces.json>" -d "<.../face_detection_model>" \
   -m "<.../openface_nn4.small2.v1.t7>" -r "<project.folder>/recognizer.pickle" -l "<project.folder>/le.pickle" -c 0.3
```
Порог присвоения персоне — `faceExtJson.recognizeProbability > 0.3`, иначе `UNDEFINDED`; при аспект-отношении лица `d > 4` → `NONPERSON`.

**Обучение модели** (`ProjectActionsFXController.doTrainFaceModel`): пишет `<project.folder>/embeddings.json` (`{embeddings, names}`), вызывает
`py "<...>/train_model_json.py" -e "<project>/embeddings.json" -r "<project>/recognizer.pickle" -l "<project>/le.pickle"`.

**CreateFacesPreview** (`CreateFacesPreview.kt`): чисто на Java2D/ImageIO, без внешних программ.
```kotlin
val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
var bi = OverlayImage.extractRegion(biSource, faceExt.startX, faceExt.startY, faceExt.endX, faceExt.endY,
                Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(),
                Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
if (faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.GREEN, 1.0F)
if (faceExt.face.isManual)  bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.RED,   1.0F)
ImageIO.write(bi, "jpg", IOFile(faceExt.pathToPreviewFile))
```
Константы (`Main.kt`): `PREVIEW_FACE_W=75.0`, `PREVIEW_FACE_H=75.0`, `PREVIEW_FACE_EXPAND_FACTOR=1.4`, `PREVIEW_FACE_CROPPING=false`, `PREVIEW_FRAME_W/H=135/75`, `MEDIUM_FRAME_W/H=720/400`, `FULL_FRAME_W/H=1920/1080`.

`RunCmd` — это НЕ ProcessBuilder-список аргументов, а запись текста во временный `ivfx*.cmd` и запуск этого файла как «исполняемого» (Windows-ассоциация). На Linux не сработает вовсе.

---

### 1.8 CreateConcat / CreateFilterResult — конкатенация

**CreateConcat** — склейка файлов шотов в один файл файла:
```kotlin
if (!File(fileExt.folderConcat!!).exists()) File(fileExt.folderConcat!!).mkdir()

var concatFiles = ""
fileExt.shotsExt.forEach { concatFiles += "file '${it.pathToCompressedWithAudio}'\n" }
val fileInput = "${fileExt.folderConcat}${File.separator}${fileExt.file.shortName}.txt"
… FileWriter(fileInput).write(concatFiles) …

val fileOutput = FileController.getConcat(fileExt)   // Concat/<shortName>_concat.<ext>

val builderOutput = FFmpegOutputBuilder(); builderOutput.setFilename(fileOutput)
val builder = FFmpegBuilder()
    .setInput(fileInput)
    .addExtraArgs("-f", "concat")
    .addExtraArgs("-safe", "0")
    .overrideOutputFiles(true)
    .addOutput(builderOutput)

builderOutput.addExtraArgs("-map", "0:v:0")
fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { … "-map","0:a:${typeOrder-1}" }
builderOutput.addExtraArgs("-c", "copy")

FFmpegExecutor(ffmpeg).createJob(builder).run()
if (File(fileInput).exists()) File(fileInput).delete()
fileExt.hasConcat = true
```

Итоговая команда (см. §3.5) — чистый **remux без перекодирования** (`-c copy`), поэтому сорца вырезается по длине шотов, а не по кадрам (микро-расхождения на стыках).

**CreateFilterResult** — то же, но список шотов задаётся пользователем через `FilterEditFXController`, а выход идёт в `Filters/`:
```kotlin
shotsExt.forEach { concatFiles += "file '${it.pathToCompressedWithAudio}'\n" }
if (filename == "")
    filename = "${projectExt.folderFilters}\\${filterExt.filter.name} [${shotsExt.first().fileExt.file.shortName}-${shotsExt.last().fileExt.file.shortName}].${VideoContainers.valueOf(projectExt.project.container).extention}"
```
Тот же concat-demuxer, `-map 0:v:0`, `-map 0:a:<typeOrder-1>`, `-c copy`.

---

## 2. Система папок

### 2.1 Двухуровневая модель: проект → файл

`Folders` (`enums/Folders.kt`) — 15 значений, у каждого `propertyCdfKey`, `folderName`, флаги `forProject`/`forFile`:

```kotlin
enum class Folders(val propertyCdfKey: String, val folderName: String,
                   val forProject: Boolean, val forFile: Boolean) {
    LOSSLESS("folder_lossless", "Lossless", true, true),
    PREVIEW("folder_preview", "Preview", true, true),
    SHOTS("folder_shots", "Shots", true, true),
    FAVORITES("folder_favorites", "Favorites", true, true),
    FRAMES_SMALL("folder_frames_small", "Frames_Small", true, true),
    FRAMES_MEDIUM("folder_frames_medium", "Frames_Medium", true, true),
    FRAMES_FULL("folder_frames_full", "Frames_Full", true, true),
    FACES_FULL("folder_faces_full", "Faces_Full", true, true),
    FACES_PREVIEW("folder_faces_preview", "Faces_Preview", true, true),
    PERSONS("folder_persons", "Persons", true, false),
    SHOTS_COMPRESSED_WITH_AUDIO("folder_shots_compressed_with_audio", "Shots", true, true),
    SHOTS_LOSSLESS_WITH_AUDIO("folder_shots_lossless_with_audio", "Shots_LL_audioYES", true, true),
    SHOTS_LOSSLESS_WITHOUT_AUDIO("folder_shots_lossless_without_audio", "Shots_LL_audioNO", true, true),
    CONCAT("folder_concat", "Concat", true, true),
    FILTERS("folder_filters", "Filters", true, false)
}
```

Разрешение пути — двухшаговое, всегда с оверрайдом «на текущий компьютер»:

**Шаг 1, уровень проекта** (`ProjectController.getFolderXxx(project)`):
```kotlin
val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.X.propertyCdfKey)
return if (value == "") project.folder + IOFile.separator + Folders.X.folderName else value
```
`project.folder` — геттер `Project.folder`:
```kotlin
var folder: String
    get() = cdfs.firstOrNull { it.computerId == Main.ccid }?.folder ?: ""
```

**Шаг 2, уровень файла** (`FileController.getFolderXxx(fileExt)`):
```kotlin
val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.X.propertyCdfKey)
return if (value == "") fileExt.projectExt.folderX + IOFile.separator + fileExt.file.shortName else value
```

`Main.ccid` = `ComputerIdentifier.getComputerId()` — `hashCode()` от `manufacturer#processorID#identifier#logicalProcessorCount` (библиотека OSHI). То есть `PropertyCdf` («computer-dependent folder») позволяет хранить в БД отдельный путь для каждой машины — проект/файл можно открыть с сетевого диска, и пути не «поедут».

`Folder` для `CONCAT` и `FILTERS` — `forFile=false` (создаётся только на уровне проекта); `PERSONS` — только проект.

### 2.2 Полная иерархия (дефолт, project.folder = `<корень>`)

```
<project.folder>/
│
├─ Lossless/                                   LOSSLESS
│   └─ <file.shortName>/
│       └─ <shortName>_lossless.mkv            ← CreateLossless (источник для нарезки шотов)
│
├─ Preview/                                    PREVIEW
│   └─ <file.shortName>/
│       └─ <shortName>_preview.mp4             ← CreatePreview (libx264/aac 720×400)
│
├─ Shots/                                      SHOTS **и** SHOTS_COMPRESSED_WITH_AUDIO
│   ├─ <file.shortName>/                       ← папка шотов пользователя (Favorites-подобная)
│   └─ <file.shortName>/
│       └─ <shortName>_shot_[h.mm.sss-h.mm.sss]-(first-last).{mp4|mkv|mxf}
│                                             ← CreateShotsCompressedWithAudio
│
├─ Favorites/                                  FAVORITES
│   └─ <file.shortName>/
│
├─ Frames_Small/                               FRAMES_SMALL
│   └─ <file.shortName>/
│       └─ <shortName>_frame_000001.jpg …      ← CreateFramesSmall (135×75, qscale 1)
│
├─ Frames_Medium/                              FRAMES_MEDIUM
│   └─ <file.shortName>/
│       └─ <shortName>_frame_000001.jpg …      ← CreateFramesMedium (720×400, qscale 2)
│
├─ Frames_Full/                                FRAMES_FULL
│   └─ <file.shortName>/
│       ├─ <shortName>_frame_000001.jpg …      ← CreateFramesFull (1920×1080)
│       ├─ frames.json                         ← DetectFaces (список кадров; в скрипт не идёт)
│       └─ faces.json                          ← DetectFaces/RecognizeFaces (обмен с OpenFace)
│
├─ Faces_Full/                                 FACES_FULL
│   └─ <file.shortName>/
│       └─ <shortName>_frame_000123_face_00.jpg …   ← detect_faces_in_folder.py
│
├─ Faces_Preview/                              FACES_PREVIEW
│   └─ <file.shortName>/
│       └─ <shortName>_frame_000123_face_00.jpg …   ← CreateFacesPreview (75×75, expand 1.4, jpg)
│
├─ Persons/                                    PERSONS (только проект)
│   └─ <personId>/ … (фото персон)
│
├─ Shots_LL_audioYES/                          SHOTS_LOSSLESS_WITH_AUDIO
│   └─ <file.shortName>/
│       └─ <shortName>_shot_[…]-(first-last)_audioON.mxf     ← CreateShotsLosslessWithAudio
│
├─ Shots_LL_audioNO/                           SHOTS_LOSSLESS_WITHOUT_AUDIO
│   └─ <file.shortName>/
│       └─ <shortName>_shot_[…]-(first-last)_audioOFF.mxf    ← CreateShotsLosslessWithoutAudio
│
├─ Concat/                                     CONCAT (БЕЗ подпапки shortName!)
│   ├─ <file.shortName>.txt                     ← список concat (временный, удаляется)
│   └─ <file.shortName>_concat.{mp4|mkv|mxf}   ← CreateConcat (remux -c copy)
│
├─ Filters/                                    FILTERS (только проект)
│   └─ "<filterName> [<short1>-<short2>].<ext>" ← CreateFilterResult
│       (рядом временный "<…>.txt")
│
├─ recognizer.pickle                           ← модель распознавания лиц (обученная)
├─ le.pickle                                   ← label-encoder
└─ embeddings.json                             ← обучающая выборка (ProjectActionsFXController)
```

**Особенности иерархии:**

1. **`Shots` дублируется.** И `Folders.SHOTS`, и `Folders.SHOTS_COMPRESSED_WITH_AUDIO` имеют `folderName = "Shots"`. На уровне проекта оба дают `<project>/Shots`; на уровне файла оба дают `<project>/Shots/<shortName>`. Т.е. «пользовательские папки шотов» и «нарезанные видеофайлы шотов» физически делят каталог (различаются только propertyCdf-ключами, позволяющими развести их через UI).

2. **`Concat` не имеет подпапки файла.** `FileController.getFolderConcat` возвращает `projectExt.folderConcat` без `+ shortName` (в отличие от всех остальных). Различение — по префиксу `<shortName>_` в именах.

3. **`Frames_Full` — «конверт» для JSON.** `frames.json` и `faces.json` кладутся прямо в папку кадров файла, а `CreateFacesPreview` читает оттуда же кадры (`frameExt.pathToFull`).

4. **Оверрайды.** Любой путь можно переопределить в UI через `PropertyCdf` (таблица `tbl_property_cdf`), привязанный к `(parentClass, parentId, computerId=Main.ccid, key=folder_xxx)`. Значение создаётся лениво через `PropertyCdfController.getOrCreate` при первом обращении. Пустое значение = «использовать дефолт».

5. **Папки создаются лениво и в одном уровне.** `IOFile(folder).mkdir()` — не `mkdirs()`, и вызывается в каждом project action только для своей папки (уровень проекта). Т.е. при первом запуске `CreateFramesSmall` пользователь должен один раз создать `Frames_Small` (или он должен существовать); вложенная `<shortName>` создаётся внутри `mkdir`-вызова на уровне файла.

---

## 3. Вызов внешних программ

### 3.1 Три разных механизма

| Механизм | Где | Класс/функция | Парсинг вывода |
|---|---|---|---|
| **bramp `FFmpegWrapper` (ProcessBuilder внутри библиотеки)** | 12 project actions | `FFmpeg(FFMPEG_PATH)`, `FFmpegExecutor`, `FFmpegBuilder`, `FFmpegJob.run()` | Не парсится — прогресс через `-progress tcp://localhost:<port>` + `ProgressListener` |
| **bramp `FFprobe`** | `IvfxFFmpegUtils.getListIFrames`, `FileController.getFFmpegProbeResult/getFps/getFramesCount` | `FFprobe(FFPROBE_PATH).probe(path)` | JSON-десериализация внутри bramp |
| **сырой `ProcessBuilder`** | `IvfxUtils.executeExe`, `MediaInfo.executeMediaInfo`, `RunCmd.exec` | верхнеуровневые функции | regex / Gson |

**Пути к бинарям** (`IvfxFFmpegUtils`):
```kotlin
val FFMPEG_PATH  = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffmpeg.exe")?.path ?: ""
val FFPROBE_PATH = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffprobe.exe")?.path ?: ""
val FFPLAY_PATH  = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffplay.exe")?.path ?: ""   // нигде не используется
```
Ресурсы бандлятся внутрь JAR (`.exe`), т.е. ffmpeg — **портативный, зашитый в приложение**.

**`MediaInfo`** (`utils/MediaInfo.kt`) — отдельный исполняемый файл `MediaInfo_CLI/MediaInfo.exe`, вызывается через `ProcessBuilder`:
```kotlin
val param = mutableListOf(parameter, media)   // параметр ПЕРЕД файлом
builder.redirectErrorStream(true)
val process = builder.start()
… InputStreamReader(process.inputStream).use { … } …   // читает ПОБАЙТОВО через reader.read()
process.waitFor()
return out.substring(0, out.length - 2)               // отрезает 2 последних символа
```
Реально используется один вызов: `MediaInfo.executeMediaInfo(file.path, "--Output=JSON")` из `TrackController.createTracksFromMediaInfo(file)` — при создании файла и по кнопке в UI. Разбор — Gson в `getFromMediaInfoTracks(json)` → `media.track`. Так формируется `tbl_tracks` (и `file.tracks`, откуда берётся `type == "Audio" && use` и `@typeorder`).

**`RunCmd`** (`threads/RunCmd.kt`) — для запуска Python-скриптов OpenFace:
```kotlin
val cmdFile = IOFile.createTempFile("ivfx", ".cmd")
BufferedWriter(FileWriter(cmdFile)).apply { write(cmdText); flush(); close() }
exec(cmdFile.absolutePath)
cmdFile.deleteOnExit()

private fun exec(cmd: String): Int {
    val pb = ProcessBuilder(cmd); pb.redirectErrorStream(true)
    val p = pb.start(); p.outputStream.close()
    val baos = ByteArrayOutputStream(); copy(p.inputStream, baos)
    val r = p.waitFor()
    if (r != 0) println(cmd + " cmd: output:\n" + baos)
    return r
}
```
`ProcessBuilder` вызывается с **одним** аргументом — путём к `.cmd`. На Windows `CreateProcess` не запускает `.cmd` напрямую, но `ProcessBuilder(String)` на Windows умеет запускать ассоциированные документы через `ShellExecute`. Код зависит от Windows-семантики.

### 3.2 ffprobe — все вызовы

`IvfxFFmpegUtils.getListIFrames` (используется только `AnalyzeFrames`):
```kotlin
param.add("-skip_frame");   param.add("nokey");
param.add("-select_streams"); param.add("v");
param.add("-show_frames");  param.add(mediaFile);
executeExe(FFPROBE_PATH, param)
```
Итоговая команда:
```
ffprobe -skip_frame nokey -select_streams v -show_frames <mediaFile>
```
Парсинг (хрупкий, **жёстко завязан на CRLF**):
```kotlin
val regExp = "(?<=\\[FRAME\\]\\r\\n)[\\w|\\W]+?(?=\\[/FRAME\\]\\r\\n)"
… внутри блока ищем строку "pkt_pts=…" → list.add(getFrameNumberByDuration(findedResult, fps))
```
`getFrameNumberByDuration(duration, fps) = round(duration / (1000/fps) + 1)`.
На Linux (где ffprobe печатает `\n`) regex не сматчится → пустой список I-кадров.

`FFprobe.probe()` (bramp) используется ещё в `FileController.getFps/getFramesCount/getFFmpegProbeResult` — там вывод парсится как JSON.

### 3.3 Уникальные ffmpeg-команды (реконструкция полной командной строки)

Порядок аргументов восстановлен из байткода bramp-библиотеки `0.6.2` (`~/.m2/repository/net/bramp/ffmpeg/ffmpeg/0.6.2/ffmpeg-0.6.2.jar`, javap):

```
FFmpegBuilder.build():
  [ffmpeg] -y (при overrideOutputFiles) -v error (verbosity по умолчанию = ERROR)
  [user_agent] [startOffset -ss] [format -f] [-re]
  [-progress tcp://localhost:PORT]      ← добавляется автоматически, если передан ProgressListener
  <extra_args билдера>                   ← ВСЕГДА перед -i
  -i <input>
  [pass]
  [complexFilter -filter_complex]
  [-af <builder.videoFilter>]           ← БАГ bramp: setVideoFilter на FFmpegBuilder → -af
  [-vf <builder.audioFilter>]           ← БАГ bramp: setAudioFilter на FFmpegBuilder → -vf
  <outputs: AbstractFFmpegStreamBuilder.build() + FFmpegOutputBuilder.build()>

AbstractFFmpegStreamBuilder.build():  -strict.. | -vn | -acodec .. -ac .. -ar .. -apre .. | -an | -spre.. | -sn
                                        | <output.extra_args> | <output.filename>
FFmpegOutputBuilder.build():          | -crf | -b:v | -qscale:v | -vpre | -vf | -bsf:v | -sample_fmt | -b:a | -qscale:a | -bsf:a | -af
```

Проверено: verbosity по умолчанию = `Verbosity.ERROR`, т.е. все команды содержат `-v error`.

**Уникальные команды (12 штук), собранные из всех project actions:**

```bash
# (1) CreateFramesSmall
ffmpeg -y -v error -progress tcp://localhost:PORT -qscale:v 1 \
       -i <file> -vframes <N> -s 135x75 <Frames_Full>/…/<shortName>_frame_%06d.jpg

# (2) CreateFramesMedium
ffmpeg -y -v error -progress tcp://localhost:PORT -qscale:v 2 \
       -i <file> -vframes <N> -s 720x400 -af "scale=720:H,pad=720:400:0:Y:black" <…_frame_%06d.jpg>

# (3) CreateFramesFull  — то же, -s 1920x1080
ffmpeg -y -v error -progress tcp://localhost:PORT -qscale:v 2 \
       -i <file> -vframes <N> -s 1920x1080 -af "scale=W:1080,pad=1920:1080:X:0:black" <…_frame_%06d.jpg>

# (4) CreatePreview
ffmpeg -y -v error -progress tcp://localhost:PORT \
       -i <file> -vcodec libx264 -s 720x400 -acodec aac -ar 48000 -ac 2 \
       -af "scale=720:H,pad=720:400:0:Y:black" -b:v 500000 -b:a 196608 \
       <Preview/…/<shortName>_preview.mp4>

# (5) CreateLossless  (MKV, DNX)
ffmpeg -y -v error -progress tcp://localhost:PORT \
       -i <file> -vcodec dnxhd -s 1920x1080 -acodec pcm_s16le -ar 48000 \
       -map 0:v:0 -map 0:a:<typeOrder-1> \
       -b:v 36M -vf "scale=W:H,pad=1920:1080:X:Y:black" <Lossless/…/<shortName>_lossless.mkv>

# (5a) CreateLossless  (MKV, RAW)
… -vcodec rawvideo … -pix_fmt yuv420p -b:v 36M -vf …

# (6) CreateShotsCompressedWithAudio
ffmpeg -y -v error -ss <start_sec> \
       -i <Lossless/…/_lossless.mkv> -vcodec libx264 -s 1920x1080 \
       -acodec aac -ar 48000 -map 0:v:0 -map 0:a:<typeOrder-1> \
       -vframes <framesToCode> -b:v <project.videoBitrate> -b:a <project.audioBitrate> \
       -vf "scale=W:H,pad=1920:1080:X:Y:black" <Shots/…/<shot>.mp4>

# (7) CreateShotsLosslessWithAudio
ffmpeg -y -v error -ss <start_sec> \
       -i <Lossless/…/_lossless.mkv> -vcodec dnxhd -s 1920x1080 \
       -acodec pcm_s16le -ar 48000 \
       -map 0:v:0 -map 0:a:<typeOrder-1> -vframes <framesToCode> -b:v 36M <…_audioON.mxf>

# (8) CreateShotsLosslessWithoutAudio  (то же, но БЕЗ -acodec/-ar; -map 0:a: остаётся!)
ffmpeg -y -v error -ss <start_sec> \
       -i <Lossless/…/_lossless.mkv> -vcodec dnxhd -s 1920x1080 \
       -map 0:v:0 -map 0:a:<typeOrder-1> -vframes <framesToCode> -b:v 36M <…_audioOFF.mxf>

# (9) CreateConcat
ffmpeg -y -v error -f concat -safe 0 \
       -i <Concat/…/<shortName>.txt> \
       -map 0:v:0 -map 0:a:<typeOrder-1> -c copy <Concat/…/<shortName>_concat.<ext>

# (10) CreateFilterResult  — идентична (9), но вход/выход в Filters/
ffmpeg -y -v error -f concat -safe 0 \
       -i <Filters/…/"<filter> [<f1>-<f2>].<ext>.txt"> \
       -map 0:v:0 -map 0:a:<typeOrder-1> -c copy <Filters/…/"<filter> [<f1>-<f2>].<ext>">
```

`FFPLAY_PATH` объявлен, но нигде не используется — плеера на базе ffplay в проекте нет.

### 3.4 Парсинг вывода ffmpeg/ffprobe

- **bramp-прогресс:** `FFmpegExecutor.createJob(builder, listener)` → `FFmpeg.run(builder, listener)` → `builder.addProgress(URI)`. В байткоде `FFmpeg.createProgressParser` инстанцирует `TcpProgressParser` — т.е. ffmpeg получает `-progress tcp://localhost:<ephemeral-port>`, поднимается локальный сокет, и ffmpeg шлёт туда блоки `frame=…/fps=…/out_time_ns=…/speed=…`. Приложение их парсит в `Progress` и зовёт `ProgressListener.progress(progress)`. Никакого парсинга stderr вручную нет.

- **Прогресс-формат в UI:**
```kotlin
val percentage2 = progress.out_time_ns / duration_ns
val textLbl2 = String.format("[%.0f%%] status: %s, frame: %d, time: %s ms, fps: %.0f, speed: %.2fx",
        percentage2*100, progress.status, progress.frame,
        FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS), progress.fps.toDouble(), progress.speed)
```
- **Прогресс без ffmpeg-событий** (CreateShots*, CreateConcat, CreateFilterResult, CreateFaces, CreateFacesPreview, DetectFaces, RecognizeFaces): считается вручную по индексу цикла; `DetectFaces`/`RecognizeFaces` ставят `ProgressBar.INDETERMINATE_PROGRESS`.

### 3.5 Точная команда concat (дословно, §6)

Входной `.txt` (создаётся `BufferedWriter(FileWriter(...))`, кодировка — платформенная):
```
file '<abs>/Shots/<shortName>/<shortName>_shot_[0.00.04.167-0.00.07.125]-(1-73).mp4'
file '<abs>/Shots/<shortName>/<shortName>_shot_[0.00.07.125-0.00.11.667]-(74-193).mp4'
…
```
Команда:
```
ffmpeg -y -v error -f concat -safe 0 -i <Concat/…/<shortName>.txt> \
       -map 0:v:0 -map 0:a:<typeOrder-1> -c copy <Concat/…/<shortName>_concat.<ext>
```
`-safe 0` — потому что пути абсолютные (иначе concat отверг бы их). `-c copy` — без перекодирования: обрезка ровно по длине, что вносит смещение на уровне GOP.

---

## 4. Многопоточность

### 4.1 Никакого пула потоков

Во всём проекте **нет** `ExecutorService`, `Executors.*`, `ThreadPool` — проверено grep-ом. Вся «параллельность» — на голых `java.lang.Thread`.

### 4.2 RunListThreads — последовательный оркестратор

```kotlin
class RunListThreads(private val listThreads: List<Thread>,
                     private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)) : Thread(), Runnable {
    override fun run() {
        this.name = "RunListThreads"
        var runningThread: Thread? = null
        var countStartedThreads = 0
        while (countStartedThreads != listThreads.size && !currentThread().isInterrupted) {
            if (runningThread == null) {
                runningThread = listThreads[countStartedThreads]; countStartedThreads++
                runningThread.isDaemon = false; runningThread.start()
            } else {
                while (runningThread.isAlive) {
                    try { sleep(100) } catch (e: InterruptedException) { runningThread.interrupt(); return }
                }
                runningThread = listThreads[countStartedThreads]; countStartedThreads++
                runningThread.isDaemon = false; runningThread.start()
            }
        }
        if (runningThread != null) while (runningThread.isAlive) {
            try { sleep(100) } catch (e: InterruptedException) { runningThread.interrupt(); return }
        }
        println("RunListThreads DONE"); flagIsDone.set(true)
    }
}
```

Ключевое: **`RunListThreads` не параллелит — он выполняет потоки строго последовательно**, опрашивая `isAlive` с `sleep(100)`. Список — это «очередь», а не набор одновременных задач. Это единственный «пул» в проекте, и он последовательный.

Ошибка (неочевидная): если `listThreads.size == 0`, цикл while не выполняется, `runningThread` остаётся null, но `flagIsDone.set(true)` всё равно вызывается — ок. Однако при прерывании (`return` в catch) `flagIsDone` **не выставляется** → UI-окно может остаться с «крутящимся» прогрессом навсегда.

### 4.3 Отслеживание прогресса — двухуровневый

Каждый project action получает `numCurrentThread` (порядковый номер задачи) и `countThreads` (всего задач):

```kotlin
val initProgress1      = (numCurrentThread - 1) / countThreads.toDouble()   // доля «до меня»
val onePeaceOfProgress = 1 / countThreads.toDouble()                        // мой кусок
val percentage2        = <мой локальный 0..1>
val percentage1        = initProgress1 + onePeaceOfProgress * percentage2   // глобальный 0..1
```
Два `ProgressBar` в UI: `pb1` = глобальный, `pb2` = локальный для текущей операции. Все обновления — через `Platform.runLater { }` (JavaFX-нить).

Заполнение в `ProjectActionsFXController.doActions`:
```kotlin
counterPb1++
listThreads.add(CreateFramesSmall(fileExt, tblFilesExt,
    "File: ${fileExt.file.name}, Action: Create Frames (small size 175x35), Issue: [${counterPb1}/${countActions}]",
    counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!))
…
RunListThreads(listThreads).start()
```
`countActions` считается заранее двойным проходом по выбранным файлам (с учётом чекбокса `checkReCreateIfExists`).

### 4.4 Отмена

- `ProjectEditFXController.kt:451` — `runListThreadsFiles!!.interrupt()` (кнопка отмены в редакторе проекта).
- `RunListThreads` ловит `InterruptedException` от `sleep(100)` → `runningThread.interrupt()` → `return` (не завершает очередь).
- `LoadList*` / `UpdateList*` — единственные классы, реально проверяющие отмену: `if (!currentThread().isInterrupted) { … } else return` внутри цикла по элементам.
- **Project actions отмену не проверяют вообще** — циклы по шотам/кадрам не имеют `isInterrupted`-проверок. `job.run()` (ffmpeg) блокирует поток до конца процесса; `job.process` нигде не `destroy()`-ится. Прерывание потока ffmpeg-процесс не останавливает.
- `DetectFaces`/`RecognizeFaces`: `RunCmd(cmdText).run()` — синхронный `.run()` (не `.start()`), `waitFor()` ждёт весь скрипт; отмена невозможна.
- `CreateShotsCompressedWithAudio` и др. вызывают `LoadListFramesExt(...).run()` / `LoadListShotsExt(...).run()` **синхронно** (не `.start()`) — блокируя свой поток, но не создавая нового.
- `CreateFilterResult(...).run()` в `FilterEditFXController.doCreateVideo` — вызывается **на FX-треде**, т.е. блокирует весь UI на время конкатенации. То же в `doTrainFaceModel`.

### 4.5 LoadList / UpdateList

Все 15 классов одинаковой формы:
```kotlin
class LoadListXxx(private var list: ObservableList<XxxExt>, …,
                  private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)) : Thread(), Runnable {
    override fun run() { this.name = "LoadListXxx"; loadList(); flagIsDone.set(true) }
    private fun loadList() {
        Platform.runLater { pb!!.progress = -1.0; pb!!.isVisible = true; lbl!!.text = "Loading …: ${fileExt.file.name}" }
        val sourceIterable = <repository>.findBy…
        list.clear()
        for ((i, item) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater { pb!!.progress = i.toDouble()/sourceIterable.count(); … }
                list.add(XxxExt(item, fileExt))
            } else return
        }
        Platform.runLater { pb!!.isVisible = false; lbl!!.isVisible = false }
    }
}
```
Заметно: `sourceIterable.count()` вызывается **внутри цикла на каждой итерации** (пересчёт размера коллекции на каждом кадре — заметная нагрузка на больших файлах).

`LoadListShotsExt` / `LoadListScenesExt` / `LoadListEventsExt` дополнительно eagerly форсируют `shotExt.previewsFirst` / `previewsLast` — то есть читают JPEG с диска и рисуют оверлеи прямо в потоке загрузки.

`UpdateListFilesExt` — «прогрев» ленивых геттеров `FileExt` (папки, флаги hasXxx), чтобы таблица не перечитывала диск при отрисовке.

---

## 5. Алгоритмы определения шотов и сцен

### 5.1 Шоты: автоматически (Sikuli image-similarity), без ffmpeg scene-detect

Цепочка: `FrameController.createFrames` → `AnalyzeFrames` → `CreateShots`.

**Шаг A — создать строки кадров (`FrameController.createFrames`).** Чистый SQL `INSERT … SELECT` с CROSS JOIN шести таблиц `SELECT 0…9` — генерирует числа 1…`framesCount+1` и вставляет их в `tbl_frames`. Никакого ffmpeg.

**Шаг B — получить I-кадры.** `getListIFrames(mediaFile, fps)` через `ffprobe -skip_frame nokey -select_streams v -show_frames` (см. §3.2). Проставляется `frame.isIFrame = listIFrames.contains(frameNumber)`.

**Шаг C — вычислить simScore (Sikuli).**
```kotlin
Settings.MinSimilarity = 0.0
val f = Finder(currentFrameExt.pathToSmall)      // 135×75 JPEG
f.find(Pattern(frameNextExt.pathToSmall))
simScore = if (f.hasNext()) f.next().score else 0.0
```
Один `Finder` переиспользуется для трёх сравнений «вперёд» (с i+1, i+2, i+3). Для каждой пары записываются **оба** направления:
```kotlin
frameNextExt1.frame.simScorePrev1 = simScore   // симметрия(текущий, i+1) с точки зрения i+1
currentFrameExt.frame.simScoreNext1 = simScore // симметрия(i, i+1) с точки зрения i
```
То есть `simScoreNextN(i)` = similarity(frame_i, frame_{i+N}), `simScorePrevN(i)` = similarity(frame_{i-N}, frame_i) (за счёт двойной записи). Хранится в `tbl_frames`: `sim_score_next_1/2/3`, `sim_score_prev_1/2/3`.

**Шаг D — вычислить diff.**
```kotlin
diffNext1 = abs(simScoreNext1(i) - simScoreNext1(i+1))
diffNext2 = abs(simScoreNext1(i+1) - simScoreNext1(i+2))
diffPrev1 = abs(simScorePrev1(i-1) - simScorePrev1(i))
diffPrev1 = abs(simScorePrev1(i-2) - simScorePrev1(i-1))   // ← БАГ: diffPrev1 перезаписывается
```
**Баг:** `diffPrev2` никогда не вычисляется (в `tbl_frames` есть колонка `diff_prev_2`, но она всегда 0.0). Второе присваивание в `diffPrev1` затирает первое.

**Шаг E — детект переходов (пороги).**
```kotlin
val diff1 = 0.4    // Порог обнаружения перехода
val diff2 = 0.42   // Вторичный порог
if (frame.simScorePrev1 < diff1) {
    if (frame.diffPrev1 > diff2 || frame.diffPrev2 > diff2) {
        frame.isFind = frame.isFinalFind = true; frame.isManualAdd = frame.isManualCancel = false
    } else {
        frame.isFind = frame.isFinalFind = false; frame.isManualAdd = frame.isManualCancel = false
    }
} else if (frame.diffPrev1 > diff2 && frame.diffPrev2 > diff2 && frame.simScoreNext1 > diff1) {
    frame.isFind = frame.isFinalFind = true; frame.isManualAdd = frame.isManualCancel = false
} else {
    frame.isFind = frame.isFinalFind = false; frame.isManualAdd = frame.isManualCancel = false
}
```
Практически, из-за `diffPrev2 ≡ 0.0`:
- **Ветка 1 срабатывает:** `simScorePrev1 < 0.4` И `diffPrev1 > 0.42` — «сильный скачок похожести назад».
- **Ветка 2 (с `diffPrev2 > 0.42`) недостижима** мёртва.

Также `AnalyzeFrames` **сбрасывает** `isManualAdd`/`isManualCancel` в false — ручные правки переживают только до повторного `AnalyzeFrames`. Ручные отметки (`isManualAdd=true → isFinalFind=true`) живут в UI (`ShotsEditFXController`).

**Шаг F — сохранить.** `Main.frameRepo.saveAll(listFrames)`.

**Шаг G — нарезка на шоты (`CreateShots`).** Границы = кадры с `isFinalFind`, последний шот закрывается последним кадром файла:
```
шот = [firstFrameNumber, (кадрПерехода - 1)]
firstFrameNumber := кадрПерехода
nearestIFrame     := последний I-кадр, встреченный до границы
```
`nearestIFrame` — «ключевой кадр», с которого начинается GOP, содержащий этот шот. Файл lossless (`CreateLossless`) кодируется так, что можно делать `-ss` по любому кадру, поэтому `nearestIFrame` используется для точного seek при нарезке lossless-шотов.

`hasCreatedShots` → `hasCreatedShotsString` отображается как `✓`/`✗` в колонке `colFileExtCS`.

### 5.2 Сцены: ручные, не автоматические

**Автодетекта сцен нет.** `SceneController` не имеет никакого `detect/createFromFrames` — только `getOrCreate(file, firstFrameNumber, lastFrameNumber)` и `createSceneExt(listShotsExt)`.

Сцены создаются **вручную** в `ShotsEditFXController` (~строка 2653) из выделенных пользователем шотов:
```kotlin
val listShotsExt = mutableListOf<ShotExt>()
for ((i, shotExt) in selectedShotsExtSorted.withIndex()) {
    if (i < selectedShotsExtSorted.size - 1) {
        if (shotExt.shot.lastFrameNumber + 1 == selectedShotsExtSorted[i+1].shot.firstFrameNumber) listShotsExt.add(shotExt)
        else break                          // ← разрыв в нумерации → сцена обрывается
    } else listShotsExt.add(shotExt)
}
if (listShotsExt.isNotEmpty()) {
    val sceneExt = SceneController.createSceneExt(listShotsExt)     // → SceneController.getOrCreate(file, first.firstFrame, last.lastFrame)
    … TextInputDialog(sceneExt.sceneName) …
}
```

`SceneController.getOrCreate` разрезает/удаляет пересекающиеся сцены:
```kotlin
val crossingScenes = Main.sceneRepo.getCrossingScenes(file.id, firstFrameNumber, lastFrameNumber).toMutableList()
if (crossingScenes.isNotEmpty()) {
    crossingScenes.sort(); crossingScenes.forEach { currentScene ->
        if (currentScene.first < first && currentScene.last > last) {
            // новая сцена [last+1 .. currentScene.last]; currentScene.last = first-1
        } else if (currentScene.first < first && currentScene.last >= first) currentScene.last = first - 1
        else if (currentScene.first <= last && currentScene.last > last)   currentScene.first = last + 1
        else delete(currentScene)
    }
}
entity.name = "Scene $firstFrameNumber-$lastFrameNumber"    // имя по умолчанию
```
Таблица — `tbl_scenes`; имя предлагается пользователю в диалоге.

`ShotExt.sceneExt` ищет сцену, **целиком содержащую** шот:
```kotlin
val sceneExt: SceneExt? get() = fileExt.scenesExt.firstOrNull {
    shot.firstFrameNumber >= it.scene.firstFrameNumber && shot.lastFrameNumber <= it.scene.lastFrameNumber }
```
И рендерится это как цветные полосы поверх превью кадров шота: `OverlayImage.setOverlayIsBodyScene` (оранжевая полоса слева, вся высота), `setOverlayIsStartScene` (оранжевая полоска сверху), `setOverlayIsEndScene` (снизу).

Аналогично `Event` (`tbl_events`) — ручная сущность того же типа, что и `Scene` (событие внутри шота, зелёная маркировка).

---

## 6. Превью и конкатенация — точные команды

### 6.1 Превью

**`CreatePreview`** (`CreatePreview.kt`), единственная операция, дающая «играбельный» mp4:
```kotlin
val builder = FFmpegBuilder()
    .setInput(fileExt.file.path)            // исходный файл, НЕ lossless
    .overrideOutputFiles(true)
    .addOutput(fileExt.pathToPreviewFile)   // Preview/<shortName>/<shortName>_preview.mp4
    .setVideoResolution(720, 400)
    .setVideoBitRate(500000)
    .setVideoCodec("libx264")
    .setAudioCodec("aac")
    .setAudioBitRate(196608)
    .setAudioSampleRate(48000)
    .setAudioChannels(2)
    .setVideoFilter(filter)                 // scale=…,pad=…:black
    .done()
```
Реконструированная командная строка:
```
ffmpeg -y -v error -progress tcp://localhost:<port> \
       -i <исходный_файл> \
       -vcodec libx264 -s 720x400 -acodec aac -ar 48000 -ac 2 \
       -af "scale=720:<H>,pad=720:400:0:<Y>:black" \
       -b:v 500000 -b:a 196608 \
       "<Preview>\<shortName>\<shortName>_preview.mp4"
```
(порядок аргументов — по байткоду bramp 0.6.2; см. §3.3, команду (4))

Фильтр letterbox:
```kotlin
val fileAspect  = fileWidth.toDouble() / fileHeight.toDouble()   // из ffprobe
val frameAspect = 720.0 / 400.0
if (fileAspect > frameAspect) {
    val frameHeight = (720.0 / fileAspect).toInt()
    "\"scale=720:" + frameHeight + ",pad=720:400:0:" + ((400 - frameHeight) / 2.0).toInt() + ":black\""
} else {
    val frameWidth = (400.0 * fileAspect).toInt()
    "\"scale=" + frameWidth + ":400,pad=720:400:" + ((720 - frameWidth) / 2.0).toInt() + ":0:black\""
}
```
Т.е. всегда 720×400 с чёрными полосами (верх/низ или лево/право), пропорции сохраняются.

**`CreateFacesPreview`** — превью лиц (не ffmpeg): `OverlayImage.extractRegion(bi, startX,startY,endX,endY, 75,75, 1.4, false)` + `ImageIO.write(bi, "jpg", …)`.

### 6.2 Конкатенация

**`CreateConcat`** — файл-в-файл (`fileExt`), `CreateFilterResult` — произвольный набор шотов (выборка фильтра).

Шаг 1 — текстовый список (создаётся и **удаляется** в конце):
```kotlin
var concatFiles = ""
fileExt.shotsExt.forEach { concatFiles += "file '${it.pathToCompressedWithAudio}'\n" }
val fileInput = "${fileExt.folderConcat}${File.separator}${fileExt.file.shortName}.txt"
FileWriter(listFilesToConcat).write(concatFiles)
```

Шаг 2 — сборка команды:
```kotlin
val fileOutput = FileController.getConcat(fileExt)
// = "${fileExt.folderConcat}\\${fileExt.file.shortName}_concat.${VideoContainers.valueOf(project.container).extention}"

val builderOutput = FFmpegOutputBuilder(); builderOutput.setFilename(fileOutput)
val builder = FFmpegBuilder()
    .setInput(fileInput)
    .addExtraArgs("-f", "concat")
    .addExtraArgs("-safe", "0")
    .overrideOutputFiles(true)
    .addOutput(builderOutput)

builderOutput.addExtraArgs("-map", "0:v:0")
fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track ->
    var typeOrder = PropertyController.getOrCreate(track::class.java.simpleName, track.id, "@typeorder")
    if (typeOrder == "") typeOrder = "1"
    builderOutput.addExtraArgs("-map", "0:a:${(typeOrder.toInt()) - 1}")
}
builderOutput.addExtraArgs("-c", "copy")

val job = FFmpegExecutor(ffmpeg).createJob(builder)
job.run()
if (File(fileInput).exists()) File(fileInput).delete()
```

**Реконструированная командная строка (точная):**
```
ffmpeg -y -v error -f concat -safe 0 \
       -i "<Concat>\<shortName>.txt" \
       -map 0:v:0 -map 0:a:0 \
       -c copy \
       "<Concat>\<shortName>_concat.<mp4|mkv|mxf>"
```
Точные особенности:
- `-f concat -safe 0` — идут на **вход** (до `-i`, из `FFmpegBuilder.extra_args`).
- `-map 0:v:0` + по одному `-map 0:a:<typeOrder-1>` на каждую включённую аудиодорожку (`Track.type == "Audio" && Track.use`), где `typeOrder` = property `@typeorder` трека (по умолчанию `"1"` → индекс 0).
- `-c copy` — **stream copy**, без перекодирования.
- Расширение выхода берётся из `project.container` (`VideoContainers`: MP4/MKV/MXF).
- Прогресса нет (нет `ProgressListener` → нет `-progress tcp://`).

**`CreateFilterResult`** — команда **идентична**, отличаются только:
- список формируется из произвольного `shotsExt` (первая/последняя дают имя);
- имя: `"${projectExt.folderFilters}\\${filterExt.filter.name} [${shotsExt.first()...shortName}-${shotsExt.last()...shortName}].${ext}"`;
- `.txt` создаётся в каталоге `Filters/` (не `Concat/`) и удаляется после;
- аудио-`-map` берётся от **первого** файла выборки (`fileExt` из `tblFiles.items.first()`), независимо от того, какие файлы реально попали в список.

---

## 7. CreateLossless и CreateFilterResult — что это

### 7.1 `CreateLossless` — «промежуточная мастер-копия»

Массовая операция, выполняемая один раз на файл. Генерирует `Lossless/<shortName>/<shortName>_lossless.mkv` из исходного видео и является **единственным источником для всех последующих операций нарезки** (`CreateShotsCompressedWithAudio`, `CreateShotsLosslessWithAudio/WithoutAudio` читают `fileExt.pathToLosslessFile`).

Настройки берутся из `tbl_projects` (`Project`):
| Поле | Колонка | Значение по умолчанию | Использование |
|---|---|---|---|
| `width`/`height` | `width`, `height` | 1920×1080 | целевое разрешение (`-s WxH` + `pad`) |
| `lossLessContainer` | `lossless_container` | `MKV` | выбор ветки (MXF → принудительно DNX) |
| `lossLessCodec` | `lossless_codec` | `RAW` | `rawvideo` (+`-pix_fmt yuv420p`) или `dnxhd` (+`-b:v 36M`) |
| аудио | жёстко | `pcm_s16le`, 48000 Гц | `AudioCodecs.PMC` |

Назначение — быстрый точный seek: `rawvideo` — кадр-в-кадр без компрессии (минимальная нагрузка при произвольном доступе), `dnxhd -b:v 36M` — редактируемый формат (монтажные системы). Перекодирование в x264 здесь не делается.

Побочные эффекты:
- флаги всех включённых аудиодорожек **переупаковываются** в `-map 0:v:0` + `-map 0:a:<typeOrder-1>` (треки с `use == false` отбрасываются);
- `tbl_properties` пополняется записями `@typeorder` (через `PropertyController.getOrCreate`);
- `hasLossless = true` → колонка `colFileExtLL` в UI.

### 7.2 `CreateFilterResult` — «выгрузка результата фильтра в видеофайл»

Это не per-file операция из `ProjectActionsFXController`, а отдельная точка вызова из `FilterEditFXController` (`doCreateVideo(event)`):
```kotlin
val shotsExt = tblShots?.items?.toMutableList()
val fileExt  = tblFiles?.items?.first()
val filterExt = currentFilterExt
if (shotsExt != null && fileExt != null && filterExt != null) {
    CreateFilterResult(filterExt, currentProjectExt!!, shotsExt, fileExt).run()   // ← .run(), не .start()
}
```

Смысл: пользователь описал фильтр (набор персон + набор условий), `FilterController`/`ShotTmpCdfController` отобрали ему подходящие шоты (`tblShots` в диалоге), и `CreateFilterResult` **склеивает их в один видеофайл** — по сути «экспорт выборки». Идентична `CreateConcat`, но:
- набор шотов задаётся произвольно (а не «все шоты файла»);
- каталог выхода — `Filters/` (создаётся `mkdir` при необходимости);
- имя включает имя фильтра и диапазон исходных файлов: `"<filterName> [<firstShortName>-<lastShortName>].<ext>"`;
- `filename` — необязательный 5-й параметр, позволяет переопределить имя;
- `CreateFilterResult` тоже наследует `Thread`, но вызывается как `.run()` с FX-треда (блокирует UI).

---

## 8. Сводка найденных дефектов / странностей (попутно)

1. **`diffPrev2` никогда не вычисляется** (`AnalyzeFrames.kt:148`) — `diffPrev1` перезаписывается дважды; вторая ветка детекта переходов из-за этого мертва.
2. **`CreateShotsLosslessWithoutAudio` всё равно мапит аудио** (`-map 0:a:<n>` не закомментирован), несмотря на имя «без аудио».
3. **БАГ bramp 0.6.2**: `FFmpegBuilder.setVideoFilter()` эмитит `-af` (а `setAudioFilter()` → `-vf`), т.к. в `FFmpegBuilder.build()` поля `videoFilter`/`audioFilter` перепутаны местами. Для `CreateFramesSmall/Medium/Full` и `CreatePreview` (все вызывают `setVideoFilter` на билдере) фильтр уходит как **аудиофильтр**. Для `CreateLossless`, `CreateShotsCompressedWithAudio` используется `builderOutput.setVideoFilter()` → корректный `-vf`.
4. **`_lossless.mkv` захардкожен** (`FileController.getLossless`), даже если `project.lossLessContainer == MXF`.
5. **Ручные правки затираются**: `AnalyzeFrames` сбрасывает `isManualAdd`/`isManualCancel` в `false`.
6. **`RunListThreads` — последовательный, а не параллельный**, несмотря на название и на то, что принимает `List<Thread>`.
7. **При прерывании `RunListThreads` флаг `flagIsDone` не выставляется** → UI может «зависнуть» на прогрессе навсегда.
8. **Project actions не проверяют `isInterrupted`** — отмена не работает для них; ffmpeg-процессы не убиваются (`job.process.destroy()` нигде нет).
9. **`CreateFilterResult(...).run()` на FX-треде** блокирует UI.
10. **`frames.json` мёртвый** — `DetectFaces` его пишет, но в Python-скрипт не передаёт.
11. **`Folders.SHOTS` и `Folders.SHOTS_COMPRESSED_WITH_AUDIO` имеют одинаковый `folderName = "Shots"`** → дефолтные пути совпадают (осознанно или нет — разводятся только через propertyCdf).
12. **Хрупкий парсинг ffprobe**: regex `(?<=\[FRAME\]\r\n)...(?=\[/FRAME\]\r\n)` жёстко завязан на `\r\n` → работает только под Windows. То же в `executeExe`/`executeMediaInfo`: `out.substring(0, out.length - 2)` отбрасывает 2 символа вывода безусловно.
13. **`sourceIterable.count()` вызывается на каждой итерации** во всех `LoadList*` (пересчёт размера коллекции в цикле).
14. **`Folders.CONCAT` без подпапки `shortName`** — все файлы проекта пишут concat-файлы в один каталог (различаются префиксом).
15. **`Folder.mkdir()` вместо `mkdirs()`** — при отсутствии родительской папки на уровне проекта операция молча не создаёт вложенный каталог.
16. **Лейбл в UI для small-кадров говорит «175x35»**, хотя код использует `135 × 75` (`CreateFramesSmall.kt:48-49` vs `ProjectActionsFXController.kt:290`).

---

## 9. Приложение: имена методов, упомянутых в отчёте

- `IvfxFFmpegUtils.getListIFrames`, `getFrameNumberByDuration`, `getDurationByFrameNumber`, `convertDurationToString`; поля `FFMPEG_PATH`, `FFPROBE_PATH`, `FFPLAY_PATH`.
- `IvfxUtils.executeExe(exePath, parameters)` — top-level функция в `utils/IvfxUtils.kt`.
- `MediaInfo.getInfo`, `getInfoByParameter`, `getInfoBySectionAndParameter`, `executeMediaInfo`; `getFromMediaInfoTracks(json)` — top-level.
- `FaceDetection.FACE_DETECTOR_PATH`.
- `OverlayImage.setTextOverlay`, `setOverlayRectangle`, `setOverlayTriangle`, `setOverlayUnderlineText`, `setOverlayUnderlinePlate`, `setOverlayFirstFrameFound/LastFrameFound`, `setOverlayFirstFrameManual/LastFrameManual`, `cancelOverlayFirstFrameManual/LastFrameManual`, `setOverlayIFrame`, `setOverlayIsBodyScene/IsStartScene/IsEndScene`, `setOverlayIsBodyEvent/IsStartEvent/IsEndEvent`, `resizeImage`, `extractRegion`.
- `ConvertToFxImage.convertToFxImage(BufferedImage)`, `getClone(Image)`.
- `ComputerIdentifier.getComputerId()`.
- Контроллеры: `FileController.getCdfFolder/getFolderXxx/getLossless/getPreview/getConcat/hasXxx/getFps/getFramesCount/getFFmpegProbeResult/create/delete/reOrder`; `ProjectController.getCdfFolder/getFolderXxx/create/delete`; `FrameController.getOrCreate/getListFrames/getListFramesExt/createFrames/deleteAll/save/getFrameExt`; `ShotController.getSetShots/save/deleteAll/getOrCreate/convertSetShotsIdsToListShotsExt`; `SceneController.getSetScenes/getOrCreate/createSceneExt/delete/deleteAll/save`; `FaceController.createOrUpdate/getListFacesExt/getListFacesExtToRecognize/getArrayFramesToDetectFaces/getFramesToRecognize/getListFacesToTrain`; `PropertyCdfController.getOrCreate/editOrCreate`; `PropertyController.getOrCreate`; `TrackController.createTracksFromMediaInfo`.
- Модели-расширения: `FileExt` (ленивые поля), `ProjectExt` (папки проекта), `FrameExt` (`pathToSmall/Medium/Full`, `labelSmall/Medium/Full`, `biSmall/Medium/Full`, `pathToStubSmall/Medium/Full`), `ShotExt` (`pathToCompressedWithAudio`, `pathToLosslessWithAudio`, `pathToLosslessWithoutAudio`, `sceneExt`, `previewsFirst/Last`), `FaceExt` (`pathToFrameFile`, `pathToFaceFile`, `pathToPreviewFile`, `FaceExtJson`-аннотации `@SerializedName`), `FilterExt`, `ProjectExt`.

---

*Исследование выполнено по исходникам на HEAD `a3003b7`. Файл создан агентом-субагентом в рамках изучения зоны «видео-пайплайн».*
