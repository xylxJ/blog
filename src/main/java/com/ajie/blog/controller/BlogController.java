package com.ajie.blog.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.ajie.blog.blog.BlogService;
import com.ajie.blog.blog.RedisBlog;
import com.ajie.blog.blog.RedisBlog.RedisBlogVo;
import com.ajie.blog.blog.exception.BlogException;
import com.ajie.blog.comment.CommentService;
import com.ajie.blog.controller.vo.BlogVo;
import com.ajie.blog.label.LabelService;
import com.ajie.blog.label.vo.LabelVo;
import com.ajie.chilli.cache.redis.RedisClient;
import com.ajie.chilli.collection.SwitchUnmodifiableList;
import com.ajie.chilli.collection.utils.TransList;
import com.ajie.chilli.common.ResponseResult;
import com.ajie.chilli.picture.Picture;
import com.ajie.chilli.picture.PictureException;
import com.ajie.chilli.picture.PictureService;
import com.ajie.chilli.utils.TimeUtil;
import com.ajie.chilli.utils.Toolkits;
import com.ajie.chilli.utils.common.StringUtils;
import com.ajie.dao.mapper.TbBlogMapper;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.dao.pojo.TbUser;
import com.ajie.resource.ResourceService;
import com.ajie.sso.user.UserService;
import com.ajie.web.XssDefenseRequest;
import com.ajie.web.utils.CookieUtils;

/**
 * 博客控制器
 *
 * @author niezhenjie
 *
 */
@Controller
public class BlogController {
	public Logger logger = LoggerFactory.getLogger(BlogController.class);
	/** 博客内容标签黑名单 */
	private static final List<String> CONTENT_BLACK_ELE = SwitchUnmodifiableList
			.valueOf("script");
	private static String prefix = "blog/";
	@Resource
	private BlogService blogService;
	@Resource
	private CommentService commentService;
	@Resource
	private LabelService labelService;
	@Resource
	private UserService userService;
	@Resource
	private PictureService pictureService;
	@Resource
	private RedisClient redisClient;
	@Resource
	private ResourceService resource;
	@Resource
	private String stopCommand;
	@Resource
	private String admin;

	@Resource
	private TbBlogMapper mapper;

	/** sso系统链接 */
	@Resource(name = "ssourl")
	private String ssoUrl;
	/** blog系统链接 */
	@Resource(name = "blogUrl")
	private String blogUrl;
	/** sso系统内网映射链接 */
	@Resource(name = "mappingSso")
	private String mappingSso;

	/** sso系统链接 */
	@Resource(name = "mappingBlog")
	private String mappingBlog;

	/**
	 * 优雅的方式关闭服务器
	 * 
	 * @param request
	 */
	@RequestMapping("/stop")
	public void stop(HttpServletRequest request, HttpServletResponse response) {
		String passwd = request.getParameter("passwd");
		if (null != passwd && stopCommand.equals(passwd)) {
			logger.info("无用户模式下操作关闭服务器");
			System.exit(0);// 在sso系统没有启动的情况下关闭
		}
		TbUser user = userService.getUser(request);
		if (null == user) {
			return;
		}
		if (!admin.equals(user.getName())) {
			return;
		}
		logger.info(user.getName() + "正在操作关闭服务器");
		System.exit(0);
	}

	/**
	 * 首页路径
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("index")
	public String index(HttpServletRequest request, HttpServletResponse response) {
		TbUser user = userService.getUser(request);
		if (null != user) {
			if (!StringUtils.isEmpty(user.getNickname())) {
				request.setAttribute("username", user.getNickname());
			} else {
				request.setAttribute("username", user.getName());
			}
			request.setAttribute("userheader", user.getHeader());
			request.setAttribute("userid", user.getId());
		}
		return prefix + "index";

	}

	/**
	 * 博客详情（以前是使用blog，但是使用/拦截后是不行的，因为控制器的名称是BlogControl，当访问/blog/blog时，
	 * springmvc会当做是访问/blog项目下的blog控制器，没有方法名，自动找到index，所以最后会访问到index）
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("blogdetail")
	public String blogdetail(HttpServletRequest request,
			HttpServletResponse response) {
		TbUser user = userService.getUser(request);
		if (null != user) {
			request.setAttribute("username", user.getName());
			request.setAttribute("userheader", user.getHeader());
			request.setAttribute("userid", user.getId());
		}
		int id = Toolkits.toInt(request.getParameter("id"), 0);
		// 更新博客的浏览数
		RedisBlog redisBlog = new RedisBlog(redisClient);
		RedisBlogVo vo = redisBlog.getRedisBlog(id);
		vo.updateReadNum();
		/*
		 * // 获取微信配置 WeixinResource wx = resource.getWeixinResource(); JsConfig
		 * config = null; if (null != wx) { config = wx.getJsConfiig(); if (null
		 * != config) { String url = getRequestUrl(request); config.sign(url);
		 * request.setAttribute("config", JsonUtils.toJSONString(config)); } }
		 * if (null == config) { request.setAttribute("config", ""); }
		 */
		response.addHeader("Access-Control-Allow-Origin",
				"http://localhost:8081");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT");
		response.addHeader("Access-Control-Allow-Headers", "X-Custom-Header");
		request.setAttribute("id", id);
		return prefix + "blog";
	}

	/**
	 * 请求链接，包含参数部分
	 * 
	 * @param request
	 * @return
	 */
	/*
	 * private String getRequestUrl(HttpServletRequest request) { String url =
	 * request.getRequestURL().toString(); String query =
	 * request.getQueryString(); if (null != query) { url += "?" + query; }
	 * return url; }
	 */

	/**
	 * 添加或编辑博客页面
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/addblog")
	public String addblog(HttpServletRequest request,
			HttpServletResponse response) {
		String id = request.getParameter("id");
		TbUser user = userService.getUser(request);
		if (null != user) {
			request.setAttribute("username", user.getName());
			request.setAttribute("userheader", user.getHeader());
			request.setAttribute("userid", user.getId());
		}
		request.setAttribute("id", id);
		return prefix + "addblog";
	}

	@RequestMapping("/moretags")
	public String moretags(HttpServletRequest request,
			HttpServletResponse response) {
		logger.info(1 / 0 + "");
		return prefix + "moretags";
	}

	/**
	 * 加载首页数据
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@ResponseBody
	@RequestMapping("/loadblogs")
	public ResponseResult loadblogs(HttpServletRequest request,
			HttpServletResponse response) {
		TbUser user = userService.getUser(request);
		String tag = request.getParameter("tag");
		// 小程序请求tag没带过来时，拿到的竟然是带引号的"null"，坑啊
		List<TbBlog> blogs = null;
		if (null == tag || "null".equals(tag)) {
			blogs = blogService.getBlogs(user, 0, null);
		} else {
			List<TbBlog> tagBlogs = labelService.getLabelBlogs(tag);
			List<Integer> blogIds = new ArrayList<Integer>(tagBlogs.size());
			for (TbBlog blog : tagBlogs) {
				blogIds.add(blog.getId());
			}
			blogs = blogService.getBlogByIds(blogIds);
		}

		List<BlogVo> trans = new TransList<BlogVo, TbBlog>(blogs) {
			@Override
			public BlogVo trans(TbBlog v) {
				return new BlogVo(v);
			}
		};
		return ResponseResult.newResult(ResponseResult.CODE_SUC, trans);
	}

	/**
	 * 我的博客，支持jsonp请求
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@ResponseBody
	@RequestMapping("/myblogs")
	public Object myblogs(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		TbUser user = userService.getUser(request);
		String callback = request.getParameter("callback");
		ResponseResult result = null;
		/*
		 * 别人也能访问你的主页 if (null == user) { result =
		 * ResponseResult.newResult(ResponseResult.CODE_SESSION_INVALID,
		 * "会话过期，请重新登录"); if (null == callback) return result; String jsonp =
		 * ResponseResult.toJsonp(result, "callback"); PrintWriter out =
		 * response.getWriter(); out.write(jsonp); return null; }
		 */

		String type = request.getParameter("type");
		List<TbBlog> blogs = null;
		if (StringUtils.isEmpty(type)) {
			int state = BlogService.MARK_STATE_NORMAL;
			state |= BlogService.MARK_STATE_HOT;
			state |= BlogService.MARK_STATE_TOP;
			state |= BlogService.MARK_VISIT_FRIEND;
			state |= BlogService.MARK_VISIT_PUBLIC;
			state |= BlogService.MARK_VISIT_SELF;
			blogs = blogService.getBlogs(user, state, user);
		} else if (StringUtils.eq("draft", type)) { // 草稿
			blogs = blogService.getBlogs(user, BlogService.MARK_STATE_DRAFT,
					user);
		}
		List<BlogVo> trans = new TransList<BlogVo, TbBlog>(blogs) {
			@Override
			public BlogVo trans(TbBlog v) {
				return new BlogVo(v);
			}
		};
		result = ResponseResult.newResult(ResponseResult.CODE_SUC, trans);
		if (null == callback)
			return result;
		String jsonp = ResponseResult.toJsonp(result, "callback");
		PrintWriter out = response.getWriter();
		out.write(jsonp);
		return null;

	}

	/**
	 * 加载所有标签
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/loadtags")
	public ResponseResult loadtags(HttpServletRequest request,
			HttpServletResponse response) {
		List<LabelVo> labels = labelService.getLabels();
		return ResponseResult.newResult(ResponseResult.CODE_SUC, labels);
	}

	/**
	 * 通过id拿到博客
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getblogbyid")
	public ResponseResult getblogbyid(HttpServletRequest request,
			HttpServletResponse response) {
		int id = Toolkits.toInt(request.getParameter("id"), 0);
		TbUser operator = userService.getUser(request);
		try {
			TbBlog blog = blogService.getBlogById(id, operator);
			if (null == blog) {
				return ResponseResult.newResult(ResponseResult.CODE_ERR,
						"文章不存在");
			}
			BlogVo vo = new BlogVo(blog);
			if (null != operator) {
				if (blog.getUserid().equals(operator.getId())) {
					vo.setSelf(true);
				}
			}
			return ResponseResult.newResult(ResponseResult.CODE_SUC, vo);
		} catch (BlogException e) {
			return ResponseResult.newResult(ResponseResult.CODE_ERR,
					e.getMessage());
		}
	}

	/**
	 * 添加一条博客或博客草稿
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@ResponseBody
	@RequestMapping("/submitblog")
	public ResponseResult submitblog(HttpServletRequest request,
			HttpServletResponse response) throws UnsupportedEncodingException {
		request.setCharacterEncoding("utf-8");
		setAjaxContentType(response);
		TbUser operator = userService.getUser(request);
		if (null == operator) {
			return ResponseResult.newResult(
					ResponseResult.CODE_SESSION_INVALID, "会话过期，请重新登录");
		}
		request = XssDefenseRequest.toXssDefenseRequest(request,
				XssDefenseRequest.MODE_BLACK, CONTENT_BLACK_ELE);
		boolean isDraft = "draft".equals(request.getParameter("op"));
		String blogId = request.getParameter("id");
		String title = request.getParameter("title");
		if (StringUtils.isEmpty(title)) {
			title = "无标题";// 保存草稿可能没有标题
		}
		String content = request.getParameter("content");
		String labelstrs = request.getParameter("labels");
		TbBlog blog = new TbBlog(title, content);
		blog.setLabelstrs(labelstrs);
		blog.setUserid(operator.getId());
		blog.setUserheader(operator.getHeader());
		blog.setUsername(operator.getName());
		blog.setUsernickname(operator.getNickname());
		String msg = assertBlog(blog, isDraft);
		if (null != msg)
			return ResponseResult.newResult(ResponseResult.CODE_ERR, msg);
		try {
			if (isDraft) { // 添加草稿
				blog.setMark(BlogService.MARK_STATE_DRAFT);
			}
			if (null == blogId) {
				blogService.createBlog(blog);
			} else {
				// 发布草稿
				int id = Integer.valueOf(blogId);
				blog.setId(id);
				blogService.updateBlog(blog, operator);
			}

		} catch (BlogException e) {
			logger.warn("", e);
			return ResponseResult.newResult(
					ResponseResult.CODE_ERR,
					isDraft ? "无法添加草稿【" + e.getMessage() + "】" : "发布失败【"
							+ e.getMessage() + "】");
		}
		List<String> list = Arrays.asList(labelstrs
				.split(LabelService.BLOG_IDS_SEPARATOR));
		try {
			labelService.openLabels(blog, list);
		} catch (BlogException e) {
			logger.warn("", e);
			String type = isDraft ? "草稿保存" : "博客发布";
			return ResponseResult.newResult(ResponseResult.CODE_ERR, type
					+ "成功，标签保存失败");
		}
		return ResponseResult.newResult(ResponseResult.CODE_SUC,
				isDraft ? "保存成功" : "发布成功", blog.getId());
	}

	/**
	 * 校验内容合法性
	 * 
	 * @param blog
	 * @param isDraft
	 *            true保存 草稿箱
	 * @return
	 */
	private String assertBlog(TbBlog blog, boolean isDraft) {

		if (null == blog) {
			return "无内容";
		}
		if (isDraft) {
			if (StringUtils.isEmpty(blog.getContent())) {
				return "内容为空";
			}
			return null;
		}
		if (StringUtils.isEmpty(blog.getTitle())) {
			return "标题为空";
		}
		if (StringUtils.isEmpty(blog.getContent())) {
			return "内容为空";
		}
		if (StringUtils.isEmpty(blog.getLabelstrs())) {
			return "标签为空";
		}
		return null;
	}

	/**
	 * 获取用户的博客
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("deprecation")
	@ResponseBody
	@RequestMapping("/getblogbyuser")
	public Object getblogbyuser(HttpServletRequest request,
			HttpServletResponse response) {
		int userId = Toolkits.toInt(request.getParameter("userId"), 0);
		String callback = request.getParameter("callback");
		TbUser user = new TbUser();
		user.setId(userId);
		List<TbBlog> blogs = blogService.getMyBlogs(user);
		ResponseResult result = ResponseResult.newResult(
				ResponseResult.CODE_SUC, blogs);
		if (null == callback) {
			return result;
		}
		MappingJacksonValue jsonp = new MappingJacksonValue(result);
		jsonp.setJsonpFunction(callback);
		return jsonp;
	}

	@ResponseBody
	@RequestMapping("deleteblog")
	public Object deleteblog(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		TbUser operator = userService.getUser(request);
		int id = Toolkits.toInt(request.getParameter("id"), 0);
		String callback = request.getParameter("callback");
		TbBlog blog = new TbBlog();
		blog.setId(id);
		ResponseResult result = null;
		try {
			blogService.deleteBlog(blog, operator);
			result = ResponseResult.success("删除成功");

		} catch (BlogException e) {
			logger.warn("删除博文失败", e);
			result = ResponseResult.fail(e.getMessage());
		}
		if (null == callback) {
			return result;
		}
		String jsonp = ResponseResult.toJsonp(result, "callback");
		PrintWriter out = response.getWriter();
		out.write(jsonp);
		return null;
	}

	@ResponseBody
	@RequestMapping("getblogcount")
	public ResponseResult getblogcount(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		TbUser operator = userService.getUser(request);
		int userId = Toolkits.toInt(request.getParameter("id"), 0);
		int count = blogService.getBlogCount(userId, operator);
		String callback = request.getParameter("callback");
		if (null == callback)
			return ResponseResult.success(count);
		String jsonp = ResponseResult.toJsonp(ResponseResult.success(count),
				"callback");
		PrintWriter out = response.getWriter();
		out.write(jsonp);
		return null;
	}

	/**
	 * 退出登录
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/logout")
	public ResponseResult logout(HttpServletRequest request,
			HttpServletResponse response) {
		String token = null;
		Cookie[] cookies = request.getCookies();
		if (null == cookies) {
			ResponseResult.newResult(ResponseResult.CODE_SUC, "退出成功");
		}

		for (Cookie cookie : cookies) {
			String name = cookie.getName();
			if (UserService.COOKIE_KEY.equals(name)) {
				token = cookie.getValue();
			}
		}
		userService.logoutByToken(token);
		CookieUtils.setCookie(request, response, UserService.COOKIE_KEY, token,
				0);
		return ResponseResult.newResult(ResponseResult.CODE_SUC, "退出成功");
	}

	/**
	 * 去登录
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/gotologin")
	public void gotologin(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String url = ssoUrl;
		if (!url.endsWith("/")) {
			url += "/";
		}
		String host = request.getHeader("host");
		if (host.indexOf("j-") > -1) {
			// 代理映射
			url = mappingSso;
			if (!url.endsWith("/")) {
				url += "/";
			}
		}
		String protocol = request.getProtocol();
		if (null == protocol) {
			// 理论上不可能吧？？？
			protocol = "http";
		}
		if (protocol.toLowerCase().startsWith("https")) {
			protocol = "https";
		} else {
			protocol = "http";
		}
		// uri部分
		String uri = request.getRequestURI();
		// 参数部分
		String query = request.getQueryString();
		String ref = "";
		ref = protocol + "://" + host + uri;
		if (!StringUtils.isEmpty(query)) {// 有带参
			try {
				// %3f解码后是?
				ref += "%3f" + URLEncoder.encode(query, "utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.warn("不支持utf-8字符编码转换" + query);
			}
		}
		response.sendRedirect(url + "login?ref=" + ref);
	}

	/**
	 * 用户详情
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void gotouserinfo(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String id = request.getParameter("id");
		String url = ssoUrl;
		if (!url.endsWith("/")) {
			url += "/";
		}
		response.sendRedirect(url + "userinfo?id=" + id);
	}

	/**
	 * 富文本的图片上传
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("/imgupload")
	public void imgupload(@RequestParam("upload") CommonsMultipartFile file,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		// 如果设置了头是json响应 则返回的结果的标签会被转义，坑爹啊，调了我这么久
		// setAjaxContentType(response);
		PrintWriter out = response.getWriter();
		String cKEditorFuncNum = request.getParameter("CKEditorFuncNum");
		InputStream stream = file.getInputStream();
		// 文件夹，按日期创建
		String folder = TimeUtil.compactYMD(new Date());
		try {
			Picture p = pictureService.create(folder, stream);
			out.println(editorcallback(cKEditorFuncNum, p.getAddress()));
			out.flush();
		} catch (PictureException e) {
			logger.error("图片上传失败", e);
			out.println(editorcallback(cKEditorFuncNum, e.getMessage()));
			out.flush();
		}
		/*
		 * out.println("<script type='text/javascript'>");
		 * out.println("window.parent.CKEDITOR.tools.callFunction(" +
		 * CKEditorFuncNum + ",'http://www.ajie18.top/images/view.jpg','')");
		 * out.println("</script>");
		 */
		out.close();
	}

	/**
	 * 富文本编辑器 回调
	 * 
	 * @param msg
	 * @return
	 */
	private String editorcallback(String cKEditorFuncNum, String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("<script type='text/javascript'>");
		sb.append("window.parent.CKEDITOR.tools.callFunction(");
		sb.append(cKEditorFuncNum);
		sb.append(",");
		sb.append("'");
		sb.append(msg);
		sb.append("'");
		sb.append(",'')");
		sb.append("</script>");
		return sb.toString();
	}

	private void setAjaxContentType(HttpServletResponse response) {
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("utf-8");
	}

	@RequestMapping("test")
	public String test() {
		return prefix + "test";
	}

	@RequestMapping("uploadtest")
	public void uploadtest(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			InputStream is = request.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			int av = bis.available();
			System.out.println(av);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@ResponseBody
	@RequestMapping("getssohost")
	public ResponseResult getssohost(HttpServletRequest request,
			HttpServletResponse response) {
		setAjaxContentType(response);
		String host = request.getHeader("host");
		String ssoHost = ssoUrl;
		if (host.indexOf("j-") > -1) {
			// 走了代理映射
			ssoHost = mappingSso;
		}
		return ResponseResult.success(ssoHost);
	}

	/**
	 * 获取博客系统的链接
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@ResponseBody
	@RequestMapping("getblogurl")
	ResponseResult getblogurl(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		setAjaxContentType(response);
		String host = request.getHeader("host");
		String url = blogUrl;
		if (host.indexOf("j-") > -1) {
			// 走了代理映射
			url = mappingBlog;
		}
		return ResponseResult.success(url);
	}

}
