package com.ajie.blog.label.vo;

import com.ajie.chilli.utils.common.StringUtils;
import com.ajie.dao.pojo.TbLabel;

/**
 * 标签vo
 *
 * @author niezhenjie
 *
 */
public class LabelVo {
	/**
	 * 标签名
	 */
	private String name;

	/**
	 * 标签id
	 */
	private int id;

	/**
	 * 标签博客数
	 */
	private int blogCount;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBlogCount() {
		return blogCount;
	}

	public void setBlogCount(int blogCount) {
		this.blogCount = blogCount;
	}

	public static LabelVo valueOf(String name, int id, int blogCount) {
		LabelVo vo = new LabelVo();
		vo.setName(name);
		vo.setId(id);
		vo.setBlogCount(blogCount);
		return vo;
	}

	public static LabelVo valueOf(TbLabel label) {
		LabelVo vo = new LabelVo();
		vo.setId(label.getId());
		vo.setName(label.getName());
		String blogids = label.getBlogids();
		if (StringUtils.isEmpty(blogids)) {
			vo.setBlogCount(0);
		} else {
			String[] ids = blogids.split(",");
			vo.setBlogCount(ids.length);
		}
		return vo;
	}
}
