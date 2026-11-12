#!/usr/bin/env bash
# test-gyro-on-device.sh - Test rápido de giroscopio en dispositivo conectado
# Uso: ./scripts/test-gyro-on-device.sh

set -euo pipefail

ADB="${ADB:-adb}"

echo "=== Test de giroscopio en dispositivo ==="
echo ""

# 1. Verificar dispositivo
DEVICES=$($ADB devices | grep -v "List" | grep "device$" | wc -l)
if [[ $DEVICES -eq 0 ]]; then
    echo "❌ No hay dispositivos conectados. Conecta tu móvil por USB (depuración USB ON)."
    exit 1
fi
echo "✅ Dispositivo(s) conectado(s): $DEVICES"

# 2. Verificar sensores
echo ""
echo "Sensores disponibles:"
$ADB shell dumpsys sensorservice | grep -A2 "Sensor {" | grep -E "(name|vendor)" | head -20

# 3. Test giroscopio en tiempo real (5 segundos)
echo ""
echo "Leyendo giroscopio 5 segundos (mueve el teléfono)..."
$ADB shell dumpsys sensorservice -s gyroscope 2>/dev/null | head -30 || \
    $ADB shell "while true; do cat /sys/bus/iio/devices/iio:device*/in_anglvel_*_raw 2>/dev/null | head -3; sleep 0.1; done" | head -50

# 4. Verificar app instalada
echo ""
echo "Verificando app instalada..."
if $ADB shell pm list packages | grep -q "org.mupen64plus.ae.v3"; then
    echo "✅ App instalada"
    # Lanzar actividad principal para ver logs
    echo "Lanzando app para ver logs de giroscopio (Ctrl+C para parar)..."
    $ADB logcat -c
    $ADB shell am start -n org.mupen64plus.ae.v3/.GameActivity
    sleep 2
    $ADB logcat -s GyroDetector GyroscopeInputMapper
else
    echo "⚠️  App NO instalada. Instala primero: adb install build-outputs/debug/app-debug.apk"
fi