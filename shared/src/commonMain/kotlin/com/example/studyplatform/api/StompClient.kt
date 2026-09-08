package com.example.studyplatform.api

import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The frame terminator STOMP requires. A real NUL octet, not a space. */
private const val NUL = '\u0000'

/**
 * Just enough STOMP to hold a conversation.
 *
 * The server speaks STOMP over a WebSocket at `/ws`. A full STOMP library is a
 * dependency this app does not need: the frames it actually uses — CONNECT, SUBSCRIBE,
 * SEND, and the MESSAGE frames coming back — are a command line, some `key:value`
 * headers, a blank line, a body, and a NUL. Writing that out is smaller than adding a
 * library that would also have to work on iOS.
 *
 * The server registers `/ws` twice — once natively for this client, once with SockJS
 * for the browser. Both are needed: `withSockJS()` replaces the native endpoint rather
 * than adding a fallback beside it, and with only the SockJS registration a plain
 * handshake to `/ws` is answered with 400.
 *
 * The token goes in a native `Authorization: Bearer` header on CONNECT, which is where
 * `WebSocketAuthInterceptor` looks — not in a query parameter, because a URL carrying a
 * token is logged by every proxy on the way.
 */
object StompClient {

    /** What the caller wants to send: a destination and a body. */
    data class Outgoing(val destination: String, val body: String)

    /**
     * Opens a session, subscribes to [destination], and emits each MESSAGE body.
     *
     * The flow owns the connection: collecting opens it, cancelling closes it. That ties
     * the socket to the screen that wants it, which is the only lifetime that makes
     * sense — one outliving its screen is a leak, one dying early is a bug.
     *
     * Bodies are emitted raw; parsing belongs to the caller, which knows what shape that
     * destination carries. [outbox] is optional: pass one to send as well as listen.
     */
    fun subscribe(
        destination: String,
        outbox: Channel<Outgoing>? = null
    ): Flow<String> = channelFlow {
        val token = ApiClient.getAccessToken() ?: error("Not signed in.")

        val url = ApiClient.baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws"

        ApiClient.client.webSocket(urlString = url) {
            send(
                frame(
                    "CONNECT",
                    mapOf(
                        "accept-version" to "1.2",
                        // Neither side promises heartbeats. The app has no use for them
                        // and a missed one would close a socket that is working.
                        "heart-beat" to "0,0",
                        "Authorization" to "Bearer $token"
                    )
                )
            )

            // A subscription id need only be unique within the session, and this session
            // holds exactly one subscription.
            send(frame("SUBSCRIBE", mapOf("id" to "sub-0", "destination" to destination)))

            outbox?.let { channel ->
                launch {
                    for (message in channel) {
                        if (!isActive) break
                        send(frame("SEND", mapOf("destination" to message.destination), message.body))
                    }
                }
            }

            for (incoming in this.incoming) {
                val text = (incoming as? Frame.Text)?.readText() ?: continue
                bodyOf(text)?.let { trySend(it) }
            }
        }
        // webSocket returns when the session ends, and the flow ends with it rather than
        // hanging open on a socket that is already gone.
    }

    /** Builds a frame: command, headers, blank line, body, NUL. */
    private fun frame(command: String, headers: Map<String, String>, body: String = ""): String =
        buildString {
            append(command).append('\n')
            headers.forEach { (k, v) -> append(k).append(':').append(v).append('\n') }
            if (body.isNotEmpty()) {
                // Byte length, not character count: an accented character would
                // otherwise make the server read a truncated body.
                append("content-length:").append(body.encodeToByteArray().size).append('\n')
            }
            append('\n')
            append(body)
            append(NUL)
        }

    /**
     * The body of a MESSAGE frame, or null for anything else.
     *
     * CONNECTED and RECEIPT frames arrive on the same socket and are not messages. An
     * ERROR frame is raised rather than swallowed, because a subscription that quietly
     * stops delivering looks exactly like a quiet group.
     */
    private fun bodyOf(raw: String): String? {
        val separator = raw.indexOf("\n\n")
        if (separator < 0) return null

        return when (raw.substringBefore('\n').trim()) {
            "ERROR" -> error(
                "Chat connection refused: " +
                        raw.substring(separator + 2).trimEnd(NUL, '\n')
            )
            "MESSAGE" -> raw.substring(separator + 2)
                .trimEnd(NUL, '\n')
                .takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
