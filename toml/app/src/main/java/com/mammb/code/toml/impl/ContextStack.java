package com.mammb.code.toml.impl;

import java.util.NoSuchElementException;

/**
 * Using the optimized stack impl as we don't require other things like iterator etc.
 */
final class ContextStack {

    private final int limit;
    private int size = 0;
    private ParserContext head;

    ContextStack(int limit) {
        this.limit = limit;
    }

    private void push(ParserContext context) {
        if (++size >= limit) {
            throw new RuntimeException("Input is too deeply nested [" + size + "]");
        }
        context.next = head;
        head = context;
    }

    private ParserContext pop() {
        if (head == null) {
            throw new NoSuchElementException();
        }
        size--;
        ParserContext temp = head;
        head = head.next;
        return temp;
    }

    private boolean isEmpty() {
        return head == null;
    }

}
