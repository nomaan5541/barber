package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.serviceVisualFor
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.util.DoorCutPreferences

@Composable
fun CustomerFavouritesScreen(
    services: List<CatalogService>,
    onQuickBookService: (CatalogService) -> Unit,
    onBrowseHome: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DoorCutPreferences.ensureLoaded(context) }
    val localPrefs by DoorCutPreferences.state.collectAsStateWithLifecycle()

    val favServices = remember(services, localPrefs.favouriteServiceIds) {
        services.filter { localPrefs.favouriteServiceIds.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_favourites_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SectionHeader(
                eyebrow = "QUICK REBOOK",
                title = "Saved Services",
                subtitle = "Rebook your favourite haircuts, shaves, or ladies styling in one tap.",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .staggeredSpringEntrance(index = 0)
            )
        }

        if (favServices.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.Favorite,
                        title = "No Saved Services Yet",
                        subtitle = "Tap the heart icon on any haircut or styling service on Home to save it here for instant rebooking.",
                        action = {
                            Button(
                                onClick = onBrowseHome,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EspressoHeaderBg,
                                    contentColor = Color(0xFFFFDF9E)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .tactilePressPhysics(enableHaptics = true)
                                    .testTag("explore_services_from_favourites_button")
                            ) {
                                Text("Explore Services", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        } else {
            itemsIndexed(favServices, key = { _, s -> s.id }) { idx, service ->
                val visual = remember(service.id, service.category) { serviceVisualFor(service) }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .staggeredSpringEntrance(index = idx + 1)
                        .testTag("fav_service_card_${service.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Image(
                                painter = painterResource(id = visual.drawableRes),
                                contentDescription = service.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${service.durationMinutes} min · Pay barber after cut",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = BrassGold.copy(alpha = 0.24f),
                                    border = BorderStroke(1.dp, BrassGold),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = CurrencyUtils.formatRupees(service.pricePaise),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                            }
                        }

                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Button(
                            onClick = { onQuickBookService(service) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EspressoHeaderBg,
                                contentColor = Color(0xFFFFDF9E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .tactilePressPhysics(enableHaptics = true)
                                .testTag("fav_book_service_button_${service.id}")
                        ) {
                            Icon(Icons.Filled.ContentCut, contentDescription = null, tint = BrassGold, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Book Service Now  •  ${CurrencyUtils.formatRupees(service.pricePaise)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
