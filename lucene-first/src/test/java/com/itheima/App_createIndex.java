package com.itheima;

import com.itheima.dao.impl.BookDaoImpl;
import com.itheima.pojo.Book;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App_createIndex {

    public static final String PATH = "D:\\Workspace\\index";

    @Test
    public void createIndexTest() throws IOException {
        //1.采集数据
        List<Book> bookList = new BookDaoImpl().findAllBooks();
        //2.建立文档对象（Document）
        List<Document> documentList = new ArrayList<Document>();
        //2.1封装文档对象
        for (Book book : bookList) {
            Document document = new Document();
            document.add(new StringField("id", book.getId() + "", Field.Store.YES));
            document.add(new TextField("bookName", book.getBookname(), Field.Store.YES));
            document.add(new DoubleField("price", book.getPrice(), Field.Store.YES));
            document.add(new StoredField("pic", book.getPic()));
            document.add(new TextField("bookDesc", book.getBookdesc(), Field.Store.NO));
            //建封装号的数据封装到集合中
            documentList.add(document);
        }
        //3.建立分析器（分词器）对象(Analyzer)，用于分词
        //Analyzer analyzer = new StandardAnalyzer();
        Analyzer analyzer = new IKAnalyzer();
        //4.建立索引库配置对象（IndexWriterConfig），配置索引库
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
        //4.1设置索引库打开模式(每次都重新创建)
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        //5.建立索引库目录对象（Directory），指定索引库的位置
        Directory directory = FSDirectory.open(new File(PATH));
        //6.建立索引库操作对象（IndexWriter），操作索引库
        IndexWriter indexWriter = new IndexWriter(directory, config);
        //7.使用IndexWriter，把文档对象写入索引库
        for (Document doc : documentList) {
            //7.1把文档对象写入索引库
            indexWriter.addDocument(doc);
        }
        //8.释放资源
        indexWriter.commit();
        indexWriter.close();
    }

    @Test
    public void searchIndex() throws Exception {
        //创建分析器对象（Analyzer）
        //Analyzer analyzer = new StandardAnalyzer();
        Analyzer analyzer = new IKAnalyzer();
        //创建分词解析器,自定查询字段和分词器
        QueryParser queryParser = new QueryParser("bookName", analyzer);
        //创建查询对象（Query）,指定查询的字段
        Query query = queryParser.parse("bookName:java");
        //创建索引库的目录（Directory），指定索引库的位置
        Directory directory = FSDirectory.open(new File(PATH));
        //创建索引读取对象（IndexReader），把索引数据读取到内存中
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建索引搜索对象（IndexSearcher），执行搜索
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //使用IndexSearcher执行搜索，返回搜索结果集（TopDocs）,传入查询的query,查询返回钱n条数据
        TopDocs topDocs = indexSearcher.search(query, 10);
        //处理TopDocs结果集
        System.out.println("总命中的条数:" + topDocs.totalHits);
        //获取所有搜索到的文档数据
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("------------------------");
            System.out.println("文档的分数:" + scoreDoc.score);
            System.out.println("文档的id:" + scoreDoc.doc);
            //通过文档的id获取域的值
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println("id:" + doc.get("id"));
            System.out.println("bookName:" + doc.get("bookName"));
            System.out.println("price:" + doc.get("price"));
            System.out.println("pic:" + doc.get("pic"));
            System.out.println("bookDesc:" + doc.get("bookDesc"));
        }
        //释放资源
        indexReader.close();
    }

    /**
     * 检索流程实现（分页搜索）
     *
     * @throws Exception
     */
    @Test
    public void readIndexPage() throws Exception {
        // 1.建立分析器对象（Analyzer），用于分词
        //Analyzer analyzer = new StandardAnalyzer();
        // 使用ik分词器
        Analyzer analyzer = new IKAnalyzer();
        // 2.建立查询对象（Query）
        // 2.1.建立查询解析器对象
        // 参数一：指定默认搜索域(bookName:java)
        // 参数二：使用的分析器对象
        QueryParser qp = new QueryParser("bookName", analyzer);
        // 2.2.使用查询解析器对象，解析表达式，实例化query对象
        Query query = qp.parse("bookName:java");
        // 3.建立索引库目录对象（Directory），指定索引库的位置
        Directory directory = FSDirectory.open(new File(PATH));
        // 4.建立索引读取对象（IndexReader），把索引数据从索引库中读取到内存
        IndexReader reader = DirectoryReader.open(directory);
        // 5.建立索引搜索对象（IndexSearcher），执行搜索
        IndexSearcher searcher = new IndexSearcher(reader);
        // 6.使用IndexSearcher对象执行搜索，返回查询结果集TopDocs
        // 参数一：查询对象
        // 参数二：指定搜索结果排序后的前n个
        TopDocs topDoc = searcher.search(query, 10);
        // 7.处理结果集
        // 7.1.打印搜搜到的实际结果数量
        System.out.println("实际搜索到的结果数量：" + topDoc.totalHits);
            // 7.2.获取搜索的文档id结果集合
            // ScoreDoc对象，包含两个信息：一个是当前的文档id，另外一个是当前文档的分值
        ScoreDoc[] scores = topDoc.scoreDocs;
        // 增加分页逻辑====================start
        // 1.当前页
        //int curPage = 1;
        int curPage = 1;
        // 2.每一页显示大小
        int pageSize = 2;
        // 3.当前页的开始记录索引
        int start = (curPage - 1) * pageSize;
        // 4.当前页的结束记录索引
        // 通常情况下是开始索引+页大小
        // 最后一页的情况：取开始索引+页大小，结果集的索引的最小值
        int end = Math.min(start + pageSize, scores.length);
        // 增加分页逻辑====================end
        //for(ScoreDoc sd:scores){
        for (int i = start; i < end; i++) {
            System.out.println("--------------------------------");
         // 获取文档id和分值
            int docId = scores[i].doc;
            float score = scores[i].score;
            // 打印文档的id和分值
            System.out.println("当前文档id：" + docId + ",当前文档的分值：" + score);
            // 根据文档id获取文档内容
            Document doc = searcher.doc(docId);
            System.out.println("id:" + doc.get("id"));
            System.out.println("bookName:" + doc.get("bookName"));
            System.out.println("price:" + doc.get("price"));
            System.out.println("pic:" + doc.get("pic"));
            System.out.println("bookDesc:" + doc.get("bookDesc"));
        }
        // 8.释放资源
        reader.close();
    }


    //删除索引
    @Test
    public void deleteIndex() throws Exception{
        //创建分词器
        Analyzer analyzer = new IKAnalyzer();
        //创建索引对象配置
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_3,analyzer);
        //设置配置
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        //创建Directory的对象
        Directory directory = FSDirectory.open(new File(PATH));
        //创建索引对象
        IndexWriter indexWriter = new IndexWriter(directory,iwc);
        //创建
        Term term = new Term("bookName","java");
        //删除索引
        indexWriter.deleteDocuments(term);
        //提交
        indexWriter.commit();
        //关闭
        indexWriter.close();
    }

    //删除全部索引
    @Test
    public void deleteAll() throws Exception{
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_3,analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        Directory directory = FSDirectory.open(new File(PATH));
        IndexWriter indexWriter = new IndexWriter(directory,iwc);
        indexWriter.deleteAll();
        indexWriter.close();
    }

    //修改索引
    @Test
    public void update() throws Exception{
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3,analyzer);
        Directory directory = FSDirectory.open(new File(PATH));
        IndexWriter indexWriter = new IndexWriter(directory,config);
        Term term = new Term("id","999");
        Document document = new Document();
        document.add(new StringField("id","999", Field.Store.YES));
        document.add(new TextField("name","中明", Field.Store.YES));
        indexWriter.updateDocument(term,document);
        indexWriter.commit();
        indexWriter.close();
    }

    //查询
    @Test
    public void termQueryTest() throws Exception{
       searcher(new TermQuery(new Term("bookName","java")));
    }

    /**
     * NumericRangeQuery数值范围查询
     * 需求：查询图书价格在80到100之间的图书
     */
    @Test
    public void NumericRangeQueryTest() throws Exception{
        Query query = NumericRangeQuery.newDoubleRange("price",80d,100d,false,false);
        searcher(query);
    }

    /**
     * BooleanQuery布尔查询
     * 需求：查询图书名称域中包含有java的图书，并且价格在80到100之间（包含边界值）。
     */
    @Test
    public void booleanQueryTest() throws Exception{
        //条件一
        Query query1= new TermQuery(new Term("bookName","java"));
        //条件二
        Query query2 = NumericRangeQuery.newDoubleRange("price",80d,100d,true,true);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(query1, BooleanClause.Occur.MUST);
        booleanQuery.add(query2, BooleanClause.Occur.MUST);
        searcher(booleanQuery);
    }

    /**
     * 使用QueryParser
     * 需求：查询图书名称域中包含有java，并且图书名称域中包含有lucene的图书
     */
    @Test
    public void testQueryParser() throws Exception{
        Analyzer analyzer = new IKAnalyzer();
        QueryParser queryParser = new QueryParser("bookName",analyzer);
        Query query = queryParser.parse("bookName:java NOT bookName:lucene");
        searcher(query);
    }


    public void searcher(Query query) throws Exception{
        System.out.println("查询语法:"+query);
        Directory directory = FSDirectory.open(new File(PATH));
        //创建indexreader创建索引库对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建IndexSearcher对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //执行查询索引
        TopDocs topDocs = indexSearcher.search(query, 10);
        //命中条数
        System.out.println("命中条数为:"+topDocs.totalHits);
        //获取搜索到的所有文档数组
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        //遍历
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档id:"+scoreDoc.doc);
            //通过文档id获取文档
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println("id:" + doc.get("id"));
            System.out.println("bookName:" + doc.get("bookName"));
            System.out.println("price:" + doc.get("price"));
            System.out.println("pic:" + doc.get("pic"));
            System.out.println("bookDesc:" + doc.get("bookDesc"));
        }
        //释放资源
        indexReader.close();
    }

}
