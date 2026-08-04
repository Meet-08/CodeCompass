[# Codebase API Specification

## Overview

The `codebase` feature manages repository import/indexing and code-aware streaming chat over indexed repositories.
Primary source module: `com.meet.server.feature.codebase`.

## Authentication and common conventions

- All `codebase` endpoints are under `/api/codebases` and require authentication (`anyRequest().authenticated()`).
- Controller success responses that use the common envelope return `ApiResponse<T>` with fields `success`, `message`,
  and `data`.
- Validation and domain errors are handled by `GlobalExceptionHandler` and returned as `ApiResponse` envelopes.
- `POST /api/codebases/{codebaseId}/chat/stream` returns `text/event-stream` (SSE) rather than `ApiResponse`.

## HTTP endpoints

### POST /api/codebases

- Purpose: Queue a repository import/indexing job for the authenticated user.
- Authentication/authorization: Authenticated user required.
- Request headers/path/query parameters:

| Name            | Type     | Required | Constraints                                  | Description                                                              |
|-----------------|----------|---------:|----------------------------------------------|--------------------------------------------------------------------------|
| `Authorization` | `string` |      Yes | JWT processing is implemented in `JwtFilter` | Authenticated principal is used as user id (`authentication.getName()`). |

- Request body: `CodebaseImportRequest`

| Field      | Type     | Required | Nullability  | Constraints                                              | Description                             |
|------------|----------|---------:|--------------|----------------------------------------------------------|-----------------------------------------|
| `name`     | `string` |      Yes | Not nullable | `@NotBlank`                                              | Display name for the imported codebase. |
| `cloneUrl` | `string` |      Yes | Not nullable | `@NotBlank`, `@Pattern((?i)^https://.+$)`                | Repository clone URL.                   |
| `branch`   | `string` |       No | Nullable     | None on DTO; blank or null defaults to `main` in service | Branch to clone.                        |

Example:

```json
{
  "name": "Server Repo",
  "cloneUrl": "https://github.com/example/server.git",
  "branch": "main"
}
```

Responses:

| Status                      | Body schema                                                                                                   | Example                                                                                                                                            |
|-----------------------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `202 Accepted`              | `ApiResponse<CodebaseImportResponse>`                                                                         | `{"success":true,"message":"Codebase import queued","data":{"codebaseId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","status":"QUEUED","fileCount":0}}` |
| `400 Bad Request`           | `ApiResponse<Map<String,String>>` (validation field/global errors) or `ApiResponse<Void>` for malformed input | `{"success":false,"message":"Validation failed","data":{"cloneUrl":"cloneUrl must use HTTPS"}}`                                                    |
| `401 Unauthorized`          | `ApiResponse<Void>`                                                                                           | `{"success":false,"message":"Not specified in source","data":null}`                                                                                |
| `500 Internal Server Error` | `ApiResponse<Void>`                                                                                           | `{"success":false,"message":"An unexpected error occurred","data":null}`                                                                           |

Errors:

- `INVALID_CLONE_URL` (`400`) when clone URL fails service-level URI checks (must be public HTTPS URL, no user info,
  host required).
- Validation errors (`400`) for missing/blank `name` or `cloneUrl`, or regex mismatch.

### POST /api/codebases/{codebaseId}/chat/stream

- Purpose: Stream assistant output for codebase chat and then emit citations and completion events.
- Authentication/authorization: Authenticated user required; requester must own the target codebase.
- Request headers/path/query parameters:

| Name            | Type     | Required | Constraints                                  | Description                                                              |
|-----------------|----------|---------:|----------------------------------------------|--------------------------------------------------------------------------|
| `Authorization` | `string` |      Yes | JWT processing is implemented in `JwtFilter` | Authenticated principal is used as user id (`authentication.getName()`). |
| `codebaseId`    | `UUID`   |      Yes | Path variable                                | Target codebase id.                                                      |

- Request body: `CodeChatRequest`

| Field     | Type     | Required | Nullability  | Constraints                                                              | Description             |
|-----------|----------|---------:|--------------|--------------------------------------------------------------------------|-------------------------|
| `chatId`  | `string` |       No | Nullable     | Blank is normalized to `default` in service                              | Conversation thread id. |
| `message` | `string` |      Yes | Not nullable | `@NotBlank(message = "message is required")`; request message is trimmed | User prompt text.       |

Example:

```json
{
  "chatId": "architecture-1",
  "message": "How does clone processing work?"
}
```

Responses:

| Status             | Body schema                                                                        | Example                                                                                                            |
|--------------------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `200 OK`           | `text/event-stream` of `ServerSentEvent<Object>`                                   | Events include `message` (string chunks), then `citations` (array of `CodeCitation`), then `done` (chatId string). |
| `400 Bad Request`  | `ApiResponse<Map<String,String>>` for bean validation failures before stream start | `{"success":false,"message":"Validation failed","data":{"message":"message is required"}}`                         |
| `401 Unauthorized` | `ApiResponse<Void>` (when unauthenticated before controller)                       | `{"success":false,"message":"Not specified in source","data":null}`                                                |
| `403 Forbidden`    | `ApiResponse<Void>`                                                                | `{"success":false,"message":"You do not own this codebase","data":null}`                                           |
| `404 Not Found`    | `ApiResponse<Void>`                                                                | `{"success":false,"message":"Codebase not found","data":null}`                                                     |

Errors:

- `CODEBASE_NOT_FOUND` (`404`) when `codebaseId` does not exist.
- `CODEBASE_FORBIDDEN` (`403`) when authenticated user does not own the codebase.
- Runtime stream failures after stream begins are emitted as SSE event `error` with payload
  `{"message":"Unable to complete chat"}` (not an HTTP status remap).

## WebSocket/message contracts

No feature-local WebSocket/STOMP `@MessageMapping` handlers were found in source. Realtime behavior is provided via SSE
at `POST /api/codebases/{codebaseId}/chat/stream` (documented above).

## Shared schemas

### ApiResponse<T>

| Field     | Type      | Required | Nullability    | Constraints                             | Meaning                            |
|-----------|-----------|---------:|----------------|-----------------------------------------|------------------------------------|
| `success` | `boolean` |      Yes | Non-null       | None                                    | Operation result flag.             |
| `message` | `string`  |      Yes | Non-null       | None                                    | Human-readable status message.     |
| `data`    | `T`       |       No | Nullable/empty | Wrapped by `Optional<T>` in Java source | Payload for success/error details. |

### CodebaseImportRequest

| Field      | Type     | Required | Nullability | Constraints                                                                           | Meaning                |
|------------|----------|---------:|-------------|---------------------------------------------------------------------------------------|------------------------|
| `name`     | `string` |      Yes | Non-null    | `@NotBlank`                                                                           | Codebase display name. |
| `cloneUrl` | `string` |      Yes | Non-null    | `@NotBlank`, HTTPS regex; additional URI checks in service                            | Clone source URL.      |
| `branch`   | `string` |       No | Nullable    | None; defaults to `main` when null/blank (inferred from `CodebaseService.startClone`) | Target branch.         |

### CodebaseImportResponse

| Field        | Type             | Required | Nullability | Constraints                | Meaning                               |
|--------------|------------------|---------:|-------------|----------------------------|---------------------------------------|
| `codebaseId` | `UUID`           |      Yes | Non-null    | Server-generated           | Created codebase id.                  |
| `status`     | `CodebaseStatus` |      Yes | Non-null    | Enum                       | Current processing state.             |
| `fileCount`  | `integer`        |      Yes | Non-null    | `>= 0` inferred from usage | Indexed file count (`0` when queued). |

`CodebaseStatus` enum values: `INDEXED`, `QUEUED`, `PROCESSING`, `FAILED`.

### CodeChatRequest

| Field     | Type     | Required | Nullability | Constraints                                  | Meaning                  |
|-----------|----------|---------:|-------------|----------------------------------------------|--------------------------|
| `chatId`  | `string` |       No | Nullable    | Blank normalized to `default` in service     | Conversation key suffix. |
| `message` | `string` |      Yes | Non-null    | `@NotBlank(message = "message is required")` | Prompt text for chat.    |

### CodeCitation

| Field       | Type                | Required | Nullability | Constraints | Meaning                        |
|-------------|---------------------|---------:|-------------|-------------|--------------------------------|
| `chunkId`   | `UUID`              |      Yes | Non-null    | None        | Referenced chunk id.           |
| `path`      | `string`            |      Yes | Non-null    | None        | Repository-relative file path. |
| `startLine` | `integer`           |      Yes | Nullable    | None        | Start line in cited file.      |
| `endLine`   | `integer`           |      Yes | Nullable    | None        | End line in cited file.        |
| `language`  | `string`            |      Yes | Nullable    | None        | Cited code language.           |
| `distance`  | `number` (`double`) |      Yes | Non-null    | None        | Similarity distance score.     |

### Validation error payload (`ApiResponse<Map<String,String>>`)

| Field     | Type      | Required | Nullability | Constraints                   | Meaning                                              |
|-----------|-----------|---------:|-------------|-------------------------------|------------------------------------------------------|
| `success` | `boolean` |      Yes | Non-null    | `false` on errors             | Envelope status.                                     |
| `message` | `string`  |      Yes | Non-null    | Typically `Validation failed` | Error summary.                                       |
| `data`    | `object`  |       No | Nullable    | Key-value map                 | Field/object/violation messages keyed by field/path. |

## Source references

- `src/main/java/com/meet/server/feature/codebase/CodebaseController.java` (`importCodebase`, `streamChat`)
- `src/main/java/com/meet/server/feature/codebase/CodebaseService.java` (`startClone`, `validateCloneUrl`, `process`,
  `streamChat`)
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportRequest.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodebaseImportResponse.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodeChatRequest.java`
- `src/main/java/com/meet/server/feature/codebase/dto/CodeCitation.java`
- `src/main/java/com/meet/server/feature/codebase/CodebaseStatus.java`
- `src/main/java/com/meet/server/common/api/ApiResponse.java`
- `src/main/java/com/meet/server/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/meet/server/common/exception/CodebaseException.java`
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java`]([]())