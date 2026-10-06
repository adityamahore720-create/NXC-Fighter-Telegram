#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("telegram")
APP = ROOT / "TMessagesProj"

src = APP / "src/main/java/org/telegram/messenger/voip/AudioRecordJNI.java"
voip = APP / "src/main/java/org/telegram/messenger/voip/VoIPService.java"
manifest = APP / "src/main/AndroidManifest.xml"
activity = APP / "src/main/java/org/telegram/ui/NxcFighterActivity.java"
processor = APP / "src/main/java/org/telegram/messenger/voip/NxcFighterProcessor.java"

here = Path(__file__).resolve().parent

def copy(name, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(here / name, dst)

copy("NxcFighterProcessor.java", processor)
copy("NxcFighterActivity.java", activity)

# Patch the native AudioRecord -> tgcalls PCM handoff.
s = src.read_text()
if "NxcFighterProcessor nxcFighter" not in s:
    needle = "private long nativeInst;\n"
    if needle not in s:
        raise SystemExit("AudioRecordJNI.java: nativeInst anchor not found")
    s = s.replace(needle, needle + "private final NxcFighterProcessor nxcFighter = new NxcFighterProcessor();\n", 1)

    old = """if (!running) {
                        audioRecord.stop();
                        break;
                    }
                    nativeCallback(buffer);"""
    new = """if (!running) {
                        audioRecord.stop();
                        break;
                    }
                    nxcFighter.process(buffer, 960 * 2);
                    nativeCallback(buffer);"""
    if old not in s:
        # tolerate different indentation
        old2 = """if (!running) {
                    audioRecord.stop();
                    break;
                }
                nativeCallback(buffer);"""
        new2 = """if (!running) {
                    audioRecord.stop();
                    break;
                }
                nxcFighter.process(buffer, 960 * 2);
                nativeCallback(buffer);"""
        if old2 not in s:
            raise SystemExit("AudioRecordJNI.java: nativeCallback anchor not found")
        s = s.replace(old2, new2, 1)
    else:
        s = s.replace(old, new, 1)
    src.write_text(s)

# Add the panel as an exported activity. This gives the user a direct launcher
# entry in Telegram without depending on fragile call-screen internals.
m = manifest.read_text()
if "NxcFighterActivity" not in m:
    anchor = "    </application>"
    entry = """        <activity
            android:name=".ui.NxcFighterActivity"
            android:exported="true"
            android:label="NXC Fighter ~ sysTEM">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
"""
    if anchor not in m:
        raise SystemExit("AndroidManifest.xml: application close anchor not found")
    m = m.replace(anchor, entry + anchor, 1)
    manifest.write_text(m)

print("NXC Fighter patch applied:")
print(" - native PCM processing in AudioRecordJNI")
print(" - NXC Fighter ~ sysTEM control panel activity")
print(" - launcher entry in Telegram")
