package com.ajie.blog.blog;

import com.ajie.dao.pojo.TbBlog;

/**
 * 博客监听者
 * 
 * @author niezhenjie
 *
 */
public interface BlogListener {

	/** 消息通知业务类型 -- 创建博客 */
	public static final String BIZ_CREATE = "C";
	/** 消息通知业务类型 -- 删除博客 */
	public static final String BIZ_DELETE = "D";
	/** 消息通知业务类型 -- 更新博客 */
	public static final String BIZ_UPDATE = "U";
	/** activemq推送名称 */
	public static final String MESSAGE_PUSH_NAME = "blog-crud";

	/**
	 * 创建博客
	 * 
	 * @param blog
	 */
	void onCreate(TbBlog blog);

	/**
	 * 删除博客
	 * 
	 * @param blog
	 */
	void onDelete(TbBlog blog);

	/**
	 * 更新博客
	 * 
	 * @param blog
	 */
	void onUpdate(TbBlog blog);
}
