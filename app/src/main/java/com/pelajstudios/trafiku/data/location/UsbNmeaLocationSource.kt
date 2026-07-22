package com.pelajstudios.trafiku.data.location

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@OptIn(ExperimentalCoroutinesApi::class)
internal class UsbNmeaLocationSource(
    private val context: Context,
    private val usbManager: UsbManager,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : LocationSource {
    private val deviceRevision = MutableStateFlow(0)
    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION,
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                -> deviceRevision.value += 1
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            deviceReceiver,
            IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun events(): Flow<LocationSourceEvent> = deviceRevision
        .flatMapLatest { connectToAvailableDevice() }

    fun requestPermission() {
        val driver = UsbSerialProber.getDefaultProber()
            .findAllDrivers(usbManager)
            .firstOrNull() ?: return
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            USB_PERMISSION_REQUEST_CODE,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        usbManager.requestPermission(driver.device, permissionIntent)
    }

    private fun connectToAvailableDevice(): Flow<LocationSourceEvent> = flow {
        val driver = UsbSerialProber.getDefaultProber()
            .findAllDrivers(usbManager)
            .firstOrNull()
        if (driver == null) {
            emit(LocationSourceEvent.Unavailable(LocationSourceKind.USB))
            awaitCancellation()
        }

        val device = checkNotNull(driver).device
        if (!usbManager.hasPermission(device)) {
            emit(
                LocationSourceEvent.UsbPermissionRequired(
                    deviceName = device.displayName(),
                ),
            )
            awaitCancellation()
        }

        val connection = usbManager.openDevice(device)
        val port = driver.ports.firstOrNull()
        if (connection == null || port == null) {
            emit(LocationSourceEvent.Error(LocationSourceKind.USB))
            awaitCancellation()
        }

        try {
            checkNotNull(port).open(checkNotNull(connection))
            emit(LocationSourceEvent.Acquiring(LocationSourceKind.USB))
            readNmea(port).collect(::emit)
        } catch (_: IOException) {
            emit(LocationSourceEvent.Error(LocationSourceKind.USB))
        } finally {
            runCatching { port?.close() }
            connection?.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun readNmea(port: UsbSerialPort): Flow<LocationSourceEvent> = flow {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        val textBuffer = StringBuilder()
        var selectedBaudRate: Int? = null

        for (baudRate in SUPPORTED_BAUD_RATES) {
            port.setParameters(
                baudRate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            runCatching { port.purgeHwBuffers(true, true) }
            textBuffer.clear()
            val parser = NmeaParser(elapsedRealtimeMillis)
            val deadline = elapsedRealtimeMillis() + BAUD_PROBE_TIME_MILLIS
            while (elapsedRealtimeMillis() < deadline && currentCoroutineContext().isActive) {
                val fixes = port.readSentences(buffer, textBuffer, parser)
                fixes.forEach { fix -> emit(LocationSourceEvent.Fix(fix)) }
                if (fixes.isNotEmpty()) {
                    selectedBaudRate = baudRate
                    break
                }
            }
            if (selectedBaudRate != null) {
                break
            }
        }

        if (selectedBaudRate == null) {
            emit(LocationSourceEvent.Error(LocationSourceKind.USB))
            return@flow
        }

        val parser = NmeaParser(elapsedRealtimeMillis)
        while (currentCoroutineContext().isActive) {
            port.readSentences(buffer, textBuffer, parser)
                .forEach { fix -> emit(LocationSourceEvent.Fix(fix)) }
        }
    }

    private fun UsbSerialPort.readSentences(
        buffer: ByteArray,
        textBuffer: StringBuilder,
        parser: NmeaParser,
    ): List<LocationFix> {
        val byteCount = read(buffer, READ_TIMEOUT_MILLIS)
        if (byteCount <= 0) {
            return emptyList()
        }

        textBuffer.append(buffer.decodeToString(endIndex = byteCount))
        val fixes = mutableListOf<LocationFix>()
        var newlineIndex = textBuffer.indexOf("\n")
        while (newlineIndex >= 0) {
            val sentence = textBuffer.substring(0, newlineIndex)
            textBuffer.delete(0, newlineIndex + 1)
            parser.parse(sentence)?.let(fixes::add)
            newlineIndex = textBuffer.indexOf("\n")
        }
        if (textBuffer.length > MAXIMUM_SENTENCE_BUFFER_SIZE) {
            textBuffer.clear()
        }
        return fixes
    }

    private fun UsbDevice.displayName(): String = productName
        ?.takeIf(String::isNotBlank)
        ?: "USB ${String.format(Locale.US, "%04X:%04X", vendorId, productId)}"

    private companion object {
        const val ACTION_USB_PERMISSION = "com.pelajstudios.trafiku.USB_PERMISSION"
        const val USB_PERMISSION_REQUEST_CODE = 80
        const val READ_BUFFER_SIZE = 4_096
        const val READ_TIMEOUT_MILLIS = 500
        const val BAUD_PROBE_TIME_MILLIS = 3_000L
        const val MAXIMUM_SENTENCE_BUFFER_SIZE = 8_192
        val SUPPORTED_BAUD_RATES = listOf(4_800, 9_600, 38_400, 115_200)
    }
}
