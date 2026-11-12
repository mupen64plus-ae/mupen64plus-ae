# Changelog

Todos los cambios notables se documentan aquí.
Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).

## [Unreleased]

### Added
- Integración de giroscopio para juegos de carreras N64
- `GyroscopeInputMapper`: Mapea inclinación teléfono → joystick virtual
- `RacingGameDetector`: Detección automática por CRC32 + keywords
- Patch aplicable contra Mupen64Plus-AE upstream
- Build script local (`build.sh`) + GitHub Actions CI/CD (`.github/workflows/build.yml`)
- Settings UI: toggle giroscopio + slider sensibilidad
- Scripts auxiliares: `verify-env.sh`, `gen-keystore.sh`, `test-gyro-on-device.sh`

### Changed
- Basado en Mupen64Plus-AE v3.0.332 (master branch)

### Fixed
- N/A

## [1.0.0] - 2026-XX-XX

### Added
- Primera versión pública con giroscopio
- Soporte para 25+ juegos de carreras detectados
- Compilación automatizada en GitHub Actions
- Documentación completa en README.md

---

### Plantilla para futuras versiones

## [X.Y.Z] - YYYY-MM-DD

### Added
- Nueva funcionalidad

### Changed
- Cambio en funcionalidad existente

### Deprecated
- Funcionalidad que se eliminará

### Removed
- Funcionalidad eliminada

### Fixed
- Corrección de bugs

### Security
- Correcciones de seguridad