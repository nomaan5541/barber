package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CatalogService
import com.example.data.model.CurrencyUtils
import com.example.ui.components.SkillPill
import com.example.ui.components.StatusBadge
import com.example.ui.components.serviceVisualFor
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.example.util.PhonePermissionsSnapshot

data class ServiceCraftDetails(
    val tagline: String,
    val idealFor: String,
    val processSteps: List<String>,
    val toolsAndHygiene: List<String>
)

/**
 * Complete canonical menu of DoorCut haircuts, beard shaves, champi massages, detan facials,
 * and value combos — featuring Normal Head Cut, Traditional Haircut, Skin Fades, Kids & Senior Cuts,
 * with transparent INR pricing and home hygiene standards.
 */
fun canonicalGroomingCatalog(): List<CatalogService> = listOf(
    CatalogService(
        id = "svc_classic_cut",
        name = "Haircut",
        category = "HAIRCUTS",
        description = "Clean scissor & clipper haircut tailored to your style at home, including protective cape, floor sheet, and neat neckline finish.",
        pricePaise = 30000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Keep any comfortable chair ready near good lighting.",
        sortOrder = 1L
    ),
    CatalogService(
        id = "svc_beard_trim",
        name = "Beard Trim",
        category = "BEARD_MOUSTACHE",
        description = "Even beard trim, sharp cheek & jawline line-up with a fresh single-use blade, and soothing aftershave splash.",
        pricePaise = 15000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        instructions = "Fresh disposable blade unsealed in front of you.",
        sortOrder = 2L
    ),
    CatalogService(
        id = "svc_pkg_essential",
        name = "Haircut + Beard",
        category = "PACKAGES",
        description = "Our most popular doorstep combo: full Haircut + crisp Beard Trim & razor line-up in one smooth 45-minute visit.",
        pricePaise = 40000L,
        durationMinutes = 45L,
        isActive = true,
        isPackage = true,
        instructions = "Save ₹50 compared to booking separately.",
        sortOrder = 3L
    ),
    CatalogService(
        id = "svc_ladies_haircut",
        name = "Ladies Haircut & Layer Styling",
        category = "LADIES_STYLING",
        description = "Salon-grade U-cut, V-cut, feather or butterfly layers by a verified Lady Hair Stylist at home, including protective cape & floor cleanup.",
        pricePaise = 49900L,
        durationMinutes = 45L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a comfortable chair near a wall socket if blow-dry finish is desired.",
        sortOrder = 4L
    ),
    CatalogService(
        id = "svc_ladies_layer_blowout",
        name = "Ladies Salon Blowout & Curls",
        category = "LADIES_STYLING",
        description = "Voluminous round-brush salon blowout, soft waves, or sleek straightening with heat-protectant serum at your doorstep.",
        pricePaise = 59900L,
        durationMinutes = 50L,
        isActive = true,
        isPackage = false,
        instructions = "Performed on clean towel-dried or dry hair near a standard power outlet.",
        sortOrder = 5L
    ),
    CatalogService(
        id = "svc_ladies_trim_wash",
        name = "Ladies Split-End Trim & Crown Spa",
        category = "LADIES_STYLING",
        description = "Healthy split-end dusting trim paired with a soothing 15-minute nourishing scalp & shoulder massage.",
        pricePaise = 39900L,
        durationMinutes = 35L,
        isActive = true,
        isPackage = false,
        instructions = "Performed by a verified female hair stylist when Lady Stylist preference is enabled.",
        sortOrder = 6L
    ),
    CatalogService(
        id = "svc_normal_head_cut",
        name = "Normal Head Cut",
        category = "HAIRCUTS",
        description = "Clean, balanced everyday head cut using sanitized clippers & scissors. Even side grading, neat top trim, and crisp neck outline.",
        pricePaise = 19900L,
        durationMinutes = 25L,
        isActive = true,
        isPackage = false,
        instructions = "Keep any straight-back chair ready near good lighting.",
        sortOrder = 7L
    ),
    CatalogService(
        id = "svc_traditional_cut",
        name = "Traditional Haircut (Side-Part / Fauji)",
        category = "HAIRCUTS",
        description = "Classic Indian barbershop scissor-over-comb haircut with neat side-part, tapered ear contour, and fresh blade neck finish.",
        pricePaise = 22900L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a sturdy chair and hand towel accessible.",
        sortOrder = 5L
    ),
    CatalogService(
        id = "svc_skin_fade",
        name = "Zero / Mid / High Skin Fade",
        category = "HAIRCUTS",
        description = "Seamless foil-shaver & precision guard gradient fade (low, mid, high, or drop fade) with sharp hairline detailing.",
        pricePaise = 35000L,
        durationMinutes = 40L,
        isActive = true,
        isPackage = false,
        instructions = "Cordless clippers & foil shaver brought fully charged.",
        sortOrder = 6L
    ),
    CatalogService(
        id = "svc_crew_buzz_cut",
        name = "Crew Cut / Buzz Cut",
        category = "HAIRCUTS",
        description = "Low-maintenance crew or single-guard buzz cut with clean temple taper and quick post-cut brush-off.",
        pricePaise = 17900L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        instructions = "Quick 20-minute mess-free cut over disposable floor sheet.",
        sortOrder = 7L
    ),
    CatalogService(
        id = "svc_kids_cut",
        name = "Kids' Gentle Home Haircut",
        category = "HAIRCUTS",
        description = "Calm, patient, low-noise clipper & rounded-tip scissor haircut at home for children under 12.",
        pricePaise = 25000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Parent or guardian should remain nearby during the haircut.",
        sortOrder = 8L
    ),
    CatalogService(
        id = "svc_senior_cut",
        name = "Senior Comfort Traditional Cut",
        category = "HAIRCUTS",
        description = "Unhurried, respectful traditional scissor haircut right on your elder's favourite home chair.",
        pricePaise = 25000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Can be performed on an armchair or wheelchair.",
        sortOrder = 9L
    ),
    CatalogService(
        id = "svc_traditional_shave",
        name = "Traditional Foam & Razor Shave",
        category = "BEARD_MOUSTACHE",
        description = "Classic rich-lather brush shave using a fresh disposable blade, warm water prep, and cooling lotion.",
        pricePaise = 12000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a small bowl of warm water available.",
        sortOrder = 10L
    ),
    CatalogService(
        id = "svc_clean_shave",
        name = "Hot Towel Clean Shave",
        category = "BEARD_MOUSTACHE",
        description = "Two-pass aromatic warm-towel shave with pre-shave oil, rich cream lather, and hydrating post-shave balm.",
        pricePaise = 18000L,
        durationMinutes = 25L,
        isActive = true,
        isPackage = false,
        instructions = "Warm water access helps prepare the steam towel.",
        sortOrder = 11L
    ),
    CatalogService(
        id = "svc_scalp_oil",
        name = "Head Massage (Champi)",
        category = "HAIR_CARE",
        description = "20-minute stress-relieving Indian champi scalp, forehead & shoulder massage with cooling or coconut oil.",
        pricePaise = 20000L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        instructions = "Barber brings sealed hair oils—or use your preferred home oil.",
        sortOrder = 12L
    ),
    CatalogService(
        id = "svc_hair_wash",
        name = "Hair Wash & Blow Dry",
        category = "HAIR_CARE",
        description = "Refreshing shampoo rinse to clear loose clippings followed by a styled blow-dry finish.",
        pricePaise = 10000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        instructions = "Requires washbasin/shower rinse access and a wall socket.",
        sortOrder = 13L
    ),
    CatalogService(
        id = "svc_face_cleanup",
        name = "Detan Face Cleanup",
        category = "ADD_ONS",
        description = "4-step tan-removal cleanser, exfoliating scrub, charcoal mask, and brightening massage.",
        pricePaise = 35000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Single-use sealed skincare sachets used for hygiene.",
        sortOrder = 14L
    ),
    CatalogService(
        id = "svc_pkg_traditional_combo",
        name = "Traditional Cut + Shave + Champi",
        category = "PACKAGES",
        description = "Complete traditional home combo: Traditional Haircut + Shave/Beard Trim + 15-min Oil Head Massage.",
        pricePaise = 42900L,
        durationMinutes = 55L,
        isActive = true,
        isPackage = true,
        instructions = "Save ₹90 compared to individual services.",
        sortOrder = 15L
    ),
    CatalogService(
        id = "svc_pkg_complete",
        name = "Complete Grooming Package",
        category = "PACKAGES",
        description = "Haircut + Beard Trim/Shave + Detan Face Cleanup + Relaxing Head Massage.",
        pricePaise = 79900L,
        durationMinutes = 80L,
        isActive = true,
        isPackage = true,
        instructions = "Full head-to-toe refresh at home.",
        sortOrder = 16L
    )
)

/**
 * Merges live Firestore catalog services with the canonical catalog so newly introduced
 * haircuts (such as Normal Head Cut & Traditional Haircut) are always available while
 * respecting any live price/description updates stored in Firestore.
 */
fun mergeWithCanonicalCatalog(liveServices: List<CatalogService>): List<CatalogService> {
    if (liveServices.isEmpty()) return canonicalGroomingCatalog()
    val canonical = canonicalGroomingCatalog()
    val liveById = liveServices.associateBy { it.id }
    val merged = mutableListOf<CatalogService>()

    canonical.forEachIndexed { index, defaultSvc ->
        val overrideSvc = liveById[defaultSvc.id]
        if (overrideSvc != null) {
            merged.add(overrideSvc.copy(sortOrder = (index + 1).toLong()))
        } else {
            merged.add(defaultSvc.copy(sortOrder = (index + 1).toLong()))
        }
    }

    val canonicalIds = canonical.map { it.id }.toSet()
    liveServices.filter { !canonicalIds.contains(it.id) }.forEach { extra ->
        merged.add(extra)
    }
    return merged.filter { it.isActive }.sortedBy { it.sortOrder }
}

fun craftDetailsFor(service: CatalogService): ServiceCraftDetails {
    return when (service.id) {
        "svc_normal_head_cut" -> ServiceCraftDetails(
            tagline = "Everyday Neat & Balanced Clipper + Scissor Cut",
            idealFor = "Daily office wear, students & regular 3-week maintenance",
            processSteps = listOf(
                "Disposable floor sheet laid around your chair for zero hair mess",
                "Even clipper guard trim on sides & back (#2, #3, or #4 as requested)",
                "Point-cut scissor trim on crown & fringe for natural balance",
                "Crisp sideburn & nape outline + post-cut floor sweep"
            ),
            toolsAndHygiene = listOf("Sanitized Cordless Clippers", "Fresh Neck Strip & Cape", "Disposable Razor Outline", "Zero-Mess Floor Sheet")
        )
        "svc_traditional_cut" -> ServiceCraftDetails(
            tagline = "Authentic Indian Barbershop Scissor-Over-Comb Craft",
            idealFor = "Classic side-part, Fauji neat trim, formal & family occasions",
            processSteps = listOf(
                "Consultation on parting side, ear clearance & crown length",
                "Precision scissor-over-comb tapering without harsh skin lines",
                "Fresh half-blade straight razor cleanup around ears & neckline",
                "Classic fine-tooth comb styling with water mist or light hair oil"
            ),
            toolsAndHygiene = listOf("Japanese Steel Scissors", "Unsealed Fresh Blade", "Antiseptic Spray", "Full Floor Cleanup")
        )
        "svc_skin_fade" -> ServiceCraftDetails(
            tagline = "Seamless Zero / Mid / High Foil-Shaver Gradient",
            idealFor = "Sharp modern fade, textured crop, pompadour & angular jawlines",
            processSteps = listOf(
                "Guideline mapping for Low, Mid, High, or Drop Fade",
                "Multi-guard blending & zero-gap foil shaver finish",
                "Scissor texturizing on top + crisp C-cup & forehead line-up",
                "Cooling talcum / after-cut brush & full floor cleanup"
            ),
            toolsAndHygiene = listOf("Zero-Gap Foil Shaver", "Hospital-Grade Disinfectant", "Precision Trimmer", "Sanitized Cape")
        )
        "svc_crew_buzz_cut" -> ServiceCraftDetails(
            tagline = "Sharp Low-Maintenance Guard Cut in 20 Minutes",
            idealFor = "Athletes, summer comfort & crisp uniform length",
            processSteps = listOf(
                "Uniform guard length selection across crown & sides",
                "Subtle temple & neckline taper for a clean silhouette",
                "Razor-sharp forehead & sideburn detailing",
                "Fast post-cut dust-off & floor sheet disposal"
            ),
            toolsAndHygiene = listOf("High-Torque Clipper", "Fresh Disposable Blade", "Neck Duster", "Disposable Floor Mat")
        )
        else -> ServiceCraftDetails(
            tagline = when (service.category) {
                "LADIES_STYLING" -> "Verified Lady Hair Stylist Precision Cut & Blowout"
                "HAIRCUTS" -> "Salon-Grade Precision Haircut at Your Doorstep"
                "BEARD_MOUSTACHE" -> "Symmetrical Razor Line-Up & Soothing Skin Care"
                "HAIR_CARE" -> "Deep Relaxation & Scalp Revitalization"
                "PACKAGES" -> "All-In-One Value Home Grooming Ritual"
                else -> "Targeted Skin Detox & Crisp Finishing"
            },
            idealFor = if (service.isPackage) "Complete weekend refresh & maximum combo savings" else "Comfort of home with 100% sanitized barbershop tools",
            processSteps = listOf(
                "Clean home station setup with protective cape & floor sheet",
                "Tools sanitized with hospital-grade spray before starting",
                service.description,
                "Post-service cleanup & pay service charge directly to barber"
            ),
            toolsAndHygiene = listOf("Sanitized Kit", "Single-Use Blade", "Pay Barber After Cut", "Cash or UPI")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailBottomSheet(
    service: CatalogService,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onGoToCart: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val visual = remember(service.id, service.category, service.name, service.isPackage) {
        serviceVisualFor(service)
    }
    val details = remember(service.id) { craftDetailsFor(service) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("service_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bright Unshaded Image Banner + Crisp Title & Right-Side Bold Price Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = visual.drawableRes),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = visual.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close details",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = details.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = BrassGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.5.dp, BrassGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatRupees(service.pricePaise),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Key Specs Strip (Duration & Pay Barber Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.glass.LiquidGlassSurface(
                    strength = com.example.ui.glass.GlassMaterialStrength.AtelierCard,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 1.dp,
                    interactivePhysics = false
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${service.durationMinutes} mins at home",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                StatusBadge("PAY BARBER AFTER CUT")
            }

            // Full Description
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            com.example.ui.glass.LiquidGlassSurface(
                strength = com.example.ui.glass.GlassMaterialStrength.AccentEmerald,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                interactivePhysics = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Best Suited For",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = details.idealFor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // What's Included Step-by-Step
            Text(
                text = "What's Included in This Service",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.processSteps.forEach { step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SageVerified,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hygiene & Tools Pills
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tools, Hygiene & Home Prep",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                details.toolsAndHygiene.forEachIndexed { idx, pill ->
                    SkillPill(skill = pill, highlighted = idx == 0)
                }
            }
            if (service.instructions.isNotBlank()) {
                Text(
                    text = "Home Note: ${service.instructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Transparent Pricing Row in Liquid Glass
            com.example.ui.glass.LiquidGlassSurface(
                strength = com.example.ui.glass.GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                interactivePhysics = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pay directly to barber after service", style = MaterialTheme.typography.bodySmall)
                        Text(
                            CurrencyUtils.formatRupees(service.pricePaise),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Paid directly to your barber after completion by Cash or UPI. A small booking confirmation fee is paid at checkout to reserve your time slot.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons with Tactile Press Physics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onToggleSelect()
                        if (!isSelected && onGoToCart == null) {
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SageVerified else EspressoHeaderBg,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .tactilePressPhysics(enableHaptics = true)
                        .testTag("sheet_toggle_service_button")
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSelected) {
                            "Added • Remove"
                        } else {
                            "Add • ${CurrencyUtils.formatRupees(service.pricePaise)}"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onGoToCart != null && isSelected) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onGoToCart()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .tactilePressPhysics(enableHaptics = true)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Compact, non-blocking Phone Permissions Readiness Banner shown when Location,
 * Notifications, or Camera permissions have not yet been granted on the phone.
 */
@Composable
fun PhonePermissionsPromptCard(
    snapshot: PhonePermissionsSnapshot,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (snapshot.allGrantedIncludingCamera) return

    com.example.ui.glass.LiquidGlassSurface(
        strength = com.example.ui.glass.GlassMaterialStrength.AtelierCard,
        shape = RoundedCornerShape(22.dp),
        accentBorderColor = BrassGold.copy(alpha = 0.55f),
        shadowElevation = 2.dp,
        interactivePhysics = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("phone_permissions_prompt_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EspressoHeaderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permissions_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.permissions_banner_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionMiniChip(
                    icon = Icons.Filled.LocationOn,
                    label = "GPS Door",
                    granted = snapshot.hasAnyLocation,
                    modifier = Modifier.weight(1f)
                )
                PermissionMiniChip(
                    icon = Icons.Filled.NotificationsActive,
                    label = "Alerts",
                    granted = snapshot.hasNotifications,
                    modifier = Modifier.weight(1f)
                )
                PermissionMiniChip(
                    icon = Icons.Filled.CameraAlt,
                    label = "Style Cam",
                    granted = snapshot.hasCamera,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EspressoHeaderBg,
                    contentColor = Color(0xFFFFDF9E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tactilePressPhysics(enableHaptics = true)
                    .testTag("request_all_permissions_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = BrassGold,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.permissions_btn_grant_all),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PermissionMiniChip(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (granted) {
            SageVerified.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (granted) SageVerified.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = if (granted) SageVerified else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (granted) SageVerified else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
