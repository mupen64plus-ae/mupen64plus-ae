#!/usr/bin/env bash
# verify-env.sh - Verifica que el entorno está listo para compilar
# Uso: ./scripts/verify-env.sh

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok() { echo -e "${GREEN}✓${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; ERRORS=$((ERRORS+1)); }

ERRORS=0

echo "=== Verificando entorno de compilación ==="
echo ""

# 1. OS
echo "1. Sistema operativo:"
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    ok "Linux detectado"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    ok "macOS detectado"
elif [[ -n "${WSL_DISTRO_NAME:-}" ]]; then
    ok "WSL2 detectado ($WSL_DISTRO_NAME)"
else
    warn "Windows nativo detectado — NO SOPORTADO. Usa WSL2."
    ERRORS=$((ERRORS+1))
fi

# 2. Java
echo ""
echo "2. Java:"
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    ok "java encontrado: $JAVA_VER"
    if [[ "$JAVA_VER" =~ ^17\. ]]; then
        ok "Versión 17 (recomendada)"
    else
        warn "Versión no es 17 (puede funcionar pero no garantizado)"
    fi
else
    fail "java NO encontrado. Instala openjdk-17-jdk"
fi

# 3. Android SDK
echo ""
echo "3. Android SDK:"
if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
    ok "ANDROID_HOME=$ANDROID_HOME"
    if [[ -d "$ANDROID_HOME/platforms/android-34" ]]; then
        ok "API 34 instalada"
    else
        warn "API 34 NO encontrada (instala desde SDK Manager)"
    fi
    if [[ -d "$ANDROID_HOME/build-tools/34.0.0" ]] || ls "$ANDROID_HOME"/build-tools/34.* >/dev/null 2>&1; then
        ok "Build Tools 34 instaladas"
    else
        warn "Build Tools 34 NO encontradas"
    fi
else
    fail "ANDROID_HOME no definido o no existe. Exporta: export ANDROID_HOME=~/Android/Sdk"
fi

# 4. Android NDK
echo ""
echo "4. Android NDK:"
NDK_VER="26.1.10909125"
if [[ -n "${ANDROID_NDK_HOME:-}" && -d "$ANDROID_NDK_HOME" ]]; then
    ok "ANDROID_NDK_HOME=$ANDROID_NDK_HOME"
elif [[ -d "$ANDROID_HOME/ndk/$NDK_VER" ]]; then
    ok "NDK $NDK_VER encontrado en \$ANDROID_HOME/ndk/"
    export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$NDK_VER"
else
    fail "NDK $NDK_VER NO encontrado. Instala desde SDK Manager"
fi

# 5. Herramientas de build
echo ""
echo "5. Herramientas:"
for cmd in git cmake ninja; do
    if command -v "$cmd" >/dev/null 2>&1; then
        VER=$($cmd --version | head -1)
        ok "$cmd: $VER"
    else
        fail "$cmd NO encontrado. Instala: sudo apt install $cmd"
    fi
done

# 6. local.properties
echo ""
echo "6. Configuración local:"
if [[ -f "local.properties" ]]; then
    ok "local.properties existe"
    grep -q "^sdk.dir=" local.properties && ok "  sdk.dir configurado" || warn "  sdk.dir NO configurado"
    grep -q "^ndk.dir=" local.properties && ok "  ndk.dir configurado" || warn "  ndk.dir NO configurado"
else
    warn "local.properties NO existe. Copia local.properties.example y edítalo"
fi

# 7. Keystore (solo para release)
echo ""
echo "7. Keystore (release):"
if [[ -f "keystore/release.keystore" && -f "keystore.properties" ]]; then
    ok "Keystore y properties encontrados"
else
    warn "Keystore NO configurado (necesario para build release). Ejecuta: ./scripts/gen-keystore.sh"
fi

# Resumen
echo ""
echo "=== RESUMEN ==="
if [[ $ERRORS -eq 0 ]]; then
    ok "Entorno LISTO para compilar. Ejecuta: ./build.sh debug"
    exit 0
else
    fail "$ERRORS error(es) crítico(s). Corrige antes de compilar."
    exit 1
fi