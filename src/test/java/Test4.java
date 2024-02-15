/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tritonus.share.TDebug;
import vavi.sound.sampled.mp3.Mp3LameFormatConversionProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.sampled.mp3.MpegAudioFileWriter.MP3;


/**
 * lame-java.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
class Test4 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "org\\.tritonus\\.share\\.TDebug#out");

        TDebug.TraceAudioConverter = false;
        TDebug.TraceCircularBuffer = false;
        TDebug.TraceAudioFileReader = false;
    }

    @Property
    String wav = "src/test/resources/test.wav";

    @Property
    String mp3raw = "src/test/resources/raw.mp3";

    @BeforeAll
    static void setupAll() throws Exception {
        Files.createDirectories(Paths.get("tmp"));
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        Test3 app = new Test3();
        PropsEntity.Util.bind(app);
        app.test2();
    }

    // TODO FileChannel#transfarXXX doesn't work???
    @Test
    @DisplayName("encoding")
    void test1() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Path.of(wav))));
        AudioFormat inFormat = ais.getFormat();
Debug.println(inFormat);
        AudioFormat outFormat = new AudioFormat(
                Mp3LameFormatConversionProvider.MPEG1L3,
                -1f,
                -1,
                2,
                -1,
                -1f,
                false);
Debug.println(outFormat);
        AudioInputStream aout = AudioSystem.getAudioInputStream(outFormat, ais);

        Path out = Paths.get("tmp", "out.mp3");
        OutputStream fos = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        byte[] buf = new byte[8192];
        while (true) {
            int r = aout.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            fos.write(buf, 0, r);
        }
        fos.close();
        aout.close();

        assertEquals(Checksum.getChecksum(out), Checksum.getChecksum(Paths.get(mp3raw)));
    }

    @Test
    @DisplayName("writer")
    void test4() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Path.of(wav))));
        AudioFormat inFormat = ais.getFormat();
Debug.println(inFormat);
        AudioFormat outFormat = new AudioFormat(
                Mp3LameFormatConversionProvider.MPEG1L3,
                inFormat.getSampleRate(),
                -1,
                inFormat.getChannels(),
                -1,
                -1f,
                false);
Debug.println(outFormat);
        AudioInputStream aout = AudioSystem.getAudioInputStream(outFormat, ais);

        Path out2 = Paths.get("tmp", "out2.mp3");
        AudioSystem.write(aout, MP3, new BufferedOutputStream(Files.newOutputStream(out2)));

        assertEquals(Checksum.getChecksum(out2), Checksum.getChecksum(Paths.get(mp3raw)));
    }

    @Test
    @DisplayName("writer")
    void test3() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Path.of(mp3raw))));
        AudioFormat inFormat = ais.getFormat();
Debug.println(inFormat);

        Path out2 = Paths.get("tmp", "out2.mp3");
        AudioSystem.write(ais, MP3, new BufferedOutputStream(Files.newOutputStream(out2)));

        assertEquals(Checksum.getChecksum(out2), Checksum.getChecksum(Paths.get(mp3raw)));
    }
}

/* */
