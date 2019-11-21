package com.ajie.blog.blog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ajie.blog.blog.BlogListener;
import com.ajie.blog.blog.BlogService;
import com.ajie.blog.blog.RedisBlog;
import com.ajie.blog.blog.RedisBlog.RedisBlogVo;
import com.ajie.blog.blog.exception.BlogException;
import com.ajie.blog.comment.CommentService;
import com.ajie.chilli.cache.redis.RedisClient;
import com.ajie.chilli.cache.redis.RedisException;
import com.ajie.chilli.common.MarkSupport;
import com.ajie.chilli.common.MarkVo;
import com.ajie.chilli.support.TimingTask;
import com.ajie.chilli.support.Worker;
import com.ajie.chilli.thread.ThreadPool;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.dao.mapper.TbBlogMapper;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbBlogExample;
import com.ajie.dao.pojo.TbBlogExample.Criteria;
import com.ajie.dao.pojo.TbUser;
import com.ajie.sso.role.Role;
import com.ajie.sso.role.RoleUtils;

@Service("blogService")
public class BlogServiceImpl implements BlogService, MarkSupport, Worker
		 {
	private static final Logger logger = LoggerFactory
			.getLogger(BlogServiceImpl.class);
	/** 数据库mapper */
	@Resource
	private TbBlogMapper mapper;

	/** 评论服务 */
	@Resource
	private CommentService commentService;

	/** redis缓存服务 */
	@Resource
	private RedisClient redisClient;

	/** redis辅助 */
	private RedisBlog redisBlog;

	private List<BlogListener> blogListeners;

	/** 线程池 */
	@Resource
	private ThreadPool threadPool;

	private Object lock = new Object();

	@Autowired
	public BlogServiceImpl(ThreadPool threadPool) {
		this.threadPool = threadPool;
		TimingTask.createTimingTask(threadPool, "blog-timing", this, "00:10",
				24 * 60 * 60 * 1000);// 零时十分，准时更新,每天这个时间执行
	}

	public RedisBlog getRedisBlog() {
		if (null == redisBlog) {
			synchronized (lock) {
				if (null == redisBlog) {
					redisBlog = new RedisBlog(redisClient);
				}
			}
		}
		return redisBlog;
	}

	@Override
	public TbBlog getBlogById(int id, TbUser operator) throws BlogException {
		TbBlogMapper proxy = getProxy();
		TbBlog blog = proxy.selectByPrimaryKey(id);
		if (null == blog)
			return null;
		// 从缓存数据中更新实时性不严格的字段
		assign(blog);
		if (RoleUtils.isAdmin(operator))
			return blog;// 管理员还能看到所有状态的博客，包括删除了的
		// 进行状态判断过滤
		int mark = blog.getMark();
		MarkVo markVo = getMarkVo(mark);
		if (markVo.isMark(MARK_STATE_DELETE))// 删除了，防止直接使用链接带上id访问
			throw new BlogException("找不到文章");
		if (markVo.isMark(VISIT_SELF.getId())
				&& (operator == null || operator.getId() != blog.getUserid()))
			throw new BlogException("找不到文章");

		return blog;
	}

	// XXX可以在这里实现保存前的一些动作
	private TbBlogMapper getProxy() {
		return (TbBlogMapper) Proxy.newProxyInstance(mapper.getClass()
				.getClassLoader(), mapper.getClass().getInterfaces(),
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						return method.invoke(mapper, args);
					}
				});
	}

	@Deprecated
	@Override
	public List<TbBlog> getBlogs(TbUser user) {
		if (null == user)
			return emptyList();
		if (user.getId() == 0) // 传过来的user忘带id，不能查询
			return emptyList();
		// 排除删除和仅自己可见的状态
		MarkVo mark = getMarkVo(0);
		mark.setMark(MARK_STATE_DELETE);
		int mark1 = mark.getMark();
		mark.setMark(VISIT_SELF.getId());
		int mark2 = mark.getMark();

		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andUseridEqualTo(user.getId());
		criteria.andMarkNotEqualTo(mark1);
		criteria.andMarkNotEqualTo(mark2);
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Deprecated
	@Override
	public List<TbBlog> getMyBlogs(TbUser loginer) {
		if (null == loginer)
			return emptyList();
		if (loginer.getId() == 0)
			return emptyList();
		MarkVo mark = getMarkVo(0);
		mark.setMark(MARK_STATE_DELETE);
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andUseridEqualTo(loginer.getId());
		criteria.andMarkNotEqualTo(mark.getMark());
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Deprecated
	@Override
	public List<TbBlog> getBlogs(int state, TbUser operator) {
		// 非管理员或超级用户不能查看删除的博客
		/*
		 * if (null == operator && state == MARK_STATE_DELETE)
		 * 
		 * if (null != operator && state == MARK_STATE_DELETE &&
		 * (checkRole(operator.getRoleids(), "管理员") ||
		 * checkRole(operator.getRoleids(), "超级用户"))) { return emptyList(); }
		 * TbBlogExample ex = new TbBlogExample(); Criteria criteria =
		 * ex.createCriteria(); criteria.andMarkEqualTo(state); List<TbBlog>
		 * blogs = mapper.selectByExample(ex); if (null == blogs) return
		 * emptyList(); return blogs;
		 */
		return emptyList();
	}

	@Override
	public List<TbBlog> getBlogs(TbUser user, int state, TbUser operator) {
		if (user != null && user.getId() == 0)
			return emptyList();
		MarkVo mark = getMarkVo(state);
		if (state == 0) {// state为0，根据传入参数（user和operator）情况分析能获取的状态
			mark.setMarks(BLOG_MARKS.getIds());
			// 管理员能查看所有的状态
			if (!isAdmin(operator)) {
				// 不是管理员，一层一层的剥夺吧
				mark.removeMark(MARK_STATE_DELETE);
				if (null != operator && null != user) {
					if (user.getId() != operator.getId()) {
						removeSensitiveState(mark);
						// TODO 如果是好友关系，则加上MARK_VISIT_FRIEND
						// mark.setMark(MARK_VISIT_FRIEND);
					}
				} else {
					removeSensitiveState(mark);
				}
			}
		} else {
			// 处理敏感状态
			if (isState(state, MARK_STATE_DELETE)
					|| isState(state, MARK_VISIT_SELF)
					|| isState(state, MARK_VISIT_FRIEND)
					|| isState(state, MARK_STATE_DRAFT)) {
				if (!isAdmin(operator)) {
					// 只有管理员能查看删除状态的博客
					mark.removeMark(MARK_STATE_DELETE);
					if (null != operator && null != user) {
						if (user.getId() != operator.getId()) {
							// 不是自己查看自己的
							removeSensitiveState(mark);
							// TODO 如果是好友关系，则加上MARK_VISIT_FRIEND
							// mark.setMark(MARK_VISIT_FRIEND);
						}
					} else {
						removeSensitiveState(mark);
					}
				}
			}
		}
		// 状态处理完毕
		state = mark.getMark();
		if (0 == state)
			return emptyList();
		// 得到一堆状态，以或（|）的形式搜索
		List<Integer> states = BLOG_MARKS.getStates(state);
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		if (null != user) {
			criteria.andUseridEqualTo(user.getId());
		}
		if (null == states || states.isEmpty()) {
			criteria.andMarkEqualTo(state);
		} else {
			criteria.andMarkIn(states);
		}

		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		Collections.sort(blogs, CREATE_DATE);
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	private boolean isState(int mark, int state) {
		return (mark & state) == state;
	}

	/**
	 * 移除敏感的状态，如果添加了新的状态且是敏感状态 需要在这里添加进去
	 * 
	 * @param mark
	 */
	private void removeSensitiveState(MarkVo mark) {
		mark.removeMark(MARK_STATE_DELETE).removeMark(MARK_VISIT_SELF)
				.removeMark(MARK_VISIT_FRIEND).removeMark(MARK_STATE_DRAFT);
	}

	@Override
	public List<TbBlog> searchBlogs(String keyword) {
		MarkVo mark = getMarkVo(0);
		mark.setMark(MARK_STATE_DELETE);
		int mark1 = mark.getMark();
		mark.setMark(MARK_VISIT_SELF);
		int mark2 = mark.getMark();
		// select * from tb_blog where (content like
		// "%keyword% and mark != mark1 & mark != mark2) or(title like "%keyword%
		// and mark != mark1 & mark != mark2)
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andContentLike(keyword);
		criteria.andMarkNotEqualTo(mark1);
		criteria.andMarkNotEqualTo(mark2);

		Criteria criteria2 = ex.createCriteria();
		criteria2.andTitleLike(keyword);
		criteria2.andMarkNotEqualTo(mark1);
		criteria2.andMarkNotEqualTo(mark2);
		ex.or(criteria2);
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Override
	public List<TbBlog> searchTitle(String keyword) {
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andTitleLike(keyword);
		criteria.andMarkNotEqualTo(MARK_STATE_DELETE);
		criteria.andMarkNotEqualTo(MARK_VISIT_SELF);
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Override
	public List<TbBlog> getHotBlog() {
		MarkVo mark = getMarkVo(MARK_STATE_HOT);
		mark.setMark(MARK_STATE_TOP);
		int markTop = mark.getMark();// 包含热门和置顶
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andMarkEqualTo(MARK_STATE_HOT);

		Criteria criteria2 = ex.createCriteria();
		criteria2.andMarkEqualTo(markTop);
		ex.or(criteria2);
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Override
	public List<TbBlog> getTop(TbUser user) {
		if (null == user)
			return emptyList();
		if (user.getId() == 0)
			return emptyList();
		TbBlogExample ex = new TbBlogExample();
		Criteria criteria = ex.createCriteria();
		criteria.andUseridEqualTo(user.getId());
		criteria.andMarkEqualTo(STATE_HOT.getId());
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		// 获取缓存的数据并赋值
		assign(blogs);
		return blogs;
	}

	@Override
	public TbBlog createBlog(TbBlog blog) throws BlogException {
		if (null == blog)
			return null;
		int ret = mapper.insert(blog);
		if (ret != 1)
			throw new BlogException("博客发布失败，请稍后再试");
		final TbBlog b = blog;
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<BlogListener> listeners = getBlogListeners();
				for (BlogListener l : listeners) {
					l.onCreate(b);
				}
			}
		}).start();
		return blog;
	}

	@Override
	public void deleteBlog(TbBlog blog, TbUser operator) throws BlogException {
		if (null == blog || blog.getId() == 0)
			throw new BlogException("删除异常，请稍后再试");
		if (null == operator)
			throw new BlogException("删除失败，操作用户为空");
		final TbBlog b = getBlogById(blog.getId(), operator);
		if (null == b) {
			logger.error("文章不存在");
		}
		mapper.updateBlogMark(b.getId(), operator.getId(), MARK_STATE_DELETE);
		logger.info("用户" + operator.getId() + " 删除了博客:" + b.getId());
		// 删除通知
		Runnable r = new Runnable() {
			@Override
			public void run() {
				List<BlogListener> Listeners = getBlogListeners();
				for (BlogListener l : Listeners) {
					l.onDelete(b);
				}
			}
		};
		threadPool.execute(r);

	}

	@Override
	public void deleteAll(TbUser user, TbUser operator) throws BlogException {
		if (null == user || user.getId() == 0 || null == operator
				|| operator.getId() == 0)
			throw new BlogException("删除失败，删除用户或操作者为空");
		if (user.getId() != operator.getId() || !isAdmin(operator))
			throw new BlogException("删除失败，不能删除非自己的博客");
		mapper.updateBlogsMark(operator.getId(), MARK_STATE_DELETE);
		logger.info("用户" + operator.getId() + " 删除了所有的博客");
	}

	@Override
	public TbBlog updateBlog(TbBlog blog, TbUser operator) throws BlogException {
		if (null == operator)
			throw new BlogException("删除失败，操作者为空");
		if (null == blog || blog.getId() == 0)
			throw new BlogException("删除失败，文章不存在");
		/*
		 * TbBlogExample ex = new TbBlogExample(); Criteria criteria =
		 * ex.createCriteria(); criteria.andIdEqualTo(blog.getId());
		 * criteria.andUseridEqualTo(operator.getId());
		 * mapper.updateByExample(blog, ex);
		 */
		TbBlog b = getBlogById(blog.getId(), operator);
		if (null == b) {
			throw new BlogException("删除失败，文章不存在");
		}
		mapper.updatePartByPrimaryKey(blog);
		return blog;
	}

	@Override
	public MarkVo getMarkVo(int mark) {
		return new MarkVo(mark);
	}

	private List<TbBlog> emptyList() {
		return Collections.emptyList();
	}

	/**
	 * 获取redis缓存的数据
	 * 
	 * @param blog
	 */
	private void assign(TbBlog blog) {
		RedisBlogVo vo = getRedisBlog().getRedisBlog(blog.getId());
		vo.assign(blog);
	}

	/**
	 * 获取redis缓存的数据
	 * 
	 * @param blog
	 */
	private void assign(List<TbBlog> blogs) {
		for (TbBlog blog : blogs) {
			RedisBlogVo vo = getRedisBlog().getRedisBlog(blog.getId());
			vo.assign(blog);
		}
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

	@Override
	public List<TbBlog> getBlogByIds(List<Integer> ids) {
		if (null == ids || ids.isEmpty())
			return emptyList();
		TbBlogExample ex = new TbBlogExample();
		for (int id : ids) {
			Criteria criteria = ex.createCriteria();
			criteria.andIdEqualTo(id);
			ex.or(criteria);
		}
		List<TbBlog> blogs = mapper.selectByExample(ex);
		if (null == blogs)
			return emptyList();
		return blogs;
	}

	@Override
	public void work() {
		TbBlogExample ex = new TbBlogExample();
		// 更新评论数和阅读数
		List<TbBlog> blogs = mapper.selectByExample(ex);
		for (TbBlog blog : blogs) {
			syncData(blog);
		}
		logger.info("定时任务更新blog的评论数和阅读数");
	}

	/**
	 * 博客的数据同步
	 * 
	 * @param blog
	 */
	private void syncData(TbBlog blog) {
		int commentCount = commentService.getBlogCommentCount(blog.getId());
		int readNum = blog.getReadnum();
		RedisBlogVo vo = getRedisBlog().getRedisBlog(blog.getId());
		if (null != vo) {
			readNum = Math.max(readNum, vo.getReadnum());
			// 更新评论数到redis
			blog.setCommentnum(commentCount);
			try {
				vo.updateRedisData(blog);
				logger.info("定时任务同步博客评论数到redis缓存,id:" + blog.getId() + "，变更前："
						+ vo.getCommentnum() + "，变更后：" + commentCount);
			} catch (RedisException e) {
				logger.warn("更新评论数到redis缓存失败", e);
			}
		}
		mapper.updateBlogCRCount(blog.getId(), commentCount, readNum);
	}

	@Override
	public int getBlogCount(Integer userId, TbUser opeator) {
		if (null == userId || userId == 0) {
			if (!isAdmin(opeator)) {
				return 0;
			}
			return mapper.getBlogCount();
		}
		return mapper.getUserBlogCount(userId);
	}

	@Override
	public List<String> getLabels() {
		List<String> lables = mapper.selectBlogLabels();
		if (null == lables) {
			return Collections.emptyList();
		}
		return lables;
	}

	@Override
	public void register(BlogListener listener) {
		if (null == listener) {
			return;
		}
		if (null == blogListeners) {
			blogListeners = new ArrayList<BlogListener>();
		}
		blogListeners.add(listener);
	}

	@Override
	public boolean unRegister(BlogListener listener) {
		if (null == listener) {
			return false;
		}
		return blogListeners.remove(listener);
	}

	@Override
	public List<BlogListener> getBlogListeners() {
		if (null == blogListeners) {
			return Collections.emptyList();
		}
		return blogListeners;
	}

/*	@Override
	public void afterPropertiesSet() throws Exception {
		// 定时对redis缓存数据进行处理
		TimingTask.createTimingTask(threadPool, "blog-timing", this, "00:10",
				24 * 60 * 60 * 1000);// 零时十分，准时更新,每天这个时间执行
	}*/
}
