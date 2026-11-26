package com.example.export

import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.ProfitSummary
import com.example.data.model.VendorPayableSummary
import com.example.util.Formatters
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin OOXML (.xlsx) 4-sheet Excel workbook generator using ZipOutputStream.
 * Generates:
 * 1. Orders
 * 2. Item Breakdown
 * 3. Profit Summary
 * 4. Vendor Payables
 */
object ExcelXlsxExporter {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    fun writeWorkbook(
        output: OutputStream,
        orders: List<OrderRecord>,
        profitSummary: ProfitSummary,
        vendorPayables: List<VendorPayableSummary>,
        dateRangeLabel: String = "All Time"
    ) {
        val ordersRows = buildOrdersRows(orders)
        val itemBreakdownRows = buildItemBreakdownRows(orders)
        val profitRows = buildProfitSummaryRows(dateRangeLabel, profitSummary)
        val vendorRows = buildVendorPayablesRows(vendorPayables)

        ZipOutputStream(output).use { zip ->
            addEntry(zip, "[Content_Types].xml", contentTypesXml())
            addEntry(zip, "_rels/.rels", rootRelsXml())
            addEntry(zip, "xl/workbook.xml", workbookXml())
            addEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            addEntry(zip, "xl/styles.xml", stylesXml())
            addEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(ordersRows, listOf(16, 14, 22, 15, 28, 14, 12, 12, 14, 14, 14, 10, 10, 11)))
            addEntry(zip, "xl/worksheets/sheet2.xml", sheetXml(itemBreakdownRows, listOf(16, 14, 24, 12, 8, 14, 14, 13, 14)))
            addEntry(zip, "xl/worksheets/sheet3.xml", sheetXml(profitRows, listOf(26, 14, 16, 16, 16, 14)))
            addEntry(zip, "xl/worksheets/sheet4.xml", sheetXml(vendorRows, listOf(24, 16, 16, 16)))
        }
    }

    private fun buildOrdersRows(orders: List<OrderRecord>): List<List<Any>> {
        val rows = mutableListOf<List<Any>>()
        rows.add(
            listOf(
                "Order #", "Date", "Customer", "Mobile", "Address",
                "Subtotal", "Delivery", "Other", "Grand Total",
                "Vendor Cost", "Profit", "Sent", "Paid", "Cancelled"
            )
        )
        orders.forEach { o ->
            val cancelled = o.status == OrderStatus.CANCELLED
            val profit = if (cancelled) 0.0 else Formatters.round2(o.grandTotal - o.totalVendorCost)
            rows.add(
                listOf(
                    o.invoiceNumber,
                    dateFmt.format(Date(o.createdAt)),
                    o.customerName,
                    o.customerPhone,
                    o.customerAddress,
                    Formatters.round2(o.subtotal),
                    Formatters.round2(o.deliveryCharge),
                    Formatters.round2(o.otherCharge),
                    Formatters.round2(o.grandTotal),
                    Formatters.round2(o.totalVendorCost),
                    profit,
                    if (o.isSent) "YES" else "NO",
                    if (o.paymentStatus == PaymentStatus.PAID) "YES" else if (o.paymentStatus == PaymentStatus.PARTIAL) "PARTIAL" else "NO",
                    if (cancelled) "YES" else "NO"
                )
            )
        }
        return rows
    }

    private fun buildItemBreakdownRows(orders: List<OrderRecord>): List<List<Any>> {
        val rows = mutableListOf<List<Any>>()
        rows.add(
            listOf(
                "Order #", "Date", "Item", "Pack", "Qty",
                "Unit Price", "Line Total", "Free Sample", "Vendor Cost"
            )
        )
        orders.forEach { o ->
            o.items.forEach { li ->
                rows.add(
                    listOf(
                        o.invoiceNumber,
                        dateFmt.format(Date(o.createdAt)),
                        li.itemName,
                        Formatters.formatGrams(li.packGrams),
                        li.quantity,
                        Formatters.round2(li.unitPrice),
                        Formatters.round2(li.lineSaleTotal),
                        if (li.isFreeSample) "YES" else "NO",
                        Formatters.round2(li.lineCostTotal)
                    )
                )
            }
        }
        return rows
    }

    private fun buildProfitSummaryRows(dateRangeLabel: String, summary: ProfitSummary): List<List<Any>> {
        return listOf(
            listOf("Date Range", "Order Count", "Total Sales", "Total Cost", "Net Profit", "Margin %"),
            listOf(
                dateRangeLabel,
                summary.orderCount,
                Formatters.round2(summary.totalSales),
                Formatters.round2(summary.totalVendorCost),
                Formatters.round2(summary.netProfit),
                Formatters.round2(summary.marginPercent)
            )
        )
    }

    private fun buildVendorPayablesRows(vendorPayables: List<VendorPayableSummary>): List<List<Any>> {
        val rows = mutableListOf<List<Any>>()
        rows.add(listOf("Vendor", "Total Payable", "Paid", "Pending"))
        vendorPayables.forEach { vp ->
            rows.add(
                listOf(
                    vp.vendor.name,
                    Formatters.round2(vp.totalPayable),
                    Formatters.round2(vp.totalPaid),
                    Formatters.round2(vp.balanceDue)
                )
            )
        }
        return rows
    }

    private fun addEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun sheetXml(rows: List<List<Any>>, colWidths: List<Int>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (colWidths.isNotEmpty()) {
            append("<cols>")
            colWidths.forEachIndexed { idx, w ->
                val colNum = idx + 1
                append("""<col min="$colNum" max="$colNum" width="$w" customWidth="1"/>""")
            }
            append("</cols>")
        }
        append("<sheetData>")
        rows.forEachIndexed { rowIdx, cells ->
            val r = rowIdx + 1
            append("""<row r="$r">""")
            cells.forEachIndexed { colIdx, value ->
                val ref = "${colLetter(colIdx)}$r"
                val style = if (rowIdx == 0) """ s="1"""" else ""
                when (value) {
                    is Int, is Long, is Double, is Float -> {
                        append("""<c r="$ref"$style><v>$value</v></c>""")
                    }
                    else -> {
                        append("""<c r="$ref" t="inlineStr"$style><is><t>${xmlEscape(value.toString())}</t></is></c>""")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun colLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun xmlEscape(s: String): String = buildString(s.length) {
        for (ch in s) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') append(ch)
            }
        }
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-package.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Orders" sheetId="1" r:id="rId1"/>
    <sheet name="Item Breakdown" sheetId="2" r:id="rId2"/>
    <sheet name="Profit Summary" sheetId="3" r:id="rId3"/>
    <sheet name="Vendor Payables" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF2E7D32"/></patternFill></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""
}
