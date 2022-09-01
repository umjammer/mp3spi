/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.spi.AudioFileReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static javazoom.spi.mpeg.sampled.file.PlayerTest.volume;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static vavix.util.DelayedWorker.later;

/**
 * line.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
class Test3 {

    @Property
    String inFile = "src/test/resources/test.mp3";

    /** */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        Test3 app = new Test3();
        PropsEntity.Util.bind(app);
        app.test2();
    }

    /** play time limit in milliseconds */
    long time;

    @BeforeEach
    void setup() {
        time = Boolean.parseBoolean(System.getProperty("vavi.test")) ? 3 * 1000 : 600 * 1000;
Debug.println("time: " + time);
    }

    @Test
    @DisplayName("just play")
    void test2() throws Exception {
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(Paths.get(inFile).toFile());
        play(originalAudioInputStream);
    }

    void play(AudioInputStream originalAudioInputStream) throws Exception {
    AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat( //PCM
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
Debug.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
Debug.println("done");
            }
        });

        byte[] buf = new byte[8192];
        line.open(audioFormat, buf.length);
        volume(line, .2d);
        line.start();
        int r = 0;
        while (!later(time).come()) {
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
    @DisplayName("https://github.com/umjammer/mp3spi/issues/5")
    void test3() throws Exception {
        try {
            Path in = Paths.get(Test3.class.getResource("/test.mid").toURI());
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(in)));
Debug.println(ais);
        } catch (EOFException e) {
            e.printStackTrace();
            fail("spi cosumes all bytes, and eof make stream unresettable");
        }
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes", "restriction" })
    void test4() throws Exception {
        List<AudioFileReader> providers = (List) com.sun.media.sound.JDK13Services.getProviders(AudioFileReader.class);
providers.forEach(System.err::println);
        assertTrue(providers.stream().map(o -> o.getClass().getName()).anyMatch(s -> s.contains("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader")));
    }

    @Test
    void test5() throws Exception {
        String file = "src/test/resources/test2.mp3";
        InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(file)));
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(is);
        play(audioInputStream);
    }

    @Test
    void test6() throws Exception {
        String file = "src/test/resources/test2.mp3";
        InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(file)));
        AudioFileFormat format = AudioSystem.getAudioFileFormat(is);
format.properties().forEach((k, v) -> { System.err.println(k + ": " + v);});
        String genre = (String) format.properties().get("mp3.id3tag.genre");
Debug.println("genre: " + genre);
        assertEquals("Pop", genre);
        assertEquals("日本語", format.properties().get("title"));
        assertEquals("アルバム", format.properties().get("album"));
        assertEquals("にほんご", format.properties().get("author"));
        assertEquals("コメント", format.properties().get("comment"));
    }

    @Test
    @Disabled
    @DisplayName("test all mp3s in your itumes music")
    void test() throws IOException {
        Path root = Paths.get(System.getProperty("user.home"), "Music", "iTunes", "iTunes Music");
Debug.println("ROOT: " + Files.exists(root));

        AtomicInteger count = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                if (file.getFileName().toString().toLowerCase().endsWith(".mp3")) {
//Debug.println(file);
                    count.incrementAndGet();
                    try {
                        AudioSystem.getAudioInputStream(file.toFile());
                    } catch (Exception e) {
                        try {
Debug.println("ERROR: " + file + ", " + Files.size(file));
                        } catch (Exception f) {}
                        error.incrementAndGet();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
Debug.println("RESULT: " + error + "/" + count);
    }
}

/* */
