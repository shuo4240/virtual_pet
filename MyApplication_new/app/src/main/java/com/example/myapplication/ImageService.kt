package com.example.myapplication

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageService(private val context: Context) {

    companion object {
//        val drawableAnimalImages = listOf(
//            R.drawable.book_0, R.drawable.book_1, R.drawable.book_2, R.drawable.book_3, R.drawable.book_4,R.drawable.book_advance_1,R.drawable.book_advance_2,R.drawable.book_advance_3,R.drawable.book_advance_4,R.drawable.book_advance_5,R.drawable.book_advance_6,
//            R.drawable.cat0, R.drawable.cat1, R.drawable.cat2, R.drawable.cat3, R.drawable.cat4,
//            R.drawable.dog0, R.drawable.dog1, R.drawable.dog2, R.drawable.dog3, R.drawable.dog4,
//            R.drawable.rabbit0, R.drawable.rabbit1, R.drawable.rabbit2, R.drawable.rabbit3, R.drawable.rabbit4
//        )

        fun getResImageResourceIds(animal: String): List<Int> {
            return when (animal) {
                "bear" -> listOf(R.drawable.bear0, R.drawable.bear1, R.drawable.bear2, R.drawable.bear3, R.drawable.bear4)
                "cat" -> listOf(R.drawable.cat0, R.drawable.cat1, R.drawable.cat2, R.drawable.cat3, R.drawable.cat4)
                "dog" -> listOf(R.drawable.dog0, R.drawable.dog1, R.drawable.dog2, R.drawable.dog3, R.drawable.dog4)
                "rabbit" -> listOf(R.drawable.rabbit0, R.drawable.rabbit1, R.drawable.rabbit2, R.drawable.rabbit3, R.drawable.rabbit4)
                else -> emptyList()
            }
        }
    }

    suspend fun getAnimalImages(): List<String> = withContext(Dispatchers.IO) {
        val animalDir = getAnimalDirectory()
        if (!animalDir.exists()) {
            animalDir.mkdirs()
        }

        val files = animalDir.listFiles { file ->
            file.isFile && file.name.lowercase().endsWith(".png")
        }?.toList() ?: emptyList()

        return@withContext files.sortedBy { it.name }.map { it.absolutePath }
    }

    suspend fun updateImage(imageIndex: Int, newImagePath: String): String = withContext(Dispatchers.IO) {
        val animalDir = getAnimalDirectory()
        if (!animalDir.exists()) {
            animalDir.mkdirs()
        }

        val fileName = "image$imageIndex.png"
        val localFile = File(animalDir, fileName)

        if (localFile.exists()) {
            localFile.delete()
        }

        try {
            val assetPath = newImagePath.removePrefix("assets/")
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw IOException("Failed to copy image: $newImagePath", e)
        }

        return@withContext localFile.absolutePath
    }

    private fun getAnimalDirectory(): File {
        return File(context.filesDir, "animals")
    }

    suspend fun getAllImagePaths(): List<String> = withContext(Dispatchers.IO) {
        val animalDir = getAnimalDirectory()
        if (!animalDir.exists()) return@withContext emptyList()

        val files = animalDir.listFiles { file ->
            file.isFile && file.name.lowercase().endsWith(".png")
        }?.toList() ?: emptyList()

        return@withContext files.sortedBy { it.name }.map { it.absolutePath }
    }

    suspend fun clearAllImages(): Boolean = withContext(Dispatchers.IO) {
        val animalDir = getAnimalDirectory()
        return@withContext animalDir.deleteRecursively()
    }
}
