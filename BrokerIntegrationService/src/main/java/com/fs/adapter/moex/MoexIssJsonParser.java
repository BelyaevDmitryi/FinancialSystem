package com.fs.adapter.moex;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses MOEX ISS JSON blocks with {@code columns} + {@code data} arrays.
 */
public final class MoexIssJsonParser {

    private MoexIssJsonParser() {
    }

    public static Optional<Map<String, String>> parseFirstRow(JsonNode root, String blockName) {
        List<Map<String, String>> rows = parseAllRows(root, blockName);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public static List<Map<String, String>> parseAllRows(JsonNode root, String blockName) {
        if (root == null || !root.has(blockName)) {
            return Collections.emptyList();
        }
        JsonNode block = root.get(blockName);
        JsonNode columnsNode = block.get("columns");
        JsonNode dataNode = block.get("data");
        if (columnsNode == null || dataNode == null || !dataNode.isArray()) {
            return Collections.emptyList();
        }

        int columnCount = columnsNode.size();
        List<Map<String, String>> rows = new java.util.ArrayList<>();
        for (JsonNode rowNode : dataNode) {
            mapRow(columnsNode, columnCount, rowNode).ifPresent(rows::add);
        }
        return rows;
    }

    private static Optional<Map<String, String>> mapRow(JsonNode columnsNode, int columnCount, JsonNode rowNode) {
        if (!rowNode.isArray() || rowNode.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < columnCount && i < rowNode.size(); i++) {
            JsonNode value = rowNode.get(i);
            row.put(columnsNode.get(i).asText(), value.isNull() ? null : value.asText());
        }
        return Optional.of(row);
    }
}
