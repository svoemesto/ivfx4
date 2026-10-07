#!/usr/bin/env bash
# Проверка Kotlin-кода линтером ktlint.
#
# Проект написан в 2021–2022 годах в компактном стиле, не совпадающем с
# официальным ktlint: 8036 нарушений разом. Вывод «исправь всё» блокирует
# любую работу, поэтому текущее состояние зафиксировано в отпечатке
# config/ktlint/baseline.xml, а проверка падает лишь на НОВЫХ нарушениях.
#
# Отпечаток привязан к паре (файл, правило) и счётчику, а не к строке:
# правка соседних строк его не ломает, а новое вхождение того же правила
# попадает в отчёт. Подробности — в tools/ktlint-baseline.py.
#
# Запуск:
#   bash tools/check-ktlint.sh       # 0 = чисто, 1 = есть новые нарушения
#
# Сжатие долга: починить нарушения в файле → bash tools/update-ktlint-baseline.sh
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1

KTLINT="tools/ktlint/ktlint"
BASELINE="config/ktlint/baseline.xml"

if [ ! -x "$KTLINT" ]; then
    echo "Линтер не установлен. Поставить: bash tools/install-ktlint.sh" >&2
    exit 1
fi

TMP=$(mktemp); trap 'rm -f "$TMP"' EXIT
"$KTLINT" --reporter=json "src/main/kotlin/**/*.kt" > "$TMP" 2>/dev/null || true
python3 tools/ktlint-baseline.py check "$TMP" "$BASELINE"
