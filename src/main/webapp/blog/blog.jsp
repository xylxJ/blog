<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width ,initial-scale=1,maximum-scale=1.0, user-scalable=0">
<title>详情</title>
<link href="${ pageContext.request.contextPath }/css/global.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/common/common.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/blog/css/blog.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/css/dark-mode-support.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/plugin/suspend-btn.css" rel="stylesheet" type="text/css">

<style type="text/css">
</style>
<script type="text/javascript">
var errMsg , errCount = 0,startTime = new Date().getTime(),endTime;
window.addEventListener("error" , function(e){
	/* console.log("监听到错误");
	console.log(e);
	alert(e.error +" \r\n  "+e.error.stack); */
	errMsg = e.error+"<br>"+e.error.stack;
	++errCount;
	pageLog();
})

</script>
</head>
<body class="darkMode">
	<div class="main">
		<div class="header-navi darkMode">
			<div onclick="javascript:location.href='addblog'" class="addblog">写博客</div>
			<div class="user-header">
				<c:choose>
					<c:when test="${not empty userid }">
						<div class="user-info user" data-id="${userid }"
							data-type="userinfo">
							<c:choose>
								<c:when test="${empty userheader }">
									<div class="user-name">${username }</div>
									<img class="user-header-img"
										src="${ pageContext.request.contextPath }/images/default_user_header.jpg" />
								</c:when>
								<c:otherwise>
									<div class="user-name">${username }</div>
									<img class="user-header-img" src="${userheader }" />
								</c:otherwise>
							</c:choose>
						</div>
					</c:when>
					<c:otherwise>
						<div class="login-btn"
							onclick="javascript:window.location.href='gotologin'">登录/注册</div>
					</c:otherwise>
				</c:choose>
			</div>
		</div>
			<div class="container">
				<div class="container-content">
					<div class="center-main darkMode" id="iBlogs">
						<section class="title"></section>
						<section class="user-list">
							<span></span>
							<span></span>
							<span></span>
						</section>
						<section class="tags">
							<!-- <span>标签：</span> -->
						</section>
						<section class="content"></section>
					</div>
					<div class="right-info">
						<span class="hits">关注公众号和小程序，获取最新动态</span>
						<div class="wxgz-qrcode">
							<img alt="找不到图片" src="${ pageContext.request.contextPath }/images/my_wxgz_qrcode.jpg">
						</div>
						<div class="wxgz-qrcode">
							<img alt="找不到图片" src="${ pageContext.request.contextPath }/images/my_wxapp_code.jpg">
						</div>
					</div>
				</div>
				<div class="comment darkMode">
					<input type="hidden" id="iUser" value="${userid }" />
					<c:choose>
						<c:when test="${not empty userid }">
							<c:choose>
								<c:when test="${not empty userheader }">
									<img alt="图片不存在" src="${userheader }" id="iUserHeader">
								</c:when>
								<c:otherwise>
									<img alt="图片不存在" src="${ pageContext.request.contextPath }/images/default_user_header.jpg" id="iUserHeader">
								</c:otherwise>
							</c:choose>
							
						</c:when>
						<c:otherwise>
							<img alt="" src="${ pageContext.request.contextPath }/images/user_header_not_login.png" id="iUserHeader">
						</c:otherwise>
					</c:choose>
					
					<div class="text-dv"> 
						<textarea id="iComment" class="comment-text darkMode" placeholder="想对作者说点什么"></textarea>
					</div>
					<div class="submit-btn" id="iSubmit">发表</div>
				</div>
				<div class="comment-list darkMode" id="iComments">
				</div>
				<div class="footer-code darkMode">
						<span class="hits">关注公众号和小程序，获取最新动态</span>
						<div class="footer-qr-code">
							<div class="wxgz-qrcode">
								<img alt="找不到图片" data-idx="1" class="viewImg" src="${ pageContext.request.contextPath }/images/my_wxgz_qrcode.jpg">
							</div>
							<div class="wxgz-qrcode">
								<img alt="找不到图片"  data-idx="2" class="viewImg"  src="${ pageContext.request.contextPath }/images/my_wxapp_code.jpg">
							</div>
						</div>
						
						
					</div>
			</div>
	</div>
	
	<!-- 登录弹窗 -->
	<div class="login-frame" id="iLoginFrame">
		<div class="login-dv">
			<div  class="mobile-login-dv">
				<div class="navBar">
					<div>登录</div>
				</div>
				<div id="iForms" class="form-group ">
					<div class="login-form" id="iLoginForm">
						<div class="key-dv"><input type="text" name="key" placeholder="用户名/手机号/邮箱"/></div>
						<div class="passwd-dv"><input type="password" name="password" placeholder="密码"/></div>
						<div id="iLoginBtn" class="login-btn submitBtn">登录</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	<div class="log-frame" id="iLogFrame">
		<div class="log-nav"><div>页面日志</div><div>info：0 warn：0 <span class="logErr error-font">error: 0</span></div></div>
		<div class="system-info">系统：</div>
		<div class="page-log">页面错误日志</div>
	</div>
	<jsp:include page="/footer.jsp"></jsp:include>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/js/jquery-1.9.1.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/common/common.js"></script>
	<script type="text/javascript" src="http://res.wx.qq.com/open/js/jweixin-1.4.0.js"></script>
	<script>
	//日志弹窗
	var  logFrame = $("#iLogFrame").getWindow();
	logFrame.setCloser(false);
	logFrame.clickbackhide();
	//页面错误日志
	function pageLog(){
		var userAgent = navigator.userAgent;
		var frame =  $("#iLogFrame")
		var supportCss3 = $.supportcss3;
		var msg = "";
		var sb = [];
		sb.push("<div>系统：");
		sb.push(userAgent);
		if(supportCss3){
			sb.push(" <span style='color: green'>正常</span>");
		}else{
			sb.push(" <span class='col-green'>异常</span>");
			frame.find(".system-info").html("系统："+userAgent+" <span class='col-red'>异常</span>");
		}
		sb.push("</div>")
		sb.push("<div>");
		sb.push("是否支持微信js api调用：");
		if($.isBrowser("weixin")){
			sb.push("<span class='col-green'>是</span>")
		}else{
			sb.push("<span class='col-red'>否</span>")
		}
		sb.push("</div>");
		frame.find(".system-info").html(sb.join(""));
		sb = [];
		sb.push("<div>");
		sb.push("页面信息：");
		//var pageLog ="页面信息：";
		if(!errMsg) {
			sb.push("<span style='color: green'>正常</span>");
		} else {
			frame.find(".logErr").html("error："+ errCount);
			sb.push("<span style='color:red'>");
			sb.push(errMsg);
			sb.push("</span><br>");
		}
		sb.push("</div>");
		sb.push("<div>");
		sb.push("页面加载时间：");
		if(!endTime){
			endTime = startTime + 2000;
		}
		var interval = (endTime - startTime) / 1000;
		sb.push("<span class='interval'>");
		sb.push(interval);
		sb.push("</span>");
		sb.push("s");
		sb.push("</div>")
		sb.push("<div>")
		sb.push("加载速度：");
		if(interval < 2) {
			sb.push("<span class='col-green'>快</span>")
		} else if (interval <3) {
			sb.push("<span class='col-green'>较快</span>")
		} else if (interval <= 5) {
			sb.push("<span class='col-red'>慢</span>")
		} else if (interval > 5) {
			sb.push("<span class='col-red'>高延迟</span>")
		}
		sb.push("</div>");
		frame.find(".page-log").html(sb.join(""));
		logFrame.show();
	}
		var loginFrame = $("#iLoginFrame").getWindow();
		loginFrame.setCloser(false);
		loginFrame.clickbackhide();
		var tagBtn = $("#slideOutTag");
		var containner = $("#iContainner");
		tagBtn.on("click" , function(){
			containner.toggleClass("active");
		})
		var id = '${id}';
		/* var config = {};
		var configstr = '${config}';
		if(configstr && configstr.length){
			config = JSON.parse(configstr);
		}
		config.debug = true;
		wx.config(config);
		 */
	</script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/plugin/suspend-btn.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/plugin/dark-mode-support.js"></script>
		<script type="text/javascript" src="${ pageContext.request.contextPath }/js/suspend-btn-instance.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/blog/js/blog.js"></script>
	<script type="text/temp" id="iCommentTemp">
		<section class="comment-item">
			<div data-id='[userId]' class="left-user-info">
				<img src="[userHeader]">
			</div>
			<div class="right-comment-info">
				<span class="commenter">[userName]：</span><span>[content]<span class="create-date">（[createDate]	#[order]楼）</span></span>
			</div>
		</section>
	</script>
	
</body>
</html>