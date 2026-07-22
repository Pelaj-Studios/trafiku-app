package com.pelajstudios.trafiku.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NmeaParserTest {
    private val parser = NmeaParser(elapsedRealtimeMillis = { 123L })

    @Test
    fun validGgaAndRmcSentencesProduceUsbFix() {
        parser.parse(
            "\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47",
        )

        val fix = parser.parse(
            "\$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A",
        )

        requireNotNull(fix)
        assertEquals(48.1173, fix.latitude, 0.000001)
        assertEquals(11.516666, fix.longitude, 0.000001)
        assertEquals(4.5F, fix.accuracyMeters, 0.01F)
        assertEquals(11.5235F, fix.speedMetersPerSecond ?: 0.0F, 0.001F)
        assertEquals(123L, fix.elapsedRealtimeMillis)
        assertEquals(LocationSourceKind.USB, fix.source)
    }

    @Test
    fun invalidChecksumDoesNotProduceFix() {
        val fix = parser.parse(
            "\$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*00",
        )

        assertNull(fix)
    }
}
