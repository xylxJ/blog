package com.ajie.blog.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajie.api.ip.IpQueryApi;
import com.ajie.api.ip.IpQueryVo;
import com.ajie.chilli.remote.RemoteCmd;
import com.ajie.chilli.remote.exception.RemoteException;
import com.ajie.chilli.support.TimingTask;
import com.ajie.chilli.support.Worker;
import com.ajie.chilli.thread.ThreadPool;
import com.ajie.chilli.utils.TimeUtil;
import com.ajie.chilli.utils.common.JsonUtils;
import com.ajie.resource.ResourceService;
import com.ajie.web.RequestFilter;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * 对拦截器进行扩展，记录访问者并定时保存
 *
 * @author niezhenjie
 *
 */
public class RequestFilterExt extends RequestFilter implements Worker {
	private static final Logger logger = LoggerFactory
			.getLogger(RequestFilterExt.class);
	/** 命令类型--执行备份 */
	private static final int TYPE_BACKUP = 1;
	/** 命令类型 -- 执行回滚 */
	private static final int TYPE_ROLLBACK = 2;
	public static final String[] TABLE_HEADER = { "key\t", "count\t", "ip\t",
			"date\t", "address\r\n" };
	/** 访问记录 key是ip去除. */
	private Map<String, Access> accessRecord;
	/** 制表符ASCII码 \t */
	public static final byte HT = 9;
	/** 换行符ASCII码 \n */
	public static final byte LR = 10;
	/** 归位符ASCII码 \r */
	public static final byte CR = 13;
	/** 文本分界符 */
	public static final char BOUNDARY = HT;

	public static final String DEFAULT_CHARSET = "utf-8";
	/** 远程命令服务 */
	private RemoteCmd cmd;
	/** 资源服务 */
	private ResourceService resourceService;
	/** 访问记录的文件路径 */
	private String path;

	private ThreadPool threadPool;

	/** 线程池 */
	/*
	 * ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 50, 10,
	 * TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(50), new
	 * ThreadPoolExecutor.AbortPolicy());
	 */

	public RequestFilterExt() {
		accessRecord = new HashMap<String, Access>();
	}

	public void setRemoteCmd(RemoteCmd cmd) {
		this.cmd = cmd;
	}

	public void setThreadPool(ThreadPool pool) {
		threadPool = pool;
		startTask();
	}

	/**
	 * 启动定时任务
	 */
	private void startTask() {
		TimingTask.createTimingTask(threadPool, "access-info-save", this,
				"00:10", TimeUtil.MILLIOFHOUR);// 每小时 XXX
	}

	public RemoteCmd getRemoteCmd() {
		return cmd;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setResourceService(ResourceService service) {
		this.resourceService = service;
	}

	public ResourceService getResourceService() {
		return resourceService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String uri = req.getRequestURI();
		if (uri.indexOf("manager") > -1) {
			res.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		enterRecord(req);
		super.doFilter(request, response, chain);
	};

	/**
	 * 定时对内存中的访问值写入文件中，写入完成需要将内存置空，每次写入时，要吧内存的记录load进来，然后作对比，如果已经存在记录，
	 * 则值要更新内存中的值的访问量，然后将这条记录写入内存
	 */
	@Override
	public void work() throws RemoteException {
		if (accessRecord.isEmpty())
			return;
		String path = this.path;
		// path为空的情况
		if (null == path) {
			// 如果没有，则在classpath中找，但是这样很危险，每次部署都会被覆盖
			URL url = Thread.currentThread().getContextClassLoader()
					.getResource("access-record.txt");
			path = url.getPath();
		} else {
			// 非本机或本地进行备份
			backup(path, TYPE_BACKUP);
		}
		FileChannel channel = null;
		RandomAccessFile raf = null;
		try {
			File file = new File(path);
			raf = new RandomAccessFile(file, "rw");
			Map<String, Access> map = loadFile(raf);
			raf.seek(0);
			merge(map);
			// 将合并的记录写入文件
			channel = raf.getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(512);
			writeHeader(channel, buffer);
			write2File(channel, buffer, map);
			map = null;
			clearRecord();
			logger.info("访问记录已同步至文件");
		} catch (Exception e) {
			logger.error("访问记录写入文件失败", e);
			logger.info("正在执行回滚操作");
			backup(path, TYPE_ROLLBACK);// 回滚
		} finally {
			if (null != channel) {
				try {
					channel.close();
					raf.close();
					channel = null;
					raf = null;
				} catch (IOException e) {
				}
			}

		}

	}

	/**
	 * 将内存中的数据读入
	 * 
	 * @param file
	 * @return
	 */
	static private Map<String, Access> loadFile(RandomAccessFile file) {
		Map<String, Access> map = new HashMap<String, Access>();
		if (null == file)
			return map;
		try {
			String line = file.readLine();// 先把第一行表头读掉
			// 分析每一行
			while (null != (line = file.readLine())) {
				if ("".equals(line))
					continue;
				byte[] bytes = line.getBytes("ISO-8859-1");
				int cursor = 0;
				int preIdx = 0;
				String[] split = new String[5];
				for (int i = 0; i < bytes.length; i++) {
					if (bytes[i] == HT) {
						String str = new String(bytes, preIdx, i - preIdx,
								DEFAULT_CHARSET);
						split[cursor++] = str.trim();
						preIdx = i + 1;
					}

				}
				// 最后一组没有HT
				String str = new String(bytes, preIdx, bytes.length - preIdx,
						DEFAULT_CHARSET);
				split[cursor] = str.trim();
				int count = 0;
				String scount = split[1];
				try {
					count = Integer.valueOf(scount);
				} catch (NumberFormatException e) {
					logger.error("访问数解析失败，使用0代替,scount=" + scount, e);
				}
				Access access = new Access(split[0], count, split[2]);
				String dstr = split[3];
				try {
					access.setDate(TimeUtil.parse(dstr));
				} catch (Exception e) {// 这里不需要太严谨
					access.setDate(new Date());
				}
				access.setAddress(split[4]);
				map.put(access.getKey(), access);
			}
		} catch (Exception e) {
			logger.error("无法将访问记录文件读入", e);
		}
		return map;
	}

	/**
	 * 写入文件
	 * 
	 * @param channel
	 * @param buffer
	 * @throws IOException
	 */
	static private void write2File(FileChannel channel, ByteBuffer buffer,
			Map<String, Access> map) throws IOException {
		Iterator<Entry<String, Access>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			buffer.clear();
			Entry<String, Access> next = it.next();
			Access access = next.getValue();
			buffer.put(access.getKey().getBytes(DEFAULT_CHARSET));
			buffer.put(HT);
			buffer.put(String.valueOf(access.getCount()).getBytes(
					DEFAULT_CHARSET));
			buffer.put(HT);
			buffer.put(access.getIp().getBytes(DEFAULT_CHARSET));
			buffer.put(HT);
			Date date = access.getDate();
			if (null == date) {
				date = new Date();
			}
			buffer.put(TimeUtil.formatDate(date).getBytes(DEFAULT_CHARSET));
			buffer.put(HT);
			String address = access.getAddress();
			if (null == address) {
				address = "";
			}
			buffer.put(address.getBytes(DEFAULT_CHARSET));
			buffer.put(CR);
			buffer.put(LR);
			buffer.flip();
			channel.write(buffer);
		}
	}

	/**
	 * 表头
	 * 
	 * @param channel
	 * @param buffer
	 * @throws UnsupportedEncodingException
	 */
	private void writeHeader(FileChannel channel, ByteBuffer buffer)
			throws UnsupportedEncodingException {
		if (null == channel || null == buffer)
			return;
		buffer.clear();
		for (int i = 0; i < TABLE_HEADER.length; i++) {
			String item = TABLE_HEADER[i];
			buffer.put(item.getBytes(DEFAULT_CHARSET));
		}
		buffer.flip();
		try {
			channel.write(buffer);
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	/**
	 * 使用远程命令对访问记录文件进行备份，如果异常，使用备份文件回滚
	 * 
	 * @param url
	 * @throws RemoteException
	 */
	private void backup(String path, int type) throws RemoteException {
		if (null == cmd) {
			return;
		}
		final RemoteCmd cmd = this.cmd;
		if (type == TYPE_BACKUP) {
			final String comand = "cp  " + path + " " + path + ".bak";
			// 因为remoteCmd是使用阻塞会话，这里手动使用异步处理
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						cmd.cmd(comand);
					} catch (RemoteException e) {
						logger.error("访问记录备份失败", e);
					}
				}
			});

		} else if (type == TYPE_ROLLBACK) {
			final String comand = "rm " + path + ".bak";
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						cmd.cmd(comand);
					} catch (RemoteException e) {
						logger.error("访问备份删除失败", e);
					}
				}
			});

		} else {
			throw new RemoteException("不支持的操做类型,path:" + path + " type" + type);
		}
	}

	/**
	 * 将内存的访问记录清除
	 */
	private void clearRecord() {
		if (accessRecord.isEmpty())
			return;
		accessRecord.clear();
	}

	/**
	 * 将内存中的值合并到map，如果map已经存在该值，则更新状态，如果没有，则添加
	 * 
	 * @param map
	 */
	private void merge(Map<String, Access> map) {
		if (null == map) {
			map = new HashMap<String, Access>();
		}
		Iterator<Entry<String, Access>> iterator = accessRecord.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<String, Access> next = iterator.next();
			String key = next.getKey();
			Access access = map.get(key);
			if (null == access) {
				// 没有，添加
				map.put(key, next.getValue());
			} else {
				// 已经存在了，将内存中的记录的访问值加上文件中的访问值
				access.setCount(access.getCount() + next.getValue().getCount());
				map.put(access.getKey(), access); // 修改完记录后覆盖掉记录里的值
			}
		}
	}

	private void enterRecord(HttpServletRequest request) {
		String uri = request.getRequestURI();
		// 只记录访问首页
		if (uri.indexOf("loadblogs") == -1) {
			return;
		}
		String ip = request.getHeader("X-Real-IP");
		if (null == ip)
			return;
		String key = ip.replaceAll("\\.", "");// 去除“.”
		Access access = accessRecord.get(key);
		if (null == access) {
			access = new Access(key, 0, ip);
		}
		accessRecord.put(key, access);
		access.setDate(new Date());
		access.setAddress("");
		access.setCount(access.getCount() + 1);
		final Access acc = access;
		// 异步查询ip地址
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					IpQueryVo vo = resourceService.queryIpAddress(acc.getIp(),
							IpQueryApi.PROVIDER_IPSTACK.getId());
					if (IpQueryVo._nil.equals(vo)) {
						if (logger.isTraceEnabled()) {
							logger.trace("查询ip失败,ip:" + acc.getIp());
						}
						return;
					}
					String province = vo.getProvince();
					if (!province.endsWith("省")) {
						province += "省";
					}
					String address = province + vo.getCity();
					acc.setAddress(address);
				} catch (Exception e) {
					logger.error("查询ip失败", e);
				}

			}
		});
	}

	public static void main(String[] args) {
		File file = new File("access.txt");
		Map<String, Access> map = new HashMap<String, Access>();
		Access access = new Access("127001", 1, "127.0.0.1");
		access.setDate(new Date());
		access.setAddress("广东省广州市");
		Access access2 = new Access("19216801", 2, "192.168.0.1");
		access2.setDate(new Date());
		map.put("127001", access);
		map.put("19216801", access2);
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			byte[] bytes = toBytes(map);
			out.write(bytes);
			out.flush();
			out.close();
			out = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		System.out.println("start load file");
		InputStream in = null;
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		Map<String, Access> entry = null;
		try {
			in = new FileInputStream(file);
			int n = 0;
			byte[] bytes = new byte[512];
			while ((n = in.read(bytes)) != -1) {
				array.write(bytes, 0, n);
			}
			byte[] byteArray = array.toByteArray();
			entry = toEntry(byteArray);
			if (null != entry && !entry.isEmpty()) {
				Iterator<Entry<String, Access>> it = entry.entrySet()
						.iterator();
				while (it.hasNext()) {
					Entry<String, Access> next = it.next();
					Access value = next.getValue();
					System.out.println(value.toString());
				}
			}
			array.close();
			array = null;

		} catch (IOException e) {
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // 在写入内存
		if (null == entry) {
			return;
		}
		Access access3 = new Access("127002", 1, "127.0.0.1");
		access3.setDate(new Date());
		access3.setAddress("广东省广州市");
		Access access4 = new Access("19216802", 2, "192.168.0.1");
		access4.setDate(new Date());
		entry.put("127002", access3);
		entry.put("19216802", access4);
		byte[] bytes = toBytes(entry);
		try {
			out = new FileOutputStream(file);
			out.write(bytes);
			out.flush();
			out.close();
			out = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done");

	}

	public static byte[] toBytes(Map<String, Access> data) {
		if (null == data || data.isEmpty())
			return new byte[0];
		Iterator<Entry<String, Access>> it = data.entrySet().iterator();
		StringBuilder sb = new StringBuilder();
		while (it.hasNext()) {
			Entry<String, Access> next = it.next();
			Access access = next.getValue();
			sb.append(access.getKey());
			sb.append(BOUNDARY);
			sb.append(access.getIp());
			sb.append(BOUNDARY);
			sb.append(access.getCount());
			sb.append(BOUNDARY);
			sb.append(TimeUtil.formatDate(access.getDate()));
			sb.append(BOUNDARY);
			sb.append(access.getAddress() == null ? "" : access.getAddress());
			sb.append((char) CR).append((char) LR);
		}
		byte[] bytes = null;
		try {
			bytes = (sb.toString()).getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			bytes = (sb.toString()).getBytes();
		}
		return bytes;

	}

	public static Map<String, Access> toEntry(byte[] bytes) {
		String[] split = new String[5];
		int cursor = 0;
		int preIdx = 0;
		Map<String, Access> map = new HashMap<String, Access>();
		String line;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == BOUNDARY) {
				try {
					line = new String(bytes, preIdx, i - preIdx, "utf-8");
				} catch (UnsupportedEncodingException e) {
					line = new String(bytes, preIdx, preIdx - i);
				}
				split[cursor++] = line.trim();
				preIdx = i + 1;
			} else if (bytes[i] == LR && bytes[i - 1] == CR) { // 换行 \r\n
				// 结束一行
				try {
					line = new String(bytes, preIdx, i - preIdx, "utf-8");
				} catch (UnsupportedEncodingException e) {
					line = new String(bytes, preIdx, preIdx - i);
				}
				split[cursor] = line.trim();
				preIdx = i + 1;
				int count = 0;
				try {
					count = Integer.valueOf(split[2]);
				} catch (Exception e) {
					count = 1;
				}
				Access access = new Access(split[0], count, split[1]);
				String dstr = split[3];
				try {
					access.setDate(TimeUtil.parse(dstr));
				} catch (Exception e) {// 这里不需要太严谨
					access.setDate(new Date());
				}
				access.setAddress(split[4]);
				map.put(split[0], access);
				cursor = 0; // 游标重新计算
			}
		}
		return map;
	}

	static class Access {
		// Map保存到key，ip去除.
		private String key;
		private String ip;
		private int count;
		@JSONField(format = "yyyy-MM-dd HH:mm:ss")
		private Date date;
		private String address;

		public Access(String key, int count, String ip) {
			this.key = key;
			this.ip = ip;
			this.count = count;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String toString() {
			return JsonUtils.toJSONString(this);
		}

	}
}
