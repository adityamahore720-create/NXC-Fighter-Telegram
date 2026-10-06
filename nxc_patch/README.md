# NXC Fighter ~ sysTEM native patch

This patch adds the supplied audio-processing concept to Telegram's native microphone
PCM path and adds an Android control panel.

It is designed to be applied after cloning Telegram commit `f2908b1`.

Files:
- `NxcFighterProcessor.java`
- `NxcFighterActivity.java`
- `apply_nxc_patch.py`

The processor intentionally does not inject keep-alive noise into the microphone stream.
That would add an artificial signal to calls. The remaining controls are implemented as
real PCM processing: gain/loudness, tone shaping, compression, saturation, sustain/auto
gain, limiter and optional short reverb.

Run from the repository root:
`python3 nxc_patch/apply_nxc_patch.py`
