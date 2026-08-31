# Security Architecture Design

This document details the security architecture of the Stall Reservation System, illustrating the secure integration of OIDC, token-based authentication, backend role-based access control (RBAC), and database protections.

---

## 1. Overall System Architecture
The application runs as a secure decoupled system using standard web security protocols:

```mermaid
graph TD
    Client["React SPA (https://localhost:5173)"]
    API["Spring Boot Backend (https://localhost:8443)"]
    DB[("MySQL Database")]
    IdP["OIDC Identity Provider (Auth0/Cognito/Okta)"]
    
    Client -- "1. Single Sign-On (Auth Code + PKCE)" --> IdP
    IdP -- "2. Issued Access Token" --> Client
    Client -- "3. HTTPS Request + Bearer Token" --> API
    API -- "4. Token Signature & Expiry Check" --> IdP
    API -- "5. SQL Operations" --> DB
```

---

## 2. Authentication Flow (OIDC + PKCE)
Authentication is delegated entirely to the cloud Identity Provider using OAuth2.0 / OpenID Connect authorization code flow with Proof Key for Code Exchange (PKCE) to prevent interception attacks:

```mermaid
sequenceDiagram
    autonumber
    actor User as Vendor/Organizer
    participant UI as React SPA
    participant IdP as Identity Provider
    participant API as Spring Boot API

    User->>UI: Click Login Button
    Note over UI: Generate Code Verifier & Challenge
    UI->>IdP: Redirect to /authorize?code_challenge=...
    IdP->>User: Render Login Interface & Authenticate
    User->>IdP: Submit Credentials
    IdP->>UI: Redirect with Authorization Code
    UI->>IdP: POST /token + code_verifier + auth_code
    IdP->>UI: Return ID & Access Token (JWT)
    UI->>API: HTTP Request (Authorization: Bearer <token>)
    API->>API: Verify Signature, Issuer, Expiry, Audience
    API->>User: Render Secured Resource
```

---

## 3. Token Processing & Role Authorization (Backend)
Once an authenticated request arrives at the Spring Boot backend, it runs through the Spring Security Filter Chain:

```mermaid
graph TD
    Request["Incoming API Request"] --> ResourceServer["OAuth2 Resource Server Filter"]
    ResourceServer --> TokenValid{"Token Signature, Issuer & Expiry Valid?"}
    
    TokenValid -- "No" --> Ret401["Return 401 Unauthorized"]
    TokenValid -- "Yes" --> AuthoritiesConverter["JwtAuthenticationConverter"]
    
    AuthoritiesConverter --> MapRoles["Extract 'roles' / 'groups' claims into ROLE_ authorities"]
    MapRoles --> UserMappingFilter["OidcUserMappingFilter"]
    
    UserMappingFilter --> DBCheck{"User exists in DB?"}
    DBCheck -- "No" --> CreateUser["Create Local User Profile tied to OIDC 'sub'"]
    DBCheck -- "Yes" --> SetSecurityContext["Set User ID as Principal in SecurityContext"]
    
    CreateUser --> SetSecurityContext
    SetSecurityContext --> SecurityRules{"Matches SecurityConfig Matchers?"}
    
    SecurityRules -- "No" --> Ret403["Return 403 Forbidden"]
    SecurityRules -- "Yes" --> Controller["REST Controller"]
```

---

## 4. End-to-End Encryption & CORS Constraints
*   **HTTPS/TLS**: Both the React dev server and the Spring Boot backend are configured to enforce TLS 1.3 to encrypt traffic in transit.
*   **Restricted CORS**: Wildcard origins are disabled. Only configured origins (e.g. `https://localhost:5173`) are allowed to send authorization headers and read response bodies.
