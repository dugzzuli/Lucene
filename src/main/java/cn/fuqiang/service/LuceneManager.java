package cn.fuqiang.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wltea.analyzer.lucene.IKAnalyzer;

import cn.fuqiang.Entity.News;

@Component
public class LuceneManager {
	//存放索引和数据的目录
	String dir="E:/lucence";
	/**
	 * 创建分词器，存入和取出都会用到分词器，而且必须要定义成同一个分词器，所以可以创建一个公用的分词器
	 */
	private Analyzer analyzer = new IKAnalyzer();
	/**
	 * 指定分词器存和取的位置
	 * @return
	 * @throws IOException
	 */
	private Directory getDirectory() throws IOException{
		Directory directory = FSDirectory.open(new File(dir));
		return directory;
	}
	
	@Autowired
	private NewsManager newsManager;
	/**
	 * 将数据库中的内容读取到磁盘，并且创建索引
	 * 注意：在这里读取数据库中的数据写入磁盘时要注意，
	 * 应该去批量写入，否则当数据库中的数据过大时，一次性将过多的值读入到内存中，有可能会内存溢出
	 * @throws IOException
	 */
	public void writeFile() throws IOException{
		//获取存储器的数据存储位置目录
		Directory directory = getDirectory();
		//创建一个索引创建的类
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
		//创建写入数据的流
		IndexWriter iwriter = new IndexWriter(directory, config);
		//从第几页开始 默认从0开始
		int curPage=0;
		//每次写入两条
		int size=2;
		//获取总页数 总页数的算法是总条数除以每次读入的条数
		long totalPage = newsManager.getTotalPage(size);
		//设置每次读取一页，因为在使用了 PagingAndSortingRepository 接口映射，所以他的第一页是0 ,如果是1则我们就需要设置循环条件为 <=
		while(curPage<totalPage){
			//获取某一页的几条数据
			List<News> list = newsManager.getNewsByPageRequest(curPage, size);
			for (News news : list) {
				//将News对象转化为Document对象
				Document doc = getDocumentByNes(news);
				//写入磁盘
				iwriter.addDocument(doc);
			}
			curPage++;
		}
		
		//关闭写入流
		iwriter.close();
	}
	/**
	 *因为Lucene是根据Document存储的，所以要将news对象转换为Document元素，
	 * @param news
	 * @return
	 */
	public Document getDocumentByNes(News news){
		//创建一个存储数据的Document  (类似于java类，也可以理解为行)
		Document doc = new Document();
		//创建该类的属性，也可以理解为列
		Field title = new Field("title",news.getTitle(),TextField.TYPE_STORED);
		doc.add(title);
		//创建该类的属性，也可以理解为列  如果以太多的内容建议只针对前两百个字为创建索引的内容，节省带宽
		String text = news.getContent();
		if(text.length()>200){
			text = text.substring(0, 200);
		}
		Field content = new Field("content",text,TextField.TYPE_STORED);
		doc.add(content);
		return doc;
	}
	/**
	 * 根据传入的值，返回一个查询到的信息集合返回
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws InvalidTokenOffsetsException 
	 */
	public List<News> search(String title) throws IOException, ParseException, InvalidTokenOffsetsException{
		//创建一个目录位置，供读写流读取
		Directory directory = getDirectory();
		//指定读取的流
		DirectoryReader ireader = DirectoryReader.open(directory);
		//根据指定的流搜索的搜索工具
		IndexSearcher isearcher = new IndexSearcher(ireader);
		//构建查询解析器
		QueryParser parser = new QueryParser(Version.LUCENE_47,"title", analyzer);
		//通过解析器将输入的查询内容进行分词,并判断得分，并封装到一个Query对象中
		Query query = parser.parse(title);
		//使用搜索器，将符合query的doc返回到一个ScoreDoc数组中
		ScoreDoc[] hits = isearcher.search(query,1000).scoreDocs;
		//创建高亮代码的前缀和后缀默认是加粗<b><b/>
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("<font color='red'>","</font>");
		
		//高亮分析器,指定分词后的 Query对象
		Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));

		List<News> list = new ArrayList<News>();
		for(int i = 0; i < hits.length;i++){
			//获取第i个元素的id
			int id = hits[i].doc;
			//把第i个document取出来
			Document document = isearcher.doc(id);
			//获取title中的值并且获取的Title的长度用于
			String dTitle = document.get("title");
			//将搜寻的结果分词
			TokenStream titleTs = TokenSources.getAnyTokenStream(isearcher.getIndexReader(), id, "title", analyzer); 
			//通过高亮分析器将，把要高亮的部分进行加工
			TextFragment[] titleFrag = highlighter.getBestTextFragments(titleTs,dTitle,false,dTitle.length());
			for (int j = 0;j<titleFrag.length;j++) {
				if ((titleFrag[j] != null) && (titleFrag[j].getScore() > 0)) {
					//获取加入高亮后的值
					dTitle = titleFrag[j].toString();
			     }
			}
			
			//获取content中的值
			String dContent = document.get("content");
			//将搜寻的结果分词
			TokenStream contentTs = TokenSources.getAnyTokenStream(isearcher.getIndexReader(), id, "content", analyzer); 
			//通过高亮分析器将，把要高亮的部分进行加工
			TextFragment[] contentFrag = highlighter.getBestTextFragments(contentTs,dContent,false,dContent.length());
			for (int j = 0;j<contentFrag.length;j++) {
				if ((contentFrag[j] != null) && (contentFrag[j].getScore() > 0)) {
					dContent = contentFrag[j].toString();
			     }
			}
			//将要查询的结果封装成一个对象
			News news = new News(null,dTitle,dContent);
			//将对象添加到list集合中最后返回个页面
			list.add(news);
		}
		return list;
		
	}
	/**
	 * 针对分词器的分词效果进行测试
	 * @param title
	 * @throws IOException
	 */
	public void analyzerTest(String title) throws IOException{

		//使用分词器测试分词
		StringReader reader = new StringReader(title);
		TokenStream ts = analyzer.tokenStream("", reader);
		//必须使用reset()方法重置一下
		ts.reset();
		CharTermAttribute term = ts.getAttribute(CharTermAttribute.class);
		//输出分词器和处理结果
		System.out.println(analyzer.getClass());
		while(ts.incrementToken()){
			System.out.print(term.toString()+"|");
		}
		
	}

}
