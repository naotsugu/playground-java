package com.mammb.code.toml.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Arrays;

public class TomlTokenizer implements Closeable {

    private final Reader reader;
    private final BufferPool bufferPool;

    private char[] buf;
    // Indexes in buffer
    //
    // XXXssssssssssssXXXXXXXXXXXXXXXXXXXXXXrrrrrrrrrrrrrrXXXXXX
    //    ^           ^                     ^             ^
    //    |           |                     |             |
    //   storeBegin  storeEnd            readBegin      readEnd
    private int readBegin;
    private int readEnd;
    private int storeBegin;
    private int storeEnd;

    // line number of the current pointer of parsing char
    private long lineNo = 1;

    // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    // ^
    // |
    // bufferOffset
    //
    // offset of the last \r\n or \n. will be used to calculate the column number
    // of a token or an error. This may be outside the buffer.
    private long lastLineOffset = 0;
    // offset in the stream for the start of the buffer, will be used in
    // calculating TomlLocation's stream offset, column no.
    private long bufferOffset = 0;

    // beginning of line
    private boolean bol = true;
    private boolean closed = false;

    private boolean minus;
    private boolean fracOrExp;
    private BigDecimal bd;

    TomlTokenizer(Reader reader) {
        this.reader = reader;
        this.bufferPool = BufferPool.defaultPool();
        this.buf = bufferPool.take();
    }

    TomlToken nextToken() {
        reset();
        int ch = readSkipWhite();
        if (bol) {
            if (isBareKeyChar(ch)) {
                readBareKey();
            }
            bol = false;
        }

        return switch (ch) {
            case '=' -> TomlToken.EQUALS;
            case '.' -> TomlToken.DOT;
            case ',' -> TomlToken.COMMA;
            case '{' -> TomlToken.CURLYOPEN;
            case '}' -> TomlToken.CURLYCLOSE;
            case '[' -> TomlToken.SQUAREOPEN;
            case ']' -> TomlToken.SQUARECLOSE;
            case -1  -> TomlToken.EOF;
            case '"' -> readBasicString();
            case '\'' -> readLiteralString();
            default -> readAny();
        };
    }


    private boolean isBareKeyChar(int ch) {
        //unquoted-key = 1*( ALPHA / DIGIT / %x2D / %x5F ) ; A-Z / a-z / 0-9 / - / _
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-';
    }

    boolean hasNextToken() {
        reset();
        int ch = peek();

        // whitespace
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            if (ch == '\r') {
                bol = true;
                ++lineNo;
                ++readBegin;
                ch = peek();
                if (ch == '\n') {
                    lastLineOffset = bufferOffset + readBegin + 1;
                } else {
                    lastLineOffset = bufferOffset + readBegin;
                    continue;
                }
            } else if (ch == '\n') {
                bol = true;
                ++lineNo;
                lastLineOffset = bufferOffset + readBegin + 1;
            }
            ++readBegin;
            ch = peek();
        }
        return ch != -1;
    }

    private int peek() {
        try {
            if (readBegin == readEnd) {
                // need to fill the buffer
                int len = fillBuf();
                if (len == -1) {
                    return -1;
                }
                assert len != 0;
                readBegin = storeEnd;
                readEnd = readBegin + len;
            }
            return buf[readBegin];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    int readSkipWhite() {
        int ch = read();
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            if (ch == '\r') {
                bol = true;
                ++lineNo;
                ch = read();
                if (ch == '\n') {
                    lastLineOffset = bufferOffset + readBegin;
                } else {
                    lastLineOffset = bufferOffset + readBegin - 1;
                    continue;
                }
            } else if (ch == '\n') {
                bol = true;
                ++lineNo;
                lastLineOffset = bufferOffset + readBegin;
            }
            ch = read();
        }
        return ch;
    }
    TomlToken readBareKey() {
        return null;
    }
    TomlToken readBasicString() {
        return null;
    }

    TomlToken readLiteralString() {
        return null;
    }

    TomlToken readAny() {
        return null;
    }

    private void unescape() {

    }

    // Reads a number char. If the char is within the buffer, directly
    // reads from the buffer. Otherwise, uses read() which takes care
    // of resizing, filling up the buf, adjusting the pointers
    private int readNumberChar() {
        if (readBegin < readEnd) {
            return buf[readBegin++];
        } else {
            storeEnd = readBegin;
            return read();
        }
    }

    private void readNumber(int ch)  {

    }

    private void readTrue() {
        int ch1 = read();
        if (ch1 != 'r') throw new RuntimeException();
        int ch2 = read();
        if (ch2 != 'u') throw new RuntimeException();
        int ch3 = read();
        if (ch3 != 'e') throw new RuntimeException();
    }

    private void readFalse() {
        int ch1 = read();
        if (ch1 != 'a') throw new RuntimeException();
        int ch2 = read();
        if (ch2 != 'l') throw new RuntimeException();
        int ch3 = read();
        if (ch3 != 's') throw new RuntimeException();
        int ch4 = read();
        if (ch4 != 'e') throw new RuntimeException();
    }

    private int read() {
        try {
            if (readBegin == readEnd) {
                // need to fill the buffer
                int len = fillBuf();
                if (len == -1) {
                    return -1;
                }
                assert len != 0;
                readBegin = storeEnd;
                readEnd = readBegin + len;
            }
            return buf[readBegin++];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int fillBuf() throws IOException {
        if (storeEnd != 0) {
            int storeLen = storeEnd - storeBegin;
            if (storeLen > 0) {
                // there is some store data
                if (storeLen == buf.length) {
                    // buffer is full, double the capacity
                    char[] doubleBuf = Arrays.copyOf(buf, 2 * buf.length);
                    bufferPool.recycle(buf);
                    buf = doubleBuf;
                } else {
                    // Left shift all the stored data to make space
                    System.arraycopy(buf, storeBegin, buf, 0, storeLen);
                    storeEnd = storeLen;
                    storeBegin = 0;
                    bufferOffset += readBegin - storeEnd;
                }
            } else {
                storeBegin = storeEnd = 0;
                bufferOffset += readBegin;
            }
        } else {
            bufferOffset += readBegin;
        }
        // fill the rest of the buf
        return reader.read(buf, storeEnd, buf.length - storeEnd);
    }

    // state associated with the current token is no more valid
    private void reset() {
        if (storeEnd != 0) {
            storeBegin = 0;
            storeEnd = 0;
            bd = null;
            minus = false;
            fracOrExp = false;
        }
    }

    String getValue() {
        return new String(buf, storeBegin, storeEnd - storeBegin);
    }

    CharSequence getCharSequence() {
        int len = storeEnd - storeBegin;
        return new StringBuilder(len).append(buf, storeBegin, len);
    }

    BigDecimal getBigDecimal() {
        if (bd == null) {
            int sourceLen = storeEnd - storeBegin;
            bd = new BigDecimal(buf, storeBegin, sourceLen);
        }
        return bd;
    }

    int getInt() {
        // no need to create BigDecimal for common integer values (1-9 digits)
        int storeLen = storeEnd - storeBegin;
        if (!fracOrExp && (storeLen <= 9 || (minus && storeLen == 10))) {
            int num = 0;
            int i = minus ? 1 : 0;
            for(; i < storeLen; i++) {
                num = num * 10 + (buf[storeBegin + i] - '0');
            }
            return minus ? -num : num;
        } else {
            return getBigDecimal().intValue();
        }
    }

    long getLong() {
        // no need to create BigDecimal for common integer values (1-18 digits)
        int storeLen = storeEnd - storeBegin;
        if (!fracOrExp && (storeLen <= 18 || (minus && storeLen == 19))) {
            long num = 0;
            int i = minus ? 1 : 0;
            for(; i < storeLen; i++) {
                num = num * 10 + (buf[storeBegin + i] - '0');
            }
            return minus ? -num : num;
        } else {
            return getBigDecimal().longValue();
        }
    }

    // returns true for common integer values (1-9 digits).
    // So there are cases it will return false even though the number is int
    boolean isDefinitelyInt() {
        int storeLen = storeEnd - storeBegin;
        return !fracOrExp && (storeLen <= 9 || (minus && storeLen == 10));
    }

    // returns true for common long values (1-18 digits).
    // So there are cases it will return false even though the number is long
    boolean isDefinitelyLong() {
        int storeLen = storeEnd - storeBegin;
        return !fracOrExp && (storeLen <= 18 || (minus && storeLen == 19));
    }

    boolean isIntegral() {
        return !fracOrExp || getBigDecimal().scale() == 0;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            reader.close();
            bufferPool.recycle(buf);
            closed = true;
        }
    }

}
