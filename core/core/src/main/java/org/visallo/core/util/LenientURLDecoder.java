package org.visallo.core.util;

import org.visallo.core.exception.VisalloException;

import java.io.UnsupportedEncodingException;

public class LenientURLDecoder {
    public static String decode(String s, String enc) {
        try {
            boolean needToChange = false;
            StringBuilder sb = new StringBuilder();
            int numChars = s.length();
            int i = 0;

            if (enc.length() == 0) {
                throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
            }

            while (i < numChars) {
                char c = s.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        i++;
                        needToChange = true;
                        break;
                    case '%':
                   /*
                    * Starting with this instance of %, process all
                    * consecutive substrings of the form %xy. Each
                    * substring %xy will yield a byte. Convert all
                    * consecutive bytes obtained this way to whatever
                    * character(s) they represent in the provided
                    * encoding.
                    */

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        byte[] bytes = new byte[(numChars - i) / 3];
                        int pos = 0;

                        while (((i + 2) < numChars) &&
                                (c == '%')) {
                            String hex = s.substring(i + 1, i + 3);
                            try {
                                bytes[pos] =
                                        (byte) Integer.parseInt(hex, 16);
                                pos++;
                            } catch (NumberFormatException e) {
                                sb.append(new String(bytes, 0, pos, enc));
                                sb.append("%");
                                sb.append(hex);
                                pos = 0;
                            }
                            i += 3;
                            if (i < numChars)
                                c = s.charAt(i);
                        }

                        sb.append(new String(bytes, 0, pos, enc));

                        // A trailing, incomplete byte encoding such as
                        // "%x" will be treated as unencoded text
                        if ((i < numChars) && (c == '%')) {
                            for (; i < numChars; i++) {
                                sb.append(s.charAt(i));
                            }
                        }

                        needToChange = true;
                        break;
                    default:
                        sb.append(c);
                        i++;
                        break;
                }
            }

            return (needToChange ? sb.toString() : s);
        } catch (Exception ex) {
            throw new VisalloException("Could not url decode string \"" + s + "\"", ex);
        }
    }
}
