package com.example.studyplatform.data

import com.example.studyplatform.api.AppJson
import com.example.studyplatform.db.StudyPlatformDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * What a read returned, and where it came from.
 *
 * The source is part of the answer, not a detail. A student looking at a stale guide
 * list needs to know it is stale — silently showing old data as though it were current
 * is how an offline app loses someone's trust.
 */
data class Offline<T>(
    val value: T,
    val fromCache: Boolean = false,
    /** When the cached copy was taken. Null when this came straight from the server. */
    val cachedAt: String? = null,
    /** Set when the network failed *and* there was nothing cached to fall back on. */
    val error: String? = null
) {
    val isEmpty: Boolean get() = value is Collection<*> && value.isEmpty()
}

/**
 * Read-through cache for everything the server owns.
 *
 * Guides, quizzes and explanations are generated server-side and the device only ever
 * reads them, so they need a cache rather than a sync: there is no local edit that
 * could conflict, and no outbox entry to drain. That is a genuinely different problem
 * from notes, and conflating the two would mean carrying conflict machinery for data
 * that can never have a conflict.
 *
 * Payloads are stored as the JSON the API returned. One table serves every screen, and
 * a new screen needs no migration — the shape is already defined by the model that will
 * parse it back.
 */
class CacheStore(private val db: StudyPlatformDatabase) {

    private val queries = db.studyPlatformQueries
    private val json: Json get() = AppJson.instance

    /**
     * Fetches a list, falling back to the last copy seen.
     *
     * @param key      identifies this list, e.g. `guides` or `notes:group:<id>`
     * @param idOf     how to find an item's id, so items can be stored individually and
     *                 a detail screen can read one without the list having to be fetched
     * @param fetch    the network call; anything it throws becomes a cache read
     */
    suspend fun <T : Any> list(
        key: String,
        entityType: String,
        serializer: KSerializer<T>,
        idOf: (T) -> String,
        fetch: suspend () -> List<T>
    ): Offline<List<T>> = withContext(Dispatchers.Default) {
        try {
            val fresh = fetch()
            val now = Clock.System.now().toString()
            db.transaction {
                fresh.forEach {
                    queries.upsertDocument(
                        entity_type = entityType,
                        entity_id = idOf(it),
                        payload = json.encodeToString(serializer, it),
                        cached_at = now
                    )
                }
                queries.upsertCollection(
                    key = key,
                    ids = json.encodeToString(
                        ListSerializer(String.serializer()),
                        fresh.map(idOf)
                    ),
                    cached_at = now
                )
                evictIfOversized()
            }
            Offline(fresh)
        } catch (e: Exception) {
            println("Falling back to cache for '$key': ${e.message}")
            readList(key, entityType, serializer)
                ?: Offline(emptyList(), fromCache = false, error = friendly(e))
        }
    }

    /** Fetches one record, falling back to the last copy seen. */
    suspend fun <T : Any> one(
        entityType: String,
        id: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T
    ): Offline<T?> = withContext(Dispatchers.Default) {
        try {
            val fresh = fetch()
            queries.upsertDocument(
                entity_type = entityType,
                entity_id = id,
                payload = json.encodeToString(serializer, fresh),
                cached_at = Clock.System.now().toString()
            )
            evictIfOversized()
            Offline(fresh)
        } catch (e: Exception) {
            println("Falling back to cache for $entityType/$id: ${e.message}")
            val row = queries.selectDocument(entityType, id).executeAsOneOrNull()
            if (row == null) {
                Offline(null, error = friendly(e))
            } else {
                Offline(
                    decode(row.payload, serializer),
                    fromCache = true,
                    cachedAt = row.cached_at
                )
            }
        }
    }

    /**
     * A single value that is not a record — a stats blob, a level, a badge list.
     *
     * Keyed by a name rather than an id, because there is only ever one of it per user.
     */
    suspend fun <T : Any> singleton(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T
    ): Offline<T?> = one("singleton", key, serializer, fetch)

    /** The cached list, or null if this list has never been fetched on this device. */
    private fun <T : Any> readList(
        key: String,
        entityType: String,
        serializer: KSerializer<T>
    ): Offline<List<T>>? {
        val collection = queries.selectCollection(key).executeAsOneOrNull() ?: return null
        val ids: List<String> = try {
            json.decodeFromString(
                ListSerializer(String.serializer()),
                collection.ids
            )
        } catch (_: Exception) {
            return null
        }

        // A document missing from the cache is skipped rather than failing the read: a
        // partial list is far more useful than an error page.
        val items = ids.mapNotNull { id ->
            queries.selectDocument(entityType, id).executeAsOneOrNull()
                ?.let { decode(it.payload, serializer) }
        }
        return Offline(items, fromCache = true, cachedAt = collection.cached_at)
    }

    private fun <T : Any> decode(payload: String, serializer: KSerializer<T>): T? =
        try {
            json.decodeFromString(serializer, payload)
        } catch (e: Exception) {
            // A payload written by an older build of the app. Dropping it is right —
            // the next successful fetch replaces it.
            println("Discarding unreadable cached payload: ${e.message}")
            null
        }

    private fun friendly(e: Exception): String =
        e.message ?: "You're offline and this hasn't been downloaded yet."

    /**
     * Keeps the cache from growing for the life of the install.
     *
     * Without this it never shrinks: every guide, quiz and course ever opened, kept
     * forever on a phone that may have 8GB in total. Filling a student's storage with
     * things they read once is the opposite of helping them.
     *
     * Oldest-touched go first. What you read last month is what you are least likely to
     * want tonight with no signal, and `cached_at` already records it.
     *
     * Runs after a successful fetch rather than on a timer — that is the only moment the
     * cache grows, so it is the only moment it can need trimming.
     */
    private fun evictIfOversized() {
        val documents = queries.countDocuments().executeAsOne()
        if (documents > MAX_DOCUMENTS) {
            val excess = documents - TRIM_DOCUMENTS_TO
            queries.deleteOldestDocuments(excess)
            println("Cache trimmed: dropped $excess of $documents documents")
        }

        // Collections are tiny, but a list whose documents were evicted would resolve
        // to a shorter list than it claims. Trimming them together keeps the two in
        // step; the next fetch rebuilds whatever is still wanted.
        val collections = queries.countCollections().executeAsOne()
        if (collections > MAX_COLLECTIONS) {
            queries.deleteOldestCollections(collections - TRIM_COLLECTIONS_TO)
        }
    }

    companion object {
        /**
         * Chosen for a cheap phone, not a flagship. A cached document is one API
         * response — a guide with its full content is the largest, at a few hundred KB —
         * so this is a ceiling in the low tens of megabytes, not gigabytes.
         */
        private const val MAX_DOCUMENTS = 400L

        /** Trimmed well below the ceiling so eviction runs rarely, not on every fetch. */
        private const val TRIM_DOCUMENTS_TO = 300L

        private const val MAX_COLLECTIONS = 60L
        private const val TRIM_COLLECTIONS_TO = 40L
    }

    /**
     * Wipes everything this device holds.
     *
     * Called on sign-out. The database is one person's work, and the next person to open
     * the app on a shared phone must not find it — which on a phone shared between
     * siblings or a school's device is not a hypothetical.
     */
    suspend fun clearAll() = withContext(Dispatchers.Default) {
        db.transaction {
            queries.clearDocuments()
            queries.clearCollections()
            queries.clearNotes()
            queries.clearTopics()
            queries.clearSessions()
            queries.clearAttempts()
            queries.clearOutbox()
            queries.clearSyncState()
        }
    }
}
