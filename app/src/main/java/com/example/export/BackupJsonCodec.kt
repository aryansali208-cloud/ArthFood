package com.example.export

import com.example.data.local.CatalogItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderWithItemsEntity
import com.example.data.local.PackSizeEntity
import com.example.data.local.VendorEntity
import com.example.data.local.VendorPaymentEntity
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val exportedAt: Long,
    val appLanguage: AppLanguage,
    val invoiceLanguage: AppLanguage,
    val businessProfile: BusinessProfile,
    val lockedRates: Boolean,
    val stockTracking: Boolean,
    val packSizes: List<PackSizeEntity>,
    val vendors: List<VendorEntity>,
    val catalogItems: List<CatalogItemEntity>,
    val orders: List<OrderWithItemsEntity>,
    val vendorPayments: List<VendorPaymentEntity>
)

object BackupJsonCodec {

    fun encode(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("schema", "arthfood_backup_v1")
        root.put("exportedAt", payload.exportedAt)
        root.put("appLanguage", payload.appLanguage.code)
        root.put("invoiceLanguage", payload.invoiceLanguage.code)
        root.put("lockedRates", payload.lockedRates)
        root.put("stockTracking", payload.stockTracking)

        val biz = JSONObject().apply {
            put("businessName", payload.businessProfile.businessName)
            put("localBusinessName", payload.businessProfile.localBusinessName)
            put("tagline", payload.businessProfile.tagline)
            put("ownerName", payload.businessProfile.ownerName)
            put("phone", payload.businessProfile.phone)
            put("whatsappSameAsPhone", payload.businessProfile.whatsappSameAsPhone)
            put("whatsappNumber", payload.businessProfile.whatsappNumber)
            put("address", payload.businessProfile.address)
            put("city", payload.businessProfile.city)
            put("state", payload.businessProfile.state)
            put("pinCode", payload.businessProfile.pinCode)
            put("upiId", payload.businessProfile.upiId)
            put("taxPercent", payload.businessProfile.taxPercent)
            put("taxLine", payload.businessProfile.taxLine)
            put("thankYouNote", payload.businessProfile.thankYouNote)
            put("qrContactName", payload.businessProfile.qrContactName)
        }
        root.put("businessProfile", biz)

        val packsArr = JSONArray()
        payload.packSizes.forEach { p ->
            packsArr.put(JSONObject().apply {
                put("grams", p.grams)
                put("customName", p.customName)
                put("sortOrder", p.sortOrder)
            })
        }
        root.put("packSizes", packsArr)

        val vendorsArr = JSONArray()
        payload.vendors.forEach { v ->
            vendorsArr.put(JSONObject().apply {
                put("id", v.id)
                put("name", v.name)
                put("phone", v.phone)
                put("notes", v.notes)
            })
        }
        root.put("vendors", vendorsArr)

        val catalogArr = JSONArray()
        payload.catalogItems.forEach { c ->
            catalogArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("sellingPricePerKg", c.sellingPricePerKg)
                put("vendorCostPerKg", c.vendorCostPerKg)
                put("vendorId", c.vendorId ?: JSONObject.NULL)
                put("vendorName", c.vendorName)
                put("stockGrams", c.stockGrams)
                put("lowStockThresholdGrams", c.lowStockThresholdGrams)
                put("trackStock", c.trackStock)
                put("packPriceOverridesJson", c.packPriceOverridesJson)
                put("isActive", c.isActive)
            })
        }
        root.put("catalogItems", catalogArr)

        val ordersArr = JSONArray()
        payload.orders.forEach { ow ->
            val o = ow.order
            val itemsArr = JSONArray()
            ow.items.forEach { li ->
                itemsArr.put(JSONObject().apply {
                    put("catalogItemId", li.catalogItemId ?: JSONObject.NULL)
                    put("itemName", li.itemName)
                    put("packGrams", li.packGrams)
                    put("quantity", li.quantity)
                    put("unitPrice", li.unitPrice)
                    put("sellingPricePerKg", li.sellingPricePerKg)
                    put("vendorCostPerKg", li.vendorCostPerKg)
                    put("vendorId", li.vendorId ?: JSONObject.NULL)
                    put("vendorName", li.vendorName)
                    put("isFreeSample", li.isFreeSample)
                    put("isVendorLinePaid", li.isVendorLinePaid)
                    put("lineSaleTotal", li.lineSaleTotal)
                    put("lineCostTotal", li.lineCostTotal)
                })
            }
            ordersArr.put(JSONObject().apply {
                put("invoiceNumber", o.invoiceNumber)
                put("customerName", o.customerName)
                put("customerPhone", o.customerPhone)
                put("customerAddress", o.customerAddress)
                put("notes", o.notes)
                put("status", o.status)
                put("paymentStatus", o.paymentStatus)
                put("paidAmount", o.paidAmount)
                put("deliveryCharge", o.deliveryCharge)
                put("otherCharge", o.otherCharge)
                put("discount", o.discount)
                put("taxPercent", o.taxPercent)
                put("subtotal", o.subtotal)
                put("taxAmount", o.taxAmount)
                put("grandTotal", o.grandTotal)
                put("totalVendorCost", o.totalVendorCost)
                put("sentAt", o.sentAt)
                put("paidAt", o.paidAt)
                put("createdAt", o.createdAt)
                put("updatedAt", o.updatedAt)
                put("items", itemsArr)
            })
        }
        root.put("orders", ordersArr)

        val paymentsArr = JSONArray()
        payload.vendorPayments.forEach { vp ->
            paymentsArr.put(JSONObject().apply {
                put("vendorId", vp.vendorId)
                put("vendorName", vp.vendorName)
                put("amount", vp.amount)
                put("paymentDate", vp.paymentDate)
                put("mode", vp.mode)
                put("note", vp.note)
            })
        }
        root.put("vendorPayments", paymentsArr)

        return root.toString(2)
    }

    fun decode(jsonText: String): BackupPayload {
        val trimmed = jsonText.trim()
        require(trimmed.startsWith("{")) { "Backup file must be a JSON object" }
        val root = JSONObject(trimmed)
        val hasKnownSection = root.has("orders") || root.has("catalogItems") || root.has("businessProfile") || root.has("vendors")
        require(hasKnownSection) { "Not a valid ArthFood backup JSON" }

        val bizObj = root.optJSONObject("businessProfile")
        val profile = if (bizObj != null) {
            BusinessProfile(
                businessName = bizObj.optString("businessName", "ArthFood Kitchen"),
                localBusinessName = bizObj.optString("localBusinessName", ""),
                tagline = bizObj.optString("tagline", "Made billing easy"),
                ownerName = bizObj.optString("ownerName", ""),
                phone = bizObj.optString("phone", ""),
                whatsappSameAsPhone = bizObj.optBoolean("whatsappSameAsPhone", true),
                whatsappNumber = bizObj.optString("whatsappNumber", ""),
                address = bizObj.optString("address", ""),
                city = bizObj.optString("city", ""),
                state = bizObj.optString("state", ""),
                pinCode = bizObj.optString("pinCode", ""),
                upiId = bizObj.optString("upiId", ""),
                taxPercent = bizObj.optDouble("taxPercent", 0.0),
                taxLine = bizObj.optString("taxLine", ""),
                thankYouNote = bizObj.optString("thankYouNote", "Thank you for your order! Visit again."),
                qrContactName = bizObj.optString("qrContactName", "")
            )
        } else BusinessProfile()

        val packs = mutableListOf<PackSizeEntity>()
        val packsArr = root.optJSONArray("packSizes")
        if (packsArr != null) {
            for (i in 0 until packsArr.length()) {
                val itemObj = packsArr.optJSONObject(i)
                if (itemObj != null) {
                    val g = itemObj.optInt("grams", 0)
                    if (g > 0) {
                        packs.add(PackSizeEntity(g, itemObj.optString("customName", ""), itemObj.optInt("sortOrder", g)))
                    }
                } else {
                    val g = packsArr.optInt(i, 0)
                    if (g > 0) packs.add(PackSizeEntity(g, "", g))
                }
            }
        }

        val vendors = mutableListOf<VendorEntity>()
        val vendorsArr = root.optJSONArray("vendors")
        if (vendorsArr != null) {
            for (i in 0 until vendorsArr.length()) {
                val o = vendorsArr.getJSONObject(i)
                vendors.add(
                    VendorEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        phone = o.optString("phone", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        val catalog = mutableListOf<CatalogItemEntity>()
        val catalogArr = root.optJSONArray("catalogItems")
        if (catalogArr != null) {
            for (i in 0 until catalogArr.length()) {
                val o = catalogArr.getJSONObject(i)
                val vId = if (o.isNull("vendorId")) null else o.optLong("vendorId")
                catalog.add(
                    CatalogItemEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        sellingPricePerKg = o.optDouble("sellingPricePerKg", 0.0),
                        vendorCostPerKg = o.optDouble("vendorCostPerKg", 0.0),
                        vendorId = vId,
                        vendorName = o.optString("vendorName", ""),
                        stockGrams = o.optInt("stockGrams", 0),
                        lowStockThresholdGrams = o.optInt("lowStockThresholdGrams", 1000),
                        trackStock = o.optBoolean("trackStock", false),
                        packPriceOverridesJson = o.optString("packPriceOverridesJson", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        val orders = mutableListOf<OrderWithItemsEntity>()
        val ordersArr = root.optJSONArray("orders")
        if (ordersArr != null) {
            for (i in 0 until ordersArr.length()) {
                val o = ordersArr.getJSONObject(i)
                val orderEntity = OrderEntity(
                    id = 0L,
                    invoiceNumber = o.optString("invoiceNumber", ""),
                    customerName = o.optString("customerName", ""),
                    customerPhone = o.optString("customerPhone", ""),
                    customerAddress = o.optString("customerAddress", ""),
                    notes = o.optString("notes", ""),
                    status = o.optString("status", "PENDING"),
                    paymentStatus = o.optString("paymentStatus", "UNPAID"),
                    paidAmount = o.optDouble("paidAmount", 0.0),
                    deliveryCharge = o.optDouble("deliveryCharge", 0.0),
                    otherCharge = o.optDouble("otherCharge", 0.0),
                    discount = o.optDouble("discount", 0.0),
                    taxPercent = o.optDouble("taxPercent", 0.0),
                    subtotal = o.optDouble("subtotal", 0.0),
                    taxAmount = o.optDouble("taxAmount", 0.0),
                    grandTotal = o.optDouble("grandTotal", 0.0),
                    totalVendorCost = o.optDouble("totalVendorCost", 0.0),
                    sentAt = o.optLong("sentAt", 0L),
                    paidAt = o.optLong("paidAt", 0L),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
                val items = mutableListOf<OrderItemEntity>()
                val itemsArr = o.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val li = itemsArr.getJSONObject(j)
                        val cId = if (li.isNull("catalogItemId")) null else li.optLong("catalogItemId")
                        val vId = if (li.isNull("vendorId")) null else li.optLong("vendorId")
                        items.add(
                            OrderItemEntity(
                                id = 0L,
                                orderId = 0L,
                                catalogItemId = cId,
                                itemName = li.optString("itemName", ""),
                                packGrams = li.optInt("packGrams", 250),
                                quantity = li.optInt("quantity", 1),
                                unitPrice = li.optDouble("unitPrice", 0.0),
                                sellingPricePerKg = li.optDouble("sellingPricePerKg", 0.0),
                                vendorCostPerKg = li.optDouble("vendorCostPerKg", 0.0),
                                vendorId = vId,
                                vendorName = li.optString("vendorName", ""),
                                isFreeSample = li.optBoolean("isFreeSample", false),
                                isVendorLinePaid = li.optBoolean("isVendorLinePaid", false),
                                lineSaleTotal = li.optDouble("lineSaleTotal", 0.0),
                                lineCostTotal = li.optDouble("lineCostTotal", 0.0)
                            )
                        )
                    }
                }
                orders.add(OrderWithItemsEntity(orderEntity, items))
            }
        }

        val payments = mutableListOf<VendorPaymentEntity>()
        val payArr = root.optJSONArray("vendorPayments")
        if (payArr != null) {
            for (i in 0 until payArr.length()) {
                val p = payArr.getJSONObject(i)
                payments.add(
                    VendorPaymentEntity(
                        id = 0L,
                        vendorId = p.optLong("vendorId", 0L),
                        vendorName = p.optString("vendorName", ""),
                        amount = p.optDouble("amount", 0.0),
                        paymentDate = p.optLong("paymentDate", System.currentTimeMillis()),
                        mode = p.optString("mode", "UPI"),
                        note = p.optString("note", "")
                    )
                )
            }
        }

        return BackupPayload(
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            appLanguage = AppLanguage.fromCode(root.optString("appLanguage", "en")),
            invoiceLanguage = AppLanguage.fromCode(root.optString("invoiceLanguage", "en")),
            businessProfile = profile,
            lockedRates = root.optBoolean("lockedRates", false),
            stockTracking = root.optBoolean("stockTracking", false),
            packSizes = packs,
            vendors = vendors,
            catalogItems = catalog,
            orders = orders,
            vendorPayments = payments
        )
    }
}
