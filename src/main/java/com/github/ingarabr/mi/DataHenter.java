package com.github.ingarabr.mi;

import java.util.TimerTask;

import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataHenter extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(DataHenter.class);

    private final Client esClient;

    public DataHenter(Client esClient) {
        this.esClient = esClient;
    }

    @Override
    public void run() {
        try {
            logger.debug("Fetching....");
            com.sun.jersey.api.client.Client httpClient = com.sun.jersey.api.client.Client.create();
            DefaultClientConfig config = new DefaultClientConfig();
            config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

            ObjectMapper objectMapper = new ObjectMapper();
            String metrics = httpClient.resource("http://localhost/metrics").get(String.class);
            JsonNode jsonNode = objectMapper.readTree(metrics);

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("@timestamp", System.currentTimeMillis());
            objectNode.put("metrics", jsonNode);
            String toStore = objectMapper.writeValueAsString(objectNode);
            esClient.prepareIndex("metrics", "metric").setSource(toStore).get();
        } catch (Exception e) {
            logger.debug("Unexpected error.", e);
        }
    }
}
