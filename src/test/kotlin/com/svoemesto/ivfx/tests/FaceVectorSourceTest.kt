package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.utils.VectorBinary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Random

/**
 * Вектор лица после удаления текстовой колонки (задача #268).
 *
 * Проверяется то, что ломается тихо: если вместо вектора подставить нули,
 * лицо уедет из своей персоны, и нигде не будет ошибки.
 */
@DisplayName("Вектор лица без текстовой колонки")
class FaceVectorSourceTest {
    /**
     * Вектор из значений, **представимых в float32**.
     *
     * Именно таких, потому что формат — 512 float32, и сужение до них есть
     * суть хранения. Значение `double`, не представимое в float32, при
     * обратном чтении закономерно изменится, и требовать тут точного
     * совпадения нельзя: это проверка не того.
     */
    private fun fullVector(seed: Long): DoubleArray {
        val random = Random(seed)
        val values = DoubleArray(VectorBinary.COMPONENTS)
        for (i in values.indices) values[i] = (random.nextDouble() * 2 - 1).toFloat().toDouble()
        return values
    }

    /**
     * Сужение до float32 — часть формата, а не потеря.
     *
     * Значение, которое в float32 не представимо, читается обратно чуть иначе;
     * это ожидаемо, и проверяется явно, чтобы никто не принял это за порчу.
     */
    @Test
    fun `значение, не представимое в float, сужается закономерно`() {
        val face = Face()
        val wide = 0.41445744410093566
        assertFalse(wide.toFloat().toDouble() == wide, "исходное значение должно быть не представимо в float32")

        face.vector = DoubleArray(VectorBinary.COMPONENTS) { wide }

        assertEquals(wide.toFloat().toDouble(), face.vector[0])
    }

    @Test
    fun `лицо без вектора отдаёт пусто, а не нули`() {
        val face = Face()
        assertFalse(face.hasVector())
        assertNull(face.vectorFloats(), "нет данных — значит null, а не 512 нулей")
        assertEquals(0, face.vector.size)
    }

    @Test
    fun `двоичный вектор читается в числа`() {
        val values = fullVector(4242L)
        val face = Face()
        face.vectorBinary = VectorBinary.toBytes(values.joinToString("|"))
        assertTrue(face.hasVector())
        val floats = face.vectorFloats()
        assertNotNull(floats)
        for (i in values.indices) {
            assertEquals(
                java.lang.Float.floatToRawIntBits(values[i].toFloat()),
                java.lang.Float.floatToRawIntBits(floats!![i]),
                "число $i разошлось",
            )
        }
    }

    @Test
    fun `обрезанный вектор не считается за вектор`() {
        val face = Face()
        face.vectorBinary = ByteArray(2044)
        assertFalse(face.hasVector())
        assertNull(face.vectorFloats())
        assertEquals(0, face.vector.size)
    }

    @Test
    fun `запись полного вектора заполняет двоичную колонку`() {
        val face = Face()
        val values = fullVector(99L)
        face.vector = values
        assertNotNull(face.vectorBinary)
        assertEquals(VectorBinary.BYTES, face.vectorBinary!!.size)
        val read = face.vectorFloats()!!
        for (i in values.indices) {
            assertEquals(
                java.lang.Float.floatToRawIntBits(values[i].toFloat()),
                java.lang.Float.floatToRawIntBits(read[i]),
                "значение $i разошлось между записью и чтением",
            )
        }
    }

    /**
     * Неполный вектор в базу не попадает.
     *
     * Иначе получился бы нулевой хвост, неотличимый от настоящих нулей: лицо
     * ушло бы в другую персону, и в журнале не было бы ни одной ошибки.
     */
    @Test
    fun `неполный вектор не пишется вовсе`() {
        val face = Face()
        face.vector = DoubleArray(300) { 0.5 }
        assertNull(face.vectorBinary, "неполный вектор обязан остаться незаписанным")
        assertFalse(face.hasVector())
    }

    @Test
    fun `пустой вектор не ломает лицо`() {
        val face = Face()
        face.vector = DoubleArray(0)
        assertNull(face.vectorBinary)
        assertEquals(0, face.vector.size)
    }

    @Test
    fun `круг записи и чтения ничего не меняет`() {
        val face = Face()
        val values = fullVector(777L)
        face.vector = values
        val read = face.vector
        assertEquals(values.size, read.size)
        for (i in values.indices) {
            assertEquals(values[i], read[i], "значение $i изменилось по дороге")
        }
    }

    @Test
    fun `перезапись двоичного вектора заменяет прежний`() {
        val face = Face()
        face.vector = DoubleArray(VectorBinary.COMPONENTS) { 0.25 }
        val first = face.vectorBinary!!.copyOf()
        face.vector = DoubleArray(VectorBinary.COMPONENTS) { -0.75 }
        val same = first.contentEquals(face.vectorBinary!!)
        assertFalse(same, "прежний вектор должен был замениться, а не остаться")
        assertEquals(-0.75f, face.vectorFloats()!![0])
    }

    @Test
    fun `значения, точные в float, переживают круг без потерь`() {
        val face = Face()
        val values = DoubleArray(VectorBinary.COMPONENTS) { (it % 3).toDouble() }
        face.vector = values
        val read = face.vector
        for (i in values.indices) {
            assertEquals(values[i], read[i], "значение $i изменилось, а было точным")
        }
    }
}
