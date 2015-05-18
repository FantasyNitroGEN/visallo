package org.visallo.core.model.artifactThumbnails;

import org.apache.commons.lang.StringUtils;
import com.v5analytics.simpleorm.Entity;
import com.v5analytics.simpleorm.Field;
import com.v5analytics.simpleorm.Id;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Entity(tableName = "artifactThumbnail")
public class ArtifactThumbnail {
    @Id
    private String id;

    @Field
    private byte[] data;

    @Field
    private String format;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected ArtifactThumbnail() {
    }

    public ArtifactThumbnail(
            String artifactVertexId,
            String type,
            byte[] data,
            String format,
            int width, int height
    ) {
        this.id = createId(artifactVertexId, type, width, height);
        this.data = data;
        this.format = format;
    }

    public static String createId(String artifactVertexId, String type, int width, int height) {
        return artifactVertexId
                + ":" + type
                + ":" + StringUtils.leftPad(Integer.toString(width), 8, '0')
                + ":" + StringUtils.leftPad(Integer.toString(height), 8, '0');
    }

    public byte[] getData() {
        return data;
    }

    public String getFormat() {
        return format;
    }

    public BufferedImage getImage() {
        try {
            byte[] data = getData();
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new RuntimeException("Could not load image", e);
        }
    }
}
