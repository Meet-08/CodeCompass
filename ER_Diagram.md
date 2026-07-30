                                  ┌──────────────────────────┐
                                  │          User            │
                                  ├──────────────────────────┤
                                  │ PK id                   │
                                  │ name                    │
                                  │ email (Unique)          │
                                  │ password                │
                                  │ provider                │
                                  │ avatar_url              │
                                  │ created_at              │
                                  │ updated_at              │
                                  └────────────┬────────────┘
                                               │ 1
                                               │
                                               │
                                               │ *
                                  ┌────────────▼────────────┐
                                  │       Codebase          │
                                  ├─────────────────────────┤
                                  │ PK id                  │
                                  │ FK user_id             │
                                  │ owner                  │
                                  │ name                   │
                                  │ provider               │
                                  │ clone_url              │
                                  │ default_branch         │
                                  │ last_commit_sha        │
                                  │ status                 │
                                  │ indexed_at             │
                                  │ created_at             │
                                  │ updated_at             │
                                  └──────┬──────────┬──────┘
                                         │          │
                                    1    │          │ 1
                                         │          │
                                       * │          │ *
                                         ▼          ▼

                    ┌────────────────────────┐    ┌────────────────────────┐
                    │    RepositoryFile      │    │         Chat           │
                    ├────────────────────────┤    ├────────────────────────┤
                    │ PK id                 │    │ PK id                 │
                    │ FK codebase_id        │    │ FK codebase_id        │
                    │ path                  │    │ FK user_id            │
                    │ language              │    │ title                 │
                    │ checksum              │    │ created_at            │
                    │ size                  │    │ updated_at            │
                    └──────────┬────────────┘    └──────────┬────────────┘
                               │                           │
                             1 │                           │ 1
                               │                           │
                             * ▼                           ▼ *

                    ┌────────────────────────┐    ┌────────────────────────┐
                    │      CodeChunk         │    │       Message          │
                    ├────────────────────────┤    ├────────────────────────┤
                    │ PK id                 │    │ PK id                 │
                    │ FK file_id            │    │ FK chat_id            │
                    │ FK codebase_id        │    │ role                  │
                    │ chunk_index           │    │ content               │
                    │ content               │    │ created_at            │
                    │ embedding             │    └────────────────────────┘
                    │ language              │
                    │ path                  │
                    │ start_line            │
                    │ end_line              │
                    │ commit_sha            │
                    │ created_at            │
                    └────────────────────────┘


                               Codebase
                                   │
                                   │ 1
                                   │
                                   │ *
                    ┌──────────────▼──────────────┐
                    │         IndexJob            │
                    ├─────────────────────────────┤
                    │ PK id                      │
                    │ FK codebase_id             │
                    │ status                     │
                    │ current_stage              │
                    │ processed_files            │
                    │ total_files                │
                    │ started_at                 │
                    │ finished_at                │
                    │ error_message              │
                    └─────────────────────────────┘
