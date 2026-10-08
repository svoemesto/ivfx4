package com.svoemesto.ivfx.utils

/**
 * Двоичное представление вектора лица (задача #268).
 *
 * **Зачем.** Вектор хранится в базе текстом: 512 чисел через вертикальную
 * черту, около 10 064 байт на лицо. На проект из 107 958 лиц это
 * **1 036 МБ** — больше половины базы. Тот же набор во float32 занимает
 * **211 МБ**, в 4,9 раза меньше.
 *
 * **Формат.** Ровно то, что читает numpy как `'<f4'`: 512 значений float32
 * подряд, little-endian, без заголовка и без разделителей — 2048 байт.
 * Порядок байт совпадает с тем, что пишет `NpyWriter`, поэтому одна и та же
 * матрица получается обоими путями.
 *
 * **Разбор берётся готовым.** Числа разбирает `NpyWriter.appendVector` — тем
 * же кодом, что и матрица для скрипта, и он покрыт тестами на побитовое
 * совпадение с `Float.parseFloat`. Свой разбор здесь был бы вторым
 * разбором: он разошёлся бы с первым рано или поздно, и расхождение
 * обнаружилось бы как тихое изменение сходства с галереей.
 *
 * **Почему не полагаться на длину.** Вектор может оказаться короче 512
 * значений — и тогда в матрице появится дырка, а в галерее персон
 * разъедутся границы. Отсюда [encode] возвращает число записанных значений,
 * и вызывающий решает сам, что делать с неполным вектором.
 */
object VectorBinary {
    /** Сколько значений в векторе лица. */
    const val COMPONENTS = 512

    /** Сколько байт занимает один вектор. */
    const val BYTES = COMPONENTS * 4

    /**
     * Разбирает [text] и укладывает значения в [dest] начиная с [offset].
     *
     * [dest] должен быть длиной не меньше `offset + 2048` — это проверяется.
     * Возвращает число записанных значений: меньше [COMPONENTS] означает, что
     * вектор неполный, и вызывающий обязан это заметить, а не записать как
     * есть.
     */
    fun encode(
        text: String,
        dest: ByteArray,
        offset: Int,
    ): Int {
        require(offset + BYTES <= dest.size) {
            "в массив $offset + $BYTES не влезает, а его длина ${dest.size}"
        }
        val parsed = FloatArray(COMPONENTS)
        val written = NpyWriter.appendVector(text, parsed, 0)
        for (i in 0 until COMPONENTS) {
            val bits = java.lang.Float.floatToRawIntBits(if (i < written) parsed[i] else 0f)
            val at = offset + i * 4
            // Little-endian: младший байт первым — так же, как в заголовке
            // .npy, иначе numpy прочитает числа в обратном порядке байт.
            dest[at] = (bits and 0xFF).toByte()
            dest[at + 1] = ((bits shr 8) and 0xFF).toByte()
            dest[at + 2] = ((bits shr 16) and 0xFF).toByte()
            dest[at + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return written
    }

    /** Укладывает [text] в новый массив ровно на [BYTES] байт. */
    fun toBytes(text: String): ByteArray {
        val dest = ByteArray(BYTES)
        encode(text, dest, 0)
        return dest
    }

    /**
     * Разворачивает [source] начиная с [offset] в [dest].
     *
     * Обратная операция к [encode]. Значения копируются побитово, без
     * пересчёта, — иначе обратное преобразование не было бы обратным.
     */
    fun decode(
        source: ByteArray,
        offset: Int,
        dest: FloatArray,
    ) {
        require(offset + BYTES <= source.size) {
            "в источнике $offset + $BYTES не влезает, а его длина ${source.size}"
        }
        require(dest.size >= COMPONENTS) { "в приёмник ${dest.size} значений, нужно $COMPONENTS" }
        for (i in 0 until COMPONENTS) {
            dest[i] = java.lang.Float.intBitsToFloat(readLittleEndian(source, offset + i * 4))
        }
    }

    /** Четыре байта little-endian в одно число. */
    private fun readLittleEndian(
        source: ByteArray,
        at: Int,
    ): Int {
        val b0 = source[at].toInt() and 0xFF
        val b1 = source[at + 1].toInt() and 0xFF
        val b2 = source[at + 2].toInt() and 0xFF
        val b3 = source[at + 3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    /** Разворачивает весь массив в новый массив из [COMPONENTS] значений. */
    fun toFloats(source: ByteArray): FloatArray {
        val dest = FloatArray(COMPONENTS)
        decode(source, 0, dest)
        return dest
    }

    /**
     * Влезает ли такой [text] в двоичный вектор.
     *
     * Отдельная проверка нужна потому, что [encode] неполный вектор не
     * отбрасывает, а дополняет нулями — и это молчаливый хвост из нулей в
     * матрице, который портит сходство точно так же, как неверное число.
     */
    fun isComplete(text: String): Boolean {
        val written = NpyWriter.appendVector(text, FloatArray(COMPONENTS), 0)
        return written == COMPONENTS
    }
}
