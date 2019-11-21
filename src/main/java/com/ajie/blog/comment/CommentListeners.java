package com.ajie.blog.comment;

/**
 * 评论监听集
 * 
 * @author niezhenjie
 */
public interface CommentListeners {

	/**
	 * 注册一个监听器
	 * 
	 * @param listener
	 * @return
	 */
	void register(CommentListener listener);

	/**
	 * 移处一个监听器
	 * 
	 * @param listener
	 * @return
	 */
	boolean unregister(CommentListener listener);
}
