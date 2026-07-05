package com.securechat.phoenix.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.phoenix.auth.TokenManager
import com.securechat.phoenix.chat.network.ChatSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = savedStateHandle.get<String>("chatId") ?: ""
    private var callType: String = "voice"

    fun setCallType(type: String) {
        callType = type
    }

    private val _callSession = MutableStateFlow(CallSession())
    val callSession: StateFlow<CallSession> = _callSession.asStateFlow()

    private val _permissionNeeded = MutableStateFlow<String?>(null)
    val permissionNeeded: StateFlow<String?> = _permissionNeeded.asStateFlow()

    private var socket: Socket? = null

    fun startCall() {
        // Check permissions first
        if (!hasAudioPermission()) {
            _permissionNeeded.value = Manifest.permission.RECORD_AUDIO
            return
        }
        if (callType == "video" && !hasCameraPermission()) {
            _permissionNeeded.value = Manifest.permission.CAMERA
            return
        }

        // Permissions granted — initiate the call
        initiateCall()
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionNeeded.value = null
        if (granted) {
            // Re-check: if video call needs camera too
            if (callType == "video" && !hasCameraPermission()) {
                _permissionNeeded.value = Manifest.permission.CAMERA
                return
            }
            initiateCall()
        } else {
            _callSession.value = CallSession(state = CallState.FAILED)
        }
    }

    private fun initiateCall() {
        _callSession.value = CallSession(
            state = CallState.OUTGOING_RINGING,
            remoteUserId = chatId,
            remoteDisplayName = "User ${chatId.takeLast(6)}",
            isOutgoing = true,
            startTime = System.currentTimeMillis()
        )

        // Connect to Socket.io and emit call:initiate
        viewModelScope.launch {
            val token = tokenManager.getAccessToken() ?: return@launch

            try {
                val options = IO.Options().apply {
                    auth = mapOf("token" to token)
                    reconnection = false
                    timeout = 30000
                }
                socket = IO.socket("http://10.0.2.2:3000", options)

                socket?.on(Socket.EVENT_CONNECT) {
                    // Send call initiation
                    val payload = JSONObject().apply {
                        put("calleeId", chatId)
                        put("callType", callType)
                        put("offer", JSONObject().apply {
                            put("type", "offer")
                            put("sdp", "v=0\r\n") // Placeholder — real SDP from WebRTC
                        })
                    }
                    socket?.emit("call:initiate", payload)
                }

                socket?.on("call:answer") { args ->
                    // Remote user answered
                    _callSession.value = _callSession.value.copy(
                        state = CallState.CONNECTED,
                        connectTime = System.currentTimeMillis()
                    )
                }

                socket?.on("call:rejected") {
                    _callSession.value = _callSession.value.copy(state = CallState.REJECTED)
                }

                socket?.on("call:timeout") {
                    _callSession.value = _callSession.value.copy(state = CallState.MISSED)
                }

                socket?.on("call:busy") {
                    _callSession.value = _callSession.value.copy(state = CallState.FAILED)
                }

                socket?.on(Socket.EVENT_CONNECT_ERROR) {
                    _callSession.value = _callSession.value.copy(state = CallState.FAILED)
                }

                socket?.connect()
            } catch (e: Exception) {
                _callSession.value = _callSession.value.copy(state = CallState.FAILED)
            }
        }
    }

    fun endCall() {
        val session = _callSession.value
        if (session.callId.isNotEmpty()) {
            val payload = JSONObject().apply {
                put("callId", session.callId)
                put("otherUserId", session.remoteUserId)
            }
            socket?.emit("call:end", payload)
        }
        _callSession.value = _callSession.value.copy(state = CallState.ENDED)
        socket?.disconnect()
        socket = null
    }

    fun toggleMute() {
        val current = _callSession.value
        _callSession.value = current.copy(isMuted = !current.isMuted)
        // TODO: mute local audio track via WebRTCEngine
    }

    fun toggleSpeaker() {
        val current = _callSession.value
        _callSession.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
        // TODO: switch audio route via AudioRoutingManager
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        super.onCleared()
        socket?.disconnect()
        socket = null
    }
}
