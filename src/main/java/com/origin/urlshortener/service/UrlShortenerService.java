package com.origin.urlshortener.service;

import com.origin.urlshortener.model.UrlMapping;
import com.origin.urlshortener.repository.UrlMappingRepository;
import com.origin.urlshortener.exception.UrlNotFoundException;
import com.origin.urlshortener.exception.InvalidUrlException;
import com.origin.urlshortener.util.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

@Service
public class UrlShortenerService {
    private final UrlMappingRepository urlMappingRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public UrlShortenerService(UrlMappingRepository urlMappingRepository, 
                             SnowflakeIdGenerator snowflakeIdGenerator) {
        this.urlMappingRepository = urlMappingRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Transactional
    public UrlMapping shortenUrl(String originalUrl) {
        validateUrl(originalUrl);

        // Check if URL already exists
        return urlMappingRepository.findByOriginalUrl(originalUrl)
                .orElseGet(() -> createNewUrlMapping(originalUrl));
    }

    @Transactional(readOnly = true)
    public UrlMapping getOriginalUrl(String shortCode) {
        return urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
    }

    private UrlMapping createNewUrlMapping(String originalUrl) {
        String shortCode = snowflakeIdGenerator.generateShortCode();
        
        // Ensure uniqueness of shortCode (extremely unlikely to happen with Snowflake IDs)
        while (urlMappingRepository.existsByShortCode(shortCode)) {
            shortCode = snowflakeIdGenerator.generateShortCode();
        }

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortCode(shortCode);
        urlMapping.setCreatedAt(LocalDateTime.now());

        return urlMappingRepository.save(urlMapping);
    }

    private void validateUrl(String url) {
        try {
            url = url.trim();  // Optional: strip whitespace
            URI uri = new URI(url);
            System.out.println("uri.getScheme():" + uri.getScheme());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new InvalidUrlException("Invalid URL format: scheme and host are required");
            }

            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new InvalidUrlException("Invalid URL scheme: only http and https are supported");
            }

            // Optional: require at least one dot (not for localhost or IPs)
            // if (!uri.getHost().contains(".")) {
            //     throw new InvalidUrlException("Host must be a valid domain (contains a dot)");
            // }

        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL format: " + e.getMessage());
        }
    }
} 