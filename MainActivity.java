package com.sevenverse.voicechanger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_MIC = 100;
    private static final int SAMPLE_RATE = 44100;
    private volatile boolean running = false;
    private Thread audioThread;
    private AudioRecord recorder;
    private AudioTrack track;
    private volatile String effect = "Normal";
    private volatile float pitch = 1.0f;
    private volatile float wet = 0.65f;
    private TextView status;
    private Button startButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFF101522);

        TextView title = new TextView(this);
        title.setText("VOICE CHANGER");
        title.setTextSize(25);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Prototype real-time • OPPO A18");
        subtitle.setTextColor(0xFFB7C3D8);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("Status: berhenti");
        status.setTextColor(0xFF8BE9FD);
        status.setPadding(0, 24, 0, 12);
        root.addView(status, matchWrap());

        startButton = new Button(this);
        startButton.setText("MULAI MIC");
        startButton.setOnClickListener(v -> {
            if (running) stopAudio(); else startAudio();
        });
        root.addView(startButton, matchWrap());

        TextView effects = label("Pilih efek suara");
        root.addView(effects, matchWrap());
        String[] names = {"Normal", "Deep", "High", "Robot", "Monster"};
        for (String name : names) {
            Button b = new Button(this);
            b.setText(name);
            b.setOnClickListener(v -> {
                effect = name;
                status.setText("Status: " + (running ? "aktif" : "berhenti") + " • Efek: " + effect);
            });
            root.addView(b, matchWrap());
        }

        root.addView(label("Pitch / nada"), matchWrap());
        SeekBar pitchBar = new SeekBar(this);
        pitchBar.setMax(100);
        pitchBar.setProgress(50);
        pitchBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                pitch = 0.65f + (p / 100f) * 0.8f;
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(pitchBar, matchWrap());

        root.addView(label("Kekuatan efek"), matchWrap());
        SeekBar wetBar = new SeekBar(this);
        wetBar.setMax(100);
        wetBar.setProgress(65);
        wetBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) { wet = p / 100f; }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(wetBar, matchWrap());

        TextView note = new TextView(this);
        note.setText("Catatan: efek diproses saat mic aktif. Android biasanya tidak mengizinkan aplikasi ini mengganti input mic MLBB secara langsung. Untuk voice chat game, coba fitur Voice Changer di Game Toolkit OPPO jika tersedia.");
        note.setTextColor(0xFFFFD28A);
        note.setTextSize(13);
        note.setPadding(0, 20, 0, 0);
        root.addView(note, matchWrap());

        setContentView(root);
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        t.setPadding(0, 16, 0, 4);
        return t;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void startAudio() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        try {
            int minIn = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int minOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(Math.max(minIn, minOut), 4096);
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2);
            track = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2,
                    AudioTrack.MODE_STREAM);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED || track.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("Audio device tidak bisa diinisialisasi");
            }
            running = true;
            recorder.startRecording();
            track.play();
            startButton.setText("STOP MIC");
            status.setText("Status: aktif • Efek: " + effect);
            audioThread = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                short[] input = new short[1024];
                short[] output = new short[1024];
                double phase = 0;
                while (running) {
                    int n = recorder.read(input, 0, input.length);
                    if (n <= 0) continue;
                    String mode = effect;
                    float p = pitch;
                    float amount = wet;
                    for (int i = 0; i < n; i++) {
                        int src = Math.min(n - 1, Math.max(0, (int)(i * p)));
                        float x = input[src] / 32768.0f;
                        float y = x;
                        if ("Robot".equals(mode)) {
                            phase += 2.0 * Math.PI * 75.0 / SAMPLE_RATE;
                            y = (float)(x * Math.sin(phase));
                        } else if ("Monster".equals(mode)) {
                            phase += 2.0 * Math.PI * 38.0 / SAMPLE_RATE;
                            y = (float)(x * (0.55 + 0.45 * Math.sin(phase)));
                            y = (float)Math.tanh(y * 2.2);
                        } else if ("Deep".equals(mode)) {
                            y = x * 0.88f;
                        } else if ("High".equals(mode)) {
                            y = x * 0.82f;
                        }
                        float effected = x * (1f - amount) + y * amount;
                        output[i] = (short)(Math.max(-1f, Math.min(1f, effected)) * 32767);
                    }
                    track.write(output, 0, n);
                }
            }, "VoiceChangerAudio");
            audioThread.start();
        } catch (Exception e) {
            stopAudio();
            Toast.makeText(this, "Gagal mulai audio: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopAudio() {
        running = false;
        try { if (audioThread != null) audioThread.join(300); } catch (InterruptedException ignored) {}
        try { if (recorder != null) { recorder.stop(); recorder.release(); } } catch (Exception ignored) {}
        try { if (track != null) { track.stop(); track.release(); } } catch (Exception ignored) {}
        recorder = null; track = null; audioThread = null;
        if (startButton != null) startButton.setText("MULAI MIC");
        if (status != null) status.setText("Status: berhenti • Efek: " + effect);
    }

    @Override protected void onDestroy() { stopAudio(); super.onDestroy(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MIC && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin mic aktif. Tekan MULAI MIC.", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_MIC) {
            Toast.makeText(this, "Izin mic diperlukan untuk memproses suara.", Toast.LENGTH_LONG).show();
        }
    }
}
