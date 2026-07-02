package com.zynboot.infra.geo.shp;

import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ShpTestSupport {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private ShpTestSupport() {
    }

    static Path createPointShapefile(Path dir, String baseName, String nameValue) throws Exception {
        Path shpPath = dir.resolve(baseName + ".shp");

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", shpPath.toUri().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore dataStore = (ShapefileDataStore) factory.createNewDataStore(params);
        try {
            SimpleFeatureType featureType = DataUtilities.createType(baseName, "the_geom:Point:srid=4326,name:String");
            dataStore.createSchema(featureType);
            dataStore.setCharset(StandardCharsets.UTF_8);

            try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                         dataStore.getFeatureWriterAppend(dataStore.getTypeNames()[0], Transaction.AUTO_COMMIT)) {
                SimpleFeature feature = writer.next();
                feature.setAttribute("the_geom", GEOMETRY_FACTORY.createPoint(new Coordinate(116.4, 39.9)));
                feature.setAttribute("name", nameValue);
                writer.write();
            }
        } finally {
            dataStore.dispose();
        }

        return shpPath;
    }

    static List<File> listSidecarFiles(Path shpPath) throws Exception {
        String baseName = stripExtension(shpPath.getFileName().toString());
        try (var files = Files.list(shpPath.getParent())) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> stripExtension(path.getFileName().toString()).equals(baseName))
                    .map(Path::toFile)
                    .toList();
        }
    }

    static void zipFiles(List<File> files, Path zipPath) throws Exception {
        try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (File file : files) {
                outputStream.putNextEntry(new ZipEntry(file.getName()));
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    inputStream.transferTo(outputStream);
                }
                outputStream.closeEntry();
            }
        }
    }

    static Path resolveRepoPath(String first, String... more) {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve(Path.of(first, more)).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return current.resolve(Path.of(first, more)).normalize();
    }

    private static String stripExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }
}
