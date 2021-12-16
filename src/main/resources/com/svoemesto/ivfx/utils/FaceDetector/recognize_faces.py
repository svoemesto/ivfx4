# USAGE
# python recognize_faces.py -i G:\iGOT_Frames\GOT.S1E1.Frames.FullSize\faces.json -d face_detection_model -m openface_nn4.small2.v1.t7 -r output\recognizer.pickle -l output\le.pickle

import sys
sys.path.append("../imutils")

import json

import numpy as np
import argparse
import imutils
import pickle
import cv2
import os

# Получаем и парсим аргументы
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--inputjson", required=True, help="Путь к файлу json с описанием изображений")
ap.add_argument("-d", "--detector", required=True, help="path to OpenCV's deep learning face detector")
ap.add_argument("-m", "--embedding-model", required=True, help="path to OpenCV's deep learning face embedding model")
ap.add_argument("-r", "--recognizer", required=True, help="path to model trained to recognize faces")
ap.add_argument("-l", "--le", required=True, help="path to label encoder")
ap.add_argument("-c", "--confidence", type=float, default=0.5, help="minimum probability to filter weak detections")
args = vars(ap.parse_args())

# загружаем детектор лиц
print("[INFO] загружаем детектор лиц...")
protoPath = os.path.sep.join([args["detector"], "deploy.prototxt"])
modelPath = os.path.sep.join([args["detector"], "res10_300x300_ssd_iter_140000.caffemodel"])
detector = cv2.dnn.readNetFromCaffe(protoPath, modelPath)

# загружаем модель распознавания лиц
print("[INFO] загружаем модель распознавания лиц...")
embedder = cv2.dnn.readNetFromTorch(args["embedding_model"])

# загрузить актуальную модель распознавания лиц вместе с кодировщиком этикеток
recognizer = pickle.loads(open(args["recognizer"], "rb").read())
le = pickle.loads(open(args["le"], "rb").read())

# Загружаем данные об изображениях из json - это список объектов
file_json_images = args["inputjson"]
data_of_images = json.loads(open(file_json_images, "rb").read())

for face_data in data_of_images:

    person_type = face_data['personType']

    # если лицо "не обучено"
    if person_type == 'UNDEFINDED':
        print(face_data['pathToFaceFile'])

        vec_flat = np.array(face_data['vector'])
        vec = vec_flat.reshape(-1, 128)
        vec_flat2 = vec.flatten()

        # perform classification to recognize the face
        preds = recognizer.predict_proba(vec)[0]
        j = np.argmax(preds)
        proba = preds[j]
        name = le.classes_[j]

        face_data['personId'] = 0
        face_data['personRecognizedName'] = name
        face_data['recognizeProbability'] = proba

        print("{} - {}".format(name, proba))

with open(file_json_images, 'w') as file:
    json.dump(data_of_images, file)