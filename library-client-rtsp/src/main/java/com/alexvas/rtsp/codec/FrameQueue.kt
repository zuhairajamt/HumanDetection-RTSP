package com.alexvas.rtsp.codec

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class FrameQueue(frameQueueSize: Int) {

    data class Frame (
        val data: ByteArray,
        val offset: Int,
        val length: Int,
        val timestamp: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Frame

            if (!data.contentEquals(other.data)) return false
            if (offset != other.offset) return false
            if (length != other.length) return false
            if (timestamp != other.timestamp) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + offset
            result = 31 * result + length
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    private val queue: BlockingQueue<Frame> = ArrayBlockingQueue(frameQueueSize)

    @Throws(InterruptedException::class)
    fun push(frame: Frame): Boolean {
        return if (queue.offer(frame, 10000 , TimeUnit.MILLISECONDS)) {
            true
        } else {
            Log.w(TAG, "Cannot add frame, queue is full 5")
            Thread.currentThread().interrupt()
            false
        }
//        Log.w(TAG, "Cannot add frame, queue is full 5")
//        Thread.currentThread().interrupt()
//        return false
    }

    @Throws(InterruptedException::class)
    fun pop(): Frame? {
        try {
            val frame: Frame? = queue.poll(10000, TimeUnit.MILLISECONDS)
            if (frame == null) {
                Log.w(TAG, "Cannot pop frame, queue is empty")
            }
            return frame
        } catch (e: InterruptedException) {
            Log.w(TAG, "Cannot delete frame, queue is full", e)
            Thread.currentThread().interrupt()
        }
        return null
    }

    fun clear() {
        queue.clear()
    }

    companion object {
        private val TAG: String = FrameQueue::class.java.simpleName
    }

}
