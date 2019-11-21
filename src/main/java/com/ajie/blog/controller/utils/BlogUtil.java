package com.ajie.blog.controller.utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.ajie.chilli.utils.TimeUtil;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.dao.pojo.TbLabel;

/**
 * 博客辅助类
 *
 * @author niezhenjie
 *
 */
public class BlogUtil {
	public static SimpleDateFormat format = new SimpleDateFormat();
	/** 前面的0方便数组寻址 */
	public static String[] dates = { "", "昨天", "2天前", "3天前", "4天前", "5天前", "6天前", "7天前", "8天前",
			"9天前", "10天前" };
	/** 一小时的毫秒数 */
	public static final long ONEHOUR = 60 * 60 * 1000;
	/** 一天的毫秒数 */
	public static final long ONEDAY = 24 * ONEHOUR;

	/**
	 * 将标签字符串转换成list
	 * 
	 * @param labelstr
	 * @return
	 */
	public static List<String> parseLabel(String labelstr) {
		if (null == labelstr)
			return Collections.emptyList();
		try {
			List<TbLabel> list = JsonUtils.toList(labelstr, TbLabel.class);
			if (null == list)
				return Collections.emptyList();
		} catch (Exception e) {
		}
		return Collections.emptyList();
	}

	public static String handleDate(Date date) {
		Date now = new Date();
		long inteval = now.getTime() - date.getTime();
		if (inteval < ONEHOUR) {
			// 小于1小时 使用分钟，不足一分钟使用一分钟
			int min = (int) Math.ceil(inteval / 1000 / 60);
			if (min == 0)
				return "刚刚";
			return min + "分钟前";
		} else if (inteval < ONEHOUR * 20) {
			// 小于20小时，使用小时显示
			return Math.round(inteval / ONEHOUR) + "小时前";
		} else if (inteval < ONEDAY * 10) {// 大于20个小时，小于10天
			if (TimeUtil.isSameDay(date, now)) {
				// 同一天，仍然显示小时
				return Math.round(inteval / ONEHOUR) + "小时前";
			}
			// 十日以内，显示天数前
			int day = TimeUtil.inteval(date, now);
			return dates[day];
		}
		return parseDate(date, "MM-dd");
	}

	/**
	 * 格式化日期
	 * 
	 * @param date
	 * @param pattern
	 * @return
	 */
	public static String parseDate(Date date, String pattern) {
		if (null == date || null == pattern)
			return "";
		synchronized (format) {
			try {
				format = new SimpleDateFormat(pattern);
				return format.format(date);
			} catch (Exception e) {
			}
		}
		return "";
	}

}
