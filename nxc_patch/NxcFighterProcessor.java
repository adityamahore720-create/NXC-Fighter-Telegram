package org.webrtc.voiceengine;

import android.content.Context;
import android.content.SharedPreferences;

import org.webrtc.ContextUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * NXC Fighter ~ sysTEM
 * Lightweight real-time mono PCM processor for Telegram's native microphone path.
 *
 * Audio is 48 kHz / 16-bit PCM. Parameters are stored in SharedPreferences so the
 * UI can change them without restarting the call.
 */
public final class NxcFighterProcessor {
    private static final String PREF = "nxc_fighter";
    private static final int SAMPLE_RATE = 48000;

    private final SharedPreferences prefs;
    private final float[] lowState = new float[2];
    private final float[] presenceState = new float[2];
    private final float[] highState = new float[2];
    private final float[] reverb = new float[2400];
    private int reverbPos;
    private float env;
    private float sustainGain = 1f;

    public NxcFighterProcessor() {
        Context c = ContextUtils.getApplicationContext();
        prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        reset();
    }

    public void reset() {
        lowState[0] = lowState[1] = 0;
        presenceState[0] = presenceState[1] = 0;
        highState[0] = highState[1] = 0;
        java.util.Arrays.fill(reverb, 0);
        reverbPos = 0;
        env = 0;
        sustainGain = 1f;
    }

    private float db(String key, float def) { return prefs.getFloat(key, def); }
    private boolean enabled() { return prefs.getBoolean("enabled", true); }

    public void process(ByteBuffer buf, int bytes) {
        if (!enabled() || bytes < 2) return;
        ByteOrder old = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        final float gain = dbToLinear(db("gainDb", 24f) + db("loudness", 4f));
        final float drive = Math.max(0f, db("drive", 0.28f));
        final float threshold = dbToLinear(db("thresholdDb", -38f));
        final float ratio = Math.max(1f, db("ratio", 12f));
        final float presence = db("presenceDb", 8f);
        final float bass = db("bassDb", 4f);
        final float treble = db("trebleDb", 6f);
        final float ceiling = dbToLinear(db("limiterDb", -2f));
        final boolean sustain = prefs.getBoolean("sustain", true);
        final boolean reverbOn = prefs.getBoolean("reverbEnabled", true);
        final float wet = clamp(prefs.getFloat("reverbWet", .08f), 0f, .35f);
        final int n = bytes / 2;

        // One-pole approximations of the three tone controls. They are intentionally
        // light-weight because this runs on the AudioRecord thread.
        final float lowA = 1f - (float)Math.exp(-2.0 * Math.PI * 200.0 / SAMPLE_RATE);
        final float highA = 1f - (float)Math.exp(-2.0 * Math.PI * 6000.0 / SAMPLE_RATE);
        final float presenceA = 1f - (float)Math.exp(-2.0 * Math.PI * 3200.0 / SAMPLE_RATE);

        for (int i = 0; i < n; i++) {
            float x = buf.getShort(i * 2) / 32768f;

            x *= gain;

            // Bass shelf approximation.
            lowState[0] += lowA * (x - lowState[0]);
            x += lowState[0] * (dbToLinear(bass) - 1f);

            // Presence emphasis around speech band.
            presenceState[0] += presenceA * (x - presenceState[0]);
            float presenceBand = x - presenceState[0];
            x += presenceBand * (dbToLinear(presence) - 1f) * .65f;

            // Treble shelf approximation.
            highState[0] += highA * (x - highState[0]);
            float highBand = x - highState[0];
            x += highBand * (dbToLinear(treble) - 1f) * .45f;

            // Fast compressor.
            float ax = Math.abs(x);
            if (ax > threshold) {
                float over = ax / Math.max(threshold, 1e-5f);
                float compressed = (float)Math.pow(over, 1f / ratio);
                x = Math.copySign(threshold * compressed, x);
            }

            // Soft saturation, matching the shape of the supplied JS implementation.
            if (drive > 0f) {
                float k = Math.max(.0001f, drive * 100f);
                x = (float)(((Math.PI + k) * x) /
                        (Math.PI + k * Math.abs(x)));
            }

            // Sustain/auto-gain toward approximately -8 dB RMS, capped at +12 dB.
            float abs = Math.abs(x);
            env += (abs - env) * .0025f;
            if (sustain) {
                float target = dbToLinear(-8f);
                float desired = target / Math.max(env, 0.0005f);
                desired = clamp(desired, 1f, dbToLinear(12f));
                sustainGain += (desired - sustainGain) * .003f;
                x *= sustainGain;
            }

            // Short feedback delay for the optional reverb ambience.
            if (reverbOn && wet > 0f) {
                int delay = Math.max(1, Math.min(reverb.length - 1,
                        Math.round(db("reverbDelay", .035f) * SAMPLE_RATE)));
                int read = reverbPos - delay;
                if (read < 0) read += reverb.length;
                float d = reverb[read];
                reverb[reverbPos] = x + d * clamp(db("reverbFeedback", .18f), 0f, .8f);
                x = x * (1f - wet) + d * wet;
                if (++reverbPos >= reverb.length) reverbPos = 0;
            }

            // Hard ceiling / limiter.
            x = clamp(x, -ceiling, ceiling);
            buf.putShort(i * 2, (short)(clamp(x, -1f, 1f) * 32767f));
        }
        buf.order(old);
    }

    private static float dbToLinear(float db) {
        return (float)Math.pow(10.0, db / 20.0);
    }

    private static float clamp(float x, float lo, float hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    public static void setFloat(String key, float value) {
        ContextUtils.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putFloat(key, value).apply();
    }

    public static void setBoolean(String key, boolean value) {
        ContextUtils.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).apply();
    }
}
