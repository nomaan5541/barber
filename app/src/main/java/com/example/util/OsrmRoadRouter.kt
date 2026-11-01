package com.example.util

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

data class OsrmRouteEstimate(
    val distanceKm: Double,
    val durationMinutes: Int,
    val roadGeometryLatLon: List<Pair<Double, Double>>,
    val isLiveOsrm: Boolean
)

data class OsmTileCoord(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val fracX: Double,
    val fracY: Double
) {
    fun tileUrl(): String = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
}

/**
 * Native Android equivalent of Project-OSRM road routing + Leaflet OpenStreetMap slippy tiles.
 *
 * - Queries OSRM (`router.project-osrm.org/route/v1/driving/...`) for real road polylines,
 *   driving distance, and travel time with in-memory LRU caching and instant offline fallback.
 * - Computes Web Mercator slippy tile coordinates and caches OpenStreetMap raster tiles.
 */
object OsrmRoadRouter {

    private const val USER_AGENT = "DoorCutHomeBarberApp/1.0 (Android; OpenStreetMap/OSRM)"
    private val routeCache = LruCache<String, OsrmRouteEstimate>(32)
    private val tileBitmapCache = LruCache<String, ImageBitmap>(64)

    fun fallbackRoadEstimate(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): OsrmRouteEstimate {
        val straightKm = LocationHelper.computeRealDistanceKm(fromLat, fromLon, toLat, toLon) ?: 0.0
        val roadKm = (straightKm * 1.28).coerceAtLeast(0.15)
        val etaMin = ((roadKm / 22.0) * 60.0 + 2.0).roundToInt().coerceIn(2, 90)
        return OsrmRouteEstimate(
            distanceKm = roadKm,
            durationMinutes = etaMin,
            roadGeometryLatLon = listOf(fromLat to fromLon, toLat to toLon),
            isLiveOsrm = false
        )
    }

    suspend fun fetchDrivingRoute(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): OsrmRouteEstimate = withContext(Dispatchers.IO) {
        if ((fromLat == 0.0 && fromLon == 0.0) || (toLat == 0.0 && toLon == 0.0)) {
            return@withContext OsrmRouteEstimate(
                distanceKm = 0.0,
                durationMinutes = 0,
                roadGeometryLatLon = emptyList(),
                isLiveOsrm = false
            )
        }

        // Quantize cache key to ~11m precision to avoid redundant OSRM calls during minor GPS jitter
        val key = String.format(
            Locale.US,
            "%.4f,%.4f->%.4f,%.4f",
            fromLat,
            fromLon,
            toLat,
            toLon
        )
        synchronized(routeCache) {
            routeCache.get(key)
        }?.let { return@withContext it }

        val liveResult = runCatching {
            val endpoint = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                fromLon,
                fromLat,
                toLon,
                toLat
            )
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3500
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    error("OSRM HTTP ${conn.responseCode}")
                }
                val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(raw)
                val routes = root.optJSONArray("routes")
                if (routes == null || routes.length() == 0) error("No OSRM route")
                val first = routes.getJSONObject(0)
                val distMeters = first.optDouble("distance", 0.0)
                val durSeconds = first.optDouble("duration", 0.0)
                val coordsJson = first.optJSONObject("geometry")?.optJSONArray("coordinates")
                val points = buildList {
                    if (coordsJson != null) {
                        for (i in 0 until coordsJson.length()) {
                            val pair = coordsJson.optJSONArray(i) ?: continue
                            if (pair.length() >= 2) {
                                val lon = pair.optDouble(0)
                                val lat = pair.optDouble(1)
                                add(lat to lon)
                            }
                        }
                    }
                }
                OsrmRouteEstimate(
                    distanceKm = (distMeters / 1000.0).coerceAtLeast(0.1),
                    durationMinutes = ceil(durSeconds / 60.0).toInt().coerceIn(1, 180),
                    roadGeometryLatLon = points.ifEmpty { listOf(fromLat to fromLon, toLat to toLon) },
                    isLiveOsrm = true
                )
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            fallbackRoadEstimate(fromLat, fromLon, toLat, toLon)
        }

        synchronized(routeCache) {
            routeCache.put(key, liveResult)
        }
        liveResult
    }

    /**
     * Converts WGS84 (lat, lon) into Web Mercator OpenStreetMap slippy tile indices (Leaflet math).
     */
    fun latLonToOsmTile(lat: Double, lon: Double, zoom: Int): OsmTileCoord {
        val clampedZoom = zoom.coerceIn(2, 19)
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val clampedLon = ((lon + 180.0).mod(360.0)) - 180.0
        val n = 2.0.pow(clampedZoom.toDouble())
        val exactX = ((clampedLon + 180.0) / 360.0) * n
        val latRad = Math.toRadians(clampedLat)
        val exactY = ((1.0 - asinh(tan(latRad)) / PI) / 2.0) * n
        val tileX = floor(exactX).toInt().coerceIn(0, n.toInt() - 1)
        val tileY = floor(exactY).toInt().coerceIn(0, n.toInt() - 1)
        return OsmTileCoord(
            zoom = clampedZoom,
            x = tileX,
            y = tileY,
            fracX = exactX - tileX,
            fracY = exactY - tileY
        )
    }

    suspend fun fetchOsmTileBitmap(zoom: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val n = 1 shl zoom.coerceIn(2, 19)
        if (y !in 0 until n) return@withContext null
        val wrappedX = ((x % n) + n) % n
        val cacheKey = "$zoom/$wrappedX/$y"
        synchronized(tileBitmapCache) {
            tileBitmapCache.get(cacheKey)
        }?.let { return@withContext it }

        runCatching {
            val url = "https://tile.openstreetmap.org/$zoom/$wrappedX/$y.png"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 3000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val bytes = conn.inputStream.use { it.readBytes() }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                if (bmp != null) {
                    synchronized(tileBitmapCache) {
                        tileBitmapCache.put(cacheKey, bmp)
                    }
                }
                bmp
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /**
     * Converts pixel drag delta at a given zoom level to WGS84 (dLat, dLon) degrees.
     */
    fun pixelDeltaToLatLonDelta(
        centerLat: Double,
        zoom: Int,
        dxPx: Float,
        dyPx: Float,
        tileSizePx: Float = 256f
    ): Pair<Double, Double> {
        val worldPixels = tileSizePx * 2.0.pow(zoom.coerceIn(2, 19).toDouble())
        val dLon = -(dxPx / worldPixels) * 360.0
        val cosLat = cos(Math.toRadians(centerLat.coerceIn(-80.0, 80.0))).coerceAtLeast(0.15)
        val dLat = (dyPx / worldPixels) * 360.0 * cosLat
        return dLat to dLon
    }
}
