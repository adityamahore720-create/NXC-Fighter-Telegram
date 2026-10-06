# Native integration plan

1. Add an `AudioBoostConfig` object for the exposed sliders.
2. Add a native DSP processor in the microphone capture path used by Telegram VoIP.
3. Port the WebAudio graph to native processing:
   - high-pass 75 Hz
   - low shelf 200 Hz
   - peaking 3.2 kHz
   - high shelf 6 kHz
   - two compressors
   - gain/loudness
   - waveshaper saturation
   - sustain/auto-level loop
   - feedback delay/reverb
   - hard limiter
4. Add the panel UI and persist values.
5. Feed processed PCM into the existing Telegram VoIP capture stream.
6. Build all required native/ABI splits and sign the resulting APK set.

Telegram Android's official source exposes the VoIP service/native instance and is buildable from source; the native route is therefore the correct integration point.
