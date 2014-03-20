package com.github.ingarabr.mi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ServerService extends Service<ServerConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);

    private final Client client;
    private final ArrayList<Timer> tasks = new ArrayList<Timer>();

    public static void main(String[] args) throws Exception {
        String[] serverArgs = new String[args.length + 2];
        int i = 0;
        serverArgs[i++] = "server";
        serverArgs[i++] = "server.yml";
        for (String arg : args) {
            i++;
            serverArgs[i] = arg;
        }
        new ServerService().run(serverArgs);
    }

    public ServerService() {
        Node node = NodeBuilder.nodeBuilder().settings(ImmutableSettings.builder()).build();
        node.start();
        client = node.client();
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.setName("metric-insights");
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(new MyResource(client));

        for (ServerConfiguration.RestFetcher restFetcher : configuration.getRestFetchers()) {
            createTimer(restFetcher, configuration.getDefaultInterval());
        }

        createIndex(client);
    }

    private void createIndex(Client client) {
        String indexName = "metrics";
        String type = "metric";
        try {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                    .startObject(type)
                        .startObject("properties")
                            .startObject("@timestamp")
                                .field("type", "date")
                            .endObject()
                            .startObject("application")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("environment")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("host")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();

            if (client.admin().indices().prepareExists(indexName).get().isExists()) {
                client.admin().indices().preparePutMapping(indexName).setType(type).setSource(builder).get();
            } else {
                client.admin().indices().prepareCreate(indexName).addMapping(type, builder).get();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createTimer(ServerConfiguration.RestFetcher restFetcher, Integer defaultInterval) {
        Integer interval = restFetcher.getInterval();
        if (interval == null) {
            interval = defaultInterval;
        }

        logger.info("Creating timer task to fetch data from {} with interval {}", restFetcher.getHost(), interval);
        Timer fetcher = new Timer("fetcher", true);
        fetcher.schedule(new DataHenter(client, new RestClient(restFetcher.getHost()), restFetcher.getTags()), 1000, interval);
        tasks.add(fetcher);
    }

}
