<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html class="darkMode">
<head >
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width , initial-scale=1,maximum-scale=1.0, user-scalable=0">
<title>首页</title>
<link href="${ pageContext.request.contextPath }/common/common.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/css/global.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/css/dark-mode-support.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/blog/css/index.css" rel="stylesheet" type="text/css">
<link href="${ pageContext.request.contextPath }/plugin/suspend-btn.css" rel="stylesheet" type="text/css">

<style type="text/css">
</style>

<script type="text/javascript">
//startTime文档执行到这里的时间戳,endTime文档加载完毕的时间戳
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
<body class="darkMode ">
	<div class="main ">
		<div id="iSlider" class="tag-btn darkMode">
			<span class="bar-icon"></span>
			<span class="bar-icon"></span>
			<span class="bar-icon"></span>
			<span class="bar-icon"></span>
		</div>
		<div class="header-navi darkMode" id="iHeaderNavi">
			<div onclick="javascript:location.href='addblog'" class="addblog">写博客</div>
			<div class="user-header">
				<c:choose>
					<c:when test="${not empty userid }">
						<div class="user-info user"  data-id="${userid }" data-type="userinfo" >
							<c:choose>
								<c:when test="${empty userheader }">
									<div class="user-name">${username }</div><img  class="user-header-img" src="${ pageContext.request.contextPath }/images/default_user_header.jpg" />
								</c:when>
								<c:otherwise>
									<div class="user-name">${username }</div><img  class="user-header-img" src="${userheader }" />
								</c:otherwise>
							</c:choose>
						</div>
					</c:when>
					<c:otherwise>
						<div class="login-btn" id="iLogin">登录/注册</div>
					</c:otherwise>
				</c:choose>
			</div>
		</div>

		<div class="container">
			<form id="iForm" class="hidden" method="post" action="blog">
				<input name="id" type="hidden" >
			</form>
			<div id="iTags" class="tags-block darkMode">
				<div class="list-group " id="iListTags">
		    	</div>
			</div>
			<div class="blogs" id="iBlogs">
			</div>
			<div class="qr-code">
				<div>关注公众号和小程序，获取获取最新状态</div>
				<div>
					<img src="${ pageContext.request.contextPath }/images/my_wxgz_qrcode.jpg" />
				</div>
				<div>
					<img src="${ pageContext.request.contextPath }/images/my_wxapp_code.jpg" />
				</div>
			</div>
		</div>
	</div>
	
	<div class="log-frame" id="iLogFrame">
		<div class="log-nav"><div>页面日志</div><div>info：0 warn：0 <span class="logErr error-font">error: 0</span></div></div>
		<div class="system-info"></div>
		<div class="page-log"></div>
	</div>
	
<!-- 	<div class="operating">
		<div class="operating-more">更多</div>
	</div>
	
	<div class="operating-mask">
		<div class="operating-menu">
		
		</div>
	</div> -->
	
	<jsp:include page="/footer.jsp"></jsp:include>
	<script type="text/temp"  id="iBlogTemp">
		<section class='darkMode'  data-id='[id]' >
			<div class="title">[title]</div>
			<div class="abstract-content">[abstractContent]</div>
			<div class="extract-list">
				<div class="list-left flex">
					<img class='user' data-id='[userId]' data-type='userinfo'  src="[userHeader]">
					<div class='user' data-id='[userId]' data-type='userinfo'>[user]</div>
					<div>[createDate]</div>
				</div>
				<div class="list-right flex">
					<div>阅读 [readNum]</div>
					<div>评论 [commentNum]</div>
				</div>
			</div>
		</section>
	</script>
	<script type="text/javascript">
		var serverId = '${serverId}';
	</script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/js/jquery-1.9.1.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/plugin/suspend-btn.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/common/common.js?d=2019"></script>
	<script type="text/javascript">
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
	</script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/plugin/dark-mode-support.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/js/suspend-btn-instance.js"></script>
	<script type="text/javascript" src="http://res.wx.qq.com/open/js/jweixin-1.4.0.js"></script>
	<script type="text/javascript" src="${ pageContext.request.contextPath }/blog/js/index.js?d=20190308"></script>
	<script type="text/javascript">
	/*var config = {};
	var configstr = '${config}';
	if(configstr && configstr.length){
		config = JSON.parse(configstr);
	}
	wx.config(config);*/
	 /*   wx.config({
	    debug: false, // 开启调试模式,调用的所有api的返回值会在客户端alert出来，若要查看传入的参数，可以在pc端打开，参数信息会通过log打出，仅在pc端时才会打印。
	    appId: 'wx207d32dbbf52e1c1', // 必填，公众号的唯一标识
	    timestamp: 1551875120, // 必填，生成签名的时间戳
	    nonceStr: 'b1fcb8e9-6045-4b4e-bfaa-ad2cc1acef09', // 必填，生成签名的随机串
	    signature: '830c2c68feea84893701992dba2aa8379d3f2b2e',// 必填，签名
	    jsApiList: ["getNetworkType"] // 必填，需要使用的JS接口列表
	})  */
	
	//var config = ${config};
	/* config.debug = true;
	wx.config(config); */	
	
</script>
	
</body>
</html>