/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package javazoom.spi.mpeg.sampled.convert;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javazoom.spi.mpeg.sampled.file.MpegEncoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * What this provider says it can do must be what it can do. It used to say yes to any target
 * format with the same channel count as the source, whatever its encoding, and at the same
 * time no to every target encoding, which made
 * {@code AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, mp3)} fail.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260726 nsano initial version <br>
 */
class MpegFormatConversionProviderTest {

    private static final Logger logger = Logger.getLogger(MpegFormatConversionProviderTest.class.getName());

    /** test.mp3 is stereo, mono.mp3 is monaural */
    static AudioInputStream stream(String name) throws Exception {
        InputStream in = MpegFormatConversionProviderTest.class.getResourceAsStream(name);
        return AudioSystem.getAudioInputStream(new BufferedInputStream(in));
    }

    static AudioFormat pcm(float sampleRate, int channels) {
        return new AudioFormat(Encoding.PCM_SIGNED, sampleRate, 16, channels, channels * 2, sampleRate, false);
    }

    @Test
    @DisplayName("the encodings of the source formats are the ones the reader produces")
    void test1() throws Exception {
        MpegFormatConversionProvider provider = new MpegFormatConversionProvider();
        assertTrue(provider.isSourceEncodingSupported(MpegEncoding.MPEG1L3));
        assertTrue(provider.isSourceEncodingSupported(MpegEncoding.MPEG2DOT5L3));
        assertTrue(provider.isTargetEncodingSupported(Encoding.PCM_SIGNED));
    }

    @Test
    @DisplayName("says yes to the target encoding it can decode to")
    void test2() throws Exception {
        AudioFormat sourceFormat = stream("/test.mp3").getFormat();
logger.info("In Format: " + sourceFormat);

        assertTrue(AudioSystem.isConversionSupported(Encoding.PCM_SIGNED, sourceFormat));
        assertTrue(List.of(AudioSystem.getTargetEncodings(sourceFormat)).contains(Encoding.PCM_SIGNED));

        try (AudioInputStream out = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, stream("/test.mp3"))) {
logger.info("Out Format: " + out.getFormat());
            assertEquals(Encoding.PCM_SIGNED, out.getFormat().getEncoding());
            assertEquals(sourceFormat.getSampleRate(), out.getFormat().getSampleRate());
            assertEquals(sourceFormat.getChannels(), out.getFormat().getChannels());
            assertEquals(16, out.getFormat().getSampleSizeInBits());
            assertTrue(out.readNBytes(0x10000).length > 0);
        }
    }

    @Test
    @DisplayName("says no to a target encoding it knows nothing about")
    void test3() throws Exception {
        AudioFormat sourceFormat = stream("/test.mp3").getFormat();
        AudioFormat targetFormat = new AudioFormat(new Encoding("NOWHERE"),
                sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(), false);

        assertFalse(AudioSystem.isConversionSupported(targetFormat, sourceFormat));
        assertFalse(AudioSystem.isConversionSupported(new Encoding("NOWHERE"), sourceFormat));
        assertThrows(IllegalArgumentException.class, () -> AudioSystem.getAudioInputStream(targetFormat, stream("/test.mp3")));
    }

    @Test
    @DisplayName("the advertised target formats are the ones it really produces")
    void test4() throws Exception {
        AudioFormat sourceFormat = stream("/test.mp3").getFormat();

        AudioFormat[] targetFormats = AudioSystem.getTargetFormats(Encoding.PCM_SIGNED, sourceFormat);
        assertTrue(targetFormats.length > 0);
        for (AudioFormat targetFormat : targetFormats) {
logger.info("Target Format: " + targetFormat);
            assertEquals(sourceFormat.getSampleRate(), targetFormat.getSampleRate());
            assertEquals(sourceFormat.getChannels(), targetFormat.getChannels());
            assertEquals(16, targetFormat.getSampleSizeInBits());
            assertTrue(AudioSystem.isConversionSupported(targetFormat, sourceFormat));
            try (AudioInputStream out = AudioSystem.getAudioInputStream(targetFormat, stream("/test.mp3"))) {
                assertTrue(targetFormat.matches(out.getFormat()));
                assertTrue(out.readNBytes(0x1000).length > 0);
            }
        }
    }

    @Test
    @DisplayName("still cannot change the channel count on its own")
    void test5() throws Exception {
        AudioFormat stereo = stream("/test.mp3").getFormat();
        AudioFormat mono = stream("/mono.mp3").getFormat();

        MpegFormatConversionProvider provider = new MpegFormatConversionProvider();
        assertFalse(provider.isConversionSupported(pcm(stereo.getSampleRate(), 1), stereo));
        assertFalse(provider.isConversionSupported(pcm(mono.getSampleRate(), 2), mono));
    }

    @Test
    @DisplayName("does not resample either")
    void test6() throws Exception {
        AudioFormat sourceFormat = stream("/test.mp3").getFormat();

        MpegFormatConversionProvider provider = new MpegFormatConversionProvider();
        assertFalse(provider.isConversionSupported(pcm(22050, sourceFormat.getChannels()), sourceFormat));
        assertTrue(provider.isConversionSupported(pcm(sourceFormat.getSampleRate(), sourceFormat.getChannels()), sourceFormat));
    }
}
