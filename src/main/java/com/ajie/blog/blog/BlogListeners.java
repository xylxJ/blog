package com.ajie.blog.blog;

import java.util.List;

/**
 * 博客监听器集
 * 
 * @author ajie
 *
 */
public interface BlogListeners {

	void register(BlogListener listener);

	boolean unRegister(BlogListener listener);

	List<BlogListener> getBlogListeners();
}
