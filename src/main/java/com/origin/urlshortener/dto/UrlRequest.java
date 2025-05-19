package com.origin.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UrlRequest {
    @NotBlank(message = "URL cannot be empty")
    private String url;
} 