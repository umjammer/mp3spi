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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * MpegAudioFileReader unit test.
 * It matches test.mp3 properties to test.mp3.properties expected results.
 * As we don't ship test.mp3, you have to generate your own test.mp3.properties
 * Uncomment out = System.out; in setUp() method to generated it on stdout from
 * your own MP3 file.
 */
public class MpegAudioFileReaderTest {

    private String basefile = null;
    private String baseurl = null;
    private String filename = null;
    private String fileurl = null;
    private String name = null;
    private Properties props = null;
    private PrintStream out = null;

    @BeforeEach
    protected void setUp() throws Exception {
        props = new Properties();
        InputStream pin = getClass().getClassLoader().getResourceAsStream("test.mp3.properties");
        props.load(pin);
        basefile = props.getProperty("basefile");
        baseurl = props.getProperty("baseurl");
        name = props.getProperty("filename");
        filename = basefile + name;
        fileurl = baseurl + name;
//        out = System.out;
    }

    @Test
    public void testGetAudioFileFormat() {
        _testGetAudioFileFormatFile();
        _testGetAudioFileFormatURL();
        _testGetAudioFileFormatInputStream();
    }

    @Test
    public void testGetAudioInputStream() {
        _testGetAudioInputStreamFile();
        _testGetAudioInputStreamURL();
        _testGetAudioInputStreamInputStream();
    }

    /* Test for AudioFileFormat getAudioFileFormat(File) */
    public void _testGetAudioFileFormatFile() {
        if (out != null)
            out.println("*** testGetAudioFileFormatFile ***");
        try {
            File file = new File(filename);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
            dumpAudioFileFormat(baseFileFormat, out, file.toString());
            assertEquals(Integer.parseInt(props.getProperty("FrameLength")), baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("ByteLength")), baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioFileFormatFile:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioFileFormatFile:" + e.getMessage());
        }
    }

    /* Test for AudioFileFormat getAudioFileFormat(URL) */
    public void _testGetAudioFileFormatURL() {
        if (out != null)
            out.println("*** testGetAudioFileFormatURL ***");
        try {
            URL url = new URL(fileurl);
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
            dumpAudioFileFormat(baseFileFormat, out, url.toString());
            assertEquals(-1, baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(-1, baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioFileFormatURL:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioFileFormatURL:" + e.getMessage());
        }
    }

    /* Test for AudioFileFormat getAudioFileFormat(InputStream) */
    public void _testGetAudioFileFormatInputStream() {
        if (out != null)
            out.println("*** testGetAudioFileFormatInputStream ***");
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filename));
            AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(in);
            dumpAudioFileFormat(baseFileFormat, out, in.toString());
            in.close();
            assertEquals(-1, baseFileFormat.getFrameLength(), "FrameLength");
            assertEquals(-1, baseFileFormat.getByteLength(), "ByteLength");
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioFileFormatInputStream:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioFileFormatInputStream:" + e.getMessage());
        }
    }

    /* Test for AudioInputStream getAudioInputStream(InputStream) */
    public void _testGetAudioInputStreamInputStream() {
        if (out != null)
            out.println("*** testGetAudioInputStreamInputStream ***");
        try {
            InputStream fin = new BufferedInputStream(new FileInputStream(filename));
            AudioInputStream in = AudioSystem.getAudioInputStream(fin);
            dumpAudioInputStream(in, out, fin.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            fin.close();
            in.close();
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioInputStreamInputStream:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioInputStreamInputStream:" + e.getMessage());
        }
    }

    /* Test for AudioInputStream getAudioInputStream(File) */
    public void _testGetAudioInputStreamFile() {
        if (out != null)
            out.println("*** testGetAudioInputStreamFile ***");
        try {
            File file = new File(filename);
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            dumpAudioInputStream(in, out, file.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            in.close();
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioInputStreamFile:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioInputStreamFile:" + e.getMessage());
        }
    }

    /* Test for AudioInputStream getAudioInputStream(URL) */
    public void _testGetAudioInputStreamURL() {
        if (out != null)
            out.println("*** testGetAudioInputStreamURL ***");
        try {
            URL url = new URL(fileurl);
            AudioInputStream in = AudioSystem.getAudioInputStream(url);
            dumpAudioInputStream(in, out, url.toString());
            assertEquals(-1, in.getFrameLength(), "FrameLength");
            assertEquals(Integer.parseInt(props.getProperty("Available")), in.available(), "Available");
            in.close();
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testGetAudioInputStreamURL:" + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testGetAudioInputStreamURL:" + e.getMessage());
        }
    }

    private void dumpAudioFileFormat(AudioFileFormat baseFileFormat,
                                     PrintStream out,
                                     String info) throws UnsupportedAudioFileException {
        AudioFormat baseFormat = baseFileFormat.getFormat();
        if (out != null) {
            // AudioFileFormat
            out.println("  -----  " + info + "  -----");
            out.println("    ByteLength=" + baseFileFormat.getByteLength());
            out.println("    FrameLength=" + baseFileFormat.getFrameLength());
            out.println("    Type=" + baseFileFormat.getType());
            // AudioFormat
            out.println("    SourceFormat=" + baseFormat.toString());
            out.println("    Channels=" + baseFormat.getChannels());
            out.println("    FrameRate=" + baseFormat.getFrameRate());
            out.println("    FrameSize=" + baseFormat.getFrameSize());
            out.println("    SampleRate=" + baseFormat.getSampleRate());
            out.println("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
            out.println("    Encoding=" + baseFormat.getEncoding());
        }
        assertEquals(props.getProperty("Type"), baseFileFormat.getType().toString(), "Type");
        assertEquals(props.getProperty("SourceFormat"), baseFormat.toString(), "SourceFormat");
        assertEquals(Integer.parseInt(props.getProperty("Channels")), baseFormat.getChannels(), "Channels");
        assertTrue(Float.parseFloat(props.getProperty("FrameRate")) == baseFormat.getFrameRate(), "FrameRate");
        assertEquals(Integer.parseInt(props.getProperty("FrameSize")), baseFormat.getFrameSize(), "FrameSize");
        assertTrue(Float.parseFloat(props.getProperty("SampleRate")) == baseFormat.getSampleRate(), "SampleRate");
        assertEquals(Integer.parseInt(props.getProperty("SampleSizeInBits")),
                     baseFormat.getSampleSizeInBits(),
                     "SampleSizeInBits");
        assertEquals(props.getProperty("Encoding"), baseFormat.getEncoding().toString(), "Encoding");
    }

    private void dumpAudioInputStream(AudioInputStream in, PrintStream out, String info) throws IOException {
        AudioFormat baseFormat = in.getFormat();
        if (out != null) {
            out.println("  -----  " + info + "  -----");
            out.println("    Available=" + in.available());
            out.println("    FrameLength=" + in.getFrameLength());
            // AudioFormat
            out.println("    SourceFormat=" + baseFormat.toString());
            out.println("    Channels=" + baseFormat.getChannels());
            out.println("    FrameRate=" + baseFormat.getFrameRate());
            out.println("    FrameSize=" + baseFormat.getFrameSize());
            out.println("    SampleRate=" + baseFormat.getSampleRate());
            out.println("    SampleSizeInBits=" + baseFormat.getSampleSizeInBits());
            out.println("    Encoding=" + baseFormat.getEncoding());
        }
        assertEquals(props.getProperty("SourceFormat"), baseFormat.toString(), "SourceFormat");
        assertEquals(Integer.parseInt(props.getProperty("Channels")), baseFormat.getChannels(), "Channels");
        assertTrue(Float.parseFloat(props.getProperty("FrameRate")) == baseFormat.getFrameRate(), "FrameRate");
        assertEquals(Integer.parseInt(props.getProperty("FrameSize")), baseFormat.getFrameSize(), "FrameSize");
        assertTrue(Float.parseFloat(props.getProperty("SampleRate")) == baseFormat.getSampleRate(), "SampleRate");
        assertEquals(Integer.parseInt(props.getProperty("SampleSizeInBits")),
                     baseFormat.getSampleSizeInBits(),
                     "SampleSizeInBits");
        assertEquals(props.getProperty("Encoding"), baseFormat.getEncoding().toString(), "Encoding");

    }
}
