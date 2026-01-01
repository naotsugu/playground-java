package com.mammb.code.toml.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

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
            case '"' -> readBasic();
            case '\'' -> readLiteral();
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

    private boolean matchPeek(int... chs) {
        int localReadBegin = readBegin;
        try {
            for (int ch : chs) {
                if (localReadBegin == readEnd) {
                    // need to fill the buffer
                    int len = fillBuf();
                    if (len == -1) {
                        return false;
                    }
                    assert len != 0;
                    localReadBegin = storeEnd;
                    readEnd = readBegin + len;
                }
                if (buf[localReadBegin++] != ch) return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
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

    TomlToken readLiteral() {

        if (matchPeek('\'', '\'')) return readLiteralTextBlock();

        storeBegin = storeEnd = readBegin;
        for (;;) {
            int ch = read();
            if (ch == -1 || ch == '\'') break;
        }
        storeEnd = readBegin - 1;
        return TomlToken.STRING;
    }

    TomlToken readBasic() {

        if (matchPeek('"', '"')) return readTextBlock();

        // when inPlace is true, no need to copy chars
        boolean inPlace = true;
        storeBegin = storeEnd = readBegin;

        for (;;) {
            // write unescaped char block within the current buffer
            if (inPlace) {
                int ch;
                while (readBegin < readEnd && ((ch = buf[readBegin]) >= ' ') && ch != '\\') {
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
            if (ch >= ' ' && ch != '"' && ch != '\\') {
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

    TomlToken readTextBlock() {

        readBegin += 2;

        // a newline immediately following the opening delimiter will be trimmed.
        if (matchPeek('\r', '\n')) {
            lineNo++;
            readBegin += 2;
        }
        if (matchPeek('\n')) {
            lineNo++;
            readBegin++;
        }

        int cons = 0;
        boolean inPlace = true;
        storeBegin = storeEnd = readBegin;

        for (;;) {
            if (inPlace) {
                int ch;
                while (readBegin < readEnd && ((ch = buf[readBegin]) != '\\')) {
                    readBegin++;
                    if (ch == '"') {
                        cons++;
                        if (cons == 3) {
                            storeEnd = readBegin - 3;
                            return TomlToken.STRING;
                        }
                    } else {
                        cons = 0;
                    }
                }
                storeEnd = readBegin;
            }

            int ch = read();
            if (ch == -1) {
                throw unexpectedChar(ch);
            } else if (ch == '\\') {
                inPlace = false;
                int n = read();
                if (n == ' ' || n == '\t' || n == '\r' || n == '\n') {
                    // it will be trimmed along with all whitespace (including newlines)
                    // up to the next non-whitespace character or closing delimiter.
                    while ((ch = read()) != -1 && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) { }
                    readBegin--;
                } else {
                    readBegin--;
                    unescape();
                }
            } else {
                if (!inPlace) {
                    buf[storeEnd] = (char) ch;
                }
                storeEnd++;
                if (ch == '"') {
                    cons++;
                    if (cons == 3) {
                        storeEnd -= 3;
                        return TomlToken.STRING;
                    }
                } else {
                    cons = 0;
                }
            }
        }
    }

    TomlToken readLiteralTextBlock() {

        readBegin += 2;
        if (matchPeek('\r', '\n')) {
            lineNo++;
            readBegin += 2;
        }
        if (matchPeek('\n')) {
            lineNo++;
            readBegin++;
        }

        int cons = 0;
        storeBegin = storeEnd = readBegin;
        for (;;) {
            int ch = read();
            if (ch == -1) break;
            if (ch == '\'') {
                cons++;
                if (cons == 3) {
                    storeEnd = readBegin - 3;
                    break;
                }
            } else {
                cons = 0;
            }
        }
        return TomlToken.STRING;
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

        // date time
        var maybe = readDateTimeOr(ch);
        if (maybe.isPresent()) return maybe.get();

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
            ch = readNumberCharSkipUnderscore();
        } else {
            do {
                ch = readNumberCharSkipUnderscore();
            } while (ch >= '0' && ch <= '9');
        }

        // frac
        if (ch == '.') {
            this.fracOrExp = true;
            int count = 0;
            do {
                ch = readNumberCharSkipUnderscore();
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
                ch = readNumberCharSkipUnderscore();
            }
            if (count == 0) {
                throw unexpectedChar(ch);
            }
        }
        if (ch != -1) {
            // only reset readBegin if eof has not been reached
            readBegin--;
            storeEnd = readBegin;
        }
        return fracOrExp ? TomlToken.FLOAT : TomlToken.INTEGER;
    }

    // Reads a number of char. If the char is within the buffer, directly
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
    private int readNumberCharSkipUnderscore() {
        int ch = readNumberChar();
        return (ch == '_') ? readNumberCharSkipUnderscore() : ch;
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

    private Optional<TomlToken> readDateTimeOr(int ch) {

        storeBegin = storeEnd = readBegin - 1;

        int n = 0;
        while ('0' <= ch && ch <= '9') {
            n++;
            ch = readNumberChar();
        }

        if (n == 2 && ch == ':') {
            // local-time 07:32:00, 00:32:00.5, 00:32:00.999999, 07:32
            while (('0' <= ch && ch <= '9') || ch == ':' || ch == '.') {
                if (n == 5 && ch != ':') throw expectedChar(ch, ':');
                if (n == 8 && ch != '.') throw expectedChar(ch, '.');
                n++;
                ch = readNumberChar();
            }
            storeEnd = readBegin--;
            return Optional.of(TomlToken.TIME);
        }

        if (n == 4 && ch == '-') {
            // Offset Date-Time
            //  1979-05-27T07:32:00Z, 1979-05-27T00:32:00-07:00, 1979-05-27T00:32:00.5-07:00,
            //  1979-05-27T00:32:00.999999-07:00, 1979-05-27 07:32:00Z, 1979-05-27 07:32Z, 1979-05-27 07:32-07:00
            // Local Date-Time
            //  1979-05-27T07:32:00, 1979-05-27T07:32:00.5, 1979-05-27T00:32:00.999999, 1979-05-27T07:32
            // Local Date
            //  1979-05-27
            boolean offset = false;
            while (('0' <= ch && ch <= '9') || ch == '-' || ch == ':' || ch == '.' || ch == ' ' || ch == 'T' || ch == 't' || ch == 'Z' || ch == '+') {
                if (n == 7 && ch != '-') throw expectedChar(ch, '-');
                if (n == 10 && ch != ' ' && ch != 'T') throw expectedChar(ch, 'T');
                if (n == 13 && ch != ':') throw expectedChar(ch, ':');
                if (!offset && ch == 'Z') offset = true;
                if (!offset && ch == '-' && n > 10) offset = true;
                if (n > 10 && ch == ' ') break;
                n++;
                ch = readNumberChar();
            }

            storeEnd = readBegin--;
            if (ch == ' ') storeEnd--;
            if (n < 12) {
                return Optional.of(TomlToken.LOCALDATE);
            } else if (offset) {
                return Optional.of(TomlToken.DATETIME);
            } else {
                return Optional.of(TomlToken.LOCALDATETIME);
            }
        }

        if (ch == -1) readBegin++;
        readBegin -= n;
        return Optional.empty();
    }

    private int read() {
        try {
            if (readBegin == readEnd) {
                // need to fill the buffer
                int len = fillBuf();
                if (len == -1) {
                    readBegin = readEnd = storeEnd;
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
                    //  --------------------
                    //  |                  |
                    //  --------------------
                    //  ^                  ^ storeEnd
                    //  storeBegin
                    //
                    //  ---------------------------------------
                    //  |                  |                  |
                    //  ---------------------------------------
                    //  ^                  ^ storeEnd
                    //  storeBegin
                    char[] doubleBuf = Arrays.copyOf(buf, 2 * buf.length);
                    bufferPool.recycle(buf);
                    buf = doubleBuf;
                } else {
                    // left shift all the stored data to make space
                    //  --------------------
                    //  |     |******|     |
                    //  --------------------
                    //        ^      ^
                    //  storeBegin  storeEnd
                    //
                    //  --------------------
                    //  |******|           |
                    //  --------------------
                    //  ^      ^ storeEnd
                    //  storeBegin
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
        //  --------------------      --------------------
        //  |******|           |  ->  |******|***********|
        //  --------------------      --------------------
        //  ^      ^ storeEnd
        //  storeBegin
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

    LocalTime getLocalTime() {
        return LocalTime.parse(new String(buf, storeBegin, storeEnd - storeBegin));
    }

    LocalDate getLocalDate() {
        return LocalDate.parse(new String(buf, storeBegin, storeEnd - storeBegin));
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

    // Table to look up hex ch -> value (for e.g., HEX['F'] = 15, HEX['5'] = 5)
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
