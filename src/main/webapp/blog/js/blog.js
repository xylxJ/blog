(function(){
	var main = $("#iBlogs");
	//用户系统地址
	var SSO_URL_KEY= "sso_service_url";
	// 模板
	String.prototype.temp = function(obj) {
		return this.replace(/\[\w+\]?/g, function(match) {
			var ret = obj[match.replace(/\[|\]/g, "")];
			return (ret + "") == "undefined" ? "" : ret;
		})
	}
	$(document).ready(function(){
		$.toggleDarkMode($.isDarkMode());
	})
	getblogbyid(id,function(data){
		var title = "<span class='title-text'>"+data.title+"</span>";
		var edit = "",deleteBtn="";
		if(data.self){
			var id = data.id;
			edit = "<span class='edit' data-id="+id+">编辑</span>"
			deleteBtn = "<span class='delete' data-id="+id+">删除</span>"
		}
		main.find(".title").html(title+edit+deleteBtn);
		var userList = "<span>"+data.createDate+" | </span><span class='user' data-id="+data.userId+">"+data.user+" | </span><span>阅读数 "+data.readNum+"</span>";
		main.find(".user-list").html(userList);
		var tagsBlock = main.find(".tags");
		var tagsstr = data.labels;
		if(tagsstr &&tagsstr.length){
			var tags = tagsstr.split(",");
			var sb = [];
			sb.push("<span>标签：</span>")
			for(let i=0;i<tags.length;i++){
				sb.push("<span>"+tags[i]+"</span>")
			}
			tagsBlock.html(sb.join(""));
		}
		
		main.find(".content").html(data.content)
	});
	function getblogbyid(id,callback){
		var loading = $.showloading("加载中")
		$.ajax({
			type: 'post',
			data: {
				id: id,
			},
			url: 'getblogbyid',
			success: function(data){
				if(data.code == 200){
					typeof callback === 'function' && callback(data.data);
					loadcomments(handleComment);
				}else{
					$.showToast(data.msg)
				}
			},
			fail: function(e){
				$.showToast(e)
			},
			complete: function(){
				loading.hide();
			}
		})
	}
	
	function loadcomments(callback){
		$.ajax({
			type: 'post',
			data: {
				blogId: id,
			},
			url: 'getcommentsbyblog',
			success: function(data){
				if(data.code == 200){
					typeof callback === 'function' && callback(data.data);
				}else{
					$.showToast(data.msg);
				}
			},
			fail: function(e){
				$.showToast(e);
			},
			complete: function(){
				//更新一下时间
				if(!endTime){
					endTime = new Date().getTime();
				}
			}
		})
	}
	
	function handleComment(data){
		if(!data || !data.length){
			return;
		}
		var tempstr = $("#iCommentTemp").html();
		var sb = [];
		for(let i=0;i<data.length;i++){
			var d = data[i];
			if(!d["userHeader"]){
				d["userHeader"] = "/blog/images/default_user_header.jpg"//XXX hardcode项目名
			}
			d["order"] = (i+1); //加入层数
			sb.push(tempstr.temp(d));
		}
		$("#iComments").html(sb.join(""));
	}
	
	$("#iSubmit").on("click", function(){
		comment()
	})
	
	function comment(callback){
		if(!checkComment()){
			return;
		}
		var content = $("#iComment").val();
		var loading = $.showloading("正在发布");
		$.ajax({
			type: 'post',
			data: {
				blogId: id,
				content: content
			},
			url: 'createcomment',
			success: function(data){
				if(data.code == 200){
					$.showToast("发布成功",1500,function(){
						$("#iComment").val("");//清空
						loadcomments(handleComment);
					})
				}else{
					$.showToast(data.msg);
				}
			},
			fail: function(e){
				$.showToast(e);
			},
			complete: function(){
			}
		})
	}
	
	function checkComment(){
		//检查是否登录
		var userId = $("#iUser").val();
		var conent = $("#iComment").val();
		if(!userId){
			loginFrame.show();
			return false;
		}
		if(!conent){
			$.showToast("内容为空");
			return false;
		}
		return true;
	}
	
	//var host = "http://"+location.host +"/blog/"+serverId+"/images/";
	var host = "";//TODO
	var url1 = host +"my_wxgz_qrcode.jpg";
	var url2 = host+"my_wxapp_code.jpg";
	var urls = [url1,url2];
	var imgs = [];
	$(".viewImg").on("click",function(){
		var idx = Number.parseInt($(this).attr("data-idx"));
		var current = urls[idx-1];
		//全屏查看图片
		wx.previewImage({
			current: current, // 当前显示图片的http链接
			urls: urls // 需要预览的图片http链接列表
		});
	})
	
	//登录
	$("#iLoginBtn").on("click",function(){
		var url = "dologin";
		var _this  = $(this);
		var parent = _this.parent();
		var name = $.trim(parent.find("input[name=key]").val());
		var password = $.trim(parent.find("input[name=password]").val());
		if(!name){
			$.showToast("用户名不能为空")
			return;
		}
		if(!password){
			$.showToast("密码不能为空");
			return;
		}
		var loading = $.showloading("正在登录");
		
		var host = location.host;
		/*if(host.indexOf("localhost") > -1 ||host.indexOf("127.0") > -1 || host.indexOf("10.8") > -1){
			url = "http://localhost:8081/sso/dologin.do";
		}else if(serverId == 'xff'){
			url = "http://www.ajie18.top/ajie/sso/dologin.do";
		}
		else{
			url = "http://www.ajie18.top/sso/dologin.do";
		}*/
		var url = getSsoServiceUrl("dologin");
		$.ajax({
		    url: url,
		    dataType: 'JSONP',
		    jsonpCallback: 'callback',//success后会进入这个函数，如果不声明，也不会报错，直接在success里处理也行
		    type: 'get',//这里即使设置post,其实也是get，因为jsonp只支持get
		    data:{
		    	key:name,
		    	password: password
		    },
		    success: function (data) {
		    	if(data.code != 200){
		    		$.showToast(data.msg);
		    		return;
		    	}
		    	$.showToast("登录成功",function(){
		    		//清空内容
		    		$("#iLoginForm").find("input").val("");
		    		loginFrame.hide();
		    		changeHeader(data.data);
		    	})
		    },
		    error: function(e){
		    	$.showToast(e.statusText);
		    }
		})
		
	})
	
	/**改变头部，显示用户登录*/
	function changeHeader(data){
		$("#iUser").val(data.id);
		var header = $("#iUserHeader");
		header.find(".login-btn").addClass("hidden");
		var user = header.find(".user-info");
		user.attr("data-id",data.id).attr("src",data.header);
		user.removeClass("hidden");
	}
	
	$(".user").on("click",function(e){
		e = e || window.event;
		e.stopPropagation(); //禁止冒泡
		var _this = $(this);
		var id = _this.attr("data-id");
		var url = getSsoServiceUrl("userinfo");
		location.href = url+"?id="+id;
	})
	
	function getSsoServiceUrl(biz){
		var url = $.Storage.get(SSO_URL_KEY);
		if(url){
			return url + biz;
		}
		$.ajax({
			url: 'getssohost',
			async: false,//阻塞执行
			success: function(data){
				url = data.msg;
			}
		})
		$.Storage.set(SSO_URL_KEY,url);
		return url + biz;
	};
	
	
	$("#iBlogs").on("click",".edit",function(){
		var id = $(this).attr("data-id");
		location.href = "addblog?id="+id;
	}).on("click",".delete",function(){
		if(confirm("确定删除博客及其所有的评论吗？")){
			var id = $(this).attr("data-id");
			var loading = $.showloading("正在删除");
			$.ajax({
				type: 'post',
				data:{
					id:id,
				},
				url:'deleteblog',
				success:function(data){
					$.showToast("删除成功",function(){
						location.href = "http://www.nzjie.cn";
					})
				},
				fail: function(e){
					$.showToast("系统忙，请稍后再试");
				},
				complete: function(){
					loading.hide();
				}
			})
		}

	})
	
})()