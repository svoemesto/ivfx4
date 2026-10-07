#!/usr/bin/env bash
# Установка линтера ktlint. Бинарь 70 МБ, поэтому в git не коммитится
# и ставится по версии из tools/ktlint/VERSION.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1

VER=$(tr -d '[:space:]' < tools/ktlint/VERSION)
URL="https://github.com/pinterest/ktlint/releases/download/${VER}/ktlint"
mkdir -p tools/ktlint

# Идемпотентно: бинарь на 70 МБ не качается заново, если нужная версия уже
# на месте и запускается. Проверяется фактический запуск, а не наличие файла.
if [ -x tools/ktlint/ktlint ] && tools/ktlint/ktlint --version 2>/dev/null | grep -qF "${VER}"; then
    echo "ktlint ${VER} уже установлен, ничего не скачиваю."
    exit 0
fi

echo "Скачиваю ktlint ${VER}"
curl -fSL --progress-bar -o tools/ktlint/ktlint "$URL"
chmod +x tools/ktlint/ktlint
echo "Установлено: $(tools/ktlint/ktlint --version 2>&1 | head -1)"
