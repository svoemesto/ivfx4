package com.svoemesto.ivfx.utils

import java.io.File

object FaceDetection {
    /**
     * Модели, без которых распознавание не работает.
     *
     * Первая — детектор SCRFD-10G, вторая — эмбеддинги ArcFace.
     */
    private val MODELS_REQUIRED = listOf("det_10g.onnx", "w600k_r50.onnx")

    /**
     * Папка со скриптами распознавания лиц.
     *
     * Раньше путь брался через `getResource().path` и перв��й символ
     * отбрасывался — так убирали приставку `file:`. При запуске из jar-а
     * `getResource()` отдаёт не путь к папке, а строку
     * `jar:file:/...jar!/...`, и после обрезания первого символа получался
     * мусор, который нельзя ни передать в ProcessBuilder, ни передать
     * скрипту на Python.
     *
     * Теперь ресурс разбирается как объект File, поэтому получается
     * настоящий путь в файловой системе.
     */
    val FACE_DETECTOR_PATH: String = resolveDetectorPath()

    /**
     * Папка с моделями распознавания лиц.
     *
     * Модели — это ~184 МБ неизменяемых бинарных файлов, и в git их держать
     * нельзя: они раздувают каждый клон и не меняются между правками кода.
     * Поэтому они скачиваются один раз и кешируются на машине.
     *
     * Путь по умолчанию — `~/.insightface/models/buffalo_l`: именно туда
     * библиотека insightface кладёт пакет buffalo_l, так что наш кеш и её
     * кеш — один и тот же каталог, и повторной загрузки не будет.
     * Переопределяется переменной окружения `IVFX_MODELS_DIR`.
     *
     * Скачивание: `bash tools/fetch-models.sh`.
     */
    val MODELS_DIR: String = resolveModelsDir()

    /**
     * Полный путь к файлу модели.
     *
     * @param name имя файла, например `w600k_r50.onnx`
     * @return путь к модели
     */
    fun modelPath(name: String): String = File(MODELS_DIR, name).absolutePath

    /**
     * Проверяет, что модели на месте.
     *
     * Вызывать перед запуском распознавания, чтобы вместо ошибки от
     * onnxruntime пользователь увидел понятное «скачайте models».
     *
     * @return список недостающих моделей; пустой список — всё на месте
     */
    fun missingModels(): List<String> = MODELS_REQUIRED.filterNot {
        File(MODELS_DIR, it).let { f -> f.isFile && f.length() > 0 }
    }

    /**
     * Папка моделей: переменная окружения, затем кеш insightface.
     */
    private fun resolveModelsDir(): String {
        val fromEnv = System.getenv("IVFX_MODELS_DIR")
        if (!fromEnv.isNullOrBlank()) {
            return File(fromEnv).absolutePath
        }
        return File(System.getProperty("user.home"), ".insightface/models/buffalo_l")
            .absolutePath
    }

    /**
     * Ищет папку со скриптами: в ресурсах, рядом с приложением, в рабочем
     * каталоге.
     *
     * @return путь к папке либо пустая строка, если папка не найдена
     */
    private fun resolveDetectorPath(): String {
        val bundled = FaceDetection::class.java.getResource("FaceDetector")
        if (bundled != null) {
            val file = File(bundled.toURI())
            if (file.isDirectory) {
                return file.absolutePath
            }
        }
        val nearby = listOf(
            File("FaceDetector"),
            File("src/main/resources/com/svoemesto/ivfx/utils/FaceDetector")
        ).firstOrNull { it.isDirectory }
        return nearby?.absolutePath ?: ""
    }

    /**
     * Интерпретатор Python для скриптов распознавания.
     *
     * Скриптам нужны пакеты opencv, numpy, imutils и scikit-learn. Они
     * установлены в виртуальное окружение, потому что Ubuntu запрещает
     * доустанавливать пакеты в системный Python. Если окружения нет,
     * берётся системный python3.
     */
    val PYTHON_PATH: String = resolvePython()

    /**
     * Порог принятия распознавания: минимальное косинусное сходство, при
     * котором лицо считается найденным.
     *
     * Значение одно и то же здесь и в записи результата, потому что раньше
     * пороги разъехались: скрипт отбрасывал всё ниже 0,45, а запись
     * результата принимала имя при сходстве больше 0,3. Лицо с результатом
     * 0,32 проходило как признанное, хотя распознавать его отвергло.
     */
    const val RECOGNIZE_THRESHOLD = 0.45

    /**
     * Минимальный запас, с которым лучший персонаж должен обгонять второго.
     *
     * Замер на этой серии: у нераспознанных лиц медиана разрыва между лучшим
     * и вторым кандидатом — 0,025, минимум 0,000. То есть больше половины
     * признанного отличалось от проигравшего практически ни на что, и
     * «победителя» можно было назвать монеткой.
     *
     * Цена отказа от таких назначений высокая — 4900 лиц вместо 2044 на
     * пороге 0,45, — но оправданная: неверно названное лицо сохраняется в
     * базе и при следующем прогоне само становится образцом галереи, то
     * есть ошибка начинает тиражироваться. Не признать лицо вовсе дешевле,
     * чем признать неверно.
     */
    const val RECOGNIZE_MARGIN = 0.05

    /**
     * Ищет интерпретатор Python.
     *
     * Сначала окружение `ivfx-venv-opencv4`: детектор лиц теперь это SCRFD из
     * пакета insightface, а модель распознавания грузится через
     * `cv2.dnn.readNetFromTorch`, которого нет в OpenCV 5. Поэтому окружение
     * держится на OpenCV 4.14, и ставить туда что-либо, что затащит пятую
     * версию, нельзя — распознавание перестанет работать.
     *
     * Затем прежнее окружение `ivfx-venv`, затем python3 из PATH.
     *
     * @return путь к интерпретатору либо имя `python3`
     */
    private fun resolvePython(): String {
        val home = System.getenv("HOME").orEmpty()
        for (name in listOf(".local/ivfx-venv-opencv4/bin/python", ".local/ivfx-venv/bin/python")) {
            val candidate = File(home, name)
            if (candidate.canExecute()) {
                return candidate.absolutePath
            }
        }
        val fromPath = System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .map { File(it, "python3") }
            .firstOrNull { it.canExecute() }
        return fromPath?.absolutePath ?: "python3"
    }
}
