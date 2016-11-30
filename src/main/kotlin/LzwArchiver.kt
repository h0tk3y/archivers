
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Created by igushs on 11/27/16.
 */

class LzwArchiver(val alphabet: List<Int>) : Archiver {
    private class Trie {
        var nodesCount = 0
            private set

        inner class Node {
            val children = HashMap<Int, Node>()
            val id = nodesCount++
        }

        private val root = Node()

        var currentNode: Node = root
            private set

        fun beginSearch() {
            currentNode = root
        }

        fun advanceSearch(symbol: Int): Int {
            val node = currentNode.children[symbol]
            if (node != null) {
                currentNode = node;
                return currentNode.id
            } else {
                currentNode.children[symbol] = Node()
                return -1
            }
        }
    }

    override fun archive(input: InputStream, output: BitOutputStream, report: CodingReport): Int {
        val bitOutput = BitOutputStream()

        val dictionary = Trie()
        for (i in alphabet) {
            dictionary.beginSearch()
            dictionary.advanceSearch(i)
            report { !"Added '${i.asSymbol()}' as id = ${dictionary.nodesCount - 1}" }
        }

        var byte = input.read()
        var ignoreLastMatch = true
        while (byte != -1) {
            var matchId = -1
            dictionary.beginSearch()
            val matchSymbols = mutableListOf<Int>()
            do {
                val nextMatchId = dictionary.advanceSearch(byte)
                if (nextMatchId == -1 || byte == -1 || (ignoreLastMatch && nextMatchId == dictionary.nodesCount - 1)) {
                    val bits = bitsForNValues(dictionary.nodesCount)
                    if (matchSymbols.isNotEmpty()) {
                        val matchIdBits = matchId.toBits(bits)
                        bitOutput.writeBits(matchIdBits)
                        report {
                            val match = matchSymbols.map { it.asSymbol() }.joinToString("")
                            !("<- '$match': ${matchIdBits.joinToString("")} = " +
                                    "(matchId = $matchId as $bits bits)" +
                                    if (byte != -1) ", added '$match${byte.asSymbol()}' as id = ${dictionary.nodesCount - 1}" else "")
                        }
                        ignoreLastMatch = true
                    } else if (alphabet.isEmpty()) {
                        val escapeCode = 0.toBits(bits)
                        val sBits = (byte and 0xFF).toBits(8)
                        bitOutput.writeBits(escapeCode)
                        bitOutput.writeBits(sBits)
                        report {
                            val symbol = byte.asSymbol()
                            !("<- '$symbol': ${escapeCode.joinToString("")}${sBits.joinToString("")} = " +
                                    "(0 as ${bits} bits, '$symbol' as 8 bits), " +
                                    "added '$symbol' as id = ${dictionary.nodesCount - 1}")
                        }
                        if (byte != -1)
                            byte = input.read()
                        ignoreLastMatch = false
                    } else {
                        throw IOException("Unknown symbol $byte found in predefined alphabet mode.")
                    }
                } else {
                    matchSymbols.add(byte)
                    byte = input.read()
                }
                matchId = nextMatchId
            } while (matchId != -1)
        }

        return bitOutput.size
    }
}