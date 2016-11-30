import com.github.h0tk3y.kotlinFun.lexicographically
import java.io.InputStream
import kotlin.comparisons.compareBy
import kotlin.comparisons.naturalOrder

fun <T> runLengthEncode(list: List<T>, upperBound: Int = -1): List<Pair<T, Int>> {
    val result = mutableListOf<Pair<T, Int>>()
    var current: T = list.firstOrNull() ?: return emptyList()
    var length = 0
    var bound = upperBound
    for (t in list) {
        if (t != current || bound == 0) {
            result.add(current to length)
            current = t
            length = 1
            bound = upperBound - 1
        } else {
            ++length
            --bound
        }
    }
    return result
}

class BwtRleHuffmanArchiver : Archiver {
    override fun archive(input: InputStream, output: BitOutputStream, report: CodingReport): Int {
        val message = input.readBytes().map { it.toInt() }

        report {
            !"Original message: "
            !message.joinToString("") { it.asSymbol() }
        }

        val cyclicShifts = message.indices.map { message.drop(it) + message.take(it) }
        report {
            !"\nCyclic shifts: "
            for ((i, c) in cyclicShifts.withIndex())
                !("$i - ${c.joinToString("") { it.asSymbol() }}")
        }

        val cyclicShiftsSorted = cyclicShifts.withIndex().sortedWith(compareBy(naturalOrder<Int>().lexicographically()) { it.value })
        report {
            !"\nCyclic shifts sorted: "
            for ((i, c) in cyclicShiftsSorted)
                !("$i - ${c.joinToString("") { it.asSymbol() }}")
        }

        val bwtResult = cyclicShiftsSorted.map { it.value.last() }
        report {
            !"\nBWT result: '${bwtResult.joinToString("") { it.asSymbol() }}'"
        }

        val messageIndexInBwtResult = cyclicShiftsSorted.indexOf(IndexedValue(0, message))
        report { !"\nOriginal message index in BWT result: $messageIndexInBwtResult" }

        val rle = runLengthEncode(bwtResult, 256)
        report { !"\nRun-length encoded (max length = 256): ${rle.map { (byte, l) -> byte.asSymbol() to l }}" }

        val symbols = rle.map { it.first }
        val lengths = rle.map { it.second }
        report {
            !"\nSeparating into sequences:"
            !"symbols = '${symbols.joinToString("") { it.asSymbol() }}'"
            !"lengths = ${lengths.joinToString(", ")}"
        }

        report { !"\nWriting: " }

        val rleLengthMonotonous = monotonous(rle.size)
        output.writeBits(rleLengthMonotonous)
        report { !"<- ${rleLengthMonotonous.joinToString("")} -- run-length-code message length ${rle.size} as monotonous code" }

        report { !"\nEncoding the lengths sequence with monotonous code:" }
        for (i in lengths) {
            val bits = monotonous(i)
            output.writeBits(bits)
            report { !"<- ${bits.joinToString("")} -- monotonous code for $i" }
        }
        val lengthBits = output.size
        report { !"Bits for the lengths: $lengthBits\n" }

        val messageIndexBits = messageIndexInBwtResult.toBits()
        output.writeBits(messageIndexBits)
        report { !"<- ${messageIndexBits.joinToString("")} -- original message index $messageIndexInBwtResult in BWT, uniformly" }

        val bitsBeforeSymbols = output.size
        report { !"\nCompressing the symbols sequence with LZSS:" }
        val symbolsBits = LzssArchiver().archive(symbols.map(Int::toByte).toByteArray().inputStream(), output, report) - bitsBeforeSymbols
        report { !"Bits for the symbols: $symbolsBits" }

        return output.size
    }
}