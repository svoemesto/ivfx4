# #244 — RF вхолостую и NullPointerException в автопрокрутке

Дата: 2026-10-08. Статус: на проверке владельцу.

## Откуда задача

Владелец запустил RF на E03 и попросил посмотреть журнал. Прогоны отработали
с `[ГОТОВО]`, падений не было, но два дефекта нашлись.

## Дефект 1. RF работает вхолостую, когда распознавать нечего

Журнал (три прогона подряд за один сеанс):

| прогон | неопределённых лиц | галерея | распознано |
|---|---|---|---|
| 1 | 1464 | 21340 | 23 |
| 2 | **0** | 22415 | 0 |
| 3 | **0** | 22415 | 0 |

Проверка на живом запуске (`docker exec … psql`): в E03 неопределённых лиц
**ноль** — 7357 отнесены к персонам, 54 к «не персона», 1313 к EXTRAS.

Защита в `RecognizeFaces` была **только на пустую галерею**
(`if (galleryFaces.isEmpty())`). На пустую очередь — не с кем сравнивать — нет.
Приложение писало json на 22 415 отмеченных лиц, запускало Python и читало json
обратно, чтобы не обновить ничего.

**Исправлено:** зеркальная проверка `if (arrFrameFaces.isEmpty())` сразу после
проверки галереи. Сообщение в журнале и в надписи формы — «Нечего распознавать:
в серии нет неопределённых лиц», вместо «идёт распознавание».

## Дефект 2. NullPointerException в автопрокрутке таблиц

Из журнала:

```
Exception in thread "Thread-440" java.lang.NullPointerException:
Cannot invoke "javafx.scene.control.IndexedCell.getIndex()"
because the return value of "javafx.scene.control.skin.VirtualFlow.getFirstVisibleCell()" is null
    at ShotsEditFXController.tblShotsSmartScroll(ShotsEditFXController.kt:2236)
```

Семь падений за один сеанс, каждое убивало свой фоновый поток. Причина:
`VirtualFlow.firstVisibleCell` и `lastVisibleCell` — **поля**, а не
вычисляемые свойства; пока таблица не размечена, они `null`. Проверка
`cellCount > 0` от этого не спасает.

Падало **семь функций подряд**, а не одна: `tblPagesFramesSmartScroll`,
`tblPagesFacesSmartScroll`, `tblShotsSmartScroll`, `tblScenesSmartScroll`,
`tblShotsForScenesSmartScroll`, `tblShotsForEventsSmartScroll`,
`tblPersonsAllSmartScroll` — у всех был один и тот же непроверенный шаблон.

**Исправлено:** одна функция `visibleRangeOf(flow)` возвращает диапазон видимых
строк или `null`, когда смотреть нечего; семь функций вызывают её и выходят на
`null`. Нет видимых ячеек — сравнивать нечего и прокручивать незачем: таблица
отрисуется сама.

## Проверка

| Проверка | Результат |
|---|---|
| `grep "firstVisibleCell\|lastVisibleCell"` | 2 вхождения, оба в `visibleRangeOf` со safe-call |
| `mvn -B test` | 28 тестов, 0 падений |
| `bash tools/check-ktlint.sh` | новых нарушений нет |
| `bash tools/check-fx-wiring.sh` | форма связана, сборка проходит |
| `python3 docs/scripts/lint-docs.py` | 0 |
| Новый запуск приложения | PID 2754408, 0 ошибок |

**Чего не проверено машинно:** что NullPointerException действительно исчез —
нужен щелчок по планам в живом приложении. Проверка владельца: пройти по
вкладке Tracks, как в прошлый раз; в журнале не должно быть ни одного
`NullPointerException`.

## Что осталось

- `doSelectFacesRb` — переключатель лиц по-прежнему только пишет в журнал.
- `doSelectFacesCb` разыменовывает `currentPersonExt!!`: щелчок по флажку до
  выбора персоны уронит поток загрузки.
- Порог распознавания 0,45 не калиброван. Владелец подтвердил, что качество
  распознавания его устраивает, поэтому не трогал.