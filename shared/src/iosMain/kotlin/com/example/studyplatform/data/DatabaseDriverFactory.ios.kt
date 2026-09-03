package com.example.studyplatform.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.studyplatform.db.StudyPlatformDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(StudyPlatformDatabase.Schema, "studyplatform.db")
}
