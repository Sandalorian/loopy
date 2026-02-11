#!/bin/bash

echo "Building Loopy application..."

# Build the application
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦 Distribution packages created:"
    echo "   target/loopy-*-dist.tar.gz  (macOS/Linux)"
    echo "   target/loopy-*-dist.zip     (Windows/All)"
    echo ""
    echo "🚀 Quick Start (Development):"
    echo "   java -jar target/loopy-*.jar --help"
    echo ""
    echo "📥 Install from distribution:"
    echo "   tar -xzf target/loopy-*-dist.tar.gz"
    echo "   cd loopy-*/"
    echo "   export PATH=\"\$PATH:\$(pwd)/bin\""
    echo "   loopy --help"
    echo ""
    echo "📖 See docs/INSTALL.md for full installation instructions"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
    echo "❌ Build failed!"
    exit 1
fi