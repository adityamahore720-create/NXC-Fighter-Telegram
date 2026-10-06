package org.telegram.ui;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.voiceengine.NxcFighterProcessor;

public class NxcFighterActivity extends Activity {
    private final SharedPreferences prefs() {
        return getSharedPreferences("nxc_fighter", MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("NXC Fighter ~ sysTEM");

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 24, 28, 24);
        root.setBackgroundColor(Color.BLACK);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("NXC Fighter ~ sysTEM");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp());

        CheckBox enabled = new CheckBox(this);
        enabled.setText("Enable NXC Fighter");
        enabled.setTextColor(Color.WHITE);
        enabled.setChecked(prefs().getBoolean("enabled", true));
        enabled.setOnCheckedChangeListener((v, checked) ->
                NxcFighterProcessor.setBoolean("enabled", checked));
        root.addView(enabled, lp());

        add(root, "Gain (dB)", "gainDb", 0, 36, 24);
        add(root, "Loudness Trim", "loudness", 0, 12, 4);
        add(root, "Saturation Drive", "drive", 0, 100, 28, 100f);
        add(root, "Compressor Threshold (dB)", "thresholdDb", -60, -10, -38);
        add(root, "Presence EQ (dB)", "presenceDb", 0, 12, 8);
        add(root, "Bass EQ (dB)", "bassDb", 0, 12, 4);
        add(root, "Treble EQ (dB)", "trebleDb", 0, 12, 6);
        add(root, "Limiter Ceiling (dB)", "limiterDb", -12, 0, -2);

        CheckBox sustain = new CheckBox(this);
        sustain.setText("Sustain / Auto Gain");
        sustain.setTextColor(Color.WHITE);
        sustain.setChecked(prefs().getBoolean("sustain", true));
        sustain.setOnCheckedChangeListener((v, checked) ->
                NxcFighterProcessor.setBoolean("sustain", checked));
        root.addView(sustain, lp());

        CheckBox reverb = new CheckBox(this);
        reverb.setText("Reverb");
        reverb.setTextColor(Color.WHITE);
        reverb.setChecked(prefs().getBoolean("reverbEnabled", true));
        reverb.setOnCheckedChangeListener((v, checked) ->
                NxcFighterProcessor.setBoolean("reverbEnabled", checked));
        root.addView(reverb, lp());

        add(root, "Reverb Delay (ms)", "reverbDelayMs", 5, 80, 35);
        add(root, "Reverb Feedback (%)", "reverbFeedbackPct", 0, 50, 18);
        add(root, "Reverb Wet (%)", "reverbWetPct", 0, 30, 8);

        Button close = new Button(this);
        close.setText("CLOSE");
        close.setOnClickListener(v -> finish());
        root.addView(close, lp());

        setContentView(scroll);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void add(LinearLayout root, String label, String key,
                     int min, int max, int def) {
        add(root, label, key, min, max, def, 1f);
    }

    private void add(LinearLayout root, String label, String key,
                     int min, int max, int def, float divisor) {
        TextView tv = new TextView(this);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        float stored;
        if (key.equals("reverbDelayMs")) {
            stored = prefs().getFloat("reverbDelay", def / 1000f) * 1000f;
        } else if (key.equals("reverbFeedbackPct")) {
            stored = prefs().getFloat("reverbFeedback", def / 100f) * 100f;
        } else if (key.equals("reverbWetPct")) {
            stored = prefs().getFloat("reverbWet", def / 100f) * 100f;
        } else {
            stored = prefs().getFloat(key, def);
            if (key.equals("drive")) stored *= 100f;
        }
        int progress = Math.max(0, Math.min(max - min, Math.round(stored) - min));
        bar.setProgress(progress);
        updateLabel(tv, label, min + bar.getProgress(), key);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int value = min + p;
                updateLabel(tv, label, value, key);
                if (!fromUser) return;
                if (key.equals("drive")) {
                    NxcFighterProcessor.setFloat(key, value / 100f);
                } else if (key.equals("reverbDelayMs")) {
                    NxcFighterProcessor.setFloat("reverbDelay", value / 1000f);
                } else if (key.equals("reverbFeedbackPct")) {
                    NxcFighterProcessor.setFloat("reverbFeedback", value / 100f);
                } else if (key.equals("reverbWetPct")) {
                    NxcFighterProcessor.setFloat("reverbWet", value / 100f);
                } else {
                    NxcFighterProcessor.setFloat(key, value);
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(tv, lp());
        root.addView(bar, lp());
    }

    private void updateLabel(TextView tv, String label, int value, String key) {
        if (key.equals("drive")) tv.setText(label + ": " + (value / 100f));
        else if (key.equals("reverbDelayMs"))
            tv.setText(label + ": " + value + " ms");
        else if (key.equals("reverbFeedbackPct") || key.equals("reverbWetPct"))
            tv.setText(label + ": " + value + "%");
        else tv.setText(label + ": " + value);
    }
}
