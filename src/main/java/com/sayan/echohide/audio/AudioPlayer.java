package com.sayan.echohide.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.File;

/**
 * Audio playback helper using javax.sound.sampled.Clip.
 */
public class AudioPlayer {

    private Clip clip;
    private File currentFile;
    private Runnable onCompleteCallback;

    public synchronized void play(File audioFile, Runnable onComplete) throws Exception {
        stop();
        this.currentFile = audioFile;
        this.onCompleteCallback = onComplete;

        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        clip = AudioSystem.getClip();
        clip.open(ais);

        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                if (clip != null && clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                    if (onCompleteCallback != null) {
                        onCompleteCallback.run();
                    }
                }
            }
        });

        clip.start();
    }

    public synchronized void stop() {
        if (clip != null) {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.close();
            } catch (Exception ignored) {
            } finally {
                clip = null;
            }
        }
    }

    public synchronized boolean isPlaying() {
        return clip != null && clip.isRunning();
    }

    public File getCurrentFile() {
        return currentFile;
    }
}

