package com.github.ingarabr.mi;

import java.util.ArrayList;
import java.util.Timer;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            createTimer(restFetcher);
        }
    }

    private void createTimer(ServerConfiguration.RestFetcher restFetcher) {
        logger.info("Creating timer task to fetch data from {} with interval {}", restFetcher.getHost(), restFetcher.getInterval());
        Timer fetcher = new Timer("fetcher", true);
        fetcher.schedule(new DataHenter(client, new RestClient(restFetcher.getHost())), 10, restFetcher.getInterval());
        tasks.add(fetcher);
    }

}
