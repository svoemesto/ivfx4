package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.models.Shot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Порядок планов и свёртка выборок фильтра — чистая арифметика без базы.
 *
 * Здесь нет ни репозитория, ни Spring-контекста, ни JavaFX: сущности
 * создаются напрямую, поля заполняются вручную. Поэтому тесты работают
 * за миллисекунды и не зависят ни от H2, ни от состояния рабочей базы.
 */
@DisplayName("Порядок планов и свёртка выборок")
class ShotAndFilterTest {
    private fun shot(
        fileOrder: Int,
        firstFrame: Int,
        lastFrame: Int = firstFrame,
    ): Shot {
        val file = File()
        file.order = fileOrder
        val shot = Shot()
        shot.file = file
        shot.firstFrameNumber = firstFrame
        shot.lastFrameNumber = lastFrame
        return shot
    }

    @Nested
    @DisplayName("Shot.compareTo — планы упорядочены по файлу, затем по кадру")
    inner class ShotOrdering {
        @Test
        @DisplayName("внутри одного файла раньше идёт меньший номер кадра")
        fun `внутри файла порядок по кадру`() {
            val first = shot(fileOrder = 1, firstFrame = 10)
            val second = shot(fileOrder = 1, firstFrame = 20)
            assertTrue(first < second, "план с кадра 10 должен идти раньше плана с кадра 20")
            assertTrue(second > first)
        }

        @Test
        @DisplayName("одинаковые планы равны")
        fun `одинаковые планы равны`() {
            assertEquals(0, shot(1, 10).compareTo(shot(1, 10)))
        }

        @Test
        @DisplayName("порядок файла важнее номера кадра")
        fun `файл важнее кадра`() {
            // Первый файл, но кадр позже — всё равно раньше второго файла.
            val fromFirstFile = shot(fileOrder = 1, firstFrame = 9_000)
            val fromSecondFile = shot(fileOrder = 2, firstFrame = 10)
            assertTrue(fromFirstFile < fromSecondFile)
        }

        @Test
        @DisplayName("граница веса файла — миллион кадров")
        fun `миллион кадров перекрывает разницу в порядке файлов`() {
            // Порядок файла умножается на 1_000_000. На миллионе кадров
            // разница в один файл и разница в миллион кадров
            // взаимно уничтожаются, и планы считаются равными.
            //
            // На практике недостижимо: миллион кадров при 25 fps — это
            // больше 11 часов видео. Закреплено, чтобы при смене веса
            // изменение не прошло молча.
            val earlyFrameInFirstFile = shot(fileOrder = 1, firstFrame = 1_000_000)
            val lateFrameInSecondFile = shot(fileOrder = 2, firstFrame = 0)
            assertEquals(0, earlyFrameInFirstFile.compareTo(lateFrameInSecondFile))
        }
    }

    @Nested
    @DisplayName("Порядок фильтров и групп")
    inner class FilterOrdering {
        @Test
        @DisplayName("фильтры сравниваются по полю order")
        fun `фильтры по order`() {
            val first = Filter()
            first.order = 1
            val second = Filter()
            second.order = 2
            assertTrue(first < second)
            assertEquals(0, first.compareTo(first))
        }

        @Test
        @DisplayName("группы сравниваются по полю order")
        fun `группы по order`() {
            val first = FilterGroup()
            first.order = 3
            val second = FilterGroup()
            second.order = 7
            assertTrue(first < second)
        }
    }

    @Nested
    @DisplayName("Пустая выборка — пустая, а не «всё»")
    inner class EmptyCollapse {
        @Test
        @DisplayName("фильтр без групп даёт пустой результат при И")
        fun `пустой фильтр И`() {
            val filter = Filter()
            filter.isAnd = true
            assertEquals(
                emptySet<Long>(),
                com.svoemesto.ivfx.modelsext
                    .FilterExt(filter)
                    .shotsIds(),
            )
        }

        @Test
        @DisplayName("фильтр без групп даёт пустой результат при ИЛИ")
        fun `пустой фильтр ИЛИ`() {
            val filter = Filter()
            filter.isAnd = false
            assertEquals(
                emptySet<Long>(),
                com.svoemesto.ivfx.modelsext
                    .FilterExt(filter)
                    .shotsIds(),
            )
        }

        @Test
        @DisplayName("группа без условий даёт пустой результат при И")
        fun `пустая группа И`() {
            val group = FilterGroup()
            group.isAnd = true
            assertEquals(
                emptySet<Long>(),
                com.svoemesto.ivfx.modelsext
                    .FilterGroupExt(group)
                    .shotsIds(),
            )
        }

        @Test
        @DisplayName("группа без условий даёт пустой результат при ИЛИ")
        fun `пустая группа ИЛИ`() {
            val group = FilterGroup()
            group.isAnd = false
            assertEquals(
                emptySet<Long>(),
                com.svoemesto.ivfx.modelsext
                    .FilterGroupExt(group)
                    .shotsIds(),
            )
        }

        @Test
        @DisplayName("текст режима совпадает с режимом")
        fun `текст режима`() {
            val groupAnd = FilterGroup()
            groupAnd.isAnd = true
            val groupOr = FilterGroup()
            groupOr.isAnd = false
            assertEquals(
                "&&",
                com.svoemesto.ivfx.modelsext
                    .FilterGroupExt(groupAnd)
                    .isAndText,
            )
            assertEquals(
                "||",
                com.svoemesto.ivfx.modelsext
                    .FilterGroupExt(groupOr)
                    .isAndText,
            )
        }
    }
}
