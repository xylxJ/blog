package com.ajie.blog.label.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ajie.blog.blog.BlogService;
import com.ajie.blog.blog.exception.BlogException;
import com.ajie.blog.label.LabelService;
import com.ajie.blog.label.vo.LabelVo;
import com.ajie.chilli.cache.redis.RedisClient;
import com.ajie.chilli.cache.redis.RedisException;
import com.ajie.chilli.collection.utils.TransList;
import com.ajie.chilli.utils.common.StringUtils;
import com.ajie.dao.mapper.TbLabelMapper;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbLabel;
import com.ajie.dao.pojo.TbLabelExample;
import com.ajie.dao.pojo.TbLabelExample.Criteria;

/**
 * 标签接口实现，因标签结构简单且量级轻，所以查询时可以全部查出来，并放入缓存
 *
 * @author niezhenjie
 *
 */
@Service
public class LabelServiceImpl implements LabelService {
	private static final Logger logger = LoggerFactory
			.getLogger(LabelServiceImpl.class);
	@Resource
	private TbLabelMapper mapper;

	@Resource
	private BlogService blogService;

	@Resource
	private RedisClient redis;

	public LabelServiceImpl() {
	}

	@Override
	public List<LabelVo> getLabels() {
		TbLabelExample ex = new TbLabelExample();
		List<TbLabel> labels = mapper.selectByExample(ex);
		return new TransList<LabelVo, TbLabel>(labels) {
			public LabelVo trans(TbLabel label) {
				return LabelVo.valueOf(label);
			}
		};
	}

	@Override
	public TbLabel getLabel(String labelName) {
		TbLabel label = null;
		try {
			label = redis.hgetAsBean(REDIS_PREFIX, labelName, TbLabel.class);
			if (null != label)
				return label;
		} catch (RedisException e) {
		}
		// 去数据库找
		TbLabelExample ex = new TbLabelExample();
		Criteria criteria = ex.createCriteria();
		criteria.andNameEqualTo(labelName);
		List<TbLabel> labels = mapper.selectByExample(ex);
		if (null == labels || labels.size() != 1)
			return null;
		label = labels.get(0);
		return label;
	}

	@Override
	public void openLabels(TbBlog blog, List<String> labelNames)
			throws BlogException {
		if (null == labelNames)
			return;
		// 先获取或创建所有传入的标签
		for (String lab : labelNames) {
			if (StringUtils.isEmpty(lab))
				continue;
			TbLabel label = getLabel(lab);
			if (null != label) {// 存在标签，判断标签是否有该博客
				String blogids = label.getBlogids();
				String[] blogs = blogids.split(BLOG_IDS_SEPARATOR);
				boolean has = false;
				for (String bid : blogs) {
					if (bid.equals(blog.getId() + "")) {
						has = true;
					}
				}
				if (has) {
					// 该标签已经有了该博客了
					continue;
				}
				label.setBlogids(blogids + BLOG_IDS_SEPARATOR + blog.getId());
				mapper.updateByPrimaryKey(label);
			} else {
				label = new TbLabel();
				label.setName(lab);
				label.setBlogids(blog.getId() + "");
				int ret = mapper.insert(label);
				// 插入成功后保存到缓存
				if (ret == 1) {
					try {
						redis.hset(REDIS_PREFIX, lab, label);
					} catch (RedisException e) {
						logger.warn("label尝试放入缓存失败" + lab, e);
					}
				}
			}
		}
	}

	@Override
	public List<TbBlog> getLabelBlogs(String labelName) {
		if (null == labelName)
			return Collections.emptyList();
		TbLabel label = getLabel(labelName);
		if (null == label)
			return Collections.emptyList();
		String blogids = label.getBlogids();
		List<Integer> blogIds = split(blogids);
		List<TbBlog> blogs = blogService.getBlogByIds(blogIds);
		return blogs;
	}

	private List<Integer> split(String ids) {
		if (StringUtils.isEmpty(ids))
			return Collections.emptyList();
		String[] blogIds = ids.split(BLOG_IDS_SEPARATOR);
		List<Integer> list = new ArrayList<Integer>(blogIds.length);
		for (String id : blogIds) {
			try {
				Integer blogid = Integer.valueOf(id);
				list.add(blogid);
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		return list;
	}

	@Override
	public void deleteLabel(String name) {

	}

}
