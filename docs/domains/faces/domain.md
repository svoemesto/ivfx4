---
id: domain-faces
title: "Домен: Лица и персоны"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - ml
  - faces
---

# Домен: Лица и персоны

## Обзор контекста (Bounded Context)

Детекция лиц, их распознавание и привязка к персонам. Лицо — рамка на кадре
вместе с 128-мерным вектором признаков.

## Dependencies | Зависимости

- `frames` — лица ищутся на полноразмерных кадрах
- ADR-0003 — модели распознавания кешируются на машине
- ADR-0005 — детекция выполняется в отдельном процессе Python

## Ubiquitous Language | Единый язык

- **Лицо**: рамка на кадре + вектор признаков + привязка к персонаже
- **Персона**: человек, известный в пределах проекта
- **Образец**: лицо, помеченное как эталон для обучения модели
- **Трек лиц**: одна и та же физическая голова в последовательности кадров
- **Тип лица**: персона, не персона (массовка), не определено

## Публичные контракты (API)

### Internal API (для других модулей системы)

`controllers/FaceController`, `controllers/PersonController`,
`controllers/FaceTrackController`;
`threads/projectactions/DetectFaces.kt`, `CreateFaces.kt`,
`RecognizeFaces.kt`, `RecheckFaces.kt`, `TrackFaces.kt`.

## Domain Invariants | Инварианты и правила бизнеса

- Естественный ключ лица — тройка (файл, номер кадра, номер лица в кадре).
- Вектор хранится строкой с разделителем `|`, а не как 128 колонок и не BLOB.
- Лицо признанным считается при сходстве выше порога **и** с запасом над
  вторым кандидатом. Порог и запас — константы в `FaceDetection`
  (`RECOGNIZE_THRESHOLD`, `RECOGNIZE_MARGIN`).
- Ошибочно признанное лицо опаснее непризнанного: оно попадает в базу и
  само становится образцом при следующем прогоне — ошибка тиражируется.
- Поле `person` у лица **не nullable**: нераспознанное лицо привязано к
  служебной персоне «не определено».

## Структура компонентов (C4 L3)

- `models/Face.kt`, `models/FaceTrack.kt`, `models/Person.kt`
- `controllers/FaceController.kt`, `PersonController.kt`, `FaceTrackRepo.kt`
- `src/main/resources/.../FaceDetector/detect_faces_in_folder.py`,
  `recognize_faces.py`
