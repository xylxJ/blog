package com.ajie.blog.controller.vo;

import com.ajie.blog.controller.utils.BlogUtil;
import com.ajie.chilli.utils.HtmlFilter;
import com.ajie.chilli.utils.common.StringUtils;
import com.ajie.dao.pojo.TbBlog;

/**
 * 博文vo
 *
 * @author niezhenjie
 *
 */
public class BlogVo {

	/** < ascii码 */
	public final static byte MARK_LEFT = 0x3C;
	/** > ascii码 */
	public final static byte MARK_RIGHT = 0x3E;
	private int id;
	private String content;
	/** 首页摘要 */
	private String abstractContent;
	private String title;
	private String userName;
	private int userId;
	private String userHeader;
	private String createDate;
	private int readNum;
	private int commentNum;
	private String labels;
	/** 是否为查看者自己的文章，如果是，则可以编辑 */
	private boolean isSelf;

	public BlogVo() {

	}

	public BlogVo(TbBlog blog) {
		this.id = blog.getId();
		this.content = blog.getContent();
		handleAbstractContent(this.content);
		this.title = blog.getTitle();
		this.createDate = BlogUtil.handleDate(blog.getCreatetime());
		this.readNum = blog.getReadnum();
		this.commentNum = blog.getCommentnum();
		this.userId = blog.getUserid();
		this.userHeader = blog.getUserheader();
		if (StringUtils.isEmpty(blog.getUsernickname())) {
			this.userName = blog.getUsername();
		} else {
			this.userName = blog.getUsernickname();
		}
		this.labels = blog.getLabelstrs();
	}

	/**
	 * 摘要部分去除html标签（因为截取一段内容，可能前面有标签，但是结束标签没有被包含，会导致页面标签混乱）<br>
	 * 摘要保留150字
	 *
	 * 
	 * @param content
	 */
	private void handleAbstractContent(String content) {
		// 先取400
		if (content.length() > 400) {
			content = content.substring(0, 399);
		}
		StringBuilder sb = new StringBuilder();
		// 过滤完整的标签组
		content = HtmlFilter.filterHtml(content, sb);
		if (content.length() > 200) {
			content = content.substring(0, 200);// 只显示150个字
		}
		// 结束可能是<div class='' 需要手动处理一下
		for (int i = content.length() - 1, j = 0; i >= 0; i--) {
			if (++j == 50) { // 只检查后50个字符
				break;
			}
			char ch = content.charAt(i);
			if (ch == MARK_LEFT) {
				content = content.substring(0, i - 1);
				break;
			}
		}
		abstractContent = content;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUserHeader() {
		return userHeader;
	}

	public void setUserHeader(String header) {
		this.userHeader = header;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAbstractContent() {
		return abstractContent;
	}

	public void setAbstractContent(String abstractContent) {
		this.abstractContent = abstractContent;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUser() {
		return userName;
	}

	public void setUser(String user) {
		this.userName = user;
	}

	public String getCreateDate() {
		return createDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	public int getReadNum() {
		return readNum;
	}

	public void setReadNum(int readNum) {
		this.readNum = readNum;
	}

	public int getCommentNum() {
		return commentNum;
	}

	public void setCommentNum(int commentNum) {
		this.commentNum = commentNum;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setSelf(boolean isSelf) {
		this.isSelf = isSelf;
	}

	public boolean isSelf() {
		return isSelf;
	}
}
