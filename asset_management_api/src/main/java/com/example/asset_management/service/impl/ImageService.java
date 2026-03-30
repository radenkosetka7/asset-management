package com.example.asset_management.service.impl;

import com.example.asset_management.dto.request.ImageRequest;
import com.example.asset_management.dto.response.ImageResponse;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.Image;
import com.example.asset_management.repository.ImageRepository;
import com.example.asset_management.service.IAssetImageService;
import com.example.asset_management.service.IImageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ImageService implements IImageService {

    private final ImageRepository imageRepository;
    private final IAssetImageService assetImageService;
    private static final Path UPLOAD_DIR = Paths.get("assets");

    @Override
    public Set<ImageResponse> insert(Asset asset, List<MultipartFile> request) {
        if (request == null || request.isEmpty()) {
            return Set.of();
        }
        List<Image> images = request.stream()
                .map(image -> {
                    if (image == null || image.isEmpty()) {
                        throw new IllegalArgumentException("File must not be null or empty");
                    }
                    String storedFileName = storeFile(image);
                    return Image.builder()
                            .fileName(storedFileName)
                            .build();
                }).toList();
        log.info("Adding images");
        List<Image> savedImages = imageRepository.saveAll(images);
        return assetImageService.insert(asset, savedImages);
    }

    private String storeFile(MultipartFile file) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String originalName = file.getOriginalFilename();
            String safeName = originalName == null ? "file" : Paths.get(originalName).getFileName().toString();
            String storedName = UUID.randomUUID() + "_" + safeName;
            Path target = UPLOAD_DIR.resolve(storedName);
            file.transferTo(target);
            return storedName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file", ex);
        }
    }
}
