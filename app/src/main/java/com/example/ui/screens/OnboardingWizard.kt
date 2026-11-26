package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import com.example.data.model.CatalogItem
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.export.ImageRenderer
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.Saffron
import com.example.ui.theme.SaffronLight
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingWizard(
    strings: AppStrings,
    appLanguage: AppLanguage,
    invoiceLanguage: AppLanguage,
    initialProfile: BusinessProfile,
    packSizes: List<Int>,
    catalogItems: List<CatalogItem>,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onInvoiceLanguageChange: (AppLanguage) -> Unit,
    onPickLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onAddPackSize: (Int, String) -> Unit,
    onDeletePackSize: (Int) -> Unit,
    onSaveCatalogItem: (CatalogItem) -> Unit,
    onDeleteCatalogItem: (Long) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onRestoreBackupText: (String) -> Unit,
    onComplete: (BusinessProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) }
    var businessNameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }
    var addressErr by remember { mutableStateOf<String?>(null) }
    var cityErr by remember { mutableStateOf<String?>(null) }
    var packErr by remember { mutableStateOf<String?>(null) }
    var itemNameErr by remember { mutableStateOf<String?>(null) }
    var itemPriceErr by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<CatalogItem?>(null) }
    var pasteJsonErr by remember { mutableStateOf<String?>(null) }

    var businessName by remember(initialProfile) { mutableStateOf(if (initialProfile.businessName == "ArthFood Kitchen") "" else initialProfile.businessName) }
    var localBusinessName by remember(initialProfile) { mutableStateOf(initialProfile.localBusinessName) }
    var tagline by remember(initialProfile) { mutableStateOf(initialProfile.tagline) }
    var ownerName by remember(initialProfile) { mutableStateOf(initialProfile.ownerName) }
    var phone by remember(initialProfile) { mutableStateOf(initialProfile.phone) }
    var waSame by remember(initialProfile) { mutableStateOf(initialProfile.whatsappSameAsPhone) }
    var waNumber by remember(initialProfile) { mutableStateOf(initialProfile.whatsappNumber) }

    var address by remember(initialProfile) { mutableStateOf(initialProfile.address) }
    var city by remember(initialProfile) { mutableStateOf(initialProfile.city) }
    var stateName by remember(initialProfile) { mutableStateOf(initialProfile.state) }
    var pinCode by remember(initialProfile) { mutableStateOf(initialProfile.pinCode) }
    var taxLine by remember(initialProfile) { mutableStateOf(initialProfile.taxLine) }
    var thankYou by remember(initialProfile) { mutableStateOf(initialProfile.thankYouNote) }
    var qrContact by remember(initialProfile) { mutableStateOf(initialProfile.qrContactName) }

    var showPasteRestoreDialog by remember { mutableStateOf(false) }
    var pasteJson by remember { mutableStateOf("") }
    var samplePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Step 4 drafts
    var newPackGrams by remember { mutableStateOf("") }
    var newPackName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemSellPrice by remember { mutableStateOf("") }
    var itemVendorCost by remember { mutableStateOf("") }
    var itemVendorName by remember { mutableStateOf("") }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickLogo(uri) }

    val jsonFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportBackupUri(uri) }

    BackHandler(enabled = step > 1) {
        businessNameErr = null
        phoneErr = null
        addressErr = null
        cityErr = null
        step--
    }

    fun currentDraftProfile(): BusinessProfile = initialProfile.copy(
        businessName = businessName.trim().ifBlank { "ArthFood Kitchen" },
        localBusinessName = localBusinessName.trim(),
        tagline = tagline.trim().ifBlank { strings.appTaglineDefault },
        ownerName = ownerName.trim(),
        phone = phone.trim(),
        whatsappSameAsPhone = waSame,
        whatsappNumber = waNumber.trim(),
        address = address.trim(),
        city = city.trim(),
        state = stateName.trim(),
        pinCode = pinCode.trim(),
        taxLine = taxLine.trim(),
        thankYouNote = thankYou.trim(),
        qrContactName = qrContact.trim()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Unified Basil Green Header + 4-Segment Progress Bar
        Surface(color = BasilGreenDark) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.arthfood_icon_fg),
                        contentDescription = "ArthFood Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.onboardingWelcome, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("ArthFood · ${strings.onboardingStepOf(step, 4)}", style = MaterialTheme.typography.bodySmall, color = SaffronLight)
                    }
                    // Numbered circular badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Saffron),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$step", fontWeight = FontWeight.Bold, color = Color(0xFF1A2419))
                    }
                }
                // 4-Segment Visual Progress Bar
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    for (idx in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (idx <= step) Saffron else Color.White.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(
                            text = "Let's set up your business. It takes about 2 minutes and everything stays on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("App language (screens and buttons)", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    AppLanguage.entries.forEach { lang ->
                                        val selected = lang == appLanguage
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) BasilGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.weight(1f).clickable { onAppLanguageChange(lang) }
                                        ) {
                                            Text(
                                                text = lang.nativeLabel,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Text("Invoice language (customer invoice, receipt and reports)", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    AppLanguage.entries.forEach { lang ->
                                        val selected = lang == invoiceLanguage
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) Saffron else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.weight(1f).clickable { onInvoiceLanguageChange(lang) }
                                        ) {
                                            Text(
                                                text = lang.nativeLabel,
                                                color = Color(0xFF1A2419),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "You can change both later in Manage → Business / Invoice.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = BasilGreenLight),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BasilGreen.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Restore, contentDescription = null, tint = BasilGreenDark)
                                    Spacer(Modifier.width(8.dp))
                                    Text("I already have a backup — Restore it", style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { jsonFilePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                        modifier = Modifier.weight(1f).testTag("onboarding_import_json")
                                    ) { Text(strings.importBackupFile) }
                                    FilledTonalButton(
                                        onClick = { showPasteRestoreDialog = true },
                                        modifier = Modifier.weight(1f).testTag("onboarding_paste_json")
                                    ) { Text(strings.pasteBackupJson) }
                                }
                            }
                        }
                    }

                    2 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it; businessNameErr = null },
                                    label = { Text("Business name (English) *") },
                                    isError = businessNameErr != null,
                                    supportingText = businessNameErr?.let { err -> { Text(err) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_business_name")
                                )
                                OutlinedTextField(
                                    value = localBusinessName,
                                    onValueChange = { localBusinessName = it },
                                    label = { Text("Name in local language (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = tagline,
                                    onValueChange = { tagline = it },
                                    label = { Text("Tagline (optional)") },
                                    placeholder = { Text("e.g. Home-made snacks & sweets") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text("Contact person name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it; phoneErr = null },
                                    label = { Text("Phone number *") },
                                    placeholder = { Text("e.g. 98765 43210 or +91...") },
                                    isError = phoneErr != null,
                                    supportingText = phoneErr?.let { err -> { Text(err) } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_phone")
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { waSame = !waSame }
                                ) {
                                    Checkbox(checked = waSame, onCheckedChange = { waSame = it })
                                    Text(
                                        "This number is on WhatsApp (show WhatsApp after the number)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!waSame) {
                                    OutlinedTextField(
                                        value = waNumber,
                                        onValueChange = { waNumber = it },
                                        label = { Text(strings.whatsappNumber) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it; addressErr = null },
                                    label = { Text("Address (building, road, landmark) *") },
                                    isError = addressErr != null,
                                    supportingText = addressErr?.let { err -> { Text(err) } },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it; cityErr = null },
                                    label = { Text("City / area *") },
                                    isError = cityErr != null,
                                    supportingText = cityErr?.let { err -> { Text(err) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = stateName,
                                        onValueChange = { stateName = it },
                                        label = { Text("State") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = pinCode,
                                        onValueChange = { pinCode = it.filter { c -> c.isDigit() }.take(6) },
                                        label = { Text("PIN code") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = taxLine,
                                    onValueChange = { taxLine = it },
                                    label = { Text("Tax line (on invoice) (optional)") },
                                    placeholder = { Text("e.g. No GST charged or GSTIN: ...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = thankYou,
                                    onValueChange = { thankYou = it },
                                    label = { Text("Thank-you line (optional)") },
                                    placeholder = { Text("Empty = automatic thank-you line") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = qrContact,
                                    onValueChange = { qrContact = it },
                                    label = { Text("Name saved in the phone when the QR is scanned (optional)") },
                                    placeholder = { Text("Leave empty to use \"Contact name - Business name\"") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (initialProfile.logoPath.isNotBlank() && File(initialProfile.logoPath).exists()) {
                                        AsyncImage(
                                            model = File(initialProfile.logoPath),
                                            contentDescription = strings.businessLogoLabel,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BasilGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = BasilGreen)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Business logo (optional)", style = MaterialTheme.typography.titleSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                }
                                            ) { Text(strings.pickLogo) }
                                            if (initialProfile.logoPath.isNotBlank()) {
                                                TextButton(onClick = onRemoveLogo) { Text(strings.removeLogo) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.packSizesGrams, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Enter weight in grams (e.g. 250, 500, 1000). Pack price = rate per kg × weight. Empty name shows automatically as '250 g', '1 kg'. Saved orders are not affected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    packSizes.forEach { grams ->
                                        InputChip(
                                            selected = true,
                                            onClick = {},
                                            label = { Text(ImageRenderer.formatGrams(grams)) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove pack",
                                                    modifier = Modifier.size(16.dp).clickable { onDeletePackSize(grams) }
                                                )
                                            }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newPackGrams,
                                        onValueChange = { newPackGrams = it.filter { c -> c.isDigit() }; packErr = null },
                                        label = { Text("Grams") },
                                        isError = packErr != null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = newPackName,
                                        onValueChange = { newPackName = it },
                                        label = { Text("Name (optional)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val g = newPackGrams.toIntOrNull()
                                            if (g != null && g > 0) {
                                                onAddPackSize(g, newPackName)
                                                newPackGrams = ""; newPackName = ""; packErr = null
                                            } else {
                                                packErr = "Enter valid grams (> 0)"
                                            }
                                        },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add pack size")
                                    }
                                }
                                if (packErr != null) {
                                    Text(
                                        text = packErr!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Your Items", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = { itemName = it; itemNameErr = null },
                                    label = { Text(strings.itemName) },
                                    isError = itemNameErr != null,
                                    supportingText = itemNameErr?.let { err -> { Text(err) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = itemSellPrice,
                                        onValueChange = { itemSellPrice = it; itemPriceErr = null },
                                        label = { Text(strings.sellingPricePerKg) },
                                        isError = itemPriceErr != null,
                                        supportingText = itemPriceErr?.let { err -> { Text(err) } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = itemVendorCost,
                                        onValueChange = { itemVendorCost = it },
                                        label = { Text(strings.vendorCostPerKg) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = itemVendorName,
                                        onValueChange = { itemVendorName = it },
                                        label = { Text(strings.vendorNameLabel) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val sell = itemSellPrice.toDoubleOrNull() ?: 0.0
                                            val cost = itemVendorCost.toDoubleOrNull() ?: 0.0
                                            var valid = true
                                            if (itemName.isBlank()) {
                                                itemNameErr = "Enter item name"
                                                valid = false
                                            }
                                            if (sell <= 0.0) {
                                                itemPriceErr = "Rate must be > ₹0"
                                                valid = false
                                            }
                                            if (valid) {
                                                onSaveCatalogItem(
                                                    CatalogItem(
                                                        name = itemName.trim(),
                                                        sellingPricePerKg = Formatters.round2(sell),
                                                        vendorCostPerKg = Formatters.round2(cost),
                                                        vendorName = itemVendorName.trim()
                                                    )
                                                )
                                                itemName = ""; itemSellPrice = ""; itemVendorCost = ""; itemVendorName = ""
                                                itemNameErr = null; itemPriceErr = null
                                            }
                                        },
                                        modifier = Modifier.height(48.dp)
                                    ) { Text(strings.addItem) }
                                }

                                catalogItems.forEach { c ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "Sell ${Formatters.formatINR(c.sellingPricePerKg)}/kg • Cost ${Formatters.formatINR(c.vendorCostPerKg)}/kg",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { pendingDeleteItem = c }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val draft = currentDraftProfile()
                                        scope.launch {
                                            samplePreviewBitmap = withContext(Dispatchers.Default) {
                                                val sampleItem = catalogItems.firstOrNull()
                                                val samplePrice = Formatters.round2(sampleItem?.let { it.sellingPricePerKg * 0.5 } ?: 250.0)
                                                val sampleOrder = OrderRecord(
                                                    invoiceNumber = "AF-2026-0001",
                                                    customerName = "Sneha Kulkarni",
                                                    customerPhone = "+91 9876543210",
                                                    status = OrderStatus.PACKED,
                                                    paymentStatus = PaymentStatus.PAID,
                                                    subtotal = samplePrice,
                                                    grandTotal = samplePrice,
                                                    items = listOf(
                                                        OrderLineItem(
                                                            itemName = sampleItem?.name ?: "Homemade Mango Pickle",
                                                            packGrams = 500,
                                                            quantity = 1,
                                                            unitPrice = samplePrice,
                                                            sellingPricePerKg = sampleItem?.sellingPricePerKg ?: 500.0,
                                                            vendorCostPerKg = sampleItem?.vendorCostPerKg ?: 300.0
                                                        )
                                                    )
                                                )
                                                ImageRenderer.renderInvoiceJpg(context, sampleOrder, draft, invoiceLanguage)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Preview sample invoice")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar
        Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = {
                            businessNameErr = null; phoneErr = null; addressErr = null; cityErr = null
                            step--
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.back)
                    }
                }
                Button(
                    onClick = {
                        when (step) {
                            1 -> step = 2
                            2 -> {
                                businessNameErr = if (businessName.isBlank()) "Business name is required" else null
                                phoneErr = if (phone.isBlank()) "Phone number is required" else null
                                if (businessNameErr == null && phoneErr == null) step = 3
                            }
                            3 -> {
                                addressErr = if (address.isBlank()) "Address is required" else null
                                cityErr = if (city.isBlank()) "City or area is required" else null
                                if (addressErr == null && cityErr == null) step = 4
                            }
                            else -> onComplete(currentDraftProfile())
                        }
                    },
                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("onboarding_next_button")
                ) {
                    if (step == 4) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Finish and start")
                    } else {
                        Text(strings.next)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    pendingDeleteItem?.let { target ->
        ConfirmDialog(
            title = "Remove ${target.name}?",
            message = "This removes the item from your catalog. Existing saved orders are unaffected.",
            confirmLabel = "Remove",
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onDeleteCatalogItem(target.id)
                pendingDeleteItem = null
            },
            onDismiss = { pendingDeleteItem = null }
        )
    }

    if (showPasteRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showPasteRestoreDialog = false; pasteJsonErr = null },
            title = { Text(strings.pasteBackupJson) },
            text = {
                OutlinedTextField(
                    value = pasteJson,
                    onValueChange = { pasteJson = it; pasteJsonErr = null },
                    minLines = 5,
                    maxLines = 8,
                    isError = pasteJsonErr != null,
                    supportingText = pasteJsonErr?.let { err -> { Text(err) } },
                    placeholder = { Text("{ \"schema\": \"arthfood_backup_v1\", ... }") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pasteJson.isBlank()) {
                        pasteJsonErr = "Paste valid backup JSON first"
                    } else {
                        onRestoreBackupText(pasteJson)
                        showPasteRestoreDialog = false
                    }
                }) { Text(strings.restoreOrder) }
            },
            dismissButton = {
                TextButton(onClick = { showPasteRestoreDialog = false; pasteJsonErr = null }) { Text(strings.close) }
            }
        )
    }

    samplePreviewBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { samplePreviewBitmap = null },
            title = { Text(strings.sampleInvoicePreview) },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = strings.sampleInvoicePreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
            },
            confirmButton = {
                Button(onClick = { samplePreviewBitmap = null }) { Text(strings.close) }
            }
        )
    }
}
