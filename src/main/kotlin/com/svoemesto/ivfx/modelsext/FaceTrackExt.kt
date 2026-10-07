package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.FaceTrack
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import java.io.File as IOFile
import javax.imageio.ImageIO

/**
 * Трек для показа в списке вкладки Tracks.
 *
 * Вкладка устроена как Persons, только вместо персоны — трек: слева узкий
 * столбец, справа картинки лиц выбранного трека. Трек — это один и тот же
 * человек в пределах одного монтажного куска, и смысл его в том, чтобы
 * назвать человека один раз на весь трек, а не по кадрам.
 *
 * Ячейка списка устроена в точности как у персон: картинка во всю ячейку и
 * имя подчёркнутым текстом поверх картинки. Так делает PersonExt через
 * OverlayImage.setOverlayUnderlineText, и здесь так же — иначе строка рядом
 * со строками персон выглядит иначе.
 *
 * Всё, что нужно для показа, считается заранее: форма работает в потоке
 * интерфейса, где сессии Hibernate нет, и обращение к отложенной связи там
 * падает. Поэтому имя и портрет готовятся до показа, а в объекте лежат
 * сущности лиц без чтения их полей.
 */
class FaceTrackExt(val tracks: List<FaceTrack>,
                   val faces: List<Face>,
                   val personName: String,
                   val mixed: Boolean,
                   private val personPreviewPath: String?,
                   /**
                    * Персона, которой принадлежит строка. Нужен, чтобы на
                    * строку можно было перетащить лицо: без него строка знает
                    * только имя подписи, а не того, кому назначать.
                    */
                   val personId: Long = 0L) {

    /** Первый трек группы — на него ссылаются действия одиночного трека. */
    val track: FaceTrack get() = tracks.first()

    companion object {
        /** Подпись для пустого списка — чтобы не путать с «треков нет». */
        const val EMPTY_TEXT = "У этого монтажного куска нет треков. Выполните шаг [TR] Track faces."

        /** Трек без персона — его надо назвать, поэтому он первый в списке. */
        const val UNNAMED_TEXT = "— не определён —"

        /** Смешанный трек нельзя назвать одним именем, поэтому подпись другая. */
        const val MIXED_NAME = "разные люди"
    }

    /** Лица трека для сетки справа. Собирается контроллером: там уже есть
     *  загруженный файл проекта, из которого безопасно строить превью. */
    var facesExtForGrid: MutableList<FaceExt> = mutableListOf()

    /** Те же лица, но разложенные по трекам, по порядку кадров внутри
     *  трека. По этому списку строится сетка: каждый трек начинается с новой
     *  строки, а между треками рисуется черта. */
    var facesExtByTrack: MutableList<MutableList<FaceExt>> = mutableListOf()

    /** Границы группы: от первого кадра первого трека до последнего последнего. */
    val framesText: String
        get() {
            if (tracks.size == 1) return "${track.firstFrameNumber}–${track.lastFrameNumber}"
            return "${tracks.minOf { it.firstFrameNumber }}–${tracks.maxOf { it.lastFrameNumber }}"
        }

    /** Сколько треков схлопнуто в строку — видно, что лиц много, а человек один. */
    val tracksCountText: String get() = if (tracks.size > 1) tracks.size.toString() else ""

    val faceNumberText: String get() = faces.size.toString()

    /** Подпись для рисунка на картинке и для строки состояния под таблицей. */
    val nameText: String
        get() = when {
            mixed -> MIXED_NAME
            personName.isEmpty() -> UNNAMED_TEXT
            else -> personName
        }

    /** Сортировка списка: работа — сверху, смешанные — в самый низ. */
    var isNamed: Boolean = false

    private var _previewSmall: ImageView? = null

    /** Портрет: картинка лица, растянутая до размера ячейки, с именем. */
    val previewSmall: ImageView
        get() {
            if (_previewSmall == null) {
                _previewSmall = ImageView(ConvertToFxImage.convertToFxImage(portraitImage()))
            }
            return _previewSmall!!
        }

    private var _labelSmall: Label? = null

    /** Ячейка списка — как у персон: только картинка, без текста. */
    val labelSmall: Label
        get() {
            if (_labelSmall == null) {
                _labelSmall = Label()
                _labelSmall!!.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                _labelSmall!!.graphic = previewSmall
                _labelSmall!!.alignment = Pos.CENTER
            }
            return _labelSmall!!
        }

    /**
     * Картинка ячейки: превью лица трека, растянутое до размера персон, с
     * именем подчёркнутым текстом. Если превью нет, рисуется пустая карточка:
     * иначе строка выглядела бы совсем иначе, чем соседние строки персон.
     */
    /**
     * Картинка ячейки — та же, что у персоны в Persons.
     *
     * Берётся картинка самой персоны, а не лицо из трека: трек может быть и
     * безымянным, и смешанным, и лицо из него — это конкретный кадр, тогда
     * как у персон в списке всегда одна и та же картинка. Чтобы строка
     * выглядела в точности как в Persons, используется ровно то, что и там:
     * картинка персоны и подчёркнутое имя поверх неё. Размер совпадает —
     * 135 на 75, — так что масштабировать ничего не нужно.
     *
     * Если персоны нет (трек неназванный или смешанный), берётся та же пустая
     * карточка, которой пользуется PersonExt, чтобы строка не выбивалась из
     * ряда.
     */
    private fun portraitImage(): BufferedImage {
        var bi: BufferedImage? = null
        val file = personPreviewPath?.let { IOFile(it) }
        if (file != null && file.exists()) {
            try {
                bi = ImageIO.read(file)
            } catch (_: Exception) {
                bi = null
            }
        }
        if (bi == null) {
            try {
                val name = PersonExt::class.java.getResource("blank_person_small.jpg")!!.toURI().path
                bi = ImageIO.read(IOFile(name))
            } catch (_: Exception) {
                bi = BufferedImage(Main.PREVIEW_FRAME_W.toInt(), Main.PREVIEW_FRAME_H.toInt(),
                    BufferedImage.TYPE_INT_RGB)
            }
        }
        val image = bi!!
        val scaled = OverlayImage.extractRegion(image, 0, 0, image.width, image.height,
            Main.PREVIEW_FRAME_W.toInt(), Main.PREVIEW_FRAME_H.toInt())
        return OverlayImage.setOverlayUnderlineText(scaled, nameText)
    }
}
