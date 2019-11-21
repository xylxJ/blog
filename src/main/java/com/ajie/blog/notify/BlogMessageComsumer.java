package com.ajie.blog.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajie.blog.blog.BlogListener;
import com.ajie.blog.comment.CommentException;
import com.ajie.blog.comment.CommentService;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbUser;
import com.ajie.push.Message;
import com.ajie.push.ext.AbstractPushListener;
import com.ajie.sso.user.UserService;

/**
 * 博客消息通知消费者
 * 
 * @author niezhenjie
 *
 */
public class BlogMessageComsumer extends AbstractPushListener {
	private static final Logger logger = LoggerFactory
			.getLogger(BlogMessageComsumer.class);

	private CommentService commentService;

	private UserService userService;

	@Override
	public void comsume(Message message) {
		String biz = message.getBiz();
		if (BlogListener.BIZ_CREATE.equals(biz)) {
			// 新增博客 XXX
		} else if (BlogListener.BIZ_DELETE.equals(biz)) {
			// 删除博客
			String ref = message.getReference();// 操作者用户id
			int userId = Integer.valueOf(ref);
			TbUser user = userService.getUserById(userId);
			TbBlog blog = message.getContent(TbBlog.class);
			try {
				commentService.deleteAllComment(blog, user);
			} catch (CommentException e) {
				logger.error("删除所有博客消息失败", e);
			}
		} else if (BlogListener.BIZ_UPDATE.equals(biz)) {
			// 更新博客 XXX
		} else {
			logger.error("位置操作类型：" + biz);
		}
	}

	public void setCommentService(CommentService service) {
		commentService = service;
	}

	public void setUserService(UserService service) {
		userService = service;
	}

}
