package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArthFoodApp
import com.example.data.model.AppLanguage
import com.example.data.model.BackupInspectionSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CatalogItem
import com.example.data.model.CustomerSummary
import com.example.data.model.DateRangePreset
import com.example.data.model.LicenseState
import com.example.data.model.MainTab
import com.example.data.model.ManageTab
import com.example.data.model.OrderFilter
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PackSize
import com.example.data.model.ParsedWhatsAppOrder
import com.example.data.model.PaymentStatus
import com.example.data.model.ProfitOrderScope
import com.example.data.model.ProfitSummary
import com.example.data.model.Vendor
import com.example.data.model.VendorPayableSummary
import com.example.data.model.VendorPayment
import com.example.export.ImageRenderer
import com.example.i18n.AppStrings
import com.example.i18n.StringsRepo
import com.example.licensing.LicenseDiagnosticState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PreviewImagePayload(
    val bitmap: Bitmap,
    val title: String,
    val fileName: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ArthFoodApp).container
    private val repo = container.repository
    private val prefs = container.preferencesStore
    private val licenseManager = container.licenseManager

    val appLanguage: StateFlow<AppLanguage> = prefs.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.EN)

    val invoiceLanguage: StateFlow<AppLanguage> = prefs.invoiceLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.EN)

    val strings: StateFlow<AppStrings> = appLanguage
        .map { StringsRepo.get(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StringsRepo.get(AppLanguage.EN))

    val onboardingCompleted: StateFlow<Boolean?> = prefs.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val businessProfile: StateFlow<BusinessProfile> = prefs.businessProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, BusinessProfile())

    val lockedRates: StateFlow<Boolean> = prefs.lockedRates
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val stockTracking: StateFlow<Boolean> = prefs.stockTracking
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastBackupAt: StateFlow<Long> = prefs.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val autoBackupFolderUri: StateFlow<String> = prefs.autoBackupFolderUri
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val pinHash: StateFlow<String> = prefs.pinHash
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val licenseState: StateFlow<LicenseState> = licenseManager.licenseState
        .stateIn(viewModelScope, SharingStarted.Eagerly, LicenseState())

    val diagnosticState: StateFlow<LicenseDiagnosticState> = licenseManager.diagnosticState

    val packSizes: StateFlow<List<Int>> = repo.packSizes
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(100, 250, 500, 1000))

    val packSizeDetails: StateFlow<List<PackSize>> = repo.packSizeDetails
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            listOf(100, 250, 500, 1000).map { PackSize(it) }
        )

    val vendors: StateFlow<List<Vendor>> = repo.vendors
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val catalogItems: StateFlow<List<CatalogItem>> = repo.catalogItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val orders: StateFlow<List<OrderRecord>> = repo.orders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val vendorPayments: StateFlow<List<VendorPayment>> = repo.vendorPayments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customerSummaries: StateFlow<List<CustomerSummary>> = orders
        .map { repo.computeCustomerSummaries(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isPinUnlocked = MutableStateFlow(false)
    val isPinUnlocked: StateFlow<Boolean> = _isPinUnlocked.asStateFlow()

    private val _activeMainTab = MutableStateFlow(MainTab.ORDERS)
    val activeMainTab: StateFlow<MainTab> = _activeMainTab.asStateFlow()

    private val _activeManageTab = MutableStateFlow(ManageTab.BUSINESS)
    val activeManageTab: StateFlow<ManageTab> = _activeManageTab.asStateFlow()

    private val _orderFilter = MutableStateFlow(OrderFilter.ALL)
    val orderFilter: StateFlow<OrderFilter> = _orderFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingOrder = MutableStateFlow<OrderRecord?>(null)
    val editingOrder: StateFlow<OrderRecord?> = _editingOrder.asStateFlow()

    private val _showLicenseModal = MutableStateFlow(false)
    val showLicenseModal: StateFlow<Boolean> = _showLicenseModal.asStateFlow()

    private val _previewImage = MutableStateFlow<PreviewImagePayload?>(null)
    val previewImage: StateFlow<PreviewImagePayload?> = _previewImage.asStateFlow()

    private val _isRenderingPreview = MutableStateFlow(false)
    val isRenderingPreview: StateFlow<Boolean> = _isRenderingPreview.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val filteredOrders: StateFlow<List<OrderRecord>> = combine(
        orders,
        _orderFilter,
        _searchQuery
    ) { list, filter, query ->
        val q = query.trim().lowercase()
        list.filter { order ->
            val matchesFilter = when (filter) {
                OrderFilter.ALL -> order.status != OrderStatus.CANCELLED
                OrderFilter.IN_PROCESS -> !order.isSent && order.status != OrderStatus.CANCELLED
                OrderFilter.PENDING -> order.status == OrderStatus.PENDING
                OrderFilter.PACKED -> order.status == OrderStatus.PACKED
                OrderFilter.DELIVERED -> order.isSent && order.status != OrderStatus.CANCELLED
                OrderFilter.PAID -> order.status != OrderStatus.CANCELLED && order.paymentStatus == PaymentStatus.PAID
                OrderFilter.UNPAID -> order.status != OrderStatus.CANCELLED && order.paymentStatus != PaymentStatus.PAID
                OrderFilter.SENT_UNPAID -> order.isSent && order.paymentStatus != PaymentStatus.PAID && order.status != OrderStatus.CANCELLED
                OrderFilter.CANCELLED -> order.status == OrderStatus.CANCELLED
            }
            val matchesSearch = q.isBlank() ||
                order.customerName.lowercase().contains(q) ||
                order.customerPhone.lowercase().contains(q) ||
                order.invoiceNumber.lowercase().contains(q) ||
                order.items.any { it.itemName.lowercase().contains(q) }
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            licenseManager.evaluateTrialOnLaunch()
        }
    }

    fun selectMainTab(tab: MainTab) {
        _activeMainTab.value = tab
    }

    fun selectManageTab(tab: ManageTab) {
        _activeManageTab.value = tab
    }

    fun openBackupTab() {
        _activeMainTab.value = MainTab.MANAGE
        _activeManageTab.value = ManageTab.BACKUP_EXCEL
    }

    fun openRatesProfitTab() {
        _activeMainTab.value = MainTab.MANAGE
        _activeManageTab.value = ManageTab.RATES_PROFIT
    }

    fun setOrderFilter(filter: OrderFilter) {
        _orderFilter.value = filter
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun startEditOrder(order: OrderRecord) {
        _editingOrder.value = order
        _activeMainTab.value = MainTab.NEW_ORDER
    }

    fun clearEditingOrder() {
        _editingOrder.value = null
    }

    fun openLicenseModal() {
        _showLicenseModal.value = true
    }

    fun closeLicenseModal() {
        _showLicenseModal.value = false
    }

    fun dismissPreviewImage() {
        _previewImage.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setAppLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefs.setAppLanguage(lang) }
    }

    fun setInvoiceLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefs.setInvoiceLanguage(lang) }
    }

    fun saveBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch { prefs.saveBusinessProfile(profile) }
    }

    fun completeOnboarding(profile: BusinessProfile) {
        viewModelScope.launch {
            prefs.saveBusinessProfile(profile)
            prefs.setOnboardingCompleted(true)
        }
    }

    fun pickAndSaveLogo(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repo.copyLogoToInternalStorage(uri)
            if (savedPath != null) {
                prefs.saveBusinessProfile(businessProfile.value.copy(logoPath = savedPath))
                showMessage("Logo updated!")
            }
        }
    }

    fun removeLogo() {
        viewModelScope.launch {
            prefs.saveBusinessProfile(businessProfile.value.copy(logoPath = ""))
        }
    }

    fun addPackSize(grams: Int, customName: String = "") {
        viewModelScope.launch { repo.addPackSize(grams, customName) }
    }

    fun deletePackSize(grams: Int) {
        viewModelScope.launch { repo.deletePackSize(grams) }
    }

    fun saveCatalogItem(item: CatalogItem) {
        viewModelScope.launch {
            repo.saveCatalogItem(item)
            showMessage("Item saved!")
        }
    }

    fun deleteCatalogItem(id: Long) {
        viewModelScope.launch {
            repo.deleteCatalogItem(id)
            showMessage("Item removed")
        }
    }

    fun addStockToItem(itemId: Long, deltaGrams: Int) {
        viewModelScope.launch {
            repo.addStockToItem(itemId, deltaGrams)
            showMessage("Stock updated!")
        }
    }

    fun assignVendorToCatalogItem(itemId: Long, vendor: Vendor?) {
        viewModelScope.launch {
            repo.assignVendorToCatalogItem(itemId, vendor)
            showMessage(
                if (vendor != null) "Assigned vendor: ${vendor.name}"
                else "Vendor unassigned"
            )
        }
    }

    fun saveVendor(vendor: Vendor) {
        viewModelScope.launch {
            repo.saveVendor(vendor)
            showMessage("Vendor saved!")
        }
    }

    fun deleteVendor(id: Long) {
        viewModelScope.launch {
            repo.deleteVendor(id)
            showMessage("Vendor deleted")
        }
    }

    fun setLockedRates(locked: Boolean) {
        viewModelScope.launch { prefs.setLockedRates(locked) }
    }

    fun setStockTracking(enabled: Boolean) {
        viewModelScope.launch { prefs.setStockTracking(enabled) }
    }

    fun parseWhatsAppOrder(text: String): ParsedWhatsAppOrder {
        return repo.parseWhatsAppOrder(text, catalogItems.value, packSizes.value)
    }

    fun saveOrder(order: OrderRecord, generateInvoiceAfter: Boolean = false) {
        viewModelScope.launch {
            licenseManager.evaluateTrialOnLaunch()
            val id = repo.saveOrder(order)
            _editingOrder.value = null
            showMessage(strings.value.orderSavedToast)
            _activeMainTab.value = MainTab.ORDERS
            if (generateInvoiceAfter) {
                val saved = orders.value.firstOrNull { it.id == id } ?: order.copy(id = id)
                openInvoicePreview(saved, isReceipt = saved.paymentStatus == PaymentStatus.PAID)
            }
        }
    }

    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        viewModelScope.launch {
            repo.updateOrderStatus(orderId, status)
            showMessage(
                when (status) {
                    OrderStatus.CANCELLED -> "Order cancelled"
                    OrderStatus.DELIVERED -> "Marked order sent"
                    else -> "Order status updated"
                }
            )
        }
    }

    fun markOrderSent(orderId: Long, sent: Boolean) {
        viewModelScope.launch {
            repo.updateOrderStatus(orderId, if (sent) OrderStatus.DELIVERED else OrderStatus.PENDING)
            showMessage(if (sent) "Marked order sent" else "Reverted order to in process")
        }
    }

    fun updateOrderPayment(orderId: Long, status: PaymentStatus, paidAmount: Double) {
        viewModelScope.launch {
            repo.updateOrderPayment(orderId, status, paidAmount)
            showMessage(
                when (status) {
                    PaymentStatus.PAID -> "Payment marked received"
                    PaymentStatus.PARTIAL -> "Partial payment saved"
                    PaymentStatus.UNPAID -> "Payment marked unpaid"
                }
            )
        }
    }

    fun recordVendorPayment(payment: VendorPayment) {
        viewModelScope.launch {
            repo.recordVendorPayment(payment)
            showMessage("Payment recorded!")
        }
    }

    fun markVendorLinesPaid(vendor: Vendor, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            repo.markVendorLinesPaid(vendor, startMs, endMs)
            showMessage("Marked ${vendor.name} order lines paid!")
        }
    }

    fun deleteVendorPayment(id: Long) {
        viewModelScope.launch { repo.deleteVendorPayment(id) }
    }

    fun viewCustomerOrders(customer: CustomerSummary) {
        _searchQuery.value = customer.phone.ifBlank { customer.name }
        _orderFilter.value = OrderFilter.ALL
        _activeMainTab.value = MainTab.ORDERS
    }

    fun dateRangeBounds(preset: DateRangePreset, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (preset) {
            DateRangePreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
            DateRangePreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            DateRangePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                start to System.currentTimeMillis()
            }
            DateRangePreset.ALL_TIME -> 0L to Long.MAX_VALUE
            DateRangePreset.CUSTOM -> minOf(customStart, customEnd) to (maxOf(customStart, customEnd) + 86_399_999L)
        }
    }

    fun dateRangeLabel(preset: DateRangePreset, customStart: Long, customEnd: Long): String {
        val s = strings.value
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return when (preset) {
            DateRangePreset.TODAY -> s.today
            DateRangePreset.THIS_WEEK -> s.thisWeek
            DateRangePreset.THIS_MONTH -> s.thisMonth
            DateRangePreset.ALL_TIME -> s.allTime
            DateRangePreset.CUSTOM -> "${fmt.format(Date(customStart))} - ${fmt.format(Date(customEnd))}"
        }
    }

    fun computeProfit(
        startMs: Long,
        endMs: Long,
        scope: ProfitOrderScope = ProfitOrderScope.ALL_ACTIVE
    ): ProfitSummary {
        return repo.computeProfitSummary(orders.value, startMs, endMs, scope)
    }

    fun computeVendorPayables(startMs: Long, endMs: Long): List<VendorPayableSummary> {
        return repo.computeVendorPayables(
            vendors = vendors.value,
            catalogItems = catalogItems.value,
            orders = orders.value,
            payments = vendorPayments.value,
            startMs = startMs,
            endMs = endMs
        )
    }

    fun openInvoicePreview(order: OrderRecord, isReceipt: Boolean) {
        viewModelScope.launch {
            _isRenderingPreview.value = true
            try {
                val bmp = withContext(Dispatchers.Default) {
                    ImageRenderer.renderInvoiceJpg(
                        context = getApplication(),
                        order = order,
                        profile = businessProfile.value,
                        invoiceLang = invoiceLanguage.value,
                        isReceipt = isReceipt
                    )
                }
                val label = if (isReceipt) strings.value.receiptJpg else strings.value.invoiceJpg
                _previewImage.value = PreviewImagePayload(
                    bitmap = bmp,
                    title = "$label • ${order.invoiceNumber}",
                    fileName = "${order.invoiceNumber}_${if (isReceipt) "Receipt" else "Invoice"}.jpg"
                )
            } finally {
                _isRenderingPreview.value = false
            }
        }
    }

    fun openProfitReportPreview(summary: ProfitSummary, dateLabel: String) {
        viewModelScope.launch {
            _isRenderingPreview.value = true
            try {
                val bmp = withContext(Dispatchers.Default) {
                    ImageRenderer.renderProfitReportJpg(
                        context = getApplication(),
                        summary = summary,
                        dateLabel = dateLabel,
                        profile = businessProfile.value,
                        invoiceLang = invoiceLanguage.value
                    )
                }
                _previewImage.value = PreviewImagePayload(
                    bitmap = bmp,
                    title = strings.value.shareProfitJpg,
                    fileName = "ArthFood_Profit_${System.currentTimeMillis()}.jpg"
                )
            } finally {
                _isRenderingPreview.value = false
            }
        }
    }

    fun openVendorStatementPreview(summary: VendorPayableSummary, dateLabel: String) {
        viewModelScope.launch {
            _isRenderingPreview.value = true
            try {
                val payments = vendorPayments.value.filter {
                    it.vendorId == summary.vendor.id || it.vendorName.equals(summary.vendor.name, ignoreCase = true)
                }
                val bmp = withContext(Dispatchers.Default) {
                    ImageRenderer.renderVendorStatementJpg(
                        context = getApplication(),
                        summary = summary,
                        payments = payments,
                        dateLabel = dateLabel,
                        profile = businessProfile.value,
                        invoiceLang = invoiceLanguage.value
                    )
                }
                _previewImage.value = PreviewImagePayload(
                    bitmap = bmp,
                    title = "${summary.vendor.name} • ${strings.value.shareVendorStatementJpg}",
                    fileName = "Vendor_${summary.vendor.name.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
                )
            } finally {
                _isRenderingPreview.value = false
            }
        }
    }

    fun openQrPosterPreview() {
        viewModelScope.launch {
            _isRenderingPreview.value = true
            try {
                val bmp = withContext(Dispatchers.Default) {
                    ImageRenderer.renderCustomerOrderQrPosterJpg(
                        context = getApplication(),
                        profile = businessProfile.value,
                        invoiceLang = invoiceLanguage.value
                    )
                }
                _previewImage.value = PreviewImagePayload(
                    bitmap = bmp,
                    title = strings.value.manageOrderQr,
                    fileName = "ArthFood_Order_QR_Poster.jpg"
                )
            } finally {
                _isRenderingPreview.value = false
            }
        }
    }

    fun shareJsonBackup() {
        viewModelScope.launch {
            val file = repo.exportBackupToCacheFile()
            ImageRenderer.shareFile(getApplication(), file, "application/json", strings.value.exportJsonBackup)
        }
    }

    fun shareDeviceReportJson() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val state = licenseState.value
            val json = licenseManager.buildDeviceReportJson(
                state = state,
                businessName = businessProfile.value.businessName,
                ordersCount = orders.value.size
            )
            val file = withContext(Dispatchers.IO) {
                val dir = java.io.File(app.cacheDir, "exports").apply { mkdirs() }
                val dateStamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
                    .format(java.util.Date())
                val cleanId = state.deviceId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                java.io.File(dir, "ArthFood_Device_Report_${cleanId}_$dateStamp.json").apply {
                    writeText(json, Charsets.UTF_8)
                }
            }
            ImageRenderer.shareFile(app, file, "application/json", "Download Device Report")
            showMessage("Signed device report ready (${file.name})")
        }
    }

    fun buildBackupJsonText(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repo.buildBackupJson()
            prefs.setLastBackupAt(System.currentTimeMillis())
            onReady(json)
        }
    }

    fun restoreBackupJson(jsonText: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repo.restoreAndMergeFromJson(jsonText)
            res.onSuccess { count ->
                val msg = "Backup merged! ($count new orders imported)"
                showMessage(msg)
                onDone(true, msg)
            }.onFailure { err ->
                val msg = "Invalid backup JSON: ${err.message ?: "parse error"}"
                showMessage(msg)
                onDone(false, msg)
            }
        }
    }

    fun inspectBackupJson(jsonText: String, onResult: (Result<BackupInspectionSummary>) -> Unit) {
        viewModelScope.launch {
            onResult(repo.inspectBackupJson(jsonText))
        }
    }

    fun readUriText(uri: Uri, onReady: (String?) -> Unit) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (text.isNullOrBlank()) {
                showMessage("Could not read selected file.")
            }
            onReady(text)
        }
    }

    fun importBackupFromUri(uri: Uri) {
        readUriText(uri) { text ->
            if (!text.isNullOrBlank()) {
                restoreBackupJson(text)
            }
        }
    }

    fun exportExcelWorkbook(
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        dateLabel: String = "All Time"
    ) {
        viewModelScope.launch {
            val file = repo.exportExcelToCacheFile(startMs, endMs, dateLabel)
            ImageRenderer.shareFile(
                getApplication(),
                file,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                strings.value.export4SheetExcel
            )
            showMessage("Excel report ready (${file.name})")
        }
    }

    fun setAutoBackupFolder(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            prefs.setAutoBackupFolderUri(uri.toString())
            val ok = repo.tryAutoBackupToConfiguredFolder()
            showMessage(if (ok) "Auto-backup folder connected & saved!" else "Auto-backup folder saved.")
        }
    }

    fun triggerAutoBackupNow() {
        viewModelScope.launch {
            val ok = repo.tryAutoBackupToConfiguredFolder()
            showMessage(if (ok) "Backup saved to folder!" else "Choose an auto-backup folder first.")
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            if (pin.length == 4 && pin.all { it.isDigit() }) {
                prefs.setPinHash(sha256(pin))
                _isPinUnlocked.value = true
                showMessage("4-Digit PIN enabled!")
            }
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            prefs.setPinHash("")
            _isPinUnlocked.value = true
            showMessage("PIN Lock disabled.")
        }
    }

    fun verifyPin(pin: String): Boolean {
        val ok = sha256(pin) == pinHash.value
        if (ok) _isPinUnlocked.value = true
        return ok
    }

    fun lockAppNow() {
        if (pinHash.value.isNotBlank()) {
            _isPinUnlocked.value = false
        }
    }

    fun verifyAndActivateLicense(key: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = licenseManager.activateWithKey(key)
            if (ok) {
                showMessage(strings.value.licenseUnlockedSuccess)
                _showLicenseModal.value = false
            }
            onResult(ok)
        }
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(app) as T
        }
    }
}
