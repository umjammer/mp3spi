/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * d  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package javazoom.spi.mpeg.sampled.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.spi.mpeg.sampled.file.tag.IcyInputStream;
import javazoom.spi.mpeg.sampled.file.tag.MP3Tag;
import org.tritonus.share.sampled.file.TAudioFileReader;
import vavi.sound.LimitedInputStream;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * This class implements AudioFileReader for MP3 SPI.
 *
 * system properties
 * <ul>
 *  <li>mp3spi.weak ... to skip controls</li>
 * </ul>
 *
 * @author JavaZOOM mp3spi@javazoom.net http://www.javazoom.net
 * @version 10/10/05 size computation bug fixed in parseID3v2Frames.
 *                   RIFF/MP3 header support added.
 *                   FLAC and MAC headers throw UnsupportedAudioFileException now.
 *                   "mp3.id3tag.publisher" (TPUB/TPB) added.
 *                   "mp3.id3tag.orchestra" (TPE2/TP2) added.
 *                   "mp3.id3tag.length" (TLEN/TLE) added.
 *          08/15/05 parseID3v2Frames improved.
 *          12/31/04 mp3spi.weak system property added to skip controls.
 *          11/29/04 ID3v2.2, v2.3 & v2.4 support improved.
 *                   "mp3.id3tag.composer" (TCOM/TCM) added
 *                   "mp3.id3tag.grouping" (TIT1/TT1) added
 *                   "mp3.id3tag.disc" (TPA/TPOS) added
 *                   "mp3.id3tag.encoded" (TEN/TENC) added
 *                   "mp3.id3tag.v2.version" added
 *         11/28/04  String encoding bug fix in chopSubstring method.
 */
public class MpegAudioFileReader extends TAudioFileReader {

    private static final Logger logger = getLogger("org.tritonus.TraceAudioFileReader");

    static {
        try {
            try (InputStream is = MpegAudioFileReader.class.getResourceAsStream("/META-INF/maven/net.javazoom/mp3spi/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    VERSION = props.getProperty("version", "undefined in pom.properties");
                } else {
                    VERSION = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static final String VERSION;
//    private final int SYNC = 0xFFE00000;
    private String weak = null;
    private final AudioFormat.Encoding[][] sm_aEncodings = {
            {MpegEncoding.MPEG2L1, MpegEncoding.MPEG2L2, MpegEncoding.MPEG2L3},
            {MpegEncoding.MPEG1L1, MpegEncoding.MPEG1L2, MpegEncoding.MPEG1L3},
            {MpegEncoding.MPEG2DOT5L1, MpegEncoding.MPEG2DOT5L2, MpegEncoding.MPEG2DOT5L3},};
    /**
     * SPI must not consume all input stream and must not cause EOF exception
     * for following other SPIs those take over to analyze audio stream.
     * but ID3v1 is located at end of mp3 data. there is a risk to consume all input data
     * when analysing ID3v1. so we determine max size of buffer to reset
     * and if it reached to max size when analysis this spi will throw an exception
     * and give up to deal the stream as a mp3.
     */
    public static final int INITIAL_READ_LENGTH = 1024 * 1024 * 20; // TODO limitation
    private static final int MARK_LIMIT = INITIAL_READ_LENGTH + 1;

    private static final String[] id3v1genres;

    static {
        Scanner scanner = new Scanner(MpegAudioFileReader.class.getResourceAsStream("/genres.properties"));
        List<String> genres = new ArrayList<>();
        while (scanner.hasNextLine()) {
            genres.add(scanner.nextLine());
        }
        scanner.close();
        id3v1genres = genres.toArray(String[]::new);
    }

    public MpegAudioFileReader() {
        super(MARK_LIMIT, true);
        logger.log(Level.TRACE, "MP3SPI " + VERSION);
        weak = System.getProperty("mp3spi.weak");
    }

    /**
     * Returns AudioFileFormat from File.
     */
    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        return super.getAudioFileFormat(file);
    }

    /**
     * Returns AudioFileFormat from URL.
     */
    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        logger.log(Level.TRACE, "MpegAudioFileReader.getAudioFileFormat(URL): begin");
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        URLConnection conn = url.openConnection();
        // Tell shoucast server (if any) that SPI support shoutcast stream.
        conn.setRequestProperty("Icy-Metadata", "1");
        InputStream inputStream = conn.getInputStream();
        AudioFileFormat audioFileFormat = null;
        try {
            audioFileFormat = getAudioFileFormat(inputStream, lFileLengthInBytes);
        } finally {
            inputStream.close();
        }
        logger.log(Level.TRACE, "MpegAudioFileReader.getAudioFileFormat(URL): end");
        return audioFileFormat;
    }

    /**
     * Returns AudioFileFormat from {@code inputstream} and {@code medialength}.
     *
     * @param inputStream it's user's responsibility to prepare enough read buffer for mp3 tag analysis like
     *                    <pre>
     *                    ... = getAudioFileFormat(new BufferedInputStream(your_input_stream, INITIAL_READ_LENGTH)) ...
     *                    </pre>
     *                    and max buffer size is defined in {@link #INITIAL_READ_LENGTH}.
     * @see #INITIAL_READ_LENGTH
     */
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream inputStream, long mediaLength) throws UnsupportedAudioFileException, IOException {
        logger.log(Level.TRACE, ">MpegAudioFileReader.getAudioFileFormat(InputStream inputStream, long mediaLength): begin");

        MpegContext context = new MpegContext();

        context.mLength = (int) mediaLength;
        int size = inputStream.available();
        // https://github.com/umjammer/mp3spi/issues/5
        inputStream = new LimitedInputStream(inputStream);
        PushbackInputStream pis = new PushbackInputStream(inputStream, MARK_LIMIT);
        byte[] head = new byte[22];
        int r = pis.read(head);
        assert r == head.length : "read header bytes";
logger.log(Level.TRACE, "InputStream : " + inputStream + " =>" + new String(head));

        // Check for WAV, AU, and AIFF, Ogg Vorbis, Flac, MAC file formats.
        // Next check for Shoutcast (supported) and OGG (unsupported) streams.
        if ((head[0] == 'R') && (head[1] == 'I') && (head[2] == 'F') && (head[3] == 'F') && (head[8] == 'W') && (head[9] == 'A') && (head[10] == 'V') && (head[11] == 'E')) {
            logger.log(Level.TRACE, "RIFF/WAV stream found");
            int isPCM = ((head[21] << 8) & 0x0000FF00) | ((head[20]) & 0x00000FF);
            if (weak == null) {
                if (isPCM == 1) throw new UnsupportedAudioFileException("WAV PCM stream found");
            }

        } else if ((head[0] == '.') && (head[1] == 's') && (head[2] == 'n') && (head[3] == 'd')) {
            logger.log(Level.TRACE, "AU stream found");
            if (weak == null) throw new UnsupportedAudioFileException("AU stream found");
        } else if ((head[0] == 'F') && (head[1] == 'O') && (head[2] == 'R') && (head[3] == 'M') && (head[8] == 'A') && (head[9] == 'I') && (head[10] == 'F') && (head[11] == 'F')) {
            logger.log(Level.TRACE, "AIFF stream found");
            if (weak == null) throw new UnsupportedAudioFileException("AIFF stream found");
        } else if (((head[0] == 'M') | (head[0] == 'm')) && ((head[1] == 'A') | (head[1] == 'a')) && ((head[2] == 'C') | (head[2] == 'c'))) {
            logger.log(Level.TRACE, "APE stream found");
            if (weak == null) throw new UnsupportedAudioFileException("APE stream found");
        } else if (((head[0] == 'F') | (head[0] == 'f')) && ((head[1] == 'L') | (head[1] == 'l')) && ((head[2] == 'A') | (head[2] == 'a')) && ((head[3] == 'C') | (head[3] == 'c'))) {
            logger.log(Level.TRACE, "FLAC stream found");
            if (weak == null) throw new UnsupportedAudioFileException("FLAC stream found");
        } else if (((head[0] == 'I') | (head[0] == 'i')) && ((head[1] == 'C') | (head[1] == 'c')) && ((head[2] == 'Y') | (head[2] == 'y'))) {
            // Shoutcast stream ?
            pis.unread(head);
            // Load shoutcast meta data.
            loadShoutcastInfo(pis, context.aff_properties);
        } else if (((head[0] == 'O') | (head[0] == 'o')) && ((head[1] == 'G') | (head[1] == 'g')) && ((head[2] == 'G') | (head[2] == 'g'))) {
            // Ogg stream ?
            logger.log(Level.TRACE, "Ogg stream found");
            if (weak == null) throw new UnsupportedAudioFileException("Ogg stream found");
        } else {
            // No, so pushback.
            pis.unread(head);
        }

        try {
            context.fill(pis);
        } catch (Exception e) {
            logger.log(Level.DEBUG, e.getMessage());
            logger.log(Level.TRACE, "not a MPEG stream: " + e.getMessage(), e);
            throw new UnsupportedAudioFileException("not a MPEG stream: " + e.getMessage());
        }
        // Deeper checks ?
        int cVersion = (context.nHeader >> 19) & 0x3;
        if (cVersion == 1) {
            logger.log(Level.TRACE, "not a MPEG stream: wrong version");
            throw new UnsupportedAudioFileException("not a MPEG stream: wrong version");
        }
        int cSFIndex = (context.nHeader >> 10) & 0x3;
        if (cSFIndex == 3) {
            logger.log(Level.TRACE, "not a MPEG stream: wrong sampling rate");
            throw new UnsupportedAudioFileException("not a MPEG stream: wrong sampling rate");
        }
        // Look up for ID3v1 tag
        if ((size == mediaLength) && (mediaLength != AudioSystem.NOT_SPECIFIED)) {
            byte[] id3v1 = new byte[128];
            @SuppressWarnings("unused")
            long bytesSkipped = inputStream.skip(inputStream.available() - id3v1.length);
            @SuppressWarnings("unused")
            int read = inputStream.read(id3v1, 0, id3v1.length);
            if ((id3v1[0] == 'T') && (id3v1[1] == 'A') && (id3v1[2] == 'G')) {
                parseID3v1Frames(id3v1, context.aff_properties);
            }
        } else {
            logger.log(Level.TRACE, "unknown size, maybe not a file: " + inputStream.available());
            if (inputStream.available() <= INITIAL_READ_LENGTH) {
                InputStream is = new BufferedInputStream(inputStream, inputStream.available());
                byte[] id3v1 = new byte[128];
                is.mark(inputStream.available());
                @SuppressWarnings("unused")
                long bytesSkipped = is.skip(inputStream.available() - id3v1.length);
                @SuppressWarnings("unused")
                int read = is.read(id3v1, 0, id3v1.length);
                logger.log(Level.TRACE, (char) id3v1[0] + ", " + (char) id3v1[1] + ", " + (char) id3v1[2]);
                if ((id3v1[0] == 'T') && (id3v1[1] == 'A') && (id3v1[2] == 'G')) {
                    parseID3v1Frames(id3v1, context.aff_properties);
                }
            } else {
                logger.log(Level.TRACE, "larger than limit 20MB, skip id3v1");
            }
        }
        AudioFormat format = new MpegAudioFormat(context.encoding, context.nFrequency,
                AudioSystem.NOT_SPECIFIED,  // SampleSizeInBits - The size of a sample
                context.nChannels,          // Channels - The number of channels
                AudioSystem.NOT_SPECIFIED,  // The number of bytes in each frame
                context.frameRate,          // FrameRate - The number of frames played or recorded per second
                true, context.af_properties);
        return new MpegAudioFileFormat(MpegFileFormatType.MP3,
                format, context.nTotalFrames, context.mLength, context.aff_properties);
    }

    // MPEG header info.
    private class MpegContext {
        int mLength;
        int nVersion;
        int nLayer;
        @SuppressWarnings("unused")
        int nSFIndex = AudioSystem.NOT_SPECIFIED;
        int nMode;
        int frameSize;
        //        int nFrameSize = AudioSystem.NOT_SPECIFIED;
        int nFrequency;
        int nTotalFrames = AudioSystem.NOT_SPECIFIED;
        float frameRate;
        int bitRate;
        int nChannels;
        int nHeader;
        int nTotalMS;
        boolean nVBR;
        AudioFormat.Encoding encoding;
        Map<String, Object> aff_properties = new HashMap<>();
        Map<String, Object> af_properties = new HashMap<>();

        void fill(PushbackInputStream pis) throws Exception {
            Bitstream m_bitstream = new Bitstream(pis);
            aff_properties.put("mp3.header.pos", m_bitstream.header_pos());
            Header m_header = m_bitstream.readFrame();
            // nVersion = 0 => MPEG2-LSF (Including MPEG2.5), nVersion = 1 => MPEG1
            nVersion = m_header.version();
            if (nVersion == 2) aff_properties.put("mp3.version.mpeg", Float.toString(2.5f));
            else aff_properties.put("mp3.version.mpeg", Integer.toString(2 - nVersion));
            // nLayer = 1,2,3
            nLayer = m_header.layer();
            aff_properties.put("mp3.version.layer", Integer.toString(nLayer));
            nSFIndex = m_header.sampleFrequency();
            nMode = m_header.mode();
            aff_properties.put("mp3.mode", nMode);
            nChannels = nMode == 3 ? 1 : 2;
            aff_properties.put("mp3.channels", nChannels);
            nVBR = m_header.vbr();
            af_properties.put("vbr", nVBR);
            aff_properties.put("mp3.vbr", nVBR);
            aff_properties.put("mp3.vbr.scale", m_header.vbrScale());
            frameSize = m_header.calculateFrameSize();
            aff_properties.put("mp3.framesize.bytes", frameSize);
            if (frameSize < 0) throw new UnsupportedAudioFileException("Invalid frameSize : " + frameSize);
            nFrequency = m_header.frequency();
            aff_properties.put("mp3.frequency.hz", nFrequency);
            frameRate = (float) ((1.0 / (m_header.msPerFrame())) * 1000.0);
            aff_properties.put("mp3.framerate.fps", frameRate);
            if (frameRate < 0) throw new UnsupportedAudioFileException("Invalid FrameRate : " + frameRate);
            if (mLength != AudioSystem.NOT_SPECIFIED) {
                aff_properties.put("mp3.length.bytes", mLength);
                nTotalFrames = m_header.maxNumberOfFrames(mLength);
                aff_properties.put("mp3.length.frames", nTotalFrames);
            }
            bitRate = m_header.bitrate();
            af_properties.put("bitrate", bitRate);
            aff_properties.put("mp3.bitrate.nominal.bps", bitRate);
            nHeader = m_header.getSyncHeader();
            encoding = sm_aEncodings[nVersion][nLayer - 1];
            aff_properties.put("mp3.version.encoding", encoding.toString());
            if (mLength != AudioSystem.NOT_SPECIFIED) {
                nTotalMS = Math.round(m_header.totalMs(mLength));
                aff_properties.put("duration", nTotalMS * 1000L);
            }
            aff_properties.put("mp3.copyright", m_header.copyright());
            aff_properties.put("mp3.original", m_header.original());
            aff_properties.put("mp3.crc", m_header.checksums());
            aff_properties.put("mp3.padding", m_header.padding());
            InputStream id3v2 = m_bitstream.getRawID3v2();
//logger.log(Level.TRACE, "id3v2: " + id3v2);
            if (id3v2 != null) {
                aff_properties.put("mp3.id3tag.v2", id3v2);
                parseID3v2Frames(id3v2, aff_properties);
            }
logger.log(Level.TRACE, m_header.toString());
        }
    }

    /**
     * Returns AudioInputStream from file.
     */
    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        logger.log(Level.TRACE, "getAudioInputStream(File file)");
        InputStream inputStream = Files.newInputStream(file.toPath());
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
    }

    /**
     * Returns AudioInputStream from url.
     */
    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        logger.log(Level.TRACE, "MpegAudioFileReader.getAudioInputStream(URL): begin");
        long lFileLengthInBytes = AudioSystem.NOT_SPECIFIED;
        URLConnection conn = url.openConnection();
        // Tell shoucast server (if any) that SPI support shoutcast stream.
        boolean isShout = false;
        int toRead = 4;
        byte[] head = new byte[toRead];
        conn.setRequestProperty("Icy-Metadata", "1");
        BufferedInputStream bInputStream = new BufferedInputStream(conn.getInputStream());
        bInputStream.mark(toRead);
        int read = bInputStream.read(head, 0, toRead);
        if ((read > 2) && (((head[0] == 'I') | (head[0] == 'i')) && ((head[1] == 'C') | (head[1] == 'c')) && ((head[2] == 'Y') | (head[2] == 'y'))))
            isShout = true;
        bInputStream.reset();
        InputStream inputStream = null;
        // Is a shoutcast server ?
        if (isShout) {
            // Yes
            IcyInputStream icyStream = new IcyInputStream(bInputStream);
            icyStream.addTagParseListener(IcyListener.getInstance());
            inputStream = icyStream;
        } else {
            // No, is Icecast 2 ?
            String metaint = conn.getHeaderField("icy-metaint");
            if (metaint != null) {
                // Yes, it might be icecast 2 mp3 stream.
                IcyInputStream icyStream = new IcyInputStream(bInputStream, metaint);
                icyStream.addTagParseListener(IcyListener.getInstance());
                inputStream = icyStream;
            } else {
                // No
                inputStream = bInputStream;
            }
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = getAudioInputStream(inputStream, lFileLengthInBytes);
        } catch (UnsupportedAudioFileException | IOException e) {
            inputStream.close();
            throw e;
        }
        logger.log(Level.TRACE, "MpegAudioFileReader.getAudioInputStream(URL): end");
        return audioInputStream;
    }

    /**
     * Return the AudioInputStream from the given InputStream.
     */
    @Override
    public AudioInputStream getAudioInputStream(InputStream inputStream) throws UnsupportedAudioFileException, IOException {
logger.log(Level.TRACE, "MpegAudioFileReader.getAudioInputStream(InputStream inputStream)");
logger.log(Level.TRACE, "inputStream: " + inputStream.getClass().getName() + ", mark: " + inputStream.markSupported());
        if (!inputStream.markSupported()) inputStream = new BufferedInputStream(inputStream);
logger.log(Level.TRACE, "available/limit: " + inputStream.available() + ", " + getMarkLimit());
        setMarkLimit(Math.min(inputStream.available(), getMarkLimit()));
        return super.getAudioInputStream(inputStream);
    }

    /**
     * Parser ID3v1 frames
     *
     * @param frames one tag
     * @param props in/out
     */
    protected void parseID3v1Frames(byte[] frames, Map<String, Object> props) {
logger.log(Level.TRACE, "Parsing ID3v1");
        String titlev1 = CharConverter.createString(frames, 3, 30).trim();
        String titlev2 = (String) props.get("title");
        if (((titlev2 == null) || (titlev2.isEmpty())) && (titlev1 != null)) props.put("title", titlev1);
        String artistv1 = CharConverter.createString(frames, 33, 30).trim();
        String artistv2 = (String) props.get("author");
        if (((artistv2 == null) || (artistv2.isEmpty())) && (artistv1 != null)) props.put("author", artistv1);
        String albumv1 = CharConverter.createString(frames, 63, 30).trim();
        String albumv2 = (String) props.get("album");
        if (((albumv2 == null) || (albumv2.isEmpty())) && (albumv1 != null)) props.put("album", albumv1);
        String yearv1 = CharConverter.createString(frames, 93, 4).trim();
        String yearv2 = (String) props.get("year");
        if (((yearv2 == null) || (yearv2.isEmpty())) && (yearv1 != null)) props.put("date", yearv1);
        String commentv1 = CharConverter.createString(frames, 97, 29).trim();
        String commentv2 = (String) props.get("comment");
        if (((commentv2 == null) || (commentv2.isEmpty())) && (commentv1 != null)) props.put("comment", commentv1);
        String trackv1 = "" + (frames[126] & 0xff);
        String trackv2 = (String) props.get("mp3.id3tag.track");
        if (((trackv2 == null) || (trackv2.isEmpty())) && (trackv1 != null)) props.put("mp3.id3tag.track", trackv1);
        int genrev1 = (frames[127] & 0xff);
        if ((genrev1 >= 0) && (genrev1 < id3v1genres.length)) {
            String genrev2 = (String) props.get("mp3.id3tag.genre");
            if (((genrev2 == null) || (genrev2.isEmpty()))) props.put("mp3.id3tag.genre", id3v1genres[genrev1]);
        }
logger.log(Level.TRACE, "ID3v1 parsed");
    }

    /**
     * Extract
     *
     * @param s source string
     * @param start start position to chop
     * @param end end position to chop
     * @return chopped string
     */
    @SuppressWarnings("unused")
    private String chopSubstring(String s, int start, int end) {
        String str = null;
        // 11/28/04 - String encoding bug fix.
        try {
            str = s.substring(start, end);
            int loc = str.indexOf('\0');
            if (loc != -1) str = str.substring(0, loc);
        } catch (StringIndexOutOfBoundsException e) {
            // Skip encoding issues.
logger.log(Level.TRACE, "Cannot chopSubString " + e.getMessage());
        }
        return str;
    }

    /**
     * Parse ID3v2 frames to add album (TALB), title (TIT2), date (TYER), author (TPE1), copyright (TCOP), comment (COMM) ...
     *
     * @param frames one tag
     * @param props out
     */
    protected void parseID3v2Frames(InputStream frames, Map<String, Object> props) {
        logger.log(Level.TRACE, "Parsing ID3v2");
        byte[] bframes = null;
        int size = -1;
        try {
            size = frames.available();
            bframes = new byte[size];
            frames.mark(size);
            int r = frames.read(bframes);
            assert r == bframes.length : "read ID3v2 tags";
            frames.reset();
        } catch (IOException e) {
            logger.log(Level.TRACE, "Cannot parse ID3v2 :" + e.getMessage());
        }
        if (!"ID3".equals(new String(bframes, 0, 3))) {
            logger.log(Level.TRACE, "No ID3v2 header found!");
            return;
        }
        int v2version = bframes[3] & 0xff;
        props.put("mp3.id3tag.v2.version", String.valueOf(v2version));
        if (v2version < 2 || v2version > 4) {
            logger.log(Level.TRACE, "Unsupported ID3v2 version " + v2version + "!");
            return;
        }
        try {
            logger.log(Level.TRACE, "ID3v2 frame dump(" + bframes.length + ")='" + new String(bframes) + "'");
            // ID3 tags : http://www.unixgods.org/~tilo/ID3/docs/ID3_comparison.html
            String value;
            for (int i = 10; i < bframes.length && bframes[i] > 0; i += size) {
                if (v2version == 3 || v2version == 4) {
                    // ID3v2.3 & ID3v2.4
                    String code = new String(bframes, i, 4);
logger.log(Level.DEBUG, "code: " + code);
                    size = ((bframes[i + 4] << 24) & 0xFF000000 | (bframes[i + 5] << 16) & 0x00FF0000 | (bframes[i + 6] << 8) & 0x0000FF00 | (bframes[i + 7]) & 0x000000FF);
                    i += 10;
                    if ((code.equals("TALB")) || (code.equals("TIT2")) || (code.equals("TYER")) ||
                            (code.equals("TPE1")) || (code.equals("TCOP")) || (code.equals("COMM")) ||
                            (code.equals("TCON")) || (code.equals("TRCK")) || (code.equals("TPOS")) ||
                            (code.equals("TDRC")) || (code.equals("TCOM")) || (code.equals("TIT1")) ||
                            (code.equals("TENC")) || (code.equals("TPUB")) || (code.equals("TPE2")) ||
                            (code.equals("TLEN"))) {
                        if (code.equals("COMM"))
                            value = parseText(bframes, i, size, getSkipForComment(bframes, i, size, 1 + 3));
                        else value = parseText(bframes, i, size, 1);
                        if ((value != null) && (!value.isEmpty())) {
                            switch (code) {
                            case "TALB" -> props.put("album", value);
                            case "TIT2" -> props.put("title", value);
                            case "TYER" -> props.put("date", value);

                            // ID3v2.4 date fix.
                            case "TDRC" -> props.put("date", value);
                            case "TPE1" -> props.put("author", value);
                            case "TCOP" -> props.put("copyright", value);
                            case "COMM" -> props.put("comment", value);
                            case "TCON" -> props.put("mp3.id3tag.genre", value);
                            case "TRCK" -> props.put("mp3.id3tag.track", value);
                            case "TPOS" -> props.put("mp3.id3tag.disc", value);
                            case "TCOM" -> props.put("mp3.id3tag.composer", value);
                            case "TIT1" -> props.put("mp3.id3tag.grouping", value);
                            case "TENC" -> props.put("mp3.id3tag.encoded", value);
                            case "TPUB" -> props.put("mp3.id3tag.publisher", value);
                            case "TPE2" -> props.put("mp3.id3tag.orchestra", value);
                            case "TLEN" -> props.put("mp3.id3tag.length", value);
                            }
                        }
                    }
                } else {
                    // ID3v2.2
                    String scode = new String(bframes, i, 3);
                    size = (0x00000000) + (bframes[i + 3] << 16) + (bframes[i + 4] << 8) + (bframes[i + 5]);
                    i += 6;
                    if ((scode.equals("TAL")) || (scode.equals("TT2")) || (scode.equals("TP1")) ||
                            (scode.equals("TYE")) || (scode.equals("TRK")) || (scode.equals("TPA")) ||
                            (scode.equals("TCR")) || (scode.equals("TCO")) || (scode.equals("TCM")) ||
                            (scode.equals("COM")) || (scode.equals("TT1")) || (scode.equals("TEN")) ||
                            (scode.equals("TPB")) || (scode.equals("TP2")) || (scode.equals("TLE"))) {
                        if (scode.equals("COM")) value = parseText(bframes, i, size, 5);
                        else value = parseText(bframes, i, size, 1);
                        if ((value != null) && (!value.isEmpty())) {
                            switch (scode) {
                            case "TAL" -> props.put("album", value);
                            case "TT2" -> props.put("title", value);
                            case "TYE" -> props.put("date", value);
                            case "TP1" -> props.put("author", value);
                            case "TCR" -> props.put("copyright", value);
                            case "COM" -> props.put("comment", value);
                            case "TCO" -> props.put("mp3.id3tag.genre", value);
                            case "TRK" -> props.put("mp3.id3tag.track", value);
                            case "TPA" -> props.put("mp3.id3tag.disc", value);
                            case "TCM" -> props.put("mp3.id3tag.composer", value);
                            case "TT1" -> props.put("mp3.id3tag.grouping", value);
                            case "TEN" -> props.put("mp3.id3tag.encoded", value);
                            case "TPB" -> props.put("mp3.id3tag.publisher", value);
                            case "TP2" -> props.put("mp3.id3tag.orchestra", value);
                            case "TLE" -> props.put("mp3.id3tag.length", value);
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Ignore all parsing errors.
            logger.log(Level.TRACE, "Cannot parse ID3v2 :" + e.getMessage());
        }
        logger.log(Level.TRACE, "ID3v2 parsed");
    }

    /** */
    private static int getSkipForComment(byte[] bframes, int offset, int size, int skip) {
//logger.log(Level.DEBUG, "\n" + StringUtil.getDump(bframes, offset, size + skip));
        int n = skip;
        while (bframes[offset + n] != 0) n++;
        return n + 1;
    }

    /**
     * Parse Text Frames.
     *
     * @param bframes
     * @param offset
     * @param size
     * @param skip
     * @return
     */
    protected String parseText(byte[] bframes, int offset, int size, int skip) {
        String value;
        String[] ENC_TYPES = {"ISO-8859-1", "UTF16", "UTF-16BE", "UTF-8"};
        if (bframes[offset] == 0) {
            int length = Math.max(size - getLastZeros(bframes, offset, offset + size, 1), 0);
//logger.log(Level.DEBUG, "length: " + length + ", size: " + size + ", skip: " + skip + ", zeros: " + getLastZeros(bframes, offset, offset + size, 1) + "\n" + StringUtil.getDump(bframes, offset, size + skip));
            value = CharConverter.createString(bframes, offset + skip, length - skip);
        } else {
            int extra = 0;
            String encpding = ENC_TYPES[bframes[offset]];
logger.log(Level.DEBUG, "enc: " + encpding + ", " + offset + ", " + size + "\n" + StringUtil.getDump(bframes, offset, size));
            extra += 1; // preset encoding
            if ((bframes[offset + 1] & 0xff) == 'e' && (bframes[offset + 2] & 0xff) == 'n' && (bframes[offset + 3] & 0xff) == 'g') {
                extra += (3 + 2 + 2); // 'eng' + bom + 00, 00
            }
            int length = Math.max(size - extra - getLastZeros(bframes, offset + extra, offset + size, 2), 0);
logger.log(Level.DEBUG, "string: " + (offset + extra) + ", " + size + ", " + getLastZeros(bframes, offset + extra, offset + size, 2) + "\n" + StringUtil.getDump(bframes, offset + extra, length));
            value = new String(bframes, offset + extra, length, Charset.forName(encpding));
        }
        return value.trim();
    }

    /**
     * @param start start INDEX
     * @param end   end INDEX
     * @param max   how many zeros for terminating strings
     */
    private static int getLastZeros(byte[] content, int start, int end, int max) {
        int c = 0;
        for (int i = end - 1; i >= start; i--) {
            if (content[i] == 0) {
                if (c == max) {
                    break;
                } else {
                    c++;
                }
            } else {
                break;
            }
        }
        return c;
    }

    /**
     * Load shoutcast (ICY) info.
     *
     * @param input
     * @param props
     * @throws IOException
     */
    protected void loadShoutcastInfo(InputStream input, Map<String, Object> props) throws IOException {
        IcyInputStream icy = new IcyInputStream(new BufferedInputStream(input));
//      Map<?, ?> metadata = icy.getTagHash();
        MP3Tag titleMP3Tag = icy.getTag("icy-name");
        if (titleMP3Tag != null) props.put("title", ((String) titleMP3Tag.getValue()).trim());
        MP3Tag[] meta = icy.getTags();
        if (meta != null) {
//          StringBuffer metaStr = new StringBuffer();
            for (MP3Tag mp3Tag : meta) {
                String key = mp3Tag.getName();
                String value = ((String) icy.getTag(key).getValue()).trim();
                props.put("mp3.shoutcast.metadata." + key, value);
            }
        }
    }
}