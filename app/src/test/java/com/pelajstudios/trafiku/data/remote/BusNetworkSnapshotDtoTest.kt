package com.pelajstudios.trafiku.data.remote

import com.pelajstudios.trafiku.data.repository.normalize
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BusNetworkSnapshotDtoTest {
    @Test
    fun serverSnapshotDecodesAndNormalizesForRoom() {
        val payload = """
            {
              "version": 1721476800000,
              "updatedAt": "2026-07-20T12:00:00Z",
              "busLines": {
                "line-id": {
                  "id": "line-id",
                  "name": "Line 1",
                  "routes": {
                    "route-id": {
                      "id": "route-id",
                      "geometry": {
                        "geoLine": {
                          "points": [
                            {"latitude": 42.6629, "longitude": 21.1655}
                          ]
                        }
                      },
                      "stops": [
                        {
                          "id": "stop-a",
                          "name": "Station A",
                          "point": {"latitude": 42.6629, "longitude": 21.1655}
                        },
                        {
                          "id": "stop-b",
                          "name": "Station B",
                          "point": {"latitude": 42.6500, "longitude": 21.1700}
                        }
                      ],
                      "direction": "OUTBOUND"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val snapshot = Json.decodeFromString<BusNetworkSnapshotDto>(payload).normalize(
            operator = "trafiku_urban",
            downloadedAtEpochMillis = 123L,
        )
        val line = snapshot.lines.single()
        val route = snapshot.routes.single()

        assertEquals("Line 1", line.name)
        assertEquals("OUTBOUND", route.direction)
        assertEquals(listOf("Station A", "Station B"), snapshot.stops.map { it.name })
        assertEquals(listOf(0, 1), snapshot.stops.map { it.sequence })
        assertEquals(1, snapshot.routePoints.size)
        assertEquals(123L, snapshot.metadata.downloadedAtEpochMillis)
    }
}
