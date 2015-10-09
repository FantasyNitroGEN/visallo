package org.visallo.marvel;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import org.apache.commons.io.IOUtils;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.AccumuloGraphConfiguration;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;
import org.visallo.core.util.RowKeyHelper;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.vertexium.*;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MarvelImport extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MarvelImport.class);
    private static final String VERTEX_ID_PREFIX = "marvel_data";
    private static final String MULTI_VALUE_KEY = MarvelImport.class.getName();
    private static final StringSingleValueVisalloProperty HERO_NAME_PROPERTY = new StringSingleValueVisalloProperty("http://visallo.org/marvel#name");
    private static final StringSingleValueVisalloProperty COMIC_BOOK_TITLE_PROPERTY = new StringSingleValueVisalloProperty("http://visallo.org/marvel#comic_book_title");
    private static final StringSingleValueVisalloProperty MOVIE_TITLE_PROPERTY = new StringSingleValueVisalloProperty("http://visallo.org/marvel#movie_title");
    private static final String CHARACTER_WAS_IN_IRI = "http://visallo.org/marvel#characterWasIn";
    private static final String COMIC_BOOK_IRI = "http://visallo.org/marvel#comicBook";
    private static final String MOVIE_IRI = "http://visallo.org/marvel#movie";
    private static final String HERO_IRI = "http://visallo.org/marvel#hero";
    private static final Visibility DEFAULT_VISIBILITY =  new Visibility("");
    private static final Metadata DEFAULT_METADATA = new Metadata();
    private static final String DEFAULT_MIME_TYPE = "image";
    public static final String GEOLOCATION_IRI = "http://visallo.org/marvel#geolocation";

    private static final int NUM_THREADS = 20;

    @Parameter(names = {"--dataDir"}, required = true, arity = 1, converter = FileConverter.class, description = "Location of the data files")
    protected File dataDir;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new MarvelImport(), args);
    }

    @Override
    protected int run() throws Exception {
        Stopwatch stopwatch = new Stopwatch().start();

        importHeroes();
        importComicBooks();
        importMovies();
        importImages();
        importLocations();
        importEdges();

        LOGGER.info("total import time: %ds", stopwatch.elapsed(TimeUnit.SECONDS));

        return 0;
    }

    private void importLocations() throws IOException, InterruptedException {
        LOGGER.info("importing locations");
        File locationData = new File(dataDir, "locations.data");
        multiThreadedImport(locationData, new FileLineWorker() {


            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                try{
                    GeoPointVisalloProperty property = new GeoPointVisalloProperty(GEOLOCATION_IRI);
                    Vertex vertex = graph.getVertex(getVertexId(fileLine.id), getAuthorizations());
                    String[] split = fileLine.data.split(" ");

                    GeoPoint geoPoint = new GeoPoint(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
                    property.addPropertyValue(vertex, "locationlocation", geoPoint, DEFAULT_METADATA, DEFAULT_VISIBILITY, getAuthorizations());
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void importImages() throws IOException, InterruptedException {
        LOGGER.info("importing images");
        File imageData = new File(dataDir, "images.data");
        multiThreadedImport(imageData, new FileLineWorker() {
            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                try {
                    LOGGER.info("importing image " + fileLine.data);
                    ElementBuilder<Vertex> image = convertToArtifact(graph, new File(dataDir, "images/" + fileLine.data), DEFAULT_MIME_TYPE, "title");
                    Vertex save = image.save(getAuthorizations());
                    Vertex vertex = graph.getVertex(getVertexId(fileLine.id), getAuthorizations());
                    vertex.setProperty(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(), save.getId(), DEFAULT_METADATA, DEFAULT_VISIBILITY, getAuthorizations());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void importEdges() throws IOException, InterruptedException {
        LOGGER.info("importing edge data");
        File edgeData = new File(dataDir, "edges.data");
        multiThreadedImport(edgeData, new FileLineWorker() {
            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                String srcId = getVertexId(fileLine.id);
                for(String id : fileLine.data.split(" ")){
                    String destId = getVertexId(id);
                    handleEdge(graph, srcId, destId, getAuthorizations());
                }
            }
        });
    }

    private void importComicBooks() throws IOException, InterruptedException {
        LOGGER.info("importing comic books");
        File comicData = getFile("comic-books.data");
        final MarvelComicBookNameParser metadata = createComicBookParser(getFile("comic-book-metadata.data"));
        multiThreadedImport(comicData, new FileLineWorker() {
            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                String code = removeFirstAndLastCharacter(fileLine.data);
                handleComicBook(graph, fileLine.id, code, metadata, getAuthorizations());
            }
        });
    }

    private void importHeroes() throws IOException, InterruptedException {
        LOGGER.info("importing hero data");
        File heroData = new File(dataDir, "heroes.data");
        multiThreadedImport(heroData, new FileLineWorker() {
            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                LOGGER.info("importing hero " + fileLine.id + " " + fileLine.data);
                String name = removeFirstAndLastCharacter(fileLine.data);
                handleHero(graph, fileLine.id, name, getAuthorizations());
            }
        });
    }

    private void importMovies() throws IOException, InterruptedException {
        LOGGER.info("importing movie data");
        File heroData = new File(dataDir, "movies.data");
        multiThreadedImport(heroData, new FileLineWorker() {
            @Override
            public void doWork(FileLine fileLine, Graph graph) {
                LOGGER.info("importing movie " + fileLine.id + " " + fileLine.data);
                String name = removeFirstAndLastCharacter(fileLine.data);
                handleMovie(graph, fileLine.id, name, getAuthorizations());
            }
        });
    }

    private void multiThreadedImport(File file, final FileLineWorker unitOfWork) throws IOException, InterruptedException {
        final Queue<FileLine> fileLines = getFileLines(file);
        final int numFileLines = fileLines.size();

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        final Stopwatch stopwatch = new Stopwatch().start();

        final AtomicInteger atomicInteger = new AtomicInteger();

        for(int i = 0; i < NUM_THREADS; i++){
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, Object> config = ImmutableMap.<String, Object>copyOf(getConfiguration().getSubset("graph"));
                        Graph graph = AccumuloGraph.create(new AccumuloGraphConfiguration(config));
                        while (!fileLines.isEmpty()) {
                            FileLine heroLine = fileLines.poll();
                            unitOfWork.doWork(heroLine, graph);
                            int numCompleted = atomicInteger.incrementAndGet();
                            if (numCompleted % 50 == 0) {
                                double numPerSecond = ((double) numCompleted) * 1000.0 / stopwatch.elapsed(TimeUnit.MILLISECONDS);
                                double estSeconds = ((double) (numFileLines - numCompleted)) / numPerSecond;
                                LOGGER.info("%d/%d, %f/s, est. remaining time: %fs\n", numCompleted, numFileLines, numPerSecond, estSeconds);
                                graph.flush();
                            }
                        }
                        synchronized (fileLines) {
                            graph.flush();
                            graph.shutdown();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1000L, TimeUnit.DAYS);
        LOGGER.info("%d/%d, total time: %ds\n", numFileLines, numFileLines, stopwatch.elapsed(TimeUnit.SECONDS));
    }

    private interface FileLineWorker{
        void doWork(FileLine fileLine, Graph graph);
    }

    private static Vertex handleComicBook(Graph graph, String id, String comicBookCode, MarvelComicBookNameParser metadata, Authorizations authorizations) {
        VertexBuilder vertexBuilder = graph.prepareVertex(getVertexId(id), getVisibility());
        vertexBuilder.setProperty(COMIC_BOOK_TITLE_PROPERTY.getPropertyName(), metadata.getName(comicBookCode), getVisibility());
        vertexBuilder.setProperty(VisalloProperties.ROW_KEY.getPropertyName(), getVertexId(id), getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, COMIC_BOOK_IRI, getVisibility());
        return vertexBuilder.save(authorizations);
    }

    private static MarvelComicBookNameParser createComicBookParser(File comicBookMetadata) throws IOException {
        MarvelComicBookNameParser metadata = new MarvelComicBookNameParser();
        List<String> lines = FileUtils.readLines(comicBookMetadata);
        for(String line : lines){
            metadata.addMetadata(line);
        }

        return metadata;
    }

    private static Vertex handleHero(Graph graph,  String id, String name, Authorizations authorizations) {
        VertexBuilder vertexBuilder = graph.prepareVertex(getVertexId(id), getVisibility());
        HERO_NAME_PROPERTY.setProperty(vertexBuilder, name, getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, HERO_IRI, getVisibility());
        vertexBuilder.setProperty(VisalloProperties.ROW_KEY.getPropertyName(), getVertexId(id), getVisibility());
        return vertexBuilder.save(authorizations);
    }

    private static Vertex handleMovie(Graph graph,  String id, String name, Authorizations authorizations) {
        VertexBuilder vertexBuilder = graph.prepareVertex(getVertexId(id), getVisibility());
        MOVIE_TITLE_PROPERTY.setProperty(vertexBuilder, name, getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(vertexBuilder, MOVIE_IRI, getVisibility());
        vertexBuilder.setProperty(VisalloProperties.ROW_KEY.getPropertyName(), getVertexId(id), getVisibility());
        return vertexBuilder.save(authorizations);
    }

    private static Edge handleEdge(Graph graph, String srcId, String destId, Authorizations authorizations) {
        return graph.prepareEdge(Joiner.on("-").join(new String[] { "edge", srcId, destId }), srcId, destId, CHARACTER_WAS_IN_IRI, getVisibility()).save(authorizations);
    }

    private static String getVertexId(String id){
        return VERTEX_ID_PREFIX + id;
    }

    private static Visibility getVisibility(){
        return new Visibility("");
    }

    private Map<String, String> createIdComicBookMap(File comicBookFile) throws IOException {
        Queue<FileLine> lines = getFileLines(comicBookFile);

        Map<String, String> map = Maps.newHashMap();

        for(FileLine line : lines){
            map.put(line.id, removeFirstAndLastCharacter(line.data));
        }

        return map;
    }

    private static Queue<FileLine> getFileLines(File file) throws IOException {
        List<String> lines = FileUtils.readLines(file);
        Queue<FileLine> fileLines = Queues.newConcurrentLinkedQueue();
        for(String line : lines){
            int split = line.indexOf(' ');
            String id = line.substring(0, split);
            String data = line.substring(split + 1);
            fileLines.add(new FileLine(id, data));
        }

        return fileLines;
    }

    private static class FileLine {
        public String id;
        public String data;

        public FileLine(String id, String data) {
            this.id = id;
            this.data = data;
        }
    }

    private static String removeFirstAndLastCharacter(String str){
        return str.substring(1, str.length() - 1);
    }

    private static ElementBuilder<Vertex> convertToArtifact(
            Graph graph,
            final File file,
            String mimeType,
            String title
    ) throws IOException {
        final InputStream fileInputStream = new FileInputStream(file);
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: %d", rawContent.length);

        final String fileName = file.getName();

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: %s", fileRowKey);

        StreamingPropertyValue rawValue = new StreamingPropertyValue(new ByteArrayInputStream(rawContent), byte[].class);
        rawValue.searchIndex(false);
        rawValue.store(true);

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex("image-" + file.getName(), DEFAULT_VISIBILITY);
        VisalloProperties.TITLE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, title, DEFAULT_METADATA, DEFAULT_VISIBILITY);
        VisalloProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(), DEFAULT_METADATA, DEFAULT_VISIBILITY);
        VisalloProperties.FILE_NAME.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, fileName, DEFAULT_METADATA, DEFAULT_VISIBILITY);
        VisalloProperties.MIME_TYPE.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, mimeType, DEFAULT_METADATA, DEFAULT_VISIBILITY);
        VisalloProperties.RAW.setProperty(vertexBuilder, rawValue, DEFAULT_METADATA, DEFAULT_VISIBILITY);
        return vertexBuilder;
    }

    private File getFile(String file){
        return new File(dataDir, file);
    }
}
