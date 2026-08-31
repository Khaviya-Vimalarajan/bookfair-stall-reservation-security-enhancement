# Walkthrough - Security Enhancements Completed

I have implemented and verified several key security enhancements and bug fixes across the application.

## 1. Concurrency & Double-Booking Prevention
* **Pessimistic Locking**: Added `@Lock(LockModeType.PESSIMISTIC_WRITE)` to `findAllByIdForUpdate` in [**`StallRepository.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/repository/StallRepository.java). This blocks concurrent threads trying to access the same stalls in the database.
* **Locking Read for Re-check**: Modified [**`ReservationStallRepository.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/repository/ReservationStallRepository.java) to add `@Lock(LockModeType.PESSIMISTIC_READ)` on the availability check query `findBookedStallIdsByEventId`. This bypasses the repeatable-read transaction snapshot and reads the most up-to-date committed bookings.
* **Safe 409 Conflict Exception**: Updated `ReservationServiceImpl.java` to return `IllegalStateException("One or more selected stalls are no longer available.")` which resolves to a clean `409 Conflict` response to the client.

## 2. Rate Limiting & Resource Abuse Protection
* **Filter Chain Registration**: Created [**`RateLimitingFilter.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/security/RateLimitingFilter.java) and configured it in [**`SecurityConfig.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/config/SecurityConfig.java) to sit explicitly after `BearerTokenAuthenticationFilter` and before `OidcUserMappingFilter`.
* **Sub-Based Identity Limits**: For authenticated requests, the filter extracts the validated OIDC `sub` directly from the token, preventing client-spoofed identities.
* **IP-Based Public/Handshake Limits**: For unauthenticated routes and WebSocket handshakes, the filter applies rate limits strictly based on `request.getRemoteAddr()`.
* **WebSocket SocksJS Transport Isolation**: Only WebSocket handshake targets (`/ws/info` and Upgrade header) are rate limited. Routine SockJS polling frames are bypassed to prevent connection drops.
* **Caffeine & Bucket4j Storage**: Implemented token-bucket rate limiting with Caffeine Cache configured with `maximumSize(10000)` and `expireAfterAccess(5, TimeUnit.MINUTES)` to prevent memory exhaustion vulnerabilities.
* **Retry-After Header**: Rounded up nanofractions to return a positive integer `Retry-After` header when returning `429 Too Many Requests`.

## 3. File Upload Security
* **Bug Fix for Extension Mapping**: Fixed the swapped image/video extension mapping bug in `AdminContentController.java` (`uploadVideo` now correctly maps video formats and `uploadImage` maps image formats).
* **Streaming Content Validation (Magic Bytes)**: Implemented streaming signature verification (using the first 12 bytes of a `BufferedInputStream`) to determine the actual file type, bypassing spoofed client-supplied `Content-Type` headers.
* **Image Decoding Verification**: Added server-side validation that attempts to decode all uploaded images using `ImageIO.read()`, rejecting corrupted, empty, or executable/HTML files disguised as images.
* **MIME Whitelist Limits**:
  * **Images**: `image/jpeg` (mapped to `.jpg`), `image/png` (mapped to `.png`), and `image/gif` (mapped to `.gif`).
  * **Videos**: `video/mp4` (mapped to `.mp4`), `video/webm` (mapped to `.webm`), and `video/ogg` (mapped to `.ogg`).
* **Path Traversal Protection**: Discards user-provided original filenames entirely and generates random unique UUID filenames on the server.
* **Empty/Zero-Byte Rejections**: Rejects empty and zero-byte uploads early in the process.

## 4. Verification Results

### A. Concurrency Verification
We wrote and executed [**`BookingConcurrencyTest.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/test/java/com/bookfair/Stall_Reservation/BookingConcurrencyTest.java):
* Proves that two concurrent vendor threads attempting to book the same stall are serialized, with exactly one succeeding and the other failing with a `409 Conflict`.
* **Result**: Passed.

### B. Rate Limiting Verification
We wrote and executed [**`RateLimitingIntegrationTest.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/test/java/com/bookfair/Stall_Reservation/RateLimitingIntegrationTest.java):
* **Booking Creation**: First 3 requests succeed; 4th request gets `429 Too Many Requests` with a valid JSON body and `Retry-After` header. A different vendor gets a separate bucket.
* **Booking Cancellation**: First 5 requests are checked; 6th gets `429`.
* **Profile Updates**: First 10 requests are checked; 11th gets `429`.
* **WebSocket Handshakes**: First 10 handshakes succeed; 11th gets `429`. SockJS `xhr` transport endpoints bypass rate limiting to prevent drops.
* **Public APIs**: First 60 requests succeed; 61st gets `429`.
* **Result**: Passed with `BUILD SUCCESS`.

### C. File Upload Verification
We wrote and executed [**`FileUploadIntegrationTest.java`**](file:///c:/Users/khavi/OneDrive/Desktop/IS\bookfair-security-enhancement/backend/src/test/java/com/bookfair/Stall_Reservation/FileUploadIntegrationTest.java):
* **Valid Images**: PNG and JPEG are accepted, decoded, and saved.
* **Spoofed Uploads**: Plain text files with spoofed `image/jpeg` or `video/mp4` headers are rejected with `400 Bad Request`.
* **Renamed Files**: Text files renamed with `.jpg` extensions are rejected.
* **Path Traversal**: Filenames containing `../../../exploit.png` payloads do not write outside the upload directories and are safely randomized.
* **Authorization**: Vendors trying to access admin uploads receive a standard `403 Forbidden`.
* **Empty Uploads**: Zero-byte file uploads are rejected.
* **Result**: Passed with `BUILD SUCCESS`.
