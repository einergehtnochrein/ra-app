package de.leckasemmel.sonde1;

/* Based on the "Walkie-Talkie" app:
   https://github.com/murtaza98/Walkie-Talkie/blob/master/app/src/main/java/com/example/murtaza/walkietalkie/AudioStreamingService.java
 */

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Base64;
import java.util.LinkedList;
import java.util.Queue;

public class MonitorService extends Service {
    private static final int SAMPLE_RATE = 8000;
    public boolean keepPlaying = true;
    private AudioTrack audioTrack = null;
    private final IBinder binder = new MonitorBinder();
    Queue<String> mQueue;
    private final Message mMsg = new Message();
    Boolean mEnd = false;
    MonitorCodec mCodec;


    public class MonitorBinder extends Binder {
        MonitorService getService() {
            return MonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mQueue = new LinkedList<>();
        mCodec = new MonitorCodec();

        startStreaming();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        keepPlaying = false;
        if (audioTrack != null) {
            audioTrack.release();
        }
    }

    public void startStreaming() {
        Runnable audioPlayerRunnable = () -> {
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                bufferSize = SAMPLE_RATE * 2;
            }

            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_UNKNOWN)
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();

            audioTrack.play();

            while (!mEnd) {
                synchronized(mMsg) {
                    try {
                        mMsg.wait(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Thread Interrupted");
                    }

                    String s = mQueue.poll();
                    if (s != null) {
                        int i, j;
                        byte[] adpcm = Base64.decode(s, Base64.DEFAULT);
                        int n = adpcm.length * 4;
                        short[] pcm = new short[n];
                        for (i = 0, j = 0; i < adpcm.length; i++) {
                            pcm[j++] = (short)mCodec.adpcm_decode((byte)(adpcm[i] >> 6));
                            pcm[j++] = (short)mCodec.adpcm_decode((byte)(adpcm[i] >> 4));
                            pcm[j++] = (short)mCodec.adpcm_decode((byte)(adpcm[i] >> 2));
                            pcm[j++] = (short)mCodec.adpcm_decode(       adpcm[i]);
                        }
                        try {
                            audioTrack.write(pcm, 0, n);
                        } catch (IllegalStateException e) {
                            System.err.println("Audio write pointer problem");
                        }
                    }
                }
            }
        };
        Thread t = new Thread(audioPlayerRunnable);
        t.start();
    }

    public void requestStop() {
        mEnd = true;
    }

    public void supplySamples(String uuEncodedSamples) {
        synchronized (mMsg) {
            mQueue.add(uuEncodedSamples);
            mMsg.notify();
        }
    }
}
