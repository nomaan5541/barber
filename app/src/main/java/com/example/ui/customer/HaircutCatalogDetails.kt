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
        id = "svc_normal_head_cut",
        name = "Normal Head Cut",
        category = "HAIRCUTS",
        description = "Clean, balanced everyday head cut using sanitized clippers & scissors. Even side grading, neat top trim, natural sideburns, and crisp neck outline.",
        pricePaise = 19900L,
        durationMinutes = 25L,
        isActive = true,
        isPackage = false,
        instructions = "Keep any comfortable straight-back chair ready near good lighting.",
        sortOrder = 1L
    ),
    CatalogService(
        id = "svc_traditional_cut",
        name = "Traditional Haircut (Side-Part / Fauji)",
        category = "HAIRCUTS",
        description = "Classic Indian barbershop scissor-over-comb haircut with neat side-part, tapered ear contour, disposable straight-razor neck finish & water/oil set.",
        pricePaise = 22900L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a sturdy chair and hand towel accessible for classic comb finish.",
        sortOrder = 2L
    ),
    CatalogService(
        id = "svc_classic_cut",
        name = "Classic Scissor & Clipper Haircut",
        category = "HAIRCUTS",
        description = "Tailored executive scissor layering & clipper blend customized to your crown and face shape, with clean neckline taper and cape setup.",
        pricePaise = 30000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a chair and power socket nearby if blow-dry finish is desired.",
        sortOrder = 3L
    ),
    CatalogService(
        id = "svc_skin_fade",
        name = "Zero / Mid / High Skin Fade",
        category = "HAIRCUTS",
        description = "Seamless foil-shaver & precision guard gradient fade (low, mid, high, or drop fade) with razor-sharp hairline detailing by fade specialists.",
        pricePaise = 35000L,
        durationMinutes = 40L,
        isActive = true,
        isPackage = false,
        instructions = "Cordless clippers & foil shaver brought fully charged by your specialist.",
        sortOrder = 4L
    ),
    CatalogService(
        id = "svc_crew_buzz_cut",
        name = "Crew Cut / Uniform Buzz Cut",
        category = "HAIRCUTS",
        description = "Low-maintenance military crew or single-guard buzz cut with clean temple taper, crisp forehead line-up, and quick post-cut brush-off.",
        pricePaise = 17900L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        instructions = "Quick 20-minute mess-free cut over disposable floor sheet.",
        sortOrder = 5L
    ),
    CatalogService(
        id = "svc_kids_cut",
        name = "Kids' Gentle Home Haircut",
        category = "HAIRCUTS",
        description = "Calm, patient, low-noise clipper & rounded-tip scissor haircut at home for children under 12—zero salon tears or waiting.",
        pricePaise = 25000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Parent or guardian should remain nearby during the haircut.",
        sortOrder = 6L
    ),
    CatalogService(
        id = "svc_senior_cut",
        name = "Senior Comfort Traditional Cut",
        category = "HAIRCUTS",
        description = "Unhurried, respectful traditional scissor haircut right on your elder's favourite home chair with zero travel strain.",
        pricePaise = 25000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Can be performed on an armchair or wheelchair at total comfort.",
        sortOrder = 7L
    ),
    CatalogService(
        id = "svc_beard_trim",
        name = "Beard Sculpting & Razor Line-Up",
        category = "BEARD_MOUSTACHE",
        description = "Symmetrical beard length grading, sharp cheek & jawline razor detailing, moustache neatening, and soothing aftershave splash.",
        pricePaise = 15000L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        instructions = "Fresh single-use disposable razor blade unsealed in front of you.",
        sortOrder = 8L
    ),
    CatalogService(
        id = "svc_traditional_shave",
        name = "Traditional Foam & Razor Shave",
        category = "BEARD_MOUSTACHE",
        description = "Classic rich-lather brush shave using a fresh disposable straight blade, warm water prep, antiseptic alum block, and cooling lotion.",
        pricePaise = 12000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        instructions = "Keep a small bowl of warm water available for lather prep.",
        sortOrder = 9L
    ),
    CatalogService(
        id = "svc_clean_shave",
        name = "Hot Towel Luxury Clean Shave",
        category = "BEARD_MOUSTACHE",
        description = "Two-pass aromatic steam-towel shave with pre-shave softening oil, rich cream lather, and hydrating post-shave balm.",
        pricePaise = 18000L,
        durationMinutes = 25L,
        isActive = true,
        isPackage = false,
        instructions = "Warm water access helps prepare the hot steam towel.",
        sortOrder = 10L
    ),
    CatalogService(
        id = "svc_scalp_oil",
        name = "Traditional Champi Head Massage",
        category = "HAIR_CARE",
        description = "20-minute stress-relieving Indian champi pressure-point scalp, forehead & shoulder massage with Navratna, coconut, or almond oil.",
        pricePaise = 20000L,
        durationMinutes = 20L,
        isActive = true,
        isPackage = false,
        instructions = "Barber brings sealed hair oils—or you may provide your preferred home oil.",
        sortOrder = 11L
    ),
    CatalogService(
        id = "svc_hair_wash",
        name = "Refreshing Hair Wash & Blow Dry",
        category = "HAIR_CARE",
        description = "Deep cleansing shampoo rinse to clear loose hair clippings followed by a styled volume blow-dry finish.",
        pricePaise = 10000L,
        durationMinutes = 15L,
        isActive = true,
        isPackage = false,
        instructions = "Requires washbasin/shower rinse access and a standard wall socket.",
        sortOrder = 12L
    ),
    CatalogService(
        id = "svc_face_cleanup",
        name = "Charcoal Detan Face Cleanup",
        category = "ADD_ONS",
        description = "4-step tan-removal cleanser, blackhead exfoliating scrub, activated charcoal mask, and brightening moisture massage.",
        pricePaise = 35000L,
        durationMinutes = 30L,
        isActive = true,
        isPackage = false,
        instructions = "Single-use sealed skincare sachets used for complete hygiene.",
        sortOrder = 13L
    ),
    CatalogService(
        id = "svc_brow_neck",
        name = "Eyebrow, Ear & Neck Detailing",
        category = "ADD_ONS",
        description = "Crisp precision trimming and razor cleanup around eyebrows, ears, and nape for a sharp finishing touch.",
        pricePaise = 8000L,
        durationMinutes = 10L,
        isActive = true,
        isPackage = false,
        instructions = "Pairs great as a quick add-on with any haircut.",
        sortOrder = 14L
    ),
    CatalogService(
        id = "svc_pkg_traditional_combo",
        name = "Normal / Traditional Cut + Shave + Champi",
        category = "PACKAGES",
        description = "Complete traditional home value combo: Normal or Traditional Haircut + Classic Shave/Beard Trim + 15-min Oil Champi Massage.",
        pricePaise = 42900L,
        durationMinutes = 55L,
        isActive = true,
        isPackage = true,
        instructions = "Save ₹90 compared to individual items • ₹0 platform fee.",
        sortOrder = 15L
    ),
    CatalogService(
        id = "svc_pkg_essential",
        name = "Haircut + Beard + Hair Wash Combo",
        category = "PACKAGES",
        description = "Signature at-home package: Precision Scissor/Clipper Haircut + Razor Beard Sculpt + Refreshing Wash & Blow-Dry.",
        pricePaise = 55000L,
        durationMinutes = 60L,
        isActive = true,
        isPackage = true,
        instructions = "Bestseller 60-minute doorstep transformation.",
        sortOrder = 16L
    ),
    CatalogService(
        id = "svc_pkg_complete",
        name = "Executive Complete Grooming Package",
        category = "PACKAGES",
        description = "Ultimate luxury ritual: Skin Fade or Executive Haircut + Hot Towel Shave/Beard + Charcoal Detan Facial + Champi Massage.",
        pricePaise = 89900L,
        durationMinutes = 90L,
        isActive = true,
        isPackage = true,
        instructions = "90-minute head-to-toe salon experience at home.",
        sortOrder = 17L
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
                "Post-service cleanup & direct pay to barber (₹0 platform fee)"
            ),
            toolsAndHygiene = listOf("Sanitized Kit", "Single-Use Disposables", "₹0 Platform Fee", "Pay Card/UPI/Cash at Door")
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
            // Hero Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = visual.drawableRes),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
                Surface(
                    color = EspressoHeaderBg.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = visual.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFDF9E),
                        fontWeight = FontWeight.Bold,
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
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close details", tint = Color.White)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = service.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = details.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFDF9E)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = BrassGold,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(service.pricePaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EspressoHeaderBg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Key Specs Strip (Duration, Platform Fee, Ideal For)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                StatusBadge("₹0 PLATFORM FEE")
            }

            // Full Description
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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

            // Transparent Pricing Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Barber Service Charge", style = MaterialTheme.typography.bodySmall)
                        Text(
                            CurrencyUtils.formatRupees(service.pricePaise),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Customer Platform / App Fee",
                            style = MaterialTheme.typography.bodySmall,
                            color = SageVerified
                        )
                        Text(
                            "₹0 FREE",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SageVerified
                        )
                    }
                    Text(
                        text = "Pay directly at your door via Credit/Debit Card POS, UPI QR, or Cash after completion.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
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
                            "In Cart • Remove"
                        } else {
                            "Add to Cart • ${CurrencyUtils.formatRupees(service.pricePaise)}"
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
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("View Cart", fontWeight = FontWeight.Bold)
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

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("phone_permissions_prompt_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
            SageVerified.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (granted) SageVerified.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
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
