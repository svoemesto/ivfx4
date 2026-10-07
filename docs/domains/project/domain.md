---
id: domain-project
title: "Домен: Проект"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - core
  - project
---

# Домен: Проект

## Обзор контекста (Bounded Context)

Корень всей модели. Проект — папка с настройками вывода и набор файлов.
Всё остальное в системе так или иначе принадлежит проекту.

## Dependencies | Зависимости

- `media-file` — файлы проекта
- `properties` — универсальные свойства проекта
- ADR-0002 — путь к папке проекта хранится per-machine

## Ubiquitous Language | Единый язык

- **Проект**: папка + параметры вывода + состав файлов
- **shortName**: короткое имя проекта, часть имён всех артефактов на диске
- **Контейнер**: формат файла (MP4, MKV, MXF)
- **Кодек видео/аудио**: X264, DNX, RAW; AAC, AC3, MP3, PCM

## Публичные контракты (API)

### Public API (для внешних клиентов)

HTTP-API нет. Внешним клиентом считается оператор в интерфейсе.

### Internal API (для других модулей системы)

`controllers/ProjectController`, `controllers/ProjectCdfController` —
создание, чтение, удаление, порядок проектов.

## Domain Invariants | Инварианты и правила бизнеса

- У проекта есть `shortName`: без него все имена артефактов на диске
  не строятся, и все `folderXxx` пусты.
- Параметры вывода (разрешение, fps, битрейты) — настройки проекта, а не
  файла: все файлы проекта режутся по ним одинаково.
- `folder` не является колонкой — это проекция на `ProjectCdf` по `computerId`.

## Структура компонентов (C4 L3)

- `models/Project.kt`, `models/ProjectCdf.kt`
- `controllers/ProjectController.kt`, `controllers/ProjectCdfController.kt`
- `fxcontrollers/ProjectEditFXController.kt`, `ProjectSelectFXController.kt`
