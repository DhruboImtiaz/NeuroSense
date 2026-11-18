"""Compare one NeuroSense voice export with Praat's pitch track on the same audio."""

import argparse
from bisect import bisect_left
import csv
import io
import math
from pathlib import Path
import statistics
import wave
import zipfile

FRAME_STEP = 0.025
FRAME_LENGTH = 0.050
MIN_PITCH = 60.0
MAX_PITCH = 500.0
MAX_CSV_BYTES = 128 * 1024
MAX_WAV_BYTES = 1024 * 1024


def read_bundle(path):
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        by_suffix = {Path(entry.filename).suffix.lower(): entry for entry in entries}
        if len(entries) != 2 or set(by_suffix) != {".csv", ".wav"}:
            raise ValueError("A voice ZIP must contain one pitch CSV and one WAV.")
        csv_entry, wav_entry = by_suffix[".csv"], by_suffix[".wav"]
        if Path(csv_entry.filename).stem != Path(wav_entry.filename).stem:
            raise ValueError("The CSV and WAV must have matching filenames from the same check.")
        if csv_entry.file_size > MAX_CSV_BYTES or wav_entry.file_size > MAX_WAV_BYTES:
            raise ValueError("The CSV or WAV is larger than a NeuroSense voice export.")
        return archive.read(csv_entry), archive.read(wav_entry)


def read_pitch_frames(data, frame_count):
    rows = csv.DictReader(io.StringIO(data.decode("utf-8-sig")))
    if not {"elapsed_s", "pitch_hz"}.issubset(rows.fieldnames or ()):
        raise ValueError("The CSV needs elapsed_s and pitch_hz columns.")

    frames = {}
    for row in rows:
        elapsed = float(row["elapsed_s"] or "nan")
        pitch = float(row["pitch_hz"] or "nan")
        if not math.isfinite(elapsed) or not MIN_PITCH <= pitch <= MAX_PITCH:
            raise ValueError(f"Invalid time or pitch at CSV line {rows.line_num}.")
        index = round((elapsed - FRAME_LENGTH / 2) / FRAME_STEP)
        center = FRAME_LENGTH / 2 + index * FRAME_STEP
        if not 0 <= index < frame_count or abs(elapsed - center) > 0.0003 or index in frames:
            raise ValueError(f"Unexpected or duplicate frame time at CSV line {rows.line_num}.")
        frames[index] = pitch
    if not frames:
        raise ValueError("The voice CSV has no usable pitch frames.")
    return frames


def praat_track(audio, parselmouth):
    import numpy as np

    with wave.open(io.BytesIO(audio)) as recording:
        if recording.getnchannels() != 1 or recording.getsampwidth() != 2:
            raise ValueError("Use the mono 16-bit WAV exported by NeuroSense.")
        rate = recording.getframerate()
        count = recording.getnframes()
        if count != 5 * rate or rate not in (16_000, 48_000):
            raise ValueError("Expected a five-second NeuroSense voice check at 16 or 48 kHz.")
        samples = recording.readframes(count)
        if len(samples) != count * 2:
            raise ValueError("The WAV data is incomplete.")

    signal = np.frombuffer(samples, dtype="<i2").astype(np.float64) / 32768.0
    pitch = parselmouth.Sound(signal, sampling_frequency=rate).to_pitch_ac(
        time_step=FRAME_STEP, pitch_floor=MIN_PITCH, pitch_ceiling=MAX_PITCH
    )
    return count / rate, pitch.xs().tolist(), pitch.selected_array["frequency"].tolist()


def compare(frames, frame_count, reference_times, reference_hz):
    joint = []
    app_only = 0
    praat_only = 0
    both_unvoiced = 0
    outside_reference = 0
    for index in range(frame_count):
        time = FRAME_LENGTH / 2 + index * FRAME_STEP
        if not reference_times or time < reference_times[0] - 1e-6 or time > reference_times[-1] + 1e-6:
            outside_reference += 1
            continue
        right = bisect_left(reference_times, time)
        neighbors = (i for i in (right - 1, right) if 0 <= i < len(reference_times))
        nearest = min(neighbors, key=lambda i: abs(reference_times[i] - time))
        app = frames.get(index)
        praat = reference_hz[nearest]
        if app is not None and praat > 0:
            joint.append((app, praat))
        elif app is not None:
            app_only += 1
        elif praat > 0:
            praat_only += 1
        else:
            both_unvoiced += 1
    return joint, app_only, praat_only, both_unvoiced, outside_reference


def print_comparison(frames, frame_count, reference_times, reference_hz):
    joint, app_only, praat_only, both_unvoiced, outside = compare(
        frames, frame_count, reference_times, reference_hz
    )
    print(f"Jointly voiced frames          {len(joint)}")
    print(f"NeuroSense only                {app_only}")
    print(f"Praat only                     {praat_only}")
    print(f"Neither estimator voiced       {both_unvoiced}")
    print(f"Outside Praat's time coverage  {outside}")
    if not joint:
        print("Pitch error is unavailable because the estimators share no voiced frames.")
        return

    errors = [app - praat for app, praat in joint]
    absolute_errors = sorted(abs(error) for error in errors)
    p95 = absolute_errors[math.ceil(0.95 * len(absolute_errors)) - 1]
    octave_jumps = sum(11 <= abs(12 * math.log2(app / praat)) <= 13 for app, praat in joint)
    print()
    for label, values in zip(("NeuroSense", "Praat"), zip(*joint)):
        deciles = statistics.quantiles(values, n=10, method="inclusive") if len(values) > 1 else [values[0]] * 9
        low, high = deciles[0], deciles[-1]
        print(f"{label:<11} median              {statistics.median(values):.2f} Hz")
        print(f"{label:<11} middle 80%          {low:.2f}–{high:.2f} Hz")
    print(f"Mean signed error              {statistics.mean(errors):+.2f} Hz")
    print(f"Mean absolute error            {statistics.mean(absolute_errors):.2f} Hz")
    print(f"95th percentile absolute error {p95:.2f} Hz")
    print(f"Approximately octave apart     {octave_jumps} frames")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle", type=Path, help="ZIP created by Export audio and pitch")
    args = parser.parse_args()
    try:
        import parselmouth
    except ImportError:
        parser.exit(2, "Install the Praat library with python3 -m pip install praat-parselmouth.\n")

    try:
        csv_data, wav_data = read_bundle(args.bundle)
        seconds, times, reference = praat_track(wav_data, parselmouth)
        frame_count = round((seconds - FRAME_LENGTH) / FRAME_STEP) + 1
        frames = read_pitch_frames(csv_data, frame_count)
        print(f"Praat {parselmouth.PRAAT_VERSION} autocorrelation, 60–500 Hz")
        print_comparison(frames, frame_count, times, reference)
    except (OSError, ValueError, KeyError, wave.Error, EOFError, zipfile.BadZipFile, parselmouth.PraatError) as error:
        parser.exit(2, f"Couldn’t compare the voice export. {error}\n")


if __name__ == "__main__":
    main()
