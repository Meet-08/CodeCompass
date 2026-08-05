# Codebase API Specification

## Overview

The codebase feature manages authenticated repository imports, indexing, metadata updates, deletion, reindexing, and
code-aware chat over indexed repositories. The primary source modules are
`com.meet.server.feature.codebase` and `com.meet.server.feature.chat`.

## Authentication and common conventions

- All endpoints require authentication because `SecurityConfig` applies `.anyRequest().authenticated()`.
- The authenticated user ID is read from `Authentication.getName()` and parsed as a UUID.
- JSON endpoints use `ApiResponse<T>` with `success`, `message`, and `data` fields.
- Validation and domain failures are mapped by `GlobalExceptionHandler` to the same response envelope.
- Codebase IDs are UUIDs.
- The list endpoint is not paginated in the current source.
- Chat uses `text/event-stream` SSE and does not use `ApiResponse` for the stream itself.

## HTTP endpoints

### POST /api/codebases

- Purpose: Queue a repository import and indexing job.
- Authentication/authorization: Authenticated user required. The new codebase belongs to that user.
- Request headers/path/query parameters:

| Name            | Type   | Required | Constraints                                           | Description                                   |
|-----------------|--------|---------:|-------------------------------------------------------|-----------------------------------------------|
| `Authorization` | string |      Yes | Authentication is enforced by security configuration. | Authenticated principal supplies the user ID. |

- Request body: `CodebaseImportRequest`

| Field      | Type   | Required | Nullability | Constraints                                                                   | Description           |
|------------|--------|---------:|-------------|-------------------------------------------------------------------------------|-----------------------|
| `name`     | string |      Yes | Non-null    | `@NotBlank`                                                                   | Display name.         |
| `cloneUrl` | string |      Yes | Non-null    | `@NotBlank`; HTTPS pattern; service requires HTTPS, a host, and no user info. | Repository clone URL. |
| `branch`   | string |       No | Nullable    | Blank or null becomes `main`.                                                 | Branch to clone.      |

Example:

```json
{
  "name": "Server Repo",
  "cloneUrl": "https://github.com/example/server.git",
  "branch": "main"
}
```

- Responses:

| Status            | Body                                   | Example                                                                                                                                            |
|-------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `202 Accepted`    | `ApiResponse<CodebaseImportResponse>`  | `{"success":true,"message":"Codebase import queued","data":{"codebaseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","status":"QUEUED","fileCount":0}}` |
| `400 Bad Request` | Validation or clone URL error envelope | `{"success":false,"message":"Validation failed","data":{"cloneUrl":"cloneUrl must use HTTPS"}}`                                                    |
| `409 Conflict`    | `ApiResponse<Void>`                    | `{"success":false,"message":"A user can have at most 5 codebases","data":null}`                                                                    |

- Errors:
    - `CODEBASE_LIMIT_REACHED` (`409`) when the user already has five persisted codebases. All statuses count.
    - `INVALID_CLONE_URL` (`400`) when service-level URI validation fails.
    - Validation failures (`400`) for invalid request fields.

### GET /api/codebases

- Purpose: Return all codebases owned by the authenticated user.
- Authentication/authorization: Authenticated user required; results are filtered by the authenticated user ID.
- Request headers/path/query parameters: No path, query, or body parameters. The `Authorization` header is required by
  security configuration.
- Responses:

| Status   | Body                                  | Example                                                                                                                                                                                                                                                                                                                                                                       |
|----------|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `200 OK` | `ApiResponse<List<CodebaseResponse>>` | `{"success":true,"message":"Codebases retrieved","data":[{"codebaseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"Server Repo","cloneUrl":"https://github.com/example/server.git","branch":"main","status":"INDEXED","lastCommitSha":"abc123","indexedAt":"2026-08-05T10:00:00Z","createdAt":"2026-08-05T09:00:00Z","updatedAt":"2026-08-05T10:00:00Z","fileCount":12}]}` |

The repository query aggregates file counts in the list query; pagination is not specified in source.

### PATCH /api/codebases/{codebaseId}

- Purpose: Update codebase metadata without reindexing.
- Authentication/authorization: Authenticated user required; the codebase must belong to the authenticated user.
- Request headers/path/query parameters:

| Name            | Type   | Required | Constraints                                           | Description              |
|-----------------|--------|---------:|-------------------------------------------------------|--------------------------|
| `Authorization` | string |      Yes | Authentication is enforced by security configuration. | Authenticated principal. |
| `codebaseId`    | UUID   |      Yes | Valid UUID path value.                                | Target codebase.         |

- Request body: `CodebaseUpdateRequest`

| Field    | Type   | Required | Nullability | Constraints | Description       |
|----------|--------|---------:|-------------|-------------|-------------------|
| `name`   | string |      Yes | Non-null    | `@NotBlank` | New display name. |
| `branch` | string |      Yes | Non-null    | `@NotBlank` | New branch name.  |

`cloneUrl` cannot be updated by this endpoint.

- Responses:

| Status | Body | Example |[](https://github.com/Meet-08/code_vault.git)
|---|---|---| | `200 OK` | `ApiResponse<CodebaseResponse>` |
`{"success":true,"message":"Codebase updated","data":{"codebaseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","name":"Updated Repo","cloneUrl":"https://github.com/example/server.git","branch":"develop","status":"INDEXED","lastCommitSha":"abc123","indexedAt":"2026-08-05T10:00:00Z","createdAt":"2026-08-05T09:00:00Z","updatedAt":"2026-08-05T10:05:00Z","fileCount":12}}` | |
`400 Bad Request` | Validation error envelope |
`{"success":false,"message":"Validation failed","data":{"name":"must not be blank"}}` | | `403 Forbidden` |
`ApiResponse<Void>` | `{"success":false,"message":"You do not own this codebase","data":null}` | | `404 Not Found` |
`ApiResponse<Void>` | `{"success":false,"message":"Codebase not found","data":null}` |

### DELETE /api/codebases/{codebaseId}

- Purpose: Permanently delete the codebase and its indexed repository files and chunks.
- Authentication/authorization: Authenticated user required; the codebase must belong to the authenticated user.
- Request headers/path/query parameters:

| Name            | Type   | Required | Constraints                                           | Description              |
|-----------------|--------|---------:|-------------------------------------------------------|--------------------------|
| `Authorization` | string |      Yes | Authentication is enforced by security configuration. | Authenticated principal. |
| `codebaseId`    | UUID   |      Yes | Valid UUID path value.                                | Target codebase.         |

- Request body: None.
- Responses:

| Status           | Body                | Description                                   |
|------------------|---------------------|-----------------------------------------------|
| `204 No Content` | Empty body          | Codebase and child indexed data were deleted. |
| `403 Forbidden`  | `ApiResponse<Void>` | Authenticated user does not own the codebase. |
| `404 Not Found`  | `ApiResponse<Void>` | Codebase does not exist.                      |
| `409 Conflict`   | `ApiResponse<Void>` | Codebase is `QUEUED` or `PROCESSING`.         |

### POST /api/codebases/{codebaseId}/reindex

- Purpose: Remove existing indexed data and queue a fresh clone/index operation.
- Authentication/authorization: Authenticated user required; the codebase must belong to the authenticated user.
- Request headers/path/query parameters:

| Name            | Type   | Required | Constraints                                           | Description              |
|-----------------|--------|---------:|-------------------------------------------------------|--------------------------|
| `Authorization` | string |      Yes | Authentication is enforced by security configuration. | Authenticated principal. |
| `codebaseId`    | UUID   |      Yes | Valid UUID path value.                                | Target codebase.         |

- Request body: None.
- Responses:

| Status          | Body                                  | Example                                                                                                                                             |
|-----------------|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `202 Accepted`  | `ApiResponse<CodebaseImportResponse>` | `{"success":true,"message":"Codebase reindex queued","data":{"codebaseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","status":"QUEUED","fileCount":0}}` |
| `403 Forbidden` | `ApiResponse<Void>`                   | `{"success":false,"message":"You do not own this codebase","data":null}`                                                                            |
| `404 Not Found` | `ApiResponse<Void>`                   | `{"success":false,"message":"Codebase not found","data":null}`                                                                                      |
| `409 Conflict`  | `ApiResponse<Void>`                   | `{"success":false,"message":"Codebase is currently being indexed","data":null}`                                                                     |

### POST /api/codebases/{codebaseId}/chat/stream

- Purpose: Stream an AI response grounded in the selected codebase.
- Authentication/authorization: Authenticated user required; the requester must own the target codebase.
- Request headers/path/query parameters:

| Name            | Type   | Required | Constraints                                           | Description              |
|-----------------|--------|---------:|-------------------------------------------------------|--------------------------|
| `Authorization` | string |      Yes | Authentication is enforced by security configuration. | Authenticated principal. |
| `codebaseId`    | UUID   |      Yes | Valid UUID path value.                                | Target codebase.         |

- Request body: `CodeChatRequest`

| Field     | Type   | Required | Nullability | Constraints                                                       | Description              |
|-----------|--------|---------:|-------------|-------------------------------------------------------------------|--------------------------|
| `chatId`  | string |       No | Nullable    | Blank or null becomes `default`.                                  | Conversation identifier. |
| `message` | string |      Yes | Non-null    | `@NotBlank(message = "message is required")`; trimmed before use. | User prompt.             |

Example:

```json
{
  "chatId": "architecture-1",
  "message": "How does clone processing work?"
}
```

- Responses:

| Status            | Body                      | Description                                                              |
|-------------------|---------------------------|--------------------------------------------------------------------------|
| `200 OK`          | `text/event-stream`       | Emits `message` events for response text, then `citations`, then `done`. |
| `400 Bad Request` | Validation error envelope | Request validation failed before streaming.                              |
| `403 Forbidden`   | `ApiResponse<Void>`       | Authenticated user does not own the codebase.                            |
| `404 Not Found`   | `ApiResponse<Void>`       | Codebase does not exist.                                                 |

- Stream events:
    - `message`: JSON string containing an assistant text fragment.
    - `citations`: JSON array of `CodeCitation` values.
    - `done`: JSON string containing the normalized chat ID.
    - `error`: JSON object `{"message":"Unable to complete chat"}` when streaming fails.

## WebSocket/message contracts

No WebSocket or STOMP handlers are defined. Realtime delivery is provided through the SSE endpoint documented above.

## Shared schemas

### ApiResponse<T>

| Field     | Type    | Required | Nullability    | Meaning                                                            |
|-----------|---------|---------:|----------------|--------------------------------------------------------------------|
| `success` | boolean |      Yes | Non-null       | Whether the operation succeeded.                                   |
| `message` | string  |      Yes | Non-null       | Human-readable result or error message.                            |
| `data`    | T       |       No | Nullable/empty | Operation payload. The Java source models this with `Optional<T>`. |

### CodebaseResponse

| Field           | Type           | Required | Nullability | Meaning                                                      |
|-----------------|----------------|---------:|-------------|--------------------------------------------------------------|
| `codebaseId`    | UUID           |      Yes | Non-null    | Codebase identity.                                           |
| `name`          | string         |      Yes | Non-null    | Display name.                                                |
| `cloneUrl`      | string         |       No | Nullable    | Repository clone URL.                                        |
| `branch`        | string         |       No | Nullable    | Indexed branch.                                              |
| `status`        | CodebaseStatus |      Yes | Non-null    | Current indexing state.                                      |
| `lastCommitSha` | string         |       No | Nullable    | Last resolved repository commit.                             |
| `indexedAt`     | timestamp      |       No | Nullable    | Time indexing reached `INDEXED`.                             |
| `createdAt`     | timestamp      |      Yes | Non-null    | Audit creation time.                                         |
| `updatedAt`     | timestamp      |      Yes | Non-null    | Audit update time.                                           |
| `fileCount`     | integer        |      Yes | Non-null    | Number of repository file rows associated with the codebase. |

### CodebaseImportResponse

| Field        | Type           | Required | Nullability | Meaning                                         |
|--------------|----------------|---------:|-------------|-------------------------------------------------|
| `codebaseId` | UUID           |      Yes | Non-null    | Codebase identity.                              |
| `status`     | CodebaseStatus |      Yes | Non-null    | Current state; queued responses use `QUEUED`.   |
| `fileCount`  | integer        |      Yes | Non-null    | Processed file count; queued responses use `0`. |

### CodebaseStatus

Enum values: `INDEXED`, `QUEUED`, `PROCESSING`, `FAILED`.

### CodeCitation

| Field       | Type    | Required | Nullability | Meaning                             |
|-------------|---------|---------:|-------------|-------------------------------------|
| `chunkId`   | UUID    |      Yes | Non-null    | Referenced code chunk.              |
| `path`      | string  |      Yes | Non-null    | Repository-relative path.           |
| `startLine` | integer |      Yes | Nullable    | Citation start line when available. |
| `endLine`   | integer |      Yes | Nullable    | Citation end line when available.   |
| `language`  | string  |      Yes | Nullable    | Detected language when available.   |
| `distance`  | number  |      Yes | Non-null    | Similarity distance.                |

### Domain and validation errors

`CodebaseException` errors use the exception’s configured HTTP status and are returned as `ApiResponse<Void>` with
`success=false`, the exception message, and empty data. Validation failures use `ApiResponse<Map<String,String>>`.

## Source references

- `src/main/java/com/meet/server/feature/codebase/CodebaseController.java` — import, list, update, delete, and reindex
  routes.
- `src/main/java/com/meet/server/feature/chat/ChatController.java` — preserved SSE chat route.
- `src/main/java/com/meet/server/feature/codebase/CodebaseService.java` — `startClone`, `getUserCodebases`,
  `updateCodebase`, `reindexCodebase`, `deleteCodebase`, and indexing lifecycle.
- `src/main/java/com/meet/server/feature/chat/ChatService.java` — chat ownership and SSE event generation.
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportRequest.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportResponse.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseResponse.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseUpdateRequest.java`
- `src/main/java/com/meet/server/feature/chat/dto/CodeChatRequest.java`
- `src/main/java/com/meet/server/feature/chat/dto/CodeCitation.java`
- `src/main/java/com/meet/server/feature/codebase/CodebaseStatus.java`
- `src/main/java/com/meet/server/common/api/ApiResponse.java`
- `src/main/java/com/meet/server/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/meet/server/common/exception/CodebaseException.java`
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java`
