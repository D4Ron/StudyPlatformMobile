package com.example.studyplatform.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The §7 matrix, on the client.
 *
 * Every case here is something that happens on a bad connection and nowhere else, which
 * is exactly why none of it would be caught by using the app on a desk with wifi. The
 * failures these guard against are silent: a note that quietly stops existing, a second
 * copy of something the student wrote once, work overwritten by an older version of
 * itself.
 */
class OfflineSyncTest {

    // ── create-offline-then-sync ─────────────────────────────

    @Test
    fun `a note written with no connection reaches the server later`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        server.offline = true
        device.notes.save(title = "Bus notes", content = "written with no signal")

        assertEquals(1, device.localNotes().size, "the write must not wait on the network")
        assertEquals(1, device.outboxSize())

        val offlineOutcome = device.engine.sync()
        assertTrue(offlineOutcome.failed, "a sync with no connection reports failure")
        assertEquals(1, device.outboxSize(), "and leaves the queue intact")

        server.offline = false
        val outcome = device.engine.sync()

        assertEquals(1, outcome.pushed)
        assertEquals(0, device.outboxSize(), "a landed change leaves the queue")
        assertEquals(1, server.count("note"))
        assertFalse(device.localNotes().first().pending == 1L,
            "and the row stops being marked as unsent")
    }

    // ── partial-failure retry ────────────────────────────────

    @Test
    fun `one rejected change does not strand the rest of the batch`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        server.offline = true
        repeat(5) { device.notes.save(title = "Note $it", content = "body $it") }
        server.offline = false

        // The server refuses the third operation and accepts the others.
        val third = device.db.studyPlatformQueries.selectOutboxBatch(50)
            .executeAsList()[2].operation_id
        server.reject += third

        val outcome = device.engine.sync()

        assertEquals(4, outcome.pushed)
        assertEquals(1, outcome.rejected)
        assertEquals(0, device.outboxSize(),
            "a rejection is an answer, not a reason to keep asking")
        assertEquals(4, server.count("note"),
            "a device offline for a week may push fifty changes; one bad record must "
                    + "not strand the other forty-nine")
    }

    // ── app-killed-mid-sync ──────────────────────────────────

    @Test
    fun `a reply lost after the server applied it does not create a second note`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        device.notes.save(title = "Only once", content = "please")

        // The server applies the change, then the connection dies before the reply.
        server.dropReply = true
        val first = device.engine.sync()
        assertTrue(first.failed)
        assertEquals(1, device.outboxSize(), "the device still believes it has work to do")

        // The phone is killed, reopened, and the worker drains the same queue.
        server.dropReply = false
        val second = device.engine.sync()

        assertEquals(1, second.pushed)
        assertEquals(1, server.count("note"),
            "the operation id is what makes the retry free rather than duplicating")
        assertEquals(0, device.outboxSize())
    }

    @Test
    fun `a failing change is eventually left alone rather than retried forever`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        device.notes.save(title = "Doomed", content = "never lands")

        server.offline = true
        repeat(SyncEngine.MAX_ATTEMPTS.toInt() + 1) { device.engine.sync() }

        assertEquals(1, device.failedCount(),
            "a change the server will never take must stop burning data on a "
                    + "connection the student pays for")

        server.offline = false
        val outcome = device.engine.sync()
        assertEquals(0, outcome.pushed, "and is not silently retried once signal returns")
        assertEquals(1, device.failedCount(), "it is surfaced instead")
    }

    // ── edit-same-record-on-two-devices ──────────────────────

    @Test
    fun `the losing device is handed the version that survived`() = runTest {
        val server = FakeServer()
        val phone = FakeDevice(server)
        val tablet = FakeDevice(server)

        // Both devices hold the same note.
        val id = phone.notes.save(title = "Shared", content = "original")
        phone.engine.sync()
        tablet.engine.sync()
        assertNotNull(tablet.noteRow(id))

        // Three hours pass, then the tablet edits and uploads first.
        server.advance(3 * 60 * 60)
        tablet.notes.save(id = id, title = "Shared", content = "edited on the tablet")
        tablet.engine.sync()

        // The phone was offline while it made an older edit, and only now uploads.
        // Its clientUpdatedAt is earlier than what the server holds.
        val staleEdit = "2026-01-01T09:30:00Z"
        phone.db.studyPlatformQueries.upsertNote(
            id = id, title = "Shared", content = "edited on the phone",
            group_id = null, shared_with_group = 0L,
            updated_at = staleEdit, version = 1L, deleted = 0L, pending = 1L
        )
        phone.db.studyPlatformQueries.enqueue(
            operation_id = randomUuid(), entity_type = "note", entity_id = id,
            operation_type = "UPDATE",
            payload = """{"title":"Shared","content":"edited on the phone","sharedWithGroup":false}""",
            client_time = staleEdit
        )

        val outcome = phone.engine.sync()

        assertEquals(1, outcome.conflicts)
        assertEquals("edited on the tablet", phone.noteRow(id)?.content,
            "the loser has to be shown what survived, or the student never learns "
                    + "their edit is gone")
        assertEquals(0L, phone.noteRow(id)?.pending,
            "and the row stops claiming it has unsent work")
    }

    // ── delete-offline ───────────────────────────────────────

    @Test
    fun `a note deleted offline stays deleted after it syncs`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        val id = device.notes.save(title = "Temporary", content = "goes away")
        device.engine.sync()

        server.offline = true
        device.notes.delete(id)
        assertEquals(0, device.localNotes().size, "gone from the list immediately")
        assertNotNull(device.noteRow(id), "but kept as a tombstone until the server knows")

        server.offline = false
        device.engine.sync()

        assertTrue(server.record(id)?.deleted == true)
        assertNull(device.noteRow(id),
            "once the deletion has round-tripped the local row can finally go")
    }

    @Test
    fun `a note deleted on another device disappears here`() = runTest {
        val server = FakeServer()
        val phone = FakeDevice(server)

        val id = phone.notes.save(title = "Doomed", content = "elsewhere")
        phone.engine.sync()

        val tablet = FakeDevice(server)
        tablet.engine.sync()

        // Time passes, then the tablet deletes it. The tombstone has to be stamped
        // later than the phone's cursor or the phone will never see it — which is the
        // whole reason the server, not the device, decides what the cursor is.
        server.advance(60 * 60)
        tablet.notes.delete(id)
        tablet.engine.sync()

        phone.engine.sync()

        assertNull(phone.noteRow(id),
            "without a tombstone the phone could never tell 'deleted elsewhere' from "
                    + "'not downloaded yet' and would keep it forever")
    }

    // ── long-offline-then-reconnect ──────────────────────────

    @Test
    fun `a week of offline work uploads in one go`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        server.offline = true
        repeat(30) { device.notes.save(title = "Day $it", content = "notes from day $it") }
        device.study.saveTopic(name = "Chemistry")
        device.study.saveSession(startTime = "2026-01-01T08:00:00Z", endTime = "2026-01-01T09:00:00Z")

        assertEquals(32, device.outboxSize())

        server.offline = false
        val outcome = device.engine.sync()

        assertEquals(32, outcome.pushed)
        assertEquals(0, device.outboxSize())
        assertEquals(1, server.pushCount, "one round trip, not thirty-two")
    }

    // ── clock skew ───────────────────────────────────────────

    @Test
    fun `the pull cursor uses the server clock, not the device clock`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        device.notes.save(title = "First", content = "one")
        device.engine.sync()

        assertEquals(server.lastServerTime, device.cursor(),
            "the cursor has to be the server's clock: one taken from a device whose "
                    + "clock runs fast silently skips every record written in the gap")
        assertTrue(
            device.cursor()!!.startsWith("2026-"),
            "and it is a real server timestamp, not a device-generated one"
        )
    }

    @Test
    fun `a record written while we were offline arrives on the next pull`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        device.engine.sync()

        // Another device adds a note an hour later.
        server.advance(60 * 60)
        val id = randomUuid()
        server.seed("note", id, server.notePayload("From elsewhere", "written on a tablet"))

        val outcome = device.engine.sync()

        assertEquals(1, outcome.pulled)
        assertEquals("written on a tablet", device.noteRow(id)?.content)
    }

    // ── push before pull ─────────────────────────────────────

    @Test
    fun `a pull never overwrites work that has not been uploaded`() = runTest {
        val server = FakeServer()
        val device = FakeDevice(server)

        val id = device.notes.save(title = "Mine", content = "local edit")

        // The server holds an older copy of the same note, and the upload will fail.
        server.seed("note", id, server.notePayload("Mine", "stale server copy"))

        val opId = device.db.studyPlatformQueries.selectOutboxBatch(50)
            .executeAsList().first().operation_id
        server.reject += opId

        device.engine.sync()

        // The rejection cleared the queue entry but the row is no longer pending, so a
        // later pull may legitimately replace it. What must never happen is the pull
        // in the *same* sync silently undoing an edit that was still waiting to go.
        assertEquals("local edit", device.noteRow(id)?.content,
            "pulling before pushing would delete work the student did offline, with "
                    + "nothing having visibly failed")
    }
}
