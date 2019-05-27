
/*
 *   PlayerTest - JavaZOOM : http://www.javazoom.net
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javazoom.spi.PropertiesContainer;


/**
 * Simple player (based on MP3 SPI) unit test.
 * It takes around 2-5% of CPU and 16MB RAM under Win2K/P4/1.6GHz/JDK1.5 beta
 * It takes around 10-12% of CPU and 10MB RAM under Win2K/PIII/1GHz/JDK1.4.1
 * It takes around 8-10% of CPU and 10MB RAM under Win2K/PIII/1GHz/JDK1.3.1
 */
@Disabled
public class PlayerTest {

    private String basefile = null;
    private String filename = null;
    private String name = null;
    private Properties props = null;
    private PrintStream out = null;

    @BeforeEach
    protected void setUp() throws Exception {
        props = new Properties();
        InputStream pin = getClass().getClassLoader().getResourceAsStream("test.mp3.properties");
        props.load(pin);
        basefile = props.getProperty("basefile");
        name = props.getProperty("filename");
        filename = basefile + name;
        out = System.out;
    }

    @Test
    public void testPlay() {
        try {
            if (out != null)
                out.println("---  Start : " + filename + "  ---");
            File file = new File(filename);
            //URL file = new URL(props.getProperty("shoutcast"));
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
            if (out != null)
                out.println("Audio Type : " + aff.getType());
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            AudioInputStream din = null;
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                if (out != null)
                    out.println("Source Format : " + baseFormat.toString());
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                            baseFormat.getSampleRate(),
                                                            16,
                                                            baseFormat.getChannels(),
                                                            baseFormat.getChannels() * 2,
                                                            baseFormat.getSampleRate(),
                                                            false);
                if (out != null)
                    out.println("Target Format : " + decodedFormat.toString());
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                if (din instanceof PropertiesContainer) {
                    assertTrue(true, "PropertiesContainer : OK");
                } else {
                    assertTrue(false, "Wrong PropertiesContainer instance");
                }
                rawplay(decodedFormat, din);
                in.close();
                if (out != null)
                    out.println("---  Stop : " + filename + "  ---");
                assertTrue(true, "testPlay : OK");
            }
        } catch (Exception e) {
            assertTrue(false, "testPlay : " + e.getMessage());
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            // Start
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1)
                    nBytesWritten = line.write(data, 0, nBytesRead);
            }
            // Stop
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }
}
