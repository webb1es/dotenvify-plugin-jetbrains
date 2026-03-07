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
- [ ] Verify plugin builds successfully (`./gradlew buildPlugin`)
- [ ] Verify plugin runs in sandbox IDE (`./gradlew runIde`)

### 1.2 Core Parser
- [ ] Create `DotEnvModels.kt` — data classes (EnvEntry, ParseResult, FormatOptions)
- [ ] Implement `DotEnvParser.kt` — multi-format parser
  - [ ] Parse `KEY=VALUE` format
  - [ ] Parse `KEY="VALUE"` quoted format
  - [ ] Parse `KEY VALUE` space-separated format
  - [ ] Parse line-pair format (KEY on line 1, VALUE on line 2)
  - [ ] Handle mixed formats in single input
  - [ ] Skip comments (`#` lines) and blank lines
  - [ ] Handle edge cases (empty values, `=` in values, special characters)
- [ ] Write unit tests for parser (aim for full coverage of all formats)

### 1.3 Core Formatter
- [ ] Implement `DotEnvFormatter.kt` — output generation
  - [ ] Basic `KEY="VALUE"` output
  - [ ] Optional `export` prefix
  - [ ] Alphabetical sorting (default on)
  - [ ] Smart quoting (URLs, values with spaces, already-quoted)
  - [ ] Filter: ignore lowercase keys
  - [ ] Filter: URL-only values
  - [ ] Preserve specific keys from existing `.env`
- [ ] Write unit tests for formatter

### 1.4 File I/O
- [ ] Implement `DotEnvIO.kt`
  - [ ] Read existing `.env` file into EnvEntry list
  - [ ] Write formatted output to file
  - [ ] Incremental backup logic (`.backup.1`, `.backup.2`, ...)
  - [ ] Overwrite mode (skip backup)
- [ ] Write unit tests for file I/O

### 1.5 IDE Actions
- [ ] Implement `ConvertSelectionAction.kt`
  - [ ] Get selected text from editor
  - [ ] Parse and format
  - [ ] Show preview dialog
  - [ ] Write to `.env` or replace selection
- [ ] Implement `ConvertFileAction.kt`
  - [ ] Read source file
  - [ ] Parse and format
  - [ ] Show preview dialog
  - [ ] Write to `.env`
- [ ] Implement `GenerateExampleAction.kt`
  - [ ] Read `.env` file
  - [ ] Strip values, keep keys
  - [ ] Write `.env.example`
- [ ] Register actions in plugin.xml
- [ ] Add keyboard shortcuts

### 1.6 Tool Window
- [ ] Implement `DotEnvifyToolWindowFactory.kt`
- [ ] Implement `DotEnvifyToolWindow.kt`
  - [ ] Input text area
  - [ ] Output preview panel (live preview as you type/toggle)
  - [ ] Toggle checkboxes: export, sort, no-lower, url-only
  - [ ] Preserve keys input field
  - [ ] "Apply to .env" button
  - [ ] "Copy to Clipboard" button
  - [ ] Output file path selector
- [ ] Register tool window in plugin.xml

### 1.7 Settings
- [ ] Implement `DotEnvifySettings.kt` — global settings state
- [ ] Implement `DotEnvifyProjectSettings.kt` — per-project settings
- [ ] Implement `DotEnvifySettingsConfigurable.kt` — settings UI panel
  - [ ] Export prefix toggle
  - [ ] Sort toggle
  - [ ] No-lower toggle
  - [ ] URL-only toggle
  - [ ] Default output path
  - [ ] Preserve keys list
  - [ ] "Use global defaults" toggle (project settings)
- [ ] Register settings in plugin.xml

### 1.8 Testing & Release Prep
- [ ] Manual testing in IntelliJ IDEA sandbox
- [ ] Manual testing in GoLand sandbox
- [ ] Fix any compatibility issues
- [ ] Write README.md with usage instructions
- [ ] Create plugin icon (40x40 SVG)
- [ ] Prepare for JetBrains Marketplace submission

---

## Phase 2: Azure DevOps Integration

### 2.1 OAuth Authentication
- [ ] Implement `AuthProvider` interface
- [ ] Implement Device Code Flow
- [ ] Implement Browser-based OAuth (optional)
- [ ] Token storage in JetBrains Password Safe
- [ ] Token refresh logic
- [ ] Sign-out flow

### 2.2 Variable Group Fetch
- [ ] Implement `AzureDevOpsClient.kt` — REST API client
- [ ] List organizations
- [ ] List projects
- [ ] List variable groups
- [ ] Fetch variables from group(s)
- [ ] Multi-group merge with precedence
- [ ] Duplicate key detection and warnings

### 2.3 Azure UX
- [ ] "Sign in to Azure DevOps" button
- [ ] Org/project/group picker UI
- [ ] Auth status indicator
- [ ] Merge preview panel
- [ ] Error handling and user feedback

---

## Phase 3: Intelligence & Polish

- [ ] Missing key diagnostics (referenced in code but not in `.env`)
- [ ] Unused key diagnostics (in `.env` but not referenced)
- [ ] Auto-watch mode for source files
- [ ] `.env.example` auto-sync
- [ ] Side-by-side merge/diff UI

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
