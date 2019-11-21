package com.ajie.blog.test;

import java.io.IOException;

import com.ajie.chilli.utils.HttpClientUtil;

/**
 * 测试站点高并发时服务器的承载
 *
 * @author niezhenjie
 *
 */
public class HighConcurrencyTest implements Runnable {

	public static void main(String[] args) {
		HighConcurrencyTest test = new HighConcurrencyTest();
		for (int i = 0; i < 500; i++) {
			new Thread(test).start();
		}
	}

	public void connect() {
		try {
			HttpClientUtil.doGet("http://www.ajie18.top/blog/index.do");
			System.out.println("success!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		connect();
	}
}
