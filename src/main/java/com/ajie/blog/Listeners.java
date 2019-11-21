package com.ajie.blog;

/**
 * 监听器集
 * 
 * @author niezhenjie
 */
public interface Listeners {

	boolean unregister();
	
	//<T> void register(<T extends Listener> listener);
}
