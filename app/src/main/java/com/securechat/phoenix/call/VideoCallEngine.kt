package com.securechat.phoenix.call

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Extended WebRTC engine with video support.
 * Manages both audio and video tracks, camera switching, and bandwidth adaptation.
 */
class VideoCallEngine(
    private val context: Context,
    private val eglBase: EglBase,
    private val listener: VideoCallListener
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // Audio
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Video
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSender: RtpSender? = null

    private var isUsingFrontCamera = true
    private var isVideoEnabled = true
    private var currentWidth = 1280
    private var currentHeight = 720
    private var currentFps = 30

    interface VideoCallListener {
        fun onLocalOffer(sdp: SessionDescription)
        fun onLocalAnswer(sdp: SessionDescription)
        fun onIceCandidate(candidate: IceCandidate)
        fun onConnected()
        fun onDisconnected()
        fun onFailed()
        fun onRemoteVideoTrack(track: VideoTrack)
        fun onRemoteVideoRemoved()
    }

    companion object {
        // Bandwidth adaptation thresholds
        const val BITRATE_GOOD = 1_500_000  // 1.5 Mbps → 720p
        const val BITRATE_FAIR = 800_000    // 800 Kbps → 480p
        const val BITRATE_POOR = 300_000    // 300 Kbps → 360p

        const val FRONT_WIDTH = 1280
        const val FRONT_HEIGHT = 720
        const val BACK_WIDTH = 1920
        const val BACK_HEIGHT = 1080
        const val DEFAULT_FPS = 30
    }

    /**
     * Initialize WebRTC factory with video encoder/decoder.
     */
    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    /**
     * Create PeerConnection with ICE servers.
     */
    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>, withVideo: Boolean) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { listener.onIceCandidate(it) }
                }
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> listener.onConnected()
                        PeerConnection.IceConnectionState.DISCONNECTED -> listener.onDisconnected()
                        PeerConnection.IceConnectionState.FAILED -> listener.onFailed()
                        else -> {}
                    }
                }
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    receiver?.track()?.let { track ->
                        if (track is VideoTrack) {
                            listener.onRemoteVideoTrack(track)
                        }
                    }
                }
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
            }
        )

        addAudioTrack()
        if (withVideo) {
            addVideoTrack()
        }
    }

    /**
     * Create SDP offer.
     */
    fun createOffer(withVideo: Boolean) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (withVideo) "true" else "false"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(NoOpSdpObserver(), it)
                    listener.onLocalOffer(it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { listener.onFailed() }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Handle remote offer and create answer.
     */
    fun handleRemoteOffer(offerSdp: SessionDescription, withVideo: Boolean) {
        peerConnection?.setRemoteDescription(NoOpSdpObserver(), offerSdp)
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (withVideo) "true" else "false"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(NoOpSdpObserver(), it)
                    listener.onLocalAnswer(it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { listener.onFailed() }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Set remote answer SDP.
     */
    fun handleRemoteAnswer(answerSdp: SessionDescription) {
        peerConnection?.setRemoteDescription(NoOpSdpObserver(), answerSdp)
    }

    /**
     * Add remote ICE candidate.
     */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    // --- Video Controls ---

    /**
     * Enable or disable the local video track.
     */
    fun setVideoEnabled(enabled: Boolean) {
        isVideoEnabled = enabled
        localVideoTrack?.setEnabled(enabled)
        if (!enabled) {
            videoCapturer?.stopCapture()
        } else {
            videoCapturer?.startCapture(currentWidth, currentHeight, currentFps)
        }
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isUsingFrontCamera = isFront
                // Adjust resolution for back camera
                if (isFront) {
                    currentWidth = FRONT_WIDTH
                    currentHeight = FRONT_HEIGHT
                } else {
                    currentWidth = BACK_WIDTH
                    currentHeight = BACK_HEIGHT
                }
                videoCapturer?.changeCaptureFormat(currentWidth, currentHeight, currentFps)
            }
            override fun onCameraSwitchError(error: String?) {}
        })
    }

    /**
     * Add video track mid-call (voice-to-video upgrade).
     */
    fun addVideoTrackMidCall() {
        if (localVideoTrack != null) return
        addVideoTrack()
    }

    /**
     * Remove video track (video-to-voice downgrade).
     */
    fun removeVideoTrack() {
        videoSender?.let { peerConnection?.removeTrack(it) }
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        videoSource?.dispose()
        videoCapturer = null
        localVideoTrack = null
        videoSource = null
        videoSender = null
    }

    /**
     * Adapt video quality based on available bandwidth.
     */
    fun adaptBandwidth(estimatedBitrate: Long) {
        val (width, height, fps) = when {
            estimatedBitrate >= BITRATE_GOOD -> Triple(1280, 720, 30)
            estimatedBitrate >= BITRATE_FAIR -> Triple(854, 480, 24)
            else -> Triple(640, 360, 15)
        }
        if (width != currentWidth || height != currentHeight || fps != currentFps) {
            currentWidth = width
            currentHeight = height
            currentFps = fps
            videoCapturer?.changeCaptureFormat(width, height, fps)
        }
    }

    /**
     * Get the local video track for rendering.
     */
    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun isVideoEnabled(): Boolean = isVideoEnabled
    fun isFrontCamera(): Boolean = isUsingFrontCamera

    /**
     * Mute/unmute audio.
     */
    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    /**
     * Clean up all resources.
     */
    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        videoSource?.dispose()
        surfaceTextureHelper?.dispose()
        localAudioTrack?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        peerConnection = null
        peerConnectionFactory = null
    }

    // --- Private ---

    private fun addAudioTrack() {
        val factory = peerConnectionFactory ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }
        audioSource = factory.createAudioSource(constraints)
        localAudioTrack = factory.createAudioTrack("audio_local", audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack, listOf("stream_local"))
    }

    private fun addVideoTrack() {
        val factory = peerConnectionFactory ?: return
        videoCapturer = createCameraCapturer() ?: return

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)

        currentWidth = if (isUsingFrontCamera) FRONT_WIDTH else BACK_WIDTH
        currentHeight = if (isUsingFrontCamera) FRONT_HEIGHT else BACK_HEIGHT
        videoCapturer!!.startCapture(currentWidth, currentHeight, currentFps)

        localVideoTrack = factory.createVideoTrack("video_local", videoSource)
        localVideoTrack?.setEnabled(true)
        videoSender = peerConnection?.addTrack(localVideoTrack, listOf("stream_local"))
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Prefer front camera
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) {
                    isUsingFrontCamera = true
                    return capturer
                }
            }
        }
        // Fallback to back camera
        for (name in deviceNames) {
            if (enumerator.isBackFacing(name)) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) {
                    isUsingFrontCamera = false
                    return capturer
                }
            }
        }
        return null
    }

    private class NoOpSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
