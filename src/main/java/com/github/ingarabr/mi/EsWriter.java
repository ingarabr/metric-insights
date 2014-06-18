package com.github.ingarabr.mi;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EsWriter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EsWriter.class);
    private static final DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy.MM.dd");

    private final Client esClient;
    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<String>();

    private boolean run = true;

    public EsWriter(Client esClient) {
        this.esClient = esClient;
    }

    public void run() {
        try {
            while (run) {
                String metric = writeQueue.take();
                log();
                if (metric != null) {
                    String indexName = "metrics-" + DateTime.now(DateTimeZone.UTC).toString(DTF);
                    esClient.prepareIndex(indexName, "metric").setSource(metric).get();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
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
