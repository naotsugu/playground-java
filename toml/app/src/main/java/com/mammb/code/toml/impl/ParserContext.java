package com.mammb.code.toml.impl;

interface ParserContext {
    ParserContext next();

    abstract class AbstractContext {
        private ParserContext next;
        public ParserContext next() {
            return next;
        }
        public void next(ParserContext next) {
            this.next = next;
        }
    }


}
