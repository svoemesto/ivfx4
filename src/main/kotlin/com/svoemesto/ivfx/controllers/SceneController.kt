package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.SceneExt
import com.svoemesto.ivfx.modelsext.ShotExt
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class SceneController() {

    companion object {

        fun getProperties(scene: Scene) : List<Property> {
            return Main.propertyRepo.findByParentClassAndParentId(scene::class.simpleName!!, scene.id).toList()
        }

        fun getSetScenes(file: File): MutableSet<Scene> {
            val result = Main.sceneRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(file.id,0).toMutableSet()
            result.forEach { shot ->
                shot.file = file
            }
            return result
        }

        fun getPropertyValue(scene: Scene, key: String) : String {
            val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(scene::class.simpleName!!, scene.id, key).firstOrNull()
            return property?.value ?: ""
        }

        fun isPropertyPresent(scene: Scene, key: String) : Boolean {
            return Main.propertyRepo.findByParentClassAndParentIdAndKey(scene::class.simpleName!!, scene.id, key).any()
        }

        fun save(scene: Scene) {
            Main.sceneRepo.save(scene)
        }

        fun saveAll(scene: Iterable<Scene>) {
            Main.sceneRepo.saveAll(scene)
        }

        fun delete(scene: Scene) {
            PropertyController.deleteAll(scene::class.java.simpleName, scene.id)
            PropertyCdfController.deleteAll(scene::class.java.simpleName, scene.id)
            TagController.deleteAll(scene::class.java.simpleName, scene.id)
            Main.sceneRepo.delete(scene)
        }

        fun deleteAll(file: File) {
            getSetScenes(file).forEach { scene ->
                PropertyController.deleteAll(scene::class.java.simpleName, scene.id)
                PropertyCdfController.deleteAll(scene::class.java.simpleName, scene.id)
                TagController.deleteAll(scene::class.java.simpleName, scene.id)
            }
            Main.sceneRepo.deleteAll(file.id)
        }

        fun getOrCreate(file: File, firstFrameNumber: Int, lastFrameNumber: Int): Scene {
            var entity = Main.sceneRepo.findByFileIdAndFirstFrameNumberAndLastFrameNumber(file.id, firstFrameNumber, lastFrameNumber).firstOrNull()
            if (entity == null) {
                entity = Scene()
                entity.file = file
                entity.firstFrameNumber = firstFrameNumber
                entity.lastFrameNumber = lastFrameNumber
                entity.name = "Scene $firstFrameNumber-$lastFrameNumber"
                save(entity)
            } else {
                entity.file = file
            }
            return entity
        }

        fun createSceneExt(listShotsExt: MutableList<ShotExt>): SceneExt? {
            if (listShotsExt.isNotEmpty()) {
                val scene = getOrCreate(listShotsExt.first().fileExt.file, listShotsExt.first().shot.firstFrameNumber, listShotsExt.last().shot.lastFrameNumber)
                return SceneExt(scene,listShotsExt.first().fileExt,listShotsExt.first().firstFrameExt, listShotsExt.last().lastFrameExt)
            }
            return null
        }

    }


}