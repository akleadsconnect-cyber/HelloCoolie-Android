package com.hellocoolie.services

import android.util.Log
import com.google.gson.Gson
import com.hellocoolie.BuildConfig
import com.hellocoolie.data.model.NewBookingRequest
import com.hellocoolie.utils.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    private var socket: Socket? = null
    private val gson = Gson()
    private val TAG  = "HelloCoolieSocket"

    // ── Listeners ─────────────────────────────────────────
    var onNewBookingRequest:   ((NewBookingRequest) -> Unit)? = null
    var onBookingConfirmed:    ((JSONObject) -> Unit)?        = null
    var onJobStarted:          ((JSONObject) -> Unit)?        = null
    var onJobStartedPorter:    ((JSONObject) -> Unit)?        = null
    var onJobCompleted:        ((JSONObject) -> Unit)?        = null
    var onBookingCancelled:    ((JSONObject) -> Unit)?        = null
    var onBookingExpired:      ((JSONObject) -> Unit)?        = null
    var onTrolleyOffer:        ((JSONObject) -> Unit)?        = null
    var onSosAlert:            ((JSONObject) -> Unit)?        = null
    var onPorterAssigned:      ((JSONObject) -> Unit)?        = null
    var onGroupBookingConfirmed:((JSONObject) -> Unit)?       = null

    fun connect() {
        val token = tokenManager.getToken() ?: return
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 2000
            }
            socket = IO.socket(BuildConfig.SOCKET_URL, opts)
            setupListeners()
            socket?.connect()
            Log.d(TAG, "Connecting to socket...")
        } catch (e: Exception) {
            Log.e(TAG, "Socket connect error: ${e.message}")
        }
    }

    private fun setupListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Socket connected")
                val role = tokenManager.getRole()
                if (role == "porter") {
                    val porter = tokenManager.getPorter()
                    porter?.station?.let { emit("join_station", it) }
                }
            }

            on(Socket.EVENT_DISCONNECT) { Log.d(TAG, "Socket disconnected") }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket error: ${args.firstOrNull()}")
            }

            // ── Porter events ──────────────────────────────
            on("new_booking_request") { args ->
                try {
                    val json = args[0] as JSONObject
                    val req  = gson.fromJson(json.toString(), NewBookingRequest::class.java)
                    onNewBookingRequest?.invoke(req)
                } catch (e: Exception) { Log.e(TAG, "Parse error: ${e.message}") }
            }

            on("booking_confirmed") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onBookingConfirmed?.invoke(it) }
            }

            on("job_started_porter") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onJobStartedPorter?.invoke(it) }
            }

            // ── User events ────────────────────────────────
            on("porter_assigned") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onPorterAssigned?.invoke(it) }
            }

            on("job_started") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onJobStarted?.invoke(it) }
            }

            on("job_completed") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onJobCompleted?.invoke(it) }
            }

            on("booking_cancelled_by_porter") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onBookingCancelled?.invoke(it) }
            }

            on("booking_expired") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onBookingExpired?.invoke(it) }
            }

            on("trolley_offer") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onTrolleyOffer?.invoke(it) }
            }

            on("group_booking_confirmed") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onGroupBookingConfirmed?.invoke(it) }
            }

            on("sos_alert") { args ->
                (args.firstOrNull() as? JSONObject)?.let { onSosAlert?.invoke(it) }
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun isConnected() = socket?.connected() == true
}
