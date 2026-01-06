#!/bin/bash
# Usage: ./build-linux-package.sh [deb|rpm|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/../.."

cd "$PROJECT_DIR"

# Prerequisites check
echo "=== Checking prerequisites ==="

# Java 21+
if ! command -v java &> /dev/null; then
    echo "❌ Java 21 not installed. Please install Java 21+."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ required, current version: $JAVA_VERSION"
    exit 1
fi
echo "✓ Java $JAVA_VERSION"

# jpackage (include in JDK 14+)
if ! command -v jpackage &> /dev/null; then
    echo "❌ jpackage not found. Ensure JDK is installed."
    exit 1
fi
echo "✓ jpackage available"

# Vérifier l'icône
if [ ! -f "$PROJECT_DIR/packaging/linux/cmp.png" ]; then
    echo "⚠ Icon not found, creating from track.png..."
    cp "$PROJECT_DIR/src/main/resources/icons/track.png" "$PROJECT_DIR/packaging/linux/cmp.png" 2>/dev/null || {
        echo "❌ Impossible to create icon. Manually add packaging/linux/cmp.png"
        exit 1
    }
fi
echo "✓ Icon found"

# Choice of package type
PACKAGE_TYPE="${1:-deb}"

echo ""
echo "=== Building package $PACKAGE_TYPE ==="

# Clean previous builds
./gradlew clean

case "$PACKAGE_TYPE" in
    deb)
        # Check dpkg for DEB
        if ! command -v dpkg &> /dev/null; then
            echo "❌ dpkg not found. Install Debian tools or ou use 'rpm'."
            exit 1
        fi
        ./gradlew jpackage
        ;;
    rpm)
        # Check rpm for RPM
        if ! command -v rpmbuild &> /dev/null; then
            echo "❌ rpmbuild not found. Install rpm-build."
            exit 1
        fi
        ./gradlew jpackage -PinstallerType=rpm
        ;;
    all)
        echo "Building DEB..."
        ./gradlew jpackage
        echo "Building RPM..."
        ./gradlew jpackage -PinstallerType=rpm
        ;;
    *)
        echo "Usage: $0 [deb|rpm|all]"
        exit 1
        ;;
esac

echo ""
echo "=== Package created successfully ==="
echo "The new package is located in: $PROJECT_DIR/build/jpackage/"
ls -la "$PROJECT_DIR/build/jpackage/" 2>/dev/null || echo "(output directory not found)"

