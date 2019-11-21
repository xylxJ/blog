/**
 * 
 *  夜间模式支持
 * 
 * 
 * 依赖dark-mode-suport.css
 * 
 * 调用 $.toggleDark(bool)
 * 
 * 页面需要切换至夜间模式的节点需添加darkMode样式
 * 
 * 如果需要边框生效，要响应添加对应的边框样式
 * 
 * @author niezhenjie
 */

(function(){
	//夜间模式cookid key
	var NIGHT_COOKIE_KEY = "night-mode";
	var NIGHT_COOKIE_VAL = "night";
	//true切换夜间，false切换白天
	function toggleDarkMode(bool) {
		var toggle = typeof bool === 'boolean' ? bool : undefined;
		if(undefined === toggle) { //切换
			var isDark = isDarkMode();
			isDark ? $(".darkMode").removeClass("darkModeActive") :$(".darkMode").addClass("darkModeActive");
			handlCookie(isDark);
		} else { //指定
			toggle ? $(".darkMode").addClass("darkModeActive") :$(".darkMode").removeClass("darkModeActive");
			handlCookie(!toggle);
		}
	}
	
	/**
	 * 处理缓存
	 * 
	 * @param isDel true 删除缓存 false添加缓存
	 */
	function handlCookie(isDel) {
		if(isDel) {
			//删除cookie
			$.Cookie.remove(NIGHT_COOKIE_KEY);
		} else {
			//添加
			$.Cookie.set(NIGHT_COOKIE_KEY,NIGHT_COOKIE_VAL);
		}
	}
	
	/**
	 * 检查是否为夜间模式
	 * 
	 * @returns {Boolean}
	 */
	function isDarkMode(){
		var nightMode = $.Cookie.get(NIGHT_COOKIE_KEY);
		if($.isEmptyObject(nightMode)){
			return false;
		}
		return !!nightMode;
	}
	$.extend($,{
		toggleDarkMode: toggleDarkMode,
		isDarkMode: isDarkMode
	})
})()
