package com.svoemesto.ivfx.apps

import com.svoemesto.ivfx.initializeH2db
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage

/**
 * Точка входа приложения.
 *
 * Запуск устроен необычно: главное окно создаёт не JavaFX, а контроллер
 * [com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController], поэтому параметр
 * `stage` метода [start] игнорируется, а [Application.launch] завершается не
 * закрытием окна, а отдельной командой [Platform.exit].
 *
 * Без неё процесс остаётся жить после закрытия окна: инструмент JavaFX не
 * получает команду выйти, а рабочие потоки не-демоны удерживают JVM. Наблюдалось
 * дважды за один день — 24,2 ГБ и 17,9 ГБ удержанной памяти, и следующий запуск
 * падал с `IllegalStateException: The file is locked` на `h2db.mv.db`.
 *
 * @see docs/adr/0005-sequential-threads-no-pool.md
 */
class ProjectFXApp : Application() {

    override fun start(stage: Stage) {
        initializeH2db()
        stopWorkersOnShutdown()

        // Окно создаёт контроллер, а не launch. Аргумент `stage` не используется.
        com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController()
            .editProject(null, hostServices)

        // Главное окно закрыто — приложение должно закончиться, а не висеть.
        Platform.exit()
    }

    /**
     * Прерывает рабочие потоки при завершении JVM.
     *
     * Потоки загрузки данных и обработки создаются как обычные `Thread`, то есть
     * не-демоны: пока такой поток жив, JVM не завершится, даже если инструмент
     * JavaFX уже остановлен. Имена у них известны (`RunListThreads`,
     * `LoadList…`, `UpdateList…`), поэтому их можно найти и прервать.
     *
     * Прерывание, а не смена на демон: обработка пишет файлы и строки в базу,
     * и обрыв на середине оставил бы после себя недописанный артефакт.
     */
    private fun stopWorkersOnShutdown() {
        val hook = object : Thread() {
            override fun run() {
                Thread.getAllStackTraces().keys
                    .filter { it.isAlive && !it.isDaemon && WORKER_PREFIXES.any { p -> it.name.startsWith(p) } }
                    .forEach { it.interrupt() }
            }
        }
        hook.name = "StopWorkers"
        Runtime.getRuntime().addShutdownHook(hook)
    }

    private companion object {
        /** Префиксы имён потоков, которые мешают завершению JVM. */
        private val WORKER_PREFIXES = listOf("RunListThreads", "LoadList", "UpdateList")
    }
}

fun main() {
    Application.launch(ProjectFXApp::class.java)
}