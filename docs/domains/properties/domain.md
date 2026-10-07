---
id: domain-properties
title: "Домен: Универсальные свойства"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - data
  - properties
---

# Домен: Универсальные свойства

## Обзор контекста (Bounded Context)

Механизм произвольных ключ-значений, привязанных к любой сущности. Позволяет
оператору завести своё поле, не меняя схему базы.

## Dependencies | Зависимости

- `filters` — свойства используются в условиях отбора

## Ubiquitous Language | Единый язык

- **Свойство**: пара «ключ — значение» у произвольного объекта
- **Класс-владелец**: строковое имя класса, например «Shot» или «Person»
- **Порядок свойства**: позиция в списке, задаётся оператором

## Публичные контракты (API)

### Internal API (для других модулей системы)

`controllers/PropertyController`, `controllers/PropertyCdfController` —
сохранение, удаление, изменение порядка.

## Domain Invariants | Инварианты и правила бизнеса

- **Внешних ключей нет**: привязка идёт парой (класс-владелец, идентификатор).
- Очистка свойств при удалении объекта выполняется вручную: каскад их не
  покрывает, и без ручной очистки остаются сироты.
- Версии `Property` и `PropertyCdf` различаются наличием `computerId`.

## Структура компонентов (C4 L3)

- `models/Property.kt`, `models/PropertyCdf.kt`
- `controllers/PropertyController.kt`, `PropertyCdfController.kt`
