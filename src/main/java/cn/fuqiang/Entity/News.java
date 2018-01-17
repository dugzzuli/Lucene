package cn.fuqiang.Entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class News {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)//主键自动装配
	private Integer nid;
	@Column
	private String title;
	@Column
	private String content;
	public News() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Integer getNid() {
		return nid;
	}
	public void setNid(Integer nid) {
		this.nid = nid;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public News(Integer nid, String title, String content) {
		super();
		this.nid = nid;
		this.title = title;
		this.content = content;
	}

	
}
