package ir.mahdiparastesh.homechat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.Socket

@Suppress("BlockingMethodInNonBlockingContext")
class Transmitter(
    // private val c: Persistent,
    private val address: Pair<String, Int>,
    private val header: Radio.Header,
    private val data: suspend () -> ByteArray,
) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Socket(address.first, address.second).use {
                    it.getOutputStream().apply {
                        val d = data()
                        write(
                            byteArrayOf(header.value)
                                .plus(d.size.toByteArray(header.indicateLenInNBytes))
                                .plus(d)
                        )
                        flush()
                    }
                }
            } catch (e: ConnectException) { // TODO show a could not connect error
                Main.handler?.obtainMessage(3, e.message.toString())?.sendToTarget()
            }
        }
    }

    private fun Number.toByteArray(size: Int) =
        ByteArray(size) { i -> (toLong() shr (i * 8)).toByte() }
}
