# Book Fair Stall Reservation System
## Security Enhancement Project

Student Number: SE22052

---

## 1. Project Overview
This project is a security-enhanced **Book Fair Stall Reservation System**. It allows stall vendors to authenticate, view events, check stall availability, create bookings, and manage their reservations. Exhibition organizers (admins) use the system to manage events, design stall layouts, review and approve/reject bookings, deactivate users, and update public web content.

The system was audited and updated to address critical vulnerabilities, including Broken Access Control (RBAC), Insecure Direct Object References (IDOR/BOLA), WebSocket security flaws, Mass Assignment, Race Conditions in concurrent double-booking, Rate Limiting, File Upload vulnerabilities, and Sensitive Data Exposure.

---

## 2. Technology Stack

### Frontend
- **Framework**: React (built with Vite)
- **Styling**: Tailwind CSS
- **Protocols**: STOMP, SockJS (for real-time stall layout synchronization)

### Backend
- **Framework**: Spring Boot 3.3.x (Java 17)
- **Security**: Spring Security (OAuth 2.0 Resource Server)
- **ORM / Persistence**: Spring Data JPA / Hibernate
- **WebSockets**: Spring WebSocket
- **Build Tool**: Maven

### Database
- **Database Engine**: MySQL 8.0+

### Authentication & Authorization
- **Identity Provider**: Auth0
- **Standards**: OAuth 2.0 / OpenID Connect (OIDC) / JWT (JSON Web Tokens)
- **Authorization**: Role-Based Access Control (RBAC) on REST endpoints and real-time destinations

### Other Security Libraries
- **Rate Limiting**: Bucket4j (for token-bucket rate limiting)
- **Caching**: Caffeine Cache (for managing rate-limiting buckets in-memory)

---

## 3. System Roles

### STALL_VENDOR
- **Authenticate**: Log in securely via the Auth0 federated portal.
- **Profile Management**: View and edit own personal profile details (Name, Phone, Business Name).
- **Event/Stall Browse**: View upcoming events, layouts, prices, and check stall booking status.
- **Reservation Creation**: Reserve available stalls for upcoming events and supply deposit billing information.
- **Reservation Management**: View list of own bookings, view specific details, and cancel bookings before deadlines.

### EXHIBITION_ORGANIZER
- **Event Management**: Create, edit, configure, and publish book fair events.
- **Stall Configuration**: Design stall placement coordinates, block/unblock stalls, and configure pricing.
- **Reservation Processing**: Oversee all vendor bookings, verify bank transfer details, and approve/reject/refund reservations.
- **User Management**: View user profiles and deactivate vendors.
- **Application Content**: Edit public page descriptions, upload video previews, and customize contact details.

---

## 4. Security Architecture

```text
React Client
      |
      | HTTPS (TLS 1.3)
      v
Spring Security (SecurityFilterChain)
      |
      | Bearer JWT Header
      v
Auth0 JWT Validation (Crytographic signature, issuer, audience checks)
      |
      v
RBAC / Authorization (Role mapping via custom claims)
      |
      v
Controllers (REST API Endpoints)
      |
      v
Services (Pessimistic DB Locks & Object-Level Owner Checks)
      |
      v
Spring Data JPA
      |
      v
MySQL Database
```

### WebSocket / STOMP Authentication
1. Real-time updates utilize SockJS fallback transports.
2. During the WebSocket STOMP `CONNECT` frame, the client passes the access token in a custom header.
3. The WebSocket channel interceptor (`WebSocketAuthChannelInterceptor.java`) intercepts the frame, decodes and validates the JWT, and binds the authenticated user identity to the WebSocket session context.
4. Subsequent subscription attempts (`SUBSCRIBE`) and message dispatches (`SEND`) are intercepted and checked against destination validation rules.

---

## 5. Security Vulnerabilities Audited and Fixed

### 5.1 Broken Access Control / RBAC
- **Risk**: Stall vendors accessing administrative APIs to modify events, stalls, or user states.
- **Security Implementation**: Restricts `/api/admin/**` and `/api/organizer/**` to clients with `ROLE_EXHIBITION_ORGANIZER`. REST endpoints are restricted by role filters defined in `SecurityConfig.java`.
- **Verification**: Verified that any authenticated client lacking the organizer role receives a `403 Forbidden` response.

### 5.2 IDOR / BOLA (Insecure Direct Object Reference)
- **Risk**: Vendors reading, modifying, or cancelling other vendors' bookings by guessing or sweeping reservation numeric IDs.
- **Security Implementation**: Added strict object-level validation checks in the service and controller layer (`ReservationController.java` & `ReservationServiceImpl.java`). The authenticated user ID is retrieved from the trusted security context (Auth0 subject identifier mapping) and compared against the owner of the target reservation record.
- **Verification**: A vendor calling `GET /api/reservations/{id}` or `POST /api/reservations/{id}/cancel` for a reservation owned by another vendor receives `403 Forbidden` ("Access denied - You do not own this reservation").

### 5.3 WebSocket Security
- **Risk**: Anonymous users subscribing to administrative channels, snooping on events, or dispatching malformed state changes.
- **Security Implementation**:
  - Validates Bearer tokens on `CONNECT` frames using standard signature checks.
  - Restricts subscriptions (`SUBSCRIBE` frames) to `/topic/admin/**` to organizers only.
  - Rejects `SEND` frames from client to server destination `/app/sync` if the vendor is not authenticated or attempts to spoof events/stalls.
- **Verification**: Unauthorized subscription attempts are intercepted and terminated at the channel level.

### 5.4 Mass Assignment / Over-posting
- **Risk**: Clients submitting parameters (like `id`, `role`, `email`, `sub`, or `enabled`) during profile updates to escalate privileges.
- **Security Implementation**: Created `ProfileUpdateRequest.java` to whitelist only updateable parameters (`name`, `phone`, `businessName`). Any other submitted field is ignored, preventing email tampering.
- **Verification**: Tested that submitting `"role": "EXHIBITION_ORGANIZER"` or `"email": "hacker@test.com"` does not overwrite the email or escalate user privileges.

### 5.5 Concurrent Double-Booking / Race Condition
- **Risk**: Two vendors concurrently booking the exact same stall for the same event.
- **Security Implementation**: Implemented database-level pessimistic write-locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the requested stalls. Lock acquisition is performed within a transactional booking workflow before final availability validation.
- **Verification**: Integration tests simulate concurrent threads booking the same stall: one succeeds and commits, while the other fails with `409 Conflict`.

### 5.6 Rate Limiting / Resource Abuse
- **Risk**: Brute-forcing endpoints, creating database/log spam, or abusing expensive WebSocket connection handshakes.
- **Security Implementation**: Filter-level rate limiting using Caffeine Cache and Bucket4j:
  - **Booking**: 3 requests/minute per authenticated Auth0 `sub`.
  - **Cancellation**: 5 requests/minute per authenticated Auth0 `sub`.
  - **Profile Updates**: 10 requests/minute per authenticated Auth0 `sub`.
  - **Public APIs**: 60 requests/minute per IP address.
  - **WebSocket handshakes**: 10 requests/minute per IP address.
- **Verification**: Exceeding the bucket allowance triggers a `429 Too Many Requests` response containing a rounded-up `Retry-After` header. In-memory storage is per-JVM instance.

### 5.7 File Upload Security
- **Risk**: Attackers uploading JSP scripts, executable files, or HTML scripts with path traversal payloads.
- **Security Implementation**:
  - Restricts uploads strictly to `ROLE_EXHIBITION_ORGANIZER`.
  - Validates actual file content by checking the first 12 bytes (magic bytes signature verification).
  - Decodes images using `ImageIO.read` to block polyglots and scripts disguised as images.
  - Discards original client filenames and generates randomized UUID filenames.
- **Verification**: Uploading a `.txt` file renamed to `.jpg` or spoofing the MIME header is blocked and returns `400 Bad Request`.

### 5.8 Sensitive Data Exposure / Information Disclosure
- **Risk**: Leakage of credentials, OIDC scopes, internal stack traces, database schema details, or SMTP logs.
- **Security Implementation**:
  - Removed all `System.out.println` debug outputs logging JWT claims or credentials.
  - Disabled SMTP protocol debug logging (`spring.mail.properties.mail.debug=false`).
  - Added a generic `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler.java` mapping unhandled exceptions to a generic JSON `500 Internal Server Error` response.
- **Verification**: Verified that stack traces, table names, and SMTP connection data do not appear in console stdout or HTTP responses.

---

## 6. Additional Security Audits

### 6.1 CSRF (Cross-Site Request Forgery)
The backend does not use session cookies for authentication. Since all endpoints validate stateless Bearer JWT tokens attached in the `Authorization` header, the backend is protected against CSRF attacks. CSRF filters are disabled in `SecurityConfig.java` to prevent unnecessary overhead.

### 6.2 Cross-Site Scripting (XSS)
- Frontend uses React's native JSX text escaping by default.
- No instances of `dangerouslySetInnerHTML` are used.
- Added Content Security Policy (CSP) and HSTS security headers at the Spring Security level to prevent script injections and protocol downgrades.

### 6.3 SQL Injection
- Implemented data-access layer using Spring Data JPA.
- Derived queries and JPQL queries use binding variables rather than string concatenation, protecting the database from injection attempts.

---

## 7. Security Testing

The project includes the following automated test suites:
- **`BookingConcurrencyTest.java`**: Simulates concurrent double-booking attempts and asserts transaction serializability.
- **`RateLimitingIntegrationTest.java`**: Validates rate limits, separate vendor buckets, IP-based public limits, and WebSocket handshake blocks.
- **`FileUploadIntegrationTest.java`**: Evaluates file signatures, ImageIO parsing, path traversal protection, and role-based upload limits.
- **`SensitiveDataExposureTest.java`**: Asserts generic error sanitization and specific exception mapping.

### Test Run Status
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 8. Security Headers / HTTPS

Spring Security is configured to output standard security headers:
- **Content-Security-Policy (CSP)**: Restricts script sources to `'self'`.
- **HSTS (HTTP Strict Transport Security)**: Enforces HTTPS connection lifetimes.
- **X-Content-Type-Options**: Set to `nosniff`.
- **X-Frame-Options**: Set to `DENY` to protect against clickjacking.

---

## 9. Project Structure

```text
bookfair-security-enhancement/
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bookfair/Stall_Reservation/
│   │   │   │   ├── config/             # SecurityConfig, WebConfig, exception handlers
│   │   │   │   ├── controller/         # REST API Controllers (Vendor, public)
│   │   │   │   ├── controller/admin/   # Admin Controllers (Organizer operations)
│   │   │   │   ├── dto/                # Request DTOs
│   │   │   │   ├── entity/             # JPA Database Entities
│   │   │   │   ├── repository/         # Data Access Repositories
│   │   │   │   ├── security/           # RateLimiter and OIDC filter interceptors
│   │   │   │   └── service/            # Core business layer
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── keystore.p12        # SSL development certificate
│   │   └── test/                       # Security Integration test suites
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── src/
    │   ├── api/                        # Client API and header builders
    │   ├── context/                    # Auth0 provider contexts
    │   └── pages/                      # Vendor and Admin pages
```

---

## 10. Environment Configuration

### Backend Environment Variables
Create a local script or set properties in your IDE profile:
```env
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
OIDC_ISSUER=https://your-auth0-domain/
OIDC_AUDIENCE=your-api-audience
MAIL_USERNAME=your_email_username
MAIL_PASSWORD=your_email_app_password
SSL_KEYSTORE_PASSWORD=your_ssl_keystore_password
```

### Frontend Environment Variables
Create a `frontend/.env` file:
```env
VITE_OIDC_AUTHORITY=https://your-auth0-domain/
VITE_OIDC_CLIENT_ID=your_auth0_client_id
VITE_OIDC_REDIRECT_URI=https://localhost:5173/callback
VITE_OIDC_POST_LOGOUT_REDIRECT_URI=https://localhost:5173/
VITE_OIDC_AUDIENCE=https://bookfair-api
VITE_OIDC_SCOPE=openid profile email roles
```

---

## 11. Running the Application

### 1. Database Initialization
Create database in MySQL and run:
```bash
mysql -u root -p < database/schema.sql
```

### 2. Run Spring Boot Backend
From the `backend` folder:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:OIDC_ISSUER="https://your-auth0-domain/"
$env:OIDC_AUDIENCE="https://your-audience"
$env:MAIL_USERNAME="your_email"
$env:MAIL_PASSWORD="your_email_app_password"
$env:SSL_KEYSTORE_PASSWORD="your_keystore_password"

.\mvnw.cmd spring-boot:run
```
Backend API will be running on `https://localhost:8443`.

### 3. Run React Frontend
From the `frontend` folder:
```bash
npm install
npm run dev
```
Frontend will be running on `https://localhost:5173`.

---

## 12. Security Limitations / Future Improvements
- **Rate Limiting Scalability**: The current Bucket4j/Caffeine rate-limiting implementation is in-memory and binds tokens per-JVM. For horizontally scaled multi-instance deployments, it must be migrated to a distributed solution (e.g. using Redis).
- **Client Token Storage**: sessionStorage access tokens can still be intercepted via memory-access scripts if an XSS vulnerability is introduced. BFF (Backend-For-Frontend) and HttpOnly cookies should be evaluated.
- **Log Security & Monitoring**: Centralized SIEM audit logs should be configured to capture and parse access logs in a production environment.

---

## 13. Author

Student Number: SE22052