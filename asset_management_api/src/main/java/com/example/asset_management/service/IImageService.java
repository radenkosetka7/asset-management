package com.example.asset_management.service;

import com.example.asset_management.dto.request.ImageRequest;
import com.example.asset_management.dto.response.ImageResponse;
import com.example.asset_management.model.Asset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface IImageService {

    Set<ImageResponse> insert(Asset asset, List<MultipartFile> images);
}
