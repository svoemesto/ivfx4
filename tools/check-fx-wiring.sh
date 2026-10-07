#!/usr/bin/env bash
# Проверка того, что форма и контроллер связаны между собой.
#
# Смысл проверки: код может собраться и при этом ничего не делать. Так было
# с кнопкой «All to EXTRAS» — кнопка нарисована, обработчик написан, сборка
# проходит, а нажатие ни к чему не приводит, потому что в форме нет
# onAction="#...". Компиляция такое не ловит.
#
# Проверяется:
#   1. у каждого fx:id из формы есть поле в её контроллере;
#   2. каждый обработчик, на который ссылается форма, есть в контроллере;
#   3. в Kotlin нет задвоенных сигнатур и не поехали скобки;
#   4. формы в target/classes не отстали от исходных;
#   5. код собирается.
#
# Контроллер формы определяется по файлу, в котором вызывается FXMLLoader,
# а не по имени класса в getResource: имя там — владелец ресурса, а
# контроллер подставляется отдельно через loader.setController(this).
#
# Запуск: bash tools/check-fx-wiring.sh — 0 всё связано, 1 есть разрывы.

set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

MVN="$HOME/.local/opt/apache-maven-3.9.16/bin/mvn"
FAILED=0

echo "== 1. Связь формы и контроллера =="
python3 - <<'PY' || FAILED=1
import re, sys, pathlib

src = pathlib.Path("src/main/kotlin")
res = pathlib.Path("src/main/resources")

# Эти идентификаторы не привязаны к контроллеру и были такими до всех правок.
KNOWN_FREE = {"pbPersonsForScenes1", "pbShotsForScenes1"}

bad = 0
loads = []
for kt in sorted(src.rglob("*.kt")):
    text = kt.read_text(encoding="utf-8")
    for m in re.finditer(r'getResource\("([^"]+\.fxml)"\)', text):
        fxml_file = next(iter(res.rglob(m.group(1))), None)
        if fxml_file is None:
            print("  ОШИБКА: форма %s не найдена (упомянута в %s)" % (m.group(1), kt.name))
            bad = 1
            continue
        loads.append((fxml_file, kt))

if not loads:
    print("  ОШИБКА: не нашлось ни одной формы, загружаемой контроллером")
    sys.exit(1)

for fxml_file, ctrl_file in sorted(loads, key=lambda x: str(x[0])):
    fxml = fxml_file.read_text(encoding="utf-8")
    ctrl = ctrl_file.read_text(encoding="utf-8")
    name = ctrl_file.stem

    ids = set(re.findall(r'fx:id="([^"]+)"', fxml))
    fields = set(re.findall(r'private\s+(?:lateinit\s+)?var\s+(\w+)\s*[?:]', ctrl))
    fields |= set(re.findall(r'private\s+(?:val|var)\s+(\w+)\s*:', ctrl))

    missing = sorted(i for i in ids if i not in fields and i not in KNOWN_FREE)
    if missing:
        print("  ОШИБКА: %s: fx:id без поля в %s: %s"
              % (fxml_file.name, name, ", ".join(missing)))
        bad = 1
    else:
        print("  ok: %s: %d fx:id имеют поле в %s" % (fxml_file.name, len(ids), name))

    # Обработчик должен существовать. @FXML не обязателен: FXMLLoader находит
    # и публичный метод без аннотации. Смотрим только событийные атрибуты:
    # "#ff0000" в стиле — это цвет, а не ссылка на метод.
    refs = set(re.findall(r'\bon\w+="#(\w+)"', fxml))
    for h in sorted(refs):
        if not re.search(r'fun\s+' + re.escape(h) + r'\s*\(', ctrl):
            print("  ОШИБКА: %s: ссылка на обработчик #%s — метода нет в %s"
                  % (fxml_file.name, h, name))
            bad = 1
    if refs:
        found = sum(1 for h in refs if re.search(r'fun\s+' + re.escape(h) + r'\s*\(', ctrl))
        print("  ok: %s: обработчиков найдено %d из %d" % (fxml_file.name, found, len(refs)))

    # Кнопка с fx:id на bt без onAction — повод проверить подключение: ровно
    # на этом споткнулась «All to EXTRAS».
    for m2 in re.finditer(r'<Button\s[^>]*fx:id="(bt\w+)"[^>]*/?>', fxml):
        if "onAction" not in m2.group(0):
            print("  ВНИМАНИЕ: %s: кнопка %s без onAction — нажатие, скорее всего, "
                  "ни к чему не приведёт" % (fxml_file.name, m2.group(1)))

sys.exit(bad)
PY

echo "== 2. Дубликаты сигнатур и баланс скобок =="
python3 - <<'PY' || FAILED=1
import re, collections, pathlib, sys
bad = 0
for kt in sorted(pathlib.Path("src/main/kotlin").rglob("*.kt")):
    text = kt.read_text(encoding="utf-8")
    # Сравниваем сигнатуры целиком: перегрузки с разными параметрами законны,
    # одинаковые под именем — признак того, что правка задвоилась.
    sigs = []
    for line in text.split("\n"):
        m = re.match(r"\s*(?:private |public |internal |override )*fun (\w+)\s*\((.*?)\)", line)
        if m:
            sigs.append(m.group(1) + "(" + re.sub(r'\s+', '', m.group(2)) + ")")
    dup = [n for n, c in collections.Counter(sigs).items() if c > 1]
    if dup:
        print("  ОШИБКА: %s: задвоенные функции: %s" % (kt.name, ", ".join(sorted(set(dup)))))
        bad = 1
    depth = text.count("{") - text.count("}")
    if depth != 0:
        print("  ОШИБКА: %s: скобки не сходятся, разница %+d" % (kt.name, depth))
        bad = 1
if not bad:
    print("  ok: одинаковых сигнатур нет, скобки сходятся во всех файлах")
sys.exit(bad)
PY

echo "== 3. Формы в target/classes не отстали =="
python3 - <<'PY' || FAILED=1
import pathlib, sys, hashlib
bad = 0
src_root = pathlib.Path("src/main/resources")
out_root = pathlib.Path("target/classes")
files = list(src_root.rglob("*.fxml"))
for f in files:
    rel = f.relative_to(src_root)
    built = out_root / rel
    if not built.exists():
        print("  ОШИБКА: %s не собран в target/classes — запусти mvn process-classes" % rel)
        bad = 1
        continue
    if hashlib.sha256(f.read_bytes()).hexdigest() != hashlib.sha256(built.read_bytes()).hexdigest():
        print("  ОШИБКА: %s в target/classes отличается — запусти mvn process-classes" % rel)
        bad = 1
if not bad:
    print("  ok: %d форм в target/classes совпадают с исходниками" % len(files))
sys.exit(bad)
PY

echo "== 4. Сборка =="
if "$MVN" -B -DskipTests process-classes > /tmp/fx-check-mvn.log 2>&1; then
    echo "  ok: код собирается"
else
    echo "  ОШИБКА: сборка не прошла — ошибки в /tmp/fx-check-mvn.log"
    grep -E '^\[ERROR\].*\.kt' /tmp/fx-check-mvn.log | head -5 | sed 's/^/    /'
    FAILED=1
fi

echo
if [ "$FAILED" -eq 0 ]; then
    echo "ИТОГ: форма и контроллер связаны, можно перезапускать."
else
    echo "ИТОГ: есть разрывы. Перезапускать рано — сначала исправить."
fi
exit "$FAILED"
