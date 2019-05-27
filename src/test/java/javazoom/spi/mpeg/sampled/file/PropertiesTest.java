
package javazoom.spi.mpeg.sampled.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * PropertiesContainer unit test. It matches test.mp3 properties to
 * test.mp3.properties expected results. As we don't ship test.mp3, you have to
 * generate your own test.mp3.properties Uncomment out = System.out; in setUp()
 * method to generated it on stdout from your own MP3 file.
 */
public class PropertiesTest {

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
        out = System.out;
    }

    @Test
    public void testPropertiesFile() {
        String[] testPropsAFF = {
            "duration", "title", "author", "album", "date", "comment", "copyright", "mp3.framerate.fps", "mp3.copyright",
            "mp3.padding", "mp3.original", "mp3.length.bytes", "mp3.frequency.hz", "mp3.length.frames", "mp3.mode",
            "mp3.channels", "mp3.version.mpeg", "mp3.framesize.bytes", "mp3.vbr.scale", "mp3.version.encoding",
            "mp3.header.pos", "mp3.version.layer", "mp3.crc"
        };
        String[] testPropsAF = {
            "vbr", "bitrate"
        };

        File file = new File(filename);
        AudioFileFormat baseFileFormat = null;
        AudioFormat baseFormat = null;
        try {
            baseFileFormat = AudioSystem.getAudioFileFormat(file);
            baseFormat = baseFileFormat.getFormat();
            if (out != null)
                out.println("-> Filename : " + filename + " <-");
            if (out != null)
                out.println(baseFileFormat);
            if (baseFileFormat instanceof TAudioFileFormat) {
                Map<?, ?> properties = ((TAudioFileFormat) baseFileFormat).properties();
                if (out != null)
                    out.println(properties);
                for (int i = 0; i < testPropsAFF.length; i++) {
                    String key = testPropsAFF[i];
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                    String valexpected = props.getProperty(key);
                    assertEquals(key, valexpected, val);
                }
            } else {
                assertTrue(false, "testPropertiesFile : TAudioFileFormat expected");
            }

            if (baseFormat instanceof TAudioFormat) {
                Map<?, ?> properties = ((TAudioFormat) baseFormat).properties();
                for (int i = 0; i < testPropsAF.length; i++) {
                    String key = testPropsAF[i];
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                    String valexpected = props.getProperty(key);
                    assertEquals(key, valexpected, val);
                }
            } else {
                assertTrue(false, "testPropertiesFile : TAudioFormat expected");
            }
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testPropertiesFile : " + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testPropertiesFile : " + e.getMessage());
        }
    }

    @Test
    public void testPropertiesURL() {
        String[] testPropsAFF = { /* "duration", */
            "title", "author", "album", "date", "comment", "copyright", "mp3.framerate.fps", "mp3.copyright", "mp3.padding",
            "mp3.original", /* "mp3.length.bytes", */"mp3.frequency.hz", /* "mp3.length.frames", */"mp3.mode", "mp3.channels",
            "mp3.version.mpeg", "mp3.framesize.bytes", "mp3.vbr.scale", "mp3.version.encoding", "mp3.header.pos",
            "mp3.version.layer", "mp3.crc"
        };
        String[] testPropsAF = {
            "vbr", "bitrate"
        };
        AudioFileFormat baseFileFormat = null;
        AudioFormat baseFormat = null;
        try {
            URL url = new URL(fileurl);
            baseFileFormat = AudioSystem.getAudioFileFormat(url);
            baseFormat = baseFileFormat.getFormat();
            if (out != null)
                out.println("-> URL : " + filename + " <-");
            if (out != null)
                out.println(baseFileFormat);
            if (baseFileFormat instanceof TAudioFileFormat) {
                Map<?, ?> properties = ((TAudioFileFormat) baseFileFormat).properties();
                for (int i = 0; i < testPropsAFF.length; i++) {
                    String key = testPropsAFF[i];
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                    String valexpected = props.getProperty(key);
                    assertEquals(key, valexpected, val);
                }
            } else {
                assertTrue(false, "testPropertiesURL : TAudioFileFormat expected");
            }
            if (baseFormat instanceof TAudioFormat) {
                Map<?, ?> properties = ((TAudioFormat) baseFormat).properties();
                for (int i = 0; i < testPropsAF.length; i++) {
                    String key = testPropsAF[i];
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                    String valexpected = props.getProperty(key);
                    assertEquals(key, valexpected, val);
                }
            } else {
                assertTrue(false, "testPropertiesURL : TAudioFormat expected");
            }
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testPropertiesURL : " + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testPropertiesURL : " + e.getMessage());
        }
    }

    @Test
    public void testPropertiesShoutcast() {
        AudioFileFormat baseFileFormat = null;
        AudioFormat baseFormat = null;
        String shoutURL = props.getProperty("shoutcast");
        try {
            URL url = new URL(shoutURL);
            baseFileFormat = AudioSystem.getAudioFileFormat(url);
            baseFormat = baseFileFormat.getFormat();
            if (out != null)
                out.println("-> URL : " + url.toString() + " <-");
            if (out != null)
                out.println(baseFileFormat);
            if (baseFileFormat instanceof TAudioFileFormat) {
                Map<?, ?> properties = ((TAudioFileFormat) baseFileFormat).properties();
                Iterator<?> it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                }
            } else {
                assertTrue(false, "testPropertiesShoutcast : TAudioFileFormat expected");
            }
            if (baseFormat instanceof TAudioFormat) {
                Map<?, ?> properties = ((TAudioFormat) baseFormat).properties();
                Iterator<?> it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String val = null;
                    if (properties.get(key) != null)
                        val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                }
            } else {
                assertTrue(false, "testPropertiesShoutcast : TAudioFormat expected");
            }
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testPropertiesShoutcast : " + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testPropertiesShoutcast : " + e.getMessage());
        }
    }

    public void _testDumpPropertiesFile() {
        File file = new File(filename);
        AudioFileFormat baseFileFormat = null;
        AudioFormat baseFormat = null;
        try {
            baseFileFormat = AudioSystem.getAudioFileFormat(file);
            baseFormat = baseFileFormat.getFormat();
            if (out != null)
                out.println("-> Filename : " + filename + " <-");
            if (baseFileFormat instanceof TAudioFileFormat) {
                Map<?, ?> properties = ((TAudioFileFormat) baseFileFormat).properties();
                Iterator<?> it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                }
            } else {
                assertTrue(false, "testDumpPropertiesFile : TAudioFileFormat expected");
            }

            if (baseFormat instanceof TAudioFormat) {
                Map<?, ?> properties = ((TAudioFormat) baseFormat).properties();
                Iterator<?> it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String val = (properties.get(key)).toString();
                    if (out != null)
                        out.println(key + "='" + val + "'");
                }
            } else {
                assertTrue(false, "testDumpPropertiesFile : TAudioFormat expected");
            }
        } catch (UnsupportedAudioFileException e) {
            assertTrue(false, "testDumpPropertiesFile : " + e.getMessage());
        } catch (IOException e) {
            assertTrue(false, "testDumpPropertiesFile : " + e.getMessage());
        }
    }
}
