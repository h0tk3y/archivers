import java.io.InputStream
import java.io.OutputStream

interface Archiver {
    fun archive(input: InputStream, output: BitOutputStream, report: CodingReport = emptyReporter): Int
}

interface Unarchiver {
    fun unarchive(input: InputStream, output: BitOutputStream, report: CodingReport = emptyReporter)
}