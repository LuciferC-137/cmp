#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ICON="${1:-$SCRIPT_DIR/../../src/main/resources/icons/app-icon.png}"
OUTPUT_DIR="$SCRIPT_DIR"

if [ ! -f "$SOURCE_ICON" ]; then
    echo "Error: icon not found: $SOURCE_ICON"
    echo "Usage: $0 [path_to_icon.png]"
    echo ""
    echo "L'icône source devrait être une image PNG de 512x512 pixels minimum."
    exit 1
fi

echo "Creating cmp.png (256x256)..."
convert "$SOURCE_ICON" -resize 256x256 "$OUTPUT_DIR/cmp.png"

mkdir -p "$OUTPUT_DIR/hicolor"
for size in 16 24 32 48 64 128 256 512; do
    mkdir -p "$OUTPUT_DIR/hicolor/${size}x${size}/apps"
    echo "Creating ${size}x${size}/apps/cmp.png..."
    convert "$SOURCE_ICON" -resize ${size}x${size} "$OUTPUT_DIR/hicolor/${size}x${size}/apps/cmp.png"
done

mkdir -p "$OUTPUT_DIR/hicolor/scalable/apps"
if [[ "$SOURCE_ICON" == *.svg ]]; then
    cp "$SOURCE_ICON" "$OUTPUT_DIR/hicolor/scalable/apps/cmp.svg"
else
    cp "$SOURCE_ICON" "$OUTPUT_DIR/hicolor/scalable/apps/cmp.png"
fi

echo ""
echo "Icon created successfully in $OUTPUT_DIR"

