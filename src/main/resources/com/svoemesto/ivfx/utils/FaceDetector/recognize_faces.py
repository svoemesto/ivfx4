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
result_file = os.path.sep.join([os.path.dirname(args["inputjson"]),
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
result_json_file = os.path.sep.join([os.path.dirname(args["inputjson"]),
                                     "recognize_faces_result.json"])


def save_result(text):
    with open(result_file, "w") as file:
        file.write(text)


def save_recognized(faces):
    with open(result_json_file, "w") as file:
        json.dump(faces, file)

# Загружаем данные об изображениях из json - это список объектов
file_json_images = args["inputjson"]
with open(file_json_images, "rb") as file:
    data_of_images = json.loads(file.read())

# Собираем галерею: все отмеченные лица, сгруппированные по имени.
# Берём лицо с меткой PERSON и непустым именем — это то, что владелец
# отметил вручную; остальные типы (массовка, не персонаж) для галереи
# не годятся.
gallery = {}
for face_data in data_of_images:
    if face_data.get("personType") != "PERSON":
        continue
    name = face_data.get("personRecognizedName", "")
    if not name:
        continue
    vector = np.asarray(face_data["vector"], dtype=np.float32)
    if vector.size == 0:
        continue
    gallery.setdefault(name, []).append(vector)

if not gallery:
    save_result("ГАЛЕРЕЯ ПУСТА: нет ни одного отмеченного лица. Распознавать нечего — "
                "сначала отметьте лица в монтажной.")
    save_recognized([])
    print("[INFO] галерея пуста: нет ни одного лица, отмеченного как PERSON с именем. "
          "Распознавать нечего — сначала отметьте лица в интерфейсе.")
    raise SystemExit(0)

# Нормализуем вектора один раз: косинус угла для нормализованных векторов
# считается обычным скалярным произведением, и на 512 значениях это дешевле
# и точнее, чем каждый раз делить на длины.
gallery_names = []
gallery_vectors = []
# Начало каждой персоны в матрице. Строки идут подряд, персона за персоной,
# потому что словарь gallery сохраняет порядок добавления, а обход идёт
# именно по нему. Эти начала нужны, чтобы брать лучшее сходство по персонам
# одним вызовом maximum.reduceat, без обхода на Python.
group_starts = []
person_names = list(gallery.keys())
for name, vectors in gallery.items():
    matrix = np.vstack(vectors)
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    matrix = matrix / norms
    group_starts.append(len(gallery_vectors))
    for row in matrix:
        gallery_names.append(name)
        gallery_vectors.append(row)

gallery_matrix = np.vstack(gallery_vectors)
gallery_names = np.array(gallery_names)
group_starts = np.array(group_starts, dtype=np.int64)
person_names = np.array(person_names)

print("[INFO] галерея: {} персон, {} отмеченных лиц".format(
    len(gallery), gallery_matrix.shape[0]))
print("[INFO] порог сходства: {}, запас: {}".format(args["confidence"], args["margin"]))

# У одного человека бывает несколько отмеченных лиц. Берём лучшее совпадение
# по всем его вырезам: один удачный кадр не должен топить результат.

recognized = 0
rejected = 0
thin_margin = 0
best_by_person = {}

recognized_faces = []

for face_data in data_of_images:

    person_type = face_data.get("personType")

    # распознаём только те лица, которые ещё не определены
    if person_type == "UNDEFINDED":

        vector = np.asarray(face_data["vector"], dtype=np.float32)
        norm = float(np.linalg.norm(vector))
        if norm == 0:
            continue
        vector = vector / norm

        similarities = gallery_matrix @ vector

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
        per_person_best = np.maximum.reduceat(similarities, group_starts)

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

save_recognized(recognized_faces)

print("[INFO] распознано: {}, отклонено по порогу: {}, отклонено по запасу: {}".format(recognized, rejected, thin_margin))
save_result("Галерея: {} персон, {} отмеченных лиц. Распознано: {}, отклонено по порогу: {}, отклонено по запасу: {}".format(
    len(gallery), gallery_matrix.shape[0], recognized, rejected, thin_margin))
if recognized == 0:
    print("[INFO] ВНИМАНИЕ: ни одно лицо не преодолело порог. Либо галерея собрана "
          "из неудачных вырезов, либо порог {} слишком высок.".format(args["confidence"]))
print("[INFO] лучшее сходство по персонам:")
for name, similarity in sorted(best_by_person.items(), key=lambda item: -item[1]):
    print("    {}: {:.3f}".format(name, similarity))
