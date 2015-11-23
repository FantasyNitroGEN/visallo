package org.visallo.zipCodeBoundaries;

import com.google.inject.Inject;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.geometry.BoundingBox;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;
import org.vertexium.util.ArrayUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZipCodeBoundariesRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ZipCodeBoundariesRepository.class);
    public static final String CONFIG_HDFS_PATH_PREFIX = ZipCodeBoundariesRepository.class.getName() + ".pathPrefix";
    public static final String CONFIG_HDFS_PATH_PREFIX_DEFAULT = "/visallo/config/" + ZipCodeBoundariesRepository.class.getName();
    private final FeatureCollection collection;

    @Inject
    public ZipCodeBoundariesRepository(
            Configuration configuration,
            FileSystemRepository fileSystemRepository
    ) {
        try {
            File tempShapeFile = copyShapeFileLocally(configuration, fileSystemRepository);

            Map connect = new HashMap();
            connect.put("url", tempShapeFile.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(connect);
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];

            LOGGER.debug("Reading content " + typeName);

            FeatureSource featureSource = dataStore.getFeatureSource(typeName);
            collection = featureSource.getFeatures();
        } catch (IOException ex) {
            throw new VisalloException("Could not load zip code shape file", ex);
        }
    }

    protected File copyShapeFileLocally(Configuration configuration, FileSystemRepository fileSystemRepository) throws IOException {
        String pathPrefix = configuration.get(CONFIG_HDFS_PATH_PREFIX, CONFIG_HDFS_PATH_PREFIX_DEFAULT);
        File tempShapeFile = fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.shp");
        fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.shx");
        fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.dbf");
        fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.prj");
        fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.shp.iso.xml");
        fileSystemRepository.getLocalFileFor(pathPrefix + "/cb_2014_us_zcta510_500k.shp.xml");
        return tempShapeFile;
    }

    public List<Features.Feature> find(GeoRect boundingBox) {
        List<Features.Feature> results = new ArrayList<>();
        try (FeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                GeometryAttribute sourceGeometry = feature.getDefaultGeometryProperty();
                BoundingBox featureBounds = sourceGeometry.getBounds();
                GeoRect featureBoundsRect = new GeoRect(new GeoPoint(featureBounds.getMaxY(), featureBounds.getMaxX()), new GeoPoint(featureBounds.getMinY(), featureBounds.getMinX()));
                if (featureBoundsRect.intersects(boundingBox)) {
                    Features.Feature f = Features.Feature.create(feature);
                    if (f != null) {
                        results.add(f);
                    }
                }
            }
        }
        return results;
    }

    public List<Features.Feature> findZipCodes(String[] zipCodes) {
        List<Features.Feature> results = new ArrayList<>();
        try (FeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                if (ArrayUtils.contains(zipCodes, Features.Feature.getZipCode(feature))) {
                    Features.Feature f = Features.Feature.create(feature);
                    if (f != null) {
                        results.add(f);
                    }
                }
            }
        }
        return results;
    }
}
