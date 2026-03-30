package com.example.asset_management.controller;

import com.example.asset_management.service.IAssetImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/assets/images")
@RequiredArgsConstructor
public class AssetImageController {

    private final IAssetImageService assetImageService;
    private static final Path UPLOAD_DIR = Paths.get("assets");

    @GetMapping("/file/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = UPLOAD_DIR.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = MediaType.IMAGE_JPEG_VALUE;
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) contentType = MediaType.IMAGE_PNG_VALUE;
            else if (lower.endsWith(".gif")) contentType = MediaType.IMAGE_GIF_VALUE;
            else if (lower.endsWith(".webp")) contentType = "image/webp";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{assetId}/{imageId}")
    public ResponseEntity<Void> deleteAssetImage(@PathVariable Long assetId, @PathVariable Long imageId) {
        assetImageService.deleteImage(assetId, imageId);
        return ResponseEntity.noContent().build();
    }
}
