#!/usr/bin/env python3
"""Сравнение отчёта ktlint с зафиксированным состоянием проекта.

Зачем это нужно
---------------
Проект написан в 2021–2022 годах в компактном стиле, не совпадающем с
официальным ktlint: 8036 нарушений разом. Вывод «исправь всё» не годится —
он блокирует любую работу. Нужен отпечаток (baseline), который скрывает
имеющееся и пропускает новое.

Чем этот отпечаток отличается от встроенного в ktlint
------------------------------------------------------
1. Привязан к паре (файл, правило) и **счётчику**, а не к номеру строки.
   Правка соседних строк не ломает отпечаток, как бывает при привязке к
   строкам.
2. Считает количество. Если правило встречалось в файле 3 раза, а теперь
   встречается 4 — это новое нарушение, и оно попадёт в отчёт. Наивный
   отпечаток по паре (файл, правило) такое бы пропустил.
3. Встроенный `--baseline` ktlint 1.5.0 в этом проекте не сработал:
   проверил на одном файле с абсолютными и относительными путями —
   подавление нулевое. Поэтому формат свой.

Формат (config/ktlint/baseline.xml):

    <baseline version="1" mode="count-by-file-rule">
      <file name="src/main/kotlin/.../TrackRepo.kt">
        <rule id="standard:no-multi-spaces" count="3"/>
      </file>
    </baseline>

Запуск:
    check   tools/ktlint-baseline.py check   <report.json> <baseline.xml>
    record  tools/ktlint-baseline.py record  <report.json> <baseline.xml>
"""
import json
import os
import sys
from collections import Counter


def load_report(path):
    """Читает JSON-отчёт ktlint.

    Отчёт может идти после предупреждения в stderr-подобной строке, поэтому
    JSON ищется с первой строки, начинающейся с '['.
    """
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip().startswith("["))
    return json.loads("\n".join(lines[start:]))


def count_violations(report, root):
    """Считает нарушения по паре (файл, правило) с путями относительно корня."""
    result = Counter()
    for entry in report:
        name = os.path.relpath(entry["file"], root)
        for err in entry.get("errors", []):
            # У синтаксических ошибок ktlint не даёт правило (rule пустой).
            # Их кладём в один счётчик: их появление означает, что файл вообще
            # не компилируется, и точное имя правила тут несущественно.
            result[(name, err.get("rule") or "синтаксическая ошибка")] += 1
    return result


def load_baseline(path):
    """Читает отпечаток. Отсутствующий файл — пустой отпечаток."""
    if not os.path.exists(path):
        return Counter()
    import xml.etree.ElementTree as ET

    root = ET.parse(path).getroot()
    counter = Counter()
    for file_el in root.findall("file"):
        name = file_el.get("name")
        for rule_el in file_el.findall("rule"):
            counter[(name, rule_el.get("id"))] = int(rule_el.get("count", "0"))
    return counter


def write_baseline(path, counter, total_before):
    import xml.etree.ElementTree as ET

    root = ET.Element("baseline", {"version": "1", "mode": "count-by-file-rule"})
    by_file = {}
    for (name, rule), count in sorted(counter.items()):
        by_file.setdefault(name, []).append((rule, count))
    for name in sorted(by_file):
        file_el = ET.SubElement(root, "file", {"name": name})
        for rule, count in by_file[name]:
            ET.SubElement(file_el, "rule", {"id": rule, "count": str(count)})
    # Каждый <file> — на своей строке: отпечаток машиночный, но его правят
    # руками при сокращении долга, и в git он должен читаться дифом, а не как
    # одна гигантская строка.
    if hasattr(ET, "indent"):
        ET.indent(root, space="  ")
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)

    delta = sum(counter.values()) - total_before
    print(
        f"Отпечаток записан: записей {sum(counter.values())}, "
        f"файлов {len(by_file)} (было {total_before}, изменение {delta:+d})"
    )


def main():
    if len(sys.argv) != 4:
        print(__doc__)
        return 2
    mode, report_path, baseline_path = sys.argv[1], sys.argv[2], sys.argv[3]
    root = os.getcwd()

    report = load_report(report_path)
    current = count_violations(report, root)

    if mode == "record":
        write_baseline(baseline_path, current, sum(load_baseline(baseline_path).values()))
        return 0

    if mode != "check":
        print(f"Неизвестный режим: {mode}", file=sys.stderr)
        return 2

    baseline = load_baseline(baseline_path)
    new = current - baseline  # Counter: положительные значения — новые
    expired = baseline - current

    for (name, rule), count in sorted(new.items(), key=lambda kv: (-kv[1], kv[0])):
        print(f"{name}: {rule} — новых вхождений {count}")
    if expired:
        print(
            f"\nСтало меньше, чем в отпечатке ({sum(expired.values())} записей) — "
            f"долг сократился, стоит перезаписать отпечаток:"
            f" bash tools/update-ktlint-baseline.sh"
        )

    if new:
        print(f"\nИТОГО новых нарушений: {sum(new.values())}")
        return 1
    print("ktlint: новых нарушений нет")
    return 0


if __name__ == "__main__":
    sys.exit(main())