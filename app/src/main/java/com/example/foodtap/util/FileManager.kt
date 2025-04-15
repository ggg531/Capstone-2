package com.example.foodtap.util

import android.content.Context
import java.io.File
import java.io.IOException

object FileManager {
    private const val FILE_NAME = "id.txt"

    fun getOrCreateId(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)

        return if (file.exists()) {
            file.readText().trim()  // 기존 ID 읽기
        } else {
            val newId = generateRandomId()
            try {
                file.writeText(newId)  // 파일에 ID 저장
            } catch (e: IOException) {
                e.printStackTrace()
            }
            newId
        }
    }

    private fun generateRandomId(): String {
        return (10000000..99999999).random().toString()  // 8자리 숫자 생성
    }
}
