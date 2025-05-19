package com.origin.urlshortener.controller;

import com.origin.urlshortener.model.UrlMapping;
import com.origin.urlshortener.service.UrlShortenerService;
import com.origin.urlshortener.dto.UrlRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/shortener")
public class UrlShortenerController {
    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping
    public ResponseEntity<UrlMapping> shortenUrl(@Valid @RequestBody UrlRequest request) {
        UrlMapping urlMapping = urlShortenerService.shortenUrl(request.getUrl());
        return new ResponseEntity<>(urlMapping, HttpStatus.CREATED);
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirectToOriginalUrl(@PathVariable String shortCode) {
        UrlMapping urlMapping = urlShortenerService.getOriginalUrl(shortCode);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(urlMapping.getOriginalUrl());
        return redirectView;
    }

    @GetMapping("/info/{shortCode}")
    public ResponseEntity<UrlMapping> getUrlInfo(@PathVariable String shortCode) {
        UrlMapping urlMapping = urlShortenerService.getOriginalUrl(shortCode);
        return ResponseEntity.ok(urlMapping);
    }
} 