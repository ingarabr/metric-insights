package com.github.ingarabr.mi.mapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

public class DefaultMapper implements MetricMapper {

    public List<String> map(String metrics, Map<String, String> tags) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(metrics);

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("@timestamp", System.currentTimeMillis());
            objectNode.put("metrics", jsonNode);
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                objectNode.put(tag.getKey(), tag.getValue());
            }
            return ImmutableList.of(objectMapper.writeValueAsString(objectNode));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create metrics", e);
        }
    }

}
