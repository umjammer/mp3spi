/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * line.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
class Test3 {

    static final String inFile = "src/test/resources/test.mp3";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
System.err.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat( //PCM
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
System.err.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
System.err.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
System.err.println("done");
            }
        });

        byte[] buf = new byte[8192];
        line.open(audioFormat, buf.length);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        int r = 0;
        while (true) {
            r = audioInputStream.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.close();
    }

    @Test
    @Disabled
    void test() throws IOException {
        Path root = Paths.get(System.getProperty("user.home"), "Music", "iTunes", "iTunes Music");
System.err.println("ROOT: " + Files.exists(root));

        AtomicInteger count = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                if (file.getFileName().toString().toLowerCase().endsWith(".mp3")) {
//System.err.println(file);
                    count.incrementAndGet();
                    try {
                        AudioSystem.getAudioInputStream(file.toFile());
                    } catch (Exception e) {
                        try {
System.err.println("ERROR: " + file + ", " + Files.size(file));
                        } catch (Exception f) {}
                        error.incrementAndGet();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
System.err.println("RESULT: " + error + "/" + count);
    }
}

/* */
