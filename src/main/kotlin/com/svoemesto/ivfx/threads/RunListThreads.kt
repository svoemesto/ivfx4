package com.svoemesto.ivfx.threads

import javafx.beans.property.SimpleBooleanProperty

class RunListThreads(private val listThreads: List<Thread>, private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)): Thread(), Runnable {
    override fun run() {
        var runningThread: Thread? = null
        var countStartedThreads = 0
        while (countStartedThreads != listThreads.size) {
            if (runningThread == null) {
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.start()
            } else {
                while (runningThread.isAlive) {
                    sleep(100)
                }
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.start()
            }
        }
        if (runningThread != null) {
            while (runningThread.isAlive) {
                sleep(100)
            }
        }

        println("RunListThreads DONE")
        flagIsDone.set(true)

    }
}