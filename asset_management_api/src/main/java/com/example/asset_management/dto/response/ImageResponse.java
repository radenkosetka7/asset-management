package com.example.asset_management.dto.response;

import com.example.asset_management.model.AssetImage;
import com.example.asset_management.model.Image;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record ImageResponse(Long id, String fileName) implements Serializable {

    public static ImageResponse from(Image image) {
        return new ImageResponse(image.getId(), image.getFileName());
    }

    public static Set<ImageResponse> from(Set<AssetImage> images) {
        return images.stream().map(AssetImage::getImage)
                .map(ImageResponse::from).collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
