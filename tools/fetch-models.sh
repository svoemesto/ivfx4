#!/usr/bin/env bash
# Скачивание моделей лиц при первой установке.
#
# Зачем: модели распознавания лиц — это ~184 МБ бинарных файлов. В git их
# держать нельзя: они раздувают каждый клон и никогда не меняются между
# правками кода. Поэтому они скачиваются один раз и кешируются на машине.
#
# Откуда: пакет buffalo_l из официального релиза InsightFace
# (v0.7). Тот же источник использует сама библиотека insightface при
# FaceAnalysis(name='buffalo_l') — так что файлы совпадают с её эталоном.
#
# Что скачивается:
#   det_10g.onnx     17 МБ   SCRFD-10G, детектор лиц
#   w600k_r50.onnx  167 МБ   ArcFace, эмбеддинги 128 измерений
#
# Использование:
#   bash tools/fetch-models.sh           # скачать, если отсутствуют
#   bash tools/fetch-models.sh --force   # перекачать заново
#   IVFX_MODELS_DIR=/path bash tools/fetch-models.sh
#
# Идемпотентно: если обе модели на месте и --force не задан, скрипт
# ничего не качает и молча выходит.

set -euo pipefail

MODELS_URL="https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip"
MODELS_DIR="${IVFX_MODELS_DIR:-$HOME/.insightface/models/buffalo_l}"
REQUIRED=(det_10g.onnx w600k_r50.onnx)

FORCE=0
[ "${1:-}" = "--force" ] && FORCE=1

# Проверка: на месте ли всё нужное.
have_all() {
    for m in "${REQUIRED[@]}"; do
        [ -s "$MODELS_DIR/$m" ] || return 1
    done
    return 0
}

if [ "$FORCE" -eq 0 ] && have_all; then
    echo "Модели уже на месте: $MODELS_DIR"
    exit 0
fi

for tool in curl unzip; do
    command -v "$tool" >/dev/null 2>&1 || {
        echo "Нужен $tool, а его нет. Установить: sudo apt install $tool" >&2
        exit 1
    }
done

mkdir -p "$MODELS_DIR"
TMPDIR_DL="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_DL"' EXIT
ZIP="$TMPDIR_DL/buffalo_l.zip"

echo "Скачиваю $MODELS_URL"
echo "  (~275 МБ, при первом запуске — долго; повторные запуски пропускают)"
# Прогресс-бар только в терминале: в логе он превращается в тысячи
# управляющих символов и засоряет вывод.
if [ -t 1 ]; then CURL_PROGRESS=(--progress-bar); else CURL_PROGRESS=(-sS); fi
curl -fL --retry 3 --retry-delay 3 "${CURL_PROGRESS[@]}" \
    -o "$ZIP" "$MODELS_URL"

# Из архива нужны только два файла — остальное (1k3d68, 2d106det,
# genderage) приложение не открывает, и держать их незачем.
echo "Распаковываю нужные модели в $MODELS_DIR"
for m in "${REQUIRED[@]}"; do
    # В buffalo_l файлы лежат прямо в корне архива, но раскладка
    # может измениться, поэтому пробуем оба варианта пути.
    unzip -o -j -q "$ZIP" "$m" -d "$MODELS_DIR" 2>/dev/null \
        || unzip -o -j -q "$ZIP" "*/$m" -d "$MODELS_DIR"
done

for m in "${REQUIRED[@]}"; do
    if [ ! -s "$MODELS_DIR/$m" ]; then
        echo "Ошибка: $m не распаковался. Архив: $MODELS_URL" >&2
        exit 1
    fi
done

echo "Готово:"
ls -lh "$MODELS_DIR" | sed 's/^/  /'