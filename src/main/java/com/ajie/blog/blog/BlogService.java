package com.ajie.blog.blog;

import java.util.Comparator;
import java.util.List;

import com.ajie.blog.blog.exception.BlogException;
import com.ajie.chilli.common.KVpair;
import com.ajie.chilli.common.KVpairs;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbUser;

/**
 * 博客服务，为了减少与数据库的交互，一些实时性要求不严格（如阅读数）的字段可以放入缓存中，<br>
 * 取数据时需要从缓存中取出并赋值，缓存借助定时器定时更新到数据库持久
 * 
 * @author niezhenjie
 */
public interface BlogService extends BlogListeners {

	/** 正常 */
	static public final int MARK_STATE_NORMAL = 1 << 0;
	/** 已删除 */
	static public final int MARK_STATE_DELETE = 1 << 2;
	/** 热门 */
	static public final int MARK_STATE_HOT = 1 << 3;
	/** 置顶 */
	static public final int MARK_STATE_TOP = 1 << 4;
	/** 草稿 */
	static public final int MARK_STATE_DRAFT = 1 << 5;

	/** 公开可见 */
	static public final int MARK_VISIT_PUBLIC = 1 << 10;
	/** 仅自己可见 */
	static public final int MARK_VISIT_SELF = 1 << 11;
	/** 仅好友可见 暂不支持 */
	static public final int MARK_VISIT_FRIEND = 1 << 12;

	/** 博文状态 -- 正常 */
	static public final KVpair STATE_NORMAL = KVpair.valueOf("正常",
			MARK_STATE_NORMAL);
	/** 博文状态 -- 已删除 */
	static public final KVpair STATE_DELETE = KVpair.valueOf("已删除",
			MARK_STATE_DELETE);
	/** 博文状态 -- 热门 */
	static public final KVpair STATE_HOT = KVpair
			.valueOf("热门 ", MARK_STATE_HOT);
	/** 博文状态 -- 置顶 */
	static public final KVpair STATE_TOP = KVpair.valueOf("置顶", MARK_STATE_TOP);
	/** 博文状态 -- 草稿 */
	static public final KVpair STATE_DRAFT = KVpair.valueOf("草稿",
			MARK_STATE_DRAFT);

	static public final KVpair VISIT_PUBLIC = KVpair.valueOf("公开",
			MARK_VISIT_PUBLIC);
	static public final KVpair VISIT_SELF = KVpair.valueOf("仅自己可见",
			MARK_VISIT_SELF);
	static public final KVpair VISIT_FRIEND = KVpair.valueOf(" 仅好友可见",
			MARK_VISIT_FRIEND);

	static public final KVpairs BLOG_MARKS = KVpairs.valueOf(STATE_NORMAL,
			STATE_DELETE, STATE_HOT, STATE_TOP, STATE_DRAFT, VISIT_PUBLIC,
			VISIT_SELF, VISIT_FRIEND);

	/** 按照创建时间排序 */
	public static final Comparator<TbBlog> CREATE_DATE = new Comparator<TbBlog>() {
		@Override
		public int compare(TbBlog o1, TbBlog o2) {
			return o2.getCreatetime().compareTo(o1.getCreatetime());
		}
	};

	/** redis map key */
	public final static String REDIS_PREFIX = "BLOG";

	/**
	 * 根据id获取博客
	 * 
	 * @param id
	 * @param operator
	 *            检验是不是自己，如果不是自己，则排除私有状态（防止在公开状态时访问过并保存了链接，后来修改了状态，
	 *            别人还是可以用保存的链接访问）如果不是私有状态的博客，则可以传空
	 * @return
	 */
	TbBlog getBlogById(int id, TbUser operator) throws BlogException;

	/**
	 * 根据id集获取博客
	 * 
	 * @param ids
	 * @return
	 * @throws BlogException
	 */
	List<TbBlog> getBlogByIds(List<Integer> ids);

	/**
	 * 用户所有博客
	 * 
	 * @param loginer
	 * @return
	 * @deprecated 请使用getBlogs(TbUser user, int state, TbUser operator)
	 */
	@Deprecated
	List<TbBlog> getMyBlogs(TbUser loginer);

	/**
	 * 搜索用户博客
	 * 
	 * @param user
	 * @return
	 * @deprecated 请使用getBlogs(TbUser user, int state, TbUser operator)
	 */
	@Deprecated
	List<TbBlog> getBlogs(TbUser user);

	/**
	 * 获取指定状态的博客
	 * 
	 * @param state
	 *            状态，0表示全部
	 * @param operator
	 *            操作者，如果是普通用户，则状态不能为删除或屏蔽，管理员或su能查看所有的博客，包括已删除和私有的，
	 *            查看一般状态的可以传null
	 * @return
	 * @deprecated 请使用getBlogs(TbUser user, int state, TbUser operator)
	 */
	@Deprecated
	List<TbBlog> getBlogs(int state, TbUser operator);

	/**
	 * 查看用户指定状态的博客
	 * 
	 * @param user
	 *            null表示获取所有
	 * @param state
	 *            状态，传0时自动根据user和operator的传入情况分析可以获取的状态
	 *            见BlogService.XXXX,支持多个标志查询，使用|即可，如查询热门和置顶：state =
	 *            STATE_HOT|MARK_STATE_TOP
	 * @param operator
	 *            操作者，如果不是自己或管理员或su，则不能查看私有的博客
	 * @return
	 */
	List<TbBlog> getBlogs(TbUser user, int state, TbUser operator);

	/**
	 * 根据关键字搜索博客
	 * 
	 * @param keyword
	 * @return
	 */
	List<TbBlog> searchBlogs(String keyword);

	/**
	 * title包含keyword的博客
	 * 
	 * @param keyword
	 * @return
	 */
	List<TbBlog> searchTitle(String keyword);

	/**
	 * 获取热门博客
	 * 
	 * @return
	 */
	List<TbBlog> getHotBlog();

	/**
	 * 获取用户置顶博客
	 * 
	 * @param user
	 * @return
	 */
	List<TbBlog> getTop(TbUser user);

	/**
	 * 添加一条博客，调用者需要将标签处理成字串格式
	 * 
	 * @param user
	 * 
	 * @param blog
	 * @return
	 * @throws BlogException
	 */
	TbBlog createBlog(TbBlog blog) throws BlogException;

	/**
	 * 删除博客
	 * 
	 * @param blog
	 * @param operator
	 *            操作者，用于检验要删除的博客是不是自己的或者是不是管理员删除
	 * @throws BlogException
	 */
	void deleteBlog(TbBlog blog, TbUser operator) throws BlogException;

	/**
	 * 删除用户的全部博客
	 * 
	 * @param user
	 * @param operator
	 *            如果是自己删自己的 则传登录者，即user
	 * @throws BlogException
	 */
	void deleteAll(TbUser user, TbUser operator) throws BlogException;

	/**
	 * 更新,需要调用者自行把mark处理好，否则可能会出现状态异常的问题
	 * 
	 * @param blog
	 * @return
	 * @throws BlogException
	 */
	TbBlog updateBlog(TbBlog blog, TbUser operator) throws BlogException;

	/**
	 * 获取博客数
	 * 
	 * @param userId
	 *            null时为全部
	 * @param operator
	 *            非管理员不能获取全部
	 * @return
	 */
	int getBlogCount(Integer userId, TbUser operator);

	/**
	 * 获取博客的标签属性
	 * 
	 * @return
	 */
	List<String> getLabels();

}
