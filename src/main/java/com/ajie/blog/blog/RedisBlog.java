package com.ajie.blog.blog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajie.blog.blog.exception.BlogException;
import com.ajie.chilli.cache.redis.RedisClient;
import com.ajie.chilli.cache.redis.RedisException;
import com.ajie.dao.pojo.TbBlog;

/**
 * blog实时性不严格的字段保存到redis
 *
 * @author niezhenjie
 *
 */
public class RedisBlog {

	static public Logger logger = LoggerFactory.getLogger(RedisBlogVo.class);
	/** redis客户端 */
	private RedisClient redisClient;

	public RedisBlog(RedisClient redisClient) {
		this.redisClient = redisClient;
	}

	/**
	 * 获取缓存中对应的博客的数据
	 * 
	 * @param blogId
	 * @return
	 */
	public RedisBlogVo getRedisBlog(int blogId) {
		RedisBlogVo blogVo = null;
		try {
			blogVo = redisClient.hgetAsBean(BlogService.REDIS_PREFIX,
					BlogService.REDIS_PREFIX + blogId, RedisBlogVo.class);
			if (null == blogVo) {
				blogVo = new RedisBlogVo();
			}
			blogVo.injectRedisBlog(this);
			blogVo.setId(blogId);
			return blogVo;
		} catch (RedisException e) {
			logger.warn("从redis缓存中获取RedisBlogVo失败", e);
		}
		if (null == blogVo) {
			blogVo = new RedisBlogVo();
		}
		blogVo.injectRedisBlog(this);
		blogVo.setId(blogId);
		return blogVo;
	}

	static public class RedisBlogVo {
		/** blog id */
		private int id;
		/** 评论数 */
		private int commentnum;
		/** 点赞数 */
		private int praisenum;
		/** 收藏数 */
		private int collectnum;
		/** 阅读数 */
		private int readnum;
		/** 不序列化，不保存到redis */
		private transient RedisBlog redisBlog;

		public RedisBlogVo() {

		}

		public void injectRedisBlog(RedisBlog redisBlog) {
			this.redisBlog = redisBlog;
		}

		/**
		 * 增加step个评论数
		 * 
		 * @param step
		 *            负数则为减少
		 */
		public void updateCommentNum(int step) {
			if (step == 0)
				return;
			commentnum += step;
			save();
		}

		/**
		 * 增加1个评论数
		 * 
		 */
		public void updateCommentNum() {
			updateCommentNum(1);
		}

		/**
		 * 增加step个点赞数
		 * 
		 * @param step
		 *            负数则为减少
		 */
		public void updatePraiseNum(int step) {
			if (0 == step)
				return;
			praisenum += step;
			save();
		}

		/**
		 * 增加1个点赞数
		 * 
		 */
		public void updatePraiseNum() {
			updatePraiseNum(1);
		}

		/**
		 * 增加step个收藏数
		 * 
		 * @param step
		 *            负数则为减少
		 */
		public void updateCollectNum(int step) {
			if (0 == step)
				return;
			collectnum += step;
			save();
		}

		/**
		 * 增加1个收藏数
		 * 
		 */
		public void updateCollectNum() {
			updateCollectNum(1);
		}

		/**
		 * 增加step个阅读数
		 * 
		 * @param redisClient
		 * @param step
		 *            负数则为减少
		 */
		public void updateReadNum(int step) {
			if (0 == step)
				return;
			readnum += step;
			save();
		}

		/**
		 * 增加1个阅读数
		 * 
		 */
		public void updateReadNum() {
			updateReadNum(1);
		}

		private void save() {
			try {
				redisBlog.redisClient.hset(BlogService.REDIS_PREFIX,
						BlogService.REDIS_PREFIX + id, this);
			} catch (RedisException e) {
				logger.warn("RedisBlogVo保存缓存失败", e);
			}
		}

		/**
		 * 赋值
		 * 
		 * @param blog
		 * @return
		 */
		public void assign(TbBlog blog) {
			blog.setCollectnum(commentnum);
			blog.setPraisenum(praisenum);
			blog.setReadnum(readnum);
			blog.setCommentnum(commentnum);
		}

		/**
		 * 更新redis数据，只会更新字段不为0的
		 * 
		 * @param blog
		 * @throws BlogException
		 */
		public void updateRedisData(TbBlog blog) throws RedisException {
			if (blog.getId() != id) {
				throw new RedisException("更新对象与源对象不一致，更新对象id:" + blog.getId()
						+ "，源对象：id:" + id);
			}
			if (blog.getCommentnum() != 0) {
				commentnum = blog.getCommentnum();
			}
			if (blog.getPraisenum() != 0) {
				praisenum = blog.getPraisenum();
			}
			if (blog.getReadnum() != 0) {
				readnum = blog.getReadnum();
			}
			if (blog.getCollectnum() != 0) {
				collectnum = blog.getCollectnum();
			}
			save();
		}

		public int getCommentnum() {
			return commentnum;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setCommentnum(int commentnum) {
			this.commentnum = commentnum;
		}

		public int getPraisenum() {
			return praisenum;
		}

		public void setPraisenum(int praisenum) {
			this.praisenum = praisenum;
		}

		public int getCollectnum() {
			return collectnum;
		}

		public void setCollectnum(int collectnum) {
			this.collectnum = collectnum;
		}

		public int getReadnum() {
			return readnum;
		}

		public void setReadnum(int readnum) {
			this.readnum = readnum;
		}

	}

}
