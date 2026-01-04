package com.mammb.code.toml.impl;

import com.mammb.code.toml.TomlObjectBuilder;
import com.mammb.code.toml.TomlParser;
import com.mammb.code.toml.TomlValue;
import com.mammb.code.toml.TomlValue.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Objects;


class TomlParserImpl implements TomlParser {

    private Context currentContext = new NoneContext();
    private Event currentEvent;

    private final Stack stack;
    private final TomlTokenizer tokenizer;
    private boolean closed = false;

    TomlParserImpl(Reader reader, BufferPool bufferPool) {
        this.tokenizer = new TomlTokenizer(reader, bufferPool);
        this.stack = new Stack(1000); // TODO settings
    }

    TomlParserImpl(InputStream in, BufferPool bufferPool) {
        this(new InputStreamReader(in), bufferPool);
    }

    @Override
    public boolean hasNext() {
        if (stack.isEmpty() && (currentEvent != null && currentEvent.compareTo(Event.KEY_NAME) > 0)) {
            TomlToken token = tokenizer.nextToken();
            if (token != TomlToken.EOF) {
                throw new RuntimeException("Expected EOF token");
            }
            return false;
        } else if (!stack.isEmpty() && !tokenizer.hasNextToken()) {
            currentEvent = currentContext.getNextEvent();
            return false;
        }
        return true;
    }

    @Override
    public Event next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currentEvent = currentContext.getNextEvent();
    }

    @Override
    public TomlObject getObject() {
        if (currentEvent != Event.START_OBJECT) {
            throw new IllegalStateException("invalid parser state[%s].".formatted(currentEvent));
        }
        return getObject(new TomlObjectBuilder());
    }

    private TomlObject getObject(TomlObjectBuilder builder) {
        while (hasNext()) {
            TomlParser.Event e = next();
            if (e == Event.END_OBJECT) {
                return builder.build();
            }
            String key = getString();
            next();
            builder.add(key, getValue());
        }
        throw parsingException(TomlToken.EOF, "[STRING, CURLYCLOSE]");
    }

    public String getString() {
        if (currentEvent == Event.KEY_NAME || currentEvent == Event.VALUE_STRING ||
            currentEvent == Event.VALUE_INTEGER || currentEvent == Event.VALUE_FLOAT) {
            return tokenizer.getValue();
        }
        throw new IllegalStateException("invalid parser states[%s].".formatted(currentEvent));
    }

    public TomlValue getValue() {
        return switch (currentEvent) {
            // TODO
            case VALUE_TRUE -> TomlValue.TRUE;
            case VALUE_FALSE -> TomlValue.FALSE;
            default -> null;
        };
    }

    @Override
    public void close() {
        if (closed) return;
        try {
            tokenizer.close();
            closed = true;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while closing TOML tokenizer", e);
        }
    }

    // Using the optimized stack impl as we don't require other things
    // like iterator etc.
    private static final class Stack {
        int size = 0;
        final int limit;
        private Context head;

        Stack(int limit) {
            this.limit = limit;
        }

        private void push(Context context) {
            if (++size >= limit) {
                throw new RuntimeException("Input is too deeply nested %d".formatted(size));
            }
            context.next = head;
            head = context;
        }

        private Context pop() {
            if (head == null) {
                throw new NoSuchElementException();
            }
            size--;
            Context temp = head;
            head = head.next;
            return temp;
        }

        private boolean isEmpty() {
            return head == null;
        }
    }

    private abstract class Context {
        Context next;
        abstract Event getNextEvent();
        abstract void skip();

        protected Event nextEventIfValueOrObjectOrArrayStart(TomlToken token) {
            if (token.isValue()) {
                return switch (token) {
                    // TODO
                    case STRING -> Event.VALUE_STRING;
                    case INTEGER -> Event.VALUE_INTEGER;
                    case FLOAT -> Event.VALUE_FLOAT;
                    case TRUE -> Event.VALUE_TRUE;
                    case FALSE -> Event.VALUE_FALSE;
                    default -> null;
                };
            } else if (token == TomlToken.CURLYOPEN) {
                stack.push(currentContext);
                currentContext = new ObjectContext();
                return Event.START_OBJECT;
            } else if (token == TomlToken.SQUAREOPEN) {
                stack.push(currentContext);
                currentContext = new ArrayContext();
                return Event.START_ARRAY;
            }
            return null;
        }
    }

    private final class NoneContext extends Context {
        @Override
        public Event getNextEvent() {
            // Handle 1. {   2. [   3. value
            TomlToken token = tokenizer.nextToken();
            Event event = nextEventIfValueOrObjectOrArrayStart(token);
            if (event != null) {
                return event;
            }
            throw parsingException(token, "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]");
        }

        @Override
        void skip() {
            // no-op
        }
    }

    private abstract class SkippingContext extends Context {
        private final TomlToken openToken;
        private final TomlToken closeToken;
        private boolean firstValue = true;

        private SkippingContext(TomlToken openToken, TomlToken closeToken) {
            this.openToken = Objects.requireNonNull(openToken);
            this.closeToken = Objects.requireNonNull(closeToken);
        }

        @Override
        void skip() {
            TomlToken token;
            int depth = 1;
            do {
                token = tokenizer.nextToken();
                if (token == closeToken) {
                    depth--;
                }
                if (token == openToken) {
                    depth++;
                }
            } while (!(token == closeToken && depth == 0));
        }

        TomlToken firstValueOrJsonToken(TomlToken token) {
            if (firstValue) {
                firstValue = false;
            } else {
                if (token != TomlToken.COMMA) {
                    throw parsingException(token, "[COMMA]");
                }
                token = tokenizer.nextToken();
            }
            return token;
        }
    }

    private final class ObjectContext extends SkippingContext {
        private ObjectContext() {
            super(TomlToken.CURLYOPEN, TomlToken.CURLYCLOSE);
        }

        /*
         * Some more things could be optimized. For example, instead
         * tokenizer.nextToken(), one could use tokenizer.matchColonToken() to
         * match ':'. That might optimize a bit, but will fragment nextToken().
         * I think the current one is more readable.
         */
        @Override
        public Event getNextEvent() {
            // Handle 1. }   2. name:value   3. ,name:value
            TomlToken token = tokenizer.nextToken();
            if (token == TomlToken.EOF) {
                switch (currentEvent) {
                    case START_OBJECT:
                        throw parsingException(token, "[STRING, CURLYCLOSE]");
                    case KEY_NAME:
                        throw parsingException(token, "[COLON]");
                    default:
                        throw parsingException(token, "[COMMA, CURLYCLOSE]");
                }
            } else if (currentEvent == Event.KEY_NAME) {
                // Handle 1. :value
                if (token != TomlToken.EQUALS) {
                    throw parsingException(token, "[EQUALS]");
                }
                token = tokenizer.nextToken();
                Event event = nextEventIfValueOrObjectOrArrayStart(token);
                if (event != null) {
                    return event;
                }
                throw parsingException(token, "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]");
            } else {
                // Handle 1. }   2. name   3. ,name
                if (token == TomlToken.CURLYCLOSE) {
                    currentContext = stack.pop();
                    return Event.END_OBJECT;
                }

                token = firstValueOrJsonToken(token);
                if (token == TomlToken.STRING) {
                    return Event.KEY_NAME;
                }
                throw parsingException(token, "[STRING]");
            }
        }
    }

    private final class ArrayContext extends SkippingContext {

        private ArrayContext() {
            super(TomlToken.SQUAREOPEN, TomlToken.SQUARECLOSE);
        }

        // Handle 1. ]   2. value   3. ,value
        @Override
        public Event getNextEvent() {
            TomlToken token = tokenizer.nextToken();
            if (token == TomlToken.EOF) {
                throw parsingException(token, (Objects.requireNonNull(currentEvent) == Event.START_ARRAY) ?
                    "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]" : "[COMMA, CURLYCLOSE]");
            }
            if (token == TomlToken.SQUARECLOSE) {
                currentContext = stack.pop();
                return Event.END_ARRAY;
            }
            token = firstValueOrJsonToken(token);

            Event event = nextEventIfValueOrObjectOrArrayStart(token);
            if (event != null) {
                return event;
            }
            throw parsingException(token, "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]");
        }
    }

    private RuntimeException parsingException(TomlToken token, String expectedTokens) {
        // TODO
        return new RuntimeException();
    }

}
