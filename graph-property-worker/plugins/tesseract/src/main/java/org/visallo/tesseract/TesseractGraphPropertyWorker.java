package org.visallo.tesseract;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.vietocr.ImageHelper;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.RowKeyHelper;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Name("Tesseract")
@Description("Uses Tesseract open source tool to OCR images")
public class TesseractGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TesseractGraphPropertyWorker.class);
    private static final List<String> ICON_MIME_TYPES = Arrays.asList("image/x-icon", "image/vnd.microsoft.icon");
    public static final String TEXT_PROPERTY_KEY = TesseractGraphPropertyWorker.class.getName();
    public static final String CONFIG_DATA_PATH = "tesseract.dataPath";
    private Tesseract tesseract;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        tesseract = Tesseract.getInstance();

        String dataPath = getConfiguration().get(CONFIG_DATA_PATH, null);
        if (dataPath != null) {
            tesseract.setDatapath(dataPath);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            LOGGER.error("Could not load image from property %s on vertex %s", data.getProperty().toString(), data.getElement().getId());
            return;
        }
        String ocrResults = extractTextFromImage(image);
        if (ocrResults == null) {
            return;
        }

        String textPropertyKey = RowKeyHelper.build(TEXT_PROPERTY_KEY, data.getProperty().getName(), data.getProperty().getKey());

        InputStream textIn = new ByteArrayInputStream(ocrResults.getBytes());
        StreamingPropertyValue textValue = new StreamingPropertyValue(textIn, String.class);

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Metadata textMetadata = data.createPropertyMetadata();
        VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(textMetadata, "OCR Text", getVisibilityTranslator().getDefaultVisibility());
        VisalloProperties.MIME_TYPE_METADATA.setMetadata(textMetadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        VisalloProperties.TEXT.addPropertyValue(m, textPropertyKey, textValue, textMetadata, data.getVisibility());
        m.save(getAuthorizations());

        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), textPropertyKey, VisalloProperties.TEXT.getPropertyName(), data.getPriority());
    }

    private String extractTextFromImage(BufferedImage image) throws TesseractException {
        BufferedImage grayImage = ImageHelper.convertImageToGrayscale(image);
        String ocrResults = tesseract.doOCR(grayImage).replaceAll("\\n{2,}", "\n");
        if (ocrResults == null || ocrResults.trim().length() == 0) {
            return null;
        }
        ocrResults = ocrResults.trim();
        // TODO remove the trash that doesn't seem to be words
        return ocrResults;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(VisalloProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null) {
            return false;
        }
        if (ICON_MIME_TYPES.contains(mimeType)) {
            return false;
        }
        return mimeType.startsWith("image");
    }
}
