#!/bin/bash
# Script pour créer les icônes de l'application à différentes tailles
# Nécessite: imagemagick (sudo apt install imagemagick)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ICON="${1:-$SCRIPT_DIR/../../src/main/resources/icons/app-icon.png}"
OUTPUT_DIR="$SCRIPT_DIR"

# Vérifier si le fichier source existe
if [ ! -f "$SOURCE_ICON" ]; then
    echo "Erreur: Icône source non trouvée: $SOURCE_ICON"
    echo "Usage: $0 [chemin_vers_icone_source.png]"
    echo ""
    echo "L'icône source devrait être une image PNG de 512x512 pixels minimum."
    exit 1
fi

# Créer l'icône principale pour jpackage (256x256 minimum recommandé)
echo "Création de cmp.png (256x256)..."
convert "$SOURCE_ICON" -resize 256x256 "$OUTPUT_DIR/cmp.png"

# Créer les différentes tailles pour le thème d'icônes hicolor
mkdir -p "$OUTPUT_DIR/hicolor"
for size in 16 24 32 48 64 128 256 512; do
    mkdir -p "$OUTPUT_DIR/hicolor/${size}x${size}/apps"
    echo "Création de ${size}x${size}/apps/cmp.png..."
    convert "$SOURCE_ICON" -resize ${size}x${size} "$OUTPUT_DIR/hicolor/${size}x${size}/apps/cmp.png"
done

# Créer une version scalable (copie de l'original si SVG, sinon PNG haute résolution)
mkdir -p "$OUTPUT_DIR/hicolor/scalable/apps"
if [[ "$SOURCE_ICON" == *.svg ]]; then
    cp "$SOURCE_ICON" "$OUTPUT_DIR/hicolor/scalable/apps/cmp.svg"
else
    cp "$SOURCE_ICON" "$OUTPUT_DIR/hicolor/scalable/apps/cmp.png"
fi

echo ""
echo "Icônes créées avec succès dans $OUTPUT_DIR"
echo "N'oubliez pas de créer une icône source de haute qualité (512x512 recommandé)"

