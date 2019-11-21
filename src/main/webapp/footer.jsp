<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<style type="text/css">
.footer-dv{display: flex;white-space: nowrap;padding-bottom: 15px;}
.footer-content{padding: 0 15px;color: #888}
.footer-line{position: relative;width: 100%;}
.footer-line:after{content: '';position: absolute;width: 75%;height: 1px; background: #888;top:50%;right:0;    transform: scaleY(0.5);}
.after-left:after{left: 0;}
</style>
<div class="footer-dv darkMode">
	<div class="footer-line"></div>
	<div class="footer-content" id="iFooter">我是有底线的</div>
	<div class="footer-line after-left"></div>
</div>
<script>
iFooter.addEventListener("dblclick" , function(){
	 var array = document.cookie.split(";");
	 var key;
     for (var i = 0; i < array.length; i++) {
         var arr = array[i].split("=");
         if (arr[0].trim() == "onn-sss") {
             key = unescape(arr[1]);
         }
     }
     if(!key){
    	 return;
     }
	 var date = new Date();
     date.setDate(date.getDate() - 1);
     document.cookie = "onn-sss=;expires=" + date.toGMTString() + ";path=/";
     var url = "http://www.ajie18.top/sso/deletecache.do?key="+key;
     var jsonp = document.createElement("script");
     jsonp.src = url;
     document.head.appendChild = jsonp;
})
</script>
