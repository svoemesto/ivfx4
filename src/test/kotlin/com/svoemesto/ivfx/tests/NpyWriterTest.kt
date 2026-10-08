package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.utils.NpyWriter
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Random

/**
 * Разбор вектора лица из текста и запись матрицы `.npy`.
 *
 * Здесь нет ни репозитория, ни Spring-контекста, ни JavaFX: разбор чистый,
 * поэтому тесты работают за миллисекунды и не зависят ни от H2, ни от
 * состояния рабочей базы.
 *
 * **Почему тесты нужны именно на разборе.** Разбор стоит в самом горячем
 * месте прогона распознавания: на 62 тысячах галерейных векторов он занимает
 * 3 549 мс из 13 887 мс всего прогона. И он молча портит данные — если число
 * разобралось неверно, ошибки нигде не видно, а портится сходство с
 * галереей.
 */
@DisplayName("Разбор вектора лица и запись матрицы")
class NpyWriterTest {
    private fun parse(text: String): FloatArray {
        val dest = FloatArray(4096)
        val written = NpyWriter.appendVector(text, dest, 0)
        return dest.copyOf(written)
    }

    private fun reference(text: String): FloatArray = text.split('|').map { number(it) }.toFloatArray()

    private fun number(text: String): Float = java.lang.Float.parseFloat(text)

    @Nested
    @DisplayName("Виды чисел в данных")
    inner class NumberForms {
        @Test
        fun `обычная дробь`() {
            assertArrayEquals(
                floatArrayOf(0.25f, -0.5f, 0.75f),
                parse("0.25|-0.5|0.75"),
            )
        }

        @Test
        fun `значение из реальных данных`() {
            assertArrayEquals(
                floatArrayOf(-0.36f, 0.1234f, -0.98765f),
                parse("-0.36|0.1234|-0.98765"),
            )
        }

        @Test
        fun `целые числа`() {
            assertArrayEquals(floatArrayOf(0f, 1f, -1f, 42f, -1000f), parse("0|1|-1|42|-1000"))
        }

        /**
         * Научная запись — не экзотика, а обычное дело в данных лиц: у неё
         * очень маленькая мантисса и отрицательный показатель.
         *
         * Первый вариант разбора сворачивал такую запись в мантиссу, то есть
         * менял число на порядки, и вектор уезжал в сторону молча. На трёхстах
         * векторах это давало 101 расхождение, на всех 62 тысячах — ноль
         * расхождений уже после починки.
         */
        @Test
        fun `научная запись с маленькой мантиссой`() {
            assertArrayEquals(
                floatArrayOf(-0.000596067460719496f, 1.0e-5f, -2.5e-7f),
                parse("-5.96067460719496E-4|1.0e-5|-2.5E-7"),
            )
        }

        @Test
        fun `научная запись со знаком показателя и без`() {
            assertArrayEquals(
                floatArrayOf(1500f, -1500f, 2.0e10f, -3.5e-8f),
                parse("1.5e+3|-1.5E3|2E10|-3.5e-8"),
            )
        }

        @Test
        fun `нулевое значение со знаком`() {
            assertArrayEquals(floatArrayOf(-0f, 0f), parse("-0|0"))
        }
    }

    @Nested
    @DisplayName("Границы строки")
    inner class Boundaries {
        @Test
        fun `пустая строка не пишет ничего`() {
            assertEquals(0, NpyWriter.appendVector("", FloatArray(16), 0))
        }

        /**
         * Разделителя в конце нет, и последнее значение теряется легко: если
         * не отличить «черты нет» от «строка кончилась», пропадает ровно
         * последняя компонента вектора, а порча обнаруживается не сразу.
         */
        @Test
        fun `последнее значение без разделителя не теряется`() {
            assertArrayEquals(floatArrayOf(1f, 2f, 3f), parse("1|2|3"))
        }

        @Test
        fun `одно значение без разделителей`() {
            assertArrayEquals(floatArrayOf(-0.75f), parse("-0.75"))
        }

        @Test
        fun `значение без цифр пишется нулём и не зависает`() {
            // Поведение зафиксировано намеренно: испорченный сегмент даёт 0,
            // а не исключение. Молчаливый ноль плох, но исключение посреди
            // 62 тысяч строк хуже: прогон встал бы на первом битом лице.
            // Изменение этого поведения обязано быть осознанным.
            assertArrayEquals(floatArrayOf(0f), parse("|"))
        }
    }

    @Nested
    @DisplayName("Запись в нужное место")
    inner class Placement {
        @Test
        fun `смещение не затирает начало массива`() {
            val dest = FloatArray(6)
            val written = NpyWriter.appendVector("2|3", dest, 3)
            assertEquals(2, written)
            assertArrayEquals(
                floatArrayOf(0f, 0f, 0f, 2f, 3f, 0f),
                dest,
                "первые три элемента должны остаться нетронутыми",
            )
        }

        /**
         * Массив короче вектора — это ошибка вызывающего, и она должна быть
         * видна по возвращённому числу, а не привести к выходу за границу.
         * На прогоне галереи именно эта проверка ловила ошибку счёта строк:
         * вместо числа лиц считали число значений.
         */
        @Test
        fun `короткий массив обрезается и возвращает влезавшее`() {
            val dest = FloatArray(3)
            val written = NpyWriter.appendVector("1|2|3|4|5", dest, 0)
            assertEquals(3, written)
            assertArrayEquals(floatArrayOf(1f, 2f, 3f), dest)
        }

        @Test
        fun `смещение плюс длина равны размеру массива`() {
            val dest = FloatArray(3)
            assertEquals(3, NpyWriter.appendVector("1|2|3", dest, 0))
        }

        @Test
        fun `смещение в конец массива пишет ноль`() {
            val dest = FloatArray(3)
            assertEquals(0, NpyWriter.appendVector("1|2|3", dest, 3))
        }
    }

    @Nested
    @DisplayName("Совпадение с эталонным разбором")
    inner class AgainstReference {
        /**
         * Главная проверка: наш разбор и `Float.parseFloat` должны давать
         * **одинаковые числа**, а не похожие.
         *
         * Сравнение побитовое, потому что «почти равно» здесь не годится:
         * вектор из 512 чисел, в котором один компонент отличается на
         * единицу в последнем разряде, даёт другое сходство и другое
         * назначение персоны.
         */
        @Test
        fun `побитовое совпадение на векторах из данных`() {
            val vectors = realisticVectors()
            assertTrue(vectors.size >= 100, "нужно хотя бы сто векторов, получилось ${vectors.size}")
            var compared = 0
            for (vector in vectors) {
                val mine = parse(vector)
                val theirs = reference(vector)
                assertEquals(theirs.size, mine.size, "разная длина: $vector")
                for (i in mine.indices) {
                    assertEquals(
                        java.lang.Float.floatToRawIntBits(theirs[i]),
                        java.lang.Float.floatToRawIntBits(mine[i]),
                        "расхождение в числе $i вектора длиной ${mine.size}",
                    )
                    compared++
                }
            }
            assertTrue(compared > 50_000, "сравнено чисел: $compared")
        }

        /**
         * Тот же разбор на случайных числах трёх форм. Диапазон ограничен
         * тем, что встречается в векторах лиц: компоненты по модулю близки к
         * единице, а очень маленькие приходят научной записью.
         */
        @Test
        fun `побитовое совпадение на случайных числах`() {
            val random = Random(20261008L)
            repeat(200) { attempt ->
                val text =
                    (0 until 512).joinToString("|") {
                        when (random.nextInt(10)) {
                            0, 1 -> (random.nextDouble() * 2 - 1).toString()
                            2 -> random.nextInt(-1000, 1000).toString()
                            3 -> String.format(java.util.Locale.ROOT, "%.17g", random.nextDouble() * 2 - 1)
                            else -> scientific(random)
                        }
                    }
                val mine = parse(text)
                val theirs = reference(text)
                assertEquals(theirs.size, mine.size, "попытка $attempt: разная длина")
                for (i in mine.indices) {
                    assertEquals(
                        java.lang.Float.floatToRawIntBits(theirs[i]),
                        java.lang.Float.floatToRawIntBits(mine[i]),
                        "попытка $attempt, число $i",
                    )
                }
            }
        }

        @Test
        fun `длинная мантисса не накапливает ошибку округления`() {
            // 20 знаков после точки в double накапливаются точно, а во float
            // мантисса копилась бы 512 шагов подряд. Сравниваем с эталоном.
            val text =
                (0 until 512).joinToString("|") {
                    String.format(java.util.Locale.ROOT, "%.20f", Math.sin(it.toDouble()) * 0.7)
                }
            val mine = parse(text)
            val theirs = reference(text)
            for (i in mine.indices) {
                assertEquals(
                    java.lang.Float.floatToRawIntBits(theirs[i]),
                    java.lang.Float.floatToRawIntBits(mine[i]),
                    "число $i",
                )
            }
        }
    }

    /**
     * Число в научной записи с очень маленькой мантиссой — такая форма
     * встречается в векторах лиц и ломает наивный разбор.
     */
    private fun scientific(random: Random): String {
        val sign = if (random.nextBoolean()) "-" else ""
        val mantissa = random.nextInt(1_000_000_000).toString().padStart(9, '0')
        return String.format(
            java.util.Locale.ROOT,
            "%s%d.%sE-%d",
            sign,
            random.nextInt(9) + 1,
            mantissa,
            random.nextInt(1, 12),
        )
    }

    /**
     * Векторы тех же трёх форм, что и в данных: обычная дробь, целое и
     * научная запись с очень маленькой мантиссой.
     */
    private fun realisticVectors(): List<String> {
        val random = Random(4242L)
        val result = ArrayList<String>()
        while (result.size < 300) {
            val text =
                (0 until 512).joinToString("|") {
                    when (random.nextInt(12)) {
                        0 -> String.format(java.util.Locale.ROOT, "%.17g", (random.nextDouble() * 2 - 1))
                        1 -> (random.nextInt(-3, 3)).toString()
                        else ->
                            String.format(
                                java.util.Locale.ROOT,
                                "%.17g",
                                (random.nextDouble() * 2 - 1) * Math.pow(10.0, -random.nextInt(1, 8).toDouble()),
                            )
                    }
                }
            result.add(text)
        }
        return result
    }
}
