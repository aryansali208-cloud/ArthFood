package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import com.example.data.model.CatalogItem
import com.example.data.model.CustomerSummary
import com.example.data.model.LicenseState
import com.example.data.model.ManageTab
import com.example.data.model.PackSize
import com.example.data.model.Vendor
import com.example.data.model.VendorPayment
import com.example.i18n.AppStrings
import com.example.licensing.LicenseDiagnosticState
import com.example.ui.AppViewModel
import com.example.ui.theme.BasilGreenDark

@Composable
fun ManageScreen(
    viewModel: AppViewModel,
    strings: AppStrings,
    activeTab: ManageTab,
    profile: BusinessProfile,
    appLanguage: AppLanguage,
    invoiceLanguage: AppLanguage,
    licenseState: LicenseState,
    diagnosticState: LicenseDiagnosticState = LicenseDiagnosticState(),
    lockedRates: Boolean,
    stockTracking: Boolean,
    packSizes: List<Int>,
    packSizeDetails: List<PackSize>,
    catalogItems: List<CatalogItem>,
    vendors: List<Vendor>,
    vendorPayments: List<VendorPayment>,
    customers: List<CustomerSummary>,
    autoBackupFolderUri: String,
    hasPin: Boolean
) {
    val tabs = listOf(
        ManageTab.BUSINESS to strings.manageBusiness,
        ManageTab.RATES_PROFIT to strings.manageRatesProfit,
        ManageTab.VENDORS to strings.manageVendors,
        ManageTab.CUSTOMERS to strings.manageCustomers,
        ManageTab.ORDER_QR to strings.manageOrderQr,
        ManageTab.BACKUP_EXCEL to strings.manageBackupExcel
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Crisp horizontal scrollable pill sub-tab bar
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (tab, title) ->
                        val selected = tab == activeTab
                        Surface(
                            onClick = { viewModel.selectManageTab(tab) },
                            shape = RoundedCornerShape(99.dp),
                            color = if (selected) BasilGreenDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("manage_subtab_${tab.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        when (activeTab) {
            ManageTab.BUSINESS -> ManageBusinessTab(
                strings = strings,
                profile = profile,
                appLanguage = appLanguage,
                invoiceLanguage = invoiceLanguage,
                packSizes = packSizeDetails,
                onAppLanguageChange = viewModel::setAppLanguage,
                onInvoiceLanguageChange = viewModel::setInvoiceLanguage,
                onPickLogo = viewModel::pickAndSaveLogo,
                onRemoveLogo = viewModel::removeLogo,
                onSaveProfile = {
                    viewModel.saveBusinessProfile(it)
                    viewModel.showMessage("Business & invoice settings saved!")
                },
                onAddOrUpdatePackSize = { grams, customName, origGrams ->
                    if (origGrams != null && origGrams != grams) {
                        viewModel.deletePackSize(origGrams)
                    }
                    viewModel.addPackSize(grams, customName)
                    viewModel.showMessage("Pack size saved!")
                },
                onDeletePackSize = { g ->
                    viewModel.deletePackSize(g)
                    viewModel.showMessage("Pack size removed")
                }
            )

            ManageTab.RATES_PROFIT -> ManageRatesProfitTab(
                strings = strings,
                lockedRates = lockedRates,
                stockTracking = stockTracking,
                packSizes = packSizes,
                catalogItems = catalogItems,
                vendors = vendors,
                onToggleLockedRates = viewModel::setLockedRates,
                onToggleStockTracking = viewModel::setStockTracking,
                onSaveCatalogItem = viewModel::saveCatalogItem,
                onDeleteCatalogItem = viewModel::deleteCatalogItem,
                onAddStockToItem = viewModel::addStockToItem,
                dateRangeBounds = viewModel::dateRangeBounds,
                dateRangeLabel = viewModel::dateRangeLabel,
                computeProfit = viewModel::computeProfit,
                onShareProfitJpg = viewModel::openProfitReportPreview
            )

            ManageTab.VENDORS -> ManageVendorsTab(
                strings = strings,
                vendors = vendors,
                catalogItems = catalogItems,
                vendorPayments = vendorPayments,
                onSaveVendor = viewModel::saveVendor,
                onDeleteVendor = viewModel::deleteVendor,
                onAssignVendorToItem = viewModel::assignVendorToCatalogItem,
                onMarkVendorLinesPaid = viewModel::markVendorLinesPaid,
                onRecordPayment = viewModel::recordVendorPayment,
                dateRangeBounds = viewModel::dateRangeBounds,
                dateRangeLabel = viewModel::dateRangeLabel,
                computePayables = viewModel::computeVendorPayables,
                onShareVendorStatement = viewModel::openVendorStatementPreview
            )

            ManageTab.CUSTOMERS -> ManageCustomersTab(
                strings = strings,
                profile = profile,
                customers = customers,
                onViewCustomerOrders = viewModel::viewCustomerOrders
            )

            ManageTab.ORDER_QR -> ManageOrderQrTab(
                strings = strings,
                profile = profile,
                onShareQrPosterJpg = viewModel::openQrPosterPreview
            )

            ManageTab.BACKUP_EXCEL -> ManageBackupExcelTab(
                strings = strings,
                licenseState = licenseState,
                diagnosticState = diagnosticState,
                autoBackupFolderUri = autoBackupFolderUri,
                hasPin = hasPin,
                onExportJsonFile = viewModel::shareJsonBackup,
                onCopyBackupText = viewModel::buildBackupJsonText,
                onInspectBackupJson = viewModel::inspectBackupJson,
                onReadUriText = viewModel::readUriText,
                onConfirmMergeRestore = { json -> viewModel.restoreBackupJson(json) },
                dateRangeBounds = viewModel::dateRangeBounds,
                dateRangeLabel = viewModel::dateRangeLabel,
                onExportExcel = viewModel::exportExcelWorkbook,
                onPickAutoBackupFolder = viewModel::setAutoBackupFolder,
                onTriggerAutoBackupNow = viewModel::triggerAutoBackupNow,
                onSetPin = viewModel::setPin,
                onDisablePin = viewModel::disablePin,
                onOpenLicenseModal = viewModel::openLicenseModal
            )
        }
    }
}
