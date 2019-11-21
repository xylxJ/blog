/**
  * 
  *
  *
  * 仿ios悬浮按钮点击菜单
  *
  *  demo:
  *   	var btn = new $.SuspendBtn(options);
  *		其中options见构造说明
  *
  *
  *
  * @author	niezhenjie
  *
  *
  */
  (function(window , $ , undefined){
  	var BODY = $(document.body);
  	var DOC = $(document);
  	var WIN = $(window);
  	var WINWIDTH = WIN.width();
  	var WINHEIGHT = WIN.height();
  
	/**
	 *	构造参数说明：
 		@param options {
 			buttonText: string default 更多 //悬浮按钮文字
 			menuWidth: number defalut 55 //悬浮按钮的宽高
 			transTimeout: number default 5000 悬浮按钮变透明的时间
 			tapHide: bool	default false 点击悬浮框是否收起
 			initTop: number, 初始位置距离顶部的距离，需要带单位 可以使用%
 			initDirect: "-1 || 1" defalut 1 初始位置在左边还是右边 -1左，1右
 			icons:[{ 悬浮框内容
				url:'./images/fresh.jpg',
				text: "刷新",
				css: {},
				callback:function(panel){
				panel.hide();
				alert("刷新");
			}
			},{
				url:'./images/fresh.jpg',
				text: "顶部",
				css: {},
				callback:function(){
			alert("顶部");
			}
		}]
 		}

 		*一般来说，只要传icons就可以了，其他使用默认就好了
 		*/



	function SuspendBtn(options){
		const DIRECT_LEFT = 1;
		//菜单相对于面板的方向 -- 右
		const DIRECT_RIGHT = 1<<1;
		//菜单相对于面板的方向 -- 上
		const DIRECT_TOP = 1<<2;
		//菜单相对于面板的方向 -- 下
		const DIRECT_BOTTOM = 1<<3;
		//菜单相对于面板的方向 -- 中
		const DIRECT_CENTER = 1<<4;
		//菜单相对于面板的方向 -- 右上
		/* var DIRECT_RIGHT_TOP = 1<<4;
		//菜单相对于面板的方向 -- 右下
		var DIRECT_RIGHT_BOTTOM = 1<<5; */
		var panelWidth = WINWIDTH * 0.8 > 380 ? 380 : WINWIDTH * 0.8;//宽度为屏幕的80%
		var ICON_WIDTH = 60;//图标的宽高
		var itemWidth = panelWidth / 3;
		var itemHeight = panelWidth / 2;
		var menu,mask,panel;
		var opts = $.extend({
			buttonText:"更多",
			menuWidth: 55, //悬浮按钮的宽高
			transTimeout: 5000, //悬浮按钮变透明的时间 单位ms
			initTop: "60%",
			initDirect: 1,
			tapHide: false
		},options);

		/**
		 *
		 * 初始化
		 *
		 */
		 (function(){
		 	menu = $("<div>").addClass("operating").appendTo(BODY).css({
		 		width: opts.menuWidth,
		 		height: opts.menuWidth,
		 		top: opts.initTop,
		 		right: opts.initDirect == 1 ? "10px" : "unset",
		 		left: opts.initDirect == -1 ? "10px" : "unset"
		 	});
		 	$("<div>").addClass("operating-more").html(opts.buttonText).appendTo(menu);
		 	mask = $("<div>").addClass("operating-mask").addClass("hidden").appendTo(BODY)
		 	panel = $("<div>").addClass("operating-panel").appendTo(mask);
		 	panel.css({width: panelWidth,height: panelWidth});
		 	adjustPanelPosi();
		 	menuBgTimeout();
		 })()

		//面板根据菜单位置寻找位置
		function adjustPanelPosi(){
			var menupos = getMenuPosi();
			var width = getPanelWidth();
			var x = (getWinWidth() - width) / 2;
			//var y = menupos.top - (width / 2) + (menu.height()/2) ;
			//y也居中
			var y = (WINHEIGHT - width) / 2;
			panel.css({left: x,top: y});
		}

		function getPanelWidth(){
			return panelWidth;
		}
		
		function getWinWidth(){
			return WIN.width();
		}
		
		/**定时对menu的背景透明度进行过修改*/
		var timing;
		function menuBgTimeout(){
			timing = setTimeout(function(){
				menu.addClass("menu-bg-transparent")
				clearTimeout(timing);
			}, opts.transTimeout)
		}
		
		function clearBgTimeout(){
			menu.removeClass("menu-bg-transparent")
			if(timing){
				clearTimeout(timing);
			}
		}
		
		function freshBgTimeout(){
			clearBgTimeout();
			menuBgTimeout();
		}
		
		function getMenuPosi(){
			var offset = menu.position();
			var menuWidth = menu.width();
			var left = offset.left;
			var top = offset.top
			//相对于面板的方向
			var direction = 0;
			if((WIN.width()-menuWidth) / 2 > left) {
				direction |= DIRECT_LEFT ;
			}else{
				direction |= DIRECT_RIGHT;
			}
			var panelPosi = getPanelPosi();
			if(panelPosi.top >= (top + menuWidth)){
				direction |= DIRECT_TOP;
			}else if(panelPosi.top + panel.height() < top){
				direction |= DIRECT_BOTTOM;
			}
			return {
				left: left,
				top: top,
				direction: direction
			}
		}
		
		function getPanelPosi(){
			var width = getPanelWidth();
			var top = (WINHEIGHT - width) / 2;
			var left = (WINWIDTH - width) / 2;
			return {
				left: left,
				top: top,
			}
		}
		
		function showPanel(){
			adjustPanelPosi();
			var menuPosi = getMenuPosi();
			panel.removeClass("transform-origin-left").removeClass("transform-origin-right");
			panel.removeClass("transform-origin-left-top").removeClass("transform-origin-left-bottom");
			panel.removeClass("transform-origin-right-top").removeClass("transform-origin-right-bottom")
			if((menuPosi.direction & DIRECT_LEFT) == DIRECT_LEFT) {
				//在左边
				if((menuPosi.direction & DIRECT_TOP) == DIRECT_TOP){
					panel.addClass("transform-origin-left-top");
				}else if((menuPosi.direction & DIRECT_BOTTOM) == DIRECT_BOTTOM){
					panel.addClass("transform-origin-left-bottom");
				}else{
					panel.addClass("transform-origin-left");
				}
			}else if((menuPosi.direction & DIRECT_RIGHT) == DIRECT_RIGHT){
				//在右边
				if((menuPosi.direction & DIRECT_TOP) == DIRECT_TOP){
					panel.addClass("transform-origin-right-top");
				}else if((menuPosi.direction & DIRECT_BOTTOM) == DIRECT_BOTTOM){
					panel.addClass("transform-origin-right-bottom");
				}else{
					panel.addClass("transform-origin-right");
				}
			}
			panel.addClass("menu_dialog_show");
			mask.removeClass("hidden");
			menu.addClass("hidden");
		}	

		//图标和文字部分 
		//因为每个框里面的内容都不一样，所以用一个集合存起来，方便遍历时使用
		function getProps(){
			return  [{
				//1
				box:{
					top: 0,
					left: 0
				},
				img:{
					"padding-top": (itemHeight/3)+"px",
					right: 0,
				},
				textClass:"operating-text-left-top"
				
			},{
				//2
				box:{
					top: 0,
					left: itemWidth
				},
				img:{
					left: (itemWidth-ICON_WIDTH)/2
				},
				textClass:"operating-text-center-top"
				
			},{
				//3
				box:{
					top: 0,
					left: itemWidth*2
				},
				img:{
					"padding-top": (itemHeight/3)+"px",
					left: 0
				},
				textClass:"operating-text-right-top"
				
			},{
				//4
				box:{
					top: itemHeight,
					left: 0
				},
				img:{
					right: 0
				},
				textClass:"operating-text-left-bottom"
				
			},{
				//5
				box:{
					top: itemHeight,
					left: itemWidth
				},
				img:{
					"padding-top": (itemHeight/3)+"px",
					left: (itemWidth-ICON_WIDTH)/2
				},
				textClass:"operating-text-center-bottom"
				
			},{
				//6
				box:{
					top: itemHeight,
					left: itemWidth*2
				},
				img:{
					left: 0
				},
				textClass:"operating-text-right-bottom"
				
			}];
		};

		(function(){
			fillIcons();
		})();

		function fillIcons(){
			panel.html("");//先清空
			var props = getProps();
			//不管传多少个 ，都是6个
			for(let i=0;i<6;i++){
				var prop = props[i];
				/*var item = $("<div>").addClass("operating-item operatingTap").css({
					width: itemWidth,
					height: itemHeight
				}).css(prop.box).appendTo(panel);
				var icon = opts.icons[i];
				if(!icon){
					continue;
				}
				var img = $("<img>").addClass("operating-icon").attr("src",icon.url).appendTo(item).css(prop.img);
				var text = $("<div>").addClass("operating-text").addClass(prop.textClass).html(icon.text).appendTo(item);	*/
				var icon = opts.icons[i];
				var prop = props[i];
				var item = drawItem(icon,prop);
				item.appendTo(panel);
			}
		}
		
		function drawItem(icon,prop){
			var item = $("<div>").addClass("operating-item operatingTap").css({
				width: itemWidth,
				height: itemHeight
			}).css(prop.box);
			if(!icon){
				return item;
			}
			var img = $("<img>").addClass("operating-icon").attr("src",icon.url).appendTo(item).css(prop.img);
			var text = $("<div>").addClass("operating-text").addClass(prop.textClass).html(icon.text).appendTo(item);
			handleCallback(img,text,icon.callback);
			return item;
		}

		/**重新设置icons*/
		function setIcons(icons){
			opts.icons = icons;
			fillIcons();
		}

		/**改变第idx个icon*/
		function setIcon(icon,idx){
			var icons = opts.icons;
			icons[idx] = icon;//更新全局控制
			var item = panel.find(".operating-item").eq(idx);
			if(item){
				//一定要用remove，不能用empty，remove会把事件一并移除，而empty则不会，一定程度上empty会造成内存泄露
				item.remove();
			}
			var item = drawItem(icon,getProps()[idx]);
			if(idx == 0){
				panel.find(".operating-item").eq(1).before(item);
			}else{
				panel.find(".operating-item").eq(idx-1).after(item);
			}
			//setIcons(icons);
		}

		function handleCallback(img,text,callback){
			var _img = img[0];
			var _text = text[0];
			_img.addEventListener("touchstart",function(e){
				itemTapStart.call(this,e);
			},false)
			_img.addEventListener("touchend",function(e){
				itemTapEnd.call(this,e);
			},false)
			_text.addEventListener("touchstart",function(e){
				itemTapStart.call(this,e);
			},false)
			_text.addEventListener("touchend",function(e){
				itemTapEnd.call(this,e);
			},false)
			_img["bindCallback"] = callback;
			_text["bindCallback"] = callback;
		}
		function hidePanel(callback) {
			panel.removeClass("menu_dialog_show");
			panel.addClass("menu_dialog_hide");
			menu.removeClass("hidden");
			var timing;
			timing = setTimeout(function(){
				mask.addClass("hidden");
				panel.removeClass("menu_dialog_hide");
				freshBgTimeout();
				clearTimeout(timing);
				typeof callback === "function" && callback();
			},200)
		}	
		var startTime,endTime,startX,startY,endX,endY,curentX,curentY,startMenuX,startMenuY,menuWidth;
		menu.get(0).addEventListener("touchstart",function(event){
			var event = event || window.event;
			event.preventDefault();//禁止页面随着滚动
			clearBgTimeout();
			startTime = event.timeStamp;
			startX = event.changedTouches[0].clientX;
			startY = event.changedTouches[0].clientY;
			var menuPosi = getMenuPosi();
			startMenuX = menuPosi.left;
			startMenuY = menuPosi.top;
			menuWidth = menu.width();
		},false)
		menu.get(0).addEventListener("touchmove",function(event){
			var event = event || window.event;
			event.preventDefault();//禁止页面随着滚动
			currentX = event.changedTouches[0].clientX;
			currentY = event.changedTouches[0].clientY;
			var moveX = currentX - startX;
			var moveY = currentY - startY;
			var x = startMenuX + moveX;
			var y = startMenuY + moveY;
			if(x <= 0){
				x = 0;
			}
			if(x+menuWidth >= WINWIDTH){
				x = WINWIDTH - menuWidth;
			}
			/* if(x <= 0 || (x+menuWidth) >= WINWIDTH){
				return;
			} */
			if(y <= 0){
				y = 0;
			}
			if(y+menuWidth >= WINHEIGHT){
				y = WINHEIGHT - menuWidth
			}
			/* if(y <= 0 || (y+menuWidth) >= WINHEIGHT){
				return;
			} */
			menu.css({
				top: y+"px",
				left: x+"px"
			})
			freshBgTimeout();
		},false)
		menu.get(0).addEventListener("touchend",function(event){
			var event = event || window.event;
			event.preventDefault();//禁止页面随着滚动
			endTime = event.timeStamp;
			var interval = endTime - startTime;
			endX = event.changedTouches[0].clientX;
			endY = event.changedTouches[0].clientY;
			var moveX = endX - startX;
			var moveY = endY - startY;
			if(interval < 200 && moveX < 5 && moveY < 5){
				//小于200毫秒并且移动距离不超过5px，视为点击
				fireMenuTap(); 
				return;
			}
			var menuPosi = getMenuPosi();
			var x = 10;
			if((menuPosi.direction & DIRECT_RIGHT) == DIRECT_RIGHT){
				menu.css({
					left: "unset",
					right: "10px"
				})
			}else{
				menu.css({
					right: "unset",
					left: "10px"
				})
			}
		},false)		
		/**菜单点击*/
		function fireMenuTap(){
			showPanel()
		}

		mask[0].addEventListener("touchstart",function(e){
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
		})

		mask[0].addEventListener("touchend",function(e){
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
			hidePanel();
		})

		//如果不监听touchstart只监听end，则会反应很慢，甚至死机，这应该是touch的bug
		/*panel[0].addEventListener("touchstart",function(e){
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
		})
		panel[0].addEventListener("touchend",function(e){
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
				alert("aa")
		})*/

		var _this = this;
		function itemTapStart(e){
			var e = e || window.event;
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
		}

		function itemTapEnd(e){
			var e = e || window.event;
			e.preventDefault();
			e.stopPropagation(); //禁止冒泡
			if(opts.tapHide){
				hidePanel();
			}
			var fn = this["bindCallback"];
			typeof fn === "function" && fn(_this);
		}

		//暴露接口
		this.hidePanel = function(){
			hidePanel();
		}

		this.setIcons = function(){
			setIcons();
		}
		this.setIcon = function(icon,idx){
			setIcon(icon,idx)
		}

	}

	$.extend($,{
		createSuspendBtn: function(options){
			return new SuspendBtn(options);
		}
	})


})(window,jQuery,undefined)