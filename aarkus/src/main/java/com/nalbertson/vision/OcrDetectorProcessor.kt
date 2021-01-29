package com.nalbertson.vision

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.appalchemist.vision.aarkus.R
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.nalbertson.vision.views.GraphicOverlay
import com.nalbertson.vision.views.OcrGraphic

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
</TextBlock> */
class OcrDetectorProcessor constructor(private val mGraphicOverlay: GraphicOverlay<OcrGraphic>, activity: Activity) : Detector.Processor<TextBlock> {

    private val textRecognizer: TextRecognizer = TextRecognizer.Builder(activity).build()

    private var mProcessingThread: Thread?
    private var mFrameProcessor: FrameProcessingRunnable

    init {
        textRecognizer.setProcessor(this)
        if (!textRecognizer.isOperational) {

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = activity.registerReceiver(null, lowstorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(activity, R.string.ocr_low_storage_error, Toast.LENGTH_LONG).show()
            }
        }
        mFrameProcessor = FrameProcessingRunnable(textRecognizer)
        mProcessingThread = Thread(mFrameProcessor)
    }

    fun start() {
        mFrameProcessor.setActive(true)
        mProcessingThread?.start()
    }

    fun stop() {
        mFrameProcessor.setActive(false)
        if (mProcessingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                mProcessingThread!!.join()
            } catch (e: InterruptedException) {
            }

            mProcessingThread = null
        }
    }

    fun processImage(bitmap: Bitmap) {
        mFrameProcessor.setNextFrame(bitmap)
    }

    override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
        mGraphicOverlay.clear()
        val items = detections.detectedItems
        for (i in 0 until items.size()) {
            val item = items.valueAt(i)
            if (item.components.isEmpty()) {
                if (item != null && item.value != null) {
                    Log.d("Processor", "Text detected! " + item.value)
                }
                val graphic = OcrGraphic(mGraphicOverlay, item)
                mGraphicOverlay.add(graphic)
            } else {
                for (text in item.components) {
                    if (text.components.isEmpty()) {
                        if (text != null && text.value != null) {
                            Log.d("Processor", "Text line detected! " + text.value)
                        }
                        val graphic = OcrGraphic(mGraphicOverlay, text)
                        mGraphicOverlay.add(graphic)
                    } else {
                        for (word in text.components) {
                            if (word != null && word.value != null) {
                                Log.d("Processor", "Text line detected! " + word.value)
                            }
                            val graphic = OcrGraphic(mGraphicOverlay, word)
                            mGraphicOverlay.add(graphic)
                        }
                    }
                }
            }

        }
    }

    override fun release() {
        mGraphicOverlay.clear()
    }

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera.  This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     *
     *
     * While detection is running on a frame, new frames may be received from the camera.  As these
     * frames come in, the most recent frame is held onto as pending.  As soon as detection and its
     * associated processing are done for the previous frame, detection on the mostly recently
     * received frame will immediately start on the same thread.
     */
    internal inner class FrameProcessingRunnable internal constructor(private var mDetector: Detector<*>?) : Runnable {
        private val mStartTimeMillis = SystemClock.elapsedRealtime()

        // This lock guards all of the member variables below.
        private val mLock = java.lang.Object()
        private var mActive = true

        // These pending variables hold the state associated with the new frame awaiting processing.
        private var mPendingTimeMillis: Long = 0
        private var mPendingFrameId = 0
        private var mPendingImage: Bitmap? = null

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        internal fun release() {
            assert(mProcessingThread?.state == Thread.State.TERMINATED)
            mDetector?.release()
            mDetector = null
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        internal fun setActive(active: Boolean) {
            synchronized(mLock) {
                mActive = active
                mLock.notifyAll()
            }
        }

        /**
         * Sets the frame data received from the camera.  This adds the previous unused frame buffer
         * (if present) back to the camera, and keeps a pending reference to the frame data for
         * future use.
         */
        internal fun setNextFrame(image: Bitmap) {
            synchronized(mLock) {
                // Timestamp and frame ID are maintained here, which will give downstream code some
                // idea of the timing of frames received and when frames were dropped along the way.
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis
                mPendingFrameId++
                mPendingImage = image

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll()
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames
         * continuously.  The next pending frame is either immediately available or hasn't been
         * received yet.  Once it is available, we transfer the frame info to local variables and
         * run detection on that frame.  It immediately loops back for the next frame without
         * pausing.
         *
         *
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context
         * switching or frame acquisition time latency.
         *
         *
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        override fun run() {
            var outputFrame: Frame? = null

            while (true) {
                synchronized(mLock) {
                    while (mActive && mPendingImage == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            mLock.wait()
                        } catch (e: InterruptedException) {
                            return
                        }

                    }

                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }

                    outputFrame = Frame.Builder()
                        .setBitmap(mPendingImage!!)
                        .setId(mPendingFrameId)
                        .setTimestampMillis(mPendingTimeMillis)
                        .build()

                    mPendingImage = null
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                mDetector?.receiveFrame(outputFrame)
            }
        }
    }
}
