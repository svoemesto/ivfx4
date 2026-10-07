#!/usr/bin/env bash
# Запуск старого приложения ivfx на этой машине.
#
# Что сделано ради запуска, и почему:
#   1. Maven стоит локально, без прав root: ~/.local/opt/apache-maven-3.9.16.
#   2. JavaFX-банки с нативными библиотеками надо класть на модульный путь,
#      на classpath JavaFX 11 не запускается: «JavaFX runtime components
#      are missing».
#   3. Приложение помнит текущую базу в собственном файле h2db.mv.db.
#      Там была запись на MySQL, которого на этой машине нет. Переключено
#      на встроенную H2 (запись 1). Резервная копия: h2db.mv.db.bak.
#   4. Классы берём из target/classes, а не из собранного банка. Приложение
#      всюду достаёт свои ресурсы (картинки-заглушки, скрипт детектора лиц,
#      бинарники ffmpeg) по файловому пути из getResource(). Из банка такой
#      ресурс отдаётся как URI вида jar:file:... — непрозрачный, у него
#      getPath() возвращает null, и загрузка класса падает с NPE. Из каталога
#      URI обычный file:, и путь получается верным. Это и есть режим, в
#      котором приложение писалось: автор запускал его из IDE, а не из банка.
#
# Окно появится на рабочем столе текущей сессии.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MVN="$HOME/.local/opt/apache-maven-3.9.16/bin/mvn"
CP_FILE="$PROJECT_DIR/target/runtime-classpath.txt"

cd "$PROJECT_DIR"

if [ ! -x "$MVN" ]; then
    echo "Не найден Maven: $MVN" >&2
    exit 1
fi

# Пересобираем только когда исходники новее собранных классов.
# Kotlin-компилятор проходит весь модуль целиком даже когда ничего не
# менялось (замерено: 32 с), а запускать это с ярлыка каждый раз не нужно.
NEEDS_BUILD=1
if [ -d "$PROJECT_DIR/target/classes" ]; then
    NEWEST_CLASS="$(find "$PROJECT_DIR/target/classes" -name '*.class' -printf '%T@\n' 2>/dev/null | sort -n | tail -1)"
    NEWEST_SRC="$(find "$PROJECT_DIR/src/main" -type f \
        \( -name '*.kt' -o -name '*.fxml' -o -name '*.xml' -o -name '*.properties' \) \
        -printf '%T@\n' 2>/dev/null | sort -n | tail -1)"
    if [ -n "$NEWEST_CLASS" ] && [ -n "$NEWEST_SRC" ] &&
       awk "BEGIN{exit !($NEWEST_CLASS >= $NEWEST_SRC)}"; then
        NEEDS_BUILD=0
    fi
fi
if [ "$NEEDS_BUILD" = "1" ]; then
    echo "Собираю проект..." >&2
    "$MVN" -q -B -DskipTests process-classes
fi

if [ ! -f "$CP_FILE" ]; then
    "$MVN" -q -B dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
fi

# Модели лиц не хранятся в репозитории: 184 МБ бинарников в git раздувают
# каждый клон и никогда не меняются между правками кода. Скачиваются один
# раз и кешируются в ~/.insightface/models/buffalo_l. Само приложение
# работает и без них — распознавание лиц просто будет недоступно, — поэтому
# здесь они не обязательны, а лишь предлагаются.
MODELS_DIR="${IVFX_MODELS_DIR:-$HOME/.insightface/models/buffalo_l}"
if [ ! -s "$MODELS_DIR/det_10g.onnx" ] || [ ! -s "$MODELS_DIR/w600k_r50.onnx" ]; then
    echo
    echo "Модели распознавания лиц не найдены в $MODELS_DIR" >&2
    echo "Без них работает всё, кроме DetectFaces / RecognizeFaces." >&2
    echo "Скачать (~275 МБ, один раз):  bash tools/fetch-models.sh" >&2
    echo
fi

# Банки с нативными библиотеками — на модульный путь, остальное — в classpath.
MODULE_PATH="$(tr ':' '\n' < "$CP_FILE" | grep -E 'javafx.*-linux\.jar$' | tr '\n' ':')"
CLASSPATH="$PROJECT_DIR/target/classes:$(tr ':' '\n' < "$CP_FILE" | grep -vE 'javafx.*-linux\.jar$' | tr '\n' ':')"

echo "Запускаю ivfx. Окно появится на рабочем столе. Закрыть: Ctrl+C здесь или окно."
exec java \
    --module-path "$MODULE_PATH" \
    --add-modules javafx.controls,javafx.fxml \
    -cp "$CLASSPATH" \
    com.svoemesto.ivfx.apps.ProjectFXApp
