package com.ajie.blog.blog.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajie.blog.blog.BlogListener;
import com.ajie.blog.blog.BlogListeners;
import com.ajie.blog.blog.BlogService;
import com.ajie.dao.pojo.TbBlog;
import com.ajie.push.Message.MessageBuilder;
import com.ajie.push.PushProducer;
import com.ajie.push.impl.SimpleMessage;
import com.ajie.push.vo.MessageVo;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * 博客监听器
 * 
 * @author ajie
 *
 */
public class BlogWatcher implements BlogListener {

	private static final Logger logger = LoggerFactory
			.getLogger(BlogWatcher.class);

	private BlogService blogService;

	private PushProducer pushProducer;

	public void setBlogService(BlogService service) {
		blogService = service;
		if (null == blogService) {
			return;
		}
		((BlogListeners) blogService).register(this);
	}

	public void setPushProducer(PushProducer pushProducer) {
		this.pushProducer = pushProducer;
	}

	@Override
	public void onCreate(TbBlog blog) {
		if (null == pushProducer) {
			return;
		}
		MessageVo msg = new MessageVo();
		msg.setContent(Collections.singletonList(blog));
		msg.setDestination("blog-test");
		pushProducer.send(new SimpleMessage(msg));
		MessageVo msg2 = new MessageVo();
		msg2.setContent("通知用户系统");
		msg2.setDestination("sso-test");
		pushProducer.send(new SimpleMessage(msg2));
	}

	@Override
	public void onDelete(TbBlog blog) {
		if (null == pushProducer) {
			return;
		}
		MessageBuilder builder = MessageBuilder.getMessageBuilder();
		builder.setBiz(BIZ_DELETE).setDestination(MESSAGE_PUSH_NAME)
				.setReference(blog.getUserid() + "").setContent(blog);
		pushProducer.send(builder.build());
	}

	@Override
	public void onUpdate(TbBlog blog) {
		logger.info("监听到更新博客");
	}

}
