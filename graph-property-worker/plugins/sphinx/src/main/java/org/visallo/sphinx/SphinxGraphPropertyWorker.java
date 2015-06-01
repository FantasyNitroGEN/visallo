package org.visallo.sphinx;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.video.VideoTranscript;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

@Name("Sphinx")
@Description("Uses Sphinx to extract audio transcripts")
public class SphinxGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SphinxGraphPropertyWorker.class);
    private static final long BYTES_PER_SAMPLE = 2;
    private static final long SAMPLES_PER_SECOND = 16000;
    public static final String MULTI_VALUE_KEY = SphinxGraphPropertyWorker.class.getName();
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        VideoTranscript transcript = extractTranscriptFromAudio(data.getLocalFile());
        if (transcript == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        Metadata metadata = data.createPropertyMetadata();
        VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(metadata, "Audio Transcript", getVisibilityTranslator().getDefaultVisibility());
        addVideoTranscriptAsTextPropertiesToMutation(m, MULTI_VALUE_KEY, transcript, metadata, data.getVisibility());
        m.save(getAuthorizations());

        getGraph().flush();
        pushVideoTranscriptTextPropertiesOnWorkQueue(data.getElement(), MULTI_VALUE_KEY, transcript, data.getPriority());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(VisalloProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("audio"));
    }

    private VideoTranscript extractTranscriptFromAudio(File localFile) throws IOException, InterruptedException {
        checkNotNull(localFile, "localFile cannot be null");
        File wavFile = File.createTempFile("encode_wav_", ".wav");
        File wavFileNoSilence = File.createTempFile("encode_wav_no_silence_", ".wav");
        File wavFileNoHeaders = File.createTempFile("encode_wav_noheader_", ".wav");

        try {
            convertAudioTo16bit1Ch(localFile, wavFile);
            removeSilenceFromBeginning(wavFile, wavFileNoSilence);

            long silenceFileSizeDiff = wavFile.length() - wavFileNoSilence.length();
            double timeOffsetInSec = (double) silenceFileSizeDiff / BYTES_PER_SAMPLE / SAMPLES_PER_SECOND;

            WavFileUtil.fixWavHeaders(wavFileNoSilence, wavFileNoHeaders); // TODO patch sphinx to handle headers correctly

            String sphinxOutput = runSphinx(wavFileNoHeaders);

            return SphinxOutputParser.parse(sphinxOutput, timeOffsetInSec);
        } finally {
            if (!wavFile.delete()) {
                LOGGER.warn("Could not delete wav file: %s", wavFile.getAbsolutePath());
            }
            if (!wavFileNoSilence.delete()) {
                LOGGER.warn("Could not delete wav no silence file: %s", wavFileNoSilence.getAbsolutePath());
            }
            if (!wavFileNoHeaders.delete()) {
                LOGGER.warn("Could not delete wav no header file: %s", wavFileNoHeaders.getAbsolutePath());
            }
        }
    }

    private String runSphinx(File inFile) throws IOException, InterruptedException {
        checkNotNull(inFile, "inFile cannot be null");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            processRunner.execute(
                    "pocketsphinx_continuous",
                    new String[]{
                            "-infile", inFile.getAbsolutePath(),
                            "-time", "true"
                    },
                    out,
                    inFile.getAbsolutePath() + ": "
            );
        } finally {
            out.close();
        }
        return new String(out.toByteArray());
    }

    private void removeSilenceFromBeginning(File inFile, File outFile) throws IOException, InterruptedException {
        checkNotNull(inFile, "inFile cannot be null");
        checkNotNull(outFile, "outFile cannot be null");
        processRunner.execute(
                "sox",
                new String[]{
                        inFile.getAbsolutePath(),
                        outFile.getAbsolutePath(),
                        "silence", "1", "0.1", "1%", // remove silence from beginning. at least 0.1s of less than 1% volume
                        "pad", "1", "0" // pad 1 second of silence to beginning
                },
                null,
                inFile.getAbsolutePath() + ": "
        );
    }

    private void convertAudioTo16bit1Ch(File inputFile, File outputFile) throws IOException, InterruptedException {
        checkNotNull(inputFile, "inputFile cannot be null");
        checkNotNull(outputFile, "outputFile cannot be null");
        processRunner.execute(
                "ffmpeg",
                new String[]{
                        "-y", // overwrite output files
                        "-i", inputFile.getAbsolutePath(),
                        "-acodec", "pcm_s16le",
                        "-ac", "1",
                        "-ar", Long.toString(SAMPLES_PER_SECOND),
                        outputFile.getAbsolutePath()
                },
                null,
                inputFile.getAbsolutePath() + ": "
        );
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }
}
