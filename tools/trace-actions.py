#!/usr/bin/env python3
"""
Проставляет Trace.action(...) в обработчики, объявленные в формах через
onAction="#имя".

Зачем: журнал должен отвечать на вопрос «что сделал пользователь», даже
если на экране ничего не изменилось. Сейчас о таких действиях в журнале
нет вообще ничего — был случай, когда склейка плана молча не применилась,
и по журналу это установить было невозможно.

Правила:
  * вставляется первым оператором тела метода;
  * уже помеченные методы пропускаются — скрипт идемпотентен;
  * вставляется только то, что реально объявлено в FXML и реально
    нашлось в контроллере; несовпадения печатаются и не правятся молча.

Запуск:
  python3 tools/trace-actions.py            # сухой ход: только показать
  python3 tools/trace-actions.py --apply    # править
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FXML_DIR = ROOT / "src/main/resources/com/svoemesto/ivfx/fxcontrollers"
KOTLIN_DIR = ROOT / "src/main/kotlin"

APPLY = "--apply" in sys.argv
MARK = "Trace.action("

# Форма -> контроллер определяется по тому, кто грузит этот ресурс.
def controller_for(form: Path):
    needle = f'getResource("{form.name}")'
    for kt in KOTLIN_DIR.rglob("*.kt"):
        if needle in kt.read_text(encoding="utf-8"):
            return kt
    return None


def handlers_of(form: Path):
    text = form.read_text(encoding="utf-8")
    return re.findall(r'onAction="#([A-Za-z_][A-Za-z0-9_]*)"', text)


def body_start(src: str, name: str):
    """Позиция сразу после открывающей скобки тела метода, либо None."""
    for m in re.finditer(r"fun\s+" + re.escape(name) + r"\s*\(", src):
        i = src.find("(", m.start())
        depth = 0
        while i < len(src):
            if src[i] == "(":
                depth += 1
            elif src[i] == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        j = src.find("{", i)
        if j == -1:
            return None
        # отступ строки, где стоит объявление
        line_start = src.rfind("\n", 0, m.start()) + 1
        indent = src[line_start:m.start()]
        return j + 1, indent + "    "
    return None


def main():
    done = missed = 0
    for form in sorted(FXML_DIR.glob("*.fxml")):
        names = handlers_of(form)
        if not names:
            continue
        kt = controller_for(form)
        if kt is None:
            print(f"  [пропуск] {form.name}: контроллер не найден")
            missed += len(names)
            continue
        src = kt.read_text(encoding="utf-8")
        original = src
        touched = 0
        for name in sorted(set(names)):
            if MARK + f'"{name}"' in src:
                continue
            spot = body_start(src, name)
            if spot is None:
                print(f"  [не найден] {kt.name}: {name}")
                missed += 1
                continue
            pos, indent = spot
            src = src[:pos] + f"\n{indent}{MARK}\"{name}\")" + src[pos:]
            touched += 1
            done += 1
        if touched and APPLY and src != original:
            kt.write_text(src, encoding="utf-8")
        print(f"  {form.name}: помечено {touched} из {len(set(names))}")
    verb = "помечено" if APPLY else "будет помечено (--apply не задан)"
    print(f"\nИТОГО {verb}: {done}, не найдено: {missed}")


if __name__ == "__main__":
    main()