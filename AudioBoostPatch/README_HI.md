# Telegram AudioBoost — extracted logic

Ye bundle pehle uploaded WebView APK se extracted audio-processing logic ko preserve karta hai.

## Original chain
Mic -> HPF 75Hz -> Bass shelf 200Hz -> Presence peaking 3200Hz -> Treble shelf 6000Hz -> Compressor -> Compressor -> Loudness -> Gain -> Saturation (4x) -> Sustain -> Limiter -> Telegram

Parallel branch: Saturation -> Delay/Reverb feedback -> Wet -> Sustain

## Default values
- Gain: +24 dB
- Loudness: +4
- Saturation drive: 0.28
- Compressor threshold: -38 dB
- Knee: 40
- Ratio: 12:1
- Attack: 0.1 ms
- Release: 30 ms
- Presence: +8 dB @ 3.2 kHz, Q 1.5
- Bass: +4 dB @ 200 Hz
- Treble: +6 dB @ 6 kHz
- Limiter: -2 dB, ratio 20:1
- Sustain target: -8 dB, max gain 12
- Reverb: 35 ms, feedback 0.18, wet 0.08

## Important
The source APK is a WebView wrapper, so this JS works there by overriding `navigator.mediaDevices.getUserMedia()` and returning a processed MediaStream.

Telegram Android 12.10.6 is native and uses its VoIP/native audio pipeline. Simply inserting this JS into the APK will NOT make the effect work. A functional native build must insert equivalent DSP into the microphone capture path (or the tgcalls/WebRTC audio processing path) and add the settings UI.

This folder intentionally does not claim to be a working modified Telegram APK.
