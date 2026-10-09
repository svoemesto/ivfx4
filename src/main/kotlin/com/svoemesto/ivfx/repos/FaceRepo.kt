package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Face
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FaceRepo : CrudRepository<Face, Long> {
    fun findByFileId(fileId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 LIMIT 1", nativeQuery = true)
    fun getFirstByFileId(fileId: Long): Iterable<Face>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_faces WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId: Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_faces WHERE id = ?", nativeQuery = true)
    fun delete(frameId: Long)

    /** Лица одного трека — нужно таблице треков и разделению трека. */
    fun findByTrackId(trackId: Long): Iterable<Face>

    @Transactional
    /**
     * Привязка лиц к треку одним UPDATE.
     *
     * Раньше связь ставилась через save трека: Hibernate в тот же момент
     * переписывал изменённые лица ЦЕЛИКОМ, вместе с вектором признаков —
     * десять килобайт текстом на лицо. Лиц в серии девять тысяч, треков
     * четыре тысячи, и на каждом сохранении вся эта масса пересматривалась
     * заново: шаг шёл со скоростью два лица в секунду.
     *
     * Здесь меняется только колонка track_id, вектор не трогается и лицо
     * не попадает в сессию как изменённое.
     */
    @Modifying
    @Query(value = "UPDATE tbl_faces SET track_id = ?1 WHERE id IN (?2)", nativeQuery = true)
    fun linkFacesToTrack(
        trackId: Long,
        faceIds: Collection<Long>,
    ): Int

    @Transactional
    /**
     * Назначение персоны лицу одним UPDATE — по той же причине, что и
     * linkFacesToTrack: лицо не должно переписываться целиком.
     */
    @Modifying
    @Query(
        value = "UPDATE tbl_faces SET person_id = ?1, person_recognized_name = ?2 WHERE id = ?3",
        nativeQuery = true,
    )
    fun assignPersonToFace(
        personId: Long,
        personRecognizedName: String,
        faceId: Long,
    ): Int

    /**
     * Лица указанных треков одним запросом.
     *
     * Нужно для списка треков плана: у лица лежит вектор признаков, около
     * десяти килобайт текстом, и чтение лиц ВСЕЙ серии ради одного плана
     * тянуло 86 МБ — на переходе между планами это и было задержкой.
     * Лица одного плана — это десятки записей вместо десяти тысяч.
     */
    @Query("select f from Face f where f.track is not null and f.track.id in :ids")
    fun findByTrackIds(ids: Collection<Long>): Iterable<Face>

    /**
     * Первая и последняя серия, где встречалась персона: [fileId, название]
     * для каждой границы либо пустой список, если лиц у персоны нет.
     *
     * Порядок берётся у файла, а не у лица: в самой таблице лиц серии нет,
     * и порядок выдачи запроса нигде не гарантирован.
     */
    @Query(
        value = """
            SELECT MIN(fl.order_file) AS fileId, MIN(fl.short_name) AS shortName FROM tbl_faces f
              JOIN tbl_files fl ON fl.id = f.file_id WHERE f.person_id = ?1
            """,
        nativeQuery = true,
    )
    fun getFirstSeriesOfPerson(personId: Long): Iterable<SeriesBound>

    @Query(
        value = """
            SELECT MAX(fl.order_file) AS fileId, MAX(fl.short_name) AS shortName FROM tbl_faces f
              JOIN tbl_files fl ON fl.id = f.file_id WHERE f.person_id = ?1
            """,
        nativeQuery = true,
    )
    fun getLastSeriesOfPerson(personId: Long): Iterable<SeriesBound>

    interface SeriesBound {
        val fileId: Long?
        val shortName: String?
    }

    /** Сколько лиц каждого персонажа файла состоят в треках: пара «идентификатор персоны, число». */
    @Query(
        value =
            "select f.person.id, count(f) from Face f " +
                "where f.track is not null and f.file.id = ?1 group by f.person.id",
    )
    fun getTrackFacesCountByPerson(fileId: Long): List<Array<Any>>

    fun findByFileIdAndFrameNumber(
        fileId: Long,
        frameNumber: Int,
    ): Iterable<Face>

    fun findByFileIdAndFrameNumberAndFaceNumberInFrame(
        fileId: Long,
        frameNumber: Int,
        faceNumberInFrame: Int,
    ): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id != ?2", nativeQuery = true)
    fun findByFileIdAndPersonIdNotEqual(
        fileId: Long,
        personId: Long,
    ): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id != ?2 LIMIT 1", nativeQuery = true)
    fun findFirstByFileIdAndPersonIdNotEqual(
        fileId: Long,
        personId: Long,
    ): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id = ?2", nativeQuery = true)
    fun findFacesToRecognize(
        fileId: Long,
        idPersonUnrecognized: Long,
    ): Iterable<Face>

    /**
     * Все лица проекта, а не одного файла.
     *
     * Галерея распознавания собирается по проекту: серии одного сериала
     * показывают одних и тех же людей, и отмеченные лица первой серии —
     * образцы для второй. Если брать только файл, на котором идёт
     * распознавание, то у новой серии своих отмеченных лиц нет, галерея
     * выходит пустой, и шаг завершается, не найдя никого.
     *
     * Запрос по одной персоне, а не сразу по проекту, — намеренно: список
     * лиц по проекту возвращает лица с отложенной связью person, и чтение
     * её вне сессии падает. Обход по персонам позволяет собрать галерею, не
     * касаясь отложенных связей вовсе.
     */
    fun findByPersonId(personId: Long): Iterable<Face>

    /**
     * Лица одной персоны в одной серии.
     *
     * Обход идёт парами «серия + персона» намеренно: так у каждого лица
     * известен его файл, и отложенную связь file можно заменить настоящим
     * объектом. Без этого конструктор FaceExt читает file.shortName и
     * падает с LazyInitializationException.
     */
    fun findAllByFileIdAndPersonId(
        fileId: Long,
        personId: Long,
    ): Iterable<Face>

    /**
     * Галерея распознавания целиком, одним запросом.
     *
     * Раньше она собиралась обходом «каждая серия × каждая персона», то
     * есть на проекте из 10 серий и 70 персон это 700 отдельных запросов на
     * каждый запуск распознавания, и пересборка повторялась для каждой
     * серии подряд.
     *
     * Отбор здесь **буквально тот же**, что был в коде: персонаж типа
     * PERSON, не неопределённый, и у лица непустое имя в распознавателе.
     * Условия перенесены в SQL не для красоты: если они разойдутся с тем,
     * что было в Kotlin, состав галереи поменяется молча, а распознавание
     * станет хуже без единой ошибки.
     *
     * Отбор идёт по файлам и персонам, а не по лицу напрямую.
     *
     * Результат — **проекция из двух столбцов**, `person_id` и `vector`, а не
     * сущности Face. Раньше здесь стояло `SELECT f.*`, и Hibernate
     * материализовал все 62 тысячи строк как объекты Face — с координатами,
     * вероятностью, флагами, серией, кадром и треком, — ради двух значений,
     * которые реально нужны: person_id (чтобы взять имя персоны из уже
     * собранной карты) и vector (чтобы положить строку в матрицу). Плюс
     * каждый объект Face тянул ленивую ссылку на персону.
     *
     * Проверено на живой базе: число строк проекции совпадает с числом строк
     * `f.*` до единой (61 941 на 2026-10-08), пустых векторов среди них ноль.
     *
     * Порядок по person_id из базы не гарантирован — сортировка по персоне
     * делается на стороне вызова (см. RecognizeFaces).
     *
     * Второй столбец — двоичный вектор (задача #268). Текстовой колонки
     * больше нет: она занимала 1 116 МБ и была причиной самого дорогого
     * разбора в проекте.
     *
     * **Почему запасного пути здесь не осталось.** Пока текстовая колонка
     * существовала, её надо было где-то указывать — и она попадала в `SELECT`
     * целиком, даже когда двоичный вектор есть. Замерено на живой базе на
     * 74 739 строках: оба столбца — 2 991 мс, только двоичный — 1 123 мс.
     * Запасной путь в списке столбцов стоит дороже самой оптимизации.
     */
    @Query(
        value =
            "SELECT f.person_id, f.vector_bin FROM tbl_faces f " +
                "INNER JOIN tbl_persons p ON f.person_id = p.id " +
                "INNER JOIN tbl_files fl ON f.file_id = fl.id " +
                "LEFT JOIN (SELECT parent_id, " +
                "MIN(CASE WHEN property_key = ?4 THEN property_value END) AS first_id, " +
                "MIN(CASE WHEN property_key = ?5 THEN property_value END) AS last_id " +
                "FROM tbl_properties WHERE parent_class = ?6 AND property_key IN (?4, ?5) " +
                "GROUP BY parent_id) b ON b.parent_id = p.id " +
                "LEFT JOIN tbl_files flf ON flf.id = " +
                "(CASE WHEN b.first_id ~ '^[0-9]+$' THEN CAST(b.first_id AS bigint) END) " +
                "LEFT JOIN tbl_files fll ON fll.id = " +
                "(CASE WHEN b.last_id ~ '^[0-9]+$' THEN CAST(b.last_id AS bigint) END) " +
                "WHERE fl.project_id = ?1 AND p.person_type = ?2 AND f.person_id <> ?3 " +
                "AND f.person_recognized_name <> '' " +
                "AND (b.first_id IS NULL OR fl.order_file >= flf.order_file) " +
                "AND (b.last_id IS NULL OR fl.order_file <= fll.order_file)",
        nativeQuery = true,
    )
    fun findGalleryByProjectId(
        projectId: Long,
        personType: Int,
        undefindedPersonId: Long,
        firstEpisodeKey: String,
        lastEpisodeKey: String,
        personClass: String,
    ): Iterable<Array<Any>>

    @Query(
        value = "SELECT * FROM tbl_faces INNER JOIN tbl_files ON tbl_faces.file_id = tbl_files.id WHERE tbl_files.project_id = ?1 AND tbl_faces.is_example = true",
        nativeQuery = true,
    )
    fun getListFacesToTrain(projectId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND frame_number = ?2", nativeQuery = true)
    fun getListFacesInFrame(
        fileId: Long,
        frameNumber: Int,
    ): Iterable<Face>

    @Query(
        value =
            "select tbl_faces.* from tbl_faces inner join tbl_files as tf on tbl_faces.file_id = tf.id " +
                "where tf.project_id = ?1 and tbl_faces.person_id = ?2 and " +
                "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and  " +
                "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6)",
        nativeQuery = true,
    )
    fun findByProjectIdAndPersonId(
        projectId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean,
    ): Iterable<Face>

    @Query(
        value =
            "select tbl_faces.* from tbl_faces " +
                "where tbl_faces.file_id = ?1 and tbl_faces.person_id = ?2 and " +
                "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and " +
                "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6)",
        nativeQuery = true,
    )
    fun findByFileIdAndPersonId(
        fileId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean,
    ): Iterable<Face>

    @Query(
        value =
            "select tbl_faces.* from tbl_faces inner join tbl_files as tf on tbl_faces.file_id = tf.id " +
                "inner join tbl_shots ts on tf.id = ts.file_id " +
                "where ts.id = ?1 and tbl_faces.person_id = ?2 and " +
                "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and " +
                "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6) and " +
                "(tbl_faces.frame_number >= ts.first_frame_number and " +
                "tbl_faces.frame_number <= ts.last_frame_number)",
        nativeQuery = true,
    )
    fun findByShotIdAndPersonId(
        shotId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean,
    ): Iterable<Face>
}
