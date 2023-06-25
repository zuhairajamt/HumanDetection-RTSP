package com.alexvas.rtsp.demo

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import timber.log.Timber


class ObjectDetectorHelper(
    var threshold: Float = 0.50f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
        Timber.tag("Test").e("TFLite cleared")
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    objectDetectorListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                MODEL_MOBILENETCOCO -> "mobnetv2-coco-meta.tflite"
                MODEL_MOBILENETCOCOVOC -> "mobnetv2-cocoinriavoc-meta.tflite"
                else -> "mobilenetv1.tflite"
            }

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
            Timber.tag("Test").d("TFLite jalan")
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Timber.tag("Test").e("TFLite failed to load model with error: %s", e.message)
        }
    }

    private var totalInferenceTime: Long = 0
    private var inferenceCount: Int = 0

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the process
        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // total inference time and increment inference count
        totalInferenceTime += inferenceTime
        inferenceCount++

        val inferenceTimeAvg = if (inferenceCount > 0) totalInferenceTime / inferenceCount else 0

        objectDetectorListener?.onResults(
            results,
            inferenceTime,
            inferenceTimeAvg,
            tensorImage.height,
            tensorImage.width
        )

        //Log.d("InferenceTime", "Average Inference Time: $inferenceTimeAvg ms")
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            inferenceTimeAvg: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
        const val MODEL_MOBILENETCOCO = 4
        const val MODEL_MOBILENETCOCOVOC = 5
    }
}
