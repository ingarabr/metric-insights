package com.github.ingarabr.mi.mapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class CodahaleMetricV3Mapper implements MetricMapper {

    public List<String> map(String metrics, Map<String, String> tags) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(metrics);
            verifyVersion(jsonNode);

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("@timestamp", System.currentTimeMillis());

            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.getFields();
            ObjectNode valueNode = objectMapper.createObjectNode();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                if (ImmutableList.of("counters", "gauges", "histograms", "meters", "timers").contains(next.getKey())) {
                    valueNode.put(next.getKey(), convert(next.getValue(), objectMapper));
                } else {
                    valueNode.put(next.getKey(), next.getValue());
                }
            }
            objectNode.put("metrics", valueNode);
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                objectNode.put(tag.getKey(), tag.getValue());
            }
            return ImmutableList.of(objectMapper.writeValueAsString(objectNode));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create metrics", e);
        }
    }

    private JsonNode convert(JsonNode value, ObjectMapper objectMapper) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        Iterator<Map.Entry<String, JsonNode>> fields = value.getFields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("name", next.getKey());

            Iterator<Map.Entry<String, JsonNode>> innter = next.getValue().getFields();
            while (innter.hasNext()) {
                Map.Entry<String, JsonNode> innerNext = innter.next();
                objectNode.put(innerNext.getKey(), innerNext.getValue());
            }

            arrayNode.add(objectNode);
        }
        return arrayNode;
    }

    private void verifyVersion(JsonNode jsonNode) {
        JsonNode version = jsonNode.get("version");
        if (!version.asText().startsWith("3.")) {
            throw new RuntimeException("Illegal version");
        }
    }

}
