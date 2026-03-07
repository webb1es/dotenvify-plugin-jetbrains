# DotEnvify JetBrains Plugin — Roadmap

## Vision

Bring DotEnvify's CLI power natively into JetBrains IDEs (IntelliJ, GoLand, WebStorm,
PyCharm, etc.) with a Kotlin-based plugin — no external binary dependencies.

---

## Phase 1: Foundation (MVP)

The minimum viable plugin that delivers core value to users.

### 1.1 Project Scaffolding
- Gradle build with IntelliJ Platform Gradle Plugin 2.x
- Plugin descriptor (plugin.xml)
- Kotlin project structure
- CI-ready (builds, tests, verifyPlugin)

### 1.2 Core Parser
Rewrite the Go parsing logic in Kotlin. Support all input formats:
- `KEY=VALUE` (standard)
- `KEY="VALUE"` (quoted)
- `KEY VALUE` (space-separated)
- `KEY` on one line, `VALUE` on next line (line-pair)
- Mixed formats in a single input
- Comments (`#` lines) are ignored
- Blank lines are ignored

### 1.3 Core Formatter
Output generation with all CLI flags ported:
- Optional `export` prefix
- Alphabetical sorting (on by default, toggle off)
- Smart quoting (URLs, values with spaces)
- Filter: ignore lowercase keys (`no-lower`)
- Filter: URL-only values (`url-only`)
- Preserve specific existing keys (`preserve`)

### 1.4 File I/O
- Read existing `.env` files (for preserve logic)
- Write output with secure permissions
- Incremental backup system (`.env.backup.1`, `.env.backup.2`, ...)
- Overwrite mode (skip backup)

### 1.5 IDE Actions
- **"DotEnvify: Convert Selection"** — convert selected text in editor to `.env` format
- **"DotEnvify: Convert File"** — convert an entire file
- Actions available via: Editor context menu, Tools menu, keyboard shortcuts

### 1.6 Tool Window
- Input text area (paste raw key-value data)
- Output preview panel (see formatted result before writing)
- Toggle controls matching CLI flags (export, sort, no-lower, url-only)
- "Apply to .env" button — writes to project `.env`
- "Copy to Clipboard" button

### 1.7 Settings
- **Global defaults** (IDE-wide): export prefix, sort, filters
- **Per-project overrides**: output path, preserve keys, project-specific filters
- Toggle: "Use global defaults" in project settings
- Settings accessible via: Settings/Preferences > Tools > DotEnvify

---

## Phase 2: Azure DevOps Integration

### 2.1 OAuth Authentication
- Device Code Flow (primary — best for IDEs)
- Browser-based OAuth (optional toggle)
- Token storage in JetBrains Password Safe
- `AuthProvider` interface for swappable flows

### 2.2 Variable Group Fetch
- REST API client for Azure DevOps
- Organization / Project / Variable Group picker
- Multi-group support with merge and precedence
- Duplicate key warnings

### 2.3 Azure UX
- "Sign in to Azure DevOps" button in tool window
- Org/project/group selection UI
- Status indicators for auth state
- Merge preview before applying

---

## Phase 3: Intelligence & Polish

### 3.1 Diagnostics
- Detect missing keys (referenced in code but not in `.env`)
- Detect unused keys (in `.env` but not referenced)
- Quick-fix suggestions

### 3.2 Auto-Watch
- Monitor source files for changes
- Auto-regenerate `.env` on change (opt-in)

### 3.3 Enhanced Merge
- Side-by-side diff when merging Azure groups
- Conflict resolution UI for duplicate keys

---

## Phase 4: Ecosystem

### 4.1 Multiple Cloud Providers
- AWS Parameter Store / Secrets Manager
- GCP Secret Manager
- HashiCorp Vault

### 4.2 Team Features
- Team-wide default settings via VCS-committed config

### 4.3 Marketplace
- Publish to JetBrains Marketplace
- Plugin icon and branding
- Documentation site

---

## Compatibility

- **Minimum IDE version:** 2024.1
- **Target IDEs:** IntelliJ IDEA, GoLand, WebStorm, PyCharm, Rider, CLion, RubyMine
- **JDK:** 17+ (plugin runtime)
- **Build JDK:** 21 (development)

---

## Architecture

```
dev.webbies.dotenvify
├── core/
│   ├── DotEnvParser.kt        — Multi-format input parser
│   ├── DotEnvFormatter.kt     — Output formatter with filters
│   ├── DotEnvIO.kt            — File read/write with backups
│   └── DotEnvModels.kt        — Data classes (EnvEntry, FormatOptions, etc.)
├── actions/
│   ├── ConvertSelectionAction.kt
│   └── ConvertFileAction.kt
├── ui/
│   ├── DotEnvifyToolWindow.kt
│   └── DotEnvifyToolWindowFactory.kt
├── settings/
│   ├── DotEnvifySettings.kt         — Global settings state
│   ├── DotEnvifyProjectSettings.kt  — Per-project settings state
│   └── DotEnvifySettingsConfigurable.kt — Settings UI
└── azure/                           — Phase 2
    ├── AzureAuthProvider.kt
    ├── AzureDevOpsClient.kt
    └── AzureVariableGroupPanel.kt
```
