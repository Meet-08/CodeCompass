# Auth API Specification

## Overview

The auth feature provides local registration and login, refresh-token rotation, logout, current-user retrieval, and
Google/GitHub OAuth2 login. Its application code is under `com.meet.server.feature.auth`; shared authentication,
security, cookie, and exception behavior is under `com.meet.server.common`.

## Authentication and common conventions

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout` are public.
- `GET /api/auth/me` requires an authenticated request. Other unmatched routes also require authentication.
- `/oauth2/**` and `/login/**` are public Spring Security OAuth2 routes.
- JWT authentication is installed by `JwtFilter`; the exact request header parsing is implemented there and is not
  restated as a controller contract here.
- Successful controller responses use `ApiResponse<T>`:

  | Field | Type | Meaning |
  |---|---|---|
  | `success` | `boolean` | Whether the operation succeeded. |
  | `message` | `string` | Human-readable result message. |
  | `data` | `Optional<T>` | Response payload; logout uses an empty value. |

- Validation and application errors are also returned through `ApiResponse` by `GlobalExceptionHandler`.
- The refresh token is stored in an HTTP-only `refresh_token` cookie with `SameSite=Lax`, `Path=/`, and a seven-day
  max age. `Secure` is enabled unless `app.env=dev`.
- CORS allows configured origins, credentials, all headers, and `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`.
- OAuth2 provider registrations configured in source are `google` and `github`.

## HTTP endpoints

### POST /api/auth/register

- Purpose: Create a local user and issue an access token plus refresh-token cookie.
- Authentication/authorization: Public (`permitAll`).
- Request body: `RegisterRequest`.

| Field | Type | Required | Constraints |
|---|---|---:|---|
| `fullName` | `string` | Yes | `@NotBlank`, maximum 100 characters |
| `username` | `string` | Yes | `@NotBlank`, maximum 50 characters |
| `email` | `string` | Yes | `@NotBlank`, `@Email`, maximum 255 characters |
| `password` | `string` | Yes | `@NotBlank`, 8–100 characters |

Example:

```json
{"fullName":"Jane Doe","username":"jane_doe","email":"jane@example.com","password":"Str0ngPass!"}
```

Responses:

| Status | Body |
|---|---|
| `200 OK` | `ApiResponse<AuthResponse>` with message `Registration successful`; also sets `refresh_token`. |
| `400 Bad Request` | Validation error envelope containing field/object messages. |
| `409 Conflict` | `ApiResponse<Void>` with the duplicate-email or duplicate-username message. |
| `500 Internal Server Error` | Generic `ApiResponse<Void>` with message `An unexpected error occurred`. |

Domain conflict codes are `EMAIL_ALREADY_EXISTS` and `USERNAME_ALREADY_EXISTS`.

### POST /api/auth/login

- Purpose: Authenticate local credentials and issue an access token plus refresh-token cookie.
- Authentication/authorization: Public (`permitAll`).
- Request body: `LoginRequest`.

| Field | Type | Required | Constraints |
|---|---|---:|---|
| `email` | `string` | Yes | `@NotBlank`, `@Email` |
| `password` | `string` | Yes | `@NotBlank` |

Example:

```json
{"email":"jane@example.com","password":"Str0ngPass!"}
```

Responses:

| Status | Body |
|---|---|
| `200 OK` | `ApiResponse<AuthResponse>` with message `Login successful`; also sets `refresh_token`. |
| `400 Bad Request` | Validation error envelope containing field/object messages. |
| `401 Unauthorized` | `ApiResponse<Void>` with `Invalid email or password`. |
| `500 Internal Server Error` | Generic `ApiResponse<Void>`. |

### POST /api/auth/refresh

- Purpose: Validate and rotate the refresh token, then issue a new access token and refresh-token cookie.
- Authentication/authorization: Public (`permitAll`).
- Request cookie:

  | Name | Type | Required | Constraints |
  |---|---|---:|---|
  | `refresh_token` | opaque `string` | Yes | `@CookieValue(required=true)` |

- Request body: None.

Responses:

| Status | Body |
|---|---|
| `200 OK` | `ApiResponse<AuthResponse>` with message `Token refreshed`; sets a rotated `refresh_token`. |
| `400 Bad Request` | `ApiResponse<Void>` with message `Malformed or incomplete request` when the required cookie is absent. |
| `401 Unauthorized` | `ApiResponse<Void>` containing an invalid, expired, revoked, required, or reused-token message. |
| `500 Internal Server Error` | Generic `ApiResponse<Void>`. |

Token messages defined by `RefreshTokenService` include `Refresh token is required`, `Invalid refresh token`,
`Refresh token expired`, `Refresh token is revoked`, and `Refresh token reuse detected. All sessions invalidated.`

### POST /api/auth/logout

- Purpose: Revoke refresh tokens where possible and clear the refresh-token cookie.
- Authentication/authorization: Public (`permitAll`).
- Request parameters:

  | Name | Location | Type | Required | Meaning |
  |---|---|---|---:|---|
  | `refresh_token` | Cookie | `string` | No | If present, revoke the token owner's sessions. |
  | `authentication` | Security context | `Authentication` | No | Used when no refresh cookie exists; its name is parsed as a UUID. |

- Request body: None.

Responses:

| Status | Body |
|---|---|
| `200 OK` | `ApiResponse<Void>` with message `Logout successful`, empty `data`, and a cleared `refresh_token` cookie. |
| `400 Bad Request` | Malformed/incomplete request envelope where applicable. |
| `401 Unauthorized` | Invalid refresh-token failure when revocation is attempted. |
| `500 Internal Server Error` | Generic `ApiResponse<Void>` for unhandled failures, including UUID parsing failures. |

### GET /api/auth/me

- Purpose: Return the currently authenticated user's public profile.
- Authentication/authorization: Authenticated request required (`anyRequest().authenticated()`).
- Request body: None.
- Authentication context: `authentication.getName()` is parsed as the user UUID.

Responses:

| Status | Body |
|---|---|
| `200 OK` | `ApiResponse<UserResponse>` with message `Current user retrieved`. |
| `401 Unauthorized` | `ApiResponse<Void>` with `Unauthorized`, written by `UnauthorizedResponseHandler`. |
| `404 Not Found` | `ApiResponse<Void>` with `User not found`. |
| `500 Internal Server Error` | Generic `ApiResponse<Void>` for unhandled failures. |

### GET /oauth2/authorization/{registrationId}

- Purpose: Start the Spring Security OAuth2 authorization flow.
- Authentication/authorization: Public through `/oauth2/**`.
- Path parameter:

  | Name | Type | Required | Values |
  |---|---|---:|---|
  | `registrationId` | `string` | Yes | Configured registrations: `google`, `github`. |

- Request body: None.
- Response: Framework-generated redirect to the selected provider. Exact status and provider URL are not specified in
  application source.

### GET /login/oauth2/code/{registrationId}

- Purpose: Spring Security OAuth2 callback.
- Authentication/authorization: Public through `/login/**`.
- Path parameter: `registrationId` (`string`, required, provider registration key).
- Query parameters: Provider-managed OAuth2 callback values such as `code` and `state`; exact set is not specified in
  application source.
- Request body: None.

Success behavior:

1. Reads provider attributes `email`, `name`/`login`, and `picture`/`avatar_url`.
2. Calls `AuthService.loginWithOAuth2`.
3. Sets the HTTP-only `refresh_token` cookie.
4. Redirects to `app.oauth2.success-redirect-uri` with `access_token=<jwt>`.

Failure behavior: Redirects to the same configured URI with `error=oauth2_login_failed`.

The exact framework redirect status is not specified in application source.

## WebSocket/message contracts

No auth-feature WebSocket/STOMP handlers or listeners were found in source.

## Shared schemas

### AuthResponse

| Field | Type | Required | Nullable | Meaning |
|---|---|---:|---:|---|
| `accessToken` | `string` | Yes | Not specified | Generated JWT access token. |
| `user` | `UserResponse` | Yes | Not specified | Public user profile. |

### UserResponse

| Field | Type | Required | Nullable | Meaning |
|---|---|---:|---:|---|
| `id` | `UUID` | Yes | Not specified | User identifier. |
| `fullName` | `string` | Yes | Not specified | Display name. |
| `username` | `string` | Yes | Not specified | Username. |
| `email` | `string` | Yes | Not specified | Email address. |
| `avatarUrl` | `string` | Yes | Not specified | Profile image URL; provider data may be absent. |
| `role` | `UserRole` | Yes | Not specified | `USER` or `ADMIN`. |

### Error envelopes

Validation errors are returned as `ApiResponse<Map<string,string>>` with `success=false`, message `Validation failed`,
and `data` mapping field/property names to validation messages. `MethodArgumentNotValidException`, `BindException`,
`ConstraintViolationException`, and `HandlerMethodValidationException` are handled.

Custom application errors are returned as `ApiResponse<Void>` with `success=false`, the exception message, and empty
`data`:

| Exception | Status |
|---|---|
| `AuthException` | Status carried by the exception, including `401`, `404`, and `409`. |
| `InvalidTokenException` | `401 Unauthorized`. |
| `ResponseStatusException` | Status carried by the exception. |
| Malformed request or missing required parameter | `400 Bad Request`. |
| Unhandled `Exception` | `500 Internal Server Error`. |

The `AuthException.errorCode` values are application metadata (`EMAIL_ALREADY_EXISTS`, `USERNAME_ALREADY_EXISTS`,
`INVALID_CREDENTIALS`, `OAUTH_EMAIL_MISSING`, and `USER_NOT_FOUND`) but are not serialized by the current handler.

### Refresh-token cookie

| Property | Value |
|---|---|
| Name | `refresh_token` |
| HTTP-only | `true` |
| SameSite | `Lax` |
| Path | `/` |
| Secure | `true` except when `app.env=dev` |
| Max-Age when issued | `604800` seconds (7 days) |
| Max-Age when cleared | `0` |

## Source references

- `src/main/java/com/meet/server/feature/auth/AuthController.java` — REST mappings and response messages.
- `src/main/java/com/meet/server/feature/auth/AuthService.java` — local and OAuth2 auth behavior.
- `src/main/java/com/meet/server/feature/auth/RefreshTokenService.java` — refresh-token lifecycle and errors.
- `src/main/java/com/meet/server/feature/auth/dto/RegisterRequest.java` — registration validation.
- `src/main/java/com/meet/server/feature/auth/dto/LoginRequest.java` — login validation.
- `src/main/java/com/meet/server/feature/auth/dto/AuthResponse.java` and `UserResponse.java` — response schemas.
- `src/main/java/com/meet/server/feature/user/UserService.java` and `UserRole.java` — user errors and role values.
- `src/main/java/com/meet/server/common/api/ApiResponse.java` — response envelope.
- `src/main/java/com/meet/server/common/exception/GlobalExceptionHandler.java` — validation and exception mappings.
- `src/main/java/com/meet/server/common/exception/AuthException.java` and `InvalidTokenException.java` — custom errors.
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java` — access rules and OAuth2 setup.
- `src/main/java/com/meet/server/common/security/filter/JwtFilter.java` — JWT request authentication.
- `src/main/java/com/meet/server/common/security/handler/UnauthorizedResponseHandler.java` — unauthenticated response.
- `src/main/java/com/meet/server/common/security/oauth2/OAuth2AuthenticationSuccessHandler.java` — OAuth2 success flow.
- `src/main/java/com/meet/server/common/security/oauth2/OAuth2AuthenticationFailureHandler.java` — OAuth2 failure flow.
- `src/main/java/com/meet/server/common/util/CookieUtil.java` and `AppConfig.java` — cookie attributes and expiry.
- `src/main/resources/application.yaml` — OAuth2 registrations and redirect configuration.
