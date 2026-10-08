-- Связь лица с кадром: колонка tbl_faces.frame_id.
--
-- Зачем. До 2026-10-08 лицо знало только серию и номер кадра, то есть
-- принадлежность кадру была ВЫВОДИМОЙ, а не записанной. Авария того дня
-- показала, чем это опасно: перезапуск создания лиц переписал 45 136 лиц
-- шести серий в седьмую (face.file = fileExt.file для каждой записи).
-- Номера кадров при этом остались прежними, но указания на то, какому
-- файлу кадр принадлежит, в базе не осталось — у tbl_frames серия есть,
-- а у tbl_faces её записывали отдельно и затёрли. Восстановление держалось
-- на одном-единственном файле на диске.
--
-- Что даёт колонка. Лицо получает собственную ссылку на кадр, которая не
-- зависит от file_id. Если серия у лица снова окажется затёрта, кадр
-- останется: найти по нему исходную серию можно всегда. Плюс исчезает
-- JOIN с tbl_frames в выборках по кадру.
--
-- Применение:
--   psql -d ivfx -f tools/add-face-frame-link.sql
--
-- Идемпотентно: повторный запуск ничего не меняет.

BEGIN;

ALTER TABLE tbl_faces ADD COLUMN IF NOT EXISTS frame_id bigint;

-- Заполнение по имеющимся данным: кадр ищется по серии и номеру.
-- Проверено на данных 2026-10-08: 80 508 лиц из 80 508 нашли свой кадр,
-- незаполненных не осталось.
UPDATE tbl_faces f
   SET frame_id = fr.id
  FROM tbl_frames fr
 WHERE f.frame_id IS NULL
   AND fr.file_id = f.file_id
   AND fr.frame_number = f.frame_number;

-- Контроль: остаться не должно ничего.
DO $$
DECLARE
    missing bigint;
BEGIN
    SELECT count(*) INTO missing FROM tbl_faces WHERE frame_id IS NULL;
    IF missing > 0 THEN
        RAISE EXCEPTION 'лиц без кадра: %', missing;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_faces_frame ON tbl_faces (frame_id);

ALTER TABLE tbl_faces
    ADD CONSTRAINT fk_faces_frame FOREIGN KEY (frame_id) REFERENCES tbl_frames (id);

COMMIT;
