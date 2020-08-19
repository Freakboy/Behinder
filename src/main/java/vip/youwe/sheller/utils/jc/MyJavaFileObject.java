package vip.youwe.sheller.utils.jc;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class MyJavaFileObject extends SimpleJavaFileObject {

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
