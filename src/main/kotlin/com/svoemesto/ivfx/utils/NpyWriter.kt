package com.svoemesto.ivfx.utils

import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Запись матрицы в формате `.npy` — тот самый, который numpy читает
 * `np.load` без всякого разбора.
 *
 * **Зачем.** Векторы лиц передавались скрипту распознавания в json. На
 * прогоне седьмой серии это 583 МБ текста: `json.load` на нём занимал
 * **10,1 секунды** — больше четверти всего прогона, — а столько же стоило
 * написание файла. Векторы — это числа, а не текст, и скрипту нужно только
 * умножать матрицу на матрицу. Тот же набор в float32 занимает 109 МБ и
 * читается мгновенно.
 *
 * Формат записан вручную, а не взятым из numpy, потому что писать его —
 * двадцать строк, а тянуть зависимость ради этого не нужно. Что именно
 * ожидает читатель:
 *
 * ```
 * \x93NUMPY \x01 \x00 <длина заголовка: 2 байта> {заголовок, дополненный пробелами до кратного 64} \n <данные>
 * ```
 *
 * Данные идут построчно, little-endian, float32 — ровно то, что numpy
 * называет `'<f4'` с `fortran_order = False`.
 */
object NpyWriter {
    /**
     * Записывает [matrix] в [file] как матрицу float32.
     *
     * [matrix] — данные построчно, по [columns] чисел в строке.
     */
    fun write(
        file: File,
        matrix: FloatArray,
        rows: Int,
        columns: Int,
    ) {
        BufferedOutputStream(file.outputStream(), 1 shl 20).use { stream ->
            writeHeader(stream, rows, columns)
            writeData(stream, matrix, columns)
        }
    }

    private fun writeHeader(
        stream: OutputStream,
        rows: Int,
        columns: Int,
    ) {
        var header = "{'descr': '<f4', 'fortran_order': False, 'shape': ($rows, $columns), }"
        // Заголовок вместе с магией и длиной обязан занимать кратное 64 байта:
        // numpy выравнивает по нему начало данных, иначе не прочитает файл.
        var padding = 64 - (10 + header.length + 1) % 64
        if (padding == 64) padding = 0
        header += " ".repeat(padding) + "\n"

        stream.write(MAGIC)
        stream.write(VERSION_MAJOR)
        stream.write(VERSION_MINOR)
        val length = header.toByteArray(Charsets.US_ASCII)
        stream.write(length.size and 0xFF)
        stream.write((length.size shr 8) and 0xFF)
        stream.write(length)
    }

    private fun writeData(
        stream: OutputStream,
        matrix: FloatArray,
        columns: Int,
    ) {
        val bytes = ByteArray(columns * 4)
        var row = 0
        while (row * columns < matrix.size) {
            for (column in 0 until columns) {
                val bits = java.lang.Float.floatToIntBits(matrix[row * columns + column])
                bytes[column * 4] = (bits and 0xFF).toByte()
                bytes[column * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
                bytes[column * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
                bytes[column * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
            }
            stream.write(bytes)
            row++
        }
    }

    /**
     * Разбирает вектор из текста в числа **без единого выделения памяти**.
     *
     * Значения разделены вертикальной чертой, разделителя в конце нет.
     * Отсюда тонкость: когда черты больше нет, строка просто кончилась, и
     * последнее значение надо записать. Если это не отличить от «разделителя
     * не нашлось», теряется ровно одно число на конце, то есть последняя
     * компонента вектора, и порча обнаруживается не сразу.
     *
     * **Числа в данных бывают трёх видов**: обычные (`-0.3668363690376282`),
     * с очень малой мантиссой в научной записи (`-5.96067460719496E-4`) и
     * целые. Научная запись обязана разбираться честно: если её свернуть в
     * мантиссу, число меняется на порядки, и вектор уезжает в сторону, молча
     * портя сходство с целой галереей.
     *
     * **Почему вручную.** Три способа, которые выглядят одинаково, на
     * 53 000 векторов дают разное:
     *
     * | способ | время |
     * |---|---|
     * | регулярка `split` плюс `Double.parseDouble` | 9 058 мс |
     * | `substring` плюс `toFloatOrNull` | 15 354 мс |
     * | этот разбор | см. отчёт задачи |
     *
     * Второй медленнее первого в полтора раза, и это неочевидно: подстрока на
     * каждое число — это 27 миллионов объектов String, а `toFloatOrNull` —
     * это try/catch внутри цикла. Здесь ни того, ни другого: символы читаются
     * напрямую, значение собирается из цифр на месте.
     *
     * Накопление идёт в double: во float мантисса копится 512 шагов подряд и
     * накапливает ошибку округления. В double накопление точное, и приведение
     * к float в конце даёт то же значение, что и Float.parseFloat.
     *
     * Возвращает количество записанных чисел.
     */
    fun appendVector(
        text: String,
        dest: FloatArray,
        offset: Int,
    ): Int {
        val length = text.length
        if (length == 0) return 0
        var written = 0
        var i = 0
        while (i < length) {
            if (offset + written >= dest.size) break
            var sign = 1.0
            if (text[i] == '-') {
                sign = -1.0
                i++
            }
            var value = 0.0
            var digits = 0
            while (i < length && text[i] in '0'..'9') {
                value = value * 10.0 + (text[i] - '0')
                digits++
                i++
            }
            if (i < length && text[i] == '.') {
                i++
                var scale = 0.1
                while (i < length && text[i] in '0'..'9') {
                    value += (text[i] - '0') * scale
                    scale *= 0.1
                    digits++
                    i++
                }
            }
            if (digits > 0 && i < length && (text[i] == 'e' || text[i] == 'E')) {
                i++
                var exponentSign = 1
                if (i < length && (text[i] == '-' || text[i] == '+')) {
                    if (text[i] == '-') exponentSign = -1
                    i++
                }
                var exponent = 0
                while (i < length && text[i] in '0'..'9') {
                    exponent = exponent * 10 + (text[i] - '0')
                    i++
                }
                value *= pow10(exponentSign * exponent)
            }
            dest[offset + written] = if (digits > 0) (sign * value).toFloat() else 0f
            written++
            if (i < length && text[i] == SEPARATOR) i++
        }
        return written
    }

    private fun pow10(exponent: Int): Double {
        var result = 1.0
        var remaining = if (exponent < 0) -exponent else exponent
        var factor = if (exponent < 0) 0.1 else 10.0
        while (remaining > 0) {
            if ((remaining and 1) == 1) result *= factor
            factor *= factor
            remaining = remaining shr 1
        }
        return result
    }

    private const val SEPARATOR = '|'

    private val MAGIC =
        byteArrayOf(0x93.toByte(), 'N'.code.toByte(), 'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte())
    private val VERSION_MAJOR = byteArrayOf(1)
    private val VERSION_MINOR = byteArrayOf(0)
}
