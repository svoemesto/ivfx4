package com.svoemesto.ivfx.enums

enum class Folders(val propertyCdfKey: String, val folderName: String, val forProject: Boolean, val forFile: Boolean) {
    LOSSLESS("folder_lossless", "Lossless", true, true),
    PREVIEW("folder_preview", "Preview", true, true),
    SHOTS("folder_shots", "Shots", true, true),
    FAVORITES("folder_favorites", "Favorites", true, true),
    FRAMES_SMALL("folder_frames_small", "Frames_Small", true, true),
    FRAMES_MEDIUM("folder_frames_medium", "Frames_Medium", true, true),
    FRAMES_FULL("folder_frames_full", "Frames_Full", true, true),
    FACES_FULL("folder_faces_full", "Faces_Full", true, true),
    FACES_PREVIEW("folder_faces_preview", "Faces_Preview", true, true),
    PERSONS("folder_persons", "Persons", true, false),
    SHOTS_COMPRESSED_WITH_AUDIO("folder_shots_compressed_with_audio", "Shots", true, true),
    SHOTS_LOSSLESS_WITH_AUDIO("folder_shots_lossless_with_audio", "Shots_LL_audioYES", true, true),
    SHOTS_LOSSLESS_WITHOUT_AUDIO("folder_shots_lossless_without_audio", "Shots_LL_audioNO", true, true),
    CONCAT("folder_concat", "Concat", true, true),
    FILTERS("folder_filters", "Filters", true, false)
}