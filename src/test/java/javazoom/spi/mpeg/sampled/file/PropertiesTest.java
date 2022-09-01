
package javazoom.spi.mpeg.sampled.file;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * PropertiesContainer unit test. It matches test.mp3 properties to
 * test.mp3.properties expected results.
 */
class PropertiesTest {

    private static Logger logger = Logger.getLogger(PropertiesTest.class.getName());

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

    @Test
    void testPropertiesFile() throws Exception {
        final String[] testPropsAFF = {
            "duration", "title", "author", "album", "date", "comment", "copyright", "mp3.framerate.fps", "mp3.copyright",
            "mp3.padding", "mp3.original", "mp3.length.bytes", "mp3.frequency.hz", "mp3.length.frames", "mp3.mode",
            "mp3.channels", "mp3.version.mpeg", "mp3.framesize.bytes", "mp3.vbr.scale", "mp3.version.encoding",
            "mp3.header.pos", "mp3.version.layer", "mp3.crc"
        };
        final String[] testPropsAF = {
            "vbr", "bitrate"
        };

        File file = new File(filename);
        AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
        AudioFormat baseFormat = baseFileFormat.getFormat();
        logger.info("-> Filename : " + filename + " <-");
        logger.info(baseFileFormat.toString());
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map<?, ?> properties = baseFileFormat.properties();
            logger.info(properties.toString());
            for (String key : testPropsAFF) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
                String valexpected = props.getProperty(key);
                assertEquals(valexpected, val, key);
            }
        } else {
            fail("testPropertiesFile : TAudioFileFormat expected");
        }

        if (baseFormat instanceof TAudioFormat) {
            Map<?, ?> properties = baseFormat.properties();
            for (String key : testPropsAF) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
                String valexpected = props.getProperty(key);
                assertEquals(valexpected, val, key);
            }
        } else {
            fail("testPropertiesFile : TAudioFormat expected");
        }
    }

    @Test
    void testPropertiesURL() throws Exception {
        final String[] testPropsAFF = { /* "duration", */
            "title", "author", "album", "date", "comment", "copyright", "mp3.framerate.fps", "mp3.copyright", "mp3.padding",
            "mp3.original", /* "mp3.length.bytes", */"mp3.frequency.hz", /* "mp3.length.frames", */"mp3.mode", "mp3.channels",
            "mp3.version.mpeg", "mp3.framesize.bytes", "mp3.vbr.scale", "mp3.version.encoding", "mp3.header.pos",
            "mp3.version.layer", "mp3.crc"
        };
        final String[] testPropsAF = {
            "vbr", "bitrate"
        };
        URL url = new URL(fileurl);
        AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
        AudioFormat baseFormat = baseFileFormat.getFormat();
        logger.info("-> URL : " + filename + " <-");
        logger.info(baseFileFormat.toString());
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map<String, ?> properties = baseFileFormat.properties();
            for (String key : testPropsAFF) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
                String valexpected = props.getProperty(key);
                assertEquals(valexpected, val, key);
            }
        } else {
            fail("testPropertiesURL : TAudioFileFormat expected");
        }

        if (baseFormat instanceof TAudioFormat) {
            Map<String, ?> properties = ((TAudioFormat) baseFormat).properties();
            for (String key : testPropsAF) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
                String valexpected = props.getProperty(key);
                assertEquals(valexpected, val, key);
            }
        } else {
            fail("testPropertiesURL : TAudioFormat expected");
        }
    }

    @Test
    @Disabled
    void testPropertiesShoutcast() throws Exception {
        String shoutURL = props.getProperty("shoutcast");
        URL url = new URL(shoutURL);
        AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
        AudioFormat baseFormat = baseFileFormat.getFormat();
        logger.info("-> URL : " + url.toString() + " <-");
        logger.info(baseFileFormat.toString());
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map<String, ?> properties = baseFileFormat.properties();
            for (String key : properties.keySet()) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
            }
        } else {
            fail("testPropertiesShoutcast : TAudioFileFormat expected");
        }

        if (baseFormat instanceof TAudioFormat) {
            Map<String, ?> properties = baseFormat.properties();
            for (String key : properties.keySet()) {
                String val = null;
                if (properties.get(key) != null)
                    val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
            }
        } else {
            fail("testPropertiesShoutcast : TAudioFormat expected");
        }
    }

    @Test
    void testDumpPropertiesURL() throws Exception {
        URL file = new URL(fileurl);
        AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
        AudioFormat baseFormat = baseFileFormat.getFormat();
        logger.info("-> Filename : " + filename + " <-");
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map<String, ?> properties = baseFileFormat.properties();
            for (String key : properties.keySet()) {
                String val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
            }
        } else {
            fail("testDumpPropertiesFile : TAudioFileFormat expected");
        }

        if (baseFormat instanceof TAudioFormat) {
            Map<String, ?> properties = baseFormat.properties();
            for (String key : properties.keySet()) {
                String val = (properties.get(key)).toString();
                logger.info(key + "='" + val + "'");
            }
        } else {
            fail("testDumpPropertiesFile : TAudioFormat expected");
        }
    }
}
