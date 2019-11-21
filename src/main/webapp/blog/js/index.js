(function(){
	//夜间模式cookid key
	var NIGHT_COOKIE_KEY = "night-mode";
	var NIGHT_COOKIE_VAL = "night";
	//用户系统地址
	var SSO_URL_KEY= "sso_service_url";
	// 模板
	String.prototype.temp = function(obj) {
		return this.replace(/\[\w+\]?/g, function(match) {
			var ret = obj[match.replace(/\[|\]/g, "")];
			return (ret + "") == "undefined" ? "" : ret;
		})
	};
	
	$(document).ready(function(){
		$.toggleDarkMode($.isDarkMode());
	});
	var tempstr = $("#iBlogTemp").html();
	var cacheTags = null;
	loadblogs();
	function loadblogs(tag) {
		var loading = $.showloading("加载中")
		var aj = $.ajax({
			type: 'post',
			data:{
				tag: tag
			},
			url: 'loadblogs',
			success: function(data) {
				console.log(data);
				if(data.code == 200){
					var blogs = data.data ||[];
					var sb = [];
					for(let i=0;i<blogs.length;i++){
						var blog = blogs[i];
						if(!blog.userHeader || !blog.userHeader.length){
							//头像为空，使用默认头像
							blog.userHeader = "/blog/images/default_user_header.jpg";
						}
						var labelsStr = blog.labels;
						if(labelsStr){
							var labels = labelsStr.split(",");
							//标签处理一下
							var lab = [];
							for(let i=0;i<labels.length;i++){
								lab.push("<span class='label'>"+labels[i]+"</span>");
							}
							blog["labels"] = lab.join("");
						}
						sb.push(tempstr.temp(blog));
						
					}
					$("#iBlogs").html(sb.join(""));
				}
				loading.hide();
				loadtags();
			},
			fail: function(e) {
				$.showToast(e)
			},
			complete: function(){
				//在文档加载时已经判断了是不是夜间模式，但是异步加载的节点比较慢，所以需要手动再判断一下是不是夜间
				if($.isDarkMode()){
					$.toggleDarkMode($.isDarkMode())
				}
			}
			
		})
	}
	
	function loadtags() {
		if(cacheTags){
			return;
		}
		$.ajax({
			type: 'post',
			data:{},
			url: 'loadtags',
			success: function(data){
				if(data.code == 200){
					var tags = data.data ||[];
					var sb = [];
					sb.push("<div class='title'>标签分类</div>")
					sb.push("<div class='tag'>全部</div>")
					for(let i=0;i<tags.length;i++){
						var tag = tags[i];
						sb.push("<div class='tag' data-name="+tag.name+">"+tag.name+"（"+tag.blogCount+"）</div>");
						if(i == 6){
							//显示7个
							sb.push("<div class='tag moreTags'>更多标签</div>");
							break;
						}
					}
					$("#iTags").find(".list-group").html(sb.join(""));
					cacheTags = tags;
				}
			},
			fail: function(e){
				$.showToast(e)
			},
			complete: function(){
				//更新一下时间
				if(!endTime){
					endTime = new Date().getTime();
				}
				//$("#iLogFrame").find(".interval").html((endTime-startTime)/1000);
			}
			
		})
	}
	
	$("#iBlogs").on("click" ,".title", "section" , function(e){
		e.stopPropagation(); //禁止冒泡
		var id = $(this).parent("section").attr("data-id");
		location.href = "blogdetail?id="+id;
	})
	
	$("#iBlogs").on("click" , ".user" , function(e){
		e.stopPropagation(); //禁止冒泡
		var _this = $(this);
		var id = _this.attr("data-id");
		var url = getSsoServiceUrl("userinfo");
		location.href = url+"?id="+id;
	})
	
	$("#iHeaderNavi").on("click",'.user',function(e){
		e.stopPropagation(); //禁止冒泡
		var _this = $(this);
		var id = _this.attr("data-id");
		var url = getSsoServiceUrl("userinfo");
		location.href = url+"?id="+id;
	})
	
	function getSsoServiceUrl(biz){
		var url = $.Storage.get(SSO_URL_KEY);
		if(!$.isEmptyObject(url)){
			if(!url.endsWith("/")){
				url += "/";
			}
			return url + biz;
		}
		$.ajax({
			url: 'getssohost',
			async: false,//阻塞执行
			success: function(data){
				url = data.msg;
			}
		})
		if(!url.endsWith("/")){
			url += "/";
		}
		$.Storage.set(SSO_URL_KEY,url);
		return url + biz;
	};
	
	var tags = $("#iTags");
	$("#iListTags").on("click",".tag",function(e){
		e.stopPropagation(); //禁止冒泡
		var _this = $(this);
		tags.removeClass("active");
		if(_this.hasClass("moreTags")){
			window.open("moretags");
			return ;
		}
		var tag = _this.attr("data-name");
		_this.siblings(".active").removeClass("active");
		_this.addClass("active");
		loadblogs(tag);
	})
	
	//点击标签 只有移动设备才有标签按钮，所以可以直接监听touchstart,方便做收起操作
	$("#iSlider").on("touchstart",function(e){
			var e = e || window.event;
			e.stopPropagation(); //禁止冒泡
			var classes = iTags.classList;
			if(tags.hasClass("active")){
				tags.removeClass("active");
			}else{
				tags.addClass("active");
			}
	});
	//移动端移动收起标签
	$(document).on("touchstart",function(e){
		if (!tags.hasClass("active")) {
			return;
		}
		if (tags[0] != e.target && tags.has(e.target).length == 0) {
			tags.removeClass("active");
		}
	})
	
	var getBlogHost = (function(){
		var host = null;
		return function(biz){
			if(host){
				if(!host.endsWith("/")){
					host += "/"
				}
				return host + biz;
			}
			$.ajax({
				url: 'getblogurl',
				async: false,//阻塞执行
				success: function(data){
					var url = data.msg;
					if(!url.endsWith("/")){
						url += "/";
					}
					host = url;
				}
			})
			return host + biz;
			
		}
	})();
	
	$("#iLogin").on("click",function(){
		var host = getBlogHost("index");
		var sso = getSsoServiceUrl("login");
		location.href = sso+"?ref="+host;
	})
	
})()