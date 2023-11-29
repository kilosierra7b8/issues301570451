# issues301570451
Reproduction code for [Android camera2 manual exposure and auto exposure of the same values are not the same](https://issuetracker.google.com/issues/301570451).

CAMERAX VERSION: "1.4.0-alpha02"

CAMERA APPLICATION NAME AND VERSION: N/A

ANDROID OS BUILD NUMBER: "TD4A.221205.042"

DEVICE NAME: "Pixel 7a"

DESCRIPTION:

## Config for Auto
```
Camera2Interop.Extender(builder)
  .setCaptureRequestOption(
      CaptureRequest.CONTROL_MODE,
      CameraMetadata.CONTROL_MODE_AUTO,
  )
  .setCaptureRequestOption(
      CaptureRequest.CONTROL_AE_MODE,
      CameraMetadata.CONTROL_AE_MODE_ON
  )
```

## Config for Manual
```
Camera2Interop.Extender(builder)
  .setCaptureRequestOption(
      CaptureRequest.CONTROL_MODE,
      CameraMetadata.CONTROL_MODE_AUTO,
  )
  .setCaptureRequestOption(
      CaptureRequest.CONTROL_AE_MODE,
      CameraMetadata.CONTROL_AE_MODE_OFF
  )
  .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 80)
  .setCaptureRequestOption(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100)
  .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 33330940)
  .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 3???????)
```

LIST ANY EXPERIMENTAL FEATURES: (As an example - @ExperimentalCamera2Interop)
- ExperimentalCamera2Interop
- ExperimentalPermissionsApi

STEPS TO REPRODUCE:
1. Take a picture as Auto, and remember (sensitivity, postRawSensitivityBoost, sensorExposureTime, frameDuration)
1. Take a picture as Manual with remembered parameters.

OBSERVED RESULTS:

Manual picture is same with Auto.

EXPECTED RESULTS:

Manual picture is darker.

REPRODUCIBILITY: (5 of 5, 1 of 100, etc)

Always (for my cases, device is 10cm above from the object). 

IMAGES

![Auto](https://github.com/kilosierra7b8/issues301570451/assets/152163337/6582e8c8-6062-4279-b19a-738800ed3dc8)
![Manual](https://github.com/kilosierra7b8/issues301570451/assets/152163337/f343b648-ae38-48cb-8e8e-8a21defa786a)

