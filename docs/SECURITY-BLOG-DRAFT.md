# Security Blog Draft (Template & Guidelines)

*This is a template and guideline document for the student/developer to write their own authentic blog post/article describing the security enhancements they performed on the Book Fair Stall Reservation System.*

> [!IMPORTANT]
> **Instructions for the Student/Developer:**
> - Do not copy another student's experiences or present their work as your own.
> - Write about the actual steps you executed, challenges you faced, and solutions you implemented.
> - Ensure no real passwords, credentials, API keys, or private tokens are included in the final draft.

---

## Suggested Article Structure

### 1. Introduction
- Briefly introduce the Stall Reservation System.
- Explain the key security goals of this project (e.g., delegating identity authentication, encrypting communication, enforcing access controls).
- Highlight the importance of aligning the application architecture with modern secure web development standards (OWASP Top 10, Zero-Trust).

### 2. Federated Identity (OIDC & OAuth 2.0 with PKCE)
- Explain why delegating credentials management to a cloud Identity Provider (like Auth0) is superior to local database user/password management.
- Detail the **Authorization Code Flow with PKCE**:
  - Why is PKCE critical for public clients (Single Page Applications like React) that cannot securely store client secrets?
  - Describe the challenge-response handshake (`Code Verifier` and `Code Challenge`) and token exchange sequence.
- Describe how you configured your frontend OIDC client library (`oidc-client-ts` or Auth0 SPA SDK) using environment variables.

### 3. Backend Token Validation & Security Mapping
- Explain how the Spring Boot backend acts as a secure **OAuth 2.0 Resource Server**.
- Discuss the JWT verification checks performed by the backend:
  - Cryptographic signature check using the provider's JWKS endpoint.
  - Issuer (`iss`), Audience (`aud`), and Expiry (`exp`) claim validations.
- Describe how the OIDC subject claim (`sub`) is extracted and mapped to the local application database users securely (e.g., through filter chains or interceptors).

### 4. Privilege Escalation & Role-Based Access Control (RBAC)
- Define the user roles: `STALL_VENDOR` and `EXHIBITION_ORGANIZER`.
- Explain how user roles are securely embedded in the OIDC Access Token claims (e.g., namespace claims like `https://bookfair-app/roles`) and mapped to Spring Security authorities (`ROLE_...`).
- Detail the URL access rules and Method Security configured on the backend controllers.

### 5. OWASP Top 10 Defenses Implemented
Provide concrete sections for defenses you reviewed or reinforced, for example:
- **A01:2021-Broken Access Control**: Prevention of Insecure Direct Object References (IDOR). How did you ensure a vendor can only view, create, or cancel their own bookings?
- **A02:2021-Cryptographic Failures**: Enforcing HTTPS/TLS in transit on both the frontend and backend.
- **A03:2021-Injection**: Using Spring Data JPA parameterized queries to prevent SQL Injection, and Jakarta validations to enforce request object bounds.
- **A05:2021-Security Misconfiguration**: Restricting CORS allowed origins and defining strict Content Security Policies (CSP).
- **WebSocket Security**: Authenticating STOMP connections using Bearer tokens and restricting admin updates topics.
- **File Upload Security**: Enforcing restricted file types, size limits, and randomized server-side UUID storage names to prevent remote code execution (RCE).

### 6. Personal Reflection & Key Technical Challenges
- What was the most challenging part of implementing this security architecture? (e.g., configuring custom claims mapping in Auth0, handling local SSL certificates, debugging WebSocket interceptors).
- Explain how you resolved these challenges.
- Highlight key lessons learned regarding secure application design and the separation of identity provider management from application business logic.
