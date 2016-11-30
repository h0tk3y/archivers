import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

/**
 * Created by igushs on 11/8/16.
 */

fun List<Int>.asBitsOfInt(): Int {
    var result = 0
    for (i in 0..31 intersect indices)
        if (get(i) > 0)
            result += 1 shl i
    return result
}

fun bitsForNValues(nValues: Int): Int {
    var n = nValues
    var result = 0
    var roundUp = false
    while (n > 1) {
        ++result
        if (n % 2 == 1)
            roundUp = true
        n /= 2
    }
    return result + if (roundUp) 1 else 0
}

fun Int.toBits(nBits: Int = -1): List<Int> {
    var n = this
    var l = nBits
    val result = ArrayList<Int>()
    while (n > 0 && l-- != 0 ) {
        result.add(n % 2 and 1)
        n = n ushr 1
    }
    while (l-- > 0) {
        result.add(0)
    }
    return result
}

class BitInputStream(input: InputStream) {
    private val bytes = input.readBytes(input.available())
    private var position: Int = 0

    val bitsLength = this.bytes.size * 8

    fun available() = bitsLength - position

    fun nextBit(): Int {
        if (position >= bitsLength)
            throw NoSuchElementException()

        val result = bytes[position / 8].toInt().shr(position % 8).and(1)
        ++position
        return result
    }

    fun nextBits(n: Int): List<Int> {
        val result = ArrayList<Int>(n)
        for (i in 1..n)
            result.add(nextBit())
        return result
    }
}

fun BitInputStream.nextInt(nBits: Int) = nextBits(nBits.coerceAtMost(available())).asBitsOfInt()

class BitOutputStream() {
    private val bytes = ByteArrayOutputStream(0)
    var positionInByte = 0
    private var currentByte = 0

    val size: Int get() = bytes.size() * 8 + positionInByte

    fun writeBit(i: Int) {
        require(i in 0..1)
        if (i == 1)
            currentByte = currentByte.or(1 shl positionInByte)
        ++positionInByte
        if (positionInByte == 8) {
            bytes.write(currentByte)
            currentByte = 0
            positionInByte = 0
        }
    }

    fun writeBits(bits: List<Int>, asBits: Int = bits.size) {
        for (i in 1..asBits)
            writeBit(bits.getOrElse(i - 1) { 0 })
    }

    fun toByteArray(): ByteArray {
        val result = bytes.toByteArray()
        return if (positionInByte == 0)
            result else
            result + currentByte.toByte()
    }
}

fun BitOutputStream.writeInt(i: Int, asBits: Int) {
    writeBits(i.toBits(asBits), asBits)
}