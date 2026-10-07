import sys
sys.path.append("../imutils")

import argparse
import os
import numpy as np
from PIL import Image
import json

# Библиотеки CUDA лежат внутри окружения, в каталогах nvidia/*/lib, и в
# пути поиска динамических библиотек не входят. Без них onnxruntime не
# находит libcublas и МОЛЧА уходит на процессор: ошибка есть, а прогоны
# идут и выглядят как успешные. Поэтому путь дополняем до импорта
# onnxruntime, а работает это только если переменная выставлена до
# загрузки библиотек.
try:
    import site
    import glob as _glob
    _sp = site.getsitepackages()[0]
    _libs = sorted(_glob.glob(os.path.join(_sp, "nvidia", "*", "lib")))
    if _libs:
        os.environ["LD_LIBRARY_PATH"] = ":".join(_libs + [os.environ.get("LD_LIBRARY_PATH", "")])
except Exception:
    pass


# Получаем и парсим аргументы
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--dataset", required=True, help="путь к папке с изображениями, на которых надо найти лица")
ap.add_argument("-o", "--output", required=True, help="путь к папке, в которую будут складываться лица")
ap.add_argument("-d", "--detector", required=True, help="путь к папке с моделью детектора")
ap.add_argument("-m", "--embedding-model", required=True, help="путь к модели распознавания лиц")
ap.add_argument("-l", "--list", default="", help="файл со списком кадров в формате frames.json. Пусто — берём frames.json из папки")
ap.add_argument("-f", "--faces-file", default="faces.json", help="имя файла с найденными лицами в папке кадров")
ap.add_argument("-c", "--confidence", type=float, default=0.5, help="минимальная достоверность")
args = vars(ap.parse_args())

# Детектор и распознаватель — модели InsightFace, обе работают через
# onnxruntime. OpenCV здесь больше не нужен: чтение кадров идёт через
# Pillow, выравнивание лица по пяти точкам — штатной функцией
# insightface, а не пиксельной арифметикой.
#
# Что было раньше и почему так больше не делаем:
#   детектор   — res10_300x300_ssd в формате Caffe, на кадрах 1920x1080
#                давал достоверность около 0,12 и не находил ни одного лица
#                при пороге 0,3. Сейчас это SCRFD-10G.
#   распознавание — openface_nn4 в формате TorchScript, грузился только
#                через cv2.dnn.readNetFromTorch, которого нет в OpenCV 5.
#                Сейчас это ArcFace R50, и он заметно сильнее.
#
# Размерность вектора при этом меняется со 128 на 512. В базе это колонка
# text, сторона на Kotlin ничего о размере не предполагает, таблица пуста —
# менять схему не нужно.

print("[INFO] загружаем детектор лиц...")
import onnxruntime as ort
from insightface.model_zoo import get_model
from insightface.utils import face_align

# Видеокарта используется, если она есть и библиотеки нашлись. Без неё
# работа идёт на процессоре и лишь медленнее, результат тот же.
USERS_GPU = "CUDAExecutionProvider" in ort.get_available_providers()
PROVIDERS = ["CUDAExecutionProvider", "CPUExecutionProvider"] if USERS_GPU else ["CPUExecutionProvider"]


def load_model(model_path, **prepare_args):
    """Создаёт модель и, если есть видеокарта, пересаживает её на неё.

    Порядок важен: prepare() сам пересоздаёт сессию на процессоре, поэтому
    свою сессию с CUDA надо подставлять ПОСЛЕ prepare. Иначе модель тихо
    работает на процессоре, и на замере это выглядит как «ускорили, но
    чуть-чуть».
    """
    model = get_model(model_path)
    model.prepare(**prepare_args)
    if USERS_GPU:
        options = ort.SessionOptions()
        model.session = ort.InferenceSession(model_path, options, providers=PROVIDERS)
    actual = model.session.get_providers()[0]
    print("[INFO]   {}: {}".format(os.path.basename(model_path), actual))
    return model


print("[INFO] считаем на:", "видеокарте" if USERS_GPU else "процессоре")
detector = load_model(os.path.sep.join([args["detector"], "det_10g.onnx"]),
                      ctx_id=-1, input_size=(640, 640), det_thresh=args["confidence"])
recognizer = load_model(args["embedding_model"], ctx_id=-1)


def read_frame(path):
    """Читает кадр целиком, без уменьшения.

    Уменьшение вдвое через draft() проверялось как ускорение: кадр 1920x1080
    читается вчетверо дешевле, и весь прогон укладывается в 3 минуты
    вместо 6. Но платит за это слишком дорого — на полном прогоне теряется
    1040 лиц из 12518, то есть 8,3%. Пропущенное лицо обходится в
    повторный прогон по всей серии, лишнее удаляется одним щелчком, так
    что экономия трёх минут того не стоит.

    Чтение идёт через Pillow, а не imageio: на том же камере 6,3 мс против
    8,4, а картинка та же, до пикселя.
    """
    return np.asarray(Image.open(path).convert("RGB"))

# Куда писать найденные лица. Имя задаётся отдельно не просто для удобства:
# обычный прогон и перепроверка обязаны писать в разные файлы. Иначе
# перепроверка затрёт faces.json, а следующий за ней шаг создания лиц
# вернул бы в базу все вручную размеченные лица как неопределённые — молча,
# без всякой ошибки.
data_file = os.path.sep.join([args["dataset"], args["faces_file"]])

# Список кадров в том же формате, что frames.json: у записи кадра нужны не
# только номер, но ещё проект, файл и путь к изображению. Список из одних
# номеров не годится и раньше здесь молча падал на отсутствующем ключе.
listframes_file = args["list"] if args["list"] else os.path.sep.join([args["dataset"], "frames.json"])
print("[INFO] loading frames list from {}...".format(listframes_file))
listframes = json.loads(open(listframes_file, "rb").read())

# получаем путь к папке с лицами и создаем её
faces_path = args["output"]
os.makedirs(name=faces_path, exist_ok=True)

data_faces = []
# Кадры, которых нет на диске. Список кадров берётся из базы, а пишут
# файлы кадров другим шагом, и на границе они расходятся: база знает на
# один кадр больше, чем лежит в папке. Такой кадр пропускаем, а не роняем
# прогон.
missing_frames = 0

# Файл прогресса. Приложение опрашивает его, пока идёт обработка, и
# показывает в форме реальное число обработанных кадров. На серии в
# 88643 кадра это единственный способ отличить работу от зависания: без
# него полоса крутится вхолостую ни о чём не говоря.
# Формат: "<обработано> <всего> <лиц найдено>" одной строкой.
progress_file = os.path.sep.join([args["dataset"], "detect_faces_progress.txt"])
if os.path.exists(progress_file):
    os.remove(progress_file)


def save_progress(done, total, faces):
    with open(progress_file, "w") as file:
        file.write("{} {} {}".format(done, total, faces))


save_progress(0, len(listframes), 0)

# цикл по кадрам
for i in range(0, len(listframes)):
    frame_info = listframes[i]

    projectid = frame_info["projectId"]
    fileid = frame_info["fileId"]
    imagePath = frame_info["pathToFrameFile"]
    frameNumber = frame_info["frameNumber"]

    # Отпечатываем не каждый кадр: на полной серии их 88643, и построчный
    # вывод забивает собой журнал приложения. В файл прогресса пишем
    # чаще — он маленький, зато полоса в форме движется ровно.
    if i % 100 == 0:
        print("[INFO] обработка изображения {}/{}".format(i + 1, len(listframes)))
    if i % 20 == 0:
        save_progress(i + 1, len(listframes), len(data_faces))

    # получаем имя файла (без пути и расширения)
    name_file_wo_ext = imagePath.split(os.path.sep)[-1]
    name_file_wo_ext = name_file_wo_ext[0:len(name_file_wo_ext) - 4]

    # Кадр читаем целиком. Уменьшение вдвое проверялось ради скорости и
    # отменено: на полном прогоне терялось 8,3% лиц, а выигрыш в минуту
    # с половиной того не стоит. Подробности в read_frame.
    try:
        image = read_frame(imagePath)
    except (FileNotFoundError, OSError) as read_err:
        missing_frames += 1
        if missing_frames <= 5:
            print("[WARNING] кадра нет на диске, пропускаем: {} ({})".format(imagePath, read_err))
        elif missing_frames == 6:
            print("[WARNING] далее такие кадры не печатаются")
        continue
    (h, w) = image.shape[:2]

    # применяем детектор: возвращает прямоугольники и пять ключевых точек
    bboxes, kps = detector.detect(image, max_num=0)

    # инициализируем счетчик лиц на изображении
    face_count_in_image = 0

    # цикл по найденным лицам
    for face_index in range(0, len(bboxes)):

        confidence = float(bboxes[face_index][4])

        if confidence > args["confidence"]:

            (startX, startY, endX, endY) = bboxes[face_index][0:4].astype("int")
            startX = max(0, startX)
            startY = max(0, startY)
            endX = min(w, endX)
            endY = min(h, endY)
            if endX - startX < 20 or endY - startY < 20:
                continue

            face_count_in_image += 1

            # сохраняем вырез по прямоугольнику: он идёт в просмотр лиц, и
            # видеть там нужно исходный кадр, а не выровненный квадрат
            face = image[startY:endY, startX:endX]
            face_file_name = name_file_wo_ext + "_face_" + "{:02d}".format(face_count_in_image) + ".jpg"
            face_file_name_and_path = os.path.sep.join([faces_path, face_file_name])
            Image.fromarray(face).save(face_file_name_and_path, quality=95)

            # Вектор считаем по выровненному лицу. ArcFace обучен именно на
            # таких вырезах, и точки даёт тот же детектор; если взять простой
            # прямоугольник, качество распознавания заметно падает.
            aligned = face_align.norm_crop(image, kps[face_index], image_size=112)
            vec = np.asarray(recognizer.get_feat(aligned)).reshape(-1)

            face_data = {'projectid': projectid,
                         'frameId': 0,
                         'fileId': fileid,
                         'personId': 0,
                         'frameNumber': frameNumber,
                         'faceNumberInFrame': face_count_in_image,
                         'pathToFrameFile': imagePath,
                         'pathToFaceFile': face_file_name_and_path,
                         'personRecognizedName': '',
                         'recognizeProbability': 0.0,
                         'startX': int(startX),
                         'startY': int(startY),
                         'endX': int(endX),
                         'endY': int(endY),
                         'vector': vec.tolist()
                         }
            data_faces.append(face_data)

save_progress(len(listframes), len(listframes), len(data_faces))
with open(data_file, 'w') as file:
    json.dump(data_faces, file)
print("[INFO] готово, найдено лиц: {}".format(len(data_faces)))
if missing_frames > 0:
    print("[WARNING] пропущено кадров без файла: {} из {}".format(
        missing_frames, len(listframes)))
    print("[WARNING] список кадров и файлы на диске расходятся: "
          "в базе кадров больше, чем в папке. Лиц с пропущенных кадров не будет.")
