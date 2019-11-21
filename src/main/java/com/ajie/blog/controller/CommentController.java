package com.ajie.blog.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ajie.blog.comment.CommentException;
import com.ajie.blog.comment.CommentService;
import com.ajie.blog.controller.vo.CommentVo;
import com.ajie.blog.label.LabelService;
import com.ajie.chilli.collection.utils.TransList;
import com.ajie.chilli.common.ResponseResult;
import com.ajie.chilli.utils.Toolkits;
import com.ajie.dao.pojo.TbComment;
import com.ajie.dao.pojo.TbUser;
import com.ajie.sso.user.UserService;
import com.ajie.web.XssDefenseRequest;

/**
 * 评论控制器
 *
 * @author niezhenjie
 *
 */
@Controller
public class CommentController {
	private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
	@Resource
	private CommentService commentService;
	@Resource
	private LabelService labelService;
	@Resource
	private UserService userService;

	@ResponseBody
	@RequestMapping("getcommentsbyblog")
	public ResponseResult getcommentsbyblog(HttpServletRequest request, HttpServletResponse response) {
		int id = Toolkits.toInt(request.getParameter("blogId"), 0);
		List<TbComment> comments = commentService.getComments(id);
		List<CommentVo> list = new TransList<CommentVo, TbComment>(comments) {
			@Override
			public CommentVo trans(TbComment v) {
				CommentVo vo = new CommentVo(v);
				return vo;
			}
		};
		return ResponseResult.newResult(ResponseResult.CODE_SUC, list);
	}

	/**
	 * 添加一条评论
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("createcomment")
	public ResponseResult createcomment(HttpServletRequest request, HttpServletResponse response) {
		TbUser user = userService.getUser(request);
		if (null == user) {
			return ResponseResult.newResult(ResponseResult.CODE_SESSION_INVALID, "会话过期，请重新登录");
		}
		request = XssDefenseRequest.toXssDefenseRequest(request);
		int blogId = Toolkits.toInt(request.getParameter("blogId"), 0);
		String content = request.getParameter("content");
		// 对html标签进行转义，防xss注入
		// content = HtmlFilter.escape(content);
		try {
			commentService.createComment(content, blogId, user);
			return ResponseResult.newResult(ResponseResult.CODE_SUC, "发布成功");
		} catch (CommentException e) {
			logger.warn("评论失败", e);
			return ResponseResult.newResult(ResponseResult.CODE_ERR, e.getMessage());
		}
	}

	/**
	 * 删除一条评论
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("deletecomment")
	public ResponseResult deletecomment(HttpServletRequest request, HttpServletResponse response) {
		TbUser operator = userService.getUser(request);
		int id = Toolkits.toInt(request.getParameter("id"), 0);
		TbComment comment = new TbComment();
		comment.setId(id);
		try {
			commentService.deleteComment(comment, operator);
			return ResponseResult.newResult(ResponseResult.CODE_SUC, "删除成功");
		} catch (CommentException e) {
			logger.warn("删除评论失败", e);
			return ResponseResult.newResult(ResponseResult.CODE_ERR, e.getMessage());
		}
	}
}
