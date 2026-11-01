package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.ui.glass.LiveGlassPulseDot
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.LocationHelper
import com.example.util.OsrmRoadRouter
import com.example.util.OsrmRouteEstimate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Uber / Rapido-inspired Live Dispatch & Barber Movement Map powered by
 * OpenStreetMap slippy tiles (Leaflet Web Mercator math) + Project-OSRM road routing.
 * - During AWAITING_BARBER: renders animated radar sonar waves centered on the customer's door pin.
 * - Once claimed (CONFIRMED / ON_THE_WAY / ARRIVED / IN_PROGRESS): fetches OSRM driving polyline,
 *   road distance, and ETA (with instant offline Haversine fallback), rendering the stylist's live
 *   position along the road route.
 */
@Composable
fun LiveBarberTrackingMap(
    booking: Booking,
    destinationAddressQuery: String? = null,
    mapHeight: Dp = 250.dp,
    isBarberPerspective: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val hasCustomerGps = booking.customerLat != 0.0 && booking.customerLon != 0.0
    val hasBarberLiveGps = booking.barberLat != 0.0 && booking.barberLon != 0.0

    var osrmRoute by remember(
        booking.customerLat,
        booking.customerLon,
        booking.barberLat,
        booking.barberLon
    ) {
        mutableStateOf<OsrmRouteEstimate?>(
            if (hasCustomerGps && hasBarberLiveGps) {
                OsrmRoadRouter.fallbackRoadEstimate(
                    fromLat = booking.barberLat,
                    fromLon = booking.barberLon,
                    toLat = booking.customerLat,
                    toLon = booking.customerLon
                )
            } else {
                null
            }
        )
    }

    LaunchedEffect(
        booking.customerLat,
        booking.customerLon,
        booking.barberLat,
        booking.barberLon,
        booking.status
    ) {
        if (hasCustomerGps && hasBarberLiveGps && booking.status != "AWAITING_BARBER") {
            osrmRoute = OsrmRoadRouter.fetchDrivingRoute(
                fromLat = booking.barberLat,
                fromLon = booking.barberLon,
                toLat = booking.customerLat,
                toLon = booking.customerLon
            )
        }
    }

    val tileCenterLat = when {
        hasCustomerGps && hasBarberLiveGps -> (booking.customerLat + booking.barberLat) * 0.5
        hasCustomerGps -> booking.customerLat
        hasBarberLiveGps -> booking.barberLat
        else -> 0.0
    }
    val tileCenterLon = when {
        hasCustomerGps && hasBarberLiveGps -> (booking.customerLon + booking.barberLon) * 0.5
        hasCustomerGps -> booking.customerLon
        hasBarberLiveGps -> booking.barberLon
        else -> 0.0
    }
    var osmTileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(tileCenterLat, tileCenterLon) {
        if (tileCenterLat != 0.0 && tileCenterLon != 0.0) {
            val coord = OsrmRoadRouter.latLonToOsmTile(tileCenterLat, tileCenterLon, zoom = 15)
            osmTileBitmap = OsrmRoadRouter.fetchOsmTileBitmap(coord.zoom, coord.x, coord.y)
        } else {
            osmTileBitmap = null
        }
    }

    val realDistanceKm = remember(
        osrmRoute,
        booking.customerLat,
        booking.customerLon,
        booking.barberLat,
        booking.barberLon
    ) {
        osrmRoute?.distanceKm ?: LocationHelper.computeRealDistanceKm(
            booking.barberLat,
            booking.barberLon,
            booking.customerLat,
            booking.customerLon
        )
    }

    // Bearing from Customer Home -> Barber Live Position (for true geographic canvas placement)
    val bearingHomeToBarberDeg = remember(
        booking.customerLat,
        booking.customerLon,
        booking.barberLat,
        booking.barberLon
    ) {
        if (hasCustomerGps && hasBarberLiveGps) {
            LocationHelper.bearingDegrees(
                booking.customerLat,
                booking.customerLon,
                booking.barberLat,
                booking.barberLon
            )
        } else {
            null
        }
    }

    // Compass heading of Barber riding toward Customer Home (e.g., "Heading NE")
    val barberHeadingCompass = remember(
        booking.customerLat,
        booking.customerLon,
        booking.barberLat,
        booking.barberLon
    ) {
        if (hasCustomerGps && hasBarberLiveGps) {
            val headingDeg = LocationHelper.bearingDegrees(
                booking.barberLat,
                booking.barberLon,
                booking.customerLat,
                booking.customerLon
            )
            LocationHelper.compassDirection(headingDeg)
        } else {
            null
        }
    }

    // Target route progress (0f = starting point, 1f = arrived at customer's doorstep)
    val rawProgress = remember(booking.status, realDistanceKm) {
        when (booking.status) {
            "AWAITING_BARBER" -> 0f
            "CONFIRMED" -> {
                if (realDistanceKm != null) {
                    (1f - (realDistanceKm.toFloat() / 6f)).coerceIn(0.12f, 0.85f)
                } else {
                    0.18f
                }
            }
            "ON_THE_WAY" -> {
                if (realDistanceKm != null) {
                    (1f - (realDistanceKm.toFloat() / 5f)).coerceIn(0.28f, 0.94f)
                } else {
                    0.62f
                }
            }
            "ARRIVED", "IN_PROGRESS", "COMPLETED" -> 1.0f
            else -> 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (reducedMotion) {
            tween(150)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "barber_route_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse_transition")
    val radarPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reducedMotion) 2800 else 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave_phase"
    )

    val etaMinutes = remember(booking.status, osrmRoute, realDistanceKm, booking.bookingMode) {
        when (booking.status) {
            "ARRIVED", "IN_PROGRESS", "COMPLETED" -> 0
            "ON_THE_WAY" -> {
                when {
                    osrmRoute != null -> osrmRoute!!.durationMinutes
                    realDistanceKm != null -> LocationHelper.estimateTravelMinutes(realDistanceKm, isAsapExpress = true)
                    else -> 8
                }
            }
            "CONFIRMED" -> {
                when {
                    osrmRoute != null -> (osrmRoute!!.durationMinutes + 4).coerceAtMost(90)
                    realDistanceKm != null -> LocationHelper.estimateTravelMinutes(realDistanceKm, isAsapExpress = false)
                    else -> 14
                }
            }
            else -> null
        }
    }

    val statusBannerText = remember(booking.status, booking.preferLadyBarber, booking.barberName, etaMinutes) {
        when (booking.status) {
            "AWAITING_BARBER" -> if (booking.preferLadyBarber) {
                "Radar Active · Alerting Lady Hair Stylists nearby"
            } else {
                "Radar Active · Alerting verified Barbers nearby"
            }
            "CONFIRMED" -> "${booking.barberName} accepted · Preparing sanitized kit"
            "ON_THE_WAY" -> "${booking.barberName} is riding to your doorstep"
            "ARRIVED" -> "${booking.barberName} has arrived at your building"
            "IN_PROGRESS" -> "Grooming session in progress at home"
            "COMPLETED" -> "Visit completed at your doorstep"
            else -> booking.friendlyStatusText()
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_barber_tracking_map")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Interactive Map Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFEEF2EE))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(0.85f, 2.2f)
                            val maxPan = 140f * zoom
                            panOffset = Offset(
                                x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f + panOffset.x
                    val cy = h * 0.5f + panOffset.y

                    val loadedTile = osmTileBitmap
                    if (loadedTile != null) {
                        val tileSide = (max(w, h) * 1.15f * zoom).roundToInt().coerceAtLeast(256)
                        drawImage(
                            image = loadedTile,
                            dstOffset = IntOffset(
                                x = (cx - tileSide * 0.5f).roundToInt(),
                                y = (cy - tileSide * 0.5f).roundToInt()
                            ),
                            dstSize = IntSize(tileSide, tileSide),
                            alpha = 0.88f
                        )
                    } else {
                        // Offline vector urban park, water & street grid fallback
                        drawRect(
                            color = Color(0xFFDCEAD9),
                            topLeft = Offset(cx - w * 0.42f * zoom, cy - h * 0.36f * zoom),
                            size = androidx.compose.ui.geometry.Size(w * 0.28f * zoom, h * 0.26f * zoom)
                        )
                        drawRect(
                            color = Color(0xFFD7E6F2),
                            topLeft = Offset(cx + w * 0.14f * zoom, cy + h * 0.10f * zoom),
                            size = androidx.compose.ui.geometry.Size(w * 0.32f * zoom, h * 0.28f * zoom)
                        )

                        val gridStep = 44.dp.toPx() * zoom
                        var gx = (cx % gridStep) - gridStep
                        while (gx < w + gridStep) {
                            drawLine(
                                color = Color(0xFFDCE1DC),
                                start = Offset(gx, 0f),
                                end = Offset(gx, h),
                                strokeWidth = 2.dp.toPx()
                            )
                            gx += gridStep
                        }
                        var gy = (cy % gridStep) - gridStep
                        while (gy < h + gridStep) {
                            drawLine(
                                color = Color(0xFFDCE1DC),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 2.dp.toPx()
                            )
                            gy += gridStep
                        }

                        val avenuePath1 = Path().apply {
                            moveTo(0f, cy + h * 0.26f * zoom)
                            cubicTo(
                                cx - w * 0.20f * zoom, cy + h * 0.16f * zoom,
                                cx + w * 0.15f * zoom, cy - h * 0.18f * zoom,
                                w, cy - h * 0.28f * zoom
                            )
                        }
                        drawPath(
                            path = avenuePath1,
                            color = Color.White,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    val homePos = if (booking.status == "AWAITING_BARBER") {
                        Offset(cx, cy)
                    } else if (bearingHomeToBarberDeg != null) {
                        val rad = Math.toRadians(bearingHomeToBarberDeg.toDouble())
                        val shiftX = (-sin(rad).toFloat() * w * 0.16f * zoom)
                        val shiftY = (cos(rad).toFloat() * h * 0.14f * zoom)
                        Offset(
                            x = (cx + shiftX).coerceIn(52.dp.toPx(), w - 52.dp.toPx()),
                            y = (cy + shiftY).coerceIn(52.dp.toPx(), h - 52.dp.toPx())
                        )
                    } else {
                        Offset(
                            x = (cx + w * 0.24f * zoom).coerceIn(48.dp.toPx(), w - 48.dp.toPx()),
                            y = (cy - h * 0.14f * zoom).coerceIn(48.dp.toPx(), h - 48.dp.toPx())
                        )
                    }

                    if (booking.status == "AWAITING_BARBER") {
                        // Uber / Rapido Open-Dispatch Radar Pulse Rings
                        val maxRadius = (minOf(w, h) * 0.44f) * zoom
                        val pulseColor = if (booking.preferLadyBarber) Color(0xFF8E24AA) else SageVerified

                        for (ringIndex in 0..2) {
                            val ringPhase = (radarPhase + ringIndex * 0.33f) % 1f
                            val radius = 18.dp.toPx() + maxRadius * ringPhase
                            val alpha = (1f - ringPhase) * 0.36f
                            drawCircle(
                                color = pulseColor.copy(alpha = alpha),
                                radius = radius,
                                center = homePos
                            )
                            drawCircle(
                                color = pulseColor.copy(alpha = alpha * 1.4f),
                                radius = radius,
                                center = homePos,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Customer Home Pin at Center
                        drawCircle(color = Color.White, radius = 18.dp.toPx(), center = homePos)
                        drawCircle(color = EspressoHeaderBg, radius = 14.dp.toPx(), center = homePos)
                        drawCircle(color = BrassGold, radius = 6.dp.toPx(), center = homePos)
                    } else {
                        // Active Barber -> Customer Live Route projected along OSRM road network or true bearing
                        val startPos = if (bearingHomeToBarberDeg != null) {
                            val rad = Math.toRadians(bearingHomeToBarberDeg.toDouble())
                            val span = minOf(w, h) * 0.48f * zoom
                            Offset(
                                x = (homePos.x + sin(rad).toFloat() * span).coerceIn(44.dp.toPx(), w - 44.dp.toPx()),
                                y = (homePos.y - cos(rad).toFloat() * span).coerceIn(44.dp.toPx(), h - 44.dp.toPx())
                            )
                        } else {
                            Offset(
                                x = (cx - w * 0.28f * zoom).coerceIn(44.dp.toPx(), w - 44.dp.toPx()),
                                y = (cy + h * 0.22f * zoom).coerceIn(44.dp.toPx(), h - 44.dp.toPx())
                            )
                        }

                        val routeGeo = osrmRoute?.roadGeometryLatLon.orEmpty()
                        val barberPos: Offset
                        if (routeGeo.size >= 3 && osrmRoute?.isLiveOsrm == true) {
                            // Project OSRM GeoJSON polyline vertices from (fromLat,fromLon)->(toLat,toLon) onto startPos->homePos frame
                            val firstPt = routeGeo.first()
                            val lastPt = routeGeo.last()
                            val dLonTotal = (lastPt.second - firstPt.second)
                            val dLatTotal = (lastPt.first - firstPt.first)
                            val screenPts = routeGeo.mapIndexed { idx, (lat, lon) ->
                                when (idx) {
                                    0 -> startPos
                                    routeGeo.lastIndex -> homePos
                                    else -> {
                                        val fx = if (kotlin.math.abs(dLonTotal) > 1e-5) {
                                            ((lon - firstPt.second) / dLonTotal).toFloat().coerceIn(-0.25f, 1.25f)
                                        } else {
                                            idx.toFloat() / routeGeo.lastIndex.toFloat()
                                        }
                                        val fy = if (kotlin.math.abs(dLatTotal) > 1e-5) {
                                            ((lat - firstPt.first) / dLatTotal).toFloat().coerceIn(-0.25f, 1.25f)
                                        } else {
                                            idx.toFloat() / routeGeo.lastIndex.toFloat()
                                        }
                                        Offset(
                                            x = (startPos.x + (homePos.x - startPos.x) * fx).coerceIn(28.dp.toPx(), w - 28.dp.toPx()),
                                            y = (startPos.y + (homePos.y - startPos.y) * fy).coerceIn(28.dp.toPx(), h - 28.dp.toPx())
                                        )
                                    }
                                }
                            }
                            val osrmPath = Path().apply {
                                moveTo(screenPts.first().x, screenPts.first().y)
                                for (i in 1 until screenPts.size) {
                                    lineTo(screenPts[i].x, screenPts[i].y)
                                }
                            }
                            drawPath(
                                path = osrmPath,
                                color = Color.White.copy(alpha = 0.85f),
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = osrmPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(SageVerified, BrassGold),
                                    start = startPos,
                                    end = homePos
                                ),
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val exactIdx = (animatedProgress.coerceIn(0f, 1f) * (screenPts.size - 1))
                            val lo = exactIdx.toInt().coerceIn(0, screenPts.lastIndex)
                            val hi = (lo + 1).coerceAtMost(screenPts.lastIndex)
                            val frac = exactIdx - lo
                            barberPos = Offset(
                                x = screenPts[lo].x + (screenPts[hi].x - screenPts[lo].x) * frac,
                                y = screenPts[lo].y + (screenPts[hi].y - screenPts[lo].y) * frac
                            )
                        } else {
                            val control1 = Offset(
                                x = startPos.x + (homePos.x - startPos.x) * 0.38f,
                                y = startPos.y - 24.dp.toPx() * zoom
                            )
                            val control2 = Offset(
                                x = startPos.x + (homePos.x - startPos.x) * 0.68f,
                                y = homePos.y + 20.dp.toPx() * zoom
                            )

                            val fullRoute = Path().apply {
                                moveTo(startPos.x, startPos.y)
                                cubicTo(control1.x, control1.y, control2.x, control2.y, homePos.x, homePos.y)
                            }
                            drawPath(
                                path = fullRoute,
                                color = EspressoHeaderBg.copy(alpha = 0.25f),
                                style = Stroke(
                                    width = 5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                )
                            )

                            val t = animatedProgress.coerceIn(0f, 1f)
                            val u = 1f - t
                            val barberX = (u * u * u * startPos.x) +
                                (3f * u * u * t * control1.x) +
                                (3f * u * t * t * control2.x) +
                                (t * t * t * homePos.x)
                            val barberY = (u * u * u * startPos.y) +
                                (3f * u * u * t * control1.y) +
                                (3f * u * t * t * control2.y) +
                                (t * t * t * homePos.y)
                            barberPos = Offset(barberX, barberY)

                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(SageVerified, BrassGold),
                                    start = barberPos,
                                    end = homePos
                                ),
                                start = barberPos,
                                end = homePos,
                                strokeWidth = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Customer Home Destination Pin
                        drawCircle(
                            color = EspressoHeaderBg.copy(alpha = 0.16f),
                            radius = 22.dp.toPx(),
                            center = homePos
                        )
                        drawCircle(color = Color.White, radius = 15.dp.toPx(), center = homePos)
                        drawCircle(color = EspressoHeaderBg, radius = 11.dp.toPx(), center = homePos)
                        drawCircle(color = BrassGold, radius = 4.5.dp.toPx(), center = homePos)

                        // Live Barber Scooter / Stylist Pulse & Marker
                        val liveGlowRadius = 18.dp.toPx() + (10.dp.toPx() * radarPhase)
                        drawCircle(
                            color = SageVerified.copy(alpha = (1f - radarPhase) * 0.38f),
                            radius = liveGlowRadius,
                            center = barberPos
                        )
                        drawCircle(color = Color.White, radius = 17.dp.toPx(), center = barberPos)
                        drawCircle(color = SageVerified, radius = 13.5.dp.toPx(), center = barberPos)
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = barberPos)
                    }
                }

                // Top-Left Live Status & ETA Pill Overlay
                Surface(
                    color = EspressoHeaderBg.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveGlassPulseDot(
                            statusKey = booking.status,
                            color = if (booking.status == "AWAITING_BARBER") BrassGold else SageVerified,
                            size = 8.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when {
                                    booking.status == "AWAITING_BARBER" -> "SEARCHING NEARBY STYLISTS"
                                    etaMinutes == 0 -> "ARRIVED AT YOUR DOORSTEP"
                                    etaMinutes != null -> "LIVE ETA · ~$etaMinutes MINS"
                                    else -> "LIVE DISPATCH MAP"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFDF9E)
                            )
                            Text(
                                text = when {
                                    realDistanceKm != null -> {
                                        val distLabel = LocationHelper.formatDistanceKm(realDistanceKm)
                                        val headingSuffix = barberHeadingCompass?.let { " · Heading $it" }.orEmpty()
                                        val routeSource = if (osrmRoute?.isLiveOsrm == true) "OSRM Road" else "Road Est."
                                        "$distLabel away$headingSuffix · $routeSource"
                                    }
                                    booking.status == "AWAITING_BARBER" -> booking.areaSummary.ifBlank { "Doorstep Pin Locked" }
                                    else -> booking.areaSummary.ifBlank { "Doorstep Route Locked" }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top-Right Recenter Map Control
                IconButton(
                    onClick = {
                        zoom = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Recenter live map",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Bottom Map Legend Badges (Barber Scooter vs Customer Door + OpenStreetMap/OSRM Attribution)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (booking.status == "AWAITING_BARBER") {
                                    Icons.Filled.ContentCut
                                } else {
                                    Icons.Filled.TwoWheeler
                                },
                                contentDescription = null,
                                tint = SageVerified,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (booking.status == "AWAITING_BARBER") {
                                    if (booking.preferLadyBarber) "Lady Stylist Pool" else "Nearby Barber Pool"
                                } else {
                                    booking.barberName.take(18)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = EspressoHeaderBg,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Home Pin",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        color = EspressoHeaderBg.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "© OSM · OSRM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Bottom Action Bar: Live Movement Summary + Launch Turn-by-Turn Maps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusBannerText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val coordSubtext = when {
                        hasBarberLiveGps -> String.format(
                            Locale.US,
                            "Stylist GPS: %.4f, %.4f",
                            booking.barberLat,
                            booking.barberLon
                        )
                        hasCustomerGps -> String.format(
                            Locale.US,
                            "Door GPS: %.4f, %.4f",
                            booking.customerLat,
                            booking.customerLon
                        )
                        else -> booking.areaSummary
                    }
                    Text(
                        text = coordSubtext,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val navTargetQuery = destinationAddressQuery?.takeIf { it.isNotBlank() }
                    ?: if (hasCustomerGps) "${booking.customerLat},${booking.customerLon}" else booking.areaSummary

                if (navTargetQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    FilledTonalButton(
                        onClick = {
                            val encoded = Uri.encode(navTargetQuery)
                            val uri = if (isBarberPerspective) {
                                Uri.parse("google.navigation:q=$encoded")
                            } else {
                                Uri.parse("geo:0,0?q=$encoded")
                            }
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(mapIntent) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_external_maps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = "Open Google Maps",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBarberPerspective) "Navigate" else "Maps",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
