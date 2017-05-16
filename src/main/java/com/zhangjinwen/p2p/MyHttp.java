package com.zhangjinwen.p2p;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyHttp {
	private String host;
	private int port;

	private Socket socket = null;
	private PrintWriter pw = null;
	private BufferedInputStream bs = null;
	private int localPort;
	private String localAddress;
	private Socket listen;	
	private UserItem[] users =null;
	private String user;
	private String conn;
	public static void main(String[] args) {
		String host = "101game.esy.es";
		int port = 80;
	
		String user="zjwTest";
		
	    if(args.length>0){
		 user=	args[0];
		}
		String conn="con1231";
		
		if(args.length>1){
			conn=	args[1];
		}
		String url = "/p2p/cmd.php?cmd=register&user="+user+"&conn="+conn;
		final MyHttp http = new MyHttp(host, port);
		http.user=user;
		http.conn=conn;
		try {
			System.out.println(http.getResult(url));
			System.out.println(http.localPort);
			System.out.println(http.localAddress);
            while(true){
            	String result=http.getResult(url);
            	UserItem[] items =http.getIps(result);
            	if(items!=null&&items.length==2){
            		http.users=items;
            		UserItem other=	http.getOther();
                	if(other==null)throw new RuntimeException("获取其它用户失败");
            	}
            	
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
			/*new Thread(){
				public void run(){
					 try {
						http.listen();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}//阻塞
				}
			}.start();*/
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		http.close();

	}
	
	private UserItem getOther() {
	 for( UserItem u: users){
		 if(!u.user.equals(user)){
			 return u;
		 }
	 }
		return null;
	}

	private  UserItem[] getIps(String result){
		System.out.println("getIps:"+result);
		String ipSp="\"ips\":";
		int ipsIndex = result.indexOf(ipSp);
		if(ipsIndex!=-1){
			String ips=result.substring(ipsIndex+ipSp.length());
			ips=ips.substring(1, ips.length()-2);
			String[] clients = ips.split("_");
			if(clients.length==2&&clients[0].trim().length()>0&&clients[1].trim().length()>0){
				UserItem item1 = getIp(clients[0]);
				UserItem item2 = getIp(clients[1]);
				if(item1!=null&&item2!=null){
					return new UserItem[]{item1,item2};
				}
			}
		}
		return null;
	}
	
	private UserItem getIp(String client){
		String[] ups1 =client.split(":");
		System.out.println("client:"+client);
		if(ups1.length==3){
			String user=ups1[0];
			String ip=ups1[1];
			int port=Integer.parseInt(ups1[2]);
			UserItem ui=new UserItem();
			ui.user=user;
			ui.ip=ip;
			ui.port=port;
			return ui;
		}
		return null;
	
	}
	
	private static class UserItem{
		private String user;
		private int port;
		private String ip;
		
	}
	
	private void listen() throws IOException{
		ServerSocket serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(localAddress, localPort));
		System.out.println("******开始监听端口：" + localPort);
		listen = serverSocket.accept();
		System.out.println("******监听成功：" + localPort);
		serverSocket.close();
	}

	public MyHttp(String host  , int port) {
		this.host = host;
		this.port = port;
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException("连接失败", e);
		}
	}

	private void init() throws UnknownHostException, IOException {
		socket = new Socket();
		socket.setReuseAddress(true);
		//SO_REUSEPORT
		socket.connect(new InetSocketAddress(host, port));
		localPort = socket.getLocalPort();
		localAddress=socket.getLocalAddress().getHostAddress();
		pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
		bs = new BufferedInputStream(socket.getInputStream());
	}

	public void close() {
		if (bs != null) {
			try {
				bs.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (pw != null) {
			pw.close();
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getResult(String url) throws IOException {

		StringBuilder sb = new StringBuilder();
		sb.append("GET " + url + " HTTP/1.1\r\n");
		sb.append("Host: " + host + ":" + port + "\r\n");
		sb.append("Connection: Keep-Alive\r\n");
		sb.append("\r\n");
		pw.write(sb.toString());
		pw.flush();
		String line = null;
		int contentLength = 0;
		boolean transferEncoding = false;
		do {
			line = readLine();
			// 如果有Content-Length消息头时取出
			if (line.startsWith("Content-Length")) {
				contentLength = Integer.parseInt(line.split(":")[1].trim());
			} else if (line.startsWith("Transfer-Encoding:")) {
				transferEncoding = true;
			}
			// 如果遇到了一个单独的回车换行，则表示请求头结束
		} while (!line.equals("\r\n"));

		// --输消息的体
		String content = null;
		if (transferEncoding) {
			StringBuilder sbc = new StringBuilder();
			while (true) {
				String chunkedLengthStr = readLine().trim();
				int chunkedLength = Integer.parseInt(chunkedLengthStr, 16);
				if (chunkedLength == 0) {
					readLength(2);
					break;
				}
				sbc.append(readLength(chunkedLength + 2));
			}
			content = sbc.toString();
		} else {
			content = readLength(contentLength);
		}
		return content;

	}

	

	private String readLength(int contentLen) throws IOException {
		byte[] data = new byte[contentLen];
		int index = 0;
		byte readByte;
		do {
			readByte = (byte) bs.read();
			data[index++] = readByte;
		} while (index < contentLen);// 消息体读还未读完
		return new String(data, charset);
	}

	private static final String charset = "utf-8";

	private String readLine() throws IOException {
		byte[] data = new byte[1 * 1024];
		int index = 0;
		byte readByte;
		do {
			readByte = (byte) bs.read();
			data[index++] = readByte;
		} while (readByte != 10);
		byte[] tmpByteArr = new byte[index];
		System.arraycopy(data, 0, tmpByteArr, 0, index);
		return new String(tmpByteArr, "utf-8");
	}

}
