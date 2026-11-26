package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import com.example.data.model.PackSize
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.util.Formatters
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageBusinessTab(
    strings: AppStrings,
    profile: BusinessProfile,
    appLanguage: AppLanguage,
    invoiceLanguage: AppLanguage,
    packSizes: List<PackSize>,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onInvoiceLanguageChange: (AppLanguage) -> Unit,
    onPickLogo: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onSaveProfile: (BusinessProfile) -> Unit,
    onAddOrUpdatePackSize: (grams: Int, customName: String, originalGrams: Int?) -> Unit,
    onDeletePackSize: (Int) -> Unit
) {
    var businessName by remember(profile) { mutableStateOf(profile.businessName) }
    var localBusinessName by remember(profile) { mutableStateOf(profile.localBusinessName) }
    var tagline by remember(profile) { mutableStateOf(profile.tagline) }
    var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var waSame by remember(profile) { mutableStateOf(profile.whatsappSameAsPhone) }
    var waNumber by remember(profile) { mutableStateOf(profile.whatsappNumber) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var city by remember(profile) { mutableStateOf(profile.city) }
    var stateName by remember(profile) { mutableStateOf(profile.state) }
    var pinCode by remember(profile) { mutableStateOf(profile.pinCode) }
    var upiId by remember(profile) { mutableStateOf(profile.upiId) }
    var taxStr by remember(profile) { mutableStateOf(if (profile.taxPercent > 0) profile.taxPercent.toString() else "") }
    var taxLine by remember(profile) { mutableStateOf(profile.taxLine) }
    var thankYou by remember(profile) { mutableStateOf(profile.thankYouNote) }
    var qrContact by remember(profile) { mutableStateOf(profile.qrContactName) }
    var businessNameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }

    // Pack Sizes CRUD state
    var editingPackGrams by remember { mutableStateOf<Int?>(null) }
    var packWeightText by remember { mutableStateOf("") }
    var packCustomName by remember { mutableStateOf("") }
    var packWeightErr by remember { mutableStateOf<String?>(null) }
    var pendingDeletePack by remember { mutableStateOf<PackSize?>(null) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickLogo(uri) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Languages Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.appLanguageLabel, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = lang == appLanguage,
                            onClick = { onAppLanguageChange(lang) },
                            label = { Text(lang.nativeLabel) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                Text(strings.invoiceLanguageLabel, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = lang == invoiceLanguage,
                            onClick = { onInvoiceLanguageChange(lang) },
                            label = { Text(lang.nativeLabel) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }

        // Business & Invoice Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile.logoPath.isNotBlank() && File(profile.logoPath).exists()) {
                        AsyncImage(
                            model = File(profile.logoPath),
                            contentDescription = strings.businessLogoLabel,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BasilGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = BasilGreen)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(strings.businessLogoLabel, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text(strings.pickLogo) }
                            if (profile.logoPath.isNotBlank()) {
                                TextButton(
                                    onClick = onRemoveLogo,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) { Text(strings.removeLogo) }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it; businessNameErr = null },
                    label = { Text(strings.businessName) },
                    isError = businessNameErr != null,
                    supportingText = businessNameErr?.let { err -> { Text(err) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = localBusinessName,
                    onValueChange = { localBusinessName = it },
                    label = { Text("Local Name (Hindi / Marathi, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text(strings.tagline) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(strings.ownerName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneErr = null },
                    label = { Text(strings.phoneNumber) },
                    isError = phoneErr != null,
                    supportingText = phoneErr?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { waSame = !waSame }
                ) {
                    Checkbox(checked = waSame, onCheckedChange = { waSame = it })
                    Text(strings.whatsappSameAsPhone, style = MaterialTheme.typography.bodyMedium)
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
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.address) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Area") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = stateName,
                        onValueChange = { stateName = it },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("PIN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxStr,
                        onValueChange = { taxStr = it },
                        label = { Text(strings.taxPercentOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text(strings.upiIdOptional) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxLine,
                    onValueChange = { taxLine = it },
                    label = { Text("Tax Line (on invoice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = thankYou,
                    onValueChange = { thankYou = it },
                    label = { Text(strings.thankYouNoteLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qrContact,
                    onValueChange = { qrContact = it },
                    label = { Text(strings.qrContactNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        businessNameErr = if (businessName.isBlank()) "Business name is required" else null
                        phoneErr = if (phone.isBlank()) "Phone number is required" else null
                        if (businessNameErr == null && phoneErr == null) {
                            onSaveProfile(
                                profile.copy(
                                    businessName = businessName.trim(),
                                    localBusinessName = localBusinessName.trim(),
                                    tagline = tagline.trim(),
                                    ownerName = ownerName.trim(),
                                    phone = phone.trim(),
                                    whatsappSameAsPhone = waSame,
                                    whatsappNumber = waNumber.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    state = stateName.trim(),
                                    pinCode = pinCode.trim(),
                                    upiId = upiId.trim(),
                                    taxPercent = taxStr.toDoubleOrNull() ?: 0.0,
                                    taxLine = taxLine.trim(),
                                    thankYouNote = thankYou.trim(),
                                    qrContactName = qrContact.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_business_profile_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.saveChanges)
                }
            }
        }

        // Pack Sizes Management Card (Weight in grams + Optional Name)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.packSizesGrams, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    packSizes.forEach { pack ->
                        val label = if (pack.customName.isNotBlank()) {
                            "${Formatters.formatGrams(pack.grams)} (${pack.customName})"
                        } else {
                            Formatters.formatGrams(pack.grams)
                        }
                        InputChip(
                            selected = editingPackGrams == pack.grams,
                            onClick = {
                                editingPackGrams = pack.grams
                                packWeightText = pack.grams.toString()
                                packCustomName = pack.customName
                                packWeightErr = null
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(label, fontWeight = FontWeight.Medium)
                                    Icon(Icons.Default.Edit, contentDescription = "Edit pack", modifier = Modifier.size(13.dp))
                                }
                            },
                            trailingIcon = {
                                if (packSizes.size > 1) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove pack",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { pendingDeletePack = pack }
                                    )
                                }
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = packWeightText,
                        onValueChange = { packWeightText = it.filter { c -> c.isDigit() }; packWeightErr = null },
                        label = { Text("Weight (g)") },
                        isError = packWeightErr != null,
                        supportingText = packWeightErr?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packCustomName,
                        onValueChange = { packCustomName = it },
                        label = { Text("Optional Name") },
                        placeholder = { Text("e.g. Jar, Pouch") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingPackGrams != null) {
                        OutlinedButton(
                            onClick = {
                                editingPackGrams = null
                                packWeightText = ""
                                packCustomName = ""
                                packWeightErr = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text(strings.close) }
                    }
                    FilledTonalButton(
                        onClick = {
                            val g = packWeightText.toIntOrNull()
                            if (g == null || g <= 0) {
                                packWeightErr = "Enter weight > 0 g"
                            } else {
                                onAddOrUpdatePackSize(g, packCustomName.trim(), editingPackGrams)
                                editingPackGrams = null
                                packWeightText = ""
                                packCustomName = ""
                                packWeightErr = null
                            }
                        },
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("save_pack_size_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (editingPackGrams != null) strings.saveChanges else strings.addPackSize)
                    }
                }
            }
        }
    }

    pendingDeletePack?.let { target ->
        ConfirmDialog(
            title = "Remove ${Formatters.formatGrams(target.grams)} pack?",
            message = "Existing saved orders will still retain this pack size.",
            confirmLabel = "Remove",
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onDeletePackSize(target.grams)
                if (editingPackGrams == target.grams) {
                    editingPackGrams = null
                    packWeightText = ""
                    packCustomName = ""
                }
                pendingDeletePack = null
            },
            onDismiss = { pendingDeletePack = null }
        )
    }
}
