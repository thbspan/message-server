package org.test.message.common;

import java.security.MessageDigest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Md5Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Md5Util.class);

    public static String encode(String origin) {
        if (StringUtils.isEmpty(origin)){
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] results = md5.digest(origin.getBytes());

            for (byte b : results) {
                int number = b & 0xff;
                String str = Integer.toHexString(number);
                if (str.length() == 1) {
                    builder.append("0");
                }
                builder.append(str);
            }

        } catch (Exception e) {
            LOGGER.error("encode exception!", e);
        }
        return builder.toString();
    }

    private Md5Util() {}
}
