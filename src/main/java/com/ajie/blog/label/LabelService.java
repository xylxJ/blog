package com.ajie.blog.label;

import java.util.List;

import com.ajie.blog.blog.exception.BlogException;
import com.ajie.blog.label.vo.LabelVo;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbLabel;

/**
 * 标签服务接口
 *
 * @author niezhenjie
 *
 */
public interface LabelService {

	/** redis map key */
	public final static String REDIS_PREFIX = "LABEL";

	/** blog的id集分割符 */
	public final static String BLOG_IDS_SEPARATOR = ",";

	public TbLabel _Nil = new TbLabel() {
		public Integer getId() {
			return 0;
		};

		public String getName() {
			return "";
		};

		public Integer getMark() {
			return 0;
		};
	};

	/**
	 * 获取全部的标签
	 * 
	 * @return
	 */
	List<LabelVo> getLabels();

	/**
	 * 根据标签名获取标签
	 * 
	 * @param labelName
	 * @return
	 */
	TbLabel getLabel(String labelName);

	/**
	 * 打开若干个标签，如果存在，则判断是否有该博客，如果没有，放入博客；如果不存在，创建并放入博客
	 * 
	 * @param blog
	 * @param labelName
	 * @return
	 */
	void openLabels(TbBlog blog, List<String> labelName) throws BlogException;

	/**
	 * 获取指定标签下面的所有博客
	 * 
	 * @param labelName
	 * @return
	 */
	List<TbBlog> getLabelBlogs(String labelName);

	/**
	 * 删除标签 TODO 暂不支持
	 * 
	 * @param name
	 */
	void deleteLabel(String name);

}
