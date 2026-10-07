#!/usr/bin/env bash
# Пересчёт отпечатка ktlint под текущее состояние кода.
#
# Запускать ПОСЛЕ того, как нарушения в конкретных файлах исправлены.
# Запускать «просто так» нельзя: отпечаток вслепую примет и новые
# нарушения, и защита перестанет работать.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1

KTLINT="tools/ktlint/ktlint"
BASELINE="config/ktlint/baseline.xml"
[ -x "$KTLINT" ] || { echo "Сначала: bash tools/install-ktlint.sh" >&2; exit 1; }

TMP=$(mktemp); trap 'rm -f "$TMP"' EXIT
"$KTLINT" --reporter=json "src/main/kotlin/**/*.kt" > "$TMP" 2>/dev/null || true
python3 tools/ktlint-baseline.py record "$TMP" "$BASELINE"
