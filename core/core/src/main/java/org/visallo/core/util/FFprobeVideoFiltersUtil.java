package org.visallo.core.util;

import com.google.common.base.Joiner;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FFprobeVideoFiltersUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FFprobeVideoFiltersUtil.class);

    public static Integer getRotation(JSONObject json) {
        if (json == null) {
            return 0;
        }

        JSONArray streamsJson = json.optJSONArray("streams");
        if (streamsJson != null) {
            for (int i = 0; i < streamsJson.length(); i++) {
                JSONObject streamsIndexJson = streamsJson.optJSONObject(i);
                if (streamsIndexJson != null) {
                    JSONObject tagsJson = streamsIndexJson.optJSONObject("tags");
                    if (tagsJson != null) {
                        Double optionalRotate = tagsJson.optDouble("rotate");
                        if (!Double.isNaN(optionalRotate)) {
                            Integer rotate = optionalRotate.intValue() % 360;
                            return rotate;
                        }
                    }
                }
            }
        }

        LOGGER.debug("Could not retrieve a \"rotate\" value from the JSON object.");
        return 0;
    }

    private static String[] getRotateTransforms(Integer videoRotation) {
        if (videoRotation == null) {
            return null;
        } else if (videoRotation % 360 == 90) {
            return new String[]{"transpose=1"};
        } else if (videoRotation % 360 == 180) {
            return new String[]{"transpose=1,transpose=1"};
        } else if (videoRotation % 360 == 270) {
            return new String[]{"transpose=2"};
        } else {
            return null;
        }
    }

    private static String[] getScaleTransforms(Integer videoRotation) {
        if (videoRotation != null && (videoRotation == 90 || videoRotation == 270)) {
            return new String[] {"scale=-1:480"};
        }
        return new String[] {"scale=720:-1"};
    }

    public static String[] getFFmpegVideoFilterOptions(Integer videoRotation) {
        List<String> transforms = new ArrayList<String>();

        //Rotate
        String[] ffmpegRotationOptions = getRotateTransforms(videoRotation);
        if (ffmpegRotationOptions != null) {
            for (String transform : ffmpegRotationOptions) {
                transforms.add(transform);
            }
        }

        // Scale
        String[] ffmpegScaleOptions = getScaleTransforms(videoRotation);
        if (ffmpegScaleOptions != null) {
            for (String transform : ffmpegScaleOptions) {
                transforms.add(transform);
            }
        }

        if (transforms.isEmpty()) {
            return null;
        }

        return new String[] {
            "-vf",
            Joiner.on(",").join(transforms)
        };
    }
}
