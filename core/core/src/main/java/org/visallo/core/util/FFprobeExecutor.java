package org.visallo.core.util;

import org.visallo.core.exception.VisalloJsonParseException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class FFprobeExecutor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FFprobeExecutor.class);

    public static JSONObject getJson(ProcessRunner processRunner, String absolutePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String output = null;
        try {
            processRunner.execute(
                    "ffprobe",
                    new String[]{
                            "-v", "quiet",
                            "-print_format", "json",
                            "-show_format",
                            "-show_streams",
                            absolutePath
                    },
                    byteArrayOutputStream,
                    absolutePath + ": "
            );
            output = new String(byteArrayOutputStream.toByteArray());
            return JSONUtil.parse(output);
        } catch (VisalloJsonParseException e) {
            LOGGER.error("unable to parse ffprobe output: [%s]", output);
        } catch (Exception e) {
            LOGGER.error("exception running ffprobe", e);
        }

        return null;
    }
}