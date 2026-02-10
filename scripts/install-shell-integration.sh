#!/bin/bash
# Installation script for Loopy shell integration

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPLETION_DIR=""
MAN_DIR=""
SHELL_TYPE=""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Loopy Shell Integration Installer${NC}"
echo "================================================="

# Detect shell
if [ -n "$BASH_VERSION" ]; then
    SHELL_TYPE="bash"
    echo "Detected shell: Bash"
elif [ -n "$ZSH_VERSION" ]; then
    SHELL_TYPE="zsh"
    echo "Detected shell: Zsh"
else
    echo -e "${YELLOW}⚠️  Could not detect shell type. Please specify:${NC}"
    echo "  1) Bash"
    echo "  2) Zsh"
    read -p "Enter choice (1-2): " choice
    case $choice in
        1) SHELL_TYPE="bash";;
        2) SHELL_TYPE="zsh";;
        *) echo -e "${RED}❌ Invalid choice${NC}"; exit 1;;
    esac
fi

# Determine installation directories
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    if command -v brew &> /dev/null; then
        COMPLETION_DIR="$(brew --prefix)/share/$SHELL_TYPE-completion/completions"
        MAN_DIR="$(brew --prefix)/share/man/man1"
    else
        COMPLETION_DIR="$HOME/.$SHELL_TYPE-completion/completions"
        MAN_DIR="$HOME/.local/share/man/man1"
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if [ "$SHELL_TYPE" = "bash" ]; then
        COMPLETION_DIR="/etc/bash_completion.d"
    else
        COMPLETION_DIR="/usr/share/zsh/site-functions"
    fi
    MAN_DIR="/usr/local/share/man/man1"
else
    # Default/other
    COMPLETION_DIR="$HOME/.$SHELL_TYPE-completion/completions"
    MAN_DIR="$HOME/.local/share/man/man1"
fi

echo "Installation directories:"
echo "  Completion: $COMPLETION_DIR"
echo "  Man pages:  $MAN_DIR"
echo

# Install completion scripts
echo -e "${GREEN}📝 Installing completion scripts...${NC}"

if [ ! -d "$COMPLETION_DIR" ]; then
    echo "Creating completion directory: $COMPLETION_DIR"
    mkdir -p "$COMPLETION_DIR" || {
        echo -e "${RED}❌ Failed to create completion directory${NC}"
        echo "Try running with sudo or install to user directory"
        exit 1
    }
fi

if [ "$SHELL_TYPE" = "bash" ]; then
    cp "$SCRIPT_DIR/loopy-completion.bash" "$COMPLETION_DIR/loopy" || {
        echo -e "${RED}❌ Failed to install bash completion${NC}"
        exit 1
    }
    echo -e "${GREEN}✅ Bash completion installed${NC}"
else
    cp "$SCRIPT_DIR/loopy-completion.zsh" "$COMPLETION_DIR/_loopy" || {
        echo -e "${RED}❌ Failed to install zsh completion${NC}"
        exit 1
    }
    echo -e "${GREEN}✅ Zsh completion installed${NC}"
fi

# Install man page
echo -e "${GREEN}📚 Installing man page...${NC}"

if [ ! -d "$MAN_DIR" ]; then
    echo "Creating man directory: $MAN_DIR"
    mkdir -p "$MAN_DIR" || {
        echo -e "${RED}❌ Failed to create man directory${NC}"
        echo "Try running with sudo or install to user directory"
        exit 1
    }
fi

cp "$SCRIPT_DIR/loopy.1" "$MAN_DIR/loopy.1" || {
    echo -e "${RED}❌ Failed to install man page${NC}"
    exit 1
}

echo -e "${GREEN}✅ Man page installed${NC}"

# Update man database
if command -v mandb &> /dev/null; then
    echo "Updating man database..."
    mandb -q "$MAN_DIR" 2>/dev/null || true
fi

# Shell-specific setup instructions
echo
echo -e "${GREEN}🔧 Setup Instructions${NC}"
echo "======================="

if [ "$SHELL_TYPE" = "bash" ]; then
    echo "To enable Loopy completion in Bash:"
    echo "  1. Add to your ~/.bashrc:"
    echo "     source $COMPLETION_DIR/loopy"
    echo "  2. Or reload your current shell:"
    echo "     source $COMPLETION_DIR/loopy"
    echo
    echo "To test completion:"
    echo "  loopy <TAB><TAB>"
else
    echo "To enable Loopy completion in Zsh:"
    echo "  1. Ensure the completion directory is in your fpath"
    echo "  2. Add to your ~/.zshrc (if not already present):"
    echo "     fpath=($COMPLETION_DIR \$fpath)"
    echo "     autoload -U compinit && compinit"
    echo "  3. Or reload your current shell:"
    echo "     source ~/.zshrc"
    echo
    echo "To test completion:"
    echo "  loopy <TAB>"
fi

echo
echo -e "${GREEN}📖 Documentation${NC}"
echo "================="
echo "View the man page:"
echo "  man loopy"
echo
echo "Quick command reference:"
echo "  loopy --help"
echo "  loopy COMMAND --help"

echo
echo -e "${GREEN}🎉 Installation completed successfully!${NC}"
echo
echo "Quick start:"
echo "  loopy setup          # Interactive configuration wizard"
echo "  loopy test-connection # Test your Neo4j connection"
echo "  loopy run            # Run a load test"
echo
echo "Enjoy using Loopy! 🚀"