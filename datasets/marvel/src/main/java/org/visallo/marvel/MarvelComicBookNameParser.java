package org.visallo.marvel;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import org.codehaus.plexus.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

public class MarvelComicBookNameParser {
    private Map<String, String> metadataMap = Maps.newHashMap();

    public String getName(String code){
        String[] split = code.split(" ");
        if(split.length != 2 && split.length != 3){
            return code;
        }

        String key = split[0];
        if(key.endsWith("@") || key.endsWith("")){
            key = key.substring(0, split.length - 1);
        }

        if(!metadataMap.containsKey(key)){
            return code;
        }

        return metadataMap.get(key) + " " + Joiner.on(" ").join(Arrays.copyOfRange(split, 1, split.length));
    }

    public void addMetadata(String metadataLine) {
        String[] split = metadataLine.split("\t");
        String key = split[0];
        String description = StringUtils.capitaliseAllWords(split[1].toLowerCase());
        metadataMap.put(key, description);
    }
}
