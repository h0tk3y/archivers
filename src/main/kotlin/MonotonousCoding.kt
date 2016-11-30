/**
 * Created by igushs on 11/22/16.
 */
/**
 * Converts [i] to a simple monotonous code, writing its length in bits using unary code first,
 * then its bits without the implicit '1'.
 *
 * @sample i = 11
 *          11 -> 1011 (4 bits, remove leading 1) -> 011
 *          4 -> 11110
 *          result = (11110)(1011) = 111101011
 */
fun monotonous(i: Int): List<Int> {
    val bits = i.toBits().reversed().drop(1)
    val len = (1..bits.size).map { 1 } + 0
    return len + bits
}


/**
 * Reads a monotonous code of a positive integer from [stream].
 *
 * @see [monotonous]
 */
fun fromMonotonous(stream: BitInputStream): Int {
    var b = stream.nextBit()
    var len = 0
    while (b != 0) {
        ++len
        b = stream.nextBit()
    }
    val bits = (stream.nextBits(len)).reversed() + 1
    return bits.asBitsOfInt()
}