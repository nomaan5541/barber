package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BarberProfile
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.BarberAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.serviceVisualFor
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.util.DoorCutPreferences

@Composable
fun CustomerFavouritesScreen(
    services: List<CatalogService>,
    approvedBarbers: List<BarberProfile>,
    onQuickBookService: (CatalogService) -> Unit,
    onQuickBookBarber: (BarberProfile) -> Unit,
    onBrowseHome: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    val fallbackBarbers = remember {
        listOf(
            BarberProfile(
                id = "barber_arman_master",
                barberId = "barber_arman_master",
                name = "Arman Siddiqui",
                experienceYears = 7L,
                approvalStatus = "APPROVED",
                isOnline = true,
                ratingAvg = 4.9,
                reviewCount = 48L
            ),
            BarberProfile(
                id = "barber_vikram_stylist",
                barberId = "barber_vikram_stylist",
                name = "Vikram Rathore",
                experienceYears = 8L,
                approvalStatus = "APPROVED",
                isOnline = true,
                ratingAvg = 4.8,
                reviewCount = 36L
            )
        )
    }

    val allBarbers = remember(approvedBarbers) {
        approvedBarbers.ifEmpty { fallbackBarbers }
    }
    val favServices = remember(services, localPrefs.favouriteServiceIds) {
        services.filter { localPrefs.favouriteServiceIds.contains(it.id) }
    }
    val favBarbers = remember(allBarbers, localPrefs.favouriteBarberIds) {
        allBarbers.filter { localPrefs.favouriteBarberIds.contains(it.barberId) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("customer_favourites_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "QUICK REBOOK",
                title = "Your Favourites",
                subtitle = "Book your preferred services and home barbers in one tap.",
                modifier = Modifier.widthIn(max = 600.dp)
            )
        }

        if (favServices.isEmpty() && favBarbers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Favorite,
                    title = "No Favourites Saved Yet",
                    subtitle = "Tap the heart icon on any haircut or barber to keep them here for instant booking.",
                    action = {
                        Button(
                            onClick = onBrowseHome,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Explore Services", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        } else {
            if (favBarbers.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Barbers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    )
                }
                items(favBarbers, key = { it.barberId }) { barber ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("fav_barber_card_${barber.barberId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BarberAvatarBadge(name = barber.name, isOnline = barber.isOnline, size = 48.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = barber.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", barber.ratingAvg)} · ${barber.experienceYears} yrs exp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { DoorCutPreferences.toggleFavouriteBarber(context, barber.barberId) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Remove favourite barber",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = { onQuickBookBarber(barber) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Book", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (favServices.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Services",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    )
                }
                items(favServices, key = { it.id }) { service ->
                    val visual = remember(service.id, service.category) { serviceVisualFor(service) }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .testTag("fav_service_card_${service.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = visual.drawableRes),
                                contentDescription = service.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${CurrencyUtils.formatRupees(service.pricePaise)} · ${service.durationMinutes} min · Pay barber",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { DoorCutPreferences.toggleFavouriteService(context, service.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Remove favourite service",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = { onQuickBookService(service) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Book", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
