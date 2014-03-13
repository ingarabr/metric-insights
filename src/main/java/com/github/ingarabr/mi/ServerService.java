package com.github.ingarabr.mi;

import java.util.Timer;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class ServerService extends Service<ServerConfiguration> {

    private final Client client;

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

        Timer fetcher = new Timer("fetcher", true);
        fetcher.schedule(new DataHenter(client), 10, 1000);
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.setName("metric-insights");
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(new MyResource(client));
    }

}
