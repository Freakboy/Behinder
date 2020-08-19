package vip.youwe.sheller.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

public class ReplacingInputStream extends FilterInputStream {

    LinkedList<Integer> inQueue = new LinkedList();
    LinkedList<Integer> outQueue = new LinkedList();

    final byte[] search;
    final byte[] replacement;

    public ReplacingInputStream(InputStream in, byte[] search, byte[] replacement) {
        super(in);
        this.search = search;
        this.replacement = replacement;
    }

    private boolean isMatchFound() {
        Iterator<Integer> inIter = this.inQueue.iterator();
        for (int i = 0; i < this.search.length; i++) {
            if (!inIter.hasNext() || this.search[i] != inIter.next())
                return false;
        }
        return true;
    }


    private void readAhead() throws IOException {
        while (this.inQueue.size() < this.search.length) {
            int next = super.read();
            this.inQueue.offer(next);
            if (next == -1) {
                break;
            }
        }
    }


    public int read() throws IOException {
        if (this.outQueue.isEmpty()) {
            readAhead();

            if (isMatchFound()) {
                for (int i = 0; i < this.search.length; i++) {
                    this.inQueue.remove();
                }
                for (byte b : this.replacement)
                    this.outQueue.offer((int) b);
            } else {
                this.outQueue.add(this.inQueue.remove());
            }
        }
        return this.outQueue.remove();
    }
}
