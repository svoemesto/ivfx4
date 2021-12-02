import sys
sys.path.append("../imutils")

import argparse
import os
import numpy as np
import cv2
from imutils import paths
import imutils
import os
import json


# Получаем и парсим аргументы
ap = argparse.ArgumentParser()
# ap.add_argument("-p", "--projectid", type=int, required=True, help="иденитификатор проекта")
# ap.add_argument("-f", "--fileid", type=int, required=True, help="иденитификатор видеофайла")
ap.add_argument("-i", "--dataset", required=True, help="путь к папке с изображениями, на которых надо найти лица")
ap.add_argument("-d", "--detector", required=True, help="path to OpenCV's deep learning face detector")
ap.add_argument("-m", "--embedding-model", required=True, help="path to OpenCV's deep learning face embedding model")
ap.add_argument("-c", "--confidence", type=float, default=0.5, help="minimum probability to filter weak detections")
args = vars(ap.parse_args())

# projectid = args['projectid']
# fileid = args['fileid']

# загружаем детектор лиц
print("[INFO] загружаем детектор лиц...")
protoPath = os.path.sep.join([args["detector"], "deploy.prototxt"])
modelPath = os.path.sep.join([args["detector"], "res10_300x300_ssd_iter_140000.caffemodel"])
detector = cv2.dnn.readNetFromCaffe(protoPath, modelPath)

# загружаем модель распознавания лиц
print("[INFO] загружаем модель распознавания лиц...")
embedder = cv2.dnn.readNetFromTorch(args["embedding_model"])

# получаем список файлов с изображениями из входящей папки
# print("[INFO] получаем список файлов с изображениями из входящей папки...")
# imagePaths = list(paths.list_images(args["dataset"]))

# получаем имя и путь к файлу json, в который будем заносить информацию о найденных лицах на фотографиях
data_file = os.path.sep.join([args["dataset"], "faces.json"])

# получаем имя и путь к файлу json, в который будем заносить информацию о найденных лицах на фотографиях
listframes_file = os.path.sep.join([args["dataset"], "frames.json"])

# считываем список фреймов
print("[INFO] loading frames list from json...")
listframes = json.loads(open(listframes_file, "rb").read())

# получаем путь к папке с лицами и создаем её
faces_path = args["dataset"] +".faces"
os.makedirs(name=faces_path,exist_ok=True)

data_faces = []

# every_frames = 10

# цикл по файлам изображений
for i in range(0, len(listframes)):
    frame_info = listframes[i]

    projectid = frame_info["projectid"]
    fileid = frame_info["fileid"]
    imagePath = frame_info["image_file"]
    frameId = frame_info["frame_id"]

# for (i, imagePath) in enumerate(imagePaths):

    # if i % every_frames != 0:
    #     continue

    print("[INFO] обработка изображения {}/{}".format(i + 1, len(listframes)))

    # получаем имя файла (без пути и расширения)
    name_file_wo_ext = imagePath.split(os.path.sep)[-1]
    name_file_wo_ext = name_file_wo_ext[0:len(name_file_wo_ext)-4]

    # загружаем изображение, изменяем его размер до ширины 600 пикселей (с сохранением соотношения сторон), а затем получаем размеры изображения
    image = cv2.imread(imagePath)
    # image = imutils.resize(image, width=600)
    image = imutils.resize(image, width=1920)
    (h, w) = image.shape[:2]

    # создаем blob из изображения
    imageBlob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0, (300, 300), (104.0, 177.0, 123.0), swapRB=False, crop=False)
    # imageBlob = cv2.dnn.blobFromImage(cv2.resize(image, (600, 600)), 1.0, (600, 600), (104.0, 177.0, 123.0), swapRB=False, crop=False)

    # применяем OpenCV's deep learning-based для обнаружения лиц на изображениях
    detector.setInput(imageBlob)
    detections = detector.forward()

    # инициализируем счетчик лиц на изображении
    face_count_in_image = 0

    # цикл по найденным лицам
    for i in range(0, detections.shape[2]):

        # получаем коэффициэнт достоверности
        confidence = detections[0, 0, i, 2]

        # если полученный коэффициэнт достоверности больше порогового коэффициэнта, переданного в аргументах
        if confidence > args["confidence"]:

            # получаем (x, y)-координаты прямоугольника вокруг лица
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            (startX, startY, endX, endY) = box.astype("int")

            # извлекаем лицо
            face = image[startY:endY, startX:endX]
            (fH, fW) = face.shape[:2]

            # если лицо меньше чем 20*20 пикселей - пропускаем его
            if fW < 20 or fH < 20:
                continue

            # лицо удовлетворяем условиям - значит инкримируем счетчик лиц
            face_count_in_image += 1

            # получаем имя файла для лица и сохраняем лицо в отдельном файле
            face_file_name = name_file_wo_ext + "_face_" + "{:02d}".format(face_count_in_image) + ".jpg"
            face_file_name_and_path = os.path.sep.join([faces_path, face_file_name])
            cv2.imwrite(face_file_name_and_path, face)

            # создаем blob для лица и создаем для него 128-мерный вектор
            faceBlob = cv2.dnn.blobFromImage(face, 1.0 / 255, (96, 96), (0, 0, 0), swapRB=True, crop=False)
            embedder.setInput(faceBlob)
            vec = embedder.forward()

            # print(list(map(str, vec.flatten().tolist())))

            # создаем объект для лица и записываем в него все нужные атрибуты
            face_data = {'fileid': fileid,
                         'projectid': projectid,
                         'image_file': imagePath,
                         'face_id': face_count_in_image,
                         'person_id': 0,
                         'person_id_recognized': 0,
                         'recognize_probability': 0.0,
                         'frame_id': frameId,
                         'face_file': face_file_name_and_path,
                         'start_x': int(startX),
                         'start_y': int(startY),
                         'end_x': int(endX),
                         'end_y': int(endY),
                         'vector': vec.flatten().tolist()
                         }
            data_faces.append(face_data)

with open(data_file, 'w') as file:
    json.dump(data_faces, file)
