package cn.fuqiang.controller;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.fuqiang.Entity.News;
import cn.fuqiang.service.LuceneManager;

@RestController
public class LuceneController {
	@Autowired
	private LuceneManager lucene;
	@GetMapping("/write")
	public String write(){
		try {
			lucene.writeFile();
			return "写入成功！";
		} catch (IOException e) {
			return "写入失败"+e.getMessage();
		}
	}
	@GetMapping("/search")
	public List<News> getNewsList(String search) throws InvalidTokenOffsetsException{
		
		List<News> list = null;
		try {
			list = lucene.search(search);
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
}
