package com.zynboot.infra.geo.shp;

import com.zynboot.kit.util.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

final class ShpSourceResolver {

    private ShpSourceResolver() {
    }

    static ResolvedShpSource resolve(String shpPath, String requestedTypeName) throws IOException {
        if (shpPath == null || shpPath.isBlank()) {
            throw new IOException("Shapefile path must not be blank");
        }

        Path sourcePath = Path.of(shpPath).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath)) {
            throw new IOException("Shapefile source not found: " + shpPath);
        }

        if (Files.isDirectory(sourcePath)) {
            return new ResolvedShpSource(findShpFile(sourcePath, requestedTypeName), null);
        }

        String fileName = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".shp")) {
            return new ResolvedShpSource(sourcePath.toFile(), null);
        }
        if (fileName.endsWith(".zip")) {
            Path tempDir = Files.createTempDirectory("zyn-shp-");
            try {
                ZipUtils.unzip(sourcePath.toFile(), tempDir.toString());
                return new ResolvedShpSource(findShpFile(tempDir, requestedTypeName), tempDir);
            } catch (IOException e) {
                deleteDirectoryQuietly(tempDir);
                throw e;
            } catch (RuntimeException e) {
                deleteDirectoryQuietly(tempDir);
                throw new IOException("Failed to unzip shapefile archive: " + shpPath, e);
            }
        }
        throw new IOException("Unsupported shapefile source: " + shpPath);
    }

    static void cleanup(ResolvedShpSource resolvedSource) {
        if (resolvedSource == null || resolvedSource.cleanupDir() == null) {
            return;
        }
        deleteDirectoryQuietly(resolvedSource.cleanupDir());
    }

    private static File findShpFile(Path rootDir, String requestedTypeName) throws IOException {
        List<Path> shpFiles;
        try (Stream<Path> paths = Files.walk(rootDir)) {
            shpFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".shp"))
                    .sorted()
                    .toList();
        }

        if (shpFiles.isEmpty()) {
            throw new IOException("No .shp file found under: " + rootDir);
        }
        if (shpFiles.size() == 1) {
            return shpFiles.getFirst().toFile();
        }
        if (requestedTypeName != null && !requestedTypeName.isBlank()) {
            List<Path> matches = shpFiles.stream()
                    .filter(path -> baseName(path).equalsIgnoreCase(requestedTypeName))
                    .toList();
            if (matches.size() == 1) {
                return matches.getFirst().toFile();
            }
        }

        String candidates = shpFiles.stream()
                .map(rootDir::relativize)
                .map(Path::toString)
                .toList()
                .toString();
        throw new IOException("Multiple .shp files found under " + rootDir
                + ", please specify typeName. candidates=" + candidates);
    }

    private static String baseName(Path path) {
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private static void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        Files.deleteIfExists(dir);
                    } catch (IOException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }

    record ResolvedShpSource(
            File shpFile,
            Path cleanupDir
    ) {
    }
}
