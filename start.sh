#!/bin/bash
# Galactic Shipwrights - Start Script
# Compiles and runs the full system

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASEDIR"

echo "=== Galactic Shipwrights ==="
echo "Compiling..."

javac -cp "jars/*" -sourcepath src -d out src/Main.java

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

echo "Starting..."
echo "  Guild Server: port 10001 (triple-redundant)"
echo "  Pipeline: Reacher -> DictionaryProfiler -> Trainer -> Speculator"
echo ""

java -cp "out:jars/*" Main
