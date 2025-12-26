package com.mammb.code.toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Toml {

    enum TokenType {
        BARE_KEY, STRING, INTEGER, FLOAT, BOOLEAN, DATETIME,
        LBRACKET, RBRACKET, LBRACE, RBRACE, // [ ] { }
        DOT, COMMA, EQUALS, // . , =
        EOF // End of File
    }

    record Token(TokenType type, String value, int line) { }

    static class Lexer {
        private final String input;
        private int pos = 0;
        private int line = 1;

        Lexer(String input) {
            this.input = input;
        }

        private char peek() {
            if (pos >= input.length()) return '\0';
            return input.charAt(pos);
        }

        private char advance() {
            char c = peek();
            pos++;
            if (c == '\n') line++;
            return c;
        }

        private boolean match(char expected) {
            if (peek() == expected) {
                advance();
                return true;
            }
            return false;
        }

        public Token nextToken() {
            skipWhitespaceAndComments();

            if (pos >= input.length()) return new Token(TokenType.EOF, "", line);

            char c = peek();

            // Structural Characters
            if (match('=')) return new Token(TokenType.EQUALS, "=", line);
            if (match('.')) return new Token(TokenType.DOT, ".", line);
            if (match(',')) return new Token(TokenType.COMMA, ",", line);
            if (match('[')) return new Token(TokenType.LBRACKET, "[", line);
            if (match(']')) return new Token(TokenType.RBRACKET, "]", line);
            if (match('{')) return new Token(TokenType.LBRACE, "{", line);
            if (match('}')) return new Token(TokenType.RBRACE, "}", line);

            // Strings (Basic & Literal)
            if (c == '"') return readBasicString();
            if (c == '\'') return readLiteralString();

            // Boolean / Numbers / Dates / Bare Keys
            // ABNF上、bare-keyは A-Z a-z 0-9 - _ なので、これらをまとめて読み込む
            if (isBareKeyChar(c)) {
                return readBareValueOrKey();
            }

            throw new IllegalStateException("Unexpected character '" + c + "' at line " + line);
        }

        private void skipWhitespaceAndComments() {
            while (true) {
                char c = peek();
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    advance();
                } else if (c == '#') {
                    // Comment: skip until newline
                    while (peek() != '\n' && peek() != '\0') advance();
                } else {
                    break;
                }
            }
        }

        private Token readBasicString() {
            advance(); // consume opening "
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            while (pos < input.length()) {
                char c = advance();
                if (escaped) {
                    switch (c) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        // TODO
                        default: sb.append(c);
                    }
                    escaped = false;
                } else {
                    if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        return new Token(TokenType.STRING, sb.toString(), line);
                    } else {
                        sb.append(c);
                    }
                }
            }
            throw new IllegalStateException("Unterminated string at line " + line);
        }

        private Token readLiteralString() {
            advance(); // consume opening '
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = advance();
                if (c == '\'') {
                    return new Token(TokenType.STRING, sb.toString(), line);
                }
                sb.append(c);
            }
            throw new IllegalStateException("Unterminated literal string at line " + line);
        }

        private Token readBareValueOrKey() {
            int start = pos;
            while (pos < input.length() && isBareKeyChar(peek())) {
                advance();
            }
            String text = input.substring(start, pos);

            // Boolean check
            if ("true".equals(text) || "false".equals(text)) {
                return new Token(TokenType.BOOLEAN, text, line);
            }

            // Date/Time check (Simple ISO8601 heuristic)
            // ABNF: digits "-" digits "-" ...
            if (text.length() >= 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
                return new Token(TokenType.DATETIME, text, line);
            }

            // Number check
            // TODO
            if (text.matches("^[\\+\\-]?[0-9_]+(\\.[0-9_]+)?([eE][\\+\\-]?[0-9_]+)?$")) {
                if (text.contains(".") || text.contains("e") || text.contains("E")) {
                    return new Token(TokenType.FLOAT, text.replace("_", ""), line);
                } else {
                    return new Token(TokenType.INTEGER, text.replace("_", ""), line);
                }
            }

            // Fallback to Bare Key
            return new Token(TokenType.BARE_KEY, text, line);
        }

        private boolean isBareKeyChar(char c) {
            return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '+' || c == ':';
            // Note: + and : included to eagerly capture dates/numbers, then refined later
        }
    }

    private final Lexer lexer;
    private Token currentToken;

    private final Map<String, Object> root = new LinkedHashMap<>();
    private Map<String, Object> currentTableContext;

    public Toml(String input) {
        this.lexer = new Lexer(input);
        this.currentTableContext = root;
        consume();
    }

    private void consume() {
        currentToken = lexer.nextToken();
    }

    private boolean match(TokenType type) {
        if (currentToken.type == type) {
            consume();
            return true;
        }
        return false;
    }

    private void expect(TokenType type) {
        if (currentToken.type != type) {
            throw new IllegalStateException("Expected " + type + " but got " + currentToken);
        }
        consume();
    }

    public Map<String, Object> parse() {
        // ABNF: toml = expression *( newline expression )
        while (currentToken.type != TokenType.EOF) {
            if (currentToken.type == TokenType.LBRACKET) {
                parseTableDefinition();
            } else {
                parseKeyVal();
            }
        }
        return root;
    }

    // [table] or [[array.of.tables]]
    private void parseTableDefinition() {
        expect(TokenType.LBRACKET);

        boolean isArrayTable = match(TokenType.LBRACKET);

        List<String> keys = parseKey();

        if (isArrayTable) {
            expect(TokenType.RBRACKET);
        }
        expect(TokenType.RBRACKET);

        this.currentTableContext = resolveTablePath(keys, isArrayTable);
    }

    // key = value
    private void parseKeyVal() {
        List<String> keys = parseKey();
        expect(TokenType.EQUALS);
        Object value = parseValue();

        Map<String, Object> targetMap = resolveKeyPath(currentTableContext, keys);
        String finalKey = keys.getLast();

        if (targetMap.containsKey(finalKey)) {
            throw new IllegalStateException("Duplicate key: " + finalKey);
        }
        targetMap.put(finalKey, value);
    }

    // key (dotted.key supported)
    private List<String> parseKey() {
        List<String> keys = new ArrayList<>();
        do {
            if (currentToken.type == TokenType.BARE_KEY || currentToken.type == TokenType.STRING || currentToken.type == TokenType.INTEGER) {
                keys.add(currentToken.value);
                consume();
            } else {
                throw new IllegalStateException("Invalid key format: " + currentToken);
            }
        } while (match(TokenType.DOT));
        return keys;
    }

    // values
    private Object parseValue() {
        Token t = currentToken;
        consume();

        switch (t.type) {
            case STRING: return t.value;
            case INTEGER: return Long.parseLong(t.value);
            case FLOAT: return Double.parseDouble(t.value);
            case BOOLEAN: return Boolean.parseBoolean(t.value);
            case DATETIME: return parseDateTime(t.value);
            case LBRACKET: return parseArray(); // Array starts with [
            case LBRACE: return parseInlineTable(); // Inline table starts with {
            default:
                throw new IllegalStateException("Unexpected value token: " + t);
        }
    }

    // Array [ v, v, v ]
    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        while (currentToken.type != TokenType.RBRACKET) {
            list.add(parseValue());
            if (!match(TokenType.COMMA)) {
                break;
            }
        }
        expect(TokenType.RBRACKET);
        return list;
    }

    // Inline Table { a = 1, b = 2 }
    private Map<String, Object> parseInlineTable() {
        // { is already consumed
        Map<String, Object> map = new LinkedHashMap<>();
        while (currentToken.type != TokenType.RBRACE) {
            List<String> keys = parseKey();
            expect(TokenType.EQUALS);
            Object value = parseValue();

            // Inline table doesn't usually support deep dotted keys inside, but let's be simple
            if (keys.size() > 1) throw new IllegalStateException("Dotted keys not strictly supported in simple inline table parser");
            map.put(keys.getFirst(), value);

            if (!match(TokenType.COMMA)) {
                break;
            }
        }
        expect(TokenType.RBRACE);
        return map;
    }

    // --- Helper Logic for Path Resolution ---

    // [a.b.c] -> creates path from root, returns the map 'c'
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveTablePath(List<String> keys, boolean isArrayOfTables) {
        Map<String, Object> current = root;

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            boolean isLast = (i == keys.size() - 1);

            if (isLast && isArrayOfTables) {
                // Array of Tables [[products]]
                if (!current.containsKey(key)) {
                    current.put(key, new ArrayList<Map<String, Object>>());
                }
                List<Map<String, Object>> list = (List<Map<String, Object>>) current.get(key);
                Map<String, Object> newTable = new LinkedHashMap<>();
                list.add(newTable);
                return newTable;
            } else {
                // Standard Table [table] or parent of array table
                // If it exists as a List (Array of Tables), we grab the LAST element
                if (current.containsKey(key) && current.get(key) instanceof List) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) current.get(key);
                    if (isLast) {
                        // Conflict or trying to extend? ABNF implies strict rules here.
                        // For simplicity: return last table in array
                        return list.getLast();
                    }
                    current = list.getLast();
                } else {
                    // Regular map
                    current.putIfAbsent(key, new LinkedHashMap<String, Object>());
                    current = (Map<String, Object>) current.get(key);
                }
            }
        }
        return current;
    }

    // a.b.c = 1 -> creates 'a' and 'b' in the current context, returns 'b'
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveKeyPath(Map<String, Object> context, List<String> keys) {
        Map<String, Object> current = context;
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            current.putIfAbsent(key, new LinkedHashMap<String, Object>());
            current = (Map<String, Object>) current.get(key);
        }
        return current;
    }

    private Object parseDateTime(String text) {
        try {
            if (text.contains("T") || text.contains("t") || text.contains(" ")) {
                return LocalDateTime.parse(text.replace(" ", "T"), DateTimeFormatter.ISO_DATE_TIME);
            } else if (text.contains(":")) {
                return LocalTime.parse(text);
            } else {
                return LocalDate.parse(text);
            }
        } catch (Exception e) {
            return text; // Fallback to string if parsing fails
        }
    }
}
