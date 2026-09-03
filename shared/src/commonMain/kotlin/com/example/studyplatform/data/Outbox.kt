package com.example.studyplatform.data

import com.example.studyplatform.db.StudyPlatformQueries
import kotlin.random.Random

/**
 * Appends local changes to the durable queue the sync engine drains.
 *
 * Enqueuing happens inside the same transaction as the write it describes. If the row
 * were stored without its outbox entry the change would be invisible to sync forever;
 * if the entry were stored without the row the server would be told about something
 * that does not exist. One transaction rules out both.
 *
 * The payload arrives already serialised. This used to take a note specifically, which
 * meant every new syncable record type would have needed its own copy of the queue.
 */
internal object Outbox {

    fun enqueue(
        queries: StudyPlatformQueries,
        entityType: String,
        entityId: String,
        type: String,
        payloadJson: String?,
        clientTime: String
    ) {
        queries.enqueue(
            operation_id = randomUuid(),
            entity_type = entityType,
            entity_id = entityId,
            operation_type = type,
            payload = payloadJson,
            client_time = clientTime
        )
    }
}

/**
 * A version-4 UUID.
 *
 * Written by hand because ids are assigned on the device and the identifier has to be
 * one the server will accept as a UUID; Kotlin Multiplatform has no common UUID type
 * that works on both Android and iOS without pulling in a dependency for it.
 */
fun randomUuid(): String {
    val bytes = ByteArray(16) { Random.nextInt(0, 256).toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()  // version 4
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()  // variant 1
    val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20)}"
}
