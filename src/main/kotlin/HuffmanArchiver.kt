
import java.io.InputStream
import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy

private var globalId = 0

class HuffmanArchiver : Archiver {

    private data class HuffmanNode(val symbol: Int = -1,
                                   val children: List<HuffmanNode> = listOf(),
                                   val frequency: Int,
                                   val id: Int = globalId++)

    private fun buildHuffmanTree(nodes: List<HuffmanNode>): HuffmanNode {
        val sortedNodes = TreeSet(compareBy<HuffmanNode> { it.frequency }.thenBy { it.id })
        sortedNodes.addAll(nodes)

        fun merge(n1: HuffmanNode, n2: HuffmanNode) =
                HuffmanNode(children = listOf(n1, n2),
                        frequency = n1.frequency + n2.frequency)

        while (sortedNodes.size > 1) {
            val n1 = sortedNodes.first()
            sortedNodes.remove(n1)
            val n2 = sortedNodes.first()
            sortedNodes.remove(n2)
            val n = merge(n1, n2)
            sortedNodes.add(n)
        }

        return sortedNodes.single()
    }

    private fun alignTree(tree: HuffmanNode): HuffmanNode {
        val nodesWithDepth = mutableListOf<Pair<HuffmanNode, Int>>()

        fun collectByDepth(tree: HuffmanNode, depth: Int) {
            if (tree.symbol != -1)
                nodesWithDepth.add(tree to depth)
            else
                tree.children.forEach { collectByDepth(it, depth + 1) }
        }
        collectByDepth(tree, 0)

        val nodesByDepth = nodesWithDepth
                .groupBy({ (_, depth) -> depth })
                .mapValues { it.value.mapTo(mutableListOf()) { (node, _) -> node } }

        val maxDepth = nodesByDepth.keys.max()!!

        fun reconstruct(depth: Int): HuffmanNode {
            val nodesAtThisDepth = nodesByDepth[depth]
            if (nodesAtThisDepth != null && nodesAtThisDepth.isNotEmpty()) {
                val n = nodesAtThisDepth.removeAt(nodesAtThisDepth.lastIndex)
                return HuffmanNode(symbol = n.symbol,
                        frequency = n.frequency)
            } else if (depth < maxDepth) {
                val c1 = reconstruct(depth + 1)
                val c2 = reconstruct(depth + 1)
                val children = listOf(c1, c2)
                return HuffmanNode(children = children,
                        frequency = children.sumBy { it.frequency })
            } else {
                error("Not enough nodes.")
            }
        }

        return reconstruct(0)
    }

    private fun getCodeMapping(tree: HuffmanNode): HashMap<Int, List<Int>> {
        val result = LinkedHashMap<Int, List<Int>>()

        fun recurse(n: HuffmanNode, depth: Int, vector: List<Int>) {
            if (n.symbol != -1)
                result[n.symbol] = vector
            else
                n.children.forEachIndexed { i, child ->
                    val newVector = vector.toList()
                    recurse(child, depth + 1, newVector + i)
                }
        }

        recurse(tree, 0, if (tree.symbol == -1) listOf() else listOf(0))

        return result
    }

    private fun rangesForLevels(levels: Map<Int, Int>): List<Int> {
        val result = mutableListOf<Int>()
        var maxLeafs = 1
        for (i in 0..levels.keys.max()!!) {
            result.add(maxLeafs)
            val leafsOnLevel = levels.getOrDefault(i, 0)
            val nonLeafs = maxLeafs - leafsOnLevel
            maxLeafs = nonLeafs * 2
        }
        return result
    }

    override fun archive(input: InputStream, output: BitOutputStream, report: CodingReport): Int {
        val bytes = input.readBytes()
        report { !"Input: ${bytes.map { it.asSymbol() }.joinToString("")} --- ${bytes.size} bytes total" }

        val histogram = bytes.groupBy { it }.mapValues { it.value.size }

        report {
            !"Frequencies histogram:"
            !histogram.entries
                    .sortedByDescending { it.value }
                    .joinToString("\n") { "${it.key.asSymbol()} -> ${it.value}" }
        }

        val tree = alignTree(buildHuffmanTree(histogram.map { HuffmanNode(symbol = it.key.toInt(), frequency = it.value) }))
        val code: Map<Int, List<Int>> = getCodeMapping(tree)

        report {
            !"Code:"
            !code.entries
                    .sortedBy { it.value.size }
                    .joinToString("\n") { "${it.key.asSymbol()} -> ${it.value.joinToString("")}" }
        }

        val numberOfNodesByLevels = code.values.groupBy { it.size }.mapValues { it.value.size }

        report {
            !"Code words by length:"
            !numberOfNodesByLevels.entries
                    .sortedBy { it.key }
                    .joinToString("\n") { "${it.key} -> ${it.value}" }
        }

        val rangesForLevels = rangesForLevels(numberOfNodesByLevels)

        report { !"Writing:" }

        for ((i, maxLeafs) in rangesForLevels.withIndex()) {
            val leafsOnLevel = numberOfNodesByLevels.getOrDefault(i, 0)
            val nBits = bitsForNValues(maxLeafs + 1)
            report { !"<- ${leafsOnLevel} as ${nBits} bits -- $leafsOnLevel out of $maxLeafs leaves on level $i" }
            output.writeInt(leafsOnLevel, nBits)
        }

        for (k in code.keys) {
            report { !"<- ${k and 0xff} as 8 bits -- character '${k.asSymbol()}' in left-to-right tree traverse" }
            output.writeInt(k, 8)
        }

        for (b in bytes) {
            val i = b.toInt()
            val codeWordBits = code[i]!!
            report { !"<- ${codeWordBits.joinToString("")} -- code word for '${i.asSymbol()}'" }
            output.writeBits(codeWordBits)
        }

        return output.size
    }
}