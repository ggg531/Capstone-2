package com.example.foodtap.util

import android.content.Context
import java.io.File
import java.io.IOException

object FileManager {
    private const val FILE_NAME = "id.txt"
    private const val EXP_FILE = "user_exp.txt"
    private const val ALLERGY_FILE = "user_allergy.txt"

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

    // exp
    fun createUserExpIfNotExists(context: Context) {
        val file = File(context.filesDir, EXP_FILE)
        if (!file.exists()) {
            file.writeText("5")
        }
    }

    fun saveUserExp(context: Context, days: Int) {
        val file = File(context.filesDir, EXP_FILE)
        file.writeText(days.toString())
    }

    fun loadUserExp(context: Context): Int {
        val file = File(context.filesDir, EXP_FILE)
        return if (file.exists()) {
            file.readText().toIntOrNull() ?: 5
        } else 5
    }

    // allergy
    fun createUserAllergyIfNotExists(context: Context) {
        val file = File(context.filesDir, ALLERGY_FILE)
        if (!file.exists()) {
            file.writeText("")
        }
    }

    fun saveAllergyList(context: Context, allergyList: List<String>) {
        val file = File(context.filesDir, ALLERGY_FILE)
        file.writeText(allergyList.joinToString(","))
    }

    fun loadAllergyList(context: Context): List<String> {
        val file = File(context.filesDir, ALLERGY_FILE)
        return if (file.exists()) {
            file.readText().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
    }
}
