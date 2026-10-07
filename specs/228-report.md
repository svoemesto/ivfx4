# Отчёт: подключение проекта ivfx к OpenProject

## Что сделано

- Скопирован CLI трекера из Karaoke: `tools/tracker.sh`, `tools/tracker-lib.sh`.
  Происхождение зафиксировано контрольными суммами, файлы не правились:
  `tracker.sh` d1d0bcc5fb9d7e99…, `tracker-lib.sh` 0a57c013e4fc47e6…
- Создан `.env.local-tracker` (права 600) и шаблон `.env.local-tracker.example`.
  В реальный файл попали только ключи, нужные CLI; `TRACKER_DB_PASSWORD` и
  `TRACKER_SECRET_KEY_BASE` не переносились — они относятся к развёртыванию
  контейнера, а не к клиенту.
- `.env.local-tracker` добавлен в `.gitignore`.
- В `AGENTS.md` добавлено правило Tier-1 «Работа идёт через OpenProject»
  с обязательными четырьмя шагами.

## Что проверено и как

| Проверка | Результат |
|---|---|
| `./tools/tracker.sh healthcheck` | OK: OpenProject UP at http://localhost:8080 (27 ms) |
| `./tools/tracker.sh list-projects` | проект IVFX найден: id 5, identifier `ivfx` |
| `create-issue` | OK: work package #228 created |
| `claim-issue 228` | выполнено |

## Что осталось

- Задача #228 «Живые доки: карта доменов и ADR» — следующий шаг плана,
  описание в `specs/living-docs-description.md`.
- Шаги 3 и 4 плана: линтеры (ktlint с baseline) и тесты (первыми — чистая
  логика: свёртка фильтров, пагинация матрицы, арифметика кадр↔время).

## Замечания

- Экземпляр OpenProject общий с Karaoke (`http://localhost:8080`), ivfx4
  подключён к нему как отдельный проект, а не разворачивал свой.
