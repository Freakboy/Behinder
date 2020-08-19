package vip.youwe.sheller.utils.jc;


import javax.tools.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Run {

    public static void main(String[] args) {
        (new Run()).test();
    }

    public void test() {
        String sourceCode = "\r\nimport javax.servlet.jsp.PageContext;\r\nimport javax.servlet.ServletOutputStream;\r\npublic class test\r\n{\r\n\tpublic boolean equals(Object obj){\r\n\r\n\tPageContext page = (PageContext) obj;\r\n\t\t\ttry {\r\n\t\t\t\tServletOutputStream so=page.getResponse().getOutputStream();\r\n\t\t\t\tso.write(\"afsddf\".getBytes(\"UTF-8\"));\r\n\t\t\t\tso.flush();\r\n\t\t\t\tso.close();\r\n\t\t\t\tpage.getOut().clear();  \r\n\t\t\t} catch (Exception e) {\r\n\t\t\t\t// TODO Auto-generated catch block\r\n\t\t\t\te.printStackTrace();\r\n\t\t\t} \r\n\t\treturn true;\r\n}\r\n}";

        try {
            while (true) {
                Thread.sleep(2000L);
            }
        } catch (Exception e) {

            e.printStackTrace();
            return;
        }
    }


    public static byte[] getClassFromSourceCode(String sourceCode) throws Exception {
        String cls;
        byte[] classBytes = null;
        Pattern CLASS_PATTERN = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s*");
        Matcher matcher = CLASS_PATTERN.matcher(sourceCode);

        if (matcher.find()) {
            cls = matcher.group(1);
        } else {
            throw new IllegalArgumentException("No such class name in " + sourceCode);
        }

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) {
            throw new Exception("本地机器上没有找到编译环境，请确认:1.是否安装了JDK环境;2." + System.getProperty("java.home") + File.separator + "lib目录下是否有tools.jar.");
        }
        StandardJavaFileManager standardJavaFileManager = jc.getStandardFileManager(null, null, null);
        JavaFileManager fileManager = new CustomClassloaderJavaFileManager(Run.class.getClassLoader(), standardJavaFileManager);
        JavaFileObject javaFileObject = new MyJavaFileObject(cls, sourceCode);

        List<String> options = new ArrayList<String>();
        options.add("-source");
        options.add("1.6");
        options.add("-target");
        options.add("1.6");
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler.CompilationTask cTask = jc.getTask(null, fileManager, collector, options, null, Arrays.asList(javaFileObject));
        boolean result = cTask.call();
        if (!result) {
            List<Diagnostic<? extends JavaFileObject>> diagnostics = collector.getDiagnostics();
            Iterator iterator = diagnostics.iterator();
            if (iterator.hasNext()) {
                Diagnostic<? extends JavaFileObject> diagnostic = (Diagnostic) iterator.next();

                throw new Exception(diagnostic.getMessage(null));
            }

        }
        JavaFileObject fileObject = CustomClassloaderJavaFileManager.fileObjects.get(cls);
        if (fileObject != null) {
            classBytes = ((MyJavaFileObject) fileObject).getCompiledBytes();
        }
        return classBytes;
    }
}
