# DotEnvify — JetBrains Plugin

Convert raw key-value pairs into properly formatted `.env` files — directly in your IDE.

A native JetBrains plugin port of the [dotenvify CLI](https://github.com/webb1es/dotenvify).
Works across IntelliJ IDEA, GoLand, WebStorm, PyCharm, Rider, CLion, and more.

## Features

- **Multi-format parsing** — `KEY=VALUE`, `KEY="VALUE"`, `KEY VALUE`, line pairs, mixed formats
- **Smart quoting** — automatically quotes URLs and values with spaces
- **Export prefix** — optional `export` keyword on each line
- **Alphabetical sorting** — on by default, toggle off
- **Filters** — ignore lowercase keys, URL-only values
- **Preserve keys** — keep existing values for specified keys when overwriting
- **Incremental backups** — `.env.backup.1`, `.env.backup.2`, etc.
- **Generate .env.example** — strip values, keep keys
- **Live preview** — see formatted output as you type

## Usage

### Tool Window

1. Open the **DotEnvify** tool window (bottom panel)
2. Paste raw key-value data into the **Input** area
3. Toggle options (export, sort, ignore lowercase, URL-only)
4. Preview updates live in the **Output** area
5. Click **"Apply to .env"** to save, or **"Copy to Clipboard"**

### Editor Actions

- **Convert Selection** — select text, right-click > DotEnvify > Convert Selection to .env
  - Keyboard shortcut: `Ctrl+Alt+E`
- **Convert File** — right-click a file in Project view > DotEnvify > Convert File to .env
- **Generate .env.example** — right-click a `.env` file > DotEnvify > Generate .env.example

All actions are also available under the **Tools > DotEnvify** menu.

### Settings

- **Global defaults**: Settings > Tools > DotEnvify
- **Per-project overrides**: Settings > Tools > DotEnvify > Project Settings
  - Toggle "Use global defaults" to switch between global and project-specific options

## Supported Input Formats

```
# Standard KEY=VALUE
API_KEY=abc123

# Quoted values
DATABASE_URL="postgres://localhost:5432/db"

# Space-separated
SECRET_TOKEN mytoken123

# Line pairs (key on one line, value on next)
REDIS_HOST
redis.example.com

# export prefix (stripped during parsing)
export NODE_ENV=production
```

All formats can be mixed in a single input.

## Building from Source

```bash
# Build the plugin
./gradlew buildPlugin

# Run in a sandbox IDE
./gradlew runIde

# Run tests
./gradlew test
```

Requires JDK 17+.

## License

MIT
