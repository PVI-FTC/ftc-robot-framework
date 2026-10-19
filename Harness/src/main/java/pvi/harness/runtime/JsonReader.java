package pvi.harness.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader, so the harness can load saved run recordings (and tests can check
 * saved files) without adding a library.
 *
 * <p>Objects become {@code LinkedHashMap<String, Object>} (keeping key order), arrays become
 * {@code List<Object>}, numbers become {@code Double}, plus {@code String}, {@code Boolean}
 * and {@code null}. Anything that isn't valid JSON throws {@link IllegalArgumentException}
 * saying where the problem is.</p>
 */
final class JsonReader {
    private final String text;
    private int at;

    private JsonReader(String text) {
        this.text = text;
    }

    /** Reads one JSON value that must fill the whole text (spaces around it are fine). */
    static Object parse(String text) {
        JsonReader reader = new JsonReader(text);
        Object value = reader.value();
        reader.skipSpaces();
        if (reader.at != text.length()) {
            throw reader.error("extra text after the JSON value");
        }
        return value;
    }

    private Object value() {
        skipSpaces();
        if (at >= text.length()) {
            throw error("unexpected end of text");
        }
        char c = text.charAt(at);
        if (c == '{') {
            return object();
        } else if (c == '[') {
            return array();
        } else if (c == '"') {
            return string();
        } else if (text.startsWith("true", at)) {
            at += 4;
            return Boolean.TRUE;
        } else if (text.startsWith("false", at)) {
            at += 5;
            return Boolean.FALSE;
        } else if (text.startsWith("null", at)) {
            at += 4;
            return null;
        }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        at++; // {
        skipSpaces();
        if (peek() == '}') {
            at++;
            return map;
        }
        while (true) {
            skipSpaces();
            String key = string();
            skipSpaces();
            expect(':');
            if (map.put(key, value()) != null) {
                throw error("duplicate key \"" + key + "\"");
            }
            skipSpaces();
            if (peek() == ',') {
                at++;
            } else {
                expect('}');
                return map;
            }
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        at++; // [
        skipSpaces();
        if (peek() == ']') {
            at++;
            return list;
        }
        while (true) {
            list.add(value());
            skipSpaces();
            if (peek() == ',') {
                at++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (at >= text.length()) {
                throw error("unfinished string");
            }
            char c = text.charAt(at++);
            if (c == '"') {
                return out.toString();
            } else if (c < 0x20) {
                throw error("raw control character in a string");
            } else if (c == '\\') {
                char e = text.charAt(at++);
                switch (e) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw error("bad escape \\" + e);
                }
            } else {
                out.append(c);
            }
        }
    }

    private Double number() {
        int start = at;
        while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
        String digits = text.substring(start, at);
        if (!digits.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
            at = start;
            throw error("not a JSON value");
        }
        return Double.valueOf(digits);
    }

    private void skipSpaces() {
        while (at < text.length() && " \t\r\n".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
    }

    private char peek() {
        return at < text.length() ? text.charAt(at) : '\0';
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("expected '" + c + "'");
        }
        at++;
    }

    private IllegalArgumentException error(String problem) {
        int from = Math.max(0, at - 20);
        int to = Math.min(text.length(), at + 20);
        return new IllegalArgumentException("Invalid JSON at character " + at + ": " + problem
                + " near \"" + text.substring(from, to) + "\"");
    }
}
