import java.nio.charset.Charset

/**
 * Created by igushs on 11/22/16.
 */

abstract class CodingReport {
    abstract fun Int.asSymbol(): String
    fun Byte.asSymbol() = toInt().asSymbol()

    abstract fun write(message: String)
    operator fun String.not() = write(this)

    open operator fun invoke(reportBlock: CodingReport.() -> Unit) = reportBlock(this)
}

class CharsetConsoleReport(val charset: Charset) : CodingReport() {
    override fun Int.asSymbol(): String = String(byteArrayOf(toByte()), charset)
    override fun write(message: String) {
        println(message)
    }
}

val emptyReporter = object : CodingReport() {
    override fun Int.asSymbol(): String = error("Should not be called.")
    override fun write(message: String) = error("Should not be called.")

    override operator fun invoke(reportBlock: CodingReport.() -> Unit) = Unit
}