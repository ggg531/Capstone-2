package com.example.foodtap.util

import android.content.Context
import android.util.Log
import com.example.foodtap.api.UserData // UserData import
import com.google.gson.Gson // Gson 라이브러리 사용 (build.gradle에 추가 필요)
import java.io.File
import java.io.IOException

object FileManager {
    private const val ID_FILE_NAME = "id.txt"
    private const val USER_PROFILE_FILE_NAME = "user_profile.json" // 사용자 전체 정보 저장 파일
    private val gson = Gson()

    // 기존 ID 생성/가져오기 로직은 유지
    fun getOrCreateId(context: Context): String {
        val file = File(context.filesDir, ID_FILE_NAME)
        return if (file.exists()) {
            val id = file.readText().trim()
            if (id.isEmpty()) { // ID 파일은 있지만 내용이 비어있는 경우
                Log.w("FileManager", "ID file exists but is empty. Generating new ID.")
                generateAndSaveId(file)
            } else {
                id
            }
        } else {
            generateAndSaveId(file)
        }
    }

    private fun generateAndSaveId(file: File): String {
        val newId = generateRandomId()
        try {
            file.writeText(newId)
        } catch (e: IOException) {
            Log.e("FileManager", "Error writing ID to file: ${e.message}", e)
        }
        return newId
    }

    private fun generateRandomId(): String {
        return (10000000..99999999).random().toString()
    }

    // UserData 저장 함수
    fun saveUserData(context: Context, userData: UserData) {
        val file = File(context.filesDir, USER_PROFILE_FILE_NAME)
        try {
            val userJson = gson.toJson(userData)
            file.writeText(userJson)
            Log.d("FileManager", "UserData saved to $USER_PROFILE_FILE_NAME: $userJson")
        } catch (e: IOException) {
            Log.e("FileManager", "Error saving UserData: ${e.message}", e)
        }
    }

    // UserData 불러오기 함수
    fun loadUserData(context: Context): UserData? {
        val file = File(context.filesDir, USER_PROFILE_FILE_NAME)
        if (!file.exists()) {
            Log.d("FileManager", "$USER_PROFILE_FILE_NAME does not exist.")
            return null
        }
        return try {
            val userJson = file.readText()
            if (userJson.isBlank()) {
                Log.w("FileManager", "$USER_PROFILE_FILE_NAME is empty.")
                null
            } else {
                val userData = gson.fromJson(userJson, UserData::class.java)
                Log.d("FileManager", "UserData loaded from $USER_PROFILE_FILE_NAME: $userData")
                userData
            }
        } catch (e: Exception) { // IOException 외 JsonSyntaxException 등 포함
            Log.e("FileManager", "Error loading UserData: ${e.message}", e)
            // 파일 내용이 손상되었을 수 있으므로, 파일을 삭제하거나 초기화하는 로직을 추가할 수 있습니다.
            // file.delete()
            null
        }
    }
}
