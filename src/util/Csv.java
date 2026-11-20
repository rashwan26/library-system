package util;

import java.util.*;

/** Single-line CSV with quoted commas, quotes and trailing empty fields. */
public final class Csv {
    private Csv() {}
    public static String encode(Object... fields) {
        StringJoiner result = new StringJoiner(",");
        for (Object field : fields) {
            String value = field == null ? "" : field.toString();
            if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
                throw new IllegalArgumentException("Fields cannot contain line breaks.");
            if (value.contains(",") || value.contains("\""))
                value = "\"" + value.replace("\"", "\"\"") + "\"";
            result.add(value);
        }
        return result.toString();
    }
    public static String[] parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\r' || c == '\n') throw new IllegalArgumentException("Unexpected line break.");
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else field.append(c);
            } else if (c == ',') {
                fields.add(field.toString()); field.setLength(0); closed = false;
            } else if (c == '"' && field.length() == 0 && !closed) quoted = true;
            else {
                if (closed || c == '"') throw new IllegalArgumentException("Malformed CSV field.");
                field.append(c);
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote.");
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
