package com.example.export

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.data.model.BusinessProfile
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 420,
        darkColor: Int = Color.parseColor("#1A2419"),
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content.ifBlank { "ArthFood" }, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) darkColor else lightColor
            }
        }
        bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bmp
    }

    fun buildVCardString(profile: BusinessProfile): String {
        val name = profile.qrContactName.ifBlank { profile.businessName }.ifBlank { "ArthFood Kitchen" }
        val tel = normalizePhoneForUrl(profile.phone)
        val wa = normalizePhoneForUrl(profile.effectiveWhatsApp)
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:$name")
            if (profile.businessName.isNotBlank()) appendLine("ORG:${profile.businessName}")
            if (tel.isNotBlank()) appendLine("TEL;TYPE=CELL,VOICE:+$tel")
            if (wa.isNotBlank() && wa != tel) appendLine("TEL;TYPE=MSG:+$wa")
            if (profile.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${profile.address.replace("\n", ", ")};;;;")
            if (profile.tagline.isNotBlank()) appendLine("NOTE:${profile.tagline}")
            append("END:VCARD")
        }
    }

    fun buildWhatsAppUrl(phone: String, message: String = ""): String {
        val digits = normalizePhoneForUrl(phone)
        if (digits.isBlank()) return "https://wa.me/"
        return if (message.isBlank()) {
            "https://wa.me/$digits"
        } else {
            "https://wa.me/$digits?text=${Uri.encode(message)}"
        }
    }

    fun normalizePhoneForUrl(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.startsWith("0") && digits.length == 11 -> "91${digits.drop(1)}"
            else -> digits
        }
    }
}
