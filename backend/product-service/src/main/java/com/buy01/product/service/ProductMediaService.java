package com.buy01.product.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.buy01.product.DTOs.MediaResponse;
import com.buy01.product.client.MediaClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private final MediaClient mediaClient;

    public List<String> uploadImages(List<MultipartFile> images, String productId) {

        List<String> imageIds = new ArrayList<>();

        for (MultipartFile image : images) {
            MediaResponse media = mediaClient.upload(image, productId);
            imageIds.add(media.id());
        }

        return imageIds;
    }

    public void deleteImages(List<String> imageIds) {

        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }

        imageIds.forEach(mediaClient::delete);
    }
}