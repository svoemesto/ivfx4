-- Индексы, без которых проект работает медленно.
--
-- Зачем. Схему создал Hibernate по моделям и по связи @ManyToOne. Hibernate
-- внешние ключи объявляет, но индексы под них НЕ создаёт — в этом и была
-- причина. Проверено 2026-10-07: во всех 19 таблицах был только первичный
-- ключ, ни одного вторичного индекса.
--
-- Пока JPA в проекте не работал (задачи #235, #236), производные запросы
-- Spring Data просто не выполнялись, и отсутствие индексов не было видно.
-- С включением JPA каждый findBy... пошёл в базу, и каждая выборка стала
-- полным сканированием. Наблюдалось на операции CreateFaces: 8740 запросов
-- поиска лица по ключу, каждый — скан 62905 строк. После создания индексов
-- тот же запрос занимает 0,175 мс, а вся операция проходит минуты.
--
-- Набор индексов выведен из реально используемых шаблонов запросов
-- репозиториев, а не назначен по умолчанию: по (file_id, frame_number)
-- там идёт findByFileId и findByFileIdAndFrameNumber..., по
-- (parent_class, parent_id) — выборка свойств, и так далее.
--
-- Применение:
--   psql -d ivfx -f tools/add-indexes.sql
--
-- Идемпотентно: IF NOT EXISTS, повторный запуск ничего не меняет.
--
-- ВНИМАНИЕ: здесь обычный CREATE INDEX, он кратковременно берёт блокировку
-- на запись. Под нагрузкой применять только через CREATE INDEX CONCURRENTLY,
-- но не в транзакции и не на пустой таблице — она выдаст предупреждение.

BEGIN;

-- Кадры: findByFileId, findByFileIdAndFrameNumber...
CREATE INDEX IF NOT EXISTS idx_frames_file_frame
    ON tbl_frames (file_id, frame_number);

-- Лица: поиск по ключу (файл, кадр, номер лица в кадру) — самый горячий
-- запрос проекта; плюс выборка по треку и по персоне.
CREATE INDEX IF NOT EXISTS idx_faces_file_frame_no
    ON tbl_faces (file_id, frame_number, face_number_in_frame);
CREATE INDEX IF NOT EXISTS idx_faces_track   ON tbl_faces (track_id);
CREATE INDEX IF NOT EXISTS idx_faces_person  ON tbl_faces (person_id);

-- Треки лиц: findByShotId, findByShotIdAndPersonId.
CREATE INDEX IF NOT EXISTS idx_facestracks_shot   ON tbl_faces_tracks (shot_id);
CREATE INDEX IF NOT EXISTS idx_facestracks_person ON tbl_faces_tracks (person_id);

-- Планы, сцены, события: findByFileId и
-- findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber
CREATE INDEX IF NOT EXISTS idx_shots_file_first  ON tbl_shots  (file_id, first_frame_number);
CREATE INDEX IF NOT EXISTS idx_scenes_file_first ON tbl_scenes (file_id, first_frame_number);
CREATE INDEX IF NOT EXISTS idx_events_file_first ON tbl_events (file_id, first_frame_number);

-- Свойства лежат в EAV-таблице без внешнего ключа, поэтому индекс по
-- (класс, id) — единственный способ не сканировать их целиком.
CREATE INDEX IF NOT EXISTS idx_props_class_parent ON tbl_properties (parent_class, parent_id);
CREATE INDEX IF NOT EXISTS idx_propscdf_class     ON tbl_properties_cdf (parent_class, parent_id, computer_id);

-- Машинозависимые привязки.
CREATE INDEX IF NOT EXISTS idx_filescdf_file ON tbl_files_cdf   (file_id, computer_id);
CREATE INDEX IF NOT EXISTS idx_projcdf_proj  ON tbl_projects_cdf (project_id, computer_id);
CREATE INDEX IF NOT EXISTS idx_shotstmp_cdf_comp  ON tbl_shots_tmp_cdf  (computer_id);
CREATE INDEX IF NOT EXISTS idx_shotstmp2_cdf_comp ON tbl_shots_tmp2_cdf (computer_id);

-- Фильтры: порядок выдаётся по (владелец, порядок).
CREATE INDEX IF NOT EXISTS idx_files_proj      ON tbl_files             (project_id, order_file);
CREATE INDEX IF NOT EXISTS idx_filestracks_file ON tbl_files_tracks     (file_id, order_file_track);
CREATE INDEX IF NOT EXISTS idx_filters_proj     ON tbl_filters          (project_id, order_filter);
CREATE INDEX IF NOT EXISTS idx_fgroups_filter   ON tbl_filters_groups   (filter_id, order_filter_group);
CREATE INDEX IF NOT EXISTS idx_fconds_group     ON tbl_filters_conditions (filter_group_id, order_filter_condition);

-- Персоны: findByProjectId и поиск по имени в распознавателе.
CREATE INDEX IF NOT EXISTS idx_persons_proj ON tbl_persons (project_id);

COMMIT;