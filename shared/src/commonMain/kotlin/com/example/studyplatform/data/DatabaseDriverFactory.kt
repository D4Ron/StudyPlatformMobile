package com.example.studyplatform.data

import app.cash.sqldelight.db.SqlDriver
import com.example.studyplatform.db.StudyPlatformDatabase

/**
 * Opens the local database.
 *
 * Each platform supplies its own driver — Android needs a `Context`, iOS does not —
 * so this is the one piece of the data layer that cannot be shared. Everything above
 * it is common code.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

/** Single entry point so callers never build the schema themselves. */
object Database {
    fun create(factory: DatabaseDriverFactory): StudyPlatformDatabase =
        StudyPlatformDatabase(factory.create())
}
