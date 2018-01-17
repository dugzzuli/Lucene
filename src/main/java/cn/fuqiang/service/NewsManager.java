package cn.fuqiang.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import cn.fuqiang.Entity.News;
import cn.fuqiang.dao.NewsDao;

@Service
public class NewsManager {
	@Autowired
	private NewsDao newsDao;
	public List<News> getNewsAll(){
		return (List<News>) newsDao.findAll();
	}
	public long getTotalRows(){
		return newsDao.count();
	}
	public long getTotalPage(int size){
		long totalRows = getTotalRows();
		long totalPage = totalRows%size == 0? totalRows/size :(totalRows/size)+1;	
		return totalPage;
	}
	public long getline(){
		return newsDao.count();
	}
	public List<News> getNewsByPageRequest(int curPage,int size){
		PageRequest pageRequest = new PageRequest(curPage, size);
		Page<News> page = newsDao.findAll(pageRequest);
		return page.getContent();
	}
	

}
