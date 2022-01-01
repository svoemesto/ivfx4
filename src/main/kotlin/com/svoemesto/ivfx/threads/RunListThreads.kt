package com.svoemesto.ivfx.threads

import javafx.beans.property.SimpleBooleanProperty

class RunListThreads(private val listThreads: List<Thread>, private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)): Thread(), Runnable {
    override fun run() {
        var runningThread: Thread? = null
        var countStartedThreads = 0
        while (countStartedThreads != listThreads.size && !currentThread().isInterrupted) {
            if (runningThread == null) {
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.isDaemon = false
                runningThread.start()
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

        println("RunListThreads DONE")
        flagIsDone.set(true)
    }

}