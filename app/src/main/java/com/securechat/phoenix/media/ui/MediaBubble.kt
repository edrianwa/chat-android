package com.securechat.phoenix.media.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.securechat.phoenix.media.MediaPayload

/**
 * Media message bubble that shows thumbnail with download/progress indicators.
 */
@Composable
fun MediaBubble(
    payload: MediaPayload,
    isDownloaded: Boolean,
    downloadProgress: Float,
    decryptedBytes: ByteArray?,
    isVideo: Boolean = false,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatio = if (payload.width > 0 && payload.height > 0) {
        payload.width.toFloat() / payload.height.toFloat()
    } else 1.5f

    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (isDownloaded && decryptedBytes != null) {
            // Show full decrypted image
            val bitmap = remember(decryptedBytes) {
                BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                )
            }
        } else {
            // Show blurred thumbnail
            val thumbnailBitmap = remember(payload.thumbnail) {
                try {
                    val bytes = Base64.decode(payload.thumbnail, Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .blur(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )
            }

            // Download/progress overlay
            when {
                downloadProgress in 0.01f..0.99f -> {
                    CircularProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
                !isDownloaded -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Video play overlay on downloaded video
        if (isDownloaded && isVideo) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
