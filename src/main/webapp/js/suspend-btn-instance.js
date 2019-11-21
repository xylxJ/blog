/**
 * 博客系统悬浮菜单，引入文件即可使用，避免每个页面都要编写
 * 
 * @param window
 * @param $
 */
(function(window,$){
	var dayIcon = {
		url:'http://www.nzjie.cn/static/images/day3.jpg',
		text: "日间模式",
		css: {},
		callback:function(panel) {
			if(typeof $.toggleDarkMode !== "function"){ //并不支持日间夜间模式
				return;
			}
			$.toggleDarkMode(false);
			//toggleDarkMode(false);
			panel.setIcon(darkIcon , 3);
			panel.hidePanel();
		}
	}
	var darkIcon = {
		url:'http://www.nzjie.cn/static/images/dark.jpg',
		text: "夜间模式",
		css: {},
		callback:function(panel) {
			if(typeof $.toggleDarkMode !== "function"){ //并不支持日间夜间模式
				return;
			}
			$.toggleDarkMode(true);
			//toggleDarkMode(true);
			panel.setIcon(dayIcon , 3);
			panel.hidePanel();
		}
	}
	var icon = $.isDarkMode() ? dayIcon  : darkIcon;
	//悬浮菜单
	var icons = [{
		url:'http://www.nzjie.cn/static/images/fresh.jpg',
		text: "刷新",
		css: {},
		callback:function() {
			var panel = arguments[0];
			location.reload();
		}
	},{
		url:'http://www.nzjie.cn/static/images/gotoTop.jpg',
		text: "顶部",
		css: {},
		callback:function(panel) {
			$('html,body').animate({scrollTop:0},'fast');
		}
	},{
		url:'http://www.nzjie.cn/static/images/logging.jpg',
		text: "日志",
		css: {},
		callback:function() {
			//有日志记录的日志，打开日志函数名必须为pageLog
			typeof pageLog === "function" && pageLog();
		}
	},icon
	,{
		url:'http://www.nzjie.cn/static/images/manager.jpg',
		text: "后台",
		css: {},
		callback:function(panel){
			location.href = "manager.do";
		}
	},{
		url:'http://www.nzjie.cn/static/images/wxapp.jpg',
		text: "小程序",
		css: {},
		callback:function(panel){
			var host = "http://"+location.host +"/blog/"+serverId+"/images/";
			var url = host+"my_wxapp_code_shuiyin.jpg";
			//全屏查看图片
			wx.previewImage({
				current: url, // 当前显示图片的http链接
				urls: [url] // 需要预览的图片http链接列表
			});
		}
	},]
	var options = {
		tapHide: true,
		icons: icons,
		transTimeout: 15000//15秒
	}
	$.createSuspendBtn(options);
})(window,jQuery)