package com.svoemesto.ivfx.utils

import javax.persistence.EntityManager

/**
 * Перезапись текстовых векторов в двоичные (задача #268).
 *
 * **Что делает.** Берёт лиц из `tbl_faces` партиями, разбирает `vector` тем
 * же кодом, что и матрица для скрипта ([VectorBinary]), и кладёт рядом
 * 2048 байт в `vector_bin`.
 *
 * **Почему проверка обязательна.** Правило 2.9 появилось из-за аварии, где
 * векторы стёрлись у 7 597 лиц, и заметить это было нечем: «строки
 * переписаны» доказательством не является. Поэтому каждая строка после
 * разбора сверяется **побитово** с тем, что лежит в тексте, и расхождение
 * останавливает всё с кодом 2 — до того, как что-то записано.
 *
 * **Режимы.**
 *
 * ```
 * --dry-run     только проверить, ничего не писать (по умолчанию)
 * --apply       перезаписать
 * --batch=N     размер партии, по умолчанию 2000
 * ```
 */
object VectorBinMigration {
    const val EXIT_OK = 0
    const val EXIT_MISMATCH = 2

    /**
     * Одно лицо: результат разбора и признак, что он полон.
     *
     * Отдельный класс, а не пара значений, потому что «полон» и «записан» —
     * разные вещи: неполный вектор пишется с нулями, и это молчаливый хвост,
     * который портит сходство.
     */
    class Row {
        var id: Long = 0
        var text: String = ""
        var bytes: ByteArray? = null
        var components: Int = 0
    }

    /**
     * Разбирает лицо в двоичный вид и **сверяет результат с исходным
     * текстом**. Возвращает `false`, если числа разошлись.
     *
     * Сверка идёт обратным ходом: из собранных байт собираются числа и
     * сравниваются с тем, что даёт эталонный разбор текста. Сравение
     * «с тем же кодом, что и запись» было бы круговым и ничего не проверяло.
     */
    fun convert(row: Row): Boolean {
        if (row.text.isEmpty()) {
            row.bytes = null
            row.components = 0
            return true
        }
        val bytes = ByteArray(VectorBinary.BYTES)
        val written = VectorBinary.encode(row.text, bytes, 0)
        row.components = written

        val fromBinary = VectorBinary.toFloats(bytes)
        val fromText = FloatArray(VectorBinary.COMPONENTS)
        val reference = NpyWriter.appendVector(row.text, fromText, 0)
        if (reference != written) {
            return false
        }
        for (i in 0 until reference) {
            if (java.lang.Float.floatToRawIntBits(fromBinary[i]) != java.lang.Float.floatToRawIntBits(fromText[i])) {
                return false
            }
        }
        row.bytes = bytes
        return true
    }

    /**
     * Проверка без записи: сколько лиц разбирается, сколько расходится и
     * сколько векторов неполных.
     */
    class Report {
        var checked = 0
        var mismatched = 0
        var incomplete = 0
        var failedIds = ArrayList<Long>()

        fun line(): String =
            "проверено $checked, расхождений $mismatched, неполных $incomplete" +
                if (failedIds.isEmpty()) "" else ", первые расхождения: ${failedIds.take(10)}"
    }

    /** Читает лиц и возвращает их одним проходом по базе. */
    fun loadBatch(
        entityManager: EntityManager,
        afterId: Long,
        batchSize: Int,
    ): List<Row> {
        val rows =
            entityManager
                .createNativeQuery("SELECT id, vector FROM tbl_faces WHERE id > ?1 ORDER BY id LIMIT ?2")
                .setParameter(1, afterId)
                .setParameter(2, batchSize)
                .resultList as List<Array<Any?>>
        return rows.map { raw ->
            val row = Row()
            row.id = (raw[0] as Number).toLong()
            row.text = raw[1] as String? ?: ""
            row
        }
    }

    /** Проверяет партию, ничего не записывая. */
    fun dryRun(
        entityManager: EntityManager,
        batchSize: Int,
        onProgress: (Report) -> Unit,
    ): Report {
        val report = Report()
        var afterId = 0L
        while (true) {
            val batch = loadBatch(entityManager, afterId, batchSize)
            if (batch.isEmpty()) break
            for (row in batch) {
                report.checked++
                afterId = row.id
                if (!convert(row)) {
                    report.mismatched++
                    if (report.failedIds.size < 10) report.failedIds.add(row.id)
                }
                if (row.components in 1 until VectorBinary.COMPONENTS) report.incomplete++
            }
            onProgress(report)
        }
        return report
    }

    /**
     * Проверяет и записывает.
     *
     * Запись идёт только после того, как партия **целиком** сошлась: если
     * хотя бы одно лицо разошлось, партия не пишется вовсе. Иначе база
     * получила бы часть верных и часть сомнительных строк, и разбираться
     * потом пришлось бы по факту.
     */
    fun apply(
        entityManager: EntityManager,
        batchSize: Int,
        onProgress: (Report) -> Unit,
    ): Report {
        val report = Report()
        var afterId = 0L
        while (true) {
            val batch = loadBatch(entityManager, afterId, batchSize)
            if (batch.isEmpty()) break
            val bad = batch.filter { !convert(it) }
            if (bad.isNotEmpty()) {
                report.mismatched += bad.size
                bad.mapTo(report.failedIds) { it.id }
                onProgress(report)
                return report
            }
            batch.forEach {
                report.checked++
                afterId = it.id
                if (it.components in 1 until VectorBinary.COMPONENTS) report.incomplete++
            }
            val update =
                entityManager.createNativeQuery(
                    "UPDATE tbl_faces SET vector_bin = :bin WHERE id = :id",
                )
            entityManager.transaction.begin()
            try {
                for (row in batch) {
                    update
                        .setParameter("bin", row.bytes)
                        .setParameter("id", row.id)
                        .executeUpdate()
                }
                entityManager.transaction.commit()
            } catch (e: Exception) {
                entityManager.transaction.rollback()
                throw e
            }
            onProgress(report)
        }
        return report
    }
}
