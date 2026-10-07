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
        Trace.start("цепочка из ${listThreads.size} операций: ${listThreads.map { it.name }}")
        var runningThread: Thread? = null
        var countStartedThreads = 0
        while (countStartedThreads != listThreads.size && !currentThread().isInterrupted) {
            if (runningThread == null) {
                val next = listThreads[countStartedThreads]
                runningThread = next
                countStartedThreads++
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
                next.join()
                if (next.state != Thread.State.NEW && !Thread.currentThread().isInterrupted) {
                    Trace.done("операция ${next.name}")
                }
            } else {
                while (runningThread.isAlive) {
                    try {
                        sleep(100)
                    } catch (e: InterruptedException) {
                        runningThread.interrupt()
                        return
                    }
                }
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.isDaemon = false
                Trace.start("операция ${runningThread.name}")
                runningThread.start()
            }
        }
        if (runningThread != null) {
            while (runningThread.isAlive) {
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    runningThread.interrupt()
                    return
                }
            }
        }

        Trace.done("цепочка из ${listThreads.size} операций завершена")
        flagIsDone.set(true)
    }
}
