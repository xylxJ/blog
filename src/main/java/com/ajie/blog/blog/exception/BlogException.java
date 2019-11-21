package com.ajie.blog.blog.exception;

/**
 * @author niezhenjie
 */
public class BlogException extends Exception {

	private static final long serialVersionUID = 1L;

	public BlogException() {
		super();
	}

	public BlogException(String message) {
		super(message);
	}

	public BlogException(Throwable e) {
		super(e);
	}

	public BlogException(String message, Throwable e) {
		super(message, e);
	}
}
