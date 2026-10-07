---
id: domain-media-file
title: "Домен: Видеофайл и дорожки"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - media
  - file
---

# Домен: Видеофайл и дорожки

## Обзор контекста (Bounded Context)

Видеофайл внутри проекта и его потоки. Из файла растут кадры, планы и лица —
всё остальное зависит от него.

## Dependencies | Зависимости

- `project` — владелец файла
- `frames` — кадры извлекаются из файла
- `pipeline` — операции обработки работают по файлу

## Ubiquitous Language | Единый язык

- **Файл**: видеофайл, принадлежащий проекту
- **Дорожка**: поток внутри файла (видео, аудио, субтитры) по данным MediaInfo
- **Мастер-копия**: lossless-версия файла, источник всех нарезок
- **Превью**: сжатая копия с пониженным разрешением для быстрого просмотра

## Публичные контракты (API)

### Internal API (для других модулей системы)

`controllers/FileController` — добавление, порядок, потеря данных;
`controllers/TrackController` — чтение дорожек;
`controllers/FileExt` (в `modelsext/`) — вычисляемые свойства (папки,
наличие артефактов, пути).

## Domain Invariants | Инварианты и правила бизнеса

- Все нарезки выполняются **из мастер-копии**, а не из исходного файла:
  это даёт единое качество и скорость.
- `path` файла — не колонка, а проекция на `FileCdf` по `computerId`.
- Порядок файлов в проекте задаётся оператором и сохраняется.

## Структура компонентов (C4 L3)

- `models/File.kt`, `models/FileCdf.kt`, `models/Track.kt`
- `controllers/FileController.kt`, `TrackController.kt`
- `modelsext/FileExt.kt`
