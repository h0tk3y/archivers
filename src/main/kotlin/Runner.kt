import java.nio.charset.Charset

fun main(args: Array<String>) {
    val s = "На дворе трава, на траве дрова. Не руби дрова на траве двора!".toUpperCase()
    println("Message to encode: $s")

    val charset = Charset.forName("KOI8-R") // using a 1-byte encoding
    val bytes = s.toByteArray(charset)
    val alphabet = " ,.!" + ('А'..'Я').joinToString("")

    val report = CharsetConsoleReport(charset)
    println("Archivers implemented: ")
    val archivers = listOf(
            "Huffman encoding" to HuffmanArchiver(),
            "LZSS" to LzssArchiver(),
            "LZW with escape codes" to LzwArchiver(alphabet = emptyList()),
            "LZW with predefined alphabet '$alphabet'" to
                    LzwArchiver(alphabet = alphabet
                            .toByteArray(charset)
                            .map { it.toInt() and 0xFF }),
            "BWT + RLE + Huffman" to BwtRleHuffmanArchiver()
    )
    archivers.forEach { (name, _) -> println("- $name") }
    println("\n---\n")

    archivers.forEach { (name, archiver) ->
        println("$name\n")
        val bits = archiver.archive(
                bytes.inputStream(),
                BitOutputStream(),
                report
        )
        println("\nTotal bits: $bits")
        println("\n---\n")
    }
}