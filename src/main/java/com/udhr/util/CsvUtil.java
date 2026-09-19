package com.udhr.util;

import java.util.List;
import java.util.stream.Collectors;

// Shared by every admin report's CSV export so quoting/escaping rules (and
// the risk of getting them subtly wrong) live in exactly one place.
public class CsvUtil {

    public static String buildCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(toCsvLine(headers));
        for (List<String> row : rows) {
            sb.append(toCsvLine(row));
        }
        return sb.toString();
    }

    private static String toCsvLine(List<String> fields) {
        return fields.stream().map(CsvUtil::escape).collect(Collectors.joining(",")) + "\n";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
