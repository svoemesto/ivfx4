package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.threads.RunListThreads
import javafx.beans.property.SimpleBooleanProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Прерывание цепочки операций и дочернего процесса.
 *
 * Обе вещи проверяются здесь, а не вручную, потому что вручную их можно
 * проверить только одним способом — закрыть окно во время операции, — и
 * этот способ ничего не сообщает, когда ломается.
 */
@DisplayName("Прерывание цепочки операций")
class InterruptChainTest {
    /**
     * Операция, которая спит, пока её не прервут.
     *
     * Прерывание ловится в `Thread.sleep`, а ложка `interrupted` говорит,
     * дошло ли оно до тела операции.
     */
    private class Sleeper : Thread() {
        @Volatile
        var interruptedInside = false

        @Volatile
        var finished = false

        override fun run() {
            try {
                sleep(30_000)
            } catch (e: InterruptedException) {
                interruptedInside = true
                currentThread().interrupt()
            }
            finished = true
        }
    }

    @Test
    fun `прерывание доходит до операции`() {
        val operation = Sleeper()
        val flag = SimpleBooleanProperty(false)
        val chain = RunListThreads(listOf(operation), flag)

        chain.start()
        Thread.sleep(500)
        assertTrue(operation.isAlive, "операция должна была начаться")

        chain.interrupt()
        chain.join(10_000)

        assertFalse(chain.isAlive, "цепочка должна закончиться после прерывания")
        assertTrue(operation.interruptedInside, "прерывание обязано дойти до операции")
    }

    /**
     * Без `flagIsDone` в `finally` прерванная цепочка исчезала молча, а
     * интерфейс оставался в состоянии ожидания навсегда: флаг слушают через
     * `addListener`, и слушатель просто не срабатывал.
     */
    @Test
    fun `флаг завершения ставится и при отмене`() {
        val operation = Sleeper()
        val flag = SimpleBooleanProperty(false)
        val chain = RunListThreads(listOf(operation), flag)

        chain.start()
        Thread.sleep(500)
        chain.interrupt()
        chain.join(10_000)

        assertTrue(flag.value, "после отмены флаг завершения обязан быть установлен")
    }

    @Test
    fun `флажная цепочка без прерывания отрабатывает целиком`() {
        val operation = Sleeper()
        val flag = SimpleBooleanProperty(false)
        val chain = RunListThreads(listOf(operation), flag)

        chain.start()
        operation.interrupt() // операция завершается, цепочка — нет
        chain.join(10_000)

        assertTrue(flag.value)
        assertFalse(chain.isAlive)
    }

    @Test
    fun `очередь операций идёт по порядку`() {
        val order = mutableListOf<String>()
        val first = quick("первая") { order.add("первая") }
        val second = quick("вторая") { order.add("вторая") }
        val flag = SimpleBooleanProperty(false)
        val chain = RunListThreads(listOf(first, second), flag)

        chain.start()
        chain.join(10_000)

        assertEquals(listOf("первая", "вторая"), order)
        assertTrue(flag.value)
    }

    private fun quick(
        name: String,
        body: () -> Unit,
    ): Thread {
        val thread = Thread { body() }
        thread.name = name
        return thread
    }

    /**
     * Дочерний процесс должен умирать вместе с прерыванием.
     *
     * Раньше чтение вывода шло через `copy()` прямо в потоке операции, а
     * чтение из канала прерывание не слушает — скрипт продолжал работать,
     * сколько его ни прерывали.
     *
     * Вторая половина проверки — про оболочку: команда запускается через
     * `/bin/sh`, а скрипт идёт её потомком, и убийство оболочки оставляло
     * python живым — его переподчинял systemd, и он продолжал писать на диск.
     */
    @Test
    fun `прерывание убивает дочерний процесс и его потомков`() {
        val before = sleepers()
        val runCmd = RunCmd("cd /tmp && sleep 30")
        val thread = Thread { runCmd.run() }

        thread.start()
        Thread.sleep(900)
        assertTrue(thread.isAlive, "процесс должен был запуститься")
        assertTrue(sleepers() > before, "потомок оболочки должен был появиться")

        thread.interrupt()
        thread.join(10_000)
        Thread.sleep(500)

        assertFalse(thread.isAlive, "прерванный RunCmd обязан вернуться, а не висеть на процессе")
        assertEquals(before, sleepers(), "после прерывания не должно остаться ни одного sleep")
    }

    /** Сколько процессов `sleep 30` сейчас живёт в системе. */
    private fun sleepers(): Int {
        val process = ProcessBuilder("pgrep", "-c", "-f", "sleep 30").start()
        return process.inputStream
            .bufferedReader()
            .readText()
            .trim()
            .toIntOrNull() ?: 0
    }

    @Test
    fun `команда без прерывания отрабатывает и возвращает код`() {
        val runCmd = RunCmd("exit 0")
        val thread = Thread { runCmd.run() }

        thread.start()
        thread.join(10_000)
        assertFalse(thread.isAlive)
    }

    @Test
    fun `пустой вызов не виснет`() {
        val runCmd = RunCmd("true")
        val thread = Thread { runCmd.run() }
        thread.start()
        thread.join(10_000)
        assertFalse(thread.isAlive)
    }
}
