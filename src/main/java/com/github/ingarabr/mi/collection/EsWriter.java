package com.github.ingarabr.mi.collection;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.elasticsearch.client.Client;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static com.codahale.metrics.MetricRegistry.name;

public class EsWriter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EsWriter.class);
    private static final DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy.MM.dd");

    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
    private final Timer putTimer;

    private Client esClient;

    private boolean run = true;

    public EsWriter(MetricRegistry metricRegistry) {
        putTimer = metricRegistry.timer(name(EsWriter.class, "put"));
    }

    public void setEsClient(Client esClient) {
        this.esClient = esClient;
    }

    public void run() {
        if (esClient == null) {
            throw new NullPointerException("Required ElasticSearch Client is null");
        }
        try {
            while (run) {
                String metric = null;
                try {
                    metric = writeQueue.take();
                    try (Timer.Context time = putTimer.time()) {
                        log();
                        if (metric != null) {
                            String indexName = "metrics-" + DateTime.now(DateTimeZone.UTC).toString(DTF);
                            esClient.prepareIndex(indexName, "metric").setSource(metric).get();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("error", e);
                    if (metric != null) {
                        writeQueue.put(metric);
                    }
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            logger.info("EsWriter interrupted", e);
        } catch (Exception e) {
            logger.error("Error in EsWriter", e);
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
