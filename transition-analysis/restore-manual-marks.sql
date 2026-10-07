-- Восстановление ручных отметок после повторного запуска Analyze Frames.
--
-- Analyze Frames при поиске переходов принудительно ставит is_manual_add и
-- is_manual_cancel в false, поэтому после перезапуска все ручные правки
-- теряются. Этот скрипт возвращает их на место по снимку, сделанному ДО
-- перезапуска. Рядом лежат: manual-add.txt (54), manual-cancel.txt (16),
-- boundaries-after-AT.txt (1027 границ), scores-snapshot.txt (оценки 88643 кадров).
--
-- Порядок запуска:
--   1. Analyze Frames [AF]
--   2. docker exec -i ivfx-db psql -U postgres -d ivfx -v ON_ERROR_STOP=1 \
--        -f - < /home/nsa/ivfx4/transition-analysis/restore-manual-marks.sql
--   3. Recalculate transitions [AT] - пересчитает автоматические границы по
--      новому правилу, ручные отметки оставит как есть
--   4. Create shots [CS]
--
-- Скрипт идемпотентен: повторный запуск даёт тот же результат.

BEGIN;

-- Снимаем ручные отметки со всех кадров, чтобы повторный запуск скрипта
-- не наследовал состояние прошлого.
UPDATE tbl_frames SET is_manual_add = false, is_manual_cancel = false
 WHERE is_manual_add OR is_manual_cancel;

-- Добавленные вручную переходы: такие кадры обязаны быть границами.
UPDATE tbl_frames f SET is_manual_add = true, is_final_find = true, is_find = true
 WHERE f.frame_number IN (2464,2698,4525,8435,8632,8900,8928,9397,11534,13611,13936,13990,17971,18134,19113,19205,19440,20256,29272,36465,42389,42536,44032,56274,56376,56569,56650,56745,58783,61295,73403,73474,73984,74168,74977,75578,75629,77105,77388,77773,78784,79670,79722,79961,80068,80193,80445,80612,80869,81101,81522,81744,81804,86111);

-- Снятые вручную переходы: такие кадры границами быть не должны.
UPDATE tbl_frames f SET is_manual_cancel = true, is_final_find = false, is_find = false
 WHERE f.frame_number IN (8430,9299,9346,9354,9480,9488,9495,9516,9538,9540,11975,12002,12694,12702,75118,75360);

COMMIT;

SELECT
  (SELECT count(*) FROM tbl_frames WHERE is_manual_add)    AS добавлено_вручную,
  (SELECT count(*) FROM tbl_frames WHERE is_manual_cancel) AS снято_вручную,
  (SELECT count(*) FROM tbl_frames WHERE is_final_find)    AS всего_границ;
