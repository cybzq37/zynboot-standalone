package com.zynboot.app.controller;

import com.zynboot.infra.storage.model.UploadedFileInfo;
import com.zynboot.infra.storage.service.StorageService;
import com.zynboot.kit.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo/storage")
public class StorageDemoController {

    private StorageService storageService;

    @Autowired(required = false)
    public void setStorageService(StorageService ss) { this.storageService = ss; }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam MultipartFile file) throws IOException {
        if (storageService == null) return ApiResponse.fail("Storage not configured");
        UploadedFileInfo info = storageService.upload(file);
        return ApiResponse.ok(Map.of(
                "key", info.getKey(),
                "originalFilename", info.getOriginalFilename(),
                "size", info.getSize()
        ));
    }

    @GetMapping("/exists")
    public ApiResponse<Boolean> exists(@RequestParam String key) throws IOException {
        if (storageService == null) return ApiResponse.fail("Storage not configured");
        return ApiResponse.ok(storageService.exists(key));
    }

    @DeleteMapping
    public ApiResponse<String> delete(@RequestParam String key) throws IOException {
        if (storageService == null) return ApiResponse.fail("Storage not configured");
        storageService.delete(key);
        return ApiResponse.ok("deleted");
    }
}
