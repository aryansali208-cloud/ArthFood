package com.example

import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.PackSummaryEntry
import com.example.data.model.ProfitSummary
import com.example.data.model.Vendor
import com.example.data.model.VendorPayableSummary
import com.example.export.ExcelXlsxExporter
import com.example.licensing.LicenseConfig
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ArthFoodCoreTest {

    @Test
    fun hmacLicenseMatchesKeygenHtml() {
        val deviceId = "AF-K9P2-M4X7"
        val key = LicenseConfig.expectedKeyForDevice(deviceId)
        assertEquals("AF1-N59L-6GB3-C2DF-YENU", key)
        assertTrue(LicenseConfig.verifyKey(deviceId, key))
        assertTrue(LicenseConfig.verifyKey(deviceId, key.lowercase().replace("-", "")))
        assertFalse(LicenseConfig.verifyKey("AF-DIFFERENT", key))

        val af499Key = LicenseConfig.expectedAf499KeyForDevice(deviceId)
        assertTrue(af499Key.startsWith("AF499-"))
        assertEquals(20, af499Key.length)
        assertTrue(LicenseConfig.verifyKey(deviceId, af499Key))
        assertTrue(LicenseConfig.verifyKey(deviceId, af499Key.lowercase().replace("-", "")))
        assertFalse(LicenseConfig.verifyKey("AF-DIFFERENT", af499Key))

        val whatsappMessage = """
            Hi Fresh Kitchen, thank you for activating ArthFood (₹499 Lifetime Full Access)!

            Device ID: $deviceId
            Activation Key: ${'\u200B'}$af499Key

            Open ArthFood and paste this key to unlock permanently.
        """.trimIndent()
        assertEquals(af499Key, LicenseConfig.sanitizeKeyInput(whatsappMessage))
        assertTrue(LicenseConfig.isWellFormedKey(whatsappMessage))
        assertTrue(LicenseConfig.verifyKey(deviceId, whatsappMessage))
    }

    @Test
    fun verifyKeyWithDiagnosticsPinpointsRejectionReasons() {
        val deviceId = "AF-K9P2-M4X7"
        val validKey = LicenseConfig.expectedAf499KeyForDevice(deviceId)

        val emptyDiag = LicenseConfig.verifyKeyWithDiagnostics(deviceId, "   ")
        assertFalse(emptyDiag.isValid)
        assertTrue(emptyDiag.errorMessage.contains("Empty key"))

        val shortDiag = LicenseConfig.verifyKeyWithDiagnostics(deviceId, "AF499-ABCD")
        assertFalse(shortDiag.isValid)
        assertTrue(shortDiag.errorMessage.contains("Invalid key length"))

        val badCharDiag = LicenseConfig.verifyKeyWithDiagnostics(deviceId, "AF499-ABCD-EFGH-I01O")
        assertFalse(badCharDiag.isValid)
        assertTrue(badCharDiag.errorMessage.contains("Invalid character"))

        val mismatchDiag = LicenseConfig.verifyKeyWithDiagnostics("AF-OTHER-0000", validKey)
        assertFalse(mismatchDiag.isValid)
        assertEquals(validKey, mismatchDiag.normalizedKey)
        assertTrue(mismatchDiag.errorMessage.contains("HMAC-SHA256 signature mismatch"))
        assertTrue(mismatchDiag.errorMessage.contains("AF-OTHER-0000"))

        val okDiag = LicenseConfig.verifyKeyWithDiagnostics(deviceId, validKey)
        assertTrue(okDiag.isValid)
        assertEquals(validKey, okDiag.normalizedKey)
    }

    @Test
    fun signedDeviceReportVerifiesAndDetectsTamper() {
        val info = LicenseConfig.DeviceReportInfo(
            deviceId = "AF-K9P2-M4X7",
            manufacturer = "Google",
            model = "Pixel",
            brand = "google",
            androidRelease = "15",
            sdkInt = 35,
            appPackage = "com.arthfood.billing",
            appVersion = "1.0",
            businessName = "Fresh Kitchen",
            ordersCount = 12,
            isLicensed = false,
            isLockedOut = true,
            isTrialExpired = true,
            isClockTampered = false,
            daysRemaining = 0,
            trialStartMillis = 1_700_000_000_000L,
            licenseKey = "",
            exportedAtMillis = 1_700_700_000_000L
        )
        val json = LicenseConfig.buildSignedDeviceReportJson(info)
        assertTrue(json.contains("\"reportType\": \"ARTHFOOD_DEVICE_LICENSE_REPORT\""))
        assertTrue(json.contains("\"algorithm\": \"HMAC-SHA256\""))
        assertTrue(json.contains("\"deviceId\": \"AF-K9P2-M4X7\""))
        assertTrue(json.contains("\"status\": \"LOCKED_OUT\""))

        val sigMatch = Regex("\"signature\": \"([0-9a-f]+)\"").find(json)
        val sig = sigMatch!!.groupValues[1]
        assertEquals(64, sig.length)
        assertTrue(LicenseConfig.verifyDeviceReportSignature(info, sig))
        assertFalse(LicenseConfig.verifyDeviceReportSignature(info.copy(isLicensed = true), sig))
    }

    @Test
    fun excelFourSheetWorkbookContainsAllSheets() {
        val out = ByteArrayOutputStream()
        val orders = listOf(
            OrderRecord(
                id = 1L,
                invoiceNumber = "AF-2026-0001",
                customerName = "Ramesh",
                customerPhone = "+91 9876543210",
                subtotal = 250.0,
                grandTotal = 250.0,
                totalVendorCost = 150.0,
                items = listOf(
                    OrderLineItem(
                        itemName = "Mango Pickle",
                        packGrams = 500,
                        quantity = 1,
                        unitPrice = 250.0,
                        sellingPricePerKg = 500.0,
                        vendorCostPerKg = 300.0,
                        vendorId = 1L,
                        vendorName = "Patil Foods"
                    )
                )
            )
        )
        val profitSummary = ProfitSummary(
            orderCount = 1,
            freeSampleCount = 0,
            totalSales = 250.0,
            totalVendorCost = 150.0,
            deliveryAndOtherCharges = 0.0,
            netProfit = 100.0,
            marginPercent = 40.0
        )
        val vendorPayables = listOf(
            VendorPayableSummary(
                vendor = Vendor(1L, "Patil Foods", "+91 9999999999"),
                totalGrams = 500,
                totalPayable = 150.0,
                totalPaid = 0.0,
                balanceDue = 150.0,
                packBreakdown = listOf(
                    PackSummaryEntry(
                        itemName = "Mango Pickle",
                        packGrams = 500,
                        quantity = 1,
                        totalGrams = 500,
                        costTotal = 150.0,
                        vendorCostPerKg = 300.0,
                        allPaid = false
                    )
                ),
                missingCostItems = emptyList(),
                hasUnpaidLines = true
            )
        )

        ExcelXlsxExporter.writeWorkbook(
            output = out,
            orders = orders,
            profitSummary = profitSummary,
            vendorPayables = vendorPayables,
            dateRangeLabel = "All Time"
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet4.xml"))
        assertTrue(entries["xl/worksheets/sheet1.xml"]!!.contains("<cols>"))
        assertTrue(entries["xl/worksheets/sheet4.xml"]!!.contains("Patil Foods"))
    }

    @Test
    fun formattersAndCurrencyMath() {
        assertEquals(0.3, Formatters.round2(0.1 + 0.2), 0.0001)
        assertEquals("₹1,240.00", Formatters.formatINR(1240.0))
        assertEquals("₹1,05,000.00", Formatters.formatINR(105000.0))
        assertEquals("-₹50.50", Formatters.formatINR(-50.5))
    }
}
