# CLAUDE.md

Claude Code guidance for M3UAndroid. Read `AGENTS.md` (root) and the nested `AGENTS.md` nearest to the code you are changing. This file adds Claude-specific workflow notes on top of those rules.

## Project At A Glance

| Item | Value |
|---|---|
| App | IPTV player — M3U, Xtream Codes, EPG, DLNA |
| Platforms | Smartphone (`:app:smartphone`) · TV (`:app:tv`) |
| Language | 100% Kotlin (no Java) |
| Min SDK | 26 (Android 8.0) |
| Compile SDK | 36 |
| Version | 1.15.1 (smartphone) / 1.0.1 (TV) |
| DB schema | Room v20 |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |

## Architecture Layers (dependency order)

```
app  →  business  →  data
              ↓         ↓
            core  ←  i18n
```

- **app** — Compose screens, navigation, Hilt entry points, platform UI
- **business** — ViewModels, feature state, user actions (KMP-friendly, no DAO access)
- **data** — Room, repositories, parsers (M3U/Xtream/EPG), Media3, WorkManager, Ktor server
- **core/foundation** — Shared primitives, DataStore, types, UI helpers (no Android APIs where avoidable)
- **i18n** — String resources only

UI must NOT directly access DAOs, databases, parsers, or low-level data sources.

## Before Making Changes

1. Read the file(s) you intend to modify.
2. Check the nested `AGENTS.md` for the layer you are working in.
3. Search for existing helpers before adding new abstractions.

## Build Commands

```bash
# Assemble smartphone debug
./gradlew :app:smartphone:assembleDebug

# Assemble TV debug
./gradlew :app:tv:assembleDebug

# Lint check on a module
./gradlew :data:lint

# Run unit tests
./gradlew :data:test

# Validate a specific module
./gradlew :<module>:assembleDebug
```

Always validate the smallest relevant module first.

## Key Files

| File | Purpose |
|---|---|
| `gradle/libs.versions.toml` | All dependency versions — edit here only |
| `gradle.properties` | JVM heap, config cache, AndroidX flags |
| `compose_compiler_config.conf` | Compose stability config |
| `native-load.yml` | FFmpeg SO load order for nextlib |
| `data/src/main/.../M3UDatabase.kt` | Room DB, entity list, migration list |
| `core/foundation/src/.../Preferences.kt` | All DataStore preference keys |
| `app/smartphone/src/main/AndroidManifest.xml` | Permissions, activities, providers |

## Coding Rules

- Kotlin only — no Java files.
- No star imports.
- No inline dependency versions — all coordinates go through `libs.versions.toml`.
- No Android platform APIs in business logic, parsers, or models.
- Imports use qualified names; use import aliases for name conflicts.
- Context parameters (`-Xcontext-parameters`) enabled globally — opt in when needed.
- Broad Compose opt-ins are applied globally in root `build.gradle.kts`; no need to repeat them per-file.

## Room / Database Rules

- Never change a Room entity, table, or column without adding a migration AND updating the schema JSON artifact.
- Schema dir: `data/schemas/`.
- Auto-migrations preferred; write manual `Migration` only when auto-migration cannot handle it.
- Do not change `@ColumnInfo(name = …)` values without a migration.

## Adding Dependencies

1. Add version to `[versions]` in `gradle/libs.versions.toml`.
2. Add library entry to `[libraries]`.
3. Reference via `libs.<alias>` in the module `build.gradle.kts`.
4. Use only Google, MavenCentral, JitPack, or Gradle Plugin Portal — no unknown repositories, no local jars.

## TV-Specific Rules

- All interactive elements need explicit `Modifier.focusable()` / focus semantics.
- Minimum touch/focus target: 48×48 dp; prefer larger for couch distance.
- No hover-only interactions; all actions must be reachable via DPad.
- Read `app/tv/AGENTS.md` before touching any TV screen.

## Native / FFmpeg

- Native SO load order defined in `native-load.yml`: avutil → swresample → swscale → avcodec → avformat → media3ext → mediainfo.
- Loading is coordinated by `CodecNativeLoader` via the custom `native-load-gradle-plugin` (composite build).
- Do not change SO load order without verifying symbol dependencies.

## Scope Discipline

- Keep PRs narrowly scoped. Do not mix feature work, refactors, and formatting in one PR.
- Do not rewrite surrounding code when fixing a targeted bug.
- Do not add error handling for impossible scenarios.
- Do not add comments to code you did not change.
