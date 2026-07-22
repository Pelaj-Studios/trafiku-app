package com.pelajstudios.trafiku.data.location

import android.os.SystemClock
import kotlin.math.max

internal class NmeaParser(
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private var accuracyMeters = DEFAULT_ACCURACY_METERS
    private var speedMetersPerSecond: Float? = null

    fun parse(sentence: String): LocationFix? {
        val normalized = sentence.trim()
        if (!normalized.startsWith('$') || !hasValidChecksum(normalized)) {
            return null
        }

        val fields = normalized.substringAfter('$').substringBefore('*').split(',')
        return when (fields.firstOrNull()?.takeLast(3)) {
            "GGA" -> parseGga(fields)
            "RMC" -> parseRmc(fields)
            else -> null
        }
    }

    private fun parseGga(fields: List<String>): LocationFix? {
        if (fields.size < 10 || fields[6].toIntOrNull() == 0) {
            return null
        }

        val latitude = parseCoordinate(fields[2], fields[3]) ?: return null
        val longitude = parseCoordinate(fields[4], fields[5]) ?: return null
        val horizontalDilution = fields[8].toFloatOrNull()
        if (horizontalDilution != null) {
            accuracyMeters = max(MINIMUM_ACCURACY_METERS, horizontalDilution * METERS_PER_HDOP)
        }
        return fix(
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun parseRmc(fields: List<String>): LocationFix? {
        if (fields.size < 10 || fields[2] != "A") {
            return null
        }

        val latitude = parseCoordinate(fields[3], fields[4]) ?: return null
        val longitude = parseCoordinate(fields[5], fields[6]) ?: return null
        speedMetersPerSecond = fields[7].toFloatOrNull()?.times(KNOTS_TO_METERS_PER_SECOND)
        return fix(
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun fix(latitude: Double, longitude: Double): LocationFix = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        elapsedRealtimeMillis = elapsedRealtimeMillis(),
        source = LocationSourceKind.USB,
    )

    private fun parseCoordinate(value: String, hemisphere: String): Double? {
        val rawCoordinate = value.toDoubleOrNull() ?: return null
        val degrees = (rawCoordinate / 100.0).toInt()
        val minutes = rawCoordinate - degrees * 100.0
        val coordinate = degrees + minutes / 60.0
        return when (hemisphere) {
            "N", "E" -> coordinate
            "S", "W" -> -coordinate
            else -> null
        }
    }

    private fun hasValidChecksum(sentence: String): Boolean {
        val separatorIndex = sentence.indexOf('*')
        if (separatorIndex < 0 || separatorIndex + 3 > sentence.length) {
            return false
        }
        val expected = sentence.substring(separatorIndex + 1, separatorIndex + 3)
            .toIntOrNull(radix = 16) ?: return false
        val actual = sentence.substring(1, separatorIndex)
            .fold(0) { checksum, character -> checksum xor character.code }
        return expected == actual
    }

    private companion object {
        const val DEFAULT_ACCURACY_METERS = 25.0F
        const val MINIMUM_ACCURACY_METERS = 3.0F
        const val METERS_PER_HDOP = 5.0F
        const val KNOTS_TO_METERS_PER_SECOND = 0.514444F
    }
}
