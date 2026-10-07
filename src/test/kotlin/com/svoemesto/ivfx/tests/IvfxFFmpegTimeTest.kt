package com.svoemesto.ivfx.tests

import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getFrameNumberByDuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Арифметика «кадр ↔ время» из [IvfxFFmpegUtils].
 *
 * Функции чистые: ни базы, ни ffmpeg, ни JavaFX. Ровно поэтому с них начато
 * покрытие — их результат показывается оператору в интерфейсе, и ошибка
 * здесь выглядит как «план длится 00:09:59.999», а найти её нечем.
 *
 * Тесты закрепляют **фактическое** поведение, включая особенности, которые
 * выглядят странно. Особенности помечены отдельно: если какая-то из них
 * окажется ошибкой, тест должен сломаться явно, а не тихо следовать за
 * изменением.
 */
@DisplayName("Кадр ↔ время: арифметика IvfxFFmpegUtils")
class IvfxFFmpegTimeTest {
    @Nested
    @DisplayName("getDurationByFrameNumber — номер кадра в миллисекунды")
    inner class DurationByFrame {
        @Test
        @DisplayName("нулевой кадр — нулевая длительность")
        fun `нулевой кадр даёт ноль`() {
            assertEquals(0, getDurationByFrameNumber(0, 25.0))
        }

        @Test
        @DisplayName("при 25 fps один кадр длится 40 мс")
        fun `один кадр при 25 fps`() {
            assertEquals(40, getDurationByFrameNumber(1, 25.0))
        }

        @Test
        @DisplayName("при 25 fps кадр 25 — ровно секунда")
        fun `25 кадров это секунда при 25 fps`() {
            assertEquals(1000, getDurationByFrameNumber(25, 25.0))
        }

        @Test
        @DisplayName("при 24 fps кадр 24 — ровно секунда")
        fun `24 кадра это секунда при 24 fps`() {
            assertEquals(1000, getDurationByFrameNumber(24, 24.0))
        }

        @Test
        @DisplayName("отрицательный номер кадра не даёт отрицательное время")
        fun `отрицательный кадр зажимается в ноль`() {
            assertEquals(0, getDurationByFrameNumber(-1, 25.0))
            assertEquals(0, getDurationByFrameNumber(-100_000, 25.0))
        }

        @Test
        @DisplayName("время округляется вниз, а не к ближайшему")
        fun `время обрезается вниз`() {
            // 1000 / 23.976 = 41.708…; 24 кадра — 1001.00 мс.
            // Округление к ближайшему дало бы 1001, обрезание — тоже 1001.
            // Показательный случай ниже: 25 кадров при 24 fps.
            assertEquals(1041, getDurationByFrameNumber(25, 24.0))
            // 25 * 41.6666… = 1041.66… — вниз до 1041.
            assertTrue(
                getDurationByFrameNumber(25, 24.0) < 25 * (1000 / 24.0),
                "время должно быть обрезано вниз, а не округлено",
            )
        }

        @Test
        @DisplayName("25 кадров при 30 fps — 833 мс, а не 834")
        fun `обрезка при 30 fps`() {
            // 25 * (1000 / 30) = 833.33… → 833
            assertEquals(833, getDurationByFrameNumber(25, 30.0))
        }
    }

    @Nested
    @DisplayName("getFrameNumberByDuration — миллисекунды в номер кадра")
    inner class FrameByDuration {
        @Test
        @DisplayName("нулевая длительность даёт кадр 1, а не 0")
        fun `нулевая длительность даёт единицу`() {
            // Особенность: в формуле стоит «+ 1». Нулевая длительность
            // поэтому не попадает в нулевой кадр — если убрать единицу,
            // изменится смысл каждого кадра в интерфейсе.
            assertEquals(1, getFrameNumberByDuration(0, 25.0))
        }

        @Test
        @DisplayName("при 25 fps секунда — это кадр 26")
        fun `секунда при 25 fps`() {
            assertEquals(26, getFrameNumberByDuration(1000, 25.0))
        }

        @Test
        @DisplayName("при 24 fps секунда — это кадр 25")
        fun `секунда при 24 fps`() {
            assertEquals(25, getFrameNumberByDuration(1000, 24.0))
        }

        @Test
        @DisplayName("две функции не обратны друг другу: сдвиг на кадр")
        fun `функции не обратны`() {
            // Прямое и обратное преобразование расходятся ровно на один кадр:
            // 25 кадров при 25 fps — это 1000 мс, но 1000 мс — это кадр 26.
            // Закреплено намеренно: «починить» одно, не тронув второе, нельзя.
            val frame = 25
            val fps = 25.0
            val millis = getDurationByFrameNumber(frame, fps)
            assertEquals(1000, millis)
            assertEquals(frame + 1, getFrameNumberByDuration(millis, fps))
        }

        @Test
        @DisplayName("округление ровно в половину идёт вверх")
        fun `половина округляется вверх`() {
            // 1000 / 25 = 40 мс на кадр. 20 мс — ровно половина кадра:
            // 20 / 40 + 1 = 1.5 → 2, а не 1.
            assertEquals(2, getFrameNumberByDuration(20, 25.0))
        }
    }

    @Nested
    @DisplayName("convertDurationToString — миллисекунды в текст")
    inner class DurationToString {
        @Test
        @DisplayName("обычные случаи разбираются на часы, минуты, секунды, миллисекунды")
        fun `разбор длительности`() {
            assertEquals("0:00:00.000", convertDurationToString(0))
            assertEquals("0:00:00.999", convertDurationToString(999))
            assertEquals("0:00:01.000", convertDurationToString(1000))
            assertEquals("0:01:00.000", convertDurationToString(60_000))
            assertEquals("1:01:01.500", convertDurationToString(3_661_500))
        }

        @Test
        @DisplayName("часы не дополняются нулём, минуты и секунды — дополняются")
        fun `часы без ведущих нулей`() {
            // 10 часов печатаются как «10:00:00.000», а не «010:…».
            // Минуты и секунды всегда двузначные.
            assertEquals("10:00:00.000", convertDurationToString(36_000_000))
        }

        @Test
        @DisplayName("миллисекунды всегда три цифры")
        fun `миллисекунды трёхзначные`() {
            assertEquals("0:00:00.005", convertDurationToString(5))
            assertEquals("0:00:00.050", convertDurationToString(50))
        }

        @Test
        @DisplayName("особенность: отрицательная длительность печатается со знаком в секундах")
        fun `отрицательная длительность даёт странный текст`() {
            // Целочисленное деление в Kotlin обрезает к нулю, поэтому минуты
            // и часы остаются нулём, а секунды уходят в минус.
            // Вызывающий код отрицательную длительность не передаёт, но
            // результат закреплён: изменение разбора должно быть заметным.
            assertEquals("0:00:-1.000", convertDurationToString(-1000))
        }
    }
}
