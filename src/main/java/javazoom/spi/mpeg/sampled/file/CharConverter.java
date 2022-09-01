/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package javazoom.spi.mpeg.sampled.file;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.mozilla.universalchardet.UniversalDetector;


/**
 * CharConverter.
 * <p>
 * system properties
 * <li> javazoom.spi.mpeg.encoding ... mp3 tags encoding, default is "ISO_8859_1"</li>
 *
 * @author <a href="umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051208 nsano initial version <br>
 */
public final class CharConverter {

    /** */
    private static Logger logger = Logger.getLogger(CharConverter.class.getName());

    /** */
    private CharConverter() {
    }

    /** */
    public static String createString(byte[] buffer, int start, int length) {
        String value = null;
        try {
            value = new String(buffer, start, length, Charset.forName(encoding));
        } catch (Exception e) {
logger.fine("exception: " + e);
            String encoding = getCharset(buffer);
            if (encoding != null) {
                value = new String(buffer, start, length, Charset.forName(encoding));
            } else {
                value = new String(buffer, start, length);
            }
        }
        int p = value.indexOf(0);
        if (p != -1) {
            value = value.substring(0, p);
        }
        return value;
    }

    /** */
    private static final String encoding = System.getProperty("javazoom.spi.mpeg.encoding", "ISO_8859_1");

    /** */
    private static UniversalDetector detector = new UniversalDetector();

    /** @return nullable */
    private static String getCharset(byte[] buf) {

        detector.reset();

        int i = 0;
        while (i < buf.length && !detector.isDone()) {
            detector.handleData(buf, 0, buf[i++]);
        }

        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
logger.fine("encoding: " + encoding);
        detector.reset();

        return encoding;
    }
}

/* */
