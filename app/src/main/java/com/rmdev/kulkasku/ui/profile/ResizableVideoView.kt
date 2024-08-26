package com.rmdev.kulkasku.ui.profile

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.VideoView
import kotlinx.coroutines.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ResizableVideoView(context: Context, attrs: AttributeSet? = null) : VideoView(context, attrs) {

    private var videoWidth = 0
    private var videoHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.getDefaultSize(videoWidth, widthMeasureSpec)
        val height = if (videoWidth > 0 && videoHeight > 0) {
            (width.toFloat() / videoWidth * videoHeight).toInt()
        } else {
            View.getDefaultSize(videoHeight, heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }

    override fun setVideoURI(uri: Uri?) {
        Log.d("ResizableVideoView", "Setting video URI: $uri")
        if (uri != null) {
            setVideoURIFromURL(uri.toString())
        }
    }

    private fun setVideoURIFromURL(videoURL: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(videoURL)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.doInput = true
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        super.setVideoURI(Uri.parse(videoURL))
                    }
                } else {
                    Log.e("ResizableVideoView", "Failed to fetch video from URL: $videoURL")
                }
            } catch (e: IOException) {
                Log.e("ResizableVideoView", "IOException while fetching video from URL: $videoURL", e)
            }
        }
    }
}