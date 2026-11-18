# NeuroSense

NeuroSense is a native Android app for measuring phone movement and voice pitch with guided check-ins. Version 1.6.0 uses Kotlin and Jetpack Compose. It runs the measurements on the phone.

- **Motion check** records 10 seconds of accelerometer data. Results show typical movement, peak movement, an acceleration trace, and the frequency and strength of a steady rhythm when the recording supports an estimate.
- **Voice check** records a five-second sustained “ah” after a short background sample. Results show median pitch, pitch variation, and a pitch trace. The app asks for a retry if the recording is too quiet, noisy, or clipped, or if it contains too little sustained voiced audio.
- **History** saves motion sessions on the device. Compare two checks with the same setup and sensor method on a shared scale.
- **Exports** include motion CSV, voice pitch CSV, and an opt-in ZIP containing the pitch CSV and matching WAV.

These are experimental measurements of motion and pitch. NeuroSense does not assess disease risk. Synthetic-signal tests cover the measurement pipeline. Accuracy across devices and real recordings still needs validation.

## Build and run

Requires Android SDK Platform 36 and JDK 17 or newer. The app runs on Android 7.0 (API 24) or later. The motion check needs a phone accelerometer, and the voice check needs a microphone.

Open the `android` directory in Android Studio, select a connected Android device, and run the `app` configuration. For command-line builds, set `ANDROID_HOME` to your Android SDK directory, then run these commands from the repository root.

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The debug APK is at `android/app/build/outputs/apk/debug/app-debug.apk`, relative to the repository root. On Windows, use `gradlew.bat`.

## Use the app

### Motion check

1. Open **Check-in**, tap **Start motion check**, and choose a setup. Hold the phone as instructed through the countdown and recording.
2. Review the result and tap **Save session** to keep it on the device.
3. Open **History** to edit a session’s setup, delete it, or select **Compare movement traces**. Sessions labeled **Test recording** stay out of comparisons.

Typical movement is the time-weighted root mean square of the three-axis acceleration magnitude. The app uses linear acceleration when available and estimates and removes gravity from accelerometer readings otherwise. The rhythm analysis resamples signed axis readings onto a 50 Hz grid and looks for a consistent spectral peak between 3 and 12 Hz. Deliberate movement can produce a steady rhythm too.

### Voice check

1. From **Check-in**, tap **Open voice check** and allow microphone access.
2. Stay quiet for the background sample. At the prompt, say “ah” at a comfortable pitch and volume for five seconds.
3. Review median pitch, the middle 80% of the pitch estimates, and the pitch trace. Gaps mark intervals without a usable pitch estimate.

Voice results are available until you close the check. Audio is processed on the phone and discarded after analysis by default. If you enable audio export before recording, the audio is kept in memory until you leave or start over so you can save a copy.

## Export and validate measurements

### Motion

Choose **Export motion CSV** on a result or saved session. The CSV contains elapsed time, signed acceleration on each axis, and movement and rhythm results. Sessions recorded before 1.3 can export their saved display trace. Use rows marked `axis_sample` for frequency comparisons.

For validation, record the phone and a reference accelerometer on the same controlled motion fixture. Include stillness, an isolated bump, and steady motion at known frequencies. Note the phone model and sensor method. Compare the estimate in Hz with the reference and record both the error and how often the app cannot produce an estimate.

### Voice

Choose **Export pitch CSV** for the estimates alone. To keep the audio too, turn on **Enable audio export for this check** before recording and select **Export audio and pitch (ZIP)** on the result screen. The WAV and CSV share the same five-second timeline. The export excludes the quiet baseline.

The included [comparison tool](tools/compare_voice.py) runs [Praat’s pitch estimator](https://www.fon.hum.uva.nl/praat/manual/Pitch.html) on the exported WAV and compares it with NeuroSense frame by frame. The files stay on your computer. Run this from the repository root with Python and [Parselmouth](https://parselmouth.readthedocs.io/en/stable/api_reference.html).

```sh
python3 -m pip install praat-parselmouth
python3 tools/compare_voice.py /path/to/neurosense-voice-recording.zip
```

The tool reports pitch error on frames both estimators can measure, estimated pitch ranges, and frames where the estimators disagree about whether pitch can be measured. Use known tones to check the comparison pipeline and repeated recordings of sustained vowels on physical phones to evaluate repeatability.
