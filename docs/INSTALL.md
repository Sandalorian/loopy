# Loopy Installation Guide

This guide covers how to install and configure Loopy on your system.

## Prerequisites

- **Java 21 or higher** is required to run Loopy
- Download Java from:
  - [Eclipse Temurin (Adoptium)](https://adoptium.net/) - Recommended
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
  - [Amazon Corretto](https://aws.amazon.com/corretto/)

Verify your Java installation:
```bash
java -version
```

---

## Quick Start

### macOS / Linux

```bash
# Download the latest release
curl -LO https://github.com/Sandalorian/loopy/releases/latest/download/loopy-dist.tar.gz

# Extract
tar -xzf loopy-dist.tar.gz
cd loopy-*/

# Option 1: Add to PATH (recommended)
export PATH="$PATH:$(pwd)"

# Option 2: Create symlink (system-wide)
sudo ln -s "$(pwd)/loopy" /usr/local/bin/loopy

# Verify installation
loopy --version
```

### Windows

1. Download `loopy-*-dist.zip` from the [releases page](https://github.com/Sandalorian/loopy/releases)
2. Extract to your preferred location (e.g., `C:\Program Files\loopy`)
3. Add the extracted folder to your PATH:
   - Open **System Properties** → **Environment Variables**
   - Edit the **Path** variable
   - Add: `C:\Program Files\loopy\loopy-0.3.0`
4. Open a new terminal and verify:
   ```cmd
   loopy --version
   ```

---

## Installation Methods

### Method 1: Direct Download

Download the appropriate distribution package from the [releases page](https://github.com/Sandalorian/loopy/releases):

| Platform | File | Description |
|----------|------|-------------|
| macOS/Linux | `loopy-*-dist.tar.gz` | Gzipped tarball |
| Windows/All | `loopy-*-dist.zip` | ZIP archive |

### Method 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/Sandalorian/loopy.git
cd loopy

# Build with Maven
./build.sh

# The distribution will be in target/loopy-*-dist.tar.gz
```

---

## Distribution Contents

After extraction, the distribution has this structure:

```
loopy-{version}/
├── loopy                 # Unix wrapper script
├── loopy.bat             # Windows CMD script
├── loopy.ps1             # Windows PowerShell script
├── lib/
│   └── loopy-{version}.jar
├── scripts/
│   ├── loopy-completion.bash
│   ├── loopy-completion.zsh
│   ├── loopy.1
│   └── install-shell-integration.sh
├── config.properties
├── example-workload.yaml
├── readme.md
├── CHANGELOG.md
└── INSTALL.md
```

---

## PATH Configuration

### macOS / Linux

**Temporary (current session only):**
```bash
export PATH="$PATH:/path/to/loopy-0.3.0"
```

**Permanent (add to shell config):**

For **Bash** (`~/.bashrc` or `~/.bash_profile`):
```bash
echo 'export PATH="$PATH:/path/to/loopy-0.3.0"' >> ~/.bashrc
source ~/.bashrc
```

For **Zsh** (`~/.zshrc`):
```bash
echo 'export PATH="$PATH:/path/to/loopy-0.3.0"' >> ~/.zshrc
source ~/.zshrc
```

**Alternative: Symlink to /usr/local/bin:**
```bash
sudo ln -s /path/to/loopy-0.3.0/loopy /usr/local/bin/loopy
```

### Windows

**Using GUI:**
1. Press `Win + R`, type `sysdm.cpl`, press Enter
2. Click **Advanced** tab → **Environment Variables**
3. Under **User variables**, select **Path** → **Edit**
4. Click **New** and add: `C:\path\to\loopy-0.3.0`
5. Click **OK** to save

**Using PowerShell (Administrator):**
```powershell
$loopyPath = "C:\path\to\loopy-0.3.0"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$currentPath;$loopyPath", "User")
```

---

## Shell Completion

Loopy includes tab-completion scripts for Bash and Zsh.

### Automatic Installation

Run the included installer script:
```bash
./scripts/install-shell-integration.sh
```

### Manual Installation

**Bash:**
```bash
# Copy completion script
cp scripts/loopy-completion.bash ~/.loopy-completion.bash

# Add to ~/.bashrc
echo 'source ~/.loopy-completion.bash' >> ~/.bashrc
source ~/.bashrc
```

**Zsh:**
```bash
# Copy to zsh functions directory
mkdir -p ~/.zsh/completions
cp scripts/loopy-completion.zsh ~/.zsh/completions/_loopy

# Add to ~/.zshrc (if not already present)
echo 'fpath=(~/.zsh/completions $fpath)' >> ~/.zshrc
echo 'autoload -U compinit && compinit' >> ~/.zshrc
source ~/.zshrc
```

---

## Man Page

Install the man page for offline documentation:

```bash
# macOS/Linux
sudo cp scripts/loopy.1 /usr/local/share/man/man1/
sudo mandb  # Update man database (Linux only)

# View man page
man loopy
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JAVA_HOME` | Java installation directory | Auto-detected |
| `LOOPY_HOME` | Loopy installation directory | Script location |
| `LOOPY_JAVA_OPTS` | Additional JVM options | (none) |
| `LOOPY_NEO4J_URI` | Default Neo4j connection URI | `bolt://localhost:7687` |

**Example with custom JVM options:**
```bash
export LOOPY_JAVA_OPTS="-Xmx2g -XX:+UseG1GC"
loopy run --workload my-test.yaml
```

---

## Verifying Installation

After installation, verify everything is working:

```bash
# Check version
loopy --version

# View help
loopy --help

# Test Neo4j connection (requires running Neo4j instance)
loopy test-connection -u bolt://localhost:7687 -U neo4j
```

---

## Troubleshooting

### "Java not found" Error

Ensure Java 21+ is installed and in your PATH:
```bash
java -version
```

If Java is installed but not found, set `JAVA_HOME`:
```bash
export JAVA_HOME=/path/to/java
```

### "Permission denied" on Unix

Make the wrapper script executable:
```bash
chmod +x /path/to/loopy-0.3.0/loopy
```

### "PowerShell execution policy" on Windows

If PowerShell blocks the script, run:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Command not found after adding to PATH

- Ensure you've opened a **new terminal** after modifying PATH
- Verify the path is correct: `echo $PATH` (Unix) or `echo %PATH%` (Windows)

---

## Uninstalling

1. Remove the Loopy directory
2. Remove from PATH (edit shell config or Windows environment variables)
3. Remove symlink if created: `sudo rm /usr/local/bin/loopy`
4. Remove shell completions if installed
5. Remove man page if installed: `sudo rm /usr/local/share/man/man1/loopy.1`

---

## Getting Help

- **Documentation:** See [readme.md](readme.md) for usage examples
- **Man page:** `man loopy` (if installed)
- **Command help:** `loopy --help` or `loopy <command> --help`
- **Issues:** [GitHub Issues](https://github.com/Sandalorian/loopy/issues)
