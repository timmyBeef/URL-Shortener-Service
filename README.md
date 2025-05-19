# URL Shortener Service

A simple URL shortener service built with Java 21 and Spring Boot that allows users to create short URLs from long URLs and redirect to the original URLs when accessed.

## Features

- Shorten long URLs to short, unique codes
- Redirect to original URLs when short URLs are accessed
- Get information about shortened URLs (creation time)
- Distributed-friendly ID generation using Twitter Snowflake algorithm
- URL validation
- In-memory database (H2) for storage
- RESTful API endpoints
- Comprehensive test coverage

## ID Generation (Twitter Snowflake Implementation)

The service uses Twitter's classic Snowflake algorithm for generating unique, distributed-friendly IDs. The implementation follows the original Twitter design:

### Bit Allocation
- **Sign Bit**: 1 bit
  - Always 0
  - Reserved for future use (can distinguish between signed/unsigned numbers)
- **Timestamp**: 41 bits
  - Milliseconds since Twitter epoch (1288834974657L - Nov 04, 2010, 01:42:54 UTC)
  - Supports ~69 years of timestamps
- **Datacenter ID**: 5 bits
  - Supports 32 datacenters (0-31)
- **Machine ID**: 5 bits
  - Supports 32 machines per datacenter (0-31)
- **Sequence**: 12 bits
  - Supports 4096 unique IDs per millisecond per machine
  - Resets to 0 every millisecond

Total: 64 bits, encoded in Base62 for URL-friendly representation

### Why Base62?
The service uses Base62 encoding (0-9, A-Z, a-z) instead of Base64 for several important reasons:

1. **URL Safety**: 
   - Base64 includes two special characters (`+` and `/`) that are not URL-safe
   - These characters would need to be URL-encoded (as `%2B` and `%2F`), making URLs longer
   - Base62 uses only alphanumeric characters, which are always URL-safe

2. **Character Set**:
   - Numbers (0-9): 10 characters
   - Uppercase letters (A-Z): 26 characters
   - Lowercase letters (a-z): 26 characters
   - Total: 62 characters, all URL-safe

3. **Benefits**:
   - No special characters that might cause issues in different contexts
   - No need for URL encoding/decoding
   - Case-sensitive, providing more possible combinations
   - Clean, readable URLs
   - Compatible with all web browsers and systems

4. **Comparison with Base64**:
   ```
   Base64: "abc+def/ghi" → "abc%2Bdef%2Fghi" (longer URL)
   Base62: "abc123def45"   → "abc123def45" (clean URL)
   ```

### Benefits
- **Time-ordered**: IDs are naturally ordered by creation time
- **Distributed-friendly**: Can generate unique IDs across 32 datacenters with 32 machines each
- **High Throughput**: Supports 4,096 IDs per millisecond per machine
- **Long Lifespan**: ~69 years of unique IDs from epoch
- **URL-safe**: Uses Base62 encoding (0-9, A-Z, a-z) for URL-friendly short codes
- **Fixed length**: Consistently generates 11-character short codes
- **No collisions**: Extremely low probability of ID collisions
- **No special characters**: All characters are URL-safe (no need for encoding)

### Example
Original URL: `https://www.originenergy.com.au/electricity-gas/plans.html`
Short URL: `http://localhost:8080/api/v1/shortener/2I9Sj7lBtLM`

### Technical Details
- Base62 encoding for URL-friendly representation
- Thread-safe implementation using synchronized method
- Clock drift protection
- Consistent 11-character length for all short codes
- Support for up to 4,194,304 unique IDs per second (32 datacenters × 32 machines × 4096 sequences)

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

## Getting Started

1. Clone the repository:
```bash
git clone <repository-url>
cd url-shortener
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Shorten URL
```http
POST /api/v1/shortener
Content-Type: application/json

{
    "url": "https://www.originenergy.com.au/electricity-gas/plans.html"
}
```

Response:
```json
{
    "id": 1,
    "originalUrl": "https://www.originenergy.com.au/electricity-gas/plans.html",
    "shortCode": "2I9Sj7lBtLM",
    "createdAt": "2025-03-14T10:00:00"
}
```

### Redirect to Original URL
```http
GET /api/v1/shortener/{shortCode}
```
This will redirect to the original URL. 
You can paste it in the browser to test (When the SpringBoot App Server is active)

### Get URL Information
```http
GET /api/v1/shortener/info/{shortCode}
```

Response:
```json
{
    "id": 1,
    "originalUrl": "https://www.originenergy.com.au/electricity-gas/plans.html",
    "shortCode": "2I9Sj7lBtLM",
    "createdAt": "2025-03-14T10:00:00"
}
```

## H2 Database Console

The H2 database console is available at `http://localhost:8080/h2-console`

Database connection details:
- JDBC URL: `jdbc:h2:mem:db`
- Username: `sa`
- Password: `CnB7aA`

## Running Tests

```bash
mvn test
```

## Error Handling

The API implements a global exception handler (`@RestControllerAdvice`) that provides consistent error responses across all endpoints. All errors follow a standard format:

```json
{
    "status": 400,
    "message": "Invalid URL format: http://invalid-url",
    "timestamp": "2024-03-14T10:00:00"
}
```

### HTTP Status Codes and Error Types

1. **400 Bad Request**
   - Invalid URL format
   - Validation errors (e.g., empty URL)
   ```json
   {
       "status": 400,
       "message": "Validation failed: {url=URL cannot be empty}",
       "timestamp": "2024-03-14T10:00:00"
   }
   ```

2. **404 Not Found**
   - Short URL not found
   ```json
   {
       "status": 404,
       "message": "Short URL not found: abc123",
       "timestamp": "2024-03-14T10:00:00"
   }
   ```

3. **500 Internal Server Error**
   - Unexpected server errors
   ```json
   {
       "status": 500,
       "message": "An unexpected error occurred: [error details]",
       "timestamp": "2024-03-14T10:00:00"
   }
   ```

The global exception handler catches and processes:
- `UrlNotFoundException`
- `InvalidUrlException`
- `MethodArgumentNotValidException` (validation errors)
- All other uncaught exceptions

## Technical Details

- Java 21
- Spring Boot 3.2.3
- Spring Data JPA
- H2 Database
- Lombok
- JUnit 5
- Mockito
- Custom Snowflake ID Generator

## Security Considerations

- URL validation to prevent malicious URLs
- Input sanitization

## URL Validation

The service performs strict URL validation to ensure only valid URLs are shortened:

1. **Format Validation**
   - Must be a valid URI format
   - Must include a scheme (http or https)
   - Must include a host
   - Only http and https schemes are supported

2. **Error Messages**
   ```json
   {
       "status": 400,
       "message": "Invalid URL format: scheme and host are required",
       "timestamp": "2025-03-14T10:00:00"
   }
   ```
   ```json
   {
       "status": 400,
       "message": "Invalid URL scheme: only http and https are supported",
       "timestamp": "2025-03-14T10:00:00"
   }
   ``` 