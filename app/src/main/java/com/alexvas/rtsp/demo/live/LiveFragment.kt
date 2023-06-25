package com.alexvas.rtsp.demo.live


//import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alexvas.rtsp.demo.ObjectDetectorHelper
import com.alexvas.rtsp.demo.R
import com.alexvas.rtsp.demo.databinding.FragmentLiveBinding
import com.alexvas.rtsp.widget.RtspSurfaceView
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SuppressLint("LogNotTimber")
class LiveFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private lateinit var binding: FragmentLiveBinding
    private lateinit var liveViewModel: LiveViewModel
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var surfaceView: SurfaceView
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var cameraExecutor: ExecutorService

    private val rtspStatusListener = object: RtspSurfaceView.RtspStatusListener {
        override fun onRtspStatusConnecting() {
            binding.tvStatus.text = "RTSP connecting"
            binding.pbLoading.visibility = View.VISIBLE
            binding.vShutter.visibility = View.VISIBLE
            binding.etRtspRequest.isEnabled = false
            binding.etRtspUsername.isEnabled = false
            binding.etRtspPassword.isEnabled = false
            binding.cbVideo.isEnabled = false
            binding.cbAudio.isEnabled = false
            binding.cbDebug.isEnabled = false

        }

        override fun onRtspStatusConnected() {
            binding.tvStatus.text = "RTSP connected"
            binding.bnStartStop.text = "Stop RTSP"
            binding.pbLoading.visibility = View.GONE
        }

        override fun onRtspStatusDisconnected() {
            binding.tvStatus.text = "RTSP disconnected"
            binding.bnStartStop.text = "Start RTSP"
            binding.pbLoading.visibility = View.GONE
            binding.vShutter.visibility = View.VISIBLE
//            binding.bnSnapshot.isEnabled = false
            binding.cbVideo.isEnabled = true
            binding.cbAudio.isEnabled = true
            binding.cbDebug.isEnabled = true
            binding.etRtspRequest.isEnabled = true
            binding.etRtspUsername.isEnabled = true
            binding.etRtspPassword.isEnabled = true

            stopObjectDetectionTimer()
        }

        override fun onRtspStatusFailedUnauthorized() {
            if (context == null) return
            binding.tvStatus.text = "RTSP username or password invalid"
            binding.pbLoading.visibility = View.GONE
            Toast.makeText(context, binding.tvStatus.text , Toast.LENGTH_LONG).show()
        }

        override fun onRtspStatusFailed(message: String?) {
            if (context == null) return
            binding.tvStatus.text = "Error: $message"
            Toast.makeText(context, binding.tvStatus.text , Toast.LENGTH_LONG).show()
            binding.pbLoading.visibility = View.GONE
        }

        override fun onRtspFirstFrameRendered() {
            binding.vShutter.visibility = View.GONE
//            binding.bnSnapshot.isEnabled = true

            startObjectDetectionWithTimer()
        }
    }

    private var timer: Timer? = null

    // Declare a global variable for the timer
    private var objectDetectionTimer: Timer? = null

    private fun stopObjectDetectionTimer() {
        objectDetectionTimer?.cancel()
        objectDetectionTimer = null
    }

    private fun startObjectDetectionWithTimer() {
        stopObjectDetectionTimer()

        // Create a new timer
        objectDetectionTimer = Timer()

        // Schedule the task to run at a specified interval
        objectDetectionTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Run the object detection code here
                val surfaceView = binding.svVideo
                val bitmap = getBitmapFromViewUsingPixelCopy(surfaceView)
                detectObjects(bitmap)
            }
        }, 3000, 1000) // Specify the interval in milliseconds (e.g., 5000 for 5 seconds)
    }

    private fun getBitmapFromViewUsingPixelCopy(view: RtspSurfaceView): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val surface = view.holder.surface

        val pixelCopyListener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                //Log.d("TAG PIXELCOPY BITMAP: ", bitmap.toString())
            } else {
                Log.e("TAG PIXELCOPY BITMAP error: ", bitmap.toString())
            }
        }

        val handler = Handler(Looper.getMainLooper())

        PixelCopy.request(surface, bitmap, pixelCopyListener, handler)

        return bitmap
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (DEBUG) Log.v(TAG, "onCreateView()")

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

        liveViewModel = ViewModelProvider(this).get(LiveViewModel::class.java)
        binding = FragmentLiveBinding.inflate(inflater, container, false)

        val rootView = inflater.inflate(R.layout.fragment_live, container, false)
        surfaceView = rootView.findViewById(R.id.svVideo)

        binding.svVideo.setStatusListener(rtspStatusListener)
        binding.etRtspRequest.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != liveViewModel.rtspRequest.value) {
                    liveViewModel.rtspRequest.value = text
                }
            }
        })
        binding.etRtspUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != liveViewModel.rtspUsername.value) {
                    liveViewModel.rtspUsername.value = text
                }
            }
        })
        binding.etRtspPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != liveViewModel.rtspPassword.value) {
                    liveViewModel.rtspPassword.value = text
                }
            }
        })

        liveViewModel.rtspRequest.observe(viewLifecycleOwner) {
            if (binding.etRtspRequest.text.toString() != it)
                binding.etRtspRequest.setText(it)
        }
        liveViewModel.rtspUsername.observe(viewLifecycleOwner) {
            if (binding.etRtspUsername.text.toString() != it)
                binding.etRtspUsername.setText(it)
        }
        liveViewModel.rtspPassword.observe(viewLifecycleOwner) {
            if (binding.etRtspPassword.text.toString() != it)
                binding.etRtspPassword.setText(it)
        }

        binding.bnStartStop.setOnClickListener {
            if (binding.svVideo.isStarted()) {
                binding.svVideo.stop()
            } else {
                val uri = Uri.parse(liveViewModel.rtspRequest.value)
                binding.svVideo.init(uri, liveViewModel.rtspUsername.value, liveViewModel.rtspPassword.value, "rtsp-client-android")
                binding.svVideo.debug = binding.cbDebug.isChecked
                binding.svVideo.start(binding.cbVideo.isChecked, binding.cbAudio.isChecked)
            }
        }

        return binding.root
    }


    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)


        // Attach listeners to UI control widgets
        initBottomSheetControls()
    }

    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        binding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.011f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        binding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.01f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        binding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        binding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 100) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        binding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        binding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        binding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        binding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        binding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        binding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    private fun updateControlsUi() {
        binding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        binding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        binding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        binding.overlay.clear()
    }

    override fun onResume() {
        if (DEBUG) Log.v(TAG, "onResume()")
        super.onResume()
        liveViewModel.loadParams(requireContext())
    }

    override fun onPause() {
        val started = binding.svVideo.isStarted()
        if (DEBUG) Log.v(TAG, "onPause(), started:$started")
        super.onPause()
        liveViewModel.saveParams(requireContext())

        if (started) {
            binding.svVideo.stop()
        }
    }

    companion object {
        private val TAG: String = LiveFragment::class.java.simpleName
        private const val DEBUG = true
    }



    // Tensorflow
    private fun detectObjects(image: Bitmap) {
        objectDetectorHelper.detect(image, 0)
    }


    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        inferenceTimeAvg: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            binding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            binding.bottomSheetLayout.inferenceTimeValAvg.text =
                String.format("%d ms", inferenceTimeAvg)

            // Pass necessary information to OverlayView for drawing on the canvas
            binding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            binding.overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

}
