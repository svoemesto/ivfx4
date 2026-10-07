---
id: guide-build
title: "Сборка и запуск"
status: Accepted
supersedes: null
superseded_by: null
date: 2026-10-07
modules:
  - ivfx
tags:
  - guide
  - build
---

# Сборка и запуск

## Инструменты

```bash
MVN=~/.local/opt/apache-maven-3.9.16/bin/mvn   # в PATH maven нет
JAVA_HOME=/usr/lib/jvm/jdk-18                  # JDK 18.0.2.1
```

## Сборка

```bash
$MVN -q clean compile      # компиляция
$MVN -q package            # банк собирается, но для запуска не нужен
```

Проект собирается под Java 8 (`source/target 8`) на JDK 18 — это
предупреждение, а не ошибка.

## Запуск

```bash
bash run-old-app.sh
```

Только из `target/classes` — см. ADR-0004. Банк запускать нельзя.

## Модели лиц

```bash
bash tools/fetch-models.sh     # скачать при первом запуске, ~275 МБ
```

Без моделей приложение работает, кроме распознавания лиц.

## Проверки

```bash
python3 docs/scripts/lint-docs.py     # структура документации
bash tools/check-fx-wiring.sh         # связь FXML и контроллера + сборка
```

## Задачи

```bash
set -a && source .env.local-tracker && set +a
./tools/tracker.sh healthcheck
./tools/tracker.sh claim-issue <NNN>
```

## После закрытия приложения

Проверять, что процесс умер: `ps -ef | grep com.svoemesto.ivfx`.
Закрытие окна не завершает JVM — см. ADR-0005.
