package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Tag
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class SceneExt {
    var tag: Tag? = null
    var fileExt: FileExt? = null
    var shotsExt: ObservableList<ShotExt> = FXCollections.observableArrayList()
}