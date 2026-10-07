package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.FaceTrack
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.models.Shot
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import java.io.File as IOFile

/**
 * Шаг отслеживания: собирает лица одного человека внутри сцены в треки.
 *
 * Зачем. DF просматривает не все кадры, а лишь часть: между соседними
 * проверенными кадрами проходит до 0,83 секунды, и человек за это время может
 * уйти из кадра и вернуться. Поэтому по одним и тем же лицам судить о
 * присутствии человека нельзя — а по треку можно: если он был в начале сцены
 * и в конце, значит был, скорее всего, и между ними, даже если ни одного
 * проверенного кадра там нет.
 *
 * Как связываем. По вектору лица, а не по месту, размеру и ракурсу. Замерено
 * на полностью размеченной серии: у пар «одинаковый персонаж» медиана
 * сходства 0,771, у пар «разные персонажи» — 0,036. Место, размер и ракурс
 * различаются тоже (медиана расстояния между центрами 0,037 против 0,254 ширины
 * кадра), но слабее: два похожих человека в разных местах дадут высокое
 * сходство векторов, и без проверки места такой случай не отличить от одного
 * человека. Пока место используется как подсказка, а не как условие.
 *
 * Связывание не переходит через границу сцены: это разные куски, и человек в
 * них может быть другим.
 *
 * Результат. На этой серии 8218 лиц-персонажей сворачиваются примерно в 1665
 * треков — решение о принадлежности к человеку принимается в 4,9 раза реже.
 */
class TrackFaces(var fileExt: FileExt,
                 val textLbl1: String,
                 val numCurrentThread: Int,
                 val countThreads: Int,
                 var lbl1: Label, var pb1: ProgressBar,
                 var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    companion object {
        /**
         * Порог сходства, при котором два лица считаются одним человеком.
         *
         * Замерено на размеченной серии: при 0,5 точность связывания 99,86%,
         * полнота 83,8%. Точность важнее: ошибка слияния не остаётся одной
         * ошибкой, а расходится именем по всем лицам трека — тот же принцип,
         * что и в галерее, где неверное имя тоже тиражируется.
         */
        const val TRACK_THRESHOLD = 0.5

        /** Сколько знаков после запятой достаточно для сравнения. Вектор
         *  хранится текстом, полная точность здесь не нужна: соседние
         *  значения всё равно сливаются порогом. */
        private const val VECTOR_PRECISION = 5
    }

    override fun run() {

        Platform.runLater {
            lbl1.isVisible = true
            pb1.isVisible = true
            lbl2.isVisible = true
            pb2.isVisible = true
            lbl1.text = textLbl1
            lbl2.text = "Linking faces ..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val shots = Main.shotRepo.findByFileId(fileExt.file.id).toMutableList()
        // Сортируем по номеру кадра, а не штатным compareTo: те сравнивают по
        // file.order, а file у сцены и лиц подгружается отложенно. В этом
        // потоке сессии Hibernate нет, и обращение к отложенному полю падает
        // с LazyInitializationException. Заодно порядок по номеру кадра
        // нужен нам самим: бисекция по сценам требует отсортированных начал,
        // а в базе сцены идут в произвольном порядке.
        shots.sortBy { it.firstFrameNumber }

        val faces = Main.faceRepo.findByFileId(fileExt.file.id).toMutableList()
        faces.sortBy { it.frameNumber }

        // Персоны подгружаем целиком заранее: тип персоны тоже лежит за
        // отложенной связью, и читать его прямо здесь так же нельзя.
        val personById = Main.personRepo.findAll().associateBy { it.id }

        Platform.runLater {
            lbl1.text = "$textLbl1: сцен ${shots.size}, лиц ${faces.size}"
            pb1.progress = if (countThreads <= 1) 0.0 else (numCurrentThread - 1) / countThreads.toDouble()
        }

        // Лица без вектора в галерею и в трек не годятся: не с чем сравнивать.
        val withVector = faces.filter { it.vector.isNotEmpty() }

        Platform.runLater { lbl2.text = "0/${withVector.size}" }

        // Привязка лица к сцене по номеру кадра. Сцены в базе не обязаны идти
        // по порядку, поэтому ищем бисекцией по отсортированным началам.
        val shotStarts = shots.map { it.firstFrameNumber }.toIntArray()
        val shotOf = IntArray(withVector.size) { -1 }
        for (i in withVector.indices) {
            val frame = withVector[i].frameNumber
            var k = java.util.Arrays.binarySearch(shotStarts, frame)
            if (k < 0) k = -(k + 1) - 1
            if (k >= 0 && frame <= shots[k].lastFrameNumber) shotOf[i] = k
        }

        val groups = mutableMapOf<Int, MutableList<Face>>()
        for (i in withVector.indices) {
            if (shotOf[i] < 0) continue
            groups.getOrPut(shotOf[i]) { mutableListOf() }.add(withVector[i])
        }

        // Старые треки файла снимаем целиком: пересчёт всегда делается с нуля,
        // иначе после изменения шага остались бы треки от прежних правил.
        Main.faceTrackRepo.unlinkTracks(fileExt.file.id)
        Main.faceTrackRepo.deleteAll(fileExt.file.id)

        var processed = 0
        var created = 0

        // Счётчик двигается на каждое лицо, а не на каждый трек и не пачками:
        // по 500 счётчик выглядел как застывший и лишь потом прыгал сразу на
        // сотни. Обновление подписи на каждое лицо дешево — это одна короткая
        // надпись, — а по ходу работы видно настоящий ход дела.
        val totalFaces = maxOf(withVector.size, 1)
        fun advance() {
            processed++
            val done = processed.toDouble() / totalFaces
            // Подпись и полосу обновляем не на каждое лицо: это около девяти
            // тысяч задач в очередь потока интерфейса, и окно подтормаживало.
            // Хватает раза на сотню — глазом не отличить.
            if (processed % 100 == 0) {
                val text = "$processed/$totalFaces (${(done * 100).toInt()}%)"
                Platform.runLater { pb2.progress = done; lbl2.text = text }
            }
        }
        var linkedFaces = 0
        // Лица, забранные треком у «не определён» — их покажем итогом.
        var adopted = 0

        for ((shotIndex, group) in groups) {

            val shot: Shot = shots[shotIndex]
            val vectors = Array(group.size) { group[it].vector }
            val parent = IntArray(group.size) { it }

            fun find(a: Int): Int {
                var x = a
                while (parent[x] != x) {
                    parent[x] = parent[parent[x]]
                    x = parent[x]
                }
                return x
            }
            fun union(a: Int, b: Int) {
                val ra = find(a); val rb = find(b)
                if (ra != rb) parent[ra] = rb
            }

            for (a in group.indices) {
                val va = vectors[a]
                if (va == null) continue
                for (b in a + 1 until group.size) {
                    val vb = vectors[b]
                    if (vb == null) continue
                    if (cosine(va, vb) >= TRACK_THRESHOLD) union(a, b)
                }
            }

            val byRoot = mutableMapOf<Int, MutableList<Face>>()
            for (i in group.indices) byRoot.getOrPut(find(i)) { mutableListOf() }.add(group[i])

            for (members in byRoot.values) {

                val track = FaceTrack()
                track.shot = shot
                track.firstFrameNumber = members.minOf { it.frameNumber }
                track.lastFrameNumber = members.maxOf { it.frameNumber }
                track.faceCount = members.size
                track.person = majorityPerson(members, personById)

                val saved = Main.faceTrackRepo.save(track)
                created++
                if (members.size > 1) linkedFaces += members.size

                // Связь пишем одним UPDATE: при сохранении трека Hibernate
                // переписывал изменённые лица целиком вместе с вектором, а
                // вектор — десять килобайт на лицо. Лиц девять тысяч, и на
                // каждом треке вся эта масса пересматривалась заново.
                Main.faceRepo.linkFacesToTrack(saved.id, members.map { it.id })
                members.forEach { advance() }

                // Неопределённые лица забираются персонажем трека — но
                // ТОЛЬКО после того, как лицу присвоен новый трек. Раньше
                // лицо сохранялось до этого, и в нём ещё стоял старый трек,
                // удалённый в начале шага: сохранение писало в базу ссылку
                // на несуществующую строку, и шаг падал на внешнем ключе.
                //
                // Если в треке узнан ровно ОДИН человек, неопределённые
                // лица — его же: трек собран по сходству векторов, значит
                // это тот же человек в кадре, где распознавание не
                // сработало. Трек перестаёт быть смешанным — в списке
                // треков вместо «разные люди» появляется этот человек.
                //
                // Если узнанных персонажей двое или больше, ничего не
                // делаем: трек действительно собрал двоих, и кому отдать
                // неопределённых, без участия человека не решить.
                val knownIds = members.mapNotNull { f -> personById[f.person.id] }
                        .filter { it.personType != PersonType.UNDEFINDED }
                        .map { it.id }.distinct()
                if (knownIds.size == 1) {
                    val known = personById[knownIds[0]]!!
                    for (f in members) {
                        val fp = personById[f.person.id] ?: continue
                        if (fp.personType != PersonType.UNDEFINDED) continue
                        Main.faceRepo.assignPersonToFace(known.id,
                                if (known.personType == PersonType.UNDEFINDED) ""
                                else known.nameInRecognizer, f.id)
                        adopted++
                    }
                }
            }

        // Поштучного сохранения лиц здесь больше нет: привязка и назначение
        // персоны пишутся пакетными UPDATE прямо в базу. Раньше здесь стояло
        // saveAll всех лиц серии — и оно выполнялось ОДИН РАЗ НА КАЖДЫЙ ПЛАН,
        // то есть около девятисот раз по девять тысяч записей. Это и была
        // причина скорости два лица в секунду.

        // Кадры, которые стоит перепроверить: пропущенные при выборке, но
        // лежащие внутри окна, где человек точно есть. Проверенные кадры
        // берём той же функцией, что и детектор, — правило шага тогда одно
        // на оба шага и не может разойтись.
        try {
            val candidates = collectRecheckCandidates(shots, withVector, shotOf, personById)
            val pathToCandidates = IOFile(fileExt.folderFramesFull, "recheck_frames.txt")
            pathToCandidates.writeText(candidates.joinToString("\n"))

            val textDone = "$processed/${withVector.size} → треков $created, лиц в них $linkedFaces, забранных у неопределённых $adopted, " +
                    "кадров на перепроверку ${candidates.size}"
            Platform.runLater {
                pb2.progress = 1.0
                lbl2.text = "Done"
                lbl1.text = textDone
            }
            println("[TR] $textDone")
        } catch (ex: Throwable) {
            // Треки к этому моменту уже созданы и сохранены — их не теряем.
            // Но подпись всё равно приводим в порядок: иначе форма навсегда
            // остаётся с прогрессом «12500/12548», и он читается как
            // «шаг ещё работает», хотя работать уже нечему.
            val textFail = "Треки созданы ($created), но список кадров не построен: ${ex.javaClass.simpleName}"
            Platform.runLater {
                pb2.progress = 1.0
                lbl2.text = "Failed"
                lbl1.text = textFail
            }
            println("[TR] $textFail")
            ex.printStackTrace()
        }
        }
    }

    private fun cosine(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size) return -1.0
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return -1.0
        return dot / (Math.sqrt(na) * Math.sqrt(nb))
    }

    /**
     * Кто в треке. Берём по большинству среди лиц: у человека одиннадцать
     * лиц в треке, и они уже размечены, тогда как лица, попавшие в него
     * по сходству с разных кадров, могли остаться неназванными.
     */
    private fun majorityPerson(members: List<Face>, personById: Map<Long, Person>): Person? {
        val counts = mutableMapOf<Long, Pair<Person, Int>>()
        for (f in members) {
            val p = personById[f.person.id] ?: continue
            if (p.personType == PersonType.UNDEFINDED) continue
            val id = p.id
            val cur = counts[id]
            counts[id] = if (cur == null) Pair(p, 1) else Pair(cur.first, cur.second + 1)
        }
        return counts.values.maxByOrNull { it.second }?.first
    }

    /**
     * Кадры для адресной перепроверки.
     *
     * Берём окно присутствия персонажа внутри сцены — от первого до последнего
     * его появления — и вычитаем из него кадры, которые детектор уже смотрел.
     * Остаток и есть кандидаты: там лица быть не могло, но человек в кадре,
     * по всей видимости, был, и DF его просто не заходил проверить.
     *
     * На этой серии так набирается около 99 тысяч кадров против 82 тысяч всех
     * пропущенных: треть всех непроверенных кадров приходится на сцены, где
     * человек есть, и при 43 мс на кадр это около 71 минуты вместо полного
     * просмотра.
     */
    private fun collectRecheckCandidates(shots: List<Shot>,
                                         faces: List<Face>,
                                         shotOf: IntArray,
                                         personById: Map<Long, Person>): List<Int> {
        val lastFrame = shots.maxOf { it.lastFrameNumber }
        val checked = FaceController.getFramesToRecognize(shots, lastFrame).toHashSet()

        val windowsByShot = mutableMapOf<Int, MutableList<Int>>()
        for (i in faces.indices) {
            val s = shotOf[i]
            if (s < 0) continue
            val f = faces[i]
            if (personById[f.person.id]?.personType == PersonType.UNDEFINDED) continue
            windowsByShot.getOrPut(s) { mutableListOf() }.add(f.frameNumber)
        }

        val result = mutableListOf<Int>()
        for ((shotIndex, frames) in windowsByShot) {
            val lo = frames.minOrNull() ?: continue
            val hi = frames.maxOrNull() ?: continue
            if (hi <= lo) continue
            // Верхняя граница окна — последний кадр сцены: человек мог ещё
            // оставаться в кадре после последнего своего найденного лица.
            val top = minOf(hi + stepOf(shots[shotIndex]), shots[shotIndex].lastFrameNumber)
            var f = lo + 1
            while (f <= top) {
                if (!checked.contains(f)) result.add(f)
                f++
            }
        }
        return result.distinct().sorted()
    }

    /** Шаг выборки кадров внутри сцены — тот же, что у детектора. */
    private fun stepOf(shot: Shot): Int {
        val d = shot.lastFrameNumber - shot.firstFrameNumber
        return if (d < 15) 3 else if (d < 30) 5 else if (d < 60) 10 else 20
    }
}