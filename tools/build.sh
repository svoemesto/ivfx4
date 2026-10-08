#!/usr/bin/env bash
# Сборка проекта с проверкой, что приложение остановлено.
#
# Правило Tier-1 требует не запускать `clean`, пока работает приложение:
# формы читаются с диска при каждом открытии окна, `clean` их удаляет, и
# после этого форма падает с «Location is not set».
#
# Написано потому, что правило нарушалось дважды за один день, причём оба
# раза одна и та же проверка была выполнена в той же команде — просто её
# вывод не был посмотрен. Напоминание не сработало, поэтому проверка стала
# частью сборки.
#
#   bash tools/build.sh              — clean + тесты, только если приложение не запущено
#   bash tools/build.sh --no-clean   — без clean (можно при живом приложении,
#                                     но всё равно с проверкой)
#
# Выход: 0 — собрано, 1 — приложение работает, сборка не тронута.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1

MVN="$HOME/.local/opt/apache-maven-3.9.16/bin/mvn"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/jdk-18}"

USE_CLEAN=1
for arg in "$@"; do
    case "$arg" in
        --no-clean) USE_CLEAN=0 ;;
        *) echo "Неизвестный аргумент: $arg" >&2; exit 1 ;;
    esac
done

RUNNING="$(ps -eo pid,args --no-headers | awk '/ProjectFXApp/ && /java/ && !/awk/ {print $1}' | tr '\n' ' ')"

if [ -n "$RUNNING" ] && [ "$USE_CLEAN" = "1" ]; then
    echo "Приложение работает (PID: $RUNNING)." >&2
    echo "Остановить его или собрать без clean: bash tools/build.sh --no-clean" >&2
    exit 1
fi

if [ -n "$RUNNING" ]; then
    echo "ВНИМАНИЕ: приложение работает (PID: $RUNNING), сборка идёт без clean." >&2
fi

GOALS="test"
[ "$USE_CLEAN" = "1" ] && GOALS="clean test"

"$MVN" -B $GOALS