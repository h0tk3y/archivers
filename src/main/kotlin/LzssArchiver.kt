
import java.io.InputStream
import java.util.*

/**
 * Created by igushs on 11/22/16.
 */

private val defaultWindowSize = 1024

class LzssArchiver(val windowSize: Int = defaultWindowSize) : Archiver {
    override fun archive(input: InputStream, output: BitOutputStream, report: CodingReport): Int {
        var window = mutableListOf<Int>()

        var matchLength = 0
        var matchSet = HashSet<Int>()

        var byte: Int = -1
        while (true) {
            // if it's not -1, then it's a byte read on previous iteration but not written in a match
            if (byte == -1)
                byte = input.read()

            // if it's still -1, this is end of stream
            if (byte == -1 && matchLength == 0)
                break

            if (matchLength == 0) {
                matchSet = window.indices.filterTo(HashSet()) { window[it] == byte }

                if (matchSet.isEmpty()) {
                    output.writeBit(1)
                    val bits = (byte and 0xFF).toBits(8)
                    output.writeBits(bits)
                    report { !"<- '${byte.asSymbol()}': 0${bits.joinToString("")} = (flag = 0, c = $byte), 9 bits" }
                    window = (window + byte).takeLast(windowSize).toMutableList()
                    matchLength = 0
                } else {
                    matchLength = 1
                    window.add(byte)
                }
                byte = -1
            } else {
                val nextMatchSet = matchSet.filterTo(HashSet()) { window[it + matchLength] == byte }
                if (nextMatchSet.isEmpty() || matchLength == windowSize) {
                    val m = matchSet.first()
                    val d = window.size - matchLength - m - 1
                    val dBits = d.toBits(nBits = bitsForNValues(window.size - matchLength))
                    val lMonotonous = monotonous(matchLength)
                    output.writeBit(1)
                    output.writeBits(dBits)
                    output.writeBits(lMonotonous)
                    report {
                        val matchString = window.subList(m, m + matchLength).joinToString("") { it.asSymbol() }
                        val nBits = 1 + dBits.size + lMonotonous.size
                        !"<- '$matchString': 1${(dBits + lMonotonous).joinToString("")} = (flag = 1, d = $d as ${dBits.joinToString("")}, l = $matchLength as monotonous ${lMonotonous.joinToString("")}), $nBits bits"
                    }
                    window = window.takeLast(windowSize).toMutableList()
                    matchLength = 0
                } else {
                    ++matchLength
                    window.add(byte)
                    byte = -1
                }
                matchSet = nextMatchSet
            }
        }

        return output.size
    }
}