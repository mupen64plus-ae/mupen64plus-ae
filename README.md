# N64 Gyro Emulator

Emulador de Nintendo 64 para Android con **control por giroscopio** para juegos de carreras.
Basado en [Mupen64Plus-AE](https://github.com/mupen64plus-ae/mupen64plus-ae) (GPL v3).

## ⚖️ LICENCIA IMPORTANTE

**Este proyecto usa GPL v3.** Al compilar y distribuir:
- ✅ Puedes subir a Play Store y cobrar/mostrar ads
- ✅ Puedes modificar el código
- ❌ **DEBES publicar TODO tu código fuente** bajo GPL v3
- ❌ **NO puedes impedir que otros clonen tu app** y la suban gratis
- ❌ **NO puedes cerrar el código** ni añadir restricciones extra

Ver [LICENSE](LICENSE) y [Mupen64Plus-AE license](https://github.com/mupen64plus-ae/mupen64plus-ae/blob/master/gpl-license).

---

## 🎮 Feature: Giroscopio para Carreras

| Juego | Control |
|-------|---------|
| Mario Kart 64 | Inclina teléfono ←/→ para girar |
| F-Zero X | Inclina teléfono ←/→ para girar |
| Wave Race 64 | Inclina teléfono ←/→ para girar |
| Diddy Kong Racing | Inclina teléfono ←/→ para girar |
| 1080° Snowboarding | Inclina teléfono ←/→ para girar |
| ... y 20+ más | Detección automática por CRC/nombre |

**Solo activa en juegos de carreras.** En el resto: controles normales (touchscreen/mando).

---

## 📋 Requisitos Previos

### Sistema Operativo
- **Linux** (Ubuntu 22.04+, Fedora, Arch, etc.)
- **macOS** (Intel o Apple Silicon)
- **Windows**: Solo via **WSL2** (no Windows nativo)

### Software Necesario
```bash
# Ubuntu/Debian/WSL2
sudo apt update && sudo apt install -y \
    openjdk-17-jdk \
    android-sdk \
    android-ndk \
    git \
    cmake \
    ninja-build \
    unzip

# macOS (Homebrew)
brew install openjdk@17 android-sdk android-ndk git cmake ninja

# Variables de entorno (añade a ~/.bashrc o ~/.zshrc)
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.1.10909125
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

### Android SDK/NDK
1. Instala Android Studio → SDK Manager
2. SDK Platform: **Android 34** (API 34) + **Android 7.0** (API 24)
3. NDK: **26.1.10909125** (o compatible)
4. CMake: **3.22.1+**
5. Build Tools: **34.0.0+**

---

## 🚀 Compilación Local

### 1. Clonar y preparar
```bash
git clone https://github.com/TU_USUARIO/n64-gyro-emulator.git
cd n64-gyro-emulator

# Copia y edita local.properties con tus paths
cp local.properties.example local.properties
nano local.properties  # Ajusta sdk.dir y ndk.dir
```

### 2. Compilar
```bash
chmod +x build.sh

# Debug (rápido, para testear)
./build.sh debug

# Release (firmado, para Play Store)
./build.sh release
```

### 3. Resultado
```
build-outputs/debug/app-debug.apk          # Para instalar en tu móvil
build-outputs/release/app-release.aab      # Para subir a Play Console
build-outputs/release/app-release.apk      # APK firmado (opcional)
```

### 4. Instalar en móvil
```bash
# Opción A: ADB
adb install build-outputs/debug/app-debug.apk

# Opción B: Transfiere el .apk al móvil y abre desde gestor de archivos
```

---

## ☁️ Compilación en GitHub Actions (Recomendado)

**No necesitas instalar nada en local.** GitHub compila por ti.

### 1. Subir a GitHub
```bash
# Crea repo vacío en GitHub (sin README, sin .gitignore, sin license)
git init
git add .
git commit -m "Initial commit: N64 Gyro Emulator"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/n64-gyro-emulator.git
git push -u origin main
```

### 2. Configurar Secrets (para firmar release)
En GitHub: **Settings → Secrets and variables → Actions → New repository secret**

| Name | Value |
|------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 < tu-keystore.jks` |
| `KEYSTORE_PASSWORD` | contraseña del keystore |
| `KEY_ALIAS` | alias de la clave (ej: `release`) |
| `KEY_PASSWORD` | contraseña de la clave |

> **Generar keystore si no tienes:**
> ```bash
> keytool -genkeypair -v -keystore release.keystore -alias release \
>   -keyalg RSA -keysize 2048 -validity 10000 \
>   -storepass TU_PASS -keypass TU_PASS \
>   -dname "CN=TuNombre, OU=Dev, O=Personal, L=Ciudad, ST=Estado, C=ES"
> # Luego: base64 -w0 release.keystore  → copia a KEYSTORE_BASE64
> ```

### 3. Build Automático
- **Cada push a main/master**: Compila debug + release, sube artifacts (30 días)
- **Cada tag `v*` (ej: `v1.0.0`)**: Crea **GitHub Release** con AAB/APK listos para Play Store

### 4. Descargar artifacts
- Pestaña **Actions** → Click en build → **Artifacts** (abajo a la derecha)
- O en **Releases** si usaste tags

---

## 📱 Cómo Usar la App

1. **Abre la app** → Permite almacenamiento (para ROMs)
2. **Settings → Input → Giroscopio (Juegos de Carreras)** → Actívalo
3. **Ajusta Sensibilidad** (15 default, más alto = menos inclinación)
4. **Carga una ROM** de carreras (Mario Kart, F-Zero, Wave Race, etc.)
5. **Juega inclinando el teléfono** ←/→ para girar

> **Nota**: El joystick virtual se oculta automáticamente en juegos de carreras detectados.

---

## 🔧 Estructura del Proyecto

```
n64-gyro-emulator/
├── build.sh                          # Script principal de build local
├── .github/workflows/build.yml       # GitHub Actions CI/CD
├── gyroscope-patch/
│   ├── patch.diff                    # Patch contra Mupen64Plus-AE
│   └── src/
│       ├── input/GyroscopeInputMapper.kt    # Lógica giroscopio → joystick
│       └── util/RacingGameDetector.kt       # Detección juegos carreras (CRC/keywords)
├── gradle.properties                 # Configuración Gradle/NDK/Kotlin
├── local.properties.example          # Template de paths locales
├── LICENSE                           # GPL v3
└── README.md                         # Este archivo
```

---

## 🐛 Solución de Problemas

### "SDK/NDK not found"
```bash
# Verifica paths en local.properties
cat local.properties
# Debe apuntar a directorios REALES existentes
```

### "Patch does not apply"
```bash
# Mupen64Plus-AE upstream cambió. Opciones:
cd mupen64plus-ae
git status                    # Ver qué cambió
git apply --reject gyroscope-patch/patch.diff  # Aplica a trozos, revisa .rej
# O: actualiza patch.diff manualmente
```

### "Gradle out of memory"
```bash
# En gradle.properties, aumenta:
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g
# O en build.sh: GRADLE_OPTS="--no-daemon -Dorg.gradle.jvmargs=-Xmx6g"
```

### "Keystore corrupt / password incorrect"
- Revisa secrets en GitHub (sin espacios extra)
- En local: borra keystore/ y vuelve a generar

### La app crashea al abrir
- `adb logcat | grep -i mupen` para ver error
- Común: falta permiso almacenamiento, ROM corrupta, NDK mismatch

### Giroscopio no funciona
- Verifica que el juego está en la lista CRC (RacingGameDetector.kt)
- Settings → Input → Giroscopio: asegúrate que está ON
- Algunos móviles requieren calibrar: Settings → Sensores → Calibrar

---

## 📦 Publicar en Play Store

1. **Play Console** → Crear app → Completar ficha (icono 512x512, screenshots, política privacidad)
2. **Testing** → Internal testing → Subir `app-release.aab`
3. **Revisión** → Enviar a revisión (1-7 días)
4. **Producción** → Publicar

> **Importante**: Al ser GPL v3, debes ofrecer código fuente. Añade en la descripción:
> > "Código fuente disponible en: https://github.com/TU_USUARIO/n64-gyro-emulator (GPL v3)"

---

## 🤝 Contribuir

1. Fork → Rama `feature/nueva-cosa`
2. Commits claros (`feat:`, `fix:`, `chore:`)
3. PR con descripción de qué cambia y por qué
4. CI debe pasar (lint + build)

---

## 📚 Recursos Útiles

- [Mupen64Plus-AE](https://github.com/mupen64plus-ae/mupen64plus-ae) - Base upstream
- [Mupen64Plus Core](https://github.com/mupen64plus/mupen64plus-core) - Core C
- [Android NDK Guides](https://developer.android.com/ndk/guides)
- [GPL v3 FAQ](https://www.gnu.org/licenses/gpl-faq.html)
- [Play Console Help](https://support.google.com/googleplay/android-developer)

---

## ⚠️ Descargo de Responsabilidad

- **No se incluyen ROMs**. Necesitas volcar tus propios cartuchos (legal en la mayoría de países para uso personal).
- **Nintendo no patrocina ni avala** este proyecto.
- **Usa bajo tu propio riesgo**. Sin garantía de ningún tipo (ver GPL v3 §15-16).
- El giroscopio es experimental; puede no funcionar en todos los juegos/móviles.

---

**¿Dudas? Abre un Issue en GitHub.** ¡Diviértete desarrollando!