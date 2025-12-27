package com.mammb.code.toml.impl;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BufferPool {

    char[] take();

    void recycle(char[] buf);


    static BufferPool defaultPool() {

        return new BufferPool() {

            // volatile since multiple threads may access queue reference
            private volatile WeakReference<ConcurrentLinkedQueue<char[]>> queue;

            @Override
            public char[] take() {
                char[] t = getQueue().poll();
                if (t == null)
                    return new char[4096];
                return t;            }

            @Override
            public void recycle(char[] t) {
                getQueue().offer(t);
            }

            private ConcurrentLinkedQueue<char[]> getQueue() {
                WeakReference<ConcurrentLinkedQueue<char[]>> q = queue;
                if (q != null) {
                    ConcurrentLinkedQueue<char[]> d = q.get();
                    if (d != null)
                        return d;
                }

                // overwrite the queue
                ConcurrentLinkedQueue<char[]> d = new ConcurrentLinkedQueue<>();
                queue = new WeakReference<>(d);
                return d;
            }
        };
    }

}
