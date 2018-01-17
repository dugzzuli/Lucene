package cn.fuqiang.dao;

import org.springframework.data.repository.PagingAndSortingRepository;

import cn.fuqiang.Entity.News;

public interface NewsDao extends PagingAndSortingRepository<News, Integer>{

}
