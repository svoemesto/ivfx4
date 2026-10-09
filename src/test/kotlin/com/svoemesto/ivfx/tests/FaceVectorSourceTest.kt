package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.utils.NpyWriter
import com.svoemesto.ivfx.utils.VectorBinary
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Random

/**
 * Выбор источника вектора лица (задача #268).
 *
 * Проверяется ровно то, что ломается тихо: если вместо двоичного вектора
 * молча взять нули, лицо уедет из своей персоны, и нигде не будет ошибки.
 */
@DisplayName("Источник вектора лица")
class FaceVectorSourceTest {
    private fun vectorText(components: Int = VectorBinary.COMPONENTS): String {
        val random = Random(4242L)
        val parts = ArrayList<String>(components)
        for (i in 0 until components) {
            parts.add(String.format(java.util.Locale.ROOT, "%.17g", random.nextDouble() * 2 - 1))
        }
        return parts.joinToString("|")
    }

    @Test
    fun `лицо без двоичного вектора читается по тексту`() {
        val face = Face()
        face.vectorText = "1.5|-2.25|3.0"

        assertNull(face.vectorFloats(), "двоичной колонки нет — значит и чисел из неё быть не должно")
        assertArrayEquals(doubleArrayOf(1.5, -2.25, 3.0), face.vector)
    }

    @Test
    fun `двоичный вектор побеждает текст`() {
        val text = vectorText()
        val face = Face()
        face.vectorText = "0.0"
        face.vectorBinary = VectorBinary.toBytes(text)

        val floats = face.vectorFloats()
        assertNotNull(floats)

        val reference = FloatArray(VectorBinary.COMPONENTS)
        NpyWriter.appendVector(text, reference, 0)
        for (i in reference.indices) {
            assertEquals(
                java.lang.Float.floatToRawIntBits(reference[i]),
                java.lang.Float.floatToRawIntBits(floats!![i]),
                "число $i разошлось: взято не то, что лежит в двоичной колонке",
            )
        }
    }

    @Test
    fun `полный вектор возвращает те же числа, что лежат в базе`() {
        val text = vectorText()
        val face = Face()
        face.vectorText = "текст-заглушка, который не должен читаться"
        face.vectorBinary = VectorBinary.toBytes(text)

        val fromFace = face.vector
        assertEquals(VectorBinary.COMPONENTS, fromFace.size)
        val reference = face.vectorFloats()!!
        for (i in reference.indices) {
            assertEquals(reference[i].toDouble(), fromFace[i], "значение $i разошлось")
        }
    }

    /**
     * Обрезанный двоичный вектор читать нельзя: из 2044 байт 512 значений не
     * получится, а дописать нули — значит тихо испортить сходство. Поэтому
     * берётся текст.
     */
    @Test
    fun `обрезанный двоичный вектор не читается, берётся текст`() {
        val face = Face()
        face.vectorText = "1.0|2.0|3.0"
        face.vectorBinary = ByteArray(2044)

        assertNull(face.vectorFloats(), "неполный двоичный вектор обязан считаться отсутствующим")
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), face.vector)
    }

    @Test
    fun `запись полного вектора заполняет обе колонки одинаково`() {
        val face = Face()
        val values = DoubleArray(VectorBinary.COMPONENTS)
        val random = Random(99L)
        for (i in values.indices) values[i] = random.nextDouble() * 2 - 1

        face.vector = values

        assertNotNull(face.vectorBinary, "полный вектор обязан попасть в двоичную колонку")
        assertEquals(VectorBinary.BYTES, face.vectorBinary!!.size)
        for (i in values.indices) {
            assertEquals(
                java.lang.Float.floatToRawIntBits(values[i].toFloat()),
                java.lang.Float.floatToRawIntBits(face.vectorFloats()!![i]),
                "значение $i разошлось между текстом и двоичной колонкой",
            )
        }
    }

    /**
     * Неполный вектор в двоичную колонку **не пишется**.
     *
     * Иначе получился бы нулевой хвост, неотличимый от настоящих нулей, и
     * лицо ушло бы в другую персону без единой ошибки в журнале.
     */
    @Test
    fun `неполный вектор в двоичную колонку не пишется`() {
        val face = Face()

        face.vector = DoubleArray(300) { 0.5 }

        assertNull(face.vectorBinary, "неполный вектор обязан остаться только в тексте")
        assertEquals(300, face.vector.size)
        assertEquals(300, face.vectorText.split("|").size)
    }

    @Test
    fun `пустой вектор не ломает ни одну из колонок`() {
        val face = Face()

        face.vector = DoubleArray(0)

        assertEquals("", face.vectorText)
        assertNull(face.vectorBinary)
    }

    /**
     * Круг «записать → прочитать» не должен терять разрядность.
     *
     * Значение, не представимое точно в `double`, обязано остаться тем же в
     * `float`: сужение до float32 — суть формата, и тест фиксирует именно
     * его, а не случайное округление.
     */
    @Test
    fun `круг записи и чтения не теряет и не добавляет значений`() {
        val face = Face()
        val values = DoubleArray(VectorBinary.COMPONENTS)
        val random = Random(777L)
        for (i in values.indices) values[i] = (random.nextDouble() * 2 - 1).toFloat().toDouble()

        face.vector = values
        val read = face.vector

        assertEquals(values.size, read.size)
        for (i in values.indices) {
            assertEquals(values[i], read[i], "значение $i изменилось по дороге")
        }
        assertTrue(face.vectorText.isNotEmpty())
    }

    @Test
    fun `разбор текста матрицы совпадает с чтением двоичного вектора`() {
        val text = vectorText()
        val face = Face()
        face.vectorText = text
        face.vectorBinary = VectorBinary.toBytes(text)

        val viaMatrix = FloatArray(VectorBinary.COMPONENTS)
        NpyWriter.appendVector(face.vectorText, viaMatrix, 0)
        val viaEntity = face.vectorFloats()!!

        for (i in viaMatrix.indices) {
            assertEquals(
                java.lang.Float.floatToRawIntBits(viaMatrix[i]),
                java.lang.Float.floatToRawIntBits(viaEntity[i]),
                "число $i разошлось между путями",
            )
        }
    }
}
