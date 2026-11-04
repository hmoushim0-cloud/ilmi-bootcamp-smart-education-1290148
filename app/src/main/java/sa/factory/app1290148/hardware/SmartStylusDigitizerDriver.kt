package sa.factory.app1290148.hardware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class StylusStrokeSample(
    val xCoordMm: Double,
    val yCoordMm: Double,
    val pressureLevel: Int,
    val tiltAzimuthDeg: Int,
    val tiltAltitudeDeg: Int,
    val isTipDown: Boolean,
    val isEraser: Boolean,
    val isBarrelButtonPressed: Boolean,
    val isInProximity: Boolean,
    val hapticFeedbackMode: Int
)

data class DigitizerStreamFrame(
    val sequenceNumber: Int,
    val sample: StylusStrokeSample,
    val isValidCrc: Boolean
)

data class StrokeDeviationAnalysis(
    val averagePressure: Double,
    val strokeLengthMm: Double,
    val isWithinLetterBoundary: Boolean,
    val maxDeviationMm: Double,
    val feedbackHapticRecommended: Int
)

class SmartStylusDigitizerDriver {

    companion object {
        const val MAGIC_HEADER: Short = 0xD161.toShort()
        const val PROTOCOL_VERSION: Byte = 0x03
        const val FRAME_SIZE_BYTES: Int = 16

        const val FLAG_TIP_DOWN: Int = 0x01
        const val FLAG_ERASER: Int = 0x02
        const val FLAG_BARREL_BTN: Int = 0x04
        const val FLAG_IN_PROXIMITY: Int = 0x08

        const val HAPTIC_OFF: Int = 0
        const val HAPTIC_BOUNDARY_ALERT: Int = 1
        const val HAPTIC_STROKE_CONFIRMED: Int = 2
    }

    fun computeCrc16Ccitt(data: ByteArray, offset: Int, length: Int): Short {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return (crc and 0xFFFF).toShort()
    }

    fun serializeFrame(
        sequenceNumber: Int,
        xMm: Double,
        yMm: Double,
        pressure: Int,
        tiltAzimuth: Int,
        tiltAltitude: Int,
        tipDown: Boolean,
        eraser: Boolean,
        barrelBtn: Boolean,
        proximity: Boolean,
        hapticMode: Int
    ): ByteArray {
        val buffer = ByteBuffer.allocate(FRAME_SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(MAGIC_HEADER)
        buffer.put(PROTOCOL_VERSION)
        buffer.put((sequenceNumber and 0xFF).toByte())

        val rawX = (xMm * 10.0).roundToInt().coerceIn(0, 65535)
        val rawY = (yMm * 10.0).roundToInt().coerceIn(0, 65535)
        buffer.putShort(rawX.toShort())
        buffer.putShort(rawY.toShort())

        val rawPressure = pressure.coerceIn(0, 4096)
        buffer.putShort(rawPressure.toShort())

        buffer.put((tiltAzimuth and 0xFF).toByte())
        buffer.put((tiltAltitude and 0xFF).toByte())

        var flags = 0
        if (tipDown) flags = flags or FLAG_TIP_DOWN
        if (eraser) flags = flags or FLAG_ERASER
        if (barrelBtn) flags = flags or FLAG_BARREL_BTN
        if (proximity) flags = flags or FLAG_IN_PROXIMITY
        buffer.put((flags and 0xFF).toByte())

        buffer.put((hapticMode and 0xFF).toByte())

        val crc = computeCrc16Ccitt(buffer.array(), 0, FRAME_SIZE_BYTES - 2)
        buffer.putShort(crc)

        return buffer.array()
    }

    fun parseFrame(packet: ByteArray): DigitizerStreamFrame {
        if (packet.size < FRAME_SIZE_BYTES) {
            throw IllegalArgumentException("Packet too short for digitizer frame: ${packet.size} bytes")
        }

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.short
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid digitizer magic: 0x${Integer.toHexString(magic.toInt() and 0xFFFF)}")
        }

        val version = buffer.get()
        if (version != PROTOCOL_VERSION) {
            throw IllegalArgumentException("Unsupported digitizer version: $version")
        }

        val seq = buffer.get().toInt() and 0xFF
        val rawX = buffer.short.toInt() and 0xFFFF
        val rawY = buffer.short.toInt() and 0xFFFF
        val rawPressure = buffer.short.toInt() and 0xFFFF
        val azimuth = buffer.get().toInt() and 0xFF
        val altitude = buffer.get().toInt() and 0xFF
        val flags = buffer.get().toInt() and 0xFF
        val haptic = buffer.get().toInt() and 0xFF

        val packetCrc = buffer.short
        val computedCrc = computeCrc16Ccitt(packet, 0, FRAME_SIZE_BYTES - 2)
        val isCrcValid = packetCrc == computedCrc

        val sample = StylusStrokeSample(
            xCoordMm = rawX / 10.0,
            yCoordMm = rawY / 10.0,
            pressureLevel = rawPressure,
            tiltAzimuthDeg = azimuth,
            tiltAltitudeDeg = altitude,
            isTipDown = (flags and FLAG_TIP_DOWN) != 0,
            isEraser = (flags and FLAG_ERASER) != 0,
            isBarrelButtonPressed = (flags and FLAG_BARREL_BTN) != 0,
            isInProximity = (flags and FLAG_IN_PROXIMITY) != 0,
            hapticFeedbackMode = haptic
        )

        return DigitizerStreamFrame(
            sequenceNumber = seq,
            sample = sample,
            isValidCrc = isCrcValid
        )
    }

    fun analyzeLetterTraceStroke(
        samples: List<StylusStrokeSample>,
        targetLetterTrajectory: List<Pair<Double, Double>>,
        maxAllowedToleranceMm: Double = 3.5
    ): StrokeDeviationAnalysis {
        if (samples.isEmpty()) {
            return StrokeDeviationAnalysis(0.0, 0.0, true, 0.0, HAPTIC_OFF)
        }

        var totalPressure = 0.0
        var totalLength = 0.0
        var maxDev = 0.0

        for (i in 0 until samples.size) {
            val s = samples[i]
            totalPressure += s.pressureLevel

            if (i > 0) {
                val prev = samples[i - 1]
                val dx = s.xCoordMm - prev.xCoordMm
                val dy = s.yCoordMm - prev.yCoordMm
                totalLength += sqrt(dx * dx + dy * dy)
            }

            // Calculate min distance to target letter guide points
            var minDistanceToGuide = Double.MAX_VALUE
            for (guidePoint in targetLetterTrajectory) {
                val gdx = s.xCoordMm - guidePoint.first
                val gdy = s.yCoordMm - guidePoint.second
                val dist = sqrt(gdx * gdx + gdy * gdy)
                if (dist < minDistanceToGuide) {
                    minDistanceToGuide = dist
                }
            }
            if (minDistanceToGuide != Double.MAX_VALUE && minDistanceToGuide > maxDev) {
                maxDev = minDistanceToGuide
            }
        }

        val avgP = totalPressure / samples.size
        val isCompliant = maxDev <= maxAllowedToleranceMm
        val haptic = if (isCompliant) HAPTIC_STROKE_CONFIRMED else HAPTIC_BOUNDARY_ALERT

        return StrokeDeviationAnalysis(
            averagePressure = (avgP * 10.0).roundToInt() / 10.0,
            strokeLengthMm = (totalLength * 10.0).roundToInt() / 10.0,
            isWithinLetterBoundary = isCompliant,
            maxDeviationMm = (maxDev * 10.0).roundToInt() / 10.0,
            feedbackHapticRecommended = haptic
        )
    }
}
