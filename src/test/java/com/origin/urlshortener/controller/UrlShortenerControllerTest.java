package com.origin.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.origin.urlshortener.dto.UrlRequest;
import com.origin.urlshortener.model.UrlMapping;
import com.origin.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
public class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @Test
    public void testShortenUrl() throws Exception {
        String originalUrl = "https://www.originenergy.com.au/electricity-gas/plans.html";
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(1L);
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortCode("abc123");
        urlMapping.setCreatedAt(LocalDateTime.now());

        // Only mock successful response for this specific valid URL
        when(urlShortenerService.shortenUrl(originalUrl)).thenReturn(urlMapping);

        UrlRequest request = new UrlRequest();
        request.setUrl(originalUrl);

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    public void testRedirectToOriginalUrl() throws Exception {
        String shortCode = "abc123";
        String originalUrl = "https://www.originenergy.com.au/electricity-gas/plans.html";
        
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortCode(shortCode);

        when(urlShortenerService.getOriginalUrl(shortCode)).thenReturn(urlMapping);

        mockMvc.perform(get("/api/v1/{shortCode}", shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(originalUrl));
    }

    @Test
    public void testGetUrlInfo() throws Exception {
        String shortCode = "abc123";
        LocalDateTime createdAt = LocalDateTime.now();
        
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(1L);
        urlMapping.setOriginalUrl("https://www.originenergy.com.au/electricity-gas/plans.html");
        urlMapping.setShortCode(shortCode);
        urlMapping.setCreatedAt(createdAt);

        when(urlShortenerService.getOriginalUrl(shortCode)).thenReturn(urlMapping);

        mockMvc.perform(get("/api/v1/info/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.originalUrl").value(urlMapping.getOriginalUrl()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    public void testShortenUrlWithInvalidInput() throws Exception {
        // Test empty URL
        UrlRequest emptyRequest = new UrlRequest();
        emptyRequest.setUrl("");

        when(urlShortenerService.shortenUrl(""))
            .thenThrow(new com.origin.urlshortener.exception.InvalidUrlException("Validation failed: {url=URL cannot be empty}"));

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed: {url=URL cannot be empty}"))
                .andExpect(jsonPath("$.timestamp").exists());

        // Test null URL
        UrlRequest nullRequest = new UrlRequest();
        nullRequest.setUrl(null);

        when(urlShortenerService.shortenUrl(null))
            .thenThrow(new com.origin.urlshortener.exception.InvalidUrlException("Validation failed: {url=URL cannot be empty}"));

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed: {url=URL cannot be empty}"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testShortenUrlWithInvalidUrlFormat() throws Exception {
        // Test URLs without scheme
        testInvalidUrl("www.example.com");
        testInvalidUrl("example.com");
        
        // Test URLs without host
        testInvalidUrl("http://");
        testInvalidUrl("https://");
        
        // Test URLs with invalid scheme
        testInvalidUrl("ftp://example.com");
        testInvalidUrl("ws://example.com");
        
        // Test malformed URLs
        testInvalidUrl("not-a-valid-url");
        testInvalidUrl("http:///example.com");
        testInvalidUrl("http://example");
    }

    private void testInvalidUrl(String invalidUrl) throws Exception {
        UrlRequest request = new UrlRequest();
        request.setUrl(invalidUrl);

        // Mock service to throw InvalidUrlException for invalid URLs
        when(urlShortenerService.shortenUrl(invalidUrl))
            .thenThrow(new com.origin.urlshortener.exception.InvalidUrlException("Validation failed: " + invalidUrl));

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed: " + invalidUrl))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testShortenUrlWithValidUrls() throws Exception {
        // Test valid HTTP URLs
        testValidUrl("http://example.com");
        testValidUrl("http://example.com/path");
        testValidUrl("http://example.com/path?query=value");
        
        // Test valid HTTPS URLs
        testValidUrl("https://example.com");
        testValidUrl("https://example.com/path");
        testValidUrl("https://example.com/path?query=value");
        
        // Test URLs with subdomains
        testValidUrl("https://sub.example.com");
        testValidUrl("https://sub1.sub2.example.com");
        
        // Test URLs with ports
        testValidUrl("http://example.com:8080");
        testValidUrl("https://example.com:8443");
    }

    private void testValidUrl(String validUrl) throws Exception {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(1L);
        urlMapping.setOriginalUrl(validUrl);
        urlMapping.setShortCode("abc123");
        urlMapping.setCreatedAt(LocalDateTime.now());

        // Only mock successful response for this specific valid URL
        when(urlShortenerService.shortenUrl(validUrl)).thenReturn(urlMapping);

        UrlRequest request = new UrlRequest();
        request.setUrl(validUrl);

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value(validUrl))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    public void testGetUrlInfoNotFound() throws Exception {
        String shortCode = "nonexistent";
        when(urlShortenerService.getOriginalUrl(shortCode))
            .thenThrow(new com.origin.urlshortener.exception.UrlNotFoundException("Short URL not found: " + shortCode));

        mockMvc.perform(get("/api/v1/info/{shortCode}", shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testRedirectToOriginalUrlNotFound() throws Exception {
        String shortCode = "nonexistent";
        when(urlShortenerService.getOriginalUrl(shortCode))
            .thenThrow(new com.origin.urlshortener.exception.UrlNotFoundException("Short URL not found: " + shortCode));

        mockMvc.perform(get("/api/v1/{shortCode}", shortCode))
                .andExpect(status().isNotFound());
    }
} 