package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.utils.NpyWriter
import com.svoemesto.ivfx.utils.VectorBinary
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Random

/**
 * Двоичное представление вектора лица (задача #268).
 *
 * Проверяется то, что ломается тихо: порядок байт и побитовое совпадение
 * чисел. Если байты разъедутся, numpy прочитает матрицу из мусора, а
 * сходство с галереей станет тихо неверным — ошибки не будет нигде.
 */
@DisplayName("Двоичный вектор лица")
class VectorBinaryTest {
    private fun parseText(text: String): FloatArray {
        val dest = FloatArray(VectorBinary.COMPONENTS)
        val written = NpyWriter.appendVector(text, dest, 0)
        return dest.copyOf(written)
    }

    private fun vectorOf(components: Int = VectorBinary.COMPONENTS): String {
        val random = Random(7L)
        val parts = ArrayList<String>(components)
        for (i in 0 until components) {
            parts.add(String.format(java.util.Locale.ROOT, "%.17g", random.nextDouble() * 2 - 1))
        }
        return parts.joinToString("|")
    }

    @Nested
    @DisplayName("Формат")
    inner class Format {
        @Test
        fun `вектор занимает ровно 2048 байт`() {
            assertEquals(2048, VectorBinary.BYTES)
            assertEquals(512, VectorBinary.COMPONENTS)
            assertEquals(VectorBinary.BYTES, VectorBinary.toBytes(vectorOf()).size)
        }

        /**
         * Порядок байт little-endian: младший байт числа идёт первым.
         *
         * Проверяется на конкретном числе, а не «вообще похоже на
         * float32»: 1.0f в little-endian — это `00 00 80 3F`, и если
         * порядок перепутается, numpy прочитает другое число, а ошибки не
         * будет нигде, кроме тихой порчи сходства.
         */
        @Test
        fun `порядок байт little-endian`() {
            val bytes = VectorBinary.toBytes("1.0")

            assertEquals(4, bytes.size.coerceAtMost(4))
            assertEquals(0x00.toByte(), bytes[0])
            assertEquals(0x00.toByte(), bytes[1])
            assertEquals(0x80.toByte(), bytes[2])
            assertEquals(0x3F.toByte(), bytes[3])
        }

        @Test
        fun `отрицательное число тоже little-endian`() {
            val bytes = VectorBinary.toBytes("-2.0")

            assertEquals(0x00.toByte(), bytes[0])
            assertEquals(0x00.toByte(), bytes[1])
            assertEquals(0x00.toByte(), bytes[2])
            assertEquals(0xC0.toByte(), bytes[3])
        }

        @Test
        fun `запись со смещением не затирает начало`() {
            val dest = ByteArray(VectorBinary.BYTES * 2)
            VectorBinary.encode("1.0|2.0", dest, VectorBinary.BYTES)

            assertEquals(0, dest[0].toInt())
            assertEquals(1.0f, VectorBinary.toFloats(dest.copyOfRange(VectorBinary.BYTES, VectorBinary.BYTES * 2))[0])
        }

        @Test
        fun `слишком короткий массив отвергается`() {
            val tiny = ByteArray(16)
            val failed = runCatching { VectorBinary.encode("1.0", tiny, 0) }.isFailure
            assertTrue(failed, "в массив 16 байт вектор влезать не должен")
        }
    }

    @Nested
    @DisplayName("Круг преобразования")
    inner class RoundTrip {
        @Test
        fun `побитовое совпадение на векторе`() {
            val text = vectorOf()
            val mine = VectorBinary.toFloats(VectorBinary.toBytes(text))
            val theirs = parseText(text)

            for (i in theirs.indices) {
                assertEquals(
                    java.lang.Float.floatToRawIntBits(theirs[i]),
                    java.lang.Float.floatToRawIntBits(mine[i]),
                    "число $i разошлось после круга текст → байты → числа",
                )
            }
        }

        @Test
        fun `научная запись переживает круг`() {
            val text = "-5.96067460719496E-4|1.0e-5|-2.5E-7"
            val mine = VectorBinary.toFloats(VectorBinary.toBytes(text))

            val expected = floatArrayOf(-5.96067460719496E-4f, 1.0e-5f, -2.5E-7f)
            assertArrayEquals(expected, mine.copyOf(3))
        }

        @Test
        fun `круг на сотне случайных векторов`() {
            val random = Random(20261009L)
            repeat(100) { attempt ->
                val parts = ArrayList<String>(VectorBinary.COMPONENTS)
                for (i in 0 until VectorBinary.COMPONENTS) {
                    parts.add(String.format(java.util.Locale.ROOT, "%.17g", random.nextDouble() * 2 - 1))
                }
                val text = parts.joinToString("|")
                val mine = VectorBinary.toFloats(VectorBinary.toBytes(text))
                val theirs = parseText(text)
                for (i in theirs.indices) {
                    assertEquals(
                        java.lang.Float.floatToRawIntBits(theirs[i]),
                        java.lang.Float.floatToRawIntBits(mine[i]),
                        "попытка $attempt, число $i",
                    )
                }
            }
        }

        /**
         * Совпадение с матрицей `.npy` — то, ради чего всё затевается.
         *
         * Галерея уходит скрипту в `.npy`, а из базы пойдёт в двоичном виде.
         * Если эти два пути разойдутся, скрипт получит не то, что лежит в
         * базе, и расхождение обнаружится только как «распознавание стало
         * хуже».
         */
        @Test
        fun `совпадает с тем, что пишет NpyWriter`() {
            val text = vectorOf()
            val fromNpy = FloatArray(VectorBinary.COMPONENTS)
            NpyWriter.appendVector(text, fromNpy, 0)
            val fromBinary = VectorBinary.toFloats(VectorBinary.toBytes(text))

            for (i in fromNpy.indices) {
                assertEquals(
                    java.lang.Float.floatToRawIntBits(fromNpy[i]),
                    java.lang.Float.floatToRawIntBits(fromBinary[i]),
                    "число $i разошлось между .npy и двоичным вектором",
                )
            }
        }
    }

    @Nested
    @DisplayName("Неполный вектор")
    inner class Incomplete {
        /**
         * Короткий вектор не отбрасывается, а дополняется нулями. Это сделано
         * намеренно — исключение посреди миграции остановило бы её
         * посередине, — поэтому проверка полноты вынесена отдельно, и о ней
         * легко забыть. Здесь она закреплена.
         */
        @Test
        fun `короткий вектор дополняется нулями`() {
            val bytes = VectorBinary.toBytes("1.0|2.0")
            val values = VectorBinary.toFloats(bytes)

            assertEquals(1.0f, values[0])
            assertEquals(2.0f, values[1])
            assertEquals(0f, values[2])
            assertEquals(0f, values[VectorBinary.COMPONENTS - 1])
        }

        @Test
        fun `полнота проверяется отдельной функцией`() {
            assertTrue(VectorBinary.isComplete(vectorOf()))
            assertFalse(VectorBinary.isComplete("1.0|2.0"))
            assertFalse(VectorBinary.isComplete(""))
        }

        @Test
        fun `число записанных значений возвращается`() {
            val dest = ByteArray(VectorBinary.BYTES)
            assertEquals(VectorBinary.COMPONENTS, VectorBinary.encode(vectorOf(), dest, 0))
            assertEquals(2, VectorBinary.encode("1.0|2.0", dest, 0))
            assertEquals(0, VectorBinary.encode("", dest, 0))
        }
    }
}
