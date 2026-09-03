package com.example.studyplatform.data

import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.db.StudyPlatformDatabase

/**
 * The one place the local database is opened.
 *
 * Deliberately a singleton: SQLite is happiest with a single connection per file, and a
 * second database instance would give the UI and the background sync worker separate
 * views of the same rows — which shows up as a note that reappears after being deleted.
 *
 * `init` is called once, from the platform's entry point, because only there is the
 * platform driver available.
 */
object AppData {

    private var database: StudyPlatformDatabase? = null

    fun init(factory: DatabaseDriverFactory) {
        if (database == null) {
            database = Database.create(factory)
        }
    }

    val db: StudyPlatformDatabase
        get() = database
            ?: error("AppData.init() must be called before the database is used.")

    /** Two-way sync: things the student creates on the device. */
    val notes: NoteRepository by lazy { NoteRepository(db) }
    val study: StudyRepository by lazy { StudyRepository(db) }
    val attempts: AttemptRepository by lazy { AttemptRepository(db) }

    /** Read-through cache: things the server owns and the device only reads. */
    val cache: CacheStore by lazy { CacheStore(db) }
    val library: LibraryRepository by lazy { LibraryRepository(cache) }

    val syncEngine: SyncEngine by lazy { SyncEngine(db) }

    val isReady: Boolean get() = database != null

    /**
     * Signs out and leaves nothing behind.
     *
     * Clearing the tokens is not enough: the cache holds this person's guides, notes and
     * results, and on a phone shared between siblings or handed round a classroom the
     * next person would find all of it. Anything still queued in the outbox goes too —
     * it can no longer be uploaded, since the account it belonged to is gone from this
     * device.
     */
    suspend fun signOut() {
        if (isReady) cache.clearAll()
        ApiClient.clearAll()
    }
}
