package com.example.studyplatform.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.studyplatform.db.StudyPlatformDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(StudyPlatformDatabase.Schema, context, "studyplatform.db")
}
