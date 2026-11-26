package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.ProfitSummary
import com.example.data.model.VendorPayableSummary
import com.example.data.model.VendorPayment
import com.example.i18n.StringsRepo
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageRenderer {

    private val moneyFmt = DecimalFormat("#,##0.##")
    private val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val shortDateFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    private val basilDark = Color.parseColor("#1B5E20")
    private val basil = Color.parseColor("#2E7D32")
    private val basilSoft = Color.parseColor("#E8F5E9")
    private val saffron = Color.parseColor("#F59E0B")
    private val saffronSoft = Color.parseColor("#FEF3C7")
    private val ivory = Color.parseColor("#FAF8F2")
    private val warmGray = Color.parseColor("#F3EFE4")
    private val ink = Color.parseColor("#1A2419")
    private val mutedInk = Color.parseColor("#4B5849")
    private val borderCol = Color.parseColor("#E2DDD0")

    fun formatMoney(amount: Double): String = Formatters.formatINR(amount)

    fun formatGrams(grams: Int): String = Formatters.formatGrams(grams)

    private fun loadLogoBitmap(context: Context, logoPath: String): Bitmap? {
        if (logoPath.isNotBlank()) {
            val file = File(logoPath)
            if (file.exists()) {
                return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.arthfood_icon_fg)
        }.getOrNull()
    }

    fun renderInvoiceJpg(
        context: Context,
        order: OrderRecord,
        profile: BusinessProfile,
        invoiceLang: AppLanguage,
        isReceipt: Boolean = false
    ): Bitmap {
        val s = StringsRepo.get(invoiceLang)
        val width = 1080
        val pad = 48f
        val contentWidth = width - pad * 2

        val itemHeight = 58f
        val extraChargeRows =
            (if (order.deliveryCharge > 0) 1 else 0) +
                (if (order.otherCharge > 0) 1 else 0) +
                (if (order.discount > 0) 1 else 0) +
                (if (order.taxAmount > 0) 1 else 0)

        val estimatedHeight = (690f + order.items.size * itemHeight + extraChargeRows * 42f + 360f)
            .toInt()
            .coerceAtLeast(1260)

        val bmp = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
        val pText = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Clean top brand accent bar
        pFill.color = basilDark
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, pFill)
        pFill.color = saffron
        canvas.drawRect(0f, 12f, width.toFloat(), 16f, pFill)

        // Header: Left (Logo + Business Info), Right (Document Badge + Order # + Date + Tax Line)
        val headerTop = 44f
        val logo = loadLogoBitmap(context, profile.logoPath)
        var leftTextX = pad
        if (logo != null) {
            val logoRect = RectF(pad, headerTop, pad + 108f, headerTop + 108f)
            pFill.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(logoRect, 18f, 18f, pFill)
            pFill.style = Paint.Style.STROKE
            pFill.strokeWidth = 2f
            pFill.color = Color.parseColor("#E5E7EB")
            canvas.drawRoundRect(logoRect, 18f, 18f, pFill)
            pFill.style = Paint.Style.FILL
            canvas.drawBitmap(logo, null, RectF(pad + 6f, headerTop + 6f, pad + 102f, headerTop + 102f), null)
            leftTextX = pad + 128f
        }

        val rightColRight = width - pad
        val badgeTitle = if (isReceipt) {
            if (invoiceLang == AppLanguage.EN) "CUSTOMER ORDER PAYMENT RECEIPT" else s.invReceiptTitle
        } else {
            if (invoiceLang == AppLanguage.EN) "CUSTOMER ORDER INVOICE" else s.invTitle
        }
        pText.textSize = 20f
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val badgeW = pText.measureText(badgeTitle) + 32f
        pFill.color = if (isReceipt) basilSoft else saffronSoft
        canvas.drawRoundRect(RectF(rightColRight - badgeW, headerTop, rightColRight, headerTop + 40f), 10f, 10f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 1.5f
        pFill.color = if (isReceipt) basil else saffron
        canvas.drawRoundRect(RectF(rightColRight - badgeW, headerTop, rightColRight, headerTop + 40f), 10f, 10f, pFill)
        pFill.style = Paint.Style.FILL
        pText.color = if (isReceipt) basilDark else Color.parseColor("#78350F")
        pText.textAlign = Paint.Align.CENTER
        canvas.drawText(badgeTitle, rightColRight - badgeW / 2f, headerTop + 27f, pText)

        // Right-aligned Order #ID, Date, Tax Line
        pText.textAlign = Paint.Align.RIGHT
        pText.color = ink
        pText.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        pText.textSize = 24f
        canvas.drawText("${s.invNumberPrefix} #${order.invoiceNumber}", rightColRight, headerTop + 74f, pText)

        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = mutedInk
        canvas.drawText("${s.invDatePrefix} ${shortDateFmt.format(Date(order.createdAt))}", rightColRight, headerTop + 104f, pText)
        val taxDisplay = profile.taxLine.ifBlank { s.noGstDefault }
        canvas.drawText(taxDisplay.take(36), rightColRight, headerTop + 132f, pText)
        pText.textAlign = Paint.Align.LEFT

        // Left-aligned Business Info
        val leftMaxW = (rightColRight - badgeW - 24f - leftTextX).coerceAtLeast(260f)
        pText.color = basilDark
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 34f
        val bizName = listOf(profile.businessName.ifBlank { "ArthFood Kitchen" }, profile.localBusinessName)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val bizTitleLayout = StaticLayout.Builder.obtain(bizName, 0, bizName.length, pText, leftMaxW.toInt()).setMaxLines(2).build()
        canvas.save()
        canvas.translate(leftTextX, headerTop - 4f)
        bizTitleLayout.draw(canvas)
        canvas.restore()

        var ly = headerTop + bizTitleLayout.height + 6f
        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = mutedInk
        if (profile.tagline.isNotBlank()) {
            canvas.drawText(profile.tagline.take(48), leftTextX, ly + 18f, pText)
            ly += 28f
        }
        if (profile.fullAddress.isNotBlank()) {
            val addrLayout = StaticLayout.Builder.obtain(
                profile.fullAddress.replace("\n", ", "),
                0,
                profile.fullAddress.length,
                pText,
                leftMaxW.toInt()
            ).setMaxLines(2).build()
            canvas.save()
            canvas.translate(leftTextX, ly + 4f)
            addrLayout.draw(canvas)
            canvas.restore()
            ly += addrLayout.height + 10f
        }

        // Hairline divider
        var y = maxOf(headerTop + 148f, ly + 14f)
        pFill.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(pad, y, width - pad, y + 2f, pFill)
        y += 22f

        // 2-Column Metadata Strip: Billed To (Left) & Status Stamps (Right)
        val stripTop = y
        val stripHeight = 148f
        pFill.color = Color.parseColor("#F9FAFB")
        canvas.drawRoundRect(RectF(pad, stripTop, width - pad, stripTop + stripHeight), 14f, 14f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 1.5f
        pFill.color = Color.parseColor("#E5E7EB")
        canvas.drawRoundRect(RectF(pad, stripTop, width - pad, stripTop + stripHeight), 14f, 14f, pFill)
        pFill.style = Paint.Style.FILL

        // Left: Billed To
        pText.color = mutedInk
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 18f
        canvas.drawText(s.invBillTo.uppercase(), pad + 20f, stripTop + 30f, pText)

        pText.color = ink
        pText.textSize = 26f
        canvas.drawText(order.customerName.ifBlank { "Walk-in Customer" }.take(34), pad + 20f, stripTop + 62f, pText)

        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = mutedInk
        if (order.customerPhone.isNotBlank()) {
            canvas.drawText(order.customerPhone, pad + 20f, stripTop + 92f, pText)
        }
        val custAddr = order.customerAddress.ifBlank { order.notes }.replace("\n", ", ")
        if (custAddr.isNotBlank()) {
            canvas.drawText(custAddr.take(46), pad + 20f, stripTop + 122f, pText)
        }

        // Right: Order Sent & Payment Received Stamps
        pText.textAlign = Paint.Align.RIGHT
        val sentStampText = if (order.isSent) {
            val dt = if (order.sentAt > 0L) " · ${shortDateFmt.format(Date(order.sentAt))}" else ""
            "ORDER SENT$dt"
        } else {
            "IN PROCESS (NOT SENT)"
        }
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 20f
        pText.color = if (order.isSent) basilDark else Color.parseColor("#92400E")
        canvas.drawText(sentStampText, rightColRight - 20f, stripTop + 48f, pText)

        val payStampText = when (order.paymentStatus) {
            PaymentStatus.PAID -> {
                val dt = if (order.paidAt > 0L) " · ${shortDateFmt.format(Date(order.paidAt))}" else ""
                "PAYMENT RECEIVED$dt"
            }
            PaymentStatus.PARTIAL -> "${s.partial.uppercase()}: ${formatMoney(order.effectivePaidAmount)}"
            PaymentStatus.UNPAID -> "${s.invBalanceDue.uppercase()}: ${formatMoney(order.balanceDue)}"
        }
        pText.color = if (order.paymentStatus == PaymentStatus.PAID) basil else Color.parseColor("#B91C1C")
        canvas.drawText(payStampText, rightColRight - 20f, stripTop + 88f, pText)

        if (order.paymentStatus == PaymentStatus.PARTIAL) {
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 18f
            pText.color = mutedInk
            canvas.drawText("Due: ${formatMoney(order.balanceDue)}", rightColRight - 20f, stripTop + 118f, pText)
        }
        pText.textAlign = Paint.Align.LEFT

        y = stripTop + stripHeight + 24f

        // Table Header: # | Product | Pack | Rate | Qty | Amount
        pFill.color = basilDark
        canvas.drawRoundRect(RectF(pad, y, width - pad, y + 50f), 10f, 10f, pFill)
        pText.color = Color.WHITE
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 20f

        val colNoX = pad + 14f
        val colItemX = pad + 56f
        val colPackX = pad + contentWidth * 0.44f
        val colRateX = pad + contentWidth * 0.68f
        val colQtyX = pad + contentWidth * 0.80f
        val colAmtX = rightColRight - 14f

        canvas.drawText("#", colNoX, y + 32f, pText)
        canvas.drawText(s.invColItem, colItemX, y + 32f, pText)
        canvas.drawText(s.invColPack, colPackX, y + 32f, pText)
        pText.textAlign = Paint.Align.RIGHT
        canvas.drawText(s.invColRate, colRateX, y + 32f, pText)
        canvas.drawText(s.invColQty, colQtyX, y + 32f, pText)
        canvas.drawText(s.invColAmount, colAmtX, y + 32f, pText)
        pText.textAlign = Paint.Align.LEFT
        y += 58f

        // Line Items
        order.items.forEachIndexed { idx, li ->
            if (idx % 2 == 1) {
                pFill.color = Color.parseColor("#F9FAFB")
                canvas.drawRect(pad, y - 4f, width - pad, y + itemHeight - 4f, pFill)
            }
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 21f
            pText.color = mutedInk
            canvas.drawText("${idx + 1}", colNoX, y + 32f, pText)

            pText.color = ink
            canvas.drawText(li.itemName.take(24), colItemX, y + 32f, pText)

            pText.color = mutedInk
            canvas.drawText(formatGrams(li.packGrams), colPackX, y + 32f, pText)

            pText.textAlign = Paint.Align.RIGHT
            pText.color = if (li.isFreeSample) Color.parseColor("#92400E") else ink
            canvas.drawText(if (li.isFreeSample) "Sample" else formatMoney(li.unitPrice), colRateX, y + 32f, pText)

            pText.color = ink
            canvas.drawText("${li.quantity}", colQtyX, y + 32f, pText)

            pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            pText.color = if (li.isFreeSample) Color.parseColor("#92400E") else ink
            canvas.drawText(if (li.isFreeSample) formatMoney(0.0) else formatMoney(li.lineSaleTotal), colAmtX, y + 32f, pText)

            pText.textAlign = Paint.Align.LEFT
            pText.typeface = Typeface.DEFAULT
            y += itemHeight
        }

        // Table Bottom Hairline
        pFill.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(pad, y + 6f, width - pad, y + 8f, pFill)
        y += 28f

        // Right-aligned Totals Block
        val totBoxLeft = pad + contentWidth * 0.48f
        val totLabelX = totBoxLeft + 16f
        val totValX = rightColRight - 14f
        var ty = y + 20f

        fun drawTotalRow(label: String, value: String, bold: Boolean = false, highlightColor: Int = ink) {
            pText.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            pText.textSize = if (bold) 28f else 22f
            pText.color = if (bold) highlightColor else mutedInk
            canvas.drawText(label, totLabelX, ty, pText)
            pText.textAlign = Paint.Align.RIGHT
            pText.color = highlightColor
            canvas.drawText(value, totValX, ty, pText)
            pText.textAlign = Paint.Align.LEFT
            ty += if (bold) 44f else 36f
        }

        drawTotalRow(s.invSubtotal, formatMoney(order.subtotal))
        if (order.deliveryCharge > 0) drawTotalRow(s.invDelivery, formatMoney(order.deliveryCharge))
        if (order.otherCharge > 0) drawTotalRow(s.invOtherCharges, formatMoney(order.otherCharge))
        if (order.discount > 0) drawTotalRow(s.invDiscount, "-${formatMoney(order.discount)}", highlightColor = basil)
        if (order.taxAmount > 0) drawTotalRow("${s.invTax} (${moneyFmt.format(order.taxPercent)}%)", formatMoney(order.taxAmount))

        pFill.color = basilSoft
        canvas.drawRoundRect(RectF(totBoxLeft, ty - 24f, rightColRight, ty + 28f), 12f, 12f, pFill)
        ty += 10f
        drawTotalRow(s.invGrandTotal, formatMoney(order.grandTotal), bold = true, highlightColor = basilDark)

        if (order.paymentStatus != PaymentStatus.PAID) {
            drawTotalRow(s.invBalanceDue, formatMoney(order.balanceDue), bold = false, highlightColor = Color.parseColor("#B91C1C"))
        } else {
            drawTotalRow(s.invPaidStamp, formatMoney(order.grandTotal), bold = false, highlightColor = basil)
        }
        if (profile.upiId.isNotBlank()) {
            pText.textSize = 19f
            pText.color = mutedInk
            pText.textAlign = Paint.Align.RIGHT
            canvas.drawText("UPI: ${profile.upiId}", totValX, ty + 4f, pText)
            pText.textAlign = Paint.Align.LEFT
            ty += 28f
        }

        // Footer Divider
        val footerTop = maxOf(ty + 20f, y + 220f).coerceAtMost(estimatedHeight - 255f)
        pFill.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(pad, footerTop, width - pad, footerTop + 2f, pFill)

        val qrSize = 168
        val qrBoxLeft = rightColRight - qrSize - 24f
        val footerTextMaxW = (qrBoxLeft - pad - 24f).toInt().coerceAtLeast(360)

        var fy = footerTop + 30f
        if (profile.thankYouNote.isNotBlank()) {
            pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            pText.textSize = 22f
            pText.color = basilDark
            val tyLayout = StaticLayout.Builder.obtain(
                profile.thankYouNote, 0, profile.thankYouNote.length, pText, footerTextMaxW
            ).setMaxLines(2).build()
            canvas.save()
            canvas.translate(pad, fy)
            tyLayout.draw(canvas)
            canvas.restore()
            fy += tyLayout.height + 12f
        }

        val contactParts = listOfNotNull(
            profile.ownerName.takeIf { it.isNotBlank() },
            profile.phone.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        val hasWa = profile.effectiveWhatsApp.isNotBlank()
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 20f
        pText.color = ink
        if (contactParts.isNotBlank()) {
            val contactLine = if (hasWa) "Contact: $contactParts  [WhatsApp]" else "Contact: $contactParts"
            canvas.drawText(contactLine, pad, fy + 18f, pText)
            fy += 30f
        }
        if (profile.fullAddress.isNotBlank()) {
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 18f
            pText.color = mutedInk
            canvas.drawText(profile.fullAddress.replace("\n", ", ").take(62), pad, fy + 18f, pText)
            fy += 28f
        }
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 18f
        pText.color = basilDark
        canvas.drawText("Scan QR to save our contact in 1 tap →", pad, fy + 20f, pText)

        // Framed vCard QR on Right
        val vCardQr = QrCodeGenerator.generateQrBitmap(QrCodeGenerator.buildVCardString(profile), qrSize, basilDark, Color.WHITE)
        pFill.color = Color.WHITE
        canvas.drawRoundRect(RectF(qrBoxLeft, footerTop + 18f, rightColRight, footerTop + qrSize + 42f), 14f, 14f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 2f
        pFill.color = Color.parseColor("#E5E7EB")
        canvas.drawRoundRect(RectF(qrBoxLeft, footerTop + 18f, rightColRight, footerTop + qrSize + 42f), 14f, 14f, pFill)
        pFill.style = Paint.Style.FILL
        canvas.drawBitmap(vCardQr, qrBoxLeft + 12f, footerTop + 30f, null)

        // Bottom tagline
        pText.typeface = Typeface.DEFAULT
        pText.textSize = 18f
        pText.color = mutedInk
        pText.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated offline via ArthFood", width / 2f, estimatedHeight - 20f, pText)

        return bmp
    }

    fun renderProfitReportJpg(
        context: Context,
        summary: ProfitSummary,
        dateLabel: String,
        profile: BusinessProfile,
        invoiceLang: AppLanguage
    ): Bitmap {
        val s = StringsRepo.get(invoiceLang)
        val width = 1080
        val height = 1090
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(ivory)

        val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
        val pText = TextPaint(Paint.ANTI_ALIAS_FLAG)

        pFill.color = basilDark
        canvas.drawRect(0f, 0f, width.toFloat(), 196f, pFill)
        pFill.color = saffron
        canvas.drawRect(0f, 186f, width.toFloat(), 196f, pFill)

        val logo = loadLogoBitmap(context, profile.logoPath)
        var tx = 48f
        if (logo != null) {
            pFill.color = Color.WHITE
            canvas.drawRoundRect(RectF(48f, 44f, 148f, 144f), 16f, 16f, pFill)
            canvas.drawBitmap(logo, null, RectF(54f, 50f, 142f, 138f), null)
            tx = 168f
        }

        pText.color = Color.WHITE
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 36f
        canvas.drawText(profile.businessName.ifBlank { "ArthFood Kitchen" }, tx, 82f, pText)
        pText.textSize = 24f
        pText.color = saffronSoft
        canvas.drawText(s.invProfitReportTitle, tx, 126f, pText)
        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = Color.WHITE
        canvas.drawText("Date Range: $dateLabel", tx, 160f, pText)

        var y = 236f
        fun drawMetricCard(label: String, value: String, bg: Int, fg: Int) {
            pFill.color = bg
            canvas.drawRoundRect(RectF(48f, y, width - 48f, y + 114f), 20f, 20f, pFill)
            pFill.style = Paint.Style.STROKE
            pFill.strokeWidth = 1.5f
            pFill.color = borderCol
            canvas.drawRoundRect(RectF(48f, y, width - 48f, y + 114f), 20f, 20f, pFill)
            pFill.style = Paint.Style.FILL
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 24f
            pText.color = mutedInk
            canvas.drawText(label, 80f, y + 66f, pText)
            pText.textAlign = Paint.Align.RIGHT
            pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            pText.textSize = 34f
            pText.color = fg
            canvas.drawText(value, width - 80f, y + 68f, pText)
            pText.textAlign = Paint.Align.LEFT
            y += 132f
        }

        drawMetricCard("Date Range", dateLabel, Color.WHITE, ink)
        drawMetricCard(
            "${s.totalOrdersLabel} (+ ${summary.freeSampleCount} ${s.invFreeLabel.lowercase()})",
            "${summary.orderCount}",
            warmGray,
            ink
        )
        drawMetricCard(s.totalSales, formatMoney(summary.totalSales), basilSoft, basilDark)
        drawMetricCard(s.totalVendorCost, formatMoney(summary.totalVendorCost), saffronSoft, Color.parseColor("#92400E"))
        drawMetricCard(
            s.netProfit,
            formatMoney(summary.netProfit),
            if (summary.netProfit >= 0) basilSoft else Color.parseColor("#FEE2E2"),
            if (summary.netProfit >= 0) basilDark else Color.parseColor("#B91C1C")
        )
        drawMetricCard(
            s.marginPercent,
            String.format(Locale.getDefault(), "%.1f%%", summary.marginPercent),
            Color.WHITE,
            basilDark
        )

        pText.typeface = Typeface.DEFAULT
        pText.textSize = 18f
        pText.color = mutedInk
        pText.textAlign = Paint.Align.CENTER
        canvas.drawText(s.invPoweredBy, width / 2f, height - 28f, pText)
        return bmp
    }

    fun renderVendorStatementJpg(
        context: Context,
        summary: VendorPayableSummary,
        payments: List<VendorPayment>,
        dateLabel: String,
        profile: BusinessProfile,
        invoiceLang: AppLanguage
    ): Bitmap {
        val s = StringsRepo.get(invoiceLang)
        val width = 1080
        val rows = summary.packBreakdown.size + payments.take(8).size
        val height = (780 + rows * 58).coerceAtLeast(1060)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(ivory)

        val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
        val pText = TextPaint(Paint.ANTI_ALIAS_FLAG)

        pFill.color = basilDark
        canvas.drawRect(0f, 0f, width.toFloat(), 196f, pFill)
        pFill.color = saffron
        canvas.drawRect(0f, 186f, width.toFloat(), 196f, pFill)

        pText.color = Color.WHITE
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 34f
        canvas.drawText(profile.businessName.ifBlank { "ArthFood Kitchen" }, 48f, 74f, pText)
        pText.textSize = 25f
        pText.color = saffronSoft
        canvas.drawText("${s.invVendorStatementTitle}: ${summary.vendor.name}", 48f, 122f, pText)
        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = Color.WHITE
        canvas.drawText("Period: $dateLabel", 48f, 158f, pText)

        var y = 230f
        pFill.color = Color.WHITE
        canvas.drawRoundRect(RectF(48f, y, width - 48f, y + 138f), 20f, 20f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 1.5f
        pFill.color = borderCol
        canvas.drawRoundRect(RectF(48f, y, width - 48f, y + 138f), 20f, 20f, pFill)
        pFill.style = Paint.Style.FILL

        pText.color = ink
        pText.textSize = 24f
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${s.totalPayable}: ${formatMoney(summary.totalPayable)}", 76f, y + 52f, pText)
        pText.color = basil
        canvas.drawText("${s.totalPaidLabel}: ${formatMoney(summary.totalPaid)}", 76f, y + 98f, pText)
        pText.textAlign = Paint.Align.RIGHT
        pText.color = if (summary.balanceDue > 0) Color.parseColor("#B91C1C") else basilDark
        pText.textSize = 28f
        canvas.drawText("${s.balanceDueLabel}: ${formatMoney(summary.balanceDue)}", width - 76f, y + 78f, pText)
        pText.textAlign = Paint.Align.LEFT

        y += 174f
        pText.color = basilDark
        pText.textSize = 24f
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${s.packWiseSummary} (${formatGrams(summary.totalGrams)})", 48f, y, pText)
        y += 22f

        // Table Header
        pFill.color = basilDark
        canvas.drawRoundRect(RectF(48f, y, width - 48f, y + 46f), 10f, 10f, pFill)
        pText.color = Color.WHITE
        pText.textSize = 19f
        canvas.drawText("Item & Pack", 64f, y + 30f, pText)
        pText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rate/kg", 690f, y + 30f, pText)
        canvas.drawText("Amount", 880f, y + 30f, pText)
        canvas.drawText("Status", width - 64f, y + 30f, pText)
        pText.textAlign = Paint.Align.LEFT
        y += 54f

        summary.packBreakdown.forEachIndexed { idx, pack ->
            if (idx % 2 == 1) {
                pFill.color = warmGray
                canvas.drawRect(48f, y - 4f, width - 48f, y + 48f, pFill)
            }
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 20f
            pText.color = ink
            canvas.drawText(
                "${pack.itemName} (${formatGrams(pack.packGrams)} × ${pack.quantity})".take(36),
                64f,
                y + 28f,
                pText
            )
            pText.textAlign = Paint.Align.RIGHT
            pText.color = mutedInk
            canvas.drawText("${formatMoney(pack.vendorCostPerKg)}/kg", 690f, y + 28f, pText)
            pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            pText.color = ink
            canvas.drawText(formatMoney(pack.costTotal), 880f, y + 28f, pText)
            pText.color = if (pack.allPaid) basil else Color.parseColor("#B91C1C")
            canvas.drawText(if (pack.allPaid) "PAID" else "PENDING", width - 64f, y + 28f, pText)
            pText.textAlign = Paint.Align.LEFT
            y += 52f
        }

        if (payments.isNotEmpty()) {
            y += 28f
            pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            pText.color = basilDark
            pText.textSize = 24f
            canvas.drawText(s.recordPayment, 48f, y, pText)
            y += 32f
            pText.typeface = Typeface.DEFAULT
            pText.textSize = 20f
            payments.take(8).forEach { pay ->
                pText.color = mutedInk
                canvas.drawText("${shortDateFmt.format(Date(pay.paymentDate))} • ${pay.mode} ${pay.note}".take(52), 56f, y + 28f, pText)
                pText.textAlign = Paint.Align.RIGHT
                pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                pText.color = basil
                canvas.drawText(formatMoney(pay.amount), width - 56f, y + 28f, pText)
                pText.textAlign = Paint.Align.LEFT
                pText.typeface = Typeface.DEFAULT
                y += 44f
            }
        }

        pText.typeface = Typeface.DEFAULT
        pText.textSize = 18f
        pText.color = mutedInk
        pText.textAlign = Paint.Align.CENTER
        canvas.drawText(s.invPoweredBy, width / 2f, height - 28f, pText)
        return bmp
    }

    fun renderCustomerOrderQrPosterJpg(
        context: Context,
        profile: BusinessProfile,
        invoiceLang: AppLanguage
    ): Bitmap {
        val s = StringsRepo.get(invoiceLang)
        val width = 1080
        val height = 1540
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(ivory)

        val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
        val pText = TextPaint(Paint.ANTI_ALIAS_FLAG)

        pFill.color = basilDark
        canvas.drawRect(0f, 0f, width.toFloat(), 320f, pFill)
        pFill.color = saffron
        canvas.drawRect(0f, 308f, width.toFloat(), 320f, pFill)

        // Business Logo centered above name
        val logo = loadLogoBitmap(context, profile.logoPath)
        if (logo != null) {
            pFill.color = Color.WHITE
            canvas.drawRoundRect(RectF((width - 96f) / 2f, 34f, (width + 96f) / 2f, 130f), 20f, 20f, pFill)
            canvas.drawBitmap(logo, null, RectF((width - 84f) / 2f, 40f, (width + 84f) / 2f, 124f), null)
        }

        pText.textAlign = Paint.Align.CENTER
        pText.color = Color.WHITE
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 44f
        canvas.drawText(profile.businessName.ifBlank { "ArthFood Kitchen" }, width / 2f, 186f, pText)
        pText.typeface = Typeface.DEFAULT
        pText.textSize = 24f
        pText.color = saffronSoft
        if (profile.tagline.isNotBlank()) canvas.drawText(profile.tagline, width / 2f, 228f, pText)
        val phoneDisplay = profile.effectiveWhatsApp.ifBlank { profile.phone }
        if (phoneDisplay.isNotBlank()) canvas.drawText("Order / WhatsApp: $phoneDisplay", width / 2f, 272f, pText)

        // QR 1: WhatsApp Order QR
        val waMsg = "Hi ${profile.businessName}, I want to place an order!"
        val waUrl = QrCodeGenerator.buildWhatsAppUrl(profile.effectiveWhatsApp, waMsg)
        val waQr = QrCodeGenerator.generateQrBitmap(waUrl, 400, basilDark, Color.WHITE)

        pFill.color = Color.WHITE
        canvas.drawRoundRect(RectF(140f, 368f, width - 140f, 886f), 28f, 28f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 3f
        pFill.color = basil
        canvas.drawRoundRect(RectF(140f, 368f, width - 140f, 886f), 28f, 28f, pFill)
        pFill.style = Paint.Style.FILL

        canvas.drawBitmap(waQr, (width - 400f) / 2f, 402f, null)
        pText.color = basilDark
        pText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pText.textSize = 28f
        canvas.drawText(s.scanToOrderWhatsApp, width / 2f, 852f, pText)

        // QR 2: vCard Save Contact
        val vCardQr = QrCodeGenerator.generateQrBitmap(QrCodeGenerator.buildVCardString(profile), 340, ink, Color.WHITE)
        pFill.color = Color.WHITE
        canvas.drawRoundRect(RectF(180f, 926f, width - 180f, 1400f), 28f, 28f, pFill)
        pFill.style = Paint.Style.STROKE
        pFill.strokeWidth = 2f
        pFill.color = borderCol
        canvas.drawRoundRect(RectF(180f, 926f, width - 180f, 1400f), 28f, 28f, pFill)
        pFill.style = Paint.Style.FILL

        canvas.drawBitmap(vCardQr, (width - 340f) / 2f, 962f, null)
        pText.color = ink
        pText.textSize = 25f
        canvas.drawText(s.scanToSaveContact, width / 2f, 1354f, pText)

        pText.typeface = Typeface.DEFAULT
        pText.textSize = 20f
        pText.color = mutedInk
        canvas.drawText(s.invPoweredBy, width / 2f, height - 38f, pText)
        return bmp
    }

    fun saveBitmapToCacheJpg(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun saveJpgToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ArthFood")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }
}
