#!/usr/bin/env bash
# build.sh - Script de compilación local para n64-gyro-emulator
# Uso: chmod +x build.sh && ./build.sh [debug|release]
# Requiere: Linux/macOS/WSL2, JDK 17, Android SDK, Android NDK r26+, git, cmake, ninja

set -euo pipefail

# ──────────────────────────────────────────────────────────────
# CONFIGURACIÓN (ajusta si tus paths son distintos)
# ──────────────────────────────────────────────────────────────
BUILD_TYPE="${1:-release}"              # debug | release
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MUPEN_REPO="https://github.com/mupen64plus-ae/mupen64plus-ae.git"
MUPEN_DIR="${PROJECT_ROOT}/mupen64plus-ae"
MUPEN_BRANCH="master"                   # o tag estable: "v3.0.332"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
NDK_VERSION="26.1.10909125"
ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_HOME}/ndk/${NDK_VERSION}}"
GRADLE_OPTS="--no-daemon -Dorg.gradle.jvmargs=-Xmx4g"
KEYSTORE_FILE="${PROJECT_ROOT}/keystore/release.keystore"
KEYSTORE_PROPS="${PROJECT_ROOT}/keystore.properties"

# ──────────────────────────────────────────────────────────────
# FUNCIONES AUXILIARES
# ──────────────────────────────────────────────────────────────
log() { echo -e "\033[1;32m[$(date '+%H:%M:%S')]\033[0m $*"; }
err() { echo -e "\033[1;31m[ERROR]\033[0m $*" >&2; }
check_cmd() { command -v "$1" >/dev/null 2>&1 || { err "Falta '$1' en PATH"; exit 1; } }

# ──────────────────────────────────────────────────────────────
# VERIFICACIONES PREVIAS
# ──────────────────────────────────────────────────────────────
log "Verificando entorno..."
check_cmd java
check_cmd git
check_cmd cmake
check_cmd ninja

[[ -d "${ANDROID_HOME}" ]] || { err "ANDROID_HOME no existe: ${ANDROID_HOME}"; exit 1; }
[[ -d "${ANDROID_NDK_HOME}" ]] || { err "ANDROID_NDK_HOME no existe: ${ANDROID_NDK_HOME}"; exit 1; }

export ANDROID_HOME ANDROID_NDK_HOME
log "ANDROID_HOME=${ANDROID_HOME}"
log "ANDROID_NDK_HOME=${ANDROID_NDK_HOME}"

# ──────────────────────────────────────────────────────────────
# 1. CLONAR/ACTUALIZAR MUPEN64PLUS-AE COMO SUBMÓDULO
# ──────────────────────────────────────────────────────────────
log "Paso 1/6: Preparando Mupen64Plus-AE..."
if [[ ! -d "${MUPEN_DIR}/.git" ]]; then
    log "Clonando Mupen64Plus-AE (puede tardar varios minutos)..."
    git clone --depth 1 --branch "${MUPEN_BRANCH}" "${MUPEN_REPO}" "${MUPEN_DIR}"
else
    log "Actualizando Mupen64Plus-AE..."
    git -C "${MUPEN_DIR}" fetch --depth 1 origin "${MUPEN_BRANCH}"
    git -C "${MUPEN_DIR}" reset --hard "origin/${MUPEN_BRANCH}"
fi

# ──────────────────────────────────────────────────────────────
# 2. APLICAR PATCH DE GIROSCOPIO
# ──────────────────────────────────────────────────────────────
log "Paso 2/6: Aplicando patch de giroscopio..."
PATCH_FILE="${PROJECT_ROOT}/gyroscope-patch/patch.diff"
if [[ -f "${PATCH_FILE}" ]]; then
    git -C "${MUPEN_DIR}" apply --check "${PATCH_FILE}" 2>/dev/null && \
        git -C "${MUPEN_DIR}" apply "${PATCH_FILE}" && log "Patch aplicado correctamente" || \
        { err "El patch no aplica limpiamente (¿cambió upstream?). Revisa manualmente."; exit 1; }
else
    err "No se encuentra ${PATCH_FILE}"
    exit 1
fi

# ──────────────────────────────────────────────────────────────
# 3. COPIAR ARCHIVOS DE GIROSCOPIO AL PROYECTO
# ──────────────────────────────────────────────────────────────
log "Paso 3/6: Instalando archivos de giroscopio..."
GYRO_SRC="${PROJECT_ROOT}/gyroscope-patch/src"
GYRO_DST="${MUPEN_DIR}/app/src/main/java/org/mupen64plus/ae/v3"
mkdir -p "${GYRO_DST}/input"
mkdir -p "${GYRO_DST}/util"
cp "${GYRO_SRC}/input/GyroscopeInputMapper.kt" "${GYRO_DST}/input/"
cp "${GYRO_SRC}/util/RacingGameDetector.kt" "${GYRO_DST}/util/"

# ──────────────────────────────────────────────────────────────
# 4. CONFIGURAR KEYSTORE (release) O GENERAR DEBUG
# ──────────────────────────────────────────────────────────────
log "Paso 4/6: Configurando firma..."
if [[ "${BUILD_TYPE}" == "release" ]]; then
    if [[ ! -f "${KEYSTORE_FILE}" ]]; then
        log "Generando keystore de release (guárdalo en sitio seguro)..."
        mkdir -p "$(dirname "${KEYSTORE_FILE}")"
        keytool -genkeypair -v \
            -keystore "${KEYSTORE_FILE}" \
            -alias release \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass "changeit" -keypass "changeit" \
            -dname "CN=N64Gyro, OU=Dev, O=Personal, L=City, ST=State, C=ES" \
            2>/dev/null || true
        cat > "${KEYSTORE_PROPS}" <<EOF
storeFile=keystore/release.keystore
storePassword=changeit
keyAlias=release
keyPassword=changeit
EOF
        log "⚠️  KEYSTORE GENERADO AUTOMÁTICAMENTE. Cámbialo antes de subir a Play Store."
    fi
    [[ -f "${KEYSTORE_PROPS}" ]] || { err "Falta ${KEYSTORE_PROPS}"; exit 1; }
fi

# ──────────────────────────────────────────────────────────────
# 5. EJECUTAR GRADLE
# ──────────────────────────────────────────────────────────────
log "Paso 5/6: Compilando con Gradle (esto tarda 10-30 min la primera vez)..."
cd "${MUPEN_DIR}"
./gradlew ${GRADLE_OPTS} ":app:assemble${BUILD_TYPE^}"

# ──────────────────────────────────────────────────────────────
# 6. COPIAR ARTIFACTS A DIRECTORIO DE SALIDA
# ──────────────────────────────────────────────────────────────
log "Paso 6/6: Recopilando artifacts..."
OUT_DIR="${PROJECT_ROOT}/build-outputs/${BUILD_TYPE}"
mkdir -p "${OUT_DIR}"

if [[ "${BUILD_TYPE}" == "release" ]]; then
    find "${MUPEN_DIR}/app/build/outputs/bundle/release" -name "*.aab" -exec cp {} "${OUT_DIR}/" \;
    find "${MUPEN_DIR}/app/build/outputs/apk/release" -name "*.apk" -exec cp {} "${OUT_DIR}/" \;
else
    find "${MUPEN_DIR}/app/build/outputs/apk/debug" -name "*.apk" -exec cp {} "${OUT_DIR}/" \;
fi

log "✅ BUILD COMPLETADO"
log "Artifacts en: ${OUT_DIR}"
ls -lh "${OUT_DIR}"