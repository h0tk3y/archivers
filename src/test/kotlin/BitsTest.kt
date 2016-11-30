
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by igushs on 11/8/16.
 */

class BitsTest {
    @Test fun simple() {
        val bits = BitInputStream(byteArrayOf(1).inputStream())
        assertEquals(8, bits.available())
        val bit0 = bits.nextBit()
        assertEquals(1, bit0)
        for (i in 1..7) {
            assertEquals(0, bits.nextBit())
        }
    }

    @Test fun writeAndRead() {
        val bits = BitOutputStream()
        bits.writeInt(123, 8)
        bits.writeBit(1)
        bits.writeBit(0)
        bits.writeBit(1)
        assertEquals(11, bits.size)
        val input = BitInputStream(bits.toByteArray().inputStream())
        assertEquals(123, input.nextInt(8))
        assertEquals(1, input.nextBit())
        assertEquals(0, input.nextBit())
        assertEquals(1, input.nextBit())
    }
}