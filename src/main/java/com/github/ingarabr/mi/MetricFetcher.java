package com.github.ingarabr.mi;

import java.util.Map;
import java.util.TimerTask;

import com.github.ingarabr.mi.mapper.MetricMapper;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricFetcher extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(MetricFetcher.class);

    private final EsWriter esClient;
    private final RestClient restClient;
    private final Map<String, String> tags;
    private final MetricMapper mapper;

    public MetricFetcher(EsWriter esClient, RestClient restClient, Map<String, String> tags, MetricMapper mapper) {
        this.esClient = esClient;
        this.restClient = restClient;
        this.tags = tags;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        try {
            String metrics = restClient.fetchData();
            for (String toStore : mapper.map(metrics, tags)) {
                esClient.addMetric(toStore);
            }
        } catch (ClientHandlerException | UniformInterfaceException e ) {
            logger.info("Failed to fetch {}, {}", restClient.getPath(), e.getMessage());
        } catch (Exception e) {
            logger.warn("Unexpected error on {}", restClient.toString(), e);
        }
    }

}
