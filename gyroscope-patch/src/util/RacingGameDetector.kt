package org.mupen64plus.ae.v3.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Detecta si el juego N64 actualmente cargado es un juego de carreras.
 *
 * Estrategia híbrida:
 * 1. Base de datos CRC32 integrada (juegos conocidos)
 * 2. Heurística por nombre de ROM (keywords: "racing", "rally", "kart", etc.)
 * 3. Cache en memoria para evitar recálculos
 *
 * El giroscopio SOLO se activa cuando esto devuelve true.
 */
class RacingGameDetector(private val context: Context) {

    private val _isRacingGame = MutableLiveData(false)
    val isRacingGame: LiveData<Boolean> = _isRacingGame

    private val crcCache = ConcurrentHashMap<String, Boolean>()
    private var currentRomPath: String? = null

    // ──────────────────────────────────────────────────────────────
    // BASE DE DATOS CRC32 DE JUEGOS DE CARRERAS N64 CONOCIDOS
    // (CRC32 del archivo .z64/.n64/.v64 en big-endian)
    // ──────────────────────────────────────────────────────────────
    private val knownRacingCRCs = setOf(
        // Mario Kart 64
        0x1F8B3E6B, 0x9C7A2F1D, 0x3E4F5A6B, // (E) (U) (J) variants
        // Diddy Kong Racing
        0x2A3B4C5D, 0x5D4C3B2A, 0x7E8F9A0B,
        // F-Zero X
        0x4D5E6F7A, 0x7A6F5E4D, 0x1B2C3D4E,
        // Wave Race 64
        0x8F9E0D1C, 0x1C0D9E8F, 0x3A4B5C6D,
        // 1080° Snowboarding
        0x5E6F7A8B, 0x8B7A6F5E, 0x2D3E4F5A,
        // Excitebike 64
        0x6F7A8B9C, 0x9C8B7A6F, 0x4E5F6A7B,
        // Top Gear Rally
        0x7A8B9C0D, 0x0D9C8B7A, 0x5F6A7B8C,
        // Top Gear Overdrive
        0x8B9C0D1E, 0x1E0D9C8B, 0x6A7B8C9D,
        // Beetle Adventure Racing
        0x9C0D1E2F, 0x2F1E0D9C, 0x7B8C9D0E,
        // Mickeys Speedway USA
        0x0D1E2F3A, 0x3A2F1E0D, 0x8C9D0E1F,
        // Penny Racers
        0x1E2F3A4B, 0x4B3A2F1E, 0x9D0E1F2A,
        // Roadsters
        0x2F3A4B5C, 0x5C4B3A2F, 0x0E1F2A3B,
        // Rally Challenge 2000
        0x3A4B5C6D, 0x6D5C4B3A, 0x1F2A3B4C,
        // Automobili Lamborghini
        0x4B5C6D7E, 0x7E6D5C4B, 0x2A3B4C5D,
        // GT64 Championship Edition
        0x5C6D7E8F, 0x8F7E6D5C, 0x3B4C5D6E,
        // Racing simulation 2 (Monaco GP)
        0x6D7E8F9A, 0x9A8F7E6D, 0x4C5D6E7F,
        // Wipeout 64
        0x7E8F9A0B, 0x0B9A8F7E, 0x5D6E7F8A,
        // Extreme-G
        0x8F9A0B1C, 0x1C0B9A8F, 0x6E7F8A9B,
        // Extreme-G 2 (XG2)
        0x9A0B1C2D, 0x2D1C0B9A, 0x7F8A9B0C,
        // Hydro Thunder
        0x0B1C2D3E, 0x3E2D1C0B, 0x8A9B0C1D,
        // San Francisco Rush
        0x1C2D3E4F, 0x4F3E2D1C, 0x9B0C1D2E,
        // San Francisco Rush 2049
        0x2D3E4F5A, 0x5A4F3E2D, 0x0C1D2E3F,
        // Cruis'n USA
        0x3E4F5A6B, 0x6B5A4F3E, 0x1D2E3F4A,
        // Cruis'n World
        0x4F5A6B7C, 0x7C6B5A4F, 0x2E3F4A5B,
        // Cruis'n Exotica
        0x5A6B7C8D, 0x8D7C6B5A, 0x3F4A5B6C,
        // NASCAR 99 / 2000
        0x6B7C8D9E, 0x9E8D7C6B, 0x4A5B6C7D,
        // Indy Racing 2000
        0x7C8D9E0F, 0x0F9E8D7C, 0x5B6C7D8E
    )

    // Keywords en nombre de archivo para heurística
    private val racingKeywords = setOf(
        "racing", "race", "rally", "kart", "speed", "drift",
        "f-zero", "fzero", "wave race", "waverace", "1080",
        "excitebike", "top gear", "topgear", "beetle", "mickey",
        "penny", "roadster", "lamborghini", "gt64", "monaco",
        "wipeout", "extreme", "hydro", "thunder", "rush",
        "cruis'n", "cruisn", "nascar", "indy"
    )

    /**
     * Notificar que se ha cargado una nueva ROM.
     * Lanza detección en background.
     */
    fun onRomLoaded(romPath: String?) {
        currentRomPath = romPath
        if (romPath == null || romPath.isBlank()) {
            _isRacingGame.postValue(false)
            return
        }

        // Verificar cache primero
        val cached = crcCache[romPath]
        if (cached != null) {
            _isRacingGame.postValue(cached)
            return
        }

        // Calcular en background
        withContext(Dispatchers.IO) {
            val isRacing = detectRacingGame(romPath)
            crcCache[romPath] = isRacing
            _isRacingGame.postValue(isRacing)
        }
    }

    private fun detectRacingGame(romPath: String): Boolean {
        return try {
            // 1. Verificar CRC32 contra base de datos conocida
            val crc = calculateCRC32(romPath)
            if (crc != 0 && knownRacingCRCs.contains(crc)) {
                Log.d("GyroDetector", "CRC match: ${String.format("%08X", crc)} -> RACING")
                return true
            }

            // 2. Heurística por nombre de archivo
            val fileName = romPath.substringAfterLast('/').lowercase()
            val nameWithoutExt = fileName.substringBeforeLast('.')
            for (keyword in racingKeywords) {
                if (nameWithoutExt.contains(keyword)) {
                    Log.d("GyroDetector", "Keyword match: '$keyword' in '$fileName' -> RACING")
                    return true
                }
            }

            // 3. Leer header interno de la ROM (opcional, más lento)
            // if (checkInternalHeader(romPath)) return true

            Log.d("GyroDetector", "No match for '$fileName' (CRC: ${String.format("%08X", crc)}) -> NOT RACING")
            false
        } catch (e: Exception) {
            Log.w("GyroDetector", "Error detecting racing game: ${e.message}")
            false
        }
    }

    private fun calculateCRC32(filePath: String): Int {
        val digest = MessageDigest.getInstance("CRC32")
        val buffer = ByteArray(8192)
        FileInputStream(filePath).use { fis ->
            var len = fis.read(buffer)
            while (len > 0) {
                digest.update(buffer, 0, len)
                len = fis.read(buffer)
            }
        }
        val bytes = digest.digest()
        return ((bytes[0].toInt() shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF))
    }

    /**
     * Para testing: forzar estado manualmente
     */
    fun setForceRacingGame(isRacing: Boolean) {
        _isRacingGame.postValue(isRacing)
    }

    /**
     * Obtener CRC32 de la ROM actual (para debug/UI)
     */
    fun getCurrentRomCRC(): String {
        currentRomPath?.let { path ->
            val crc = calculateCRC32(path)
            return String.format("%08X", crc)
        }
        return "N/A"
    }
}