package ir.mahdiparastesh.homechat

import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.min

@Suppress("FunctionName")
suspend fun Transmitter(
    address: Pair<String, Int>,
    header: Receiver.Header,
    data: suspend () -> ByteArray,
    failure: suspend () -> Unit,
    success: suspend (response: ByteArray) -> Unit
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
    } catch (_: SocketException) { // "Connection reset"
    }
    if (res != null) success(res)
    else failure()
}

@Throws(IOException::class, SocketException::class)
fun InputStream.readNBytesCompat(len: Int): ByteArray {
    require(len >= 0) { "len < 0" }
    var bufs: MutableList<ByteArray>? = null
    var result: ByteArray? = null
    var total = 0
    var remaining = len
    var n: Int
    do {
        val buf = ByteArray(min(remaining, DEFAULT_BUFFER_SIZE))
        var nread = 0
        while (read(buf, nread, min(buf.size - nread, remaining)).also { n = it } > 0) {
            nread += n
            remaining -= n
        }
        if (nread > 0) {
            if ((Int.MAX_VALUE - 8) - total < nread)
                throw OutOfMemoryError("Required array size too large")
            total += nread
            if (result == null) result = buf
            else {
                if (bufs == null) {
                    bufs = ArrayList()
                    bufs.add(result)
                }
                bufs.add(buf)
            }
        }
    } while (n >= 0 && remaining > 0)
    if (bufs == null) {
        if (result == null) return ByteArray(0)
        return if (result.size == total) result else result.copyOf(total)
    }
    result = ByteArray(total)
    var offset = 0
    remaining = total
    for (b in bufs) {
        val count = min(b.size, remaining)
        System.arraycopy(b, 0, result, offset, count)
        offset += count
        remaining -= count
    }
    return result
}

fun Number.toByteArray(): ByteArray {
    if (this is Byte) return byteArrayOf(this)
    val bb = when (this) {
        is Short -> ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this)
        is Int -> ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this)
        is Long -> ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this)
        else -> throw IllegalArgumentException()
    }
    bb.rewind()
    return bb.array()
}

@Suppress("UNCHECKED_CAST")
fun <N> ByteArray.toNumber(): N {
    if (size == Byte.SIZE_BYTES) return this[0] as N
    val bb = ByteBuffer.wrap(this)
    bb.rewind()
    return when (size) {
        Short.SIZE_BYTES -> bb.short as N
        Int.SIZE_BYTES -> bb.int as N
        Long.SIZE_BYTES -> bb.long as N
        else -> 0 as N
    }
}

fun <N> List<Byte>.toNumber(): N =
    toByteArray().toNumber<N>()
