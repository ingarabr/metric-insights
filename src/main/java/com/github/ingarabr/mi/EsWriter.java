package com.github.ingarabr.mi;

import org.elasticsearch.client.Client;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EsWriter implements Runnable {

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
                    esClient.prepareIndex("metrics", "metric").setSource(metric).get();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log() {
        if (writeQueue.size() > 10) {
            System.out.println("ElasticSearch WriteQueue size: " + writeQueue.size());
        }
    }

    public void addMetric(String metric) {
        writeQueue.add(metric);
    }

    public void stop() {
        run = false;
    }
}
