package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MainTab
import com.example.ui.components.ArthFoodHeader
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.LicenseActivationScreen
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.TrialStatusBanner
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.NewOrderScreen
import com.example.ui.screens.OnboardingWizard
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.BasilGreen

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val invoiceLanguage by viewModel.invoiceLanguage.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val licenseState by viewModel.licenseState.collectAsStateWithLifecycle()
    val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()
    val showLicenseModal by viewModel.showLicenseModal.collectAsStateWithLifecycle()
    val pinHash by viewModel.pinHash.collectAsStateWithLifecycle()
    val isPinUnlocked by viewModel.isPinUnlocked.collectAsStateWithLifecycle()

    val activeTab by viewModel.activeMainTab.collectAsStateWithLifecycle()
    val activeManageTab by viewModel.activeManageTab.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val filteredOrders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val orderFilter by viewModel.orderFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val editingOrder by viewModel.editingOrder.collectAsStateWithLifecycle()

    val packSizes by viewModel.packSizes.collectAsStateWithLifecycle()
    val packSizeDetails by viewModel.packSizeDetails.collectAsStateWithLifecycle()
    val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val vendorPayments by viewModel.vendorPayments.collectAsStateWithLifecycle()
    val customers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val lockedRates by viewModel.lockedRates.collectAsStateWithLifecycle()
    val stockTracking by viewModel.stockTracking.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val autoBackupFolderUri by viewModel.autoBackupFolderUri.collectAsStateWithLifecycle()
    val previewImage by viewModel.previewImage.collectAsStateWithLifecycle()
    val isRenderingPreview by viewModel.isRenderingPreview.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Loading state while DataStore resolves initial onboarding flag
    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BasilGreen)
        }
        return
    }

    // 4-Digit PIN Lock Gate
    if (pinHash.isNotBlank() && !isPinUnlocked) {
        PinLockOverlay(strings = strings, onVerifyPin = viewModel::verifyPin)
        return
    }

    // Hard Lock Gate when 7-day trial expires or clock rollback detected during trial
    if (licenseState.isLockedOut) {
        LicenseActivationScreen(
            licenseState = licenseState,
            strings = strings,
            isHardLock = true,
            onVerifyKey = viewModel::verifyAndActivateLicense,
            onExportBackup = viewModel::shareJsonBackup,
            onExportDeviceReport = viewModel::shareDeviceReportJson,
            onDismissIfOptional = {}
        )
        return
    }

    // Optional License / Help Modal
    if (showLicenseModal) {
        BackHandler { viewModel.closeLicenseModal() }
        LicenseActivationScreen(
            licenseState = licenseState,
            strings = strings,
            isHardLock = false,
            onVerifyKey = viewModel::verifyAndActivateLicense,
            onExportBackup = viewModel::shareJsonBackup,
            onExportDeviceReport = viewModel::shareDeviceReportJson,
            onDismissIfOptional = viewModel::closeLicenseModal
        )
        return
    }

    // First-run 4-step Onboarding Wizard
    if (onboardingCompleted == false) {
        OnboardingWizard(
            strings = strings,
            appLanguage = appLanguage,
            invoiceLanguage = invoiceLanguage,
            initialProfile = profile,
            packSizes = packSizes,
            catalogItems = catalogItems,
            onAppLanguageChange = viewModel::setAppLanguage,
            onInvoiceLanguageChange = viewModel::setInvoiceLanguage,
            onPickLogo = viewModel::pickAndSaveLogo,
            onRemoveLogo = viewModel::removeLogo,
            onAddPackSize = viewModel::addPackSize,
            onDeletePackSize = viewModel::deletePackSize,
            onSaveCatalogItem = viewModel::saveCatalogItem,
            onDeleteCatalogItem = viewModel::deleteCatalogItem,
            onImportBackupUri = viewModel::importBackupFromUri,
            onRestoreBackupText = { json -> viewModel.restoreBackupJson(json) },
            onComplete = viewModel::completeOnboarding
        )
        return
    }

    BackHandler(enabled = activeTab != MainTab.ORDERS) {
        viewModel.selectMainTab(MainTab.ORDERS)
    }

    val navItems = listOf(
        Triple(MainTab.ORDERS, strings.tabOrders, Icons.Filled.ReceiptLong to Icons.Outlined.ReceiptLong),
        Triple(MainTab.NEW_ORDER, strings.tabNewOrder, Icons.Filled.AddCircle to Icons.Outlined.AddCircleOutline),
        Triple(MainTab.MANAGE, strings.tabManage, Icons.Filled.Tune to Icons.Outlined.Tune)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A2419),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = data.visuals.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            topBar = {
                Column {
                    ArthFoodHeader(
                        profile = profile,
                        strings = strings,
                        licenseState = licenseState,
                        appLanguage = appLanguage,
                        hasPin = pinHash.isNotBlank(),
                        onLanguageSelected = viewModel::setAppLanguage,
                        onLicenseClick = viewModel::openLicenseModal,
                        onLockClick = viewModel::lockAppNow,
                        diagnosticState = diagnosticState
                    )
                    TrialStatusBanner(
                        licenseState = licenseState,
                        onActivateClick = viewModel::openLicenseModal
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectMainTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectMainTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        when (activeTab) {
                            MainTab.ORDERS -> OrdersScreen(
                                strings = strings,
                                allOrders = orders,
                                filteredOrders = filteredOrders,
                                selectedFilter = orderFilter,
                                searchQuery = searchQuery,
                                lastBackupAt = lastBackupAt,
                                stockTracking = stockTracking,
                                catalogItems = catalogItems,
                                onSelectFilter = viewModel::setOrderFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onBackupBannerClick = viewModel::openBackupTab,
                                onLowStockBannerClick = viewModel::openRatesProfitTab,
                                onMarkOrderSent = viewModel::markOrderSent,
                                onUpdateStatus = viewModel::updateOrderStatus,
                                onUpdatePayment = viewModel::updateOrderPayment,
                                onOpenInvoiceJpg = viewModel::openInvoicePreview,
                                onEditOrder = viewModel::startEditOrder
                            )

                            MainTab.NEW_ORDER -> NewOrderScreen(
                                strings = strings,
                                profile = profile,
                                catalogItems = catalogItems,
                                packSizes = packSizes,
                                editingOrder = editingOrder,
                                onParseWhatsApp = viewModel::parseWhatsAppOrder,
                                onShowToast = viewModel::showMessage,
                                onCancelEdit = {
                                    viewModel.clearEditingOrder()
                                    viewModel.selectMainTab(MainTab.ORDERS)
                                },
                                onSaveOrder = viewModel::saveOrder
                            )

                            MainTab.MANAGE -> ManageScreen(
                                viewModel = viewModel,
                                strings = strings,
                                activeTab = activeManageTab,
                                profile = profile,
                                appLanguage = appLanguage,
                                invoiceLanguage = invoiceLanguage,
                                licenseState = licenseState,
                                diagnosticState = diagnosticState,
                                lockedRates = lockedRates,
                                stockTracking = stockTracking,
                                packSizes = packSizes,
                                packSizeDetails = packSizeDetails,
                                catalogItems = catalogItems,
                                vendors = vendors,
                                vendorPayments = vendorPayments,
                                customers = customers,
                                autoBackupFolderUri = autoBackupFolderUri,
                                hasPin = pinHash.isNotBlank()
                            )
                        }
                    }
                }
            }
        }
    }

    if (isRenderingPreview) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = BasilGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Text("Generating HD JPG…", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    previewImage?.let { payload ->
        ImagePreviewDialog(
            payload = payload,
            strings = strings,
            onDismiss = viewModel::dismissPreviewImage
        )
    }
}
