<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width , initial-scale=1">
<title>拒绝访问</title>

<style type="text/css">
.notify{
    margin-top: 120px;
    font-weight: 600;
    height: 100%;
    font-size: 20px;
    text-align: center;
}

.qrcode-dv{
	text-align: center;
	margin-top: 20px;
}
</style>
</head>
<body>
	<div class="notify">抱歉，权限不足，如需继续访问，<br>请联系管理员！</div>
	<div class="qrcode-dv"><img id="iPic" src="${ pageContext.request.contextPath }/images/my_qr_code.jpg" width="200px"/></div>
	<script type="text/javascript" src="http://res.wx.qq.com/open/js/jweixin-1.4.0.js"></script>
	<script type="text/javascript">
		var url = "http://www.ajie18.top/images/my_wxgz_qrcode.jpg"
		iPic.addEventListener("click",function(){
			wx.previewImage({
				current: url, // 当前显示图片的http链接
				urls: [url] // 需要预览的图片http链接列表
			});
		},false)
	
	</script>
</body>
</html>