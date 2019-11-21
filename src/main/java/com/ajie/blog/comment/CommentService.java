package com.ajie.blog.comment;

import java.util.Comparator;
import java.util.List;

import com.ajie.chilli.common.KVpair;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbComment;
import com.ajie.dao.pojo.TbUser;

/**
 * 评论服务接口
 *
 * @author niezhenjie
 *
 */
public interface CommentService {
	/** 正常 */
	static public final int MARK_STATE_NORMAL = 1 << 0;
	/** 删除 */
	static public final int MARK_STATE_DELETE = 1 << 1;

	/** 评论状态 -- 正常 */
	static public final KVpair STATE_NOTMAL = KVpair.valueOf("正常", MARK_STATE_NORMAL);
	/** 评论状态 -- 已删除 */
	static public final KVpair STATE_DELETE = KVpair.valueOf("已删除", MARK_STATE_DELETE);

	/** 按照创建时间排序 */
	public static final Comparator<TbComment> CREATE_DATE = new Comparator<TbComment>() {
		@Override
		public int compare(TbComment o1, TbComment o2) {
			return o2.getCreatetime().compareTo(o1.getCreatetime());
		}
	};
	
	/**
	 * 添加一条评论
	 * 
	 * @param content
	 * @param blogId
	 * @param userId
	 * @return
	 * @throws CommentException
	 */
	TbComment createComment(String content, int blogId, TbUser user) throws CommentException;

	/**
	 * 删除一条评论
	 * 
	 * @param comment
	 * @param operator
	 *            操作者，用于校验是否是自己的评论或是管理员
	 * @throws CommentException
	 */
	void deleteComment(TbComment comment, TbUser operator) throws CommentException;

	/**
	 * 删除博客所有的评论
	 * 
	 * @param blog
	 * @param operator
	 *            操作者，用于校验是否是自己的评论或是管理员
	 * @throws CommentException
	 */
	void deleteAllComment(TbBlog blog, TbUser operator) throws CommentException;

	/**
	 * 获取博客所有的评论
	 * 
	 * @param blog
	 * @return
	 */
	List<TbComment> getComments(TbBlog blog);

	/**
	 * 获取博客所有的评论
	 * 
	 * @param blog
	 * @return
	 */
	List<TbComment> getComments(int blogId);

	/**
	 * 获取指定博客的评论条数
	 * 
	 * @return
	 */
	int getBlogCommentCount(int blogId);

}
