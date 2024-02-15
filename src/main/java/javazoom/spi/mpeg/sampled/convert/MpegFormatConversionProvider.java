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

import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.convert.TEncodingFormatConversionProvider;

import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * ConversionProvider for MPEG files.
 */
public class MpegFormatConversionProvider extends TEncodingFormatConversionProvider {

    private static final AudioFormat.Encoding MP3 = Encodings.getEncoding("MP3");
    private static final AudioFormat.Encoding PCM_SIGNED = Encodings.getEncoding("PCM_SIGNED");

    private static final AudioFormat[] INPUT_FORMATS = {
            // mono
            new AudioFormat(MP3, NOT_SPECIFIED, NOT_SPECIFIED, 1, NOT_SPECIFIED, NOT_SPECIFIED, false),
            new AudioFormat(MP3, NOT_SPECIFIED, NOT_SPECIFIED, 1, NOT_SPECIFIED, NOT_SPECIFIED, true),
            // stereo
            new AudioFormat(MP3, NOT_SPECIFIED, NOT_SPECIFIED, 2, NOT_SPECIFIED, NOT_SPECIFIED, false),
            new AudioFormat(MP3, NOT_SPECIFIED, NOT_SPECIFIED, 2, NOT_SPECIFIED, NOT_SPECIFIED, true),
    };

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
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider()");
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider.getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream):");
        }
        return new DecodedMpegAudioInputStream(targetFormat, audioInputStream);
    }

    /**
     * Add conversion support for any MpegEncoding source with FrameRate or FrameSize not empty.
     *
     * @param targetFormat
     * @param sourceFormat
     * @return
     */
    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider.isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat):");
            TDebug.out("checking if conversion possible");
            TDebug.out("from: " + sourceFormat);
            TDebug.out("to: " + targetFormat);
        }

        boolean conversion = super.isConversionSupported(targetFormat, sourceFormat);
        if (!conversion) {
            AudioFormat.Encoding enc = sourceFormat.getEncoding();
            if (enc instanceof MpegEncoding) {
                if ((sourceFormat.getFrameRate() != AudioSystem.NOT_SPECIFIED) || (sourceFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED)) {
                    if (sourceFormat.getChannels() == targetFormat.getChannels()) {
                        conversion = true;
                    }
                }
            }
        }
        return conversion;
    }
}
