package com.zynboot.infra.storage;

import com.zynboot.infra.storage.config.StorageFilenameStrategy;
import com.zynboot.infra.storage.support.StorageObjectKeyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageObjectKeyGeneratorTest {

    @Test
    void shouldGenerateNormalizedKeyWithOriginalFilenameStrategy() {
        StorageObjectKeyGenerator generator = new StorageObjectKeyGenerator("yyyy/MM/dd", StorageFilenameStrategy.ORIGINAL);

        String key = generator.generate("folder\\Monthly Report (Final).PDF");

        assertTrue(key.matches("\\d{4}/\\d{2}/\\d{2}/Monthly_Report_Final\\.pdf"));
    }

    @Test
    void shouldNormalizeDuplicateSlashesAndLeadingSlash() {
        assertEquals("a/b/c.txt", StorageObjectKeyGenerator.normalizeKey("//a//b/c.txt"));
    }

    @Test
    void shouldAppendSuffixBeforeFileExtension() {
        assertEquals("a/b/report_2.txt", StorageObjectKeyGenerator.appendNumericSuffix("a/b/report.txt", 2));
        assertEquals("report_1", StorageObjectKeyGenerator.appendNumericSuffix("report", 1));
    }

    @Test
    void shouldGenerateStoredFilenameUnderPrefix() {
        StorageObjectKeyGenerator generator = new StorageObjectKeyGenerator("yyyy/MM/dd", StorageFilenameStrategy.ORIGINAL);

        String key = generator.generateUnderPrefix("avatars/user-1", "profile.png");
        assertTrue(key.matches("avatars/user-1/\\d{4}/\\d{2}/\\d{2}/profile\\.png"),
                "Expected key matching 'avatars/user-1/yyyy/MM/dd/profile.png' but was: " + key);
    }

    @Test
    void shouldRejectInvalidObjectKeySegmentsAndKeepPrefixCompatible() {
        assertThrows(IllegalArgumentException.class,
                () -> StorageObjectKeyGenerator.requireValidKey("docs/../readme.txt", "key"));
        assertThrows(IllegalArgumentException.class,
                () -> StorageObjectKeyGenerator.requireValidKey("docs/readme.txt/", "key"));
        assertEquals("avatars/user-1", StorageObjectKeyGenerator.requireValidPrefix("avatars/user-1/", "dir"));
    }
}
