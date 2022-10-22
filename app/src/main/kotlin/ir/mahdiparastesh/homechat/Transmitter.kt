package ir.mahdiparastesh.homechat

import ir.mahdiparastesh.homechat.Receiver.Companion.readNBytesCompat
import java.net.ConnectException
import java.net.Socket

@Suppress("BlockingMethodInNonBlockingContext", "FunctionName")
suspend fun Transmitter(
    address: Pair<String, Int>,
    header: Receiver.Header,
    data: suspend () -> ByteArray,
    response: (suspend (response: ByteArray?) -> Unit)? = null
) {
    var res: ByteArray? = null
    try {
        Socket(address.first, address.second).use {
            it.getOutputStream().apply {
                val d = data()
                write(byteArrayOf(header.value).plus(header.putLength(d.size)).plus(d))
                flush()
            }
            if (header.responseBytes > 0)
                it.getInputStream().apply { res = readNBytesCompat(header.responseBytes) }
        }
    } catch (_: ConnectException) {
    }
    response?.also { it(res) }
}
