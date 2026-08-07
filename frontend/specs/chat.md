# Chat API Specification

## Overview

The `feature.chat` module manages chat sessions associated with an owned codebase and streams code-aware assistant responses. It persists user and assistant messages, retrieves relevant code through `CodeAdvisor`, emits citations, and generates a title after the first successful response.

## Authentication and common conventions

- All chat routes require an authenticated request. `SecurityConfig` permits only the listed auth and OAuth routes; all other requests require authentication.
- The authenticated principal name is parsed as the user UUID. The chat controller uses this UUID for ownership checks.
- The supported authentication mechanism is the repository's JWT filter with an `Authorization: Bearer <token>` header. The exact token issuance contract is defined by the auth feature.
- JSON HTTP responses use `ApiResponse<T>`: `success`, `message`, and `data`. Successful session endpoints use `success: true`; error responses use `success: false`.
- UUID path parameters are server-side identifiers. Timestamps in session responses are `Instant` values serialized according to the application's Jackson configuration.
- The stream endpoint produces `text/event-stream`. Each SSE event has an event name and a JSON-serialized `data` value.
- The rate-limit filter may reject any request with HTTP `429 Too Many Requests` and a plain-text body. It adds `X-Rate-Limit-Retry-After-Seconds` when rejected.

## HTTP endpoints

### GET /api/codebases/{codebaseId}/chat/sessions/{sessionId}/messages

- Purpose: Fetch persisted messages for an owned chat session. The first request returns the newest page; subsequent requests use `before` to fetch progressively older messages.
- Authentication/authorization: Authentication required. The user must own the codebase and session.
- Query parameters:

  | Name     | Type    | Required | Constraints                 | Description                             |
  | -------- | ------- | -------- | --------------------------- | --------------------------------------- |
  | `limit`  | integer | No       | `1` to `100`; default `20`  | Maximum number of messages in the page. |
  | `before` | string  | No       | Opaque server-issued cursor | Loads messages older than the cursor.   |

- Responses:

  #### 200 OK

  Body: `ApiResponse<ChatHistoryResponse>`.

  ```json
  {
    "success": true,
    "message": "Chat history retrieved",
    "data": {
      "messages": [
        {
          "messageId": "33333333-3333-3333-3333-333333333333",
          "role": "USER",
          "content": "Where is indexing implemented?",
          "createdAt": "2026-08-07T10:00:00Z",
          "updatedAt": "2026-08-07T10:00:00Z"
        }
      ],
      "hasMore": true,
      "nextCursor": "opaque-cursor"
    }
  }
  ```

- Messages within each response are ordered oldest to newest. When `hasMore` is false, `nextCursor` is null.
- Errors: Invalid `limit` or `before` returns `400`. Codebase/session ownership errors return `403` or `404`; unauthenticated requests receive `401`.

### GET /api/codebases/{codebaseId}/chat/sessions

- Purpose: List the authenticated user's chat sessions for a codebase, ordered by `updatedAt` descending.
- Authentication/authorization: Authentication required. The user must own the codebase; otherwise the service raises `CODEBASE_NOT_FOUND` (`404`) or `CODEBASE_FORBIDDEN` (`403`).
- Request headers/path/query parameters:

  | Name            | Type                | Required | Constraints              | Description                                                                        |
  | --------------- | ------------------- | -------- | ------------------------ | ---------------------------------------------------------------------------------- |
  | `Authorization` | Bearer token        | Yes      | Must authenticate a user | Authentication header; exact JWT details are defined by the security/auth modules. |
  | `codebaseId`    | UUID path parameter | Yes      | Valid UUID syntax        | Codebase whose sessions are listed.                                                |

- Request body: None.
- Responses:

  #### 200 OK

  Body: `ApiResponse<List<ChatSessionResponse>>`.

  ```json
  {
    "success": true,
    "message": "Chat sessions retrieved",
    "data": [
      {
        "sessionId": "11111111-1111-1111-1111-111111111111",
        "codebaseId": "22222222-2222-2222-2222-222222222222",
        "title": "untitled-1",
        "createdAt": "2026-08-07T10:00:00Z",
        "updatedAt": "2026-08-07T10:00:00Z"
      }
    ]
  }
  ```

- Errors: See shared errors. Ownership/domain errors are `404` or `403` as described above; unauthenticated requests receive `401`.

### PATCH /api/codebases/{codebaseId}/chat/sessions/{sessionId}

- Purpose: Replace the title of an owned chat session.
- Authentication/authorization: Authentication required. The user must own both the codebase and session. The service raises `CODEBASE_NOT_FOUND` (`404`), `CODEBASE_FORBIDDEN` (`403`), `CHAT_SESSION_NOT_FOUND` (`404`), or `CHAT_SESSION_FORBIDDEN` (`403`) when applicable.
- Request headers/path/query parameters:

  | Name            | Type                | Required     | Constraints              | Description                 |
  | --------------- | ------------------- | ------------ | ------------------------ | --------------------------- |
  | `Authorization` | Bearer token        | Yes          | Must authenticate a user | Authentication header.      |
  | `codebaseId`    | UUID path parameter | Yes          | Valid UUID syntax        | Owning codebase identifier. |
  | `sessionId`     | UUID path parameter | Yes          | Valid UUID syntax        | Session to rename.          |
  | `Content-Type`  | Media type          | Yes for body | JSON request             | Request body media type.    |

- Request body: `ChatSessionUpdateRequest`.

  | Field   | Type   | Required | Nullability/constraints                               | Description        |
  | ------- | ------ | -------- | ----------------------------------------------------- | ------------------ |
  | `title` | string | Yes      | `@NotBlank`; the service trims surrounding whitespace | New session title. |

  ```json
  { "title": "Repository indexing questions" }
  ```

- Responses:

  #### 200 OK

  Body: `ApiResponse<ChatSessionResponse>`.

  ```json
  {
    "success": true,
    "message": "Chat session updated",
    "data": {
      "sessionId": "11111111-1111-1111-1111-111111111111",
      "codebaseId": "22222222-2222-2222-2222-222222222222",
      "title": "Repository indexing questions",
      "createdAt": "2026-08-07T10:00:00Z",
      "updatedAt": "2026-08-07T10:05:00Z"
    }
  }
  ```

- Errors: A blank or missing `title` returns `400` with validation data. Ownership/session errors return `403` or `404` as described above; unauthenticated requests receive `401`.

### DELETE /api/codebases/{codebaseId}/chat/sessions/{sessionId}

- Purpose: Delete an owned chat session. Database cascade configuration removes its chat messages.
- Authentication/authorization: Authentication required. The user must own the codebase and session; ownership/domain errors are `403` or `404` as described for the update endpoint.
- Request headers/path/query parameters:

  | Name            | Type                | Required | Constraints              | Description                 |
  | --------------- | ------------------- | -------- | ------------------------ | --------------------------- |
  | `Authorization` | Bearer token        | Yes      | Must authenticate a user | Authentication header.      |
  | `codebaseId`    | UUID path parameter | Yes      | Valid UUID syntax        | Owning codebase identifier. |
  | `sessionId`     | UUID path parameter | Yes      | Valid UUID syntax        | Session to delete.          |

- Request body: None.
- Responses:

  #### 204 No Content

  Empty body.

- Errors: Ownership/session errors return `403` or `404`; unauthenticated requests receive `401`.

### POST /api/codebases/{codebaseId}/chat/stream

- Purpose: Submit a message to a codebase-aware chat and receive an SSE stream. If `chatId` is absent or blank, a new session is created with the next available `untitled-N` title; otherwise the supplied session is reused after ownership validation.
- Authentication/authorization: Authentication required. The user must own the codebase and, when supplied, the chat session. `chatId` must be a valid UUID when non-blank; invalid values produce `INVALID_CHAT_ID` (`400`). Codebase/session ownership errors produce `403` or `404`.
- Request headers/path/query parameters:

  | Name            | Type                | Required    | Constraints              | Description                                     |
  | --------------- | ------------------- | ----------- | ------------------------ | ----------------------------------------------- |
  | `Authorization` | Bearer token        | Yes         | Must authenticate a user | Authentication header.                          |
  | `Content-Type`  | Media type          | Yes         | JSON request             | Request body media type.                        |
  | `Accept`        | Media type          | Recommended | `text/event-stream`      | Requests the SSE response representation.       |
  | `codebaseId`    | UUID path parameter | Yes         | Valid UUID syntax        | Codebase used for ownership and code retrieval. |

- Request body: `CodeChatRequest`.

  | Field     | Type                   | Required | Nullability/constraints                                           | Description                                                           |
  | --------- | ---------------------- | -------- | ----------------------------------------------------------------- | --------------------------------------------------------------------- |
  | `chatId`  | string containing UUID | No       | May be absent, null, or blank; if non-blank it must parse as UUID | Existing session to continue. Server-generated when omitted or blank. |
  | `message` | string                 | Yes      | `@NotBlank`; service trims it before processing                   | User prompt.                                                          |

  ```json
  {
    "chatId": "11111111-1111-1111-1111-111111111111",
    "message": "Where is the repository indexing pipeline implemented?"
  }
  ```

- Responses:

  #### 200 OK

  Content type: `text/event-stream`.

  Events are emitted in this logical order. Assistant output may be split across multiple `message` events according to the model stream.

  | Event       | Data JSON type                    | Meaning                                                                                                                             |
  | ----------- | --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
  | `message`   | JSON string                       | A streamed assistant text fragment. The fragment is also accumulated and saved as one assistant message when streaming completes.   |
  | `citations` | JSON array of `CodeCitation`      | Retrieved code citations. Emitted after model output; may be empty.                                                                 |
  | `title`     | JSON string                       | Generated title, emitted only on the first response when a non-empty answer was produced.                                           |
  | `done`      | JSON string containing UUID       | Completion marker containing the resolved chat session ID.                                                                          |
  | `error`     | JSON object with `message` string | Emitted when streaming fails after the stream has started; the current implementation uses `{"message":"Unable to complete chat"}`. |

  Example event sequence:

  ```text
  event: message
  data: "The indexing pipeline starts in ..."

  event: citations
  data: [{"chunkId":"33333333-3333-3333-3333-333333333333","path":"src/main/...","startLine":10,"endLine":30,"language":"java","distance":0.12}]

  event: title
  data: "Repository indexing pipeline"

  event: done
  data: "11111111-1111-1111-1111-111111111111"
  ```

- Errors: Request validation, invalid `chatId`, and ownership failures occurring before stream creation use the global HTTP error handling described in Shared schemas. Failures during the reactive stream are converted to an `error` SSE event; the stream's HTTP status is not changed by that handler.

## WebSocket/message contracts

The chat feature defines no WebSocket, STOMP, or message-destination handlers. Chat streaming is HTTP SSE only.

## Shared schemas

### ApiResponse<T>

| Field     | Type             | Required                           | Nullability/constraints                                                                                                                                                | Description                             |
| --------- | ---------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `success` | boolean          | Yes                                | Not nullable in the record                                                                                                                                             | Whether the HTTP operation succeeded.   |
| `message` | string           | Yes                                | Source does not declare a validation constraint                                                                                                                        | Human-readable result or error message. |
| `data`    | T or absent/null | Yes as a serialized envelope field | The record stores `Optional<T>` for normal controller responses; error handlers use an empty value. Exact absent/null JSON rendering depends on Jackson configuration. |

### ChatSessionResponse

| Field        | Type    | Required | Nullability/constraints                                        | Description                 |
| ------------ | ------- | -------- | -------------------------------------------------------------- | --------------------------- |
| `sessionId`  | UUID    | Yes      | Server-generated                                               | Chat session identifier.    |
| `codebaseId` | UUID    | Yes      | Server-associated                                              | Parent codebase identifier. |
| `title`      | string  | Yes      | Persisted non-null; generated sessions start with `untitled-N` | Session title.              |
| `createdAt`  | Instant | Yes      | Auditing field, persisted non-null                             | Creation timestamp.         |
| `updatedAt`  | Instant | Yes      | Auditing field, persisted non-null                             | Last update timestamp.      |

### CodeCitation

| Field       | Type    | Required                                    | Nullability/constraints                  | Description                      |
| ----------- | ------- | ------------------------------------------- | ---------------------------------------- | -------------------------------- |
| `chunkId`   | UUID    | Source record has no validation annotations | Source does not specify JSON nullability | Retrieved code chunk identifier. |
| `path`      | string  | Source record has no validation annotations | Source does not specify JSON nullability | Repository file path.            |
| `startLine` | integer | Source record has no validation annotations | Source does not specify JSON nullability | Retrieved chunk start line.      |
| `endLine`   | integer | Source record has no validation annotations | Source does not specify JSON nullability | Retrieved chunk end line.        |
| `language`  | string  | Source record has no validation annotations | Source does not specify JSON nullability | Chunk language classification.   |
| `distance`  | number  | Yes in the record shape                     | Primitive `double`                       | Similarity-search distance.      |

### Validation error response

For `@Valid` request-body failures, `GlobalExceptionHandler` returns HTTP `400`:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": { "message": "message is required" }
}
```

The `data` map is keyed by field name. The exact JSON representation of an empty error data value is controlled by the `Optional`/Jackson configuration.

### Domain and authentication error response

`CodebaseException` is mapped to its source-defined HTTP status and message, without exposing `errorCode` in the response body:

```json
{
  "success": false,
  "message": "Codebase not found",
  "data": null
}
```

Known chat-relevant domain codes are `CODEBASE_NOT_FOUND`, `CODEBASE_FORBIDDEN`, `CHAT_SESSION_NOT_FOUND`, `CHAT_SESSION_FORBIDDEN`, `INVALID_CHAT_ID`, and `USER_NOT_FOUND`. The first, third, and last are `404`; ownership failures are `403`; `INVALID_CHAT_ID` is `400`.

Unauthenticated requests are returned as HTTP `401` with the `ApiResponse<Void>` shape and message `Unauthorized`.

## Source references

- `src/main/java/com/meet/server/feature/chat/ChatController.java`: route declarations, authentication extraction, response envelopes, and SSE content type.
- `src/main/java/com/meet/server/feature/chat/ChatService.java`: session resolution, message persistence, advisor context, SSE event generation, title timing, and stream error behavior.
- `src/main/java/com/meet/server/feature/chat/ChatTitleService.java`: generated title normalization and persistence.
- `src/main/java/com/meet/server/feature/chat/dto/CodeChatRequest.java`: stream request fields and `message` validation.
- `src/main/java/com/meet/server/feature/chat/dto/ChatSessionUpdateRequest.java`: title update field and validation.
- `src/main/java/com/meet/server/feature/chat/dto/ChatSessionResponse.java`: session response fields.
- `src/main/java/com/meet/server/feature/chat/dto/CodeCitation.java`: citation fields.
- `src/main/java/com/meet/server/feature/chat/session/ChatSessionService.java`: ownership checks, session creation, listing, update, deletion, and domain errors.
- `src/main/java/com/meet/server/feature/chat/session/mapper/ChatSessionMapper.java`: entity-to-response conversion.
- `src/main/java/com/meet/server/feature/chat/message/ChatMessageService.java`: prompt history and persisted message roles.
- `src/main/java/com/meet/server/feature/advisor/CodeAdvisor.java`: codebase context and citation propagation.
- `src/main/java/com/meet/server/feature/retriver/CodeRetriever.java`: similarity retrieval and citation construction.
- `src/main/java/com/meet/server/common/api/ApiResponse.java`: common response envelope.
- `src/main/java/com/meet/server/common/exception/GlobalExceptionHandler.java`: validation, domain, malformed-request, and unexpected-error mappings.
- `src/main/java/com/meet/server/common/exception/CodebaseException.java`: domain error code/status model.
- `src/main/java/com/meet/server/common/security/config/SecurityConfig.java`: authentication requirement for non-permitted routes.
- `src/main/java/com/meet/server/common/security/handler/UnauthorizedResponseHandler.java`: unauthenticated response.
- `src/main/java/com/meet/server/common/ratelimit/filter/RateLimiterFilter.java`: rate-limit response and headers.
- `src/main/java/com/meet/server/feature/chat/session/ChatSession.java` and `src/main/java/com/meet/server/feature/chat/message/ChatMessage.java`: persisted session/message fields and relationships.
