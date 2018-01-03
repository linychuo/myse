import com.ibm.icu.text.CharsetDetector
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.poifs.filesystem.FileMagic
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.DosFileAttributes
import java.util.*

val plainText = arrayOf("txt", "TXT")
val html = arrayOf("html", "htm", "html", "htm")
val richContent = arrayOf("pdf", "doc", "docx")
val allExt = plainText + html + richContent
var cnt = 0

fun main(args: Array<String>) {
    val rootDir = Paths.get("d:/")
    val start = Date()
    val dir = FSDirectory.open(Paths.get("index"))
    val analyzer = SmartChineseAnalyzer()
    val iwc = IndexWriterConfig(analyzer)

    iwc.openMode = IndexWriterConfig.OpenMode.CREATE
    IndexWriter(dir, iwc).use { writer ->
        indexDocs1(writer, rootDir)
    }

    val end = Date()
    println("${end.time - start.time} total milliseconds, $cnt files")
}


private fun indexDocs(writer: IndexWriter, root: Path) {
    root.toFile().walkTopDown().onEnter {
        if (it.toPath() == root) {
            true
        } else {
            val dfa = Files.readAttributes(it.toPath(), DosFileAttributes::class.java)
            !dfa.isHidden && !dfa.isSystem
        }
    }.forEach {
        if (it.extension in allExt) {
            indexDoc(writer, it, it.lastModified())
        }
    }
}

private fun indexDocs1(writer: IndexWriter, root: Path) {
    try {
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                indexDoc(writer, file.toFile(), attrs.lastModifiedTime().toMillis())
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                return if (dir == root) {
                    FileVisitResult.CONTINUE
                } else {
                    val dfa = Files.readAttributes(dir, DosFileAttributes::class.java)
                    if (!dfa.isHidden && !dfa.isSystem) {
                        FileVisitResult.CONTINUE
                    } else {
                        FileVisitResult.SKIP_SUBTREE
                    }
                }
            }
        })
    } catch (e: AccessDeniedException) {
        //ignore
    }
}


private fun indexDoc(writer: IndexWriter, file: File, lastModified: Long) {
    val doc = Document()
    doc.add(StringField("path", file.toString(), Field.Store.YES))
    doc.add(LongPoint("modified", lastModified))
    doc.add(TextField("contents", content(file), Field.Store.YES))
    println("adding $file")
    writer.addDocument(doc)
}

private fun content(f: File): String {
    f.inputStream().use { fis ->
        return when (f.extension) {
            in plainText -> {
                val detector = CharsetDetector()
                var encoding = "gbk"
                val bytes = fis.readBytes()
                val match = detector.setText(bytes).detect()
                if (match.confidence > 50) {
                    encoding = match.name
                }
                java.lang.String(bytes, encoding) as String
            }
            in richContent -> {
                FileMagic.prepareToCheckMagic(fis).use {
                    when (FileMagic.valueOf(it)) {
                        FileMagic.OLE2 -> WordExtractor(it).paragraphText.joinToString("")
                        FileMagic.OOXML -> XWPFWordExtractor(XWPFDocument(it)).text
                        FileMagic.PDF -> PDFTextStripper().getText(PDDocument.load(it))
                        else -> it.reader().readText()
                    }
                }
            }
            else -> fis.reader().readText()
        }
    }
}