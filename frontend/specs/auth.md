# Auth API Specification

## Overview

Auth feature handles registration, login, token refresh, logout, and current-user retrieval in `com.meet.server.feature.auth`.

## Authentication and common conventions

- Public routes (no authentication required): `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/auth/logout`, OAuth2 routes `/oauth2/**` and `/login/**` (`SecurityConfig`).
- Other routes require authentication (`anyRequest().authenticated()`), including `GET /api/auth/me`.
- Response envelope for successful controller responses is `ApiResponse<T>` with fields:
  - `success` (`boolean`)
  - `message` (`String`)
  - `data` (`Optional<T>`)
- `register`, `login`, `refresh`, and `me` return `data = Optional.of(...)`; `logout` returns `data = Optional.empty()`.
- Refresh token cookie:
  - Name: `refresh_token`
  - `HttpOnly: true`, `SameSite=Lax`, `Path=/`
  - `Secure`: `true` unless `app.env=dev`
  - Max-Age on set: `AppConfig.REFRESH_TOKEN_EXPIRY_SECONDS` (7 days)
  - Max-Age on clear: `0`
- CORS is enabled globally; allowed origins come from `app.cors.allowed-origins`; credentials allowed.

## HTTP endpoints

### POST /api/auth/register

- Purpose: Register a new local (email/password) user and issue tokens.
- Authentication/authorization: Public (`permitAll`).

Request parameters

| Name       | Location | Type     | Required | Constraints                             | Description               |
| ---------- | -------- | -------- | -------- | --------------------------------------- | ------------------------- |
| `fullName` | body     | `string` | Yes      | `@NotBlank`, `@Size(max=100)`           | User display name.        |
| `username` | body     | `string` | Yes      | `@NotBlank`, `@Size(max=50)`            | Unique username.          |
| `email`    | body     | `string` | Yes      | `@NotBlank`, `@Email`, `@Size(max=255)` | Unique email address.     |
| `password` | body     | `string` | Yes      | `@NotBlank`, `@Size(min=8,max=100)`     | Plain password to encode. |

Request example

```json
{
  "fullName": "Jane Doe",
  "username": "jane_doe",
  "email": "jane@example.com",
  "password": "Str0ngPass!"
}
```

Responses

| Status   | Body schema                 | Example                                                                                                                                                                                                            |
| -------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `200 OK` | `ApiResponse<AuthResponse>` | `{"success":true,"message":"Registration successful","data":{"accessToken":"<jwt>","user":{"id":"<uuid>","fullName":"Jane Doe","username":"jane_doe","email":"jane@example.com","avatarUrl":null,"role":"USER"}}}` |

Errors

- `400 Bad Request`: validation failure on request body (`@Valid`). Response body shape is `Not specified in source`.
- `409 Conflict` intent for duplicate email/username via `AuthException` (`EMAIL_ALREADY_EXISTS`, `USERNAME_ALREADY_EXISTS`) — `Inferred from AuthService` (no global handler found in source that guarantees this HTTP mapping).

### POST /api/auth/login

- Purpose: Authenticate local credentials and issue tokens.
- Authentication/authorization: Public (`permitAll`).

Request parameters

| Name       | Location | Type     | Required | Constraints           | Description     |
| ---------- | -------- | -------- | -------- | --------------------- | --------------- |
| `email`    | body     | `string` | Yes      | `@NotBlank`, `@Email` | Account email.  |
| `password` | body     | `string` | Yes      | `@NotBlank`           | Plain password. |

Request example

```json
{
  "email": "jane@example.com",
  "password": "Str0ngPass!"
}
```

Responses

| Status   | Body schema                 | Example                                                                                                                                                                                                     |
| -------- | --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK` | `ApiResponse<AuthResponse>` | `{"success":true,"message":"Login successful","data":{"accessToken":"<jwt>","user":{"id":"<uuid>","fullName":"Jane Doe","username":"jane_doe","email":"jane@example.com","avatarUrl":null,"role":"USER"}}}` |

Errors

- `400 Bad Request`: validation failure on request body (`@Valid`). Response body shape is `Not specified in source`.
- `401 Unauthorized` intent for invalid credentials via `AuthException` (`INVALID_CREDENTIALS`) — `Inferred from AuthService` (mapping handler not specified in source).

### POST /api/auth/refresh

- Purpose: Rotate refresh token and issue a new access token (and new refresh token cookie).
- Authentication/authorization: Public (`permitAll`), but requires `refresh_token` cookie.

Request parameters

| Name            | Location | Type     | Required | Constraints                   | Description           |
| --------------- | -------- | -------- | -------- | ----------------------------- | --------------------- |
| `refresh_token` | cookie   | `string` | Yes      | `@CookieValue(required=true)` | Opaque refresh token. |

Request body

- None.

Responses

| Status   | Body schema                 | Example                                                                                                                                                                                                    |
| -------- | --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK` | `ApiResponse<AuthResponse>` | `{"success":true,"message":"Token refreshed","data":{"accessToken":"<jwt>","user":{"id":"<uuid>","fullName":"Jane Doe","username":"jane_doe","email":"jane@example.com","avatarUrl":null,"role":"USER"}}}` |

Errors

- `400 Bad Request`: missing required `refresh_token` cookie parameter. Body shape `Not specified in source`.
- `401 Unauthorized`: invalid/revoked/expired/reused token via `InvalidTokenException` (`@ResponseStatus(HttpStatus.UNAUTHORIZED)`). Message examples include `Invalid refresh token`, `Refresh token expired`, `Refresh token is revoked`, `Refresh token reuse detected. All sessions invalidated.`

### POST /api/auth/logout

- Purpose: Revoke user refresh tokens (if token/user context present) and clear refresh token cookie.
- Authentication/authorization: Public (`permitAll`).

Request parameters

| Name             | Location         | Type             | Required | Constraints                                                 | Description                              |
| ---------------- | ---------------- | ---------------- | -------- | ----------------------------------------------------------- | ---------------------------------------- |
| `refresh_token`  | cookie           | `string`         | No       | `@CookieValue(required=false)`                              | If present, logout by token owner.       |
| `authentication` | security context | `Authentication` | No       | `authentication.getName()` expected to be UUID when present | Used when no refresh cookie is provided. |

Request body

- None.

Responses

| Status   | Body schema                                        | Example                                                                                                     |
| -------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `200 OK` | `ApiResponse<Void>` with `data = Optional.empty()` | `Inferred from AuthController; concrete JSON representation of Optional.empty() is not specified in source` |

Errors

- If `authentication.getName()` is not a UUID, parsing behavior/status is `Not specified in source`.
- If token/user lookup fails during revoke calls, mapped status is `Not specified in source` (domain exceptions are thrown in services; no explicit handler found).

### GET /api/auth/me

- Purpose: Return currently authenticated user profile.
- Authentication/authorization: Requires authenticated request (`anyRequest().authenticated()`).

Request parameters

| Name             | Location         | Type             | Required | Constraints                               | Description                       |
| ---------------- | ---------------- | ---------------- | -------- | ----------------------------------------- | --------------------------------- |
| `authentication` | security context | `Authentication` | Yes      | `authentication.getName()` parsed as UUID | Current user principal ID source. |

Request body

- None.

Responses

| Status             | Body schema                 | Example                                                                                                                                                                            |
| ------------------ | --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `200 OK`           | `ApiResponse<UserResponse>` | `{"success":true,"message":"Current user retrieved","data":{"id":"<uuid>","fullName":"Jane Doe","username":"jane_doe","email":"jane@example.com","avatarUrl":null,"role":"USER"}}` |
| `401 Unauthorized` | `ApiResponse<Void>`         | `{"success":false,"message":"Unauthorized","data":null}` (from `UnauthorizedResponseHandler` when unauthenticated access reaches entry point).                                     |

Errors

- `404 Not Found` intent when user ID does not exist (`USER_NOT_FOUND`) — `Inferred from UserService` (explicit HTTP mapping for `AuthException` not specified in source).
- UUID parsing failures from principal name: `Not specified in source`.

### GET /oauth2/authorization/{registrationId}

- Purpose: Start OAuth2 login using configured provider.
- Authentication/authorization: Public (`permitAll` via `/oauth2/**`).
- Supported `registrationId` values from configuration: `google`, `github`.
- Behavior: Redirects user agent to provider consent/login page (handled by Spring Security OAuth2 client).

Request parameters

| Name             | Location | Type     | Required | Constraints                                      | Description                                |
| ---------------- | -------- | -------- | -------- | ------------------------------------------------ | ------------------------------------------ |
| `registrationId` | path     | `string` | Yes      | must match configured OAuth2 client registration | OAuth provider key (`google` or `github`). |

Request body

- None.

Responses

| Status      | Body schema | Example                                                                                 |
| ----------- | ----------- | --------------------------------------------------------------------------------------- |
| `302 Found` | Redirect    | `Location: https://accounts.google.com/...` (provider URL; varies by provider/session). |

Errors

- Unsupported/unconfigured `registrationId`: behavior/status `Not specified in source` (framework-handled).

### GET /login/oauth2/code/{registrationId}

- Purpose: OAuth2 callback endpoint processed by Spring Security after provider authentication.
- Authentication/authorization: Public (`permitAll` via `/login/**`).
- Behavior:
  - On success:
    - Resolves provider profile attributes (`email`, `name` or `login`, `picture` or `avatar_url`).
    - Calls `AuthService.loginWithOAuth2(provider, email, fullName, avatar)`.
    - Sets `refresh_token` cookie (`HttpOnly`, `SameSite=Lax`, env-dependent `Secure`, `Path=/`, 7-day max-age).
    - Redirects to `app.oauth2.success-redirect-uri` with query parameter `access_token=<jwt>`.
  - On failure:
    - Redirects to `app.oauth2.success-redirect-uri` with query parameter `error=oauth2_login_failed`.

Request parameters

| Name                                  | Location | Type     | Required           | Constraints                                  | Description                          |
| ------------------------------------- | -------- | -------- | ------------------ | -------------------------------------------- | ------------------------------------ |
| `registrationId`                      | path     | `string` | Yes                | provider-specific OAuth2 client registration | Provider key (`google` or `github`). |
| OAuth2 params (`code`, `state`, etc.) | query    | `string` | Provider-dependent | managed by Spring Security OAuth2 flow       | Authorization callback parameters.   |

Request body

- None.

Responses

| Status      | Body schema                       | Example                                                                                                                                |
| ----------- | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `302 Found` | Redirect + `Set-Cookie` (success) | `Location: http://localhost:3000/oauth2/callback?access_token=<jwt>` + `Set-Cookie: refresh_token=...; HttpOnly; Path=/; SameSite=Lax` |
| `302 Found` | Redirect (failure)                | `Location: http://localhost:3000/oauth2/callback?error=oauth2_login_failed`                                                            |

## WebSocket/message contracts

No auth feature-local WebSocket/STOMP handlers were found in source (`@MessageMapping`, `@SendTo`, socket listeners not present).

## Shared schemas

### RegisterRequest

| Field      | Type     | Required | Nullable                 | Validation                              | Meaning           |
| ---------- | -------- | -------- | ------------------------ | --------------------------------------- | ----------------- |
| `fullName` | `string` | Yes      | No (in request contract) | `@NotBlank`, `@Size(max=100)`           | Display name.     |
| `username` | `string` | Yes      | No (in request contract) | `@NotBlank`, `@Size(max=50)`            | Unique username.  |
| `email`    | `string` | Yes      | No (in request contract) | `@NotBlank`, `@Email`, `@Size(max=255)` | Email identifier. |
| `password` | `string` | Yes      | No (in request contract) | `@NotBlank`, `@Size(min=8,max=100)`     | Plain password.   |

### LoginRequest

| Field      | Type     | Required | Nullable                 | Validation            | Meaning         |
| ---------- | -------- | -------- | ------------------------ | --------------------- | --------------- |
| `email`    | `string` | Yes      | No (in request contract) | `@NotBlank`, `@Email` | Account email.  |
| `password` | `string` | Yes      | No (in request contract) | `@NotBlank`           | Plain password. |

### AuthResponse

| Field         | Type           | Required | Nullable                | Validation | Meaning                                     |
| ------------- | -------------- | -------- | ----------------------- | ---------- | ------------------------------------------- |
| `accessToken` | `string`       | Yes      | Not specified in source | None       | JWT access token generated by `JwtService`. |
| `user`        | `UserResponse` | Yes      | Not specified in source | None       | Public user payload.                        |

### UserResponse

| Field       | Type       | Required | Nullable                                              | Validation                   | Meaning                   |
| ----------- | ---------- | -------- | ----------------------------------------------------- | ---------------------------- | ------------------------- |
| `id`        | `uuid`     | Yes      | Not specified in source                               | None                         | Server-generated user ID. |
| `fullName`  | `string`   | Yes      | Not specified in source                               | None                         | User display name.        |
| `username`  | `string`   | Yes      | Not specified in source                               | None                         | Unique username.          |
| `email`     | `string`   | Yes      | Not specified in source                               | None                         | User email.               |
| `avatarUrl` | `string`   | Yes      | May be nullable (inferred from entity/service writes) | None                         | Profile image URL.        |
| `role`      | `UserRole` | Yes      | Not specified in source                               | Enum values: `USER`, `ADMIN` | Authorization role.       |

### ApiResponse<T> envelope

| Field     | Type          | Required | Nullable                                                                | Meaning                        |
| --------- | ------------- | -------- | ----------------------------------------------------------------------- | ------------------------------ |
| `success` | `boolean`     | Yes      | No                                                                      | Indicates operation result.    |
| `message` | `string`      | Yes      | Not specified in source                                                 | Human-readable status message. |
| `data`    | `Optional<T>` | Yes      | In practice can be present, empty, or `null` (see unauthorized handler) | Payload wrapper.               |

### Unauthorized payload example

- Produced by security entry point:

```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```

### Refresh token cookie contract

| Field           | Type          | Required                                                  | Nullable | Constraints                                                  | Meaning                |
| --------------- | ------------- | --------------------------------------------------------- | -------- | ------------------------------------------------------------ | ---------------------- |
| `refresh_token` | opaque string | Required for `POST /refresh`; optional for `POST /logout` | N/A      | `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure` env-dependent | Session refresh token. |

### OAuth2 redirect query contract

| Field          | Type     | Required          | Nullable | Constraints                                                 | Meaning                                         |
| -------------- | -------- | ----------------- | -------- | ----------------------------------------------------------- | ----------------------------------------------- |
| `access_token` | `string` | On OAuth2 success | N/A      | JWT produced by backend                                     | Access token returned to frontend redirect URI. |
| `error`        | `string` | On OAuth2 failure | N/A      | fixed value `oauth2_login_failed` in current implementation | OAuth2 login failure signal.                    |

## Source references

- `src/main/java/com/meet/server/feature/auth/AuthController.java` (`register`, `login`, `refresh`, `logout`, `currentUser`)
- `src/main/java/com/meet/server/feature/auth/AuthService.java` (registration/login/refresh/logout behavior and domain error intent)
- `src/main/java/com/meet/server/feature/auth/RefreshTokenService.java` (token validation/rotation/revocation)
- `src/main/java/com/meet/server/feature/auth/dto/RegisterRequest.java`
- `src/main/java/com/meet/server/feature/auth/dto/LoginRequest.java`
- `src/main/java/com/meet/server/feature/auth/dto/AuthResponse.java`
- `src/main/java/com/meet/server/feature/auth/dto/UserResponse.java`
- `src/main/java/com/meet/server/feature/auth/mapper/AuthMapper.java`
- `src/main/java/com/meet/server/common/api/ApiResponse.java`
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java`
- `src/main/java/com/meet/server/common/security/oauth2/OAuth2UserService.java`
- `src/main/java/com/meet/server/common/security/oauth2/OAuth2AuthenticationSuccessHandler.java`
- `src/main/java/com/meet/server/common/security/oauth2/OAuth2AuthenticationFailureHandler.java`
- `src/main/java/com/meet/server/common/security/handler/UnauthorizedResponseHandler.java`
- `src/main/java/com/meet/server/common/util/CookieUtil.java`
- `src/main/java/com/meet/server/common/config/AppConfig.java`
- `src/main/java/com/meet/server/common/exception/InvalidTokenException.java`
- `src/main/java/com/meet/server/common/exception/AuthException.java`
- `src/main/java/com/meet/server/feature/user/UserService.java`
- `src/main/java/com/meet/server/feature/user/UserRole.java`
- `src/main/resources/application.yaml`
