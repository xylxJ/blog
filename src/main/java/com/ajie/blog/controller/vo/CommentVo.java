package com.ajie.blog.controller.vo;

import com.ajie.blog.controller.utils.BlogUtil;
import com.ajie.dao.pojo.TbComment;

/**
 * 评论vo
 *
 * @author niezhenjie
 *
 */
public class CommentVo {

	private int id;
	private String content;
	private String userName;
	private int userId;
	private String userHeader;
	private String createDate;

	public CommentVo() {

	}

	public CommentVo(TbComment comment) {
		id = comment.getId();
		content = comment.getContent();
		createDate = BlogUtil.handleDate(comment.getCreatetime());
		userName = comment.getUsername();
		userHeader = comment.getUserheader();
		userId = comment.getUserid();
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

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getCreateDate() {
		return createDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

}
