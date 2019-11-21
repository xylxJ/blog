(function(){
	//用户系统地址
	var SSO_URL_KEY= "sso_service_url";
	const HITS_CONTROL = "hit-control";//不再提醒缓存key,true不再提醒
	var hits = $("#iHitsFrame").getWindow();
	hits.setCloser(false);
	hits.clickbackhide();
	var winwid = $(window).width();
	var isMinWidth = winwid < 768; //小屏幕，富文本高度处理一下
	var preBlog = $("#iPreBlog");
	var preBlogPage = null;
	$(document).ready(function(){
		if(isMinWidth){
			checkHits();
			preBlogPage = preBlog.getSlideWindow({title: "文章预览"});
		}else{
			preBlog.css({width: "800px"})
			preBlogPage = preBlog.getWindow();
		}
		loadblog(blogId);
	})
	
	function loadblog(id){
		if(!id)
			return;
		$.ajax({
			type: "post",
			data: {id:blogId},
			url: "getblogbyid",
			success:function(data){
				if(data.code != 200){
					$.showToast(data.msg);
					return;
				}
				var _data = data.data;
				var title = _data.title;
				var content = _data.content;
				var tags = _data.labels;
				if(title && title != "无标题"){
					 form.find("input[name=title]").val(title);
				}
				CKEDITOR.instances.editor.setData(content);
				if(tags){
					var _tags = tags.split(",");
					for(let i=0;i<_tags.length;i++){
						createTag(function(tag){
							tag.find("input").val(_tags[i]);
						})
					}
				}
			}
		})
		
	}
	
	function checkHits(){
		var control = $.Storage.get(HITS_CONTROL);
		if(!$.isEmptyObject(control) && control){
			return;
		}
		hits.show();
	}
	//标签模板	
	var labelTemp = "<section><input type='text' /><span class='delLabelBtn'>x</span></section>";
	var labelInputGruop = $("#iInputGruop");
	var form = $("#iForm");
	var height = isMinWidth ? 150 : 600;
	CKEDITOR.replace( 'editor' , {
 	 filebrowserImageUploadUrl: "imgupload",
 	 language : 'zh-cn',
 	 height: height
	});
     
     $("#iBtn").on("click",function(){
    	 submit(function(data){
    		 if(data.code == 200){
				 $.showToast("发布成功",function(){
    				 location.href = "index";
    			 }) 
			 }else{
				 $.showToast(data.msg);
			 }
    	 });
     })
     
     $("#iAddLabelBtn").on("click",function(){
    	 //需要append到页面之后再聚焦，否则无效
    	 createTag(function(tag){
    		 tag.find("input").focus();
    	 })
    	
     })
     
     function createTag(callback){
    	 var tag = $(labelTemp);
    	 labelInputGruop.append(tag).find("input").attr("focus",false);
    	 typeof callback === "function" && callback(tag);
     }
     
     labelInputGruop.on("click" , ".delLabelBtn" , function(){
    	 $(this).parent("section").remove();
     })
     
     var canClick = true;
     
     /**
      * 
      * @param arg1 操作或回调
      * @param arg2 回调
      */
     function submit(arg1,arg2){
    	 if(!canClick) {
    		 return false;
    	 }
    	 var callback,op;
    	 if(typeof arguments[0] === "function"){
    		 callback = arguments[0];
    	 }else if(typeof arguments[0] === "string"){
    		 op = arguments[0];
    	 }
    	 if(typeof arguments[1] === "function"){
    		 callback = arguments[1];
    	 }
    	 canClick = false;//防止多次点击，提交多次
    	 var datas = getDatas();
    	 if(!op && !submitVertify(datas.title , datas.content , datas.labels)){
    		 canClick = true;
    		 return false;
    	 }else{
    		 if(!datas.content) {
    			 $.showToast("草稿内容不能为空");
    			 canClick = true;
        		 return false;
    		 }
    	 }
    	 if(op){
    		 datas.op = "draft";
    	 }
    	 if(blogId){
    		 datas.id = blogId;
    	 }
    	 var loading = $.showloading(op ? "正在保存" : "正在发布")
    	 $.ajax({
    		 type: 'post',
    		 url: 'submitblog',
    		 data:datas,
    		 success: function(data){
    			typeof callback === "function" && callback(data);
    		 },
    		 fail: function(e){
    			 $.showToast(e);
    		 },
    		 complete:function(){
    			 canClick = true;
    		 }
    	 })
     }
     
     function getDatas(){
    	 var title = form.find("input[name=title]").val();
    	 var content = CKEDITOR.instances.editor.getData();
    	 var labels = getLabels();
    	 return {
    		 title: title,
    		 content: content,
    		 labels: labels ? labels.join(",") : null
    	 }
     }
     
     function getLabels(){
    	 var inputs = $("#iInputGruop").find("input");
    	 if(!inputs.length){
    		 return null;
    	 }
    	 var sb = [];
    	 for(let i=0;i<inputs.length;i++){
    		 var  input = inputs.eq(i);
    		 if(!input.val()){
    			 continue;
    		 }
    		 sb.push(input.val());
    	 }
    	 return sb;
     }
     
     function submitVertify(title , content , labels){
    	 if(!title || !title.length){
    		 $.showToast("标题不能为空")
    		 return false;
    	 }
    	 if(!content || !content.length){
    		 $.showToast("内容不能为空");
    		 return false;
    	 }
    	 if(!labels || !labels.length){
    		 $.showToast("标签不能为空");
    		 return false;
    	 }
    	 return true;
     }
     
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
 	
     
     $(".user").on("click",function(e){
    	e.stopPropagation(); //禁止冒泡
 		var _this = $(this);
 		var id = _this.attr("data-id");
 		var url = getSsoServiceUrl("userinfo");
 		location.href = url+"?id="+id;
 	})
 	
     $("#iHitsFrame").on("click",".hitsForbit",function(){
    	 $.Storage.set(HITS_CONTROL,true);
    	 hits.hide();
     }).on("click",".hitsHide",function(){
    	  hits.hide();
     })
 	
     $("#iPre").on("click",function(){
    	 var datas = getDatas();
    	 if(!datas.content || !datas.content.length){
    		 $.showToast("无预览内容");
    		 return;
    	 }
    	 datas.labels = getLabels();
    	 preBlog.find(".title").html(datas.title ? datas.title : "无标题");
    	 var labs = ["<span>标签：</span>"];
    	 if(datas.labels){
    		 labs.push.apply(labs, datas.labels.map(function(item){
        		 return "<span>"+item+"</span>"
        	 }));
    	 }
    	 preBlog.find(".tags").html(labs.join(""));
    	 preBlog.find(".content").html(datas.content);
    	 preBlog.removeClass("hidden");
    	 preBlogPage.show();
     })
     
     $("#iSaveDraft").on("click",function(){
    	 submit("draft",function(data){
    		 if(data.code == 200){
				 $.showToast("保存成功");
				 blogId = data.data;
			 }else{
				 $.showToast(data.msg);
			 }
    	 });
     })
     
})()