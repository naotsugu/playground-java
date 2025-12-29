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

    private boolean bareKeyAllowed = true;
    private boolean inlineTableScope = false;
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
        if (bareKeyAllowed) {
            if (isBareKeyChar(ch)) {
                return readBareKey();
            }
            // table or dotted key
            bareKeyAllowed = (ch == '[' || ch == '.');
        }

        TomlToken tomlToken = switch (ch) {
            case '=' -> TomlToken.EQUALS;
            case '.' -> TomlToken.DOT;
            case ',' -> TomlToken.COMMA;
            case '{' -> TomlToken.CURLYOPEN;
            case '}' -> TomlToken.CURLYCLOSE;
            case '[' -> TomlToken.SQUAREOPEN;
            case ']' -> TomlToken.SQUARECLOSE;
            case 't' -> readTrue();
            case 'f' -> readFalse();
            case -1  -> TomlToken.EOF;
            case '"' -> readBasicString();
            case '\'' -> readLiteralString();
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                 '-', '+', 'i', 'n' -> readNumber(ch);
            default -> throw unexpectedChar(ch);
        };

        // inline table
        if (ch == '{') {
            inlineTableScope = true;
            bareKeyAllowed = true;
        } else if (ch == ',' && inlineTableScope) {
            bareKeyAllowed = true;
        } else if (ch == '}') {
            inlineTableScope = false;
        }

        return tomlToken;
    }


    private boolean isBareKeyChar(int ch) {
        // unquoted-key = 1*( ALPHA / DIGIT / %x2D / %x5F ) ; A-Z / a-z / 0-9 / - / _
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') ||
               (ch >= '0' && ch <= '9') || ch == '_' || ch == '-';
    }

    boolean hasNextToken() {
        reset();
        int ch = peek();

        // whitespace
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            if (ch == '\r') {
                bareKeyAllowed = true;
                lineNo++;
                readBegin++;
                ch = peek();
                if (ch == '\n') {
                    lastLineOffset = bufferOffset + readBegin + 1;
                } else {
                    lastLineOffset = bufferOffset + readBegin;
                    continue;
                }
            } else if (ch == '\n') {
                bareKeyAllowed = true;
                lineNo++;
                lastLineOffset = bufferOffset + readBegin + 1;
            }
            readBegin++;
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
                bareKeyAllowed = true;
                lineNo++;
                ch = read();
                if (ch == '\n') {
                    lastLineOffset = bufferOffset + readBegin;
                } else {
                    lastLineOffset = bufferOffset + readBegin - 1;
                    continue;
                }
            } else if (ch == '\n') {
                bareKeyAllowed = true;
                lineNo++;
                lastLineOffset = bufferOffset + readBegin;
            }
            ch = read();
        }
        return ch;
    }

    TomlToken readBareKey() {
        storeBegin = storeEnd = readBegin - 1;
        int ch;
        for (;;) {
            if (!isBareKeyChar(ch = read())) break;
        }
        if (ch != -1) {
            readBegin--;
            storeEnd = readBegin;
        }
        return TomlToken.STRING;
    }

    TomlToken readLiteralString() {
        storeBegin = storeEnd = readBegin;
        for (;;) {
            int ch = read();
            if (ch == -1 || ch == '\'') break;
        }
        storeEnd = readBegin - 1;
        return TomlToken.STRING;
    }

    TomlToken readBasicString() {
        // when inPlace is true, no need to copy chars
        boolean inPlace = true;
        storeBegin = storeEnd = readBegin;

        for (;;) {
            // write unescaped char block within the current buffer
            if (inPlace) {
                int ch;
                while (readBegin < readEnd && ((ch = buf[readBegin]) >= 0x20) && ch != '\\') {
                    if (ch == '"') {
                        storeEnd = readBegin++;  // ++ to consume quote char
                        return TomlToken.STRING; // Got the entire string
                    }
                    readBegin++;                 // consume unescaped char
                }
                storeEnd = readBegin;
            }

            // string may be crossing buffer boundaries and may contain
            // escaped characters.
            int ch = read();
            if (ch >= 0x20 && ch != 0x22 && ch != 0x5c) {
                if (!inPlace) {
                    buf[storeEnd] = (char) ch;
                }
                storeEnd++;
                continue;
            }
            switch (ch) {
                case '\\':
                    inPlace = false; // from now onwards need to copy chars
                    unescape();
                    break;
                case '"':
                    return TomlToken.STRING;
                default:
                    throw unexpectedChar(ch);
            }
        }
    }

    private void unescape() {
        int ch = read();
        switch (ch) {
            case 'b' -> buf[storeEnd++] = '\b';
            case 't' -> buf[storeEnd++] = '\t';
            case 'n' -> buf[storeEnd++] = '\n';
            case 'f' -> buf[storeEnd++] = '\f';
            case 'r' -> buf[storeEnd++] = '\r';
            case '"', '\\' , '/' -> buf[storeEnd++] = (char) ch;
            case 'u', 'U' -> {
                int unicode = 0;
                for (int i = 0; i < 4; i++) {
                    int ch3 = read();
                    int digit = (ch3 >= 0 && ch3 < HEX_LENGTH) ? HEX[ch3] : -1;
                    if (digit < 0) {
                        throw unexpectedChar(ch3);
                    }
                    unicode = (unicode << 4) | digit;
                }
                buf[storeEnd++] = (char) unicode;
            }
            default ->  throw unexpectedChar(ch);
        }
    }

    private TomlToken readNumber(int ch)  {

        storeBegin = storeEnd = readBegin - 1;

        // sign
        if (ch == '+') {
            ch = readNumberChar();
            if ((ch < '0' || ch > '9') && ch != 'i' && ch != 'n') {
                throw unexpectedChar(ch);
            }
        } else if (ch == '-') {
            this.minus = true;
            ch = readNumberChar();
            if ((ch < '0' || ch > '9') && ch != 'i' && ch != 'n') {
                throw unexpectedChar(ch);
            }
        }

        if (ch == 'i') {
            return readInf();
        } else if (ch == 'n') {
            return readNan();
        }

        // int
        if (ch == '0') {
            ch = readNumberChar();
        } else {
            do {
                ch = readNumberChar();
            } while (ch >= '0' && ch <= '9');
        }

        // frac
        if (ch == '.') {
            this.fracOrExp = true;
            int count = 0;
            do {
                ch = readNumberChar();
                count++;
            } while (ch >= '0' && ch <= '9');
            if (count == 1) {
                throw unexpectedChar(ch);
            }
        }

        // exp
        if (ch == 'e' || ch == 'E') {
            this.fracOrExp = true;
            ch = readNumberChar();
            if (ch == '+' || ch == '-') {
                ch = readNumberChar();
            }
            int count;
            for (count = 0; ch >= '0' && ch <= '9'; count++) {
                ch = readNumberChar();
            }
            if (count == 0) {
                throw unexpectedChar(ch);
            }
        }
        if (ch != -1) {
            // Only reset readBegin if eof has not been reached
            readBegin--;
            storeEnd = readBegin;
        }
        return fracOrExp ? TomlToken.FLOAT : TomlToken.INTEGER;
    }

    // Reads a number of char. If the char is within the buffer, directly
    // reads from the buffer. Otherwise, uses read() which takes care
    // of resizing, filling up the buf, adjusting the pointers
    private int readNumberChar() {
        int ch;
        if (readBegin < readEnd) {
            ch = buf[readBegin++];
        } else {
            storeEnd = readBegin;
            ch = read();
        }
        return (ch == '_') ? readNumberChar() : ch;
    }

    private TomlToken readTrue() {
        int ch1 = read();
        if (ch1 != 'r') throw expectedChar(ch1, 'r');
        int ch2 = read();
        if (ch2 != 'u') throw expectedChar(ch1, 'u');
        int ch3 = read();
        if (ch3 != 'e') throw expectedChar(ch1, 'e');
        return TomlToken.TRUE;
    }

    private TomlToken readFalse() {
        int ch1 = read();
        if (ch1 != 'a') throw expectedChar(ch1, 'a');
        int ch2 = read();
        if (ch2 != 'l') throw expectedChar(ch1, 'l');
        int ch3 = read();
        if (ch3 != 's') throw expectedChar(ch1, 's');
        int ch4 = read();
        if (ch4 != 'e') throw expectedChar(ch1, 'e');
        return TomlToken.FALSE;
    }

    private TomlToken readInf() {
        int ch1 = read();
        if (ch1 != 'n') throw expectedChar(ch1, 'n');
        int ch2 = read();
        if (ch2 != 'f') throw expectedChar(ch1, 'f');
        return minus ? TomlToken.N_INF : TomlToken.INF;
    }

    private TomlToken readNan() {
        int ch1 = read();
        if (ch1 != 'a') throw expectedChar(ch1, 'a');
        int ch2 = read();
        if (ch2 != 'n') throw expectedChar(ch1, 'n');
        return TomlToken.NAN;
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
            for (; i < storeLen; i++) {
                int ch = buf[storeBegin + i];
                if (ch == '_') continue;
                num = num * 10 + (ch - '0');
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
            for (; i < storeLen; i++) {
                int ch = buf[storeBegin + i];
                if (ch == '_') continue;
                num = num * 10 + (ch - '0');
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
    // So there are cases that will return false even though the number is long
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

    // Table to look up hex ch -> value (for e.g. HEX['F'] = 15, HEX['5'] = 5)
    private final static int[] HEX = new int[128];
    static {
        Arrays.fill(HEX, -1);
        for (int i = '0'; i <= '9'; i++) {
            HEX[i] = i - '0';
        }
        for (int i = 'A'; i <= 'F'; i++) {
            HEX[i] = 10 + i - 'A';
        }
        for (int i = 'a'; i <= 'f'; i++) {
            HEX[i] = 10 + i - 'a';
        }
    }
    private final static int HEX_LENGTH = HEX.length;

    private RuntimeException unexpectedChar(int ch) {
        return new RuntimeException("tokenizer.unexpected.char [" + ch + "]");
    }

    private RuntimeException expectedChar(int unexpected, char expected) {
        return new RuntimeException("tokenizer.expected.char [" + unexpected + "], expected [" + expected + "]");
    }
}
