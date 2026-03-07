# DotEnvify JetBrains Plugin — Task Tracker

> Mark tasks with `[x]` as they are completed.
> Add date of completion in parentheses, e.g. `[x] (2026-03-07) Task name`

---

## Phase 1: Foundation (MVP)

### 1.1 Project Scaffolding
- [x] (2026-03-07) Create Gradle build config (build.gradle.kts, settings.gradle.kts)
- [x] (2026-03-07) Set up Gradle wrapper
- [x] (2026-03-07) Create plugin.xml descriptor
- [x] (2026-03-07) Set up Kotlin source structure
- [x] (2026-03-07) Create ROADMAP.md and TODO.md
- [x] (2026-03-07) Verify plugin builds successfully (`./gradlew buildPlugin`)
- [ ] Verify plugin runs in sandbox IDE (`./gradlew runIde`)

### 1.2 Core Parser
- [x] (2026-03-07) Create `DotEnvModels.kt` — data classes (EnvEntry, ParseResult, FormatOptions)
- [x] (2026-03-07) Implement `DotEnvParser.kt` — multi-format parser
  - [x] Parse `KEY=VALUE` format
  - [x] Parse `KEY="VALUE"` quoted format
  - [x] Parse `KEY VALUE` space-separated format
  - [x] Parse line-pair format (KEY on line 1, VALUE on line 2)
  - [x] Handle mixed formats in single input
  - [x] Skip comments (`#` lines) and blank lines
  - [x] Handle edge cases (empty values, `=` in values, special characters)
- [x] (2026-03-07) Write unit tests for parser (aim for full coverage of all formats)

### 1.3 Core Formatter
- [x] (2026-03-07) Implement `DotEnvFormatter.kt` — output generation
  - [x] Basic `KEY=VALUE` output
  - [x] Optional `export` prefix
  - [x] Alphabetical sorting (default on)
  - [x] Smart quoting (URLs, values with spaces, already-quoted)
  - [x] Filter: ignore lowercase keys
  - [x] Filter: URL-only values
- [x] (2026-03-07) Write unit tests for formatter

### 1.4 File I/O
- [x] (2026-03-07) Implement `DotEnvIO.kt`
  - [x] Read existing `.env` file into EnvEntry list
  - [x] Write formatted output to file with secure permissions
  - [x] Incremental backup logic (`.backup.1`, `.backup.2`, ...)
  - [x] Overwrite mode (skip backup)
  - [x] Preserve logic (keep existing values for specified keys)
- [x] (2026-03-07) Write unit tests for file I/O

### 1.5 IDE Actions
- [x] (2026-03-07) Implement `ConvertSelectionAction.kt`
  - [x] Get selected text from editor
  - [x] Parse and format
  - [x] Show preview dialog with live toggles (export, sort, no-lower, url-only)
  - [x] Replace selection with formatted output
- [x] (2026-03-07) Implement `ConvertFileAction.kt`
  - [x] Read source file
  - [x] Parse and format
  - [x] Show preview dialog
  - [x] Save to user-chosen `.env` path with backup
- [x] (2026-03-07) Register actions in plugin.xml (editor context menu, project view, Tools menu)
- [x] (2026-03-07) Add keyboard shortcut (Ctrl+Alt+E for Convert Selection)

### 1.6 Tool Window
- [x] (2026-03-07) Implement `DotEnvifyToolWindowFactory.kt`
- [x] (2026-03-07) Implement `DotEnvifyToolWindowPanel.kt`
  - [x] Input text area with placeholder text
  - [x] Output preview panel (live preview as you type/toggle)
  - [x] Toggle checkboxes: export, sort, no-lower, url-only
  - [x] "Apply to .env" button with save dialog
  - [x] "Copy to Clipboard" button
  - [x] "Clear" button
  - [x] Status bar (entry count, warnings)
- [x] (2026-03-07) Register tool window in plugin.xml (bottom panel)

### 1.7 Settings
- [x] (2026-03-07) Implement `DotEnvifySettings.kt` — global settings state (persisted to XML)
- [x] (2026-03-07) Implement `DotEnvifyProjectSettings.kt` — per-project settings (persisted to XML)
- [x] (2026-03-07) Implement `DotEnvifySettingsConfigurable.kt` — global settings UI
  - [x] Export prefix toggle
  - [x] Sort toggle
  - [x] No-lower toggle
  - [x] URL-only toggle
  - [x] Default output path
- [x] (2026-03-07) Implement `DotEnvifyProjectSettingsConfigurable.kt` — project settings UI
  - [x] "Use global defaults" toggle (disables project fields when checked)
  - [x] Preserve keys list (comma-separated)
  - [x] All format toggles + output path
- [x] (2026-03-07) Register services and configurables in plugin.xml

### 1.8 Testing & Release Prep
- [x] (2026-03-07) Manual testing in IntelliJ IDEA sandbox
- [ ] Manual testing in GoLand sandbox
- [ ] Fix any compatibility issues
- [x] (2026-03-07) Write README.md with usage instructions
- [ ] Create plugin icon (40x40 SVG)
- [ ] Prepare for JetBrains Marketplace submission

---

## Phase 2: Azure DevOps Integration

### 2.1 OAuth Authentication
- [x] (2026-03-07) Implement `AzureAuthProvider.kt` with Device Code Flow
- [x] (2026-03-07) Device Code Flow (request code, poll for token)
- [x] (2026-03-07) Browser-based OAuth investigated — not feasible without custom app registration (redirect URI restrictions). Device Code Flow is the standard for IDE plugins.
- [x] (2026-03-07) Token storage in JetBrains Password Safe
- [x] (2026-03-07) Token refresh logic
- [x] (2026-03-07) Sign-out flow

### 2.2 Variable Group Fetch
- [x] (2026-03-07) Implement `AzureDevOpsClient.kt` — REST API client
- [x] (2026-03-07) List variable groups
- [x] (2026-03-07) Get variable group by name
- [x] (2026-03-07) Fetch variables from group(s)
- [x] (2026-03-07) Multi-group merge with last-wins precedence
- [x] (2026-03-07) Duplicate key detection and warnings
- [x] (2026-03-07) Secret variable detection (skipped with warning)
- [x] (2026-03-07) URL parser for both dev.azure.com and visualstudio.com formats
- [x] (2026-03-07) Unit tests for URL parsing and models

### 2.3 Azure UX
- [x] (2026-03-07) "Sign in to Azure DevOps" button with device code dialog
- [x] (2026-03-07) Org URL / project / group name input fields
- [x] (2026-03-07) Auth status indicator (signed in/out)
- [x] (2026-03-07) Fetch preview panel with formatted output
- [x] (2026-03-07) "Apply to .env" button
- [x] (2026-03-07) Error handling and user feedback
- [x] (2026-03-07) Azure DevOps tab added to tool window

---

## Phase 3: Intelligence & Polish

### 3.1 Diagnostics
- [x] (2026-03-07) Implement `EnvKeyScanner.kt` — multi-language env key reference scanner
  - [x] JavaScript/TypeScript: `process.env.KEY`, `process.env['KEY']`, `import.meta.env.KEY`
  - [x] Python: `os.environ['KEY']`, `os.environ.get('KEY')`, `os.getenv('KEY')`
  - [x] Go: `os.Getenv("KEY")`
  - [x] Java/Kotlin: `System.getenv("KEY")`, `dotenv.get("KEY")`, `env("KEY")`
  - [x] Ruby: `ENV['KEY']`, `ENV.fetch('KEY')`
  - [x] PHP: `getenv('KEY')`, `$_ENV['KEY']`
  - [x] Rust: `env::var("KEY")`
  - [x] C#: `Environment.GetEnvironmentVariable("KEY")`
  - [x] YAML/Docker: `${KEY}` variable substitution
  - [x] Skips `node_modules`, `.git`, `build`, `dist`, `vendor`, etc.
- [x] (2026-03-07) Implement `EnvDiagnostics.kt` — diagnostic analysis engine
  - [x] Missing key detection (referenced in code but not in `.env`)
  - [x] Unused key detection (in `.env` but not referenced in code)
- [x] (2026-03-07) Implement `DiagnosticsPanel.kt` — diagnostics UI tab
  - [x] "Run Diagnostics" button with background scanning
  - [x] Results view with summary, missing keys (with file:line references), unused keys
  - [x] Diagnostics tab added to tool window
- [x] (2026-03-07) Write unit tests — 15 scanner tests + 4 diagnostics tests (all passing)

### 3.2 Auto-Watch
- [x] (2026-03-07) Implement `EnvFileWatcher.kt` — project-scoped VFS file watcher
  - [x] Watches for .env file changes using IntelliJ's BulkFileListener
  - [x] Listener registration/cleanup with Disposable lifecycle
  - [x] Registered as project service in plugin.xml
- [x] (2026-03-07) Auto-watch toggle in Diagnostics panel
  - [x] Checkbox to enable/disable auto-watch
  - [x] Auto-re-runs diagnostics when .env changes

### 3.3 Merge/Diff UI
- [x] (2026-03-07) Implement `EnvDiffDialog.kt` — side-by-side merge preview
  - [x] Shows added, removed, changed, and unchanged keys
  - [x] Summary with counts per category
  - [x] Detailed diff with existing vs incoming values
  - [x] Builds merged result (incoming wins for conflicts, existing-only keys kept)
- [x] (2026-03-07) Integrated into Azure DevOps "Apply to .env" flow
  - [x] Shows merge dialog when .env already exists
  - [x] Direct save when .env doesn't exist
- [x] (2026-03-07) Integrated into Convert tool window "Apply to .env" flow
  - [x] Shows merge dialog when target file already has entries

---

## Phase 4: Ecosystem

- [ ] AWS Parameter Store integration
- [ ] GCP Secret Manager integration
- [ ] HashiCorp Vault integration
- [ ] Publish to JetBrains Marketplace
- [ ] Documentation site

---

## Notes

_Add notes here as you work through tasks. This helps with context when resuming._

- **2026-03-07:** Project scaffolded. Gradle build, plugin.xml, source structure created.
- **2026-03-07:** Core implemented — parser (all formats), formatter (all filters + quoting), file I/O (read, write, backup, preserve). All tests pass.
- **2026-03-07:** IDE actions implemented — ConvertSelection (with preview dialog + live toggles), ConvertFile (with save picker). Registered in editor context menu, project view, and Tools menu.
- **2026-03-07:** Tool window implemented — split input/output panel with live preview, option toggles, Apply/Copy/Clear buttons, status bar. Registered as bottom panel.
- **2026-03-07:** Settings implemented — global (Settings > Tools > DotEnvify) + per-project with "Use global defaults" toggle. Persisted state, registered services.
- **2026-03-07:** Phase 2 (Azure DevOps) implemented — OAuth device code flow, token storage in Password Safe, REST API client, variable group fetch with multi-group merge, Azure tab in tool window.
- **2026-03-07:** Phase 3.1 (Diagnostics) implemented — multi-language env key scanner (JS/TS/Python/Go/Java/Kotlin/Ruby/PHP/Rust/C#/YAML), missing/unused key detection, Diagnostics tab in tool window. 19 new tests.
- **2026-03-07:** Phase 3.2 (Auto-Watch) implemented — EnvFileWatcher service monitors .env changes via VFS listener, auto-re-runs diagnostics when enabled.
- **2026-03-07:** Phase 3.3 (Merge/Diff) implemented — EnvDiffDialog shows side-by-side merge preview (added/removed/changed/unchanged), integrated into both Azure and Convert "Apply to .env" flows.
- **2026-03-07:** Removed all .env.example functionality (GenerateExampleAction, formatExample, related tests/references).
