import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths

fun main(args: Array<String>) {
    DirectoryReader.open(FSDirectory.open(Paths.get("index"))).use { reader ->
        val searcher = IndexSearcher(reader)
        val analyzer = SmartChineseAnalyzer()
        val queryParser = QueryParser("contents", analyzer)
        val query = queryParser.parse("测试")
        println("Searching for: ${query.toString("contents")}")

        val results = searcher.search(query, 50)
        val numTotalHits = Math.toIntExact(results.totalHits);
        println("$numTotalHits total matching documents")

        results.scoreDocs.forEach {
            val doc = searcher.doc(it.doc)
            val path = doc.get("path")
            println(path)
        }
    }
}