# HumanDetection-RTSP
Human Detection implementation with RTSP Camera and telegram notification for android. Successfully tested on S905X Amlogic/ZTE B680H V5

## Installation
clone this project
```
git clone https://github.com/zuhairajamt/HumanDetection-RTSP.git
```

change your view layout on fragment_live.xml to your desired layout height (300dp for phone)
```kt
<FrameLayout 
    android:layout_width="match_parent"
    android:layout_height="720dp"
    android:id="@+id/frame_layout"
    android:animateLayoutChanges="true">
```

erase this if want to use other object detection model (with metadata)
OverlayView.kt
```kt
if (result.categories[0].label != "person") continue
```
you can change specific label and timer for notification in LiveFragment.kt
```kt
if (detectionDuration in 15000..20000) { // send photo if true 15s-20s
     val numberOfPersons = results.count { it.categories[0].label == "person" }
     val messageText = "**Detected $numberOfPersons " + "Person** \n" +
```

build and install apk then grant storage permission for saved picture if object detected (required for telegram notification) image location = storage\emulated\0\dcim\

## Requirement
Android 7.0 or 8.1 for NNAPI delegate

## Credit
https://github.com/alexeyvasilyev/rtsp-client-android 
