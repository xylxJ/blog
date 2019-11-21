package com.ajie.blog.comment;

import com.ajie.dao.pojo.TbComment;

/**
 * 评论监听器
 * 
 * @author niezhenjie
 *
 */
public interface CommentListener {

	void onCreate(TbComment common);

	void onDelete(TbComment common);

	void onUpdate(TbComment common);

}
