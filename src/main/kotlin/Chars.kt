import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult

/**
 * Created by igushs on 11/8/16.
 */

private const val lowercaseStart = 0
private const val lowercaseCount = 'я' - 'а' + 1
private const val lowercaseEnd = lowercaseStart + lowercaseCount - 1

private const val capitalsStart = lowercaseEnd + 1
private const val capitalsCount = 'Я' - 'А' + 1
private const val capitalsEnd = capitalsStart + capitalsCount - 1

private const val punctuationStart = capitalsEnd + 1

fun Int.toSimpleCyrillic() = when (this) {
    in lowercaseStart..lowercaseEnd -> 'а' + (this - lowercaseStart)
    in capitalsStart..capitalsEnd -> 'А' + (this - capitalsStart)
    punctuationStart + 0 -> ' '
    punctuationStart + 1 -> '.'
    punctuationStart + 2 -> ','
    punctuationStart + 3 -> '!'
    punctuationStart + 4 -> '-'
    else -> 0.toChar()
}

fun Char.toIntAsSimpleCyrillic() = when (this) {
    in 'а'..'я' -> lowercaseStart + (this - 'а')
    in 'А'..'Я' -> capitalsStart + (this - 'А')
    ' ' -> punctuationStart + 0
    '.' -> punctuationStart + 1
    ',' -> punctuationStart + 2
    '!' -> punctuationStart + 3
    '-' -> punctuationStart + 4
    else -> -1
}

class SimpleCyrillicCharSet : Charset("simple-cyrillic", emptyArray()) {
    override fun contains(cs: Charset?): Boolean = false

    override fun newEncoder() = object : CharsetEncoder(this, 1f, 1f) {
        override fun encodeLoop(input: CharBuffer, out: ByteBuffer): CoderResult {
            for (i in 1..Math.min(input.remaining(), out.remaining())) {
                val c = input.get()
                val int = c.toIntAsSimpleCyrillic()
                if (int == -1)
                    return CoderResult.malformedForLength(i)
                out.put(int.toByte())
            }
            return if (input.remaining() == 0) CoderResult.UNDERFLOW else CoderResult.OVERFLOW
        }
    }

    override fun newDecoder() = object : CharsetDecoder(this, 1f, 1f) {
        override fun decodeLoop(input: ByteBuffer, out: CharBuffer): CoderResult {
            for (i in 1..Math.min(input.remaining(), out.remaining())) {
                val b = input.get()
                val char = b.toInt().toSimpleCyrillic()
                if (char == 0.toChar())
                    return CoderResult.malformedForLength(i)
                out.put(char)
            }
            return if (input.remaining() == 0) CoderResult.UNDERFLOW else CoderResult.OVERFLOW
        }
    }

}