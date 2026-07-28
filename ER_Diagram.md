                    ┌──────────────────────────┐
                    │          User            │
                    ├──────────────────────────┤
                    │ PK id                    │
                    │ name                     │
                    │ email (Unique)           │
                    │ password                 │
                    │ provider                 │
                    │ avatar_url               │
                    │ created_at               │
                    │ updated_at               │
                    └────────────┬─────────────┘
                                 │ 1
                                 │
                                 │
                                 │ *
                    ┌────────────▼─────────────┐
                    │       Repository         │
                    ├──────────────────────────┤
                    │ PK id                    │
                    │ FK user_id              │
                    │ owner                   │
                    │ name                    │
                    │ github_url              │
                    │ default_branch          │
                    │ last_commit_sha         │
                    │ status                  │
                    │ indexed_at              │
                    │ created_at              │
                    │ updated_at              │
                    └──────┬───────────┬──────┘
                           │           │
                    1      │           │ 1
                           │           │
                           │           │
                         * │           │ *
                           ▼           ▼

             ┌───────────────────┐   ┌─────────────────────┐
             │ RepositoryFile    │   │      Chat           │
             ├───────────────────┤   ├─────────────────────┤
             │ PK id             │   │ PK id              │
             │ FK repository_id  │   │ FK repository_id   │
             │ path              │   │ FK user_id         │
             │ language          │   │ title              │
             │ checksum          │   │ created_at         │
             │ size              │   │ updated_at         │
             └────────┬──────────┘   └─────────┬──────────┘
                      │                        │
                    1 │                        │ 1
                      │                        │
                      │                        │
                    * ▼                        ▼ *

            ┌───────────────────┐     ┌─────────────────────┐
            │    CodeChunk      │     │      Message        │
            ├───────────────────┤     ├─────────────────────┤
            │ PK id             │     │ PK id              │
            │ FK file_id        │     │ FK chat_id         │
            │ FK repository_id  │     │ role              │
            │ chunk_index       │     │ content           │
            │ content           │     │ created_at        │
            │ embedding         │     └─────────────────────┘
            │ language          │
            │ path              │
            │ start_line        │
            │ end_line          │
            │ commit_sha        │
            │ created_at        │
            └───────────────────┘


                    Repository
                          │
                          │1
                          │
                          │*
               ┌──────────▼───────────┐
               │      IndexJob        │
               ├──────────────────────┤
               │ PK id                │
               │ FK repository_id     │
               │ status               │
               │ current_stage        │
               │ processed_files      │
               │ total_files          │
               │ started_at           │
               │ finished_at          │
               │ error_message        │
               └──────────────────────┘