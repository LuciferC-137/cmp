#!/bin/bash
# Script pour créer le package Linux de CMP
# Usage: ./build-linux-package.sh [deb|rpm|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/../.."

cd "$PROJECT_DIR"

# Vérifier les prérequis
echo "=== Vérification des prérequis ==="

# Java 21+
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Installez Java 21+."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ requis, version actuelle: $JAVA_VERSION"
    exit 1
fi
echo "✓ Java $JAVA_VERSION"

# jpackage (inclus dans JDK 14+)
if ! command -v jpackage &> /dev/null; then
    echo "❌ jpackage non trouvé. Assurez-vous que le JDK 21+ est correctement installé."
    exit 1
fi
echo "✓ jpackage disponible"

# Vérifier l'icône
if [ ! -f "$PROJECT_DIR/packaging/linux/cmp.png" ]; then
    echo "⚠ Icône non trouvée. Création depuis track.png..."
    cp "$PROJECT_DIR/src/main/resources/icons/track.png" "$PROJECT_DIR/packaging/linux/cmp.png" 2>/dev/null || {
        echo "❌ Impossible de créer l'icône. Créez manuellement packaging/linux/cmp.png"
        exit 1
    }
fi
echo "✓ Icône présente"

# Choix du type de package
PACKAGE_TYPE="${1:-deb}"

echo ""
echo "=== Construction du package $PACKAGE_TYPE ==="

# Nettoyer et construire
./gradlew clean

case "$PACKAGE_TYPE" in
    deb)
        # Vérifier dpkg pour DEB
        if ! command -v dpkg &> /dev/null; then
            echo "❌ dpkg non trouvé. Installez les outils Debian ou utilisez 'rpm'."
            exit 1
        fi
        ./gradlew jpackage
        ;;
    rpm)
        # Vérifier rpm pour RPM
        if ! command -v rpmbuild &> /dev/null; then
            echo "❌ rpmbuild non trouvé. Installez rpm-build."
            exit 1
        fi
        ./gradlew jpackage -PinstallerType=rpm
        ;;
    all)
        echo "Construction DEB..."
        ./gradlew jpackage
        echo "Construction RPM..."
        ./gradlew jpackage -PinstallerType=rpm
        ;;
    *)
        echo "Usage: $0 [deb|rpm|all]"
        exit 1
        ;;
esac

echo ""
echo "=== Package créé avec succès ==="
echo "Le package se trouve dans: $PROJECT_DIR/build/jpackage/"
ls -la "$PROJECT_DIR/build/jpackage/" 2>/dev/null || echo "(répertoire de sortie non trouvé)"

