package sa.factory.app1290148.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.app1290148.publishing.BindingType
import sa.factory.app1290148.publishing.BookTrimSize
import sa.factory.app1290148.publishing.CurriculumBookMetadata
import sa.factory.app1290148.publishing.KhatwahCurriculumPublishingEngine

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "Smart Stylus Digitizer (4096 Levels) + Micro-Haptic Actuator"
    private val digitizerDriver = SmartStylusDigitizerDriver()
    private val publishingEngine = KhatwahCurriculumPublishingEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Smart Stylus 4096-level Digitizer pairing over BLE..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "Smart Stylus Digitizer: ONLINE | PROTOCOL READY [0xD161]"))

        var sequence = 100
        while (true) {
            delay(1500)
            sequence++

            val bookMeta = CurriculumBookMetadata(
                bookId = "BK-KHATWAH-2026-$sequence",
                titleArabic = "سلسلة خطوة لتأسيس القراءة والكتابة",
                targetAgeStage = "رياض أطفال 4-6 سنوات",
                isbn13 = "978-603-01-2345-6",
                kingFahdDepositNumber = "1446/9120",
                pageCount = 32,
                trimSize = BookTrimSize.A5_LANDSCAPE,
                binding = BindingType.SADDLE_STITCH
            )

            // Simulate handwriting letter trace stroke on A5 landscape spread
            val packet = digitizerDriver.serializeFrame(
                sequenceNumber = sequence,
                xMm = 105.0,
                yMm = 74.0,
                pressure = 2048,
                tiltAzimuth = 45,
                tiltAltitude = 60,
                tipDown = true,
                eraser = false,
                barrelBtn = false,
                proximity = true,
                hapticMode = SmartStylusDigitizerDriver.HAPTIC_STROKE_CONFIRMED
            )

            val frame = digitizerDriver.parseFrame(packet)

            val payload = String.format(
                "Digitizer Frame #%d: Pos(%.1f, %.1f mm) | Pressure: %d/4096 | Tilt(%d, %d deg) | Haptic: %d | CRC: %s",
                frame.sequenceNumber,
                frame.sample.xCoordMm,
                frame.sample.yCoordMm,
                frame.sample.pressureLevel,
                frame.sample.tiltAzimuthDeg,
                frame.sample.tiltAltitudeDeg,
                frame.sample.hapticFeedbackMode,
                if (frame.isValidCrc) "VALID" else "INVALID"
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
