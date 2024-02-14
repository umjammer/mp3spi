/*
 *   MpegAudioFileReaderTest - JavaZOOM : http://www.javazoom.net
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package javazoom.spi.mpeg.sampled.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * MpegAudioFileReader unit test.
 * It matches test.mp3 properties to test.mp3.properties expected results.
 */
class MpegAudioFileReaderTest {

    private static Logger logger = Logger.getLogger(MpegAudioFileReaderTest.class.getName());

    private String basefile = null;
    private String baseurl = null;
    private String filename = null;
    private String fileurl = null;
    private String name = null;
    private Properties props = null;

    @BeforeEach
    void setUp() throws Exception {
        props = new Properties();
        InputStream pin = getClass().getClassLoader().getResourceAsStream("test.mp3.properties");
        props.load(pin);
        basefile = props.getProperty("basefile");
        baseurl = props.getProperty("baseurl").replaceAll("\\$\\{PWD\\}", System.getProperty("user.dir"));
        name = props.getProperty("filename");
        filename = basefile + name;
        fileurl = baseurl + name;
    }

    @DisplayName("Test for AudioFileFormat getAudioFileFormat(File)")
    @Test
    void _testGetAudioFileFormatFile() {
        logger.info("*** testGetAudioFileFormatFile ***");
        try {
            File file = new File(filename);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
            dumpAudioFileFormat(baseFileFormat, file.toString());
            assertEquals(Integer.parseInt(props.getProperty("FrameLength")), baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("ByteLength")), baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioFileFormatFile: " + e);
        }
    }

    @DisplayName("Test for AudioFileFormat getAudioFileFormat(URL)")
    @Test
    void _testGetAudioFileFormatURL() {
        logger.info("*** testGetAudioFileFormatURL ***");
        try {
            URL url = new URL(fileurl);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
            dumpAudioFileFormat(baseFileFormat, url.toString());
            assertEquals(-1, baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(-1, baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioFileFormatURL: " + e);
        }
    }

    @DisplayName("Test for AudioFileFormat getAudioFileFormat(InputStream)")
    @Test
    void _testGetAudioFileFormatInputStream() {
        logger.info("*** testGetAudioFileFormatInputStream ***");
        try {
            InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(filename)));
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(in);
            dumpAudioFileFormat(baseFileFormat, in.toString());
            in.close();
            assertEquals(-1, baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(-1, baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioFileFormatInputStream: " + e);
        }
    }

    @DisplayName("Test for AudioInputStream getAudioInputStream(InputStream)")
    @Test
    void _testGetAudioInputStreamInputStream() {
        logger.info("*** testGetAudioInputStreamInputStream ***");
        try {
            InputStream fin = new BufferedInputStream(Files.newInputStream(Paths.get(filename)));
            AudioInputStream in = AudioSystem.getAudioInputStream(fin);
            dumpAudioInputStream(in, fin.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            fin.close();
            in.close();
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioInputStreamInputStream: " + e);
        }
    }

    @DisplayName("Test for AudioInputStream getAudioInputStream(File)")
    @Test
    void _testGetAudioInputStreamFile() {
        logger.info("*** testGetAudioInputStreamFile ***");
        try {
            File file = new File(filename);
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            dumpAudioInputStream(in, file.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            in.close();
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioInputStreamFile:" + e);
        }
    }

    @DisplayName("Test for AudioInputStream getAudioInputStream(URL)")
    @Test
    void _testGetAudioInputStreamURL() {
        logger.info("*** testGetAudioInputStreamURL ***");
        try {
            URL url = new URL(fileurl);
            AudioInputStream in = AudioSystem.getAudioInputStream(url);
            dumpAudioInputStream(in, url.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            in.close();
        } catch (UnsupportedAudioFileException | IOException e) {
            fail("testGetAudioInputStreamURL: " + e);
        }
    }

    private void dumpAudioFileFormat(AudioFileFormat baseFileFormat,
                                     String info) throws UnsupportedAudioFileException {
        AudioFormat baseFormat = baseFileFormat.getFormat();
        // AudioFileFormat
        logger.info("  -----  " + info + "  -----");
        logger.info("    ByteLength=" + baseFileFormat.getByteLength());
        logger.info("    FrameLength=" + baseFileFormat.getFrameLength());
        logger.info("    Type=" + baseFileFormat.getType());
        // AudioFormat
        logger.info("    SourceFormat=" + baseFormat.toString());
        logger.info("    Channels=" + baseFormat.getChannels());
        logger.info("    FrameRate=" + baseFormat.getFrameRate());
        logger.info("    FrameSize=" + baseFormat.getFrameSize());
        logger.info("    SampleRate=" + baseFormat.getSampleRate());
        logger.info("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
        logger.info("    Encoding=" + baseFormat.getEncoding());
        assertEquals(props.getProperty("Type"), baseFileFormat.getType().toString(), "Type");
        assertEquals(props.getProperty("SourceFormat"), baseFormat.toString(), "SourceFormat");
        assertEquals(Integer.parseInt(props.getProperty("Channels")), baseFormat.getChannels(), "Channels");
        assertEquals(Float.parseFloat(props.getProperty("FrameRate")), baseFormat.getFrameRate(), "FrameRate");
        assertEquals(Integer.parseInt(props.getProperty("FrameSize")), baseFormat.getFrameSize(), "FrameSize");
        assertEquals(Float.parseFloat(props.getProperty("SampleRate")), baseFormat.getSampleRate(), "SampleRate");
        assertEquals(Integer.parseInt(props.getProperty("SampleSizeInBits")),
                     baseFormat.getSampleSizeInBits(),
                     "SampleSizeInBits");
        assertEquals(props.getProperty("Encoding"), baseFormat.getEncoding().toString(), "Encoding");
    }

    private void dumpAudioInputStream(AudioInputStream in, String info) throws IOException {
        AudioFormat baseFormat = in.getFormat();
        logger.info("  -----  " + info + "  -----");
        logger.info("    Available=" + in.available());
        logger.info("    FrameLength=" + in.getFrameLength());
        // AudioFormat
        logger.info("    SourceFormat=" + baseFormat.toString());
        logger.info("    Channels=" + baseFormat.getChannels());
        logger.info("    FrameRate=" + baseFormat.getFrameRate());
        logger.info("    FrameSize=" + baseFormat.getFrameSize());
        logger.info("    SampleRate=" + baseFormat.getSampleRate());
        logger.info("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
        logger.info("    Encoding=" + baseFormat.getEncoding());
        assertEquals(props.getProperty("SourceFormat"), baseFormat.toString(), "SourceFormat");
        assertEquals(Integer.parseInt(props.getProperty("Channels")), baseFormat.getChannels(), "Channels");
        assertEquals(Float.parseFloat(props.getProperty("FrameRate")), baseFormat.getFrameRate(), "FrameRate");
        assertEquals(Integer.parseInt(props.getProperty("FrameSize")), baseFormat.getFrameSize(), "FrameSize");
        assertEquals(Float.parseFloat(props.getProperty("SampleRate")), baseFormat.getSampleRate(), "SampleRate");
        assertEquals(Integer.parseInt(props.getProperty("SampleSizeInBits")),
                     baseFormat.getSampleSizeInBits(),
                     "SampleSizeInBits");
        assertEquals(props.getProperty("Encoding"), baseFormat.getEncoding().toString(), "Encoding");
    }
}
