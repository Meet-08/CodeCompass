# Codebase API Specification

## Overview

The `feature.codebase` module manages authenticated repository imports, asynchronous indexing, metadata updates, deletion, and reindexing. Chat routes are declared by `feature.chat` and are documented separately in `specs/chat.md`.

## Authentication and common conventions

- All codebase routes require authentication. `SecurityConfig` permits only the explicitly listed auth and OAuth routes; all other requests require an authenticated principal.
- The controller parses `Authentication.getName()` as the authenticated user's UUID.
- JSON responses use `ApiResponse<T>` with `success`, `message`, and `data` fields. Error responses use `success: false`.
- Codebase and user identifiers are UUIDs. `Instant` fields are serialized using the application's Jackson configuration.
- The list endpoint is not paginated and is ordered by `createdAt` descending.
- Import and reindex are asynchronous: the HTTP response reports `QUEUED`; processing later transitions the persisted status to `PROCESSING`, then `INDEXED` or `FAILED`.
- The rate-limit filter may return HTTP `429` with a plain-text body and `X-Rate-Limit-Retry-After-Seconds`. This response is produced before controller handling and is not an `ApiResponse`.

## HTTP endpoints

### POST /api/codebases

- Purpose: Create a codebase record and queue repository cloning/indexing.
- Authentication/authorization: Authentication required. The new codebase is assigned to the authenticated user. A pessimistic user lock is used while enforcing the maximum of five persisted codebases.
- Request headers/path/query parameters:

  | Name | Type | Required | Constraints | Description |
  | --- | --- | --- | --- | --- |
  | `Authorization` | Bearer token | Yes | Must authenticate a user | JWT authentication header. |
  | `Content-Type` | Media type | Yes | JSON request | Request body media type. |

- Request body: `CodebaseImportRequest`.

  | Field | Type | Required | Nullability/constraints | Description |
  | --- | --- | --- | --- | --- |
  | `name` | string | Yes | `@NotBlank` | Display name; import service stores the supplied value. |
  | `cloneUrl` | string | Yes | `@NotBlank`; `@Pattern` requires an HTTPS URL; service additionally requires HTTPS, a host, and no user info | Repository clone URL. |
  | `branch` | string | No | Nullable or blank becomes `main` | Branch passed to the clone operation. |

  ```json
  {
    "name": "Server Repo",
    "cloneUrl": "https://github.com/example/server.git",
    "branch": "main"
  }
  ```

- Responses:

  #### 202 Accepted

  Body: `ApiResponse<CodebaseImportResponse>`.

  ```json
  {
    "success": true,
    "message": "Codebase import queued",
    "data": {
      "codebaseId": "11111111-1111-1111-1111-111111111111",
      "status": "QUEUED",
      "fileCount": 0
    }
  }
  ```

- Errors:
  - `400 Bad Request`: request validation failure or `INVALID_CLONE_URL` (`Clone URL must be a public HTTPS URL`).
  - `404 Not Found`: authenticated user does not exist (`USER_NOT_FOUND`).
  - `409 Conflict`: `CODEBASE_LIMIT_REACHED` when the user already has five codebases; all persisted statuses count.

### GET /api/codebases

- Purpose: List all codebases owned by the authenticated user, including an aggregate repository-file count.
- Authentication/authorization: Authentication required. Results are filtered by the authenticated user ID.
- Request headers/path/query parameters:

  | Name | Type | Required | Constraints | Description |
  | --- | --- | --- | --- | --- |
  | `Authorization` | Bearer token | Yes | Must authenticate a user | JWT authentication header. |

  No path parameters, query parameters, or request body are defined.

- Responses:

  #### 200 OK

  Body: `ApiResponse<List<CodebaseResponse>>`.

  ```json
  {
    "success": true,
    "message": "Codebases retrieved",
    "data": [
      {
        "codebaseId": "11111111-1111-1111-1111-111111111111",
        "name": "Server Repo",
        "cloneUrl": "https://github.com/example/server.git",
        "branch": "main",
        "status": "INDEXED",
        "lastCommitSha": "abc123",
        "indexedAt": "2026-08-07T10:00:00Z",
        "createdAt": "2026-08-07T09:00:00Z",
        "updatedAt": "2026-08-07T10:00:00Z",
        "fileCount": 12
      }
    ]
  }
  ```

- Errors: `404 Not Found` with `USER_NOT_FOUND` if the authenticated user cannot be loaded.

### PATCH /api/codebases/{codebaseId}

- Purpose: Update codebase metadata. This endpoint does not start reindexing and does not change `cloneUrl`.
- Authentication/authorization: Authentication required. The target codebase must belong to the authenticated user.
- Request headers/path/query parameters:

  | Name | Type | Required | Constraints | Description |
  | --- | --- | --- | --- | --- |
  | `Authorization` | Bearer token | Yes | Must authenticate a user | JWT authentication header. |
  | `Content-Type` | Media type | Yes | JSON request | Request body media type. |
  | `codebaseId` | UUID path parameter | Yes | Valid UUID syntax | Target codebase. |

- Request body: `CodebaseUpdateRequest`.

  | Field | Type | Required | Nullability/constraints | Description |
  | --- | --- | --- | --- | --- |
  | `name` | string | Yes | `@NotBlank`; service trims before saving | New display name. |
  | `branch` | string | Yes | `@NotBlank`; service trims before saving | New branch name. |

  ```json
  {
    "name": "Updated Server Repo",
    "branch": "develop"
  }
  ```

- Responses:

  #### 200 OK

  Body: `ApiResponse<CodebaseResponse>` with message `Codebase updated`. `fileCount` is read after the update.

  ```json
  {
    "success": true,
    "message": "Codebase updated",
    "data": {
      "codebaseId": "11111111-1111-1111-1111-111111111111",
      "name": "Updated Server Repo",
      "cloneUrl": "https://github.com/example/server.git",
      "branch": "develop",
      "status": "INDEXED",
      "lastCommitSha": "abc123",
      "indexedAt": "2026-08-07T10:00:00Z",
      "createdAt": "2026-08-07T09:00:00Z",
      "updatedAt": "2026-08-07T10:05:00Z",
      "fileCount": 12
    }
  }
  ```

- Errors:
  - `400 Bad Request`: blank or missing `name`/`branch`.
  - `403 Forbidden`: `CODEBASE_FORBIDDEN` when the user does not own the codebase.
  - `404 Not Found`: `CODEBASE_NOT_FOUND`.

### DELETE /api/codebases/{codebaseId}

- Purpose: Delete an owned codebase and its indexed chunks and repository-file rows.
- Authentication/authorization: Authentication required. The target codebase must belong to the authenticated user.
- Request headers/path/query parameters:

  | Name | Type | Required | Constraints | Description |
  | --- | --- | --- | --- | --- |
  | `Authorization` | Bearer token | Yes | Must authenticate a user | JWT authentication header. |
  | `codebaseId` | UUID path parameter | Yes | Valid UUID syntax | Target codebase. |

- Request body: None.
- Responses:

  #### 204 No Content

  Empty body. The service deletes code chunks and repository files before deleting the codebase row.

- Errors:
  - `403 Forbidden`: `CODEBASE_FORBIDDEN`.
  - `404 Not Found`: `CODEBASE_NOT_FOUND`.
  - `409 Conflict`: `CODEBASE_BUSY` when status is `QUEUED` or `PROCESSING`.

### POST /api/codebases/{codebaseId}/reindex

- Purpose: Clear existing indexed data and queue a fresh clone/index operation.
- Authentication/authorization: Authentication required. The target codebase must belong to the authenticated user.
- Request headers/path/query parameters:

  | Name | Type | Required | Constraints | Description |
  | --- | --- | --- | --- | --- |
  | `Authorization` | Bearer token | Yes | Must authenticate a user | JWT authentication header. |
  | `codebaseId` | UUID path parameter | Yes | Valid UUID syntax | Target codebase. |

- Request body: None.
- Responses:

  #### 202 Accepted

  Body: `ApiResponse<CodebaseImportResponse>` with message `Codebase reindex queued`. Existing `lastCommitSha` and `indexedAt` are cleared, status becomes `QUEUED`, and `fileCount` is `0` before the asynchronous job begins.

  ```json
  {
    "success": true,
    "message": "Codebase reindex queued",
    "data": {
      "codebaseId": "11111111-1111-1111-1111-111111111111",
      "status": "QUEUED",
      "fileCount": 0
    }
  }
  ```

- Errors:
  - `403 Forbidden`: `CODEBASE_FORBIDDEN`.
  - `404 Not Found`: `CODEBASE_NOT_FOUND`.
  - `409 Conflict`: `CODEBASE_BUSY` when status is `QUEUED` or `PROCESSING`.

## WebSocket/message contracts

The `feature.codebase` module defines no WebSocket, STOMP, or message-destination handlers. Chat SSE is owned by `feature.chat`.

## Shared schemas

### ApiResponse<T>

| Field | Type | Required | Nullability/constraints | Description |
| --- | --- | --- | --- | --- |
| `success` | boolean | Yes | Non-null record component | Whether the operation succeeded. |
| `message` | string | Yes | No validation constraint in source | Human-readable result or error message. |
| `data` | T or empty/null | Source envelope field | Java type is `Optional<T>` for controller responses; exact JSON rendering is configuration-dependent | Operation payload. |

### CodebaseImportRequest

| Field | Type | Required | Nullability/constraints | Description |
| --- | --- | --- | --- | --- |
| `name` | string | Yes | `@NotBlank` | Display name. |
| `cloneUrl` | string | Yes | `@NotBlank`; HTTPS regex; service validates scheme, host, and user info | Repository URL. |
| `branch` | string | No | Nullable; blank is normalized to `main` | Clone branch. |

### CodebaseUpdateRequest

| Field | Type | Required | Nullability/constraints | Description |
| --- | --- | --- | --- | --- |
| `name` | string | Yes | `@NotBlank`; trimmed by service | Updated display name. |
| `branch` | string | Yes | `@NotBlank`; trimmed by service | Updated branch. |

### CodebaseImportResponse

| Field | Type | Required | Nullability/constraints | Description |
| --- | --- | --- | --- | --- |
| `codebaseId` | UUID | Yes | Server-generated | Codebase identifier. |
| `status` | `CodebaseStatus` | Yes | Enum value | Import/indexing status at response creation. |
| `fileCount` | integer | Yes | Primitive `int` | `0` when queued; processed file count when asynchronous processing completes internally. |

### CodebaseResponse

| Field | Type | Required | Nullability/constraints | Description |
| --- | --- | --- | --- | --- |
| `codebaseId` | UUID | Yes | Server-generated | Codebase identifier. |
| `name` | string | Yes | Persisted non-null | Display name. |
| `cloneUrl` | string | No | Nullable in entity | Repository clone URL. |
| `branch` | string | No | Entity default is `main`; source does not declare database non-null here | Configured branch. |
| `status` | `CodebaseStatus` | Yes | Enum value; entity default is `QUEUED` | Current lifecycle status. |
| `lastCommitSha` | string | No | Nullable | Indexed repository HEAD commit. |
| `indexedAt` | Instant | No | Nullable until status becomes `INDEXED` | Time status became indexed. |
| `createdAt` | Instant | Yes | Auditing field persisted non-null | Creation timestamp. |
| `updatedAt` | Instant | Yes | Auditing field persisted non-null | Last update timestamp. |
| `fileCount` | integer | Yes | Aggregate count of associated repository-file rows | Number of repository files. |

### CodebaseStatus

Enum values: `INDEXED`, `QUEUED`, `PROCESSING`, `FAILED`.

### Validation and domain errors

For request-body validation, `GlobalExceptionHandler` returns HTTP `400` with `ApiResponse<Map<String,String>>`:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": { "cloneUrl": "cloneUrl must use HTTPS" }
}
```

`CodebaseException` responses use the exception's configured status and message and do not expose `errorCode` in the body:

```json
{
  "success": false,
  "message": "Codebase not found",
  "data": null
}
```

Known codebase domain codes include `CODEBASE_LIMIT_REACHED` (`409`), `INVALID_CLONE_URL` (`400`), `CODEBASE_NOT_FOUND` (`404`), `CODEBASE_FORBIDDEN` (`403`), `CODEBASE_BUSY` (`409`), clone failure `CODEBASE_CLONE_FAILED` (`502`), and processing/cleanup/file errors configured as `500`. Clone and processing failures occur in the asynchronous worker after the initial `202`; the persisted status is set to `FAILED`.

Unauthenticated requests receive HTTP `401` with an unauthorized `ApiResponse<Void>` response. Malformed JSON is mapped to HTTP `400` with message `Malformed or incomplete request`; unexpected synchronous exceptions are mapped to HTTP `500` with message `An unexpected error occurred`.

## Source references

- `src/main/java/com/meet/server/feature/codebase/CodebaseController.java`: all five codebase route declarations and HTTP response statuses/messages.
- `src/main/java/com/meet/server/feature/codebase/CodebaseService.java`: ownership, five-codebase limit, URL validation, metadata update, delete/reindex busy checks, child cleanup, transaction-after-commit scheduling, and lifecycle handling.
- `src/main/java/com/meet/server/feature/codebase/CodebaseStatusService.java`: `PROCESSING`/`INDEXED` status persistence and `indexedAt` assignment.
- `src/main/java/com/meet/server/feature/codebase/GitService.java`: clone, commit resolution, file listing, and worker error codes/statuses.
- `src/main/java/com/meet/server/feature/codebase/Codebase.java`: persisted codebase fields and defaults.
- `src/main/java/com/meet/server/feature/codebase/CodebaseStatus.java`: status enum values.
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportRequest.java`: import request validation.
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseUpdateRequest.java`: update request validation.
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportResponse.java`: queued/indexing response fields.
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseResponse.java`: list/update response fields.
- `src/main/java/com/meet/server/feature/codebase/mapper/CodebaseMapper.java`: update response conversion.
- `src/main/java/com/meet/server/feature/codebase/CodebaseRepository.java`: aggregate list query and count-by-owner query.
- `src/main/java/com/meet/server/feature/repositoryfile/RepositoryFileRepository.java`: file counting and deletion.
- `src/main/java/com/meet/server/feature/codechunk/CodeChunkRepository.java`: indexed chunk deletion.
- `src/main/java/com/meet/server/common/api/ApiResponse.java`: common response envelope.
- `src/main/java/com/meet/server/common/exception/GlobalExceptionHandler.java`: validation, domain, malformed-request, and unexpected-error mappings.
- `src/main/java/com/meet/server/common/exception/CodebaseException.java`: domain error code/status model.
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java`: authentication requirement.
- `src/main/java/com/meet/server/common/security/handler/UnauthorizedResponseHandler.java`: unauthenticated response.
- `src/main/java/com/meet/server/common/ratelimit/filter/RateLimiterFilter.java`: rate-limit response and headers.
