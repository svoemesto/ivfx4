package com.svoemesto.ivfx.threads

class RunListThreads(val listThreads: List<Thread>): Thread(), Runnable {
    override fun run() {
        var runningThread: Thread? = null
        var countStartedThreads = 0
        while (countStartedThreads != listThreads?.size) {
            if (runningThread == null) {
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.start()
            } else {
                while (runningThread.isAlive) {
                    Thread.sleep(100)
                }
                runningThread = listThreads[countStartedThreads]
                countStartedThreads++
                runningThread.start()
            }
        }
    }
}