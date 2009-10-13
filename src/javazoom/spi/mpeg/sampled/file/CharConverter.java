/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package javazoom.spi.mpeg.sampled.file;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * CharConverter. 
 *
 * @author <a href="vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
            // Special!!!
            value = new String(buffer, start, length, encoding);
        } catch (Exception e) {
            try {
                value = new String(buffer, start, length, "UNICODE");
            } catch (Exception f) {
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
    private static String encoding = System.getProperty("file.encoding");

    /** */
    static {
        try {
            Properties props = new Properties();
            props.load(CharConverter.class.getResourceAsStream("id3.properties"));
            encoding = props.getProperty("id3.encoding");
        } catch (IOException e) {
            logger.warning(e.getStackTrace()[0].toString());
        }
    }
}

/* */
