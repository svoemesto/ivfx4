---
id: sys-containers
title: "Система: контейнеры (L2)"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - c4
  - system
---

# Система: контейнеры (L2)

## Технические блоки

| Блок | Что это | Где в коде |
|---|---|---|
| **Ядро и Spring-контекст** | сущности, репозитории, бизнес-логика | `models/`, `repos/`, `controllers/` |
| **Интерфейс** | 11 окон JavaFX | `fxcontrollers/`, `resources/.../fxcontrollers/*.fxml` |
| **Пайплайн** | 16 долгих операций | `threads/projectactions/`, `threads/RunListThreads.kt` |
| **Внешние программы** | ffmpeg, ffprobe, mediainfo, Python | `utils/`, `resources/.../FaceDetector/` |
| **База** | JPA + H2 (служебная) или Postgres (рабочая) | `SpringConfig.kt`, `H2db.kt` |

Внешних программ в репозитории нет: `ffmpeg`, `ffprobe` и `mediainfo`
берутся из системы (ADR в `AGENTS.md`, Tier-1 «Внешние утилиты берутся из
системы»). Модели лиц не хранятся в репозитории и кешируются
(ADR-0003).

## Хранилище на диске

Иерархия внутри папки проекта, по папке на файл:

```
<project.folder>/
  Lossless/<short>/<short>_lossless.mkv        мастер-копия
  Preview/<short>/<short>_preview.mp4          превью
  Frames_Small|Medium|Full/<short>/…            кадры трёх размеров
  Faces_Full/<short>/…                          вырезанные лица
  Faces_Preview/<short>/…                       превью лиц
  Shots/<short>/…                               планы (сжатые)
  Shots_LL_audioYES|NO/<short>/…                планы (lossless, со звуком и без)
  Concat/<short>_concat.<ext>                   склейка планов файла
  Filters/<filter> [<f1>-<f2>].<ext>           склейка выборки фильтра
  Persons/ , recognizer.pickle , le.pickle
```

Пути переопределяются per-machine через схему `*Cdf` (ADR-0002).

## База данных

Два независимых соединения:

1. **Служебная** — `H2db.kt`, список баз данных (`tbl_databases`), выбирает
   рабочую базу. Файл `h2db.mv.db` в корне проекта.
2. **Рабочая** — по выбору пользователя: H2, Postgres или MySQL.

Схема создаётся из сущностей: `spring.jpa.hibernate.ddl-auto=update`.
Миграций нет.

## Внешние процессы

| Процесс | Как запускается | Замечание |
|---|---|---|
| `ffmpeg`, `ffprobe` | через библиотеку bramp | системные бинарники |
| `mediainfo` | `ProcessBuilder` | системный бинарник |
| Python-скрипты | `ProcessBuilder` | окружение `~/.local/ivfx-venv-opencv4` |

Возвращаемые коды внешних процессов **игнорируются** — это долг.

## Связанные документы

- [Контекст (L1)](01-context.md)
- [Пайплайн](../domains/pipeline/domain.md)
