package com.ajie.blog.comment.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ajie.blog.blog.BlogService;
import com.ajie.blog.blog.RedisBlog;
import com.ajie.blog.blog.RedisBlog.RedisBlogVo;
import com.ajie.blog.blog.impl.BlogServiceImpl;
import com.ajie.blog.comment.CommentException;
import com.ajie.blog.comment.CommentListener;
import com.ajie.blog.comment.CommentListeners;
import com.ajie.blog.comment.CommentService;
import com.ajie.chilli.cache.redis.RedisClient;
import com.ajie.chilli.thread.ThreadPool;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.dao.mapper.TbCommentMapper;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbComment;
import com.ajie.dao.pojo.TbCommentExample;
import com.ajie.dao.pojo.TbCommentExample.Criteria;
import com.ajie.dao.pojo.TbUser;
import com.ajie.sso.role.Role;

/**
 * 评论服务接口实现
 *
 * @author niezhenjie
 *
 */
@Service("commentService")
public class CommentServiceImpl implements CommentService, CommentListeners {
	private static final Logger logger = LoggerFactory
			.getLogger(CommentServiceImpl.class);
	@Resource
	private TbCommentMapper mapper;
	@Resource
	private BlogService blogService;
	@Resource
	private RedisClient redisClient;

	/** 线程池 */
	@Resource
	private ThreadPool threadPool;

	private List<CommentListener> listeners;

	@Override
	public TbComment createComment(String content, int blogId, TbUser user)
			throws CommentException {
		if (null == content || blogId == 0)
			throw new CommentException("评论失败，参数异常");
		if (null == user || user.getId() == 0)
			throw new CommentException("会话过期，请重新登录");
		TbComment comment = new TbComment(content, blogId, user);
		int ret = mapper.insert(comment);
		if (ret != 1)
			throw new CommentException("评论失败");
		// 更新评论数
		RedisBlog redisBlog = getRedisBlog();
		if (null == redisBlog)
			return comment;
		RedisBlogVo vo = redisBlog.getRedisBlog(blogId);
		vo.updateCommentNum();
		return comment;
	}

	public RedisBlog getRedisBlog() {
		BlogServiceImpl blogServiceImp = null;
		if (blogService instanceof BlogServiceImpl) {
			blogServiceImp = (BlogServiceImpl) this.blogService;
			return blogServiceImp.getRedisBlog();
		}
		logger.warn("打开blog缓存失败，无法更新评论信息到缓存");
		return null;
	}

	@Override
	public void deleteComment(TbComment comment, TbUser operator)
			throws CommentException {
		if (null == comment || comment.getId() == 0)
			throw new CommentException("删除失败，评论不存在");
		if (null == operator || operator.getId() == 0)
			throw new CommentException("删除失败，不能删除非自己的评论");
		// 防止评论id被篡改但用户id没有改，而出现删除别的评论的情况
		// 接口修改解释：不应该由数据库接口控制业务，业务在这里自己做控制，在数据库接口做控制的话会将整个接口都限制的死死的，一点都不灵活，比如管理员要删除，就不行了，以为判断不是评论所有者
		// mapper.updateCommentMark(comment.getId(), operator.getId(),
		// MARK_STATE_DELETE);
		TbComment m = mapper.selectByPrimaryKey(comment.getId());
		if (null == m) {
			throw new CommentException("删除失败，评论不存在");
		}
		int userId = m.getUserid();
		if (userId != operator.getId() && !isAdmin(operator)) {
			throw new CommentException("删除失败，不能删除非自己的评论");
		}
		mapper.updateCommentMark(comment.getId(), MARK_STATE_DELETE);
	}

	@Override
	public void deleteAllComment(TbBlog blog, TbUser operator)
			throws CommentException {
		if (blog.getId() == 0)
			throw new CommentException("删除失败，博客不存在");
		if (null == operator || operator.getId() == 0)
			throw new CommentException("删除失败，不能删除非自己的评论");
		int userId = blog.getUserid();
		if (0 == userId) {
			throw new CommentException("删除失败，缺少博客所有者");
		}
		if (userId != operator.getId() && !isAdmin(operator)) {
			throw new CommentException("删除失败，不能删除非自己的评论");
		}
		mapper.updateBlogCommentsMark(blog.getId(), MARK_STATE_DELETE);
	}

	@Override
	public List<TbComment> getComments(TbBlog blog) {
		if (null == blog || blog.getId() == 0)
			return Collections.emptyList();
		TbCommentExample ex = new TbCommentExample();
		Criteria criteria = ex.createCriteria();
		criteria.andBlogidEqualTo(blog.getId());
		List<TbComment> comments = mapper.selectByExample(ex);
		if (null == comments)
			return Collections.emptyList();
		Collections.sort(comments, CREATE_DATE);
		return comments;
	}

	@Override
	public List<TbComment> getComments(int blogId) {
		TbCommentExample ex = new TbCommentExample();
		Criteria criteria = ex.createCriteria();
		criteria.andBlogidEqualTo(blogId);
		List<TbComment> comments = mapper.selectByExample(ex);
		if (null == comments)
			return Collections.emptyList();
		Collections.sort(comments, CREATE_DATE);
		return comments;
	}

	@Override
	public int getBlogCommentCount(int blogId) {
		int commentCount = mapper.getBlogCommentCount(blogId);
		return commentCount;
	}

	/**
	 * 检查用户是否为管理员用户
	 * 
	 * @param user
	 * @return
	 */
	private boolean isAdmin(TbUser user) {
		if (null == user)
			return false;
		return checkRole(user.getRoleids(), "管理员")
				|| checkRole(user.getRoleids(), "超级用户");
	}

	/**
	 * 检查权限id集里是否有roleName名字的权限
	 * 
	 * @param roleids
	 * @param roleName
	 * @return
	 */
	private boolean checkRole(String roleids, String roleName) {
		List<Role> roles = JsonUtils.toList(roleids, Role.class);
		if (null == roles)
			return false;
		for (Role role : roles) {
			if (role.getName().equals(roleName)) {
				return true;
			}
		}
		return false;
	}

	private List<CommentListener> getListeners() {
		if (null == listeners) {
			listeners = new ArrayList<CommentListener>();
		}
		return listeners;
	}

	@Override
	public void register(CommentListener listener) {
		listeners.add(listener);
	}

	@Override
	public boolean unregister(CommentListener listener) {
		List<CommentListener> ls = getListeners();
		return ls.remove(listener);
	}
}
