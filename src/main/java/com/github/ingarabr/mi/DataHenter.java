package com.github.ingarabr.mi;

import java.util.TimerTask;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataHenter extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(DataHenter.class);

    private final Client esClient;
    private final RestClient restClient;

    public DataHenter(Client esClient, RestClient restClient) {
        this.esClient = esClient;
        this.restClient = restClient;
    }

    @Override
    public void run() {
        try {
            String metrics = restClient.fetchData();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(metrics);

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("@timestamp", System.currentTimeMillis());
            objectNode.put("metrics", jsonNode);
            String toStore = objectMapper.writeValueAsString(objectNode);
            esClient.prepareIndex("metrics", "metric").setSource(toStore).get();
        } catch (Exception e) {
            logger.warn("Unexpected error.", e);
        }
    }

}
