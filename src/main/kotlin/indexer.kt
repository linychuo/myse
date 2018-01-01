import com.ibm.icu.text.CharsetDetector
import org.apache.commons.io.IOUtils
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.poifs.filesystem.FileMagic
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.StringWriter
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import org.apache.poi.xwpf.extractor.XWPFWordExtractor


val fileExt = arrayOf("txt", "html", "htm", "doc", "docx")

fun main(args: Array<String>) {
    val rootDir = Paths.get("d:/workspace")
    val start = Date()
    val dir = FSDirectory.open(Paths.get("index"))
    val analyzer = SmartChineseAnalyzer()
    val iwc = IndexWriterConfig(analyzer)

    iwc.openMode = IndexWriterConfig.OpenMode.CREATE
    IndexWriter(dir, iwc).use { writer ->
        indexDocs(writer, rootDir)
    }

    val end = Date()
    println("${end.time - start.time} total milliseconds")
}


private fun indexDocs(writer: IndexWriter, path: Path) {
    if (Files.isDirectory(path)) {
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                indexDoc(writer, file, attrs.lastModifiedTime().toMillis())
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isHidden(dir) && Files.isReadable(dir)) return super.preVisitDirectory(dir, attrs)
                return FileVisitResult.SKIP_SUBTREE
            }
        })
    } else {
        indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis())
    }
}


private fun indexDoc(writer: IndexWriter, file: Path, lastModified: Long) {
    if (fileExt.indexOf(file.toFile().extension) != -1) {
        val doc = Document()
        doc.add(StringField("path", file.toString(), Field.Store.YES))
        doc.add(LongPoint("modified", lastModified))
        doc.add(TextField("contents", content(file), Field.Store.YES))
        println("adding $file")
        writer.addDocument(doc)
    }
}

private fun content(file: Path): String {
    FileMagic.prepareToCheckMagic(file.toFile().inputStream()).use {
        val magic = FileMagic.valueOf(it)
        when (magic) {
            FileMagic.OLE2 -> {
                val extractor = WordExtractor(it)
                return extractor.paragraphText.joinToString("")
            }
            FileMagic.OOXML -> {
                val word = XWPFDocument(it)
                val extractor = XWPFWordExtractor(word)
                return extractor.text
            }
            else -> {
                val detector = CharsetDetector()
                var encoding = "gbk"
                val match = detector.setText(it).detect()
                if (match.confidence > 50) {
                    encoding = match.name
                }
                val sw = StringWriter()
                IOUtils.copy(it, sw, encoding)
                return sw.toString()
            }
        }
    }

}