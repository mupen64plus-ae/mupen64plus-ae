#!/usr/bin/env bash
# gen-keystore.sh - Genera keystore de release y keystore.properties
# Uso: ./scripts/gen-keystore.sh [alias] [storepass] [keypass] [validity_days]

set -euo pipefail

ALIAS="${1:-release}"
STOREPASS="${2:-$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)}"
KEYPASS="${3:-$STOREPASS}"
VALIDITY="${4:-10000}"
OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/keystore"
KEYSTORE_FILE="$OUT_DIR/release.keystore"
PROPS_FILE="$OUT_DIR/keystore.properties"

mkdir -p "$OUT_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
    echo "⚠️  Keystore ya existe en $KEYSTORE_FILE"
    read -p "¿Sobrescribir? (y/N): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]] || exit 1
fi

echo "Generando keystore..."
keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity "$VALIDITY" \
    -storepass "$STOREPASS" -keypass "$KEYPASS" \
    -dname "CN=N64Gyro, OU=Dev, O=Personal, L=City, ST=State, C=ES"

cat > "$PROPS_FILE" <<EOF
storeFile=keystore/release.keystore
storePassword=$STOREPASS
keyAlias=$ALIAS
keyPassword=$KEYPASS
EOF

chmod 600 "$PROPS_FILE"

echo ""
echo "✅ Keystore generado:"
echo "   Archivo: $KEYSTORE_FILE"
echo "   Alias: $ALIAS"
echo "   Store pass: $STOREPASS"
echo "   Key pass: $KEYPASS"
echo ""
echo "📋 Para GitHub Secrets (copia estos valores):"
echo "   KEYSTORE_BASE64: $(base64 -w0 "$KEYSTORE_FILE")"
echo "   KEYSTORE_PASSWORD: $STOREPASS"
echo "   KEY_ALIAS: $ALIAS"
echo "   KEY_PASSWORD: $KEYPASS"
echo ""
echo "⚠️  GUARDA ESTAS CONTRASEÑAS EN SITIO SEGURO. No se pueden recuperar."
echo "⚠️  NO subas keystore/ a git (está en .gitignore)"