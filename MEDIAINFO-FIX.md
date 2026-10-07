# MediaInfo: windows-заглушка вместо рабочей утилиты

## Что было

При добавлении файла приложение падало:

```
java.io.IOException: Cannot run program "file:/home/nsa/ivfx4/target/ivfx-1.0-SNAPSHOT.jar!/com/svoemesto/ivfx/utils/MediaInfo_CLI/MediaInfo.exe"
    at java.lang.ProcessBuilder.start
    at com.svoemesto.ivfx.utils.MediaInfo.executeMediaInfo(MediaInfo.kt:55)
    at com.svoemesto.ivfx.controllers.TrackController.createTracksFromMediaInfo
```

Две разные поломки в одной строке кода:

1. В ресурсах лежит `MediaInfo.exe` и рядом `LIBCURL.DLL` — поставка для
   Windows. На Linux такой файл запустить нельзя в принципе.
2. Путь брался через `getResource().path`. При запуске из jar-а это не путь
   к файлу, а строка `file:/...jar!/...`, которую ProcessBuilder не
   принимает. То есть поломка была и на Windows, если запускать из jar.

## Что сделано

Поиск утилиты в три шага: системная в PATH, затем файл рядом с
приложением, затем встроенный ресурс с корректным разбором URI. Если
утилиты нет — понятное сообщение вместо IOException из ProcessBuilder.

Проверено: `/usr/bin/mediainfo` версии 24.01 на файле серии отдаёт JSON,
6198 символов, формат тот же, что запрашивает приложение (`--Output=JSON`).
