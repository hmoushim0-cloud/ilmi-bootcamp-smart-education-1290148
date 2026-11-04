package sa.factory.app1290148.publishing

import kotlin.math.roundToInt

enum class BookTrimSize(val widthMm: Double, val heightMm: Double) {
    A4_PORTRAIT(210.0, 297.0),
    A5_LANDSCAPE(210.0, 148.0),
    CROWN_QUARTO(189.0, 246.0),
    SQUARE_CHILDREN(200.0, 200.0)
}

enum class ColorSpaceProfile {
    CMYK_FOGRA39,
    CMYK_GRACOL,
    RGB_DIGITAL_PREVIEW
}

enum class BindingType {
    SADDLE_STITCH,   // دبوس وسط (مناسب لكراسات التأسيس حتى 48 صفحة)
    PERFECT_BOUND,   // غراء حراري مقوى
    HARDCOVER_CASE   // غلاف مقوى مجلد
}

data class CurriculumBookMetadata(
    val bookId: String,
    val titleArabic: String,
    val targetAgeStage: String,
    val isbn13: String,
    val kingFahdDepositNumber: String,
    val pageCount: Int,
    val trimSize: BookTrimSize,
    val colorSpace: ColorSpaceProfile = ColorSpaceProfile.CMYK_FOGRA39,
    val binding: BindingType = BindingType.SADDLE_STITCH,
    val paperWeightGsm: Int = 135,
    val bleedMarginMm: Double = 3.0,
    val safeZoneMarginMm: Double = 5.0
)

data class PrintReadyPreflightReport(
    val bookId: String,
    val isPreflightApproved: Boolean,
    val calculatedSpineWidthMm: Double,
    val totalSpreadWidthMm: Double,
    val totalSpreadHeightMm: Double,
    val imposedPlateCount: Int,
    val preflightViolations: List<String>,
    val certificationArabic: String
)

data class ReadabilityMetricsReport(
    val vocabularyDiversityRatio: Double,
    val averageWordsPerPage: Double,
    val interactiveActivityRatio: Double,
    val pedagogicalSuitabilityArabic: String
)

class KhatwahCurriculumPublishingEngine {

    companion object {
        const val STANDARD_BLEED_MM: Double = 3.0
        const val STANDARD_SAFE_ZONE_MM: Double = 5.0
    }

    fun validateIsbn13Checksum(isbn: String): Boolean {
        val cleaned = isbn.replace("-", "").trim()
        if (cleaned.length != 13 || !cleaned.all { it.isDigit() }) return false

        var sum = 0
        for (i in 0 until 12) {
            val digit = cleaned[i] - '0'
            val weight = if (i % 2 == 0) 1 else 3
            sum += digit * weight
        }
        val expectedCheckDigit = (10 - (sum % 10)) % 10
        val actualCheckDigit = cleaned[12] - '0'
        return expectedCheckDigit == actualCheckDigit
    }

    fun calculateSpineWidthMm(pageCount: Int, paperGsm: Int, binding: BindingType): Double {
        if (binding == BindingType.SADDLE_STITCH) {
            return 0.0 // دبوس الوسط لا يتطلب كعبا مستقلا
        }

        // Caliper in mm per sheet (135 gsm coated paper ~ 0.125 mm per sheet)
        val sheetCount = (pageCount + 1) / 2
        val caliperPerSheet = (paperGsm / 1000.0) * 0.95
        var spine = sheetCount * caliperPerSheet

        if (binding == BindingType.HARDCOVER_CASE) {
            spine += 4.0 // إضافة سمك الكرتون والمفصلات للغلاف المقوى
        }

        return (spine * 10.0).roundToInt() / 10.0
    }

    fun runPreflightInspection(metadata: CurriculumBookMetadata): PrintReadyPreflightReport {
        val violations = mutableListOf<String>()

        // 1. ISBN-13 Checksum Validation
        if (!validateIsbn13Checksum(metadata.isbn13)) {
            violations.add("خطأ في الرقم الدولي المعياري للكتاب (ISBN-13): فحص المجموع غير متطابق")
        }

        // 2. King Fahd National Library Deposit Number Format
        if (metadata.kingFahdDepositNumber.isBlank() || !metadata.kingFahdDepositNumber.contains("/")) {
            violations.add("رقم الإيداع النظامي بمكتبة الملك فهد الوطنية مفقود أو غير مطابق للصيغة النظامية (مثال: 1446/7890)")
        }

        // 3. Saddle Stitch Page Count Rule: Must be a multiple of 4
        if (metadata.binding == BindingType.SADDLE_STITCH && metadata.pageCount % 4 != 0) {
            violations.add("تجليد الدبوس (Saddle Stitch) يفرض أن يكون عدد الصفحات مضاعفا للرقم 4 (الحالي: ${metadata.pageCount} صفحة)")
        }

        // 4. Perfect Bound Minimum Page Count Rule
        if (metadata.binding == BindingType.PERFECT_BOUND && metadata.pageCount < 40) {
            violations.add("تجليد الغراء الحراري (Perfect Bound) يتطلب 40 صفحة على الأقل لتشكيل كعب متماسك (الحالي: ${metadata.pageCount} صفحة)")
        }

        // 5. Offset Print Color Profile
        if (metadata.colorSpace == ColorSpaceProfile.RGB_DIGITAL_PREVIEW) {
            violations.add("الملف بتنسيق ألوان RGB رقمي - يلزم التحويل إلى مساحة ألوان الطباعة الأوفست CMYK FOGRA39 لمنع انحراف الألوان")
        }

        // 6. Margins & Bleed Compliance
        if (metadata.bleedMarginMm < STANDARD_BLEED_MM) {
            violations.add("هامش التسييل (Bleed) أقل من الحد الأدنى للمطابع التجارية البالغ 3.0 ملم (الحالي: ${metadata.bleedMarginMm} ملم)")
        }
        if (metadata.safeZoneMarginMm < STANDARD_SAFE_ZONE_MM) {
            violations.add("منطقة الأمان للنصوص والعناصر أقل من 5.0 ملم مما يعرض المحتوى لخطر القص الميكانيكي")
        }

        val isApproved = violations.isEmpty()
        val spine = calculateSpineWidthMm(metadata.pageCount, metadata.paperWeightGsm, metadata.binding)

        // Spread width = (2 * page width) + spine + (2 * bleed)
        val spreadWidth = (2 * metadata.trimSize.widthMm) + spine + (2 * metadata.bleedMarginMm)
        val spreadHeight = metadata.trimSize.heightMm + (2 * metadata.bleedMarginMm)

        // 8-page or 16-page commercial printing signature imposition
        val plates = (metadata.pageCount + 15) / 16

        val certification = if (isApproved) {
            "الملف معتمد طباعيا وجاهز للفرز اللوني وإنتاج ألواح CTP للمطبعة التجارية"
        } else {
            "الملف غير مطابق للاشتراطات الفنية للمطابع - يلزم تصحيح الملاحظات قبل اعتماد الطباعة"
        }

        return PrintReadyPreflightReport(
            bookId = metadata.bookId,
            isPreflightApproved = isApproved,
            calculatedSpineWidthMm = spine,
            totalSpreadWidthMm = (spreadWidth * 10.0).roundToInt() / 10.0,
            totalSpreadHeightMm = (spreadHeight * 10.0).roundToInt() / 10.0,
            imposedPlateCount = plates,
            preflightViolations = violations,
            certificationArabic = certification
        )
    }

    fun evaluatePedagogicalReadability(
        totalWords: Int,
        uniqueWords: Int,
        pageCount: Int,
        interactiveActivityPages: Int
    ): ReadabilityMetricsReport {
        if (pageCount <= 0 || totalWords <= 0) {
            throw IllegalArgumentException("Page count and words must be positive")
        }

        val diversity = (uniqueWords.toDouble() / totalWords.toDouble())
        val wordsPerPage = totalWords.toDouble() / pageCount.toDouble()
        val activityRatio = interactiveActivityPages.toDouble() / pageCount.toDouble()

        val suitability = when {
            wordsPerPage <= 25.0 && activityRatio >= 0.30 ->
                "مثالي لمرحلة التأسيس المبكر ورياض الأطفال (تركيز بصري ونشاط تفاعلي عال)"
            wordsPerPage in 26.0..60.0 && activityRatio >= 0.20 ->
                "مناسب للمرحلة الابتدائية (الصفوف الأولية الأول والثاني الابتدائي)"
            else ->
                "محتوى نصي مكثف يلائم المرحلة المتوسطة أو كتب القراءة الإثرائية المتقدمة"
        }

        return ReadabilityMetricsReport(
            vocabularyDiversityRatio = (diversity * 100.0).roundToInt() / 100.0,
            averageWordsPerPage = (wordsPerPage * 10.0).roundToInt() / 10.0,
            interactiveActivityRatio = (activityRatio * 100.0).roundToInt() / 100.0,
            pedagogicalSuitabilityArabic = suitability
        )
    }
}
