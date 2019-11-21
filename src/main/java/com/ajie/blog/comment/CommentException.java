package com.ajie.blog.comment;

/**
 *
 *
 * @author niezhenjie
 *
 */
public class CommentException extends Exception {

	private static final long serialVersionUID = 1L;

	public CommentException() {
		super();
	}

	public CommentException(String message) {
		super(message);
	}

	public CommentException(Throwable e) {
		super(e);
	}

	public CommentException(String message, Throwable e) {
		super(message, e);
	}

}
