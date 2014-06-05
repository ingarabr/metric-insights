package com.github.ingarabr.mi;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsWriter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EsWriter.class);

    private final Client esClient;
    private final Queue<String> writeQueue = new LinkedBlockingQueue<String>();

    private boolean run = true;

    public EsWriter(Client esClient) {
        this.esClient = esClient;
    }

    public void run() {
        while (run) {
            String metric = writeQueue.poll();
            log();
            esClient.prepareIndex("metrics", "metric").setSource(metric).get();
        }
    }

    private void log() {
        if (writeQueue.size() > 10) {
            logger.debug("ElasticSearch WriteQueue size: {}", writeQueue.size());
        }
    }

    public void addMetric(String metric) {
        writeQueue.add(metric);
    }

    public void stop() {
        run = false;
    }

}
