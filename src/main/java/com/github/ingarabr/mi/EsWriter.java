package com.github.ingarabr.mi;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.elasticsearch.client.Client;

public class EsWriter implements Runnable {

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
