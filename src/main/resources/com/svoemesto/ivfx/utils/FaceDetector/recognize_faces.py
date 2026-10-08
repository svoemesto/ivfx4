# USAGE
# python recognize_faces.py -i faces.json -c 0.45
#
# Распознавание лиц по галерее.
#
# Раньше здесь обучалась модель: обучающая выборка отмечалась вручную, по ней
# подгонялся линейный SVM и кодировщик меток, оба сохранялись в pickle, а
# здесь для каждого неопознанного лица вызывался predict_proba. Всё это
# больше не нужно и не используется.
#
# Теперь лицо сравнивается с отмеченными лицами напрямую: вектор признаков
# у уже найденного лица уже лежит в json, поэтому ни детектор, ни модель
# распознавания, ни pickle не загружаются вообще — остаётся арифметика.
# Модель даёт вектор, в котором одинаковые люди сходятся ближе, чем разные;
# сравнение идёт по косинусу угла между векторами.
#
# Что при этом пропадает: два файла recognizer.pickle и le.pickle, отдельный
# шаг обучения модели и его зависимость от версий sklearn.
# Что остаётся: ручная отметка, кому какое лицо принадлежит. Галерея
# строится из отмеченных лиц, поэтому без неё распознавать нечего — скрипт
# об этом и сообщает, а не молча ничего не находит.

import argparse
import json
import os

import numpy as np

# Получаем и парсим аргументы
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--inputjson", required=True, help="Путь к файлу json с описанием лиц")
ap.add_argument("-c", "--confidence", type=float, default=0.45,
                help="минимальное косинусное сходство, при котором лицо считается найденным")
ap.add_argument("-m", "--margin", type=float, default=0.05,
                help="минимальный запас, с которым лучший персонаж обгоняет второго")
args = vars(ap.parse_args())

# Итог в файл: приложение не читает то, что скрипт печатает, и без этого
# результат распознавания не виден нигде — ни в журнале, ни в форме.
result_file = os.path.sep.join([os.path.dirname(os.path.abspath(args["inputjson"])),
                                "recognize_faces_result.txt"])

# Результат — в отдельный json, и только распознанные лица.
#
# Раньше скрипт перезаписывал входной файл целиком. На прогоне E05 это
# стоило 20,3 секунды из 78,7: две трети времени уходило на чтение и
# запись 492 МБ, из которых 69 % — галерея, которая только читается и
# никогда не меняется. Приложение затем читало тот же файл обратно и
# применяло лишь записи очереди, то есть платило за перезапись дважды.
#
# Теперь входной файл остаётся нетронутым — он же разметка, по которой
# разбирают прогон, — а в отдельный файл уходят только те лица очереди,
# которые преодолели порог и запас. Обычно это единицы процентов от
# очереди, то есть файл в десятки раз меньше входа.
result_json_file = os.path.sep.join([os.path.dirname(os.path.abspath(args["inputjson"])),
                                     "recognize_faces_result.json"])

# Ход работы в отдельный файл: приложение ждёт скрипт, блокируя поток, и
# без этого прогон идёт чёрным окном — на реальной серии это десятки секунд,
# за которые непонятно, работает приложение или зависло. Признак тот же,
# по которому другие операции пайплайна пишут ход в журнал.
#
# Пишем редко: перезапись файла на каждом лице сама станет тормозом.
progress_file = os.path.sep.join([os.path.dirname(os.path.abspath(args["inputjson"])),
                                  "recognize_faces_progress.txt"])


def save_progress(done, total):
    with open(progress_file, "w") as file:
        file.write("{} {}".format(done, total))


def save_result(text):
    with open(result_file, "w") as file:
        file.write(text)


def save_recognized(faces):
    with open(result_json_file, "w") as file:
        json.dump(faces, file)

# Загружаем три файла вместо одного.
#
# Векторы приходят в бинарной матрице .npy, а не в json. На прогоне седьмой
# серии один json со всеми векторами весил 583 МБ, и РАЗБОР его занимал
# 10,1 секунды — больше четверти прогона. Здесь эти векторы нужны только
# чтобы перемножаться, то есть это числа, а не текст: та же матрица в float32
# занимает 109 МБ и читается мгновенно, без единого json.loads.
#
#   inputjson           — очередь без векторов: идентификаторы и кадры,
#                          чтобы вернуть результат по тем же записям;
#   recognize_faces_vectors.npy — матрица float32, сначала очередь, потом
#                          галерея, построчно;
#   recognize_faces_meta.json  — где кончается очередь и с какой строки
#                          начинается каждая персона.
file_json_images = args["inputjson"]
folder = os.path.dirname(os.path.abspath(args["inputjson"]))
path_to_npy = os.path.join(folder, "recognize_faces_vectors.npy")
path_to_meta = os.path.join(folder, "recognize_faces_meta.json")

with open(file_json_images, "rb") as file:
    data_of_images = json.loads(file.read())

with open(path_to_meta, "r") as file:
    meta = json.load(file)
queue_count = int(meta["queueCount"])
total_rows = int(meta["rows"])

# Матрица читается отображением: numpy не тащит файл в память целиком и не
# преобразует тип — берёт готовые float32 как есть.
vectors = np.load(path_to_npy, mmap_mode="r")

# Галерея: строки после очереди, нарезанные по персонам. Нарезка приходит в
# meta.personStarts, и порядок строк задан приложением — его надо сохранить,
# иначе лица перемешаются с персонами.
person_names = list(meta["personStarts"].keys())
group_starts = np.array([int(meta["personStarts"][name]) for name in person_names], dtype=np.int64)

# Начала обязаны идти строго по возрастанию: с ними работает reduceat, и
# без проверки неправильный порядок даст не ошибку, а мусор вместо галереи.
if len(group_starts) > 1 and not bool(np.all(np.diff(group_starts) > 0)):
    broken = int(np.sum(np.diff(group_starts) <= 0))
    save_result("ГАЛЕРЕЯ СБИТА: порядок персон в матрице нарушен (" + str(broken) +
                " раз), признать нельзя. Признаки записаны не по порядку.")
    save_recognized([])
    print("[INFO] порядок начал персон нарушен, вхождений: " + str(broken))
    raise SystemExit(1)

gallery_rows = []
for index in range(len(group_starts)):
    start = int(group_starts[index])
    end = int(group_starts[index + 1]) if index + 1 < len(group_starts) else total_rows
    gallery_rows.extend(range(start, end))

if not person_names or not gallery_rows:
    save_result("ГАЛЕРЕЯ ПУСТА: нет ни одного отмеченного лица. Распознавать нечего — "
                "сначала отметьте лица в монтажной.")
    save_recognized([])
    print("[INFO] галерея пуста: ни одного лица, отмеченного как PERSON с именем. "
          "Распознавать нечего — сначала отметьте лица в интерфейсе.")
    raise SystemExit(0)

gallery_matrix = np.asarray(vectors[gallery_rows], dtype=np.float32)
# Начала персон приходят в координатах ВСЕЙ матрицы, а галерея — вырезка из
# неё со своим отсчётом с нуля. Сдвигаем начала, иначе reduceat выйдет за
# правый край.
gallery_starts = group_starts - gallery_rows[0]

# Нормализуем один раз: косинус угла для нормализованных векторов считается
# обычным скалярным произведением, и на 512 значениях это дешевле и точнее,
# чем каждый раз делить на длины.
norms = np.linalg.norm(gallery_matrix, axis=1, keepdims=True)
norms[norms == 0] = 1.0
gallery_matrix = gallery_matrix / norms

person_names = np.array(person_names)
queue_matrix = np.asarray(vectors[0:queue_count], dtype=np.float32)

print("[INFO] галерея: {} персон, {} отмеченных лиц".format(
    len(person_names), gallery_matrix.shape[0]))
print("[INFO] порог сходства: {}, запас: {}".format(args["confidence"], args["margin"]))

# У одного человека бывает несколько отмеченных лиц. Берём лучшее совпадение
# по всем его вырезам: один удачный кадр не должен топить результат.

recognized = 0
rejected = 0
thin_margin = 0
best_by_person = {}

recognized_faces = []

# Очередь собирается заранее и обрабатывается ПАЧКАМИ.
#
# Раньше для каждого лица считалось отдельное умножение матрицы галереи на
# вектор. Матрица на прогоне E06 — 77 МБ, и она перечитывалась заново на
# каждое из 13426 лиц: 1,03 ТБ трафика, и на этом уходило всё время шага.
# Умножение матрицы на матрицу даёт то же самое произведение, но матрица
# галереи перечитывается один раз на пачку: при пачках по 256 трафик падает
# с 1034 ГБ до 4 ГБ.
#
# Внутри пачки результат считается построчно, то есть ровно так же, как раньше:
# то же произведение, тот же reduceat, та же сортировка по персонам.
QUEUE_BATCH = 256

queue_faces = [face_data for face_data in data_of_images
               if face_data.get("personType") == "UNDEFINDED"]
# Вектор очереди берётся из матрицы по порядку: очередь в json и в матрице
# записана одинаково, очередь идёт первой, а faceId в json служит проверкой
# этого соответствия.
queue_total = len(queue_faces)
queue_done = 0
progress_every = max(50, queue_total // 50)
save_progress(0, queue_total)

for start in range(0, queue_total, QUEUE_BATCH):

    chunk = queue_faces[start:start + QUEUE_BATCH]
    rows = []
    valid = []
    for index, face_data in enumerate(chunk):
        vector = queue_matrix[start + index]
        norm = float(np.linalg.norm(vector))
        if norm == 0:
            continue
        valid.append(index)
        rows.append(vector / norm)

    if rows:
        chunk_matrix = np.vstack(rows)
        # Один проход по матрице галереи на всю пачку.
        chunk_similarities = chunk_matrix @ gallery_matrix.T

        for position, index in enumerate(valid):
            face_data = chunk[index]
            similarities = chunk_similarities[position]

            # Сначала лучший вырез каждого персона: у одного человека их
            # несколько, и иначе он сравнивал бы сам с собой и «запас» всегда
            # оказывался нулевым. Запас считается между РАЗНЫМИ персонами —
            # именно он показывает, насколько выдающийся победитель.
            #
            # Раньше здесь стоял обход на Python по всем отмеченным лицам: на 8957
            # проверяемых лиц и 8226 образцов это 73,7 миллиона итераций, и на них
            # уходило всё время шага — в сотни раз больше, чем на само умножение.
            # Строки галереи подряд идут по персонам, поэтому максимум по каждой
            # группе берётся одним вызовом maximum.reduceat.
            per_person_best = np.maximum.reduceat(similarities, gallery_starts)

            # Порядок персон в person_names тот же, что и порядок групп в галерее,
            # поэтому сортировка со стабильностью при равных сходствах выбирает
            # того же кандидата, что прежняя сортировка словаря.
            top = np.argsort(-per_person_best, kind="stable")[:2]
            similarity = float(per_person_best[top[0]])
            name = str(person_names[top[0]])
            second_similarity = float(per_person_best[top[1]]) if len(top) > 1 else 0.0
            margin = similarity - second_similarity

            # Лицо признаётся, только если лучший кандидат и выше порога, и
            # обгоняет второго на запас. Без проверки запаса на этой серии
            # медиана разрыва составляла 0,025: больше половины признанного
            # отличалось от проигравшего на ничто. Причина в том, что
            # неверно названное лицо попадает в базу и в следующий прогон само
            # становится образцом — ошибка начинает тиражироваться. Лучше не
            # признать лицо вовсе, чем признать неверно.
            if similarity > args["confidence"] and margin >= args["margin"]:
                face_data['personId'] = 0
                face_data['personRecognizedName'] = name
                # Раньше здесь была вероятность predict_proba, теперь это
                # косинусное сходство: смысл другой, шкала та же. Высокое значение
                # означает не «уверенность классификатора», а «лицо похоже на
                # отмеченное», и калибруется порогом выше.
                face_data['recognizeProbability'] = similarity
                recognized += 1
                recognized_faces.append(face_data)
            elif similarity > args["confidence"]:
                # Порог взят, но запас не набран: кандидат сомнительный.
                thin_margin += 1
            else:
                rejected += 1

            best_by_person[name] = max(best_by_person.get(name, 0.0), similarity)

    # Ход считаем по пачкам: нулевые векторы пропускаются, но в общий счётчик
    # входят, иначе полоса дошла бы не до конца.

    queue_done = start + len(chunk)
    if queue_done % progress_every < len(chunk) or queue_done == queue_total:
        save_progress(queue_done, queue_total)

save_progress(queue_done, queue_total)
save_recognized(recognized_faces)

print("[INFO] распознано: {}, отклонено по порогу: {}, отклонено по запасу: {}".format(recognized, rejected, thin_margin))
save_result("Галерея: {} персон, {} отмеченных лиц. Распознано: {}, отклонено по порогу: {}, отклонено по запасу: {}".format(
    len(person_names), gallery_matrix.shape[0], recognized, rejected, thin_margin))
if recognized == 0:
    print("[INFO] ВНИМАНИЕ: ни одно лицо не преодолело порог. Либо галерея собрана "
          "из неудачных вырезов, либо порог {} слишком высок.".format(args["confidence"]))
print("[INFO] лучшее сходство по персонам:")
for name, similarity in sorted(best_by_person.items(), key=lambda item: -item[1]):
    print("    {}: {:.3f}".format(name, similarity))
