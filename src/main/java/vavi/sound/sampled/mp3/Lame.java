/*
 *  Copyright (c) 2000,2001,2007 by Florian Bomers
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package vavi.sound.sampled.mp3;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;

import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.MPEGMode;
import org.tritonus.share.TDebug;
import vavi.util.Debug;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * Low level wrapper for the LAME native encoder.
 * <p>
 * TODO: fill frame rate, frame size
 *
 * @author Florian Bomers
 */
public class Lame {

    public static final AudioFormat.Encoding MPEG1L3 = new AudioFormat.Encoding("MPEG1L3");
    // Lame converts automagically to MPEG2 or MPEG2.5, if necessary.
    public static final AudioFormat.Encoding MPEG2L3 = new AudioFormat.Encoding("MPEG2L3");
    public static final AudioFormat.Encoding MPEG2DOT5L3 = new AudioFormat.Encoding("MPEG2DOT5L3");

    // property constants

    /**
     * legacy: system property key to read the effective encoding of the encoded
     * audio data, an instance of AudioFormat.Encoding
     */
    public static final String P_ENCODING = "encoding";

    /**
     * legacy: system property key to read the effective sample rate of the
     * encoded audio stream (an instance of Float)
     */
    public static final String P_SAMPLERATE = "samplerate";

    /**
     * property key to read/set the VBR mode: an instance of Boolean (default:
     * false)
     */
    public static final String P_VBR = "vbr";

    /**
     * property key to read/set the channel mode: a String, one of
     * &quot;jointstereo&quot;, &quot;dual&quot;, &quot;mono&quot;,
     * &quot;auto&quot; (default).
     */
    public static final String P_CHMODE = "chmode";

    /**
     * property key to read/set the bitrate: an Integer value. Set to -1 for
     * default bitrate.
     */
    public static final String P_BITRATE = "bitrate";

    /**
     * property key to read/set the quality: an Integer from 1 (highest) to 9
     * (lowest).
     */
    public static final String P_QUALITY = "quality";

    // constants from lame.h
    public static final int MPEG_VERSION_2 = 0; // MPEG-2
    public static final int MPEG_VERSION_1 = 1; // MPEG-1
    public static final int MPEG_VERSION_2DOT5 = 2; // MPEG-2.5

    // low mean bitrate in VBR mode
    public static final int QUALITY_LOWEST = 9;
    public static final int QUALITY_LOW = 7;
    public static final int QUALITY_MIDDLE = 5;
    public static final int QUALITY_HIGH = 2;
    // quality==0 not yet coded in LAME (3.83alpha)
    // high mean bitrate in VBR // mode
    public static final int QUALITY_HIGHEST = 1;

    public static final int CHANNEL_MODE_STEREO = 0;
    public static final int CHANNEL_MODE_JOINT_STEREO = 1;
    public static final int CHANNEL_MODE_DUAL_CHANNEL = 2;
    public static final int CHANNEL_MODE_MONO = 3;

    // channel mode has no influence on mono files.
    public static final int CHANNEL_MODE_AUTO = -1;
    public static final int BITRATE_AUTO = -1;

    // suggested maximum buffer size for an mpeg frame
    private static final int DEFAULT_PCM_BUFFER_SIZE = 2048 * 16;

    private static int DEFAULT_QUALITY = QUALITY_MIDDLE;
    private static int DEFAULT_BITRATE = BITRATE_AUTO;
    private static int DEFAULT_CHANNEL_MODE = CHANNEL_MODE_AUTO;
    // in VBR mode, bitrate is ignored.
    private static boolean DEFAULT_VBR = false;

    public static final int OUT_OF_MEMORY = -300;
    public static final int NOT_INITIALIZED = -301;
    private static final int LAME_ENC_NOT_FOUND = -302;

    private static final String PROPERTY_PREFIX = "tritonus.lame.";

    /**
     * Holds LameApi This field is long because on 64 bit architectures, the
     * native size of ints may be 64 bit.
     */
    private LameEncoder lameApi;

    // encoding values
    private int quality = DEFAULT_QUALITY;
    private int bitRate = DEFAULT_BITRATE;
    private boolean vbr = DEFAULT_VBR;
    private int chMode = DEFAULT_CHANNEL_MODE;

    // these fields are set upon successful initialization to show effective
    // values.
    private int effQuality;
    private int effBitRate;
    private boolean effVbr;
    private int effChMode;
    private int effSampleRate;
    private AudioFormat.Encoding effEncoding;

    /**
     * this flag is set if the user set the encoding properties by way of
     * system.properties
     */
    private boolean hadSystemProps = false;

    private void handleNativeException(int resultCode) {
        close();
        if (resultCode == OUT_OF_MEMORY) {
            throw new OutOfMemoryError("out of memory");
        } else if (resultCode == NOT_INITIALIZED) {
            throw new RuntimeException("not initialized");
        }
    }

    /**
     * Initializes the encoder with the given source/PCM format. The default mp3
     * encoding parameters are used, see DEFAULT_BITRATE, DEFAULT_CHANNEL_MODE,
     * DEFAULT_QUALITY, and DEFAULT_VBR.
     *
     * @throws IllegalArgumentException when parameters are not supported by LAME.
     */
    public Lame(AudioFormat sourceFormat) {
        readParams(sourceFormat, null);
        initParams(sourceFormat);
    }

    /**
     * Initializes the encoder with the given source/PCM format. The mp3
     * parameters are read from the targetFormat's properties. For any parameter
     * that is not set, global system properties are queried for backwards
     * tritonus compatibility. Last, parameters will use the default values
     * DEFAULT_BITRATE, DEFAULT_CHANNEL_MODE, DEFAULT_QUALITY, and DEFAULT_VBR.
     *
     * @throws IllegalArgumentException when parameters are not supported by LAME.
     */
    public Lame(AudioFormat sourceFormat, AudioFormat targetFormat) {
        readParams(sourceFormat, targetFormat.properties());
        initParams(sourceFormat);
    }

    /**
     * Initializes the encoder, overriding any parameters set in the audio
     * format's properties or in the system properties.
     *
     * @throws IllegalArgumentException when parameters are not supported by LAME.
     */
    public Lame(AudioFormat sourceFormat, int bitRate, int channelMode, int quality, boolean vbr) {
        this.bitRate = bitRate;
        this.chMode = channelMode;
        this.quality = quality;
        this.vbr = vbr;
        initParams(sourceFormat);
    }

    private void readParams(AudioFormat sourceFormat, Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            // legacy support for system properties
            readSystemProps();
        }
        if (props != null) {
            readProps(props);
        }
    }

    private void initParams(AudioFormat sourceFormat) {
        // simple check that bitrate is not too high for MPEG2 and MPEG2.5
        // todo: exception ?
        if (sourceFormat.getSampleRate() < 32000 && bitRate > 160) {
            bitRate = 160;
        }
        if (TDebug.TraceAudioConverter) {
            String br = bitRate < 0 ? "auto" : (bitRate + "KBit/s");
            TDebug.out("LAME parameters: channels="
                    + sourceFormat.getChannels() + "  sample rate="
                    + (Math.round(sourceFormat.getSampleRate()) + "Hz")
                    + "  bitrate=" + br);
            TDebug.out("                 channelMode=" + chmode2string(chMode)
                    + "   quality=" + quality + " (" + quality2string(quality)
                    + ")   VBR=" + vbr + "  bigEndian="
                    + sourceFormat.isBigEndian());
        }
        nInitParams(sourceFormat, sourceFormat.getChannels(),
                Math.round(sourceFormat.getSampleRate()), bitRate, chMode,
                quality, vbr, sourceFormat.isBigEndian());
        if (TDebug.TraceAudioConverter) {
            TDebug.out("LAME effective quality=" + effQuality + " (" + quality2string(effQuality) + ")");
        }
        // legacy provide effective parameters to user by way of system
        // properties
        if (hadSystemProps) {
            setEffectiveParamsToSystemProps();
        }
    }

    /**
     * Initializes the lame encoder. Throws IllegalArgumentException when
     * parameters are not supported by LAME.
     */
    private void nInitParams(AudioFormat format, int channels, int sampleRate, int bitrate,
                            int mode, int quality, boolean vbr, boolean bigEndian) {

        MPEGMode lameMode = mode == -1 ? MPEGMode.STEREO : MPEGMode.values()[mode];

        TDebug.out("initParams: ");
        TDebug.out(String.format("   %d channels, %d Hz, %d KBit/s, mode %s, quality=%d VBR=%s bigEndian=%s",
                channels, sampleRate, bitrate, lameMode, quality, vbr, bigEndian));

        this.lameApi = new LameEncoder(format, bitrate, lameMode, quality, vbr);

        // update the Lame instance with the effective values
        this.effSampleRate = (int) lameApi.getEffectiveFormat().getSampleRate();
        this.effBitRate = lameApi.getEffectiveBitRate();
        this.effChMode = lameApi.getEffectiveChannelMode().ordinal();
        this.effQuality = lameApi.getEffectiveQuality();
        this.effVbr = lameApi.getEffectiveVBR();
        this.effEncoding = lameApi.getEffectiveEncoding();
    }

    public String getEncoderVersion() {
        String sRes = this.lameApi.getEncoderVersion();
Debug.println(Level.FINE, "getEncoderVersion: " + sRes);
        return sRes;
    }

    /**
     * Returns the buffer needed pcm buffer size. The passed parameter is a
     * wished buffer size. The implementation of the encoder may return a lower
     * or higher buffer size. The encoder must be initialized (i.e. not closed)
     * at this point.
     * @return value of <0 denotes an error.
     */
    public int getPCMBufferSize() {
        int ret = lameApi.getPCMBufferSize();
        if (ret < 0) {
            handleNativeException(ret);
            throw new IllegalArgumentException("Unknown error in Lame.nGetPCMBufferSize(). Resultcode=" + ret);
        }
        return ret;
    }

    public int getMP3BufferSize() {
        return lameApi.getMP3BufferSize();
    }

    /**
     * @return result of lame_encode_buffer:
     * return code     number of bytes output in mp3buf. Can be 0
     *                 -1:  mp3buf was too small
     *                 -2:  malloc() problem
     *                 -3:  lame_init_params() not called
     *                 -4:  psycho acoustic problems
     *                 -5:  ogg cleanup encoding error
     *                 -6:  ogg frame encoding error
     */
    private int nEncodeBuffer(byte[] pcm, int length, byte[] encoded) {

        TDebug.out("Lame#nEncodeBuffer: ");
        TDebug.out(String.format("   length:%d", length));
        TDebug.out(String.format("   %d bytes in PCM array", length));
        TDebug.out(String.format("   %d bytes in to-be-encoded array", encoded.length));

        //TDebug.out("   Sample1=%d Sample2=%d", pcmSamples[0], pcmSamples[1]);

        int result = lameApi.encodeBuffer(pcm, 0, length, encoded);
        //TDebug.out("   MP3-1=%d MP3-2=%d", (int) encodedBytes[0], (int) encodedBytes[1]);

        return result;
    }

    private static void swapSamples(byte[] samples, int count) {
        for (int i = 0; i < count; i++) {
            samples[i * 2 + 1] = samples[i * 2];
            samples[i * 2] = samples[i * 2 + 1];
        }
    }

    /**
     * Encode a block of data.
     *
     * @return the number of bytes written to <code>encoded</code>. May be 0.
     * @throws IllegalArgumentException       when parameters are wrong.
     * @throws ArrayIndexOutOfBoundsException When the <code>encoded</code> array is too small,
     *                                        <code>length</code> should be
     *                                        the value returned by getPCMBufferSize.
     */
    public int encodeBuffer(byte[] pcm, int length, byte[] encoded) {
        if (length < 0 || length > pcm.length) {
            throw new IllegalArgumentException("inconsistent parameters");
        }
        int result = nEncodeBuffer(pcm, length, encoded);
        if (result < 0) {
            if (result == -1) {
                throw new ArrayIndexOutOfBoundsException("Encode buffer too small");
            }
            handleNativeException(result);
            throw new IllegalStateException("crucial error in encodeBuffer.");
        }
        return result;
    }

    /**
     * Has to be called to finish encoding. <code>encoded</code> may be null.
     *
     * @return the number of bytes written to <code>encoded</code>
     */
    public int encodeFinish(byte[] encoded) {
        int result = 0;

        //jsize length=(*env).GetArrayLength(env, buffer);
        TDebug.out("encodeFinish: ");
        //TDebug.out("   %d bytes in the array", (int) length);

        result = lameApi.encodeFinish(encoded);

        close();

        TDebug.out(String.format("   %d bytes returned", result));

        return result;
    }

    /*
     * Deallocates resources used by the native library. *MUST* be called !
     */
    public void close() {

        TDebug.out("close. ");

        if (lameApi != null) {
            lameApi.close();
            lameApi = null;
        }
    }

    // properties
    private void readProps(Map<String, Object> props) {
        Object q = props.get(P_QUALITY);
        if (q instanceof String) {
            quality = string2quality(((String) q).toLowerCase(), quality);
        } else if (q instanceof Integer) {
            quality = (Integer) q;
        } else if (q != null) {
            throw new IllegalArgumentException("illegal type of quality property: " + q);
        }
        q = props.get(P_BITRATE);
        if (q instanceof String) {
            bitRate = Integer.parseInt((String) q);
        } else if (q instanceof Integer) {
            bitRate = (Integer) q;
        } else if (q != null) {
            throw new IllegalArgumentException("illegal type of bitrate property: " + q);
        }
        q = props.get(P_CHMODE);
        if (q instanceof String) {
            chMode = string2chmode(((String) q).toLowerCase(), chMode);
        } else if (q != null) {
            throw new IllegalArgumentException("illegal type of chmode property: " + q);
        }
        q = props.get(P_VBR);
        if (q instanceof String) {
            vbr = string2bool(((String) q));
        } else if (q instanceof Boolean) {
            vbr = (Boolean) q;
        } else if (q != null) {
            throw new IllegalArgumentException("illegal type of vbr property: " + q);
        }
    }

    /**
     * Return the audioformat representing the encoded mp3 stream. The format
     * object will have the following properties:
     * <ul>
     * <li>quality: an Integer, 1 (highest) to 9 (lowest)
     * <li>bitrate: an Integer, 32...320 kbit/s
     * <li>chmode: channel mode, a String, one of &quot;jointstereo&quot;,
     * &quot;dual&quot;, &quot;mono&quot;, &quot;auto&quot; (default).
     * <li>vbr: a Boolean
     * <li>encoder.version: a string with the version of the encoder
     * <li>encoder.name: a string with the name of the encoder
     * </ul>
     */
    public AudioFormat getEffectiveFormat() {
        // first gather properties
        HashMap<String, Object> map = new HashMap<>();
        map.put(P_QUALITY, getEffectiveQuality());
        map.put(P_BITRATE, getEffectiveBitRate());
        map.put(P_CHMODE, chmode2string(getEffectiveChannelMode()));
        map.put(P_VBR, getEffectiveVBR());
        // map.put(P_SAMPLERATE, getEffectiveSampleRate());
        // map.put(P_ENCODING,getEffectiveEncoding());
        map.put("encoder.name", "LAME");
        map.put("encoder.version", getEncoderVersion());
        int channels = 2;
        if (chMode == CHANNEL_MODE_MONO) {
            channels = 1;
        }
        return new AudioFormat(getEffectiveEncoding(),
                getEffectiveSampleRate(), NOT_SPECIFIED, channels,
                NOT_SPECIFIED, NOT_SPECIFIED, false, map);
    }

    public int getEffectiveQuality() {
        if (effQuality >= QUALITY_LOWEST) {
            return QUALITY_LOWEST;
        } else if (effQuality >= QUALITY_LOW) {
            return QUALITY_LOW;
        } else if (effQuality >= QUALITY_MIDDLE) {
            return QUALITY_MIDDLE;
        } else if (effQuality >= QUALITY_HIGH) {
            return QUALITY_HIGH;
        }
        return QUALITY_HIGHEST;
    }

    public int getEffectiveBitRate() {
        return effBitRate;
    }

    public int getEffectiveChannelMode() {
        return effChMode;
    }

    public boolean getEffectiveVBR() {
        return effVbr;
    }

    public int getEffectiveSampleRate() {
        return effSampleRate;
    }

    public AudioFormat.Encoding getEffectiveEncoding() {
        if (effEncoding == MPEG2L3) {
            if (getEffectiveSampleRate() < 16000) {
                return MPEG2DOT5L3;
            }
            return MPEG2L3;
        } else if (effEncoding == MPEG2DOT5L3) {
            return MPEG2DOT5L3;
        }
        // default
        return MPEG1L3;
    }

    // LEGACY support: read/write encoding parameters from/to system.properties

    /** legacy: set effective parameters in system properties */
    private void setEffectiveParamsToSystemProps() {
        try {
            System.setProperty(PROPERTY_PREFIX + "effective" + "." + P_QUALITY,
                    quality2string(getEffectiveQuality()));
            System.setProperty(PROPERTY_PREFIX + "effective" + "." + P_BITRATE,
                    String.valueOf(getEffectiveBitRate()));
            System.setProperty(PROPERTY_PREFIX + "effective" + "." + P_CHMODE,
                    chmode2string(getEffectiveChannelMode()));
            System.setProperty(PROPERTY_PREFIX + "effective" + "." + P_VBR,
                    String.valueOf(getEffectiveVBR()));
            System.setProperty(PROPERTY_PREFIX + "effective" + "."
                    + P_SAMPLERATE, String.valueOf(getEffectiveSampleRate()));
            System.setProperty(PROPERTY_PREFIX + "effective" + "." + P_ENCODING, getEffectiveEncoding().toString());
            System.setProperty(PROPERTY_PREFIX + "encoder.version", getEncoderVersion());
        } catch (Throwable t) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(t);
            }
        }
    }

    /**
     * workaround for missing paramtrization possibilities for
     * FormatConversionProviders
     */
    private void readSystemProps() {
        String v = getStringProperty(P_QUALITY, quality2string(quality));
        quality = string2quality(v.toLowerCase(), quality);
        bitRate = getIntProperty(P_BITRATE, bitRate);
        v = getStringProperty(P_CHMODE, chmode2string(chMode));
        chMode = string2chmode(v.toLowerCase(), chMode);
        vbr = getBooleanProperty(P_VBR, vbr);
        if (hadSystemProps) {
            // set the parameters back so that user program can verify them
            try {
                System.setProperty(PROPERTY_PREFIX + P_QUALITY, quality2string(DEFAULT_QUALITY));
                System.setProperty(PROPERTY_PREFIX + P_BITRATE, String.valueOf(DEFAULT_BITRATE));
                System.setProperty(PROPERTY_PREFIX + P_CHMODE, chmode2string(DEFAULT_CHANNEL_MODE));
                System.setProperty(PROPERTY_PREFIX + P_VBR, String.valueOf(DEFAULT_VBR));
            } catch (Throwable t) {
                if (TDebug.TraceAllExceptions) {
                    TDebug.out(t);
                }
            }
        }
    }

    private String quality2string(int quality) {
        if (quality >= QUALITY_LOWEST) {
            return "lowest";
        } else if (quality >= QUALITY_LOW) {
            return "low";
        } else if (quality >= QUALITY_MIDDLE) {
            return "middle";
        } else if (quality >= QUALITY_HIGH) {
            return "high";
        }
        return "highest";
    }

    private int string2quality(String quality, int def) {
        switch (quality) {
        case "lowest":
            return QUALITY_LOWEST;
        case "low":
            return QUALITY_LOW;
        case "middle":
            return QUALITY_MIDDLE;
        case "high":
            return QUALITY_HIGH;
        case "highest":
            return QUALITY_HIGHEST;
        }
        return def;
    }

    private String chmode2string(int chmode) {
        if (chmode == CHANNEL_MODE_STEREO) {
            return "stereo";
        } else if (chmode == CHANNEL_MODE_JOINT_STEREO) {
            return "jointstereo";
        } else if (chmode == CHANNEL_MODE_DUAL_CHANNEL) {
            return "dual";
        } else if (chmode == CHANNEL_MODE_MONO) {
            return "mono";
        } else if (chmode == CHANNEL_MODE_AUTO) {
            return "auto";
        }
        return "auto";
    }

    private int string2chmode(String chmode, int def) {
        switch (chmode) {
        case "stereo":
            return CHANNEL_MODE_STEREO;
        case "jointstereo":
            return CHANNEL_MODE_JOINT_STEREO;
        case "dual":
            return CHANNEL_MODE_DUAL_CHANNEL;
        case "mono":
            return CHANNEL_MODE_MONO;
        case "auto":
            return CHANNEL_MODE_AUTO;
        }
        return def;
    }

    /**
     * @return true if val is starts with t or y or on, false if val starts with
     * f or n or off.
     * @throws IllegalArgumentException if val is neither true nor false
     */
    private static boolean string2bool(String val) {
        if (val.length() > 0) {
            if ((val.charAt(0) == 'f') // false
                    || (val.charAt(0) == 'n') // no
                    || (val.equals("off"))) {
                return false;
            }
            if ((val.charAt(0) == 't') // true
                    || (val.charAt(0) == 'y') // yes
                    || (val.equals("on"))) {
                return true;
            }
        }
        throw new IllegalArgumentException(
                "wrong string for boolean property: " + val);
    }

    private boolean getBooleanProperty(String strName, boolean def) {
        String strPropertyName = PROPERTY_PREFIX + strName;
        String strValue = def ? "true" : "false";
        try {
            String s = System.getProperty(strPropertyName);
            if (s != null && s.length() > 0) {
                hadSystemProps = true;
                strValue = s;
            }
        } catch (Throwable t) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(t);
            }
        }
        strValue = strValue.toLowerCase();
        boolean bValue = false;
        if (strValue.length() > 0) {
            if (def) {
                bValue = (strValue.charAt(0) != 'f') // false
                        && (strValue.charAt(0) != 'n') // no
                        && (!strValue.equals("off"));
            } else {
                bValue = (strValue.charAt(0) == 't') // true
                        || (strValue.charAt(0) == 'y') // yes
                        || (strValue.equals("on"));
            }
        }
        return bValue;
    }

    private String getStringProperty(String strName, String def) {
        String strPropertyName = PROPERTY_PREFIX + strName;
        String strValue = def;
        try {
            String s = System.getProperty(strPropertyName);
            if (s != null && s.length() > 0) {
                hadSystemProps = true;
                strValue = s;
            }
        } catch (Throwable t) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(t);
            }
        }
        return strValue;
    }

    private int getIntProperty(String strName, int def) {
        String strPropertyName = PROPERTY_PREFIX + strName;
        int value = def;
        try {
            String s = System.getProperty(strPropertyName);
            if (s != null && s.length() > 0) {
                hadSystemProps = true;
                value = Integer.parseInt(s);
            }
        } catch (Throwable e) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(e);
            }
        }
        return value;
    }
}
