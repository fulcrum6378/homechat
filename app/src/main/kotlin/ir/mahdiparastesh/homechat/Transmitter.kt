package ir.mahdiparastesh.homechat

import ir.mahdiparastesh.homechat.Radio.Companion.readNBytesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.Socket

@Suppress("BlockingMethodInNonBlockingContext")
class Transmitter(
    private val address: Pair<String, Int>,
    private val header: Radio.Header,
    private val responseLength: Int = 0,
    private val data: suspend () -> ByteArray,
    private val response: (suspend (response: ByteArray?) -> Unit)? = null
) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            var res: ByteArray? = null
            try {
                Socket(address.first, address.second).use {
                    it.getOutputStream().apply {
                        val d = data()
                        write(byteArrayOf(header.value).plus(header.put(d.size)).plus(d))
                        flush()
                    }
                    if (responseLength > 0)
                        it.getInputStream().apply { res = readNBytesCompat(responseLength) }
                }
            } catch (_: ConnectException) {
            }
            response?.also { it(res) }
        }
    }
}
