# Security Verification & Testing Report

This document records the security test cases executed against the secure Stall Reservation System.

---

## Test Results Registry

### TEST 1: No access token accessing protected API
*   **Vulnerability/Requirement**: Broken Access Control (Unauthenticated entry).
*   **Attack/Test**: Send a `GET` request to `/api/reservations/my` without any `Authorization` header.
*   **Request**: `GET /api/reservations/my` (no headers)
*   **Expected Result**: `401 Unauthorized` response with a secure custom JSON payload.
*   **Actual Result**: `401 Unauthorized` (exited with JSON body: `{"message": "Unauthenticated", "error": "Unauthorized"}`).
*   **Mitigation**: Spring Security filter chain `.anyRequest().authenticated()` blocks unauthenticated calls.
*   **Relevant Code**: [`SecurityConfig.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/config/SecurityConfig.java#L58-L81)

---

### TEST 2: Malformed/invalid access token
*   **Vulnerability/Requirement**: Broken Cryptographic Authentication.
*   **Attack/Test**: Send a request with a fake JWT string as Bearer token.
*   **Request**: `GET /api/reservations/my` with header `Authorization: Bearer invalid.jwt.signature`
*   **Expected Result**: `401 Unauthorized`.
*   **Actual Result**: `401 Unauthorized`.
*   **Mitigation**: Nimbus JWT decoder automatically validates signatures against JWKS.
*   **Relevant Code**: [`SecurityConfig.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/config/SecurityConfig.java#L82-L100)

---

### TEST 3: STALL_VENDOR accesses organizer API
*   **Vulnerability/Requirement**: Broken Access Control / Privilege Escalation.
*   **Attack/Test**: Authenticate as a user possessing the `STALL_VENDOR` role, then attempt to request `/api/admin/dashboard`.
*   **Request**: `GET /api/admin/dashboard` + Header `Authorization: Bearer <vendor_token>`
*   **Expected Result**: `403 Forbidden`.
*   **Actual Result**: `403 Forbidden`.
*   **Mitigation**: Spring Security antMatchers restrict `/api/admin/**` to `ROLE_EXHIBITION_ORGANIZER`.
*   **Relevant Code**: [`SecurityConfig.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/config/SecurityConfig.java#L66-L68)

---

### TEST 4: Vendor accesses another vendor's reservation (IDOR)
*   **Vulnerability/Requirement**: Broken Object Level Authorization (IDOR) - OWASP A1.
*   **Attack/Test**: Vendor A tries to fetch details of a reservation (e.g. ID `11`) belonging to Vendor B.
*   **Request**: `GET /api/reservations/11` + Header `Authorization: Bearer <vendor_a_token>`
*   **Expected Result**: `403 Forbidden` (Access Denied).
*   **Actual Result**: `403 Forbidden` with body `{"message": "Access denied - You do not own this reservation"}`.
*   **Mitigation**: Custom ownership validation logic matching `r.getVendor().getId()` with the authenticated principal.
*   **Relevant Code**: [`ReservationController.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/controller/ReservationController.java#L133-L148)

---

### TEST 5: Vendor can access their own reservations
*   **Vulnerability/Requirement**: Functional Authorization.
*   **Attack/Test**: Vendor A fetches their own reservation details.
*   **Request**: `GET /api/reservations/10` + Header `Authorization: Bearer <vendor_a_token>`
*   **Expected Result**: `200 OK` + reservation details JSON.
*   **Actual Result**: `200 OK` (Details returned successfully).
*   **Mitigation**: Validates ownership successfully.
*   **Relevant Code**: [`ReservationController.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/controller/ReservationController.java#L143-L159)

---

### TEST 6: Organizer can access all reservations
*   **Vulnerability/Requirement**: Administrative Privileged Access.
*   **Attack/Test**: Organizer fetches details of Vendor A's reservation.
*   **Request**: `GET /api/reservations/10` + Header `Authorization: Bearer <organizer_token>`
*   **Expected Result**: `200 OK`.
*   **Actual Result**: `200 OK` (Details returned successfully).
*   **Mitigation**: `ROLE_EXHIBITION_ORGANIZER` authority is check-exempted from the ownership filter block.
*   **Relevant Code**: [`ReservationController.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/controller/ReservationController.java#L140-L148)

---

### TEST 7: Past reservation date
*   **Vulnerability/Requirement**: Validation / Data Integrity.
*   **Attack/Test**: Submit a booking request with `reservationDate` set in the past.
*   **Request**: `POST /api/reservations/book` with body `{"reservationDate": "2020-01-01", ...}`
*   **Expected Result**: `400 Bad Request`.
*   **Actual Result**: `400 Bad Request`.
*   **Mitigation**: Jakarta `@FutureOrPresent` annotation checks date bounds.
*   **Relevant Code**: [`CreateBookingRequest.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/dto/reservation/CreateBookingRequest.java#L52-L54)

---

### TEST 8: Number of stalls <= 0
*   **Vulnerability/Requirement**: Validation / Data Integrity.
*   **Attack/Test**: Submit booking request with `numberOfStallsRequired = 0`.
*   **Request**: `POST /api/reservations/book` with body `{"numberOfStallsRequired": 0, ...}`
*   **Expected Result**: `400 Bad Request`.
*   **Actual Result**: `400 Bad Request`.
*   **Mitigation**: Jakarta `@Min(value = 1)` annotation.
*   **Relevant Code**: [`CreateBookingRequest.java`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/backend/src/main/java/com/bookfair/Stall_Reservation/dto/reservation/CreateBookingRequest.java#L44-L46)

---

### TEST 9: Stored XSS in comments
*   **Vulnerability/Requirement**: Stored Cross-Site Scripting (XSS) - OWASP A3.
*   **Attack/Test**: Submit a comment containing `<script>alert(1)</script>` in `specialRequirements`.
*   **Request**: `POST /api/reservations/book` with body `{"specialRequirements": "<script>alert(1)</script>", ...}`
*   **Expected Result**: Values stored inside DB, but rendered securely by React without executing.
*   **Actual Result**: Rendered safely in React browser as plain text.
*   **Mitigation**: React automatically escapes text content. We avoided any use of `dangerouslySetInnerHTML`.
*   **Relevant Code**: [`MyReservations.jsx`](file:///c:/Users/khavi/OneDrive/Desktop/IS/bookfair-security-enhancement/frontend/src/pages/MyReservations.jsx) and React default JSX rendering engines.

---

## Submission Checklist for Screenshots/Evidence

Ensure you capture screenshots of the following scenarios for your blog/final evaluation:
- [ ] **SSO Redirect Flow**: Screenshot of the "Sign In with OIDC" landing button.
- [ ] **Decoupled OIDC Login**: Redirect redirecting the browser to your OIDC cloud login screen.
- [ ] **Callback Landing**: Loading spinner as authorization code PKCE validation happens.
- [ ] **Vendor Profile UI**: The profile details showing the read-only Email claim and editable organization fields.
- [ ] **IDOR Prevention Proof**: Screenshot of Postman/Console receiving `403 Forbidden` when attempting to query details of another user's reservation ID.
- [ ] **Validation Rejection**: Booking error on date picker for selecting past events.
