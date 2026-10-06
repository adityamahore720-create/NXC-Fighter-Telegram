#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("telegram")
APP = ROOT / "TMessagesProj"
HERE = Path(__file__).resolve().parent

# 12.10.6 does not contain Telegram's later AudioRecordJNI.java capture wrapper.
# Its native call stack uses the WebRTC Android AudioRecord implementation.
# Support both layouts so the patch fails clearly if Telegram changes the path.
legacy = list(APP.rglob("AudioRecordJNI.java"))
webrtc = list(APP.rglob("WebRtcAudioRecord.java"))

if legacy:
    capture = legacy[0]
    capture_kind = "legacy"
elif webrtc:
    capture = webrtc[0]
    capture_kind = "webrtc"
else:
    raise SystemExit("NXC patch: no AudioRecordJNI.java or WebRtcAudioRecord.java found")

manifest = APP / "src/main/AndroidManifest.xml"
activity = APP / "src/main/java/org/telegram/ui/NxcFighterActivity.java"

# Processor is placed in the WebRTC package so the WebRTC Java target can compile
# without depending on Telegram's application package.
if capture_kind == "webrtc":
    processor = capture.parent / "NxcFighterProcessor.java"
else:
    processor = APP / "src/main/java/org/telegram/messenger/voip/NxcFighterProcessor.java"

def copy(name, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(HERE / name, dst)

copy("NxcFighterProcessor.java", processor)
copy("NxcFighterActivity.java", activity)

s = capture.read_text(encoding="utf-8")
if "NxcFighterProcessor" not in s:
    if capture_kind == "legacy":
        needle = "private long nativeInst;\n"
        if needle not in s:
            raise SystemExit("AudioRecordJNI.java: nativeInst anchor not found")
        s = s.replace(needle, needle + "private final NxcFighterProcessor nxcFighter = new NxcFighterProcessor();\n", 1)
        anchor = "nativeCallback(buffer);"
        if anchor not in s:
            raise SystemExit("AudioRecordJNI.java: nativeCallback anchor not found")
        s = s.replace(anchor, "nxcFighter.process(buffer, 960 * 2);\n\n" + anchor, 1)
    else:
        # WebRtcAudioRecord is in the same package as the processor.
        needle = "private final long nativeAudioRecord;\n"
        if needle not in s:
            raise SystemExit("WebRtcAudioRecord.java: nativeAudioRecord anchor not found")
        s = s.replace(needle, needle + "private final NxcFighterProcessor nxcFighter = new NxcFighterProcessor();\n", 1)
        anchor = "if (keepAlive) {\n\n nativeDataIsRecorded(bytesRead, nativeAudioRecord);"
        if anchor in s:
            s = s.replace(anchor, "if (keepAlive) {\n\n nxcFighter.process(byteBuffer, bytesRead);\n\n nativeDataIsRecorded(bytesRead, nativeAudioRecord);", 1)
        else:
            anchor = "if (keepAlive) {\nnativeDataIsRecorded(bytesRead, nativeAudioRecord);"
            if anchor not in s:
                raise SystemExit("WebRtcAudioRecord.java: nativeDataIsRecorded anchor not found")
            s = s.replace(anchor, "if (keepAlive) {\nnxcFighter.process(byteBuffer, bytesRead);\nnativeDataIsRecorded(bytesRead, nativeAudioRecord);", 1)

capture.write_text(s, encoding="utf-8")

m = manifest.read_text(encoding="utf-8")
if "NxcFighterActivity" not in m:
    anchor = "    </application>"
    entry = '''        <activity\n            android:name=".ui.NxcFighterActivity"\n            android:exported="true"\n            android:label="NXC Fighter ~ sysTEM">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n        </activity>\n'''
    if anchor not in m:
        raise SystemExit("AndroidManifest.xml: application close anchor not found")
    m = m.replace(anchor, entry + anchor, 1)
    manifest.write_text(m, encoding="utf-8")

print(f"NXC Fighter patch applied to {capture_kind}: {capture}")
print(f"Processor: {processor}")
