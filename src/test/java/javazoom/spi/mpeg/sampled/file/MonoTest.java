/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package javazoom.spi.mpeg.sampled.file;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static vavi.sound.SoundUtil.volume;


/**
 * MonoTest.
 *
 * TODO volume for clip doesn't work on mac (oracle 1.8.0_291)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/11/23 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class MonoTest {

    static final Logger logger = Logger.getLogger(SkipTest.class.getName());

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    /** play frames limit */
    static int frames;

    @BeforeEach
    void before() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.print("volume: " + volume);
    }

    @BeforeAll
    static void setup() {
        frames = System.getProperty("vavi.test", "").equals("ide") ? 1000 : 2;
    }

    @Test
    @DisplayName("mono -> mono")
    void test1() throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(MonoTest.class.getResourceAsStream("/mono.mp3"));
        AudioFormat inFormat = in.getFormat();
logger.info("In Format: " + inFormat.toString());
        AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                inFormat.getSampleRate(),
                                                16,
                                                inFormat.getChannels(),
                                                inFormat.getChannels(),
                                                inFormat.getSampleRate(),
                                                false);
logger.info("Out Format: " + outFormat);
        AudioInputStream out = AudioSystem.getAudioInputStream(outFormat, in);
        Clip c = AudioSystem.getClip();
        c.open(out);
        volume(c, volume); // doesn't work for clip
        c.start();
        Thread.sleep((long) (c.getFrameLength() / 44100.) * frames);
    }

    @Test
    @DisplayName("stereo -(mp3spi)-> stereo -(tritnus-remaining)-> mono")
    void test2() throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(MonoTest.class.getResourceAsStream("/test.mp3"));
        AudioFormat inFormat = in.getFormat();
        logger.info("In Format: " + inFormat.toString());
        AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                inFormat.getSampleRate(),
                                                16,
                                                2,
                                                4,
                                                inFormat.getSampleRate(),
                                                false);
logger.info("Out Format: " + outFormat);
        AudioInputStream out = AudioSystem.getAudioInputStream(outFormat, in);
        AudioFormat outFormat2 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                 outFormat.getSampleRate(),
                                                 16,
                                                 1,
                                                 2,
                                                 outFormat.getSampleRate(),
                                                 false);
logger.info("Out Format 2: " + outFormat2);
        AudioInputStream out2 = AudioSystem.getAudioInputStream(outFormat2, out);
        Clip c = AudioSystem.getClip();
        c.open(out2);
        volume(c, volume); // doesn't work for clip
        c.start();
        Thread.sleep((long) (c.getFrameLength() / 44100.) * frames);
    }

    @Test
    @DisplayName("mono -(mp3spi)-> mono -(tritnus-remaining)-> stereo")
    void test3() throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(MonoTest.class.getResourceAsStream("/mono.mp3"));
        AudioFormat inFormat = in.getFormat();
        logger.info("In Format: " + inFormat.toString());
        AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                inFormat.getSampleRate(),
                                                16,
                                                1,
                                                2,
                                                inFormat.getSampleRate(),
                                                false);
logger.info("Out Format: " + outFormat);
        AudioInputStream out = AudioSystem.getAudioInputStream(outFormat, in);
        AudioFormat outFormat2 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                 outFormat.getSampleRate(),
                                                 16,
                                                 2,
                                                 4,
                                                 outFormat.getSampleRate(),
                                                 false);
logger.info("Out Format 2: " + outFormat2);
        AudioInputStream out2 = AudioSystem.getAudioInputStream(outFormat2, out);
        Clip c = AudioSystem.getClip();
        c.open(out2);
        volume(c, volume); // doesn't work for clip
        c.start();
        Thread.sleep((long) (c.getFrameLength() / 44100.) * frames);
    }

    @Test
    @DisplayName("stereo -> mono -> fail")
    void test4() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            AudioInputStream in = AudioSystem.getAudioInputStream(MonoTest.class.getResourceAsStream("/test.mp3"));
            AudioFormat inFormat = in.getFormat();
            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                    inFormat.getSampleRate(),
                                                    16,
                                                    1,
                                                    2,
                                                    inFormat.getSampleRate(),
                                                    false);
            AudioSystem.getAudioInputStream(outFormat, in);
        });
    }

    @Test
    @DisplayName("mono -> stereo -> fail")
    void test5() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            AudioInputStream in = AudioSystem.getAudioInputStream(MonoTest.class.getResourceAsStream("/mono.mp3"));
            AudioFormat inFormat = in.getFormat();
            AudioFormat outFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                    inFormat.getSampleRate(),
                                                    16,
                                                    2,
                                                    4,
                                                    inFormat.getSampleRate(),
                                                    false);
            AudioSystem.getAudioInputStream(outFormat, in);
        });
    }
}
