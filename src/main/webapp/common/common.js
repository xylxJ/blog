/**
 *
 * jquery扩展插件
 *
 *
 *
 *
 * @autor niezhenjie
 *
 */
(function(){
	//body需要设置html和body height为100%；否则页面大于可是界面body的高度是页面的总高度
	var BODY = $(document.body);
	var DOC = $(document);
	var WIN = $(window);
	/**空对象*/
	var EMPTY = {}; 
	/**本地储存*/
    var _storage = localStorage;
     //代理字符串
    var userAgent = navigator.userAgent.toLowerCase();
      // 移动端常见的浏览器类型判断
    var BROWSER = {
        // ie10及以下版本的判断
        ie: !!DOC.all,
        android: userAgent.indexOf("android") > -1 || userAgent.indexOf("linux") > -1,
        iphone: userAgent.indexOf("iphone") > -1,
        mobile: userAgent.indexOf("mobile") > -1,
        ipad: userAgent.indexOf("ipad") > -1,
        //是否在微信浏览器环境中
        weixin: userAgent.indexOf("micromessenger") > -1
    };

    /**过滤空格*/
    var MATCH_TRIM = /(^\s*)|(\s*$)/g;
     //手机号匹配规则
    var MATCH_PHONE = /^[+]?\d{8,}$/g;
    //邮箱匹配规则
    var MATCH_MAIL = /^([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+@([a-zA-Z0-9]+[_|\_|\.]?)*[a-zA-Z0-9]+\.[a-zA-Z]{2,3}$/;
    //过滤特殊字符正则表达式
    var REG_SYMB = new RegExp("[`~!@＠%Y#$^&*()=|{}':;',\\[\\].<>/?~！#￥¥……&*（）&;—|{}【】‘；：”“'。，、？]");
    var base64EncodeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    var base64DecodeChars = new Array(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1);
	function showmsg(msg,delay,callback){
		showmsgExt({
			msg: msg,
			delay: delay || 1000,
			icon: 'none',
			callback: callback
		})
	}

	function showloading(msg,delay,callback){
		return new showmsgExt({
			msg: msg,
			delay: delay ||0,
			icon: 'loading',
			callback: callback
		})
	}


	/**交互提示框，注意，每一时刻只能显示一种，如果同时出现两种，则第一种会隐藏，如loading中需要显示showToast
	* 则loading自动隐藏
	*/
	function showmsgExt(options){
	var opts = $.extend({
		msg: '',//显示的内容
		delay: 0,//消失时间，默认不消失
		icon: 'none',//图标，支持 succ loading warming
		callback: null,//隐藏回调，show一般需要手动调用，可以在调用的时候传入
	},options)
	var instance = $.MsgInstance;
	if(instance){ //当已经有显示框了，先把它隐藏了，在构造新的
		instance.hide();
	}
	/*var mark = $("<div>").addClass("show-msg-mark");
	mark.appendTo("BODY");*/
	var modal = $("<div/>").addClass("show-msg-modal");
	modal.appendTo(BODY);
	var dialog = $("<div/>").addClass("show-msg-dialog");
	dialog.addClass("modal-dialog-show").appendTo(BODY);
	var content = $("<div/>").html(opts.msg);
	content.appendTo(dialog);
	if('none' != opts.icon){
		var width = DOC.width() * 0.3; //窗口的30% 最大200px
		width = width > 200 ? 200 : width
		var height = width;
		dialog.css({
			width: width,
			height:height
		})
		adjustCenter(dialog,width,height);
		var icon = $("<div/>").addClass("show-msg-loading-icon").appendTo(dialog);
		if(opts.icon == 'loading'){
			var iconHeight = height *0.7;
			icon.css({height: iconHeight});
			var itemHeight = iconHeight * 0.13;
			for(let i=0;i<12;i++){
				var delay = i*(1/12)-1; //因为动画的执行时间是1s，所以第一个应该在-1秒的时候就执行，不然的话会等待一秒后才执行
				icon.append($("<div/>").addClass("loading-item").css({
					'animation-delay':+delay+'s',
					'-webkit-animation-delay':+delay+'s', /* Safari 和 Chrome */
					'transform': 'rotate('+ i * 30 + 'deg) translate(0, -142%)',
					'-webkit-transform': 'rotate(' + i * 30 + 'deg) translate(0, -142%)',
					"height":itemHeight,
					"top": (itemHeight*4)+"px",
				}));
			}
			var contentPosi = (height - iconHeight ) / 2;
			content.css({
				bottom: contentPosi+"px"
			}).addClass("content-text-icon")
		}
	}else{
		//showToast情况
		content.addClass("content-text")
		dialog.css({
			width:"normal",
			height:"normal"
		})
		dialog.css({
			"max-width": "80%"
		})
		adjustCenter(dialog);
	}
	
	var delay = opts.delay;
	if(delay > 0){
		setTimeout(function(){
			hide();
		},delay)
	}
	var callbacks = options.callbacks ||{};

	/**添加action（如hide）动作的回调 */
	this.addCallback = function(action,fn){
		callbacks[action] = fn;
	}

	function hide(callback){
		if(!dialog)
			return;
		dialog.removeClass("modal-dialog-show");
		dialog.addClass("modal-dialog-hide");
		setTimeout(function(){
			dialog && dialog.hide();
			modal && modal.hide();
			destory();
			$.MsgInstance = null;//消除实例
			//先判断hide调用有没有传入callback,没有则检查callbacks，也没有则检查构造时传入的
			(typeof callback=== "function" && callback()) || typeof callbacks["hide"] === "function" && callbacks["hide"]() || typeof opts.callback ==='function' && opts.callback();
			
		},200)
	}

	function show(callback){
		dialog.addClass("modal-dialog-show")
		modal.show();
		dialog.show();
		//先判断hide调用有没有传入callback,没有则检查callbacks，也没有则检查构造时传入的
			(typeof callback=== "function" && callback()) || typeof callbacks["hide"] === "function" && callbacks["hide"]();
	}

	function destory(){
		dialog && dialog.remove();
		modal && modal.remove();
		content && content.remove();
		dialog = null;
		modal = null;
		content = null;
		$.MsgInstance = null;
		typeof callbacks["destory"] === "function" && callbacks["destory"]();
	}


	this.getModal = function(){
		return modal;
	}

	this.show = function(callback){
		show(callback);
	}

	this.hide = function(callback){
		hide(callback);
	}
	this.destory = function(){
		destory();
	}
	//$.MsgType = opts.icon == 'none' ? 'loading' : opts.icon;
	$.MsgInstance = this; //保存一下实例，当下一个实例进来时，判断当前是否已经存在实例，存在则先销毁
}

/**
 *
 * 将绝对定位元素ele设置为中间显示
 *
 */
 function adjustCenter(ele,width,height){
 	var elem = $(ele);
 	width = width || elem.width();
 	height = height || elem.height();
 	var docWidth = WIN.width();
 	var docHeight = WIN.height();
 	var left =(docWidth - width)/2;
 	var top = (docHeight - height)/2;
 	elem.css({
 		left: left,
 		top: top
 	})
 }

/**
 * 弹窗，为了体验更好，用作弹窗的元素应该指定显示为none，否则在构造前会显示出来，不雅观
 * 
 * @param ele 弹出个内容元素
 */
 function WindowPlugin(ele) {
 	var content = $(ele);
 	//因为元素可能再次之前已经隐藏了（隐藏也有宽高，隐藏只是不显示，不是宽高为0），所以需要获取一下他的宽高，不重新指定宽高显示出来并不会是初识时的宽高
 	var contentWidth = content.width();
 	var contentHeight = content.height();
 	content.css({
 		width: contentWidth,
 		contentHeight:contentHeight
 	})
 	//可能没有隐藏，手动隐藏content,不然页面显示出来的时间太长，不雅观
 	content.hide();
 	var win = $(window);
 	var plugin = this;
 	var mask = $("<div/>").addClass("window-plugin-background");
 	var dialog = $("<div/>").addClass("window-plugin-dialog");
 	var closer = null;
 	content.appendTo(dialog).show();//content上面隐藏了
	var clickbackhide = true; //点击背景关闭 默认关闭
	var callbackafterclose; //关闭后回调
	this.setCallbackafterclose = function (callbackafterclose) {
		this.callbackafterclose = callbackafterclose;
	}
	this.clickbackhide = function() {
		clickbackhide = true;
		if(clickbackhide) {
			mask.bind("click" , function() {
				mask.attr("title" , "关闭");
				plugin.hide();
			});
		}
	}
	this.show = function(callback) {
		mask.show();
		dialog.show();
		mask.removeClass("modal-background-hide");
		dialog.removeClass("modal-dialog-hide");
		mask.addClass("modal-background-show");
		dialog.addClass("modal-dialog-show");	
		center();
		var timer = setTimeout(function() {
			typeof callback === "function" && callback();
			clearTimeout(timer);
		},200)
	}
	this.hide = function(callback) {
		mask.removeClass("modal-background-show");
		dialog.removeClass("modal-dialog-show");
		mask.addClass("modal-background-hide");
		dialog.addClass("modal-dialog-hide");
		setTimeout(function() {
			hideWindow(callback);
		} , 500)
	}
	this.setCloser = function(bool) { //显示||隐藏X关闭图标，默认是不显示的
		var b = typeof bool ==='boolean' ? bool :  true;
		if(!closer){
				closer = $("<span/>").addClass("window-plugin-closer").attr("title", "关闭");
				closer.bind("click" , function() {
					plugin.hide();
				})
			}
		if(b) {
			closer.show();
		} else {
			closer.hide();
		}
	}
	this.center = function(){
		center();
	}

	this.destory = function(){
		dialog && dialog.remove();
		mask && mask.remove();
	}
	
	if(clickbackhide) {
		mask.bind("click" , function() {
			mask.attr("title" , "关闭");
			plugin.hide();
		});
	}
	
	function hideWindow(callback) {
		mask.hide();
		dialog.hide();
		if(typeof callback ==='function') {
			callback();
		}
		 //关闭后回调，用于不直接在外部调用hide(callback)函数关闭窗体时的回调（如点击背景modal关闭 ， 点击右上角X关闭）
		 if(callbackafterclose){
		 	if(typeof callbackafterclose ==='function'){
		 		callbackafterclose();
		 	}
		 }
		}
	function center(){ //使居中
		var height = WIN.height();
		var width = WIN.width();
		var dialogHeight = dialog.height();
		var dialogWidth = dialog.width();
		var top , left;
		if(dialogHeight > height){
			top = 0;
		}else{
			top = (height - dialogHeight) / 2;
		}
		if(dialogWidth > width){
			left = 0;
		}else{
			left = (width - dialogWidth) / 2;
		}

		dialog.css({
			top: top+"px",
			left: left+"px"
		})
	}
	win.bind("resize" , function() {
		if(dialog.is(":visible")) {
			center();
			mask.css({
				height: win.height(),
				width: win.width()
			})
		}
	});
	mask.appendTo(BODY);
	dialog.appendTo(BODY);
}
 
 /**
  * 侧边滑出全屏窗口
  * @param options
  */
 function SlideWindow(options){
 	const timeout = 300;//过渡时间，要与css动画相同
	var opts = $.extend({
	 	 ele: null,//元素
		 direction: "right", //方向，right从右边滑出，left从从边滑出,top从上面滑下，bottom从底部
		 callback: null,//滑出后回调
		 hidecallback: null,//收起后回调
		 zIndex: -1,//-1表示不设置，由css控制
		 title: "",//滑出后title显示内容
	 },options)
	 var ele = opts.ele;
	 ele.hide();//如果页面没隐藏，先隐藏，否则加载慢时会显示出来
	 var mask = $("<div>").addClass("slide-win-mask");
	 var dialog = $("<div>").addClass("slide-win-dialog").append(ele);
	 ele.show();//上面隐藏了
	 if(opts.zIndex >=0 ){
	 	dialog.css({"z-index":opts.zIndex})
	 }
	 var direction = opts.direction;
	 var id = opts.ele.attr("id");
	 var anchor = null;
	  if(id){
	  	anchor = hash(id);
	  }else{
		anchor = new Date().getTime();
	  }

	  trans("hide");

	 /**
	  *
	  * action show为打开，hide为收起
	  *
	  */
	 function trans(action){
	 	var direction = opts.direction;
	 	if("right" === direction || "left" === direction){
	 		var dir = "right" === direction ? 1 :-1;
	 		var x = action === "show" ? 0 : 100 * dir;
	 		dialog.css({
	 			transform: "translateX("+x+"%)"
	 		})
	 	}else if("top" === direction || "bottom" === direction){
	 		var dir = "bottom" === direction ? 1 : -1;
	 		var y = action === "show" ? 0 : 100 * dir;
	 		dialog.css({
	 			transform: "translateY("+y+"%)"
	 		})
	 	}
	 }

	 function show(title , callback){
	 	//后退
	 	window.history.pushState(null, null, "#"+anchor);
	 	mask.removeClass("slide-win-mask-hide").addClass("slide-win-mask-show").show();
	 	//禁止页面滚动时下面的也跟着滚动
 		BODY.addClass("disable-scroll");
 		$("html").addClass("disable-scroll")
	 	trans("show");
	 	var oldTitle = DOC[0].title;
	 	opts.oldTitle = oldTitle;
	 	var tt = title || opts.title;
	 	if(tt && tt.length){
			DOC[0].title = tt;
	 	}
	 	var timing = setTimeout(function(){
	 		typeof callback === "function" && callback() || typeof opts.callback === "function" && opts.callback();
	 		mask.hide();
	 		//删除transform，防止绝对定位失效（transform会使定位失效，具体原因可百度）；或者可改成使用动画
	 		dialog.css("transform","");
	 		clearTimeout(timing);
	 	},timeout);
	 }

	 window.onpopstate = function() {
		hide();//监听后退
	}

	 function hide(callback){
	 	mask.removeClass("slide-win-mask-show").addClass("slide-win-mask-hide").show();
		//解除禁止滑动
 		BODY.removeClass("disable-scroll");
 		$("html").removeClass("disable-scroll")
	 	trans("hide");
	 	DOC[0].title = opts.oldTitle;
	 	var timing = setTimeout(function(){
	 		mask.removeClass("slide-win-mask-show").addClass("slide-win-mask-hide").hide();
	 		typeof callback === "function" && callback() ||typeof opts.hidecallback === "function" && opts.hidecallback();
	 		clearTimeout(timing);
	 	},timeout);
	 }

	 this.show = function(title,callback){
	 	show(title,callback);
	 };
	 this.hide = function(callback){
	 	hide(callback);
	 }
	 mask.appendTo(BODY);
	 dialog.appendTo(BODY);
  }

  function FunnelLoading(){
  	
  }

 /**
  * base64编码
  * @param {Object} str
  */
 function base64encode(str) {
     var out, i, len;
     var c1, c2, c3;
     len = str.length;
     i = 0;
     out = EMPTY;
     while (i < len) {
         c1 = str.charCodeAt(i++) & 255;
         if (i == len) {
             out += base64EncodeChars.charAt(c1 >> 2);
             out += base64EncodeChars.charAt((c1 & 3) << 4);
             out += "==";
             break;
         }
         c2 = str.charCodeAt(i++);
         if (i == len) {
             out += base64EncodeChars.charAt(c1 >> 2);
             out += base64EncodeChars.charAt((c1 & 3) << 4 | (c2 & 240) >> 4);
             out += base64EncodeChars.charAt((c2 & 15) << 2);
             out += "=";
             break;
         }
         c3 = str.charCodeAt(i++);
         out += base64EncodeChars.charAt(c1 >> 2);
         out += base64EncodeChars.charAt((c1 & 3) << 4 | (c2 & 240) >> 4);
         out += base64EncodeChars.charAt((c2 & 15) << 2 | (c3 & 192) >> 6);
         out += base64EncodeChars.charAt(c3 & 63);
     }
     return out;
 }

 /**
  * base64解码
  * @param {Object} str
  */
 function base64decode(str) {
     var c1, c2, c3, c4;
     var i, len, out;
     len = str.length;
     i = 0;
     out = EMPTY;
     while (i < len) {
         /* c1 */
         do {
             c1 = base64DecodeChars[str.charCodeAt(i++) & 255];
         } while (i < len && c1 == -1);
         if (c1 == -1) break;
         /* c2 */
         do {
             c2 = base64DecodeChars[str.charCodeAt(i++) & 255];
         } while (i < len && c2 == -1);
         if (c2 == -1) break;
         out += String.fromCharCode(c1 << 2 | (c2 & 48) >> 4);
         /* c3 */
         do {
             c3 = str.charCodeAt(i++) & 255;
             if (c3 == 61) return out;
             c3 = base64DecodeChars[c3];
         } while (i < len && c3 == -1);
         if (c3 == -1) break;
         out += String.fromCharCode((c2 & 15) << 4 | (c3 & 60) >> 2);
         /* c4 */
         do {
             c4 = str.charCodeAt(i++) & 255;
             if (c4 == 61) return out;
             c4 = base64DecodeChars[c4];
         } while (i < len && c4 == -1);
         if (c4 == -1) break;
         out += String.fromCharCode((c3 & 3) << 6 | c4);
     }
     return out;
 }

 function hash(str){
	var h = 0;
	var len = str.length;
	var t = 2147483648;
	for (var i = 0; i < len; i++) {
		h = 31 * h + str.charCodeAt(i);
		if (h > 2147483647){
			h %= t; //java int溢出则取模
		} 
	}
	return h;
 }

	//扩展jquery
	$.extend($,{
		
		/**
		 * Base64工具
		 */
		
		Base64: {
			/**
			 * 编码
			 */
			encode: function(str){
				return base64encode(str);
			},
			
			/**
			 * 解码
			 */
			decode:function(str){
				return base64decode(str);
			}
			
		},
		/**
	     * Cookie操作
	     */
	    Cookie: {
	        /**
	         * 保存cookie
	         * 例如：
	         *        $.Cookie.set("key","value");//不指定过期时间
	         *        $.Cookie.set("key","value",1000*60*60);//指定过期时间【毫秒】
	         */
	        set: function (key, value, expiresMillis, path) {
	            var ck = key + "=" + escape(value);
	            // 判断是否设置过期时间
	            var date = new Date();
	            if (expiresMillis) {
	                date.setTime(date.getTime() + expiresMillis);
	            } else {
	                date.setFullYear(date.getFullYear() + 1);
	            }
	            ck = ck + ";expires=" + date.toGMTString() + ";path=" + (path || "");
	            DOC[0].cookie = ck;
	        },
	        /**
	         * 获取保存的cookie
	         * 例如：$.Cookie.get("key");
	         */
	        get: function (key) {
	            var array = DOC[0].cookie.split(";");
	            for (var i = 0; i < array.length; i++) {
	                var arr = array[i].split("=");
	                if (arr[0].trim() == key) {
	                    return unescape(arr[1]);
	                }
	            }
	            return EMPTY;
	        },

	        /**
	         * 移除cookie
	         * 例如：$.Cookie.remove("key","path=/");
	         * key:键
	         * path:路径【可选】
	         */
	        remove: function (key, path) {
	            var date = new Date();
	            date.setDate(date.getDate() - 1);
	            DOC[0].cookie = key + "=;expires=" + date.toGMTString() + ";path=" + (path || "");
	        },
	        /**
	         * 删除所有的cookie
	         * 例如Query.Cookie.clear();
	         * path:路径【可选】
	         */
	        clear: function (path) {
	            var date = new Date();
	            date.setDate(date.getDate() - 1);
	            var ex = date.toGMTString();
	            var keys = DOC[0].cookie.match(/[^ =;]+(?=\=)/g);
	            $.each(keys, function (key) {
	                var ck = key + "=;expires=" + ex;
	                if (path) {
	                    ck += ";path=" + (path || "");
	                }
	                DOC[0].cookie = ck;
	            });
	        }
	    },
	    /**
	     * HTML5本地储存，用法同Cookie操作
	     * 区别：
	     *    Cookie只能储存最多4k的内容，Storage：2M
	     * Cookie可以在后台获取到，Storage不能 
	     * FIXME 使用base64格式化后存储
	     */
	    Storage: {
	        get: function (key) {
	           /* var data = _storage.getItem($.Base64.encode(key));
	            return data ? $.Base64.decode(data) : EMPTY;*/
	        	 var data = _storage.getItem(key);
		         return data ? data : EMPTY;
	        },
	        set: function (key, value) {
	            _storage.setItem(key, value);
	        },
	        remove: function (key) {
	            _storage.removeItem(key);
	        },
	        clear: function () {
	            _storage.clear();
	        }
	    },
	  //判断是否支持无前缀型css3
		supportcss3:function(){
			var bool = false;
			if(null ===CSS3_SUPPORTED){
				var support = $("<i/>");
				bool = support[0].style.animation==="";
				support.remove();
			}
			return bool;
		},

		//判断是否为指定浏览器
		isBrowser:function(browser){
			return !!BROWSER[browser]
		},

		/**转小写*/
		lowerCase: function(src){
			if(!src || !src.length){
				return "";
			}
			return src.toLocaleLowerCase();
		},
		
		showToast: function(msg,delay,callback){
			var arg = arguments[1];
			//如果第二个参数是函数，则第二个参数就是回调，delay给一个默认值
			 if(typeof arg === 'function'){
			 	callback = arg;
			 	delay = 1000;
			 }
			showmsg(msg,delay,callback);
		},
		showloading: function(msg,delay,callback){
			return showloading(msg,delay,callback);
		},

		/**计算字符串的hash值*/
		hash: function(str){
			return hash(str);
		},


		
		/**str是否符合手机号码格式*/
		isMobile: function (str) {
			str=$.trim(str);
			return str && str.length == 11 && !!str.match(MATCH_PHONE);
		},

		/*
		 * 判断字串是否为邮箱
		 * str:string
		 * return 时手机号返回true，否则返回false
		 */
		isMail: function (str) {
			return !!$.trim(str).match(MATCH_MAIL);
		},
		  /**字符串中是否包含特殊字符*/
		 hasSymbol: function (str) {
			return !!$.trim(str).match(REG_SYMB);
		},
	})
	
	$.extend($.fn , {
			getWindow: function(){
				return new WindowPlugin(this);
			},
			getSlideWindow:function(options){
				var _this = $(this);
				options.ele = _this;
				return new SlideWindow(options)
			},
	})
})()
