package sa.factory.app1290148

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sa.factory.app1290148.hardware.SmartStylusDigitizerDriver
import sa.factory.app1290148.hardware.StylusStrokeSample
import sa.factory.app1290148.publishing.*

class KhatwahPublishingFeatureTest {

    private lateinit var publishingEngine: KhatwahCurriculumPublishingEngine
    private lateinit var digitizerDriver: SmartStylusDigitizerDriver

    @Before
    fun setUp() {
        publishingEngine = KhatwahCurriculumPublishingEngine()
        digitizerDriver = SmartStylusDigitizerDriver()
    }

    @Test
    fun testIsbn13ChecksumValidation() {
        // Valid EAN-13: 9786030123452 (Sum = 98 -> (10 - 8) = 2)
        assertTrue(publishingEngine.validateIsbn13Checksum("978-603-01-2345-2"))
        assertTrue(publishingEngine.validateIsbn13Checksum("9786030123452"))

        // Invalid check digit
        assertFalse(publishingEngine.validateIsbn13Checksum("978-603-01-2345-6"))
        assertFalse(publishingEngine.validateIsbn13Checksum("978-603-01"))
        assertFalse(publishingEngine.validateIsbn13Checksum("978-603-01-2345-X"))
    }

    @Test
    fun testSpineWidthCalculation() {
        // Saddle stitch has 0.0mm spine
        val saddleSpine = publishingEngine.calculateSpineWidthMm(32, 135, BindingType.SADDLE_STITCH)
        assertEquals(0.0, saddleSpine, 0.01)

        // Perfect bound: 64 pages (32 sheets), 135 gsm -> 32 * (0.135 * 0.95) = 4.104 -> 4.1 mm
        val perfectSpine = publishingEngine.calculateSpineWidthMm(64, 135, BindingType.PERFECT_BOUND)
        assertEquals(4.1, perfectSpine, 0.1)

        // Hardcover adds 4.0mm board allowance
        val hardcoverSpine = publishingEngine.calculateSpineWidthMm(64, 135, BindingType.HARDCOVER_CASE)
        assertEquals(8.1, hardcoverSpine, 0.1)
    }

    @Test
    fun testPreflightInspectionSaddleStitchApproval() {
        val validBook = CurriculumBookMetadata(
            bookId = "BK-KHT-001",
            titleArabic = "كتيب تأسيس الحروف الهجائية بالرسم القرآني",
            targetAgeStage = "رياض أطفال 4-6 سنوات",
            isbn13 = "978-603-01-2345-2",
            kingFahdDepositNumber = "1446/9120",
            pageCount = 32, // Multiple of 4
            trimSize = BookTrimSize.A5_LANDSCAPE,
            colorSpace = ColorSpaceProfile.CMYK_FOGRA39,
            binding = BindingType.SADDLE_STITCH,
            paperWeightGsm = 135,
            bleedMarginMm = 3.0,
            safeZoneMarginMm = 5.0
        )

        val report = publishingEngine.runPreflightInspection(validBook)
        assertTrue(report.isPreflightApproved)
        assertEquals(0, report.preflightViolations.size)
        assertEquals(0.0, report.calculatedSpineWidthMm, 0.01)

        // Spread width: 2 * 210.0 + 0 + 2 * 3.0 = 426.0 mm
        assertEquals(426.0, report.totalSpreadWidthMm, 0.1)
        // Spread height: 148.0 + 2 * 3.0 = 154.0 mm
        assertEquals(154.0, report.totalSpreadHeightMm, 0.1)
        assertEquals(2, report.imposedPlateCount) // 32 pages / 16 = 2 plates
        assertTrue(report.certificationArabic.contains("معتمد طباعيا"))
    }

    @Test
    fun testPreflightInspectionRejectionViolations() {
        val invalidBook = CurriculumBookMetadata(
            bookId = "BK-KHT-ERR",
            titleArabic = "كراس التلوين",
            targetAgeStage = "رياض أطفال",
            isbn13 = "978-603-01-2345-9", // Bad checksum
            kingFahdDepositNumber = "InvalidFormat", // Missing slash
            pageCount = 30, // Not multiple of 4
            trimSize = BookTrimSize.A5_LANDSCAPE,
            colorSpace = ColorSpaceProfile.RGB_DIGITAL_PREVIEW, // RGB rejection
            binding = BindingType.SADDLE_STITCH,
            bleedMarginMm = 1.5, // Bleed < 3.0 mm
            safeZoneMarginMm = 3.0 // Safe zone < 5.0 mm
        )

        val report = publishingEngine.runPreflightInspection(invalidBook)
        assertFalse(report.isPreflightApproved)
        assertTrue(report.preflightViolations.size >= 5)
        assertTrue(report.preflightViolations.any { it.contains("ISBN-13") })
        assertTrue(report.preflightViolations.any { it.contains("مضاعفا للرقم 4") })
        assertTrue(report.preflightViolations.any { it.contains("RGB") })
        assertTrue(report.preflightViolations.any { it.contains("هامش التسييل") })
    }

    @Test
    fun testPedagogicalReadabilityEvaluation() {
        // Foundation early childhood: 20 words per page, 40% activity pages
        val earlyChildhood = publishingEngine.evaluatePedagogicalReadability(
            totalWords = 400,
            uniqueWords = 180,
            pageCount = 20,
            interactiveActivityPages = 8
        )
        assertEquals(20.0, earlyChildhood.averageWordsPerPage, 0.1)
        assertEquals(0.45, earlyChildhood.vocabularyDiversityRatio, 0.05)
        assertEquals(0.40, earlyChildhood.interactiveActivityRatio, 0.01)
        assertTrue(earlyChildhood.pedagogicalSuitabilityArabic.contains("رياض الأطفال"))
    }

    @Test
    fun testSmartStylusDigitizerSerializationAndParsing() {
        val seq = 77
        val x = 120.5 // mm
        val y = 85.0  // mm
        val pressure = 2048
        val azimuth = 90
        val altitude = 45
        val haptic = SmartStylusDigitizerDriver.HAPTIC_STROKE_CONFIRMED

        val packet = digitizerDriver.serializeFrame(
            sequenceNumber = seq,
            xMm = x,
            yMm = y,
            pressure = pressure,
            tiltAzimuth = azimuth,
            tiltAltitude = altitude,
            tipDown = true,
            eraser = false,
            barrelBtn = false,
            proximity = true,
            hapticMode = haptic
        )

        assertEquals(SmartStylusDigitizerDriver.FRAME_SIZE_BYTES, packet.size)

        val frame = digitizerDriver.parseFrame(packet)
        assertEquals(seq, frame.sequenceNumber)
        assertEquals(x, frame.sample.xCoordMm, 0.1)
        assertEquals(y, frame.sample.yCoordMm, 0.1)
        assertEquals(pressure, frame.sample.pressureLevel)
        assertEquals(azimuth, frame.sample.tiltAzimuthDeg)
        assertEquals(altitude, frame.sample.tiltAltitudeDeg)
        assertTrue(frame.sample.isTipDown)
        assertFalse(frame.sample.isEraser)
        assertTrue(frame.sample.isInProximity)
        assertEquals(haptic, frame.sample.hapticFeedbackMode)
        assertTrue("CRC-16-CCITT should be verified as valid", frame.isValidCrc)

        // Corrupted packet check
        val corrupted = packet.copyOf()
        corrupted[8] = (corrupted[8].toInt() xor 0xFF).toByte()
        val corruptFrame = digitizerDriver.parseFrame(corrupted)
        assertFalse("Corrupted frame CRC must fail verification", corruptFrame.isValidCrc)
    }

    @Test
    fun testLetterTraceBoundaryAndHapticFeedback() {
        // Target guide path for Arabic Letter Alif (vertical stroke from (50, 20) down to (50, 70))
        val targetAlifTrajectory = listOf(
            Pair(50.0, 20.0),
            Pair(50.0, 35.0),
            Pair(50.0, 50.0),
            Pair(50.0, 70.0)
        )

        // Compliant child stroke within 1.0mm deviation
        val goodSamples = listOf(
            StylusStrokeSample(50.2, 20.1, 1500, 45, 60, true, false, false, true, 0),
            StylusStrokeSample(50.5, 35.0, 1800, 45, 60, true, false, false, true, 0),
            StylusStrokeSample(49.8, 50.2, 2100, 45, 60, true, false, false, true, 0),
            StylusStrokeSample(50.1, 69.8, 1900, 45, 60, true, false, false, true, 0)
        )

        val goodAnalysis = digitizerDriver.analyzeLetterTraceStroke(goodSamples, targetAlifTrajectory, 3.5)
        assertTrue(goodAnalysis.isWithinLetterBoundary)
        assertTrue(goodAnalysis.maxDeviationMm <= 3.5)
        assertEquals(SmartStylusDigitizerDriver.HAPTIC_STROKE_CONFIRMED, goodAnalysis.feedbackHapticRecommended)

        // Straying stroke deviating by 8.0mm
        val straySamples = listOf(
            StylusStrokeSample(50.2, 20.1, 1500, 45, 60, true, false, false, true, 0),
            StylusStrokeSample(58.5, 35.0, 1800, 45, 60, true, false, false, true, 0) // Deviates by 8.5mm
        )

        val strayAnalysis = digitizerDriver.analyzeLetterTraceStroke(straySamples, targetAlifTrajectory, 3.5)
        assertFalse(strayAnalysis.isWithinLetterBoundary)
        assertTrue(strayAnalysis.maxDeviationMm > 3.5)
        assertEquals(SmartStylusDigitizerDriver.HAPTIC_BOUNDARY_ALERT, strayAnalysis.feedbackHapticRecommended)
    }
}
