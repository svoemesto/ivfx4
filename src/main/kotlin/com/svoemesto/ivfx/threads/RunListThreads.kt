package com.svoemesto.ivfx.threads

import com.svoemesto.ivfx.utils.Trace
import javafx.beans.property.SimpleBooleanProperty

class RunListThreads(
    private val listThreads: List<Thread>,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false),
) : Thread(),
    Runnable {
    override fun run() {
        this.name = "RunListThreads"
        // Потоки операций создаются анонимно и по умолчанию зовутся
        // Thread-4, Thread-6 — в журнале это бессмыслица, операцию не опознать.
        // Имя ставим по классу: CreateShots.run() сам знает, кто он.
        // Уже названные потоки (LoadListFilesExt и подобные) не трогаем —
        // у них имя осмысленнее класса.
        val autoName = Regex("^Thread-\\d+$")
        listThreads.forEach { if (autoName.matches(it.name)) it.name = it.javaClass.simpleName }
        Trace.start("цепочка из ${listThreads.size} операций: ${listThreads.map { it.name }}")
        // Операция, которую ждём прямо сейчас. Нужна для обработки прерывания:
        // прерывание цепочки обязано дойти и до неё.
        var running: Thread? = null
        try {
            for (next in listThreads) {
                if (Thread.currentThread().isInterrupted) {
                    // Прервали между операциями: следующую не запускаем.
                    Trace.fail("цепочка из ${listThreads.size} операций", "остановлена перед ${next.name}")
                    return
                }
                running = next
                next.isDaemon = false
                // Поток, упавший с исключением, выглядит завершившимся, и по
                // факту смерти потока не отличить успех от аварии. Раньше
                // операция, упавшая по внешнему ключу, отмечалась как
                // [ГОТОВО] — в журнале это выглядело как успех.
                next.uncaughtExceptionHandler =
                    Thread.UncaughtExceptionHandler { t, e ->
                        Trace.fail("операция ${t.name}", "${e::class.java.simpleName}: ${e.message ?: "без сообщения"}")
                    }
                Trace.start("операция ${next.name}")
                next.start()
                // Ожидание — одним механизмом, join(). Раньше здесь стоял
                // join(), а дальше опрос isAlive через sleep(100), то есть два
                // разных способа ждать одно и то же; различались они только
                // тем, что прерывание в одном ловилось, а в другом нет.
                next.join()
                running = null
                if (Thread.currentThread().isInterrupted) {
                    Trace.fail("цепочка из ${listThreads.size} операций", "прервано на операции ${next.name}")
                    return
                }
                Trace.done("операция ${next.name}")
            }
            Trace.done("цепочка из ${listThreads.size} операций завершена")
        } catch (e: InterruptedException) {
            // Сюда попадает прерывание во время join(). Само по себе оно
            // ничего не делает: дочерний поток продолжает работать, а его
            // прерывание никто не отправил. Именно поэтому он упирается в
            // не-демон и не даёт JVM завершиться после закрытия окна.
            running?.interrupt()
            Trace.fail(
                "цепочка из ${listThreads.size} операций",
                "прервано на операции ${running?.name ?: "?"}: ${e.message ?: "без сообщения"}",
            )
            // Флаг прерывания возвращается: иначе вызывающий код, который
            // сам нас прервал, не увидит этого и решит, что всё прошло.
            Thread.currentThread().interrupt()
        } finally {
            // Флаг ставится на ЛЮБОМ выходе, включая отмену.
            //
            // Его слушают через addListener, и без него интерфейс остаётся
            // в состоянии ожидания навсегда: прерванная цепочка просто
            // исчезала, не сообщив об этом ни одной строкой.
            flagIsDone.set(true)
        }
    }
}
