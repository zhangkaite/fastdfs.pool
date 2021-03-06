package com.fast.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageServerImpl implements ImageServer {
	private static final Logger log = LoggerFactory
			.getLogger(ImageServerImpl.class);
	/**
	 * 跟踪服务器地址
	 */
	private String serverIp;
	/**
	 * 跟踪服务器访问端口
	 */
	private int port = 22133;
	/**
	 * 连接池大小
	 */
	private int size;
	/**
	 * 获取链接超时时间，单位秒
	 */
	private int waitTimes = 5;

	private ConnectionPool pool = null;

	/**
	 * 
	 * @param serverIp
	 *            域名地址或IP
	 * @param port
	 *            端口
	 * @param size
	 *            连接池大小
	 * 
	 *            默认心跳时间为半个小时。
	 * @throws IOException
	 */
	public ImageServerImpl(String serverIp, int port, int size)
			throws IOException {
		this(serverIp, port, size, 60 * 30);
	}

	/**
	 * 
	 * @param serverIp
	 *            域名地址或IP
	 * @param port
	 *            端口
	 * @param size
	 *            连接池大小
	 * 
	 * @param heartBeatTime
	 *            心跳时间 。单位为秒
	 * @throws IOException
	 */
	public ImageServerImpl(String serverIp, int port, int size,
			int heartBeatTime) throws IOException {
		this.serverIp = serverIp;
		this.port = port;
		this.size = size;
		pool = new ConnectionPool(serverIp, port, this.size, heartBeatTime);
	}

	/**
	 * 
	 * @param serverIp
	 *            域名地址或IP
	 * @param port
	 *            端口
	 * @throws IOException
	 */
	public ImageServerImpl(String serverIp, int port) throws IOException {
		this(serverIp, port, 2);
	}

	/**
	 * 
	 * @param serverIp
	 *            域名地址或IP 端口默认
	 * @throws IOException
	 */
	public ImageServerImpl(String serverIp) throws IOException {
		this(serverIp, 22122);
	}

	public String uploadFile(File file) throws IOException, Exception {
		if (file == null) {
			return null;
		}
		return uploadFile(file, getFileExtName(file.getName()));
	}

	public String uploadFile(File file, String suffix) throws IOException,
			Exception {
		if (file == null) {
			return null;
		}
		byte[] fileBuff = getFileBuffer(file);
		return send(fileBuff, suffix);
	}

	public String uploadFile(byte[] fileBuff, String suffix)
			throws IOException, Exception {
		return send(fileBuff, suffix);
	}

	private String send(byte[] fileBuff, String fileExtName)
			throws IOException, Exception {
		String upPath = null;
		if (fileBuff == null) {
			return null;
		}
		TrackerServer trackerServer = null;
		try {
			trackerServer = pool.checkout(waitTimes);
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer,
					storageServer);
			upPath = client1.upload_file1(fileBuff, fileExtName, null);
			pool.checkin(trackerServer);
		} catch (InterruptedException e) {
			// 确实没有空闲连接,并不需要删除与fastdfs连接
			log.error("ImageServerImpl execution send throw :", e);
			throw e;
		} catch (NullPointerException e) {
			log.error("ImageServerImpl execution send throw :", e);
			throw e;
		} catch (Exception e) {
			// 发生io异常等其它异常，默认删除这次连接重新申请
			log.error("ImageServerImpl execution send throw :", e);
			pool.drop(trackerServer);
			throw e;
		}
		return upPath;
	}

	private String getFileExtName(String name) {
		String extName = null;
		if (name != null && name.contains(".")) {
			extName = name.substring(name.lastIndexOf(".") + 1);
		}
		return extName;
	}

	private byte[] getFileBuffer(File file) {
		byte[] fileByte = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			fileByte = new byte[fis.available()];
			fis.read(fileByte);
		} catch (FileNotFoundException e) {
			log.error("ImageServerImpl  read file   throw :", e);
		} catch (IOException e) {
			log.error("ImageServerImpl  read file   throw :", e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				fis = null;
			}
		}
		return fileByte;
	}

	@Override
	public String getServerIp() {
		return serverIp;
	}

	@Override
	public int getPort() {
		return port;
	}

	public int getSize() {
		return size;
	}

	public int getWaitTimes() {
		return waitTimes;
	}

	public void setWaitTimes(int waitTimes) {
		this.waitTimes = waitTimes;
	}

	@Override
	public boolean deleteFile(String fileId) throws IOException, Exception {
		boolean result = false;
		TrackerServer trackerServer = null;
		try {
			trackerServer = pool.checkout(waitTimes);
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer,
					storageServer);
			result = client1.delete_file1(fileId) == 0 ? true : false;
			pool.checkin(trackerServer);
		} catch (InterruptedException e) {
			// 确实没有空闲连接,并不需要删除与fastdfs连接
			log.error("ImageServerImpl execution deleteFile throw:", e);
			throw e;
		} catch (NullPointerException e) {
			log.error("ImageServerImpl execution deleteFile throw:", e);
			throw e;
		} catch (Exception e) {
			// 发生io异常等其它异常，默认删除这次连接重新申请
			log.error("ImageServerImpl execution deleteFile throw:", e);
			e.printStackTrace();
			pool.drop(trackerServer);
			throw e;
		}
		return result;
	}

	@Override
	public byte[] getFileByID(String fileId) throws IOException, Exception {
		byte[] result = null;
		TrackerServer trackerServer = null;
		try {
			trackerServer = pool.checkout(waitTimes);
			StorageServer storageServer = null;
			StorageClient1 client1 = new StorageClient1(trackerServer,
					storageServer);
			result = client1.download_file1(fileId);
			pool.checkin(trackerServer);
		} catch (InterruptedException e) {
			// 确实没有空闲连接,并不需要删除与fastdfs连接
			log.error("ImageServerImpl execution getFileByID throw :", e);
			throw e;
		} catch (NullPointerException e) {
			log.error("ImageServerImpl execution getFileByID throw :", e);
			throw e;
		} catch (Exception e) {
			// 发生io异常等其它异常，默认删除这次连接重新申请
			log.error("ImageServerImpl execution getFileByID throw :", e);
			e.printStackTrace();
			pool.drop(trackerServer);
			throw e;
		}
		return result;
	}
}
