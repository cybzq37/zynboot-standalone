package com.zynboot.infra.geo.shp;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShpReader {

    private ShpReader() {
    }

    public static ShpReadResult read(String shpPath) throws IOException {
        return read(shpPath, ShpReadOptions.defaults());
    }

    public static ShpReadResult read(String shpPath, ShpReadOptions options) throws IOException {
        ShpReadOptions readOptions = options == null ? ShpReadOptions.defaults() : options;
        ShpSourceResolver.ResolvedShpSource resolvedSource = ShpSourceResolver.resolve(shpPath, readOptions.typeName());
        try {
            return readResolvedShapefile(resolvedSource.shpFile(), readOptions);
        } finally {
            ShpSourceResolver.cleanup(resolvedSource);
        }
    }

    private static ShpReadResult readResolvedShapefile(File shpFile, ShpReadOptions readOptions) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("url", shpFile.toURI().toURL());
        params.put("charset", readOptions.charset());

        DataStore dataStore = DataStoreFinder.getDataStore(params);
        if (dataStore == null) {
            throw new IOException("Unable to open shapefile datastore: " + shpFile.getAbsolutePath());
        }

        List<ShpFeatureData> features = new ArrayList<>();
        try {
            String typeName = resolveTypeName(dataStore, readOptions.typeName());
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = (SimpleFeatureCollection) featureSource.getFeatures();
            ShpSchema schema = ShpSchemaMapper.buildSchema(typeName, featureSource.getSchema(), readOptions);
            Map<String, String> fieldNameMapping = schema.fieldNameMapping();

            try (SimpleFeatureIterator iterator = collection.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    Map<String, Object> rawAttributes = new LinkedHashMap<>();
                    Geometry geometry = null;

                    for (var descriptor : feature.getFeatureType().getAttributeDescriptors()) {
                        String attrName = descriptor.getLocalName();
                        Object value = feature.getAttribute(attrName);
                        if (value instanceof Geometry geom) {
                            geometry = geom;
                        } else {
                            rawAttributes.put(attrName, value);
                        }
                    }
                    features.add(ShpSchemaMapper.buildFeatureData(feature.getID(), rawAttributes, geometry, fieldNameMapping));
                }
            }
            return new ShpReadResult(schema, features);
        } finally {
            dataStore.dispose();
        }
    }

    private static String resolveTypeName(DataStore dataStore, String requestedTypeName) throws IOException {
        if (requestedTypeName == null || requestedTypeName.isBlank()) {
            return dataStore.getTypeNames()[0];
        }

        for (String typeName : dataStore.getTypeNames()) {
            if (typeName.equals(requestedTypeName)) {
                return typeName;
            }
        }
        throw new IOException("Shapefile type name not found: " + requestedTypeName);
    }
}
