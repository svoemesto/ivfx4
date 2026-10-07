package com.svoemesto.ivfx.utils

/**
 * Журнал работы приложения.
 *
 * Задача — отвечать на вопрос «что сейчас происходит», не снимая дамп
 * потоков. Поводов для такого журнала набралось много: операция пайплайна
 * молча возвращалась, не меняя планы; не было видно, что операция вообще
 * началась и чем закончилась; при падении в базе оставался только стек
 * JavaFX.
 *
 * Два слоя, и это важно:
 *
 * **Границы** — начало и конец операций пайплайна и переходов между слоями.
 * Всегда включены. Их немного, и они дают стек вызовов на уровне
 * «операция → контроллер → репозиторий».
 *
 * **Трассировка всех методов** — вход и выход каждого метода проекта.
 * Выключена по умолчанию, включается ключом:
 *
 * ```
 * bash run-old-app.sh --trace          # обычный запуск с трассировкой
 * ```
 *
 * Почему по умолчанию выключено. Методов в проекте 842, а прогон одной
 * операции делает порядка 100 000 вызовов: логирование каждого дало бы
 * около 210 000 строк на операцию. Ровно этим путём проект уже проходил —
 * при отладочном уровне Hibernate на один прогон анализа кадров
 * накапливалось 439 МБ вывода.
 *
 * Формат строки: `[метка] что именно`, метки — [НАЧАЛО], [ГОТОВО],
 * [ОШИБКА], [ПРОПУЩЕНО], [ВХОД], [ВЫХОД]. По метке читается, что
 * случилось, без знания кода.
 */
object Trace {
    const val START = "НАЧАЛО"
    const val DONE = "ГОТОВО"
    const val FAIL = "ОШИБКА"
    const val SKIP = "ПРОПУЩЕНО"
    const val ACTION = "ДЕЙСТВИЕ"
    const val ENTER = "ВХОД"
    const val EXIT = "ВЫХОД"

    /** Трассировка всех методов. По умолчанию выключена — см. KDoc выше. */
    val enabled: Boolean = System.getProperty("ivfx.trace") == "true"

    fun start(what: String) {
        println("[$START] $what")
    }

    fun done(what: String) {
        println("[$DONE] $what")
    }

    /** [done] с указанием затраченного времени. */
    fun done(
        what: String,
        millis: Long,
    ) {
        println("[$DONE] $what (${String.format("%.1f", millis / 1000.0)} с)")
    }

    fun fail(
        what: String,
        reason: String,
    ) {
        println("[$FAIL] $what: $reason")
    }

    fun skip(
        what: String,
        reason: String,
    ) {
        println("[$SKIP] $what: $reason")
    }

    /**
     * Действие пользователя в интерфейсе: нажатие кнопки, пункт меню,
     * двойной клик. Всегда включено — частота здесь человеческая, десятки
     * событий в минуту, а польза большая: по журналу видно, что именно
     * сделал пользователь, даже если на экране ничего не изменилось.
     *
     * Прокрутка и смена выделения сюда намеренно не попадают: они летят
     * десятками в секунду и на флаг трассировки.
     */

    /**
     * Ход работы операции: сделано из скольки, и что получилось.
     *
     * Печатается не каждый шаг, а не чаще раза в 3 секунды на метку. Без
     * ограничения анализ кадров напечатал бы 82336 строк — тот же расход,
     * из-за которого трассировка методов вынесена под флаг.
     */
    fun progress(
        tag: String,
        done: Long,
        total: Long,
        note: String = "",
    ) {
        val now = System.currentTimeMillis()
        val last = lastProgress[tag] ?: 0L
        if (now - last < PROGRESS_INTERVAL_MS) return
        lastProgress[tag] = now
        val tail = if (note.isEmpty()) "" else ", $note"
        println("[$tag] $done/$total$tail")
    }

    private val lastProgress = mutableMapOf<String, Long>()
    private const val PROGRESS_INTERVAL_MS = 3000L

    fun action(what: String) {
        println("[$ACTION] $what")
    }

    /** Нажатие клавиши: под флагом, частоты не угадаешь заранее. */
    fun key(what: String) {
        if (enabled) println("[КЛАВИША] $what")
    }

    fun enter(what: String) {
        if (enabled) println("[$ENTER] $what")
    }

    fun exit(what: String) {
        if (enabled) println("[$EXIT] $what")
    }

    /**
     * Обёртка для метода: пишет вход и выход, если трассировка включена.
     * Подключение нового метода — одна строка вместо двух, и забыть
     * про выход уже нельзя.
     */
    inline fun <T> trace(
        name: String,
        block: () -> T,
    ): T {
        enter(name)
        return try {
            val result = block()
            exit(name)
            result
        } catch (e: Throwable) {
            fail(name, "${e::class.java.simpleName}: ${e.message ?: "без сообщения"}")
            throw e
        }
    }

    /**
     * Точка наблюдения: кто снимает ручную отметку кадра.
     *
     * Отметка «это поправил человек» обязана переживать прогон анализа.
     * Правка в AnalyzeFrames оказалась недостаточной: флаги всё равно
     * обнуляются, и писать их могут несколько мест. Пока не видно, кто
     * именно, этот метод печатает кадр и стек вызовов.
     */
    fun manualReset(
        where: String,
        frameNumber: Any?,
        flags: String,
    ) {
        if (!enabled) return
        val st =
            Thread
                .currentThread()
                .stackTrace
                .drop(2)
                .joinToString(" <- ") { it.className.substringAfterLast('.') + "." + it.methodName }
        println("[СБРОС ОТМЕТКИ] $where: кадр=$frameNumber, флаги=$flags, из: $st")
    }
}
