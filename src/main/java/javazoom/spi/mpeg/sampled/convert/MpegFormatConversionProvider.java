/*
 *   MpegFormatConversionProvider.
 *
 * JavaZOOM : mp3spi@javazoom.net
 *               http://www.javazoom.net
 *
 * ---------------------------------------------------------------------------
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
 * --------------------------------------------------------------------------
 */

package javazoom.spi.mpeg.sampled.convert;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.convert.TEncodingFormatConversionProvider;

import static java.lang.System.getLogger;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * ConversionProvider for MPEG files.
 */
public class MpegFormatConversionProvider extends TEncodingFormatConversionProvider {

    private static final Logger logger = getLogger("org.tritonus.TraceAudioConverter");

    private static final AudioFormat.Encoding PCM_SIGNED = Encodings.getEncoding("PCM_SIGNED");

    /**
     * The encodings {@link javazoom.spi.mpeg.sampled.file.MpegAudioFileReader} puts into the
     * formats it returns. Those are what a source format actually holds, an encoding simply
     * named "MP3" is never one of them.
     */
    private static final AudioFormat.Encoding[] MPEG_ENCODINGS = {
            MpegEncoding.MPEG1L1, MpegEncoding.MPEG1L2, MpegEncoding.MPEG1L3,
            MpegEncoding.MPEG2L1, MpegEncoding.MPEG2L2, MpegEncoding.MPEG2L3,
            MpegEncoding.MPEG2DOT5L1, MpegEncoding.MPEG2DOT5L2, MpegEncoding.MPEG2DOT5L3,
    };

    private static final AudioFormat[] INPUT_FORMATS = inputFormats();

    private static AudioFormat[] inputFormats() {
        List<AudioFormat> formats = new ArrayList<>();
        for (AudioFormat.Encoding encoding : MPEG_ENCODINGS) {
            for (int channels : new int[] {1, 2}) {
                for (boolean bigEndian : new boolean[] {false, true}) {
                    formats.add(new AudioFormat(encoding, NOT_SPECIFIED, NOT_SPECIFIED, channels, NOT_SPECIFIED, NOT_SPECIFIED, bigEndian));
                }
            }
        }
        return formats.toArray(AudioFormat[]::new);
    }

    private static final AudioFormat[] OUTPUT_FORMATS = {
            // mono, 16 bit signed
            new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, 1, 2, NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, 1, 2, NOT_SPECIFIED, true),
            // stereo, 16 bit signed
            new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, 2, 4, NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 16, 2, 4, NOT_SPECIFIED, true),
    };

    /**
     * Constructor.
     */
    public MpegFormatConversionProvider() {
        super(Arrays.asList(INPUT_FORMATS), Arrays.asList(OUTPUT_FORMATS));
        logger.log(Level.TRACE, ">MpegFormatConversionProvider()");
    }

    /**
     * Whether the given format is an mpeg stream this provider can decode, i.e. one that
     * carries everything the decoder needs to size its output.
     */
    private static boolean isDecodable(AudioFormat sourceFormat) {
        return sourceFormat.getEncoding() instanceof MpegEncoding
                && (sourceFormat.getFrameRate() != NOT_SPECIFIED || sourceFormat.getFrameSize() != NOT_SPECIFIED)
                && sourceFormat.getChannels() != NOT_SPECIFIED
                && sourceFormat.getSampleRate() != NOT_SPECIFIED;
    }

    /**
     * The one pcm format the decoder produces out of the given mpeg stream. It never changes
     * the sample rate nor the channel count, and always writes 16 bit signed samples; only
     * the endianness is up to the caller.
     */
    private static AudioFormat decodedFormat(AudioFormat sourceFormat, boolean bigEndian) {
        return new AudioFormat(PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                bigEndian);
    }

    /**
     * The formats of the super class are matched against the source with
     * {@code AudioFormats.matches()}, which cannot express "same sample rate and same channel
     * count as the source". They are therefore built from the source format here.
     */
    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        logger.log(Level.TRACE, ">MpegFormatConversionProvider.getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat):");
        if (isDecodable(sourceFormat)) {
            if (PCM_SIGNED.equals(targetEncoding)) {
                return new AudioFormat[] {
                        decodedFormat(sourceFormat, false),
                        decodedFormat(sourceFormat, true),
                };
            }
            return new AudioFormat[0];
        }
        return super.getTargetFormats(targetEncoding, sourceFormat);
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        logger.log(Level.TRACE, ">MpegFormatConversionProvider.getTargetEncodings(AudioFormat sourceFormat):");
        if (isDecodable(sourceFormat)) {
            return new AudioFormat.Encoding[] {PCM_SIGNED};
        }
        return super.getTargetEncodings(sourceFormat);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream audioInputStream) {
        logger.log(Level.TRACE, ">MpegFormatConversionProvider.getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream audioInputStream):");
        AudioFormat sourceFormat = audioInputStream.getFormat();
        if (isDecodable(sourceFormat) && PCM_SIGNED.equals(targetEncoding)) {
            // the super class would ask for a target format with every field NOT_SPECIFIED,
            // which the decoder cannot size its output buffer from
            return getAudioInputStream(decodedFormat(sourceFormat, sourceFormat.isBigEndian()), audioInputStream);
        }
        return super.getAudioInputStream(targetEncoding, audioInputStream);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        logger.log(Level.TRACE, ">MpegFormatConversionProvider.getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream):");
        AudioFormat sourceFormat = audioInputStream.getFormat();
        if (isDecodable(sourceFormat)) {
            if (!isConversionSupported(targetFormat, sourceFormat)) {
                throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
            }
            // the requested format may leave fields unspecified, the decoder needs them all
            return new DecodedMpegAudioInputStream(decodedFormat(sourceFormat, targetFormat.isBigEndian()), audioInputStream);
        }
        return new DecodedMpegAudioInputStream(targetFormat, audioInputStream);
    }
}
