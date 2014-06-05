package com.github.ingarabr.mi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

import com.github.ingarabr.mi.mapper.CodahaleMetricV3Mapper;
import com.github.ingarabr.mi.mapper.DefaultMapper;
import com.github.ingarabr.mi.mapper.MetricMapper;
import com.github.ingarabr.mi.servlet.ElasticSearchHttpServlet;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
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

public class ServerService extends Service<ServerConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);

    private static final String DEFAULT_MAPPER = "default";
    private static final ImmutableMap<String, MetricMapper> MAPPERS = ImmutableMap.of(
            DEFAULT_MAPPER, new DefaultMapper(),
            "codahale_v3", new CodahaleMetricV3Mapper()
    );

    private final ArrayList<Timer> tasks = new ArrayList<Timer>();
    private final Node node;
    private EsWriter esWriter;

    public static void main(String[] args) throws Exception {
        String[] serverArgs = new String[args.length + 2];
        int i = 0;
        serverArgs[i++] = "server";
        serverArgs[i++] = "server.yml";
        for (String arg : args) {
            i++;
            serverArgs[i] = arg;
        }
    }

    public ServerService() {
        node = NodeBuilder.nodeBuilder()
                .settings(ImmutableSettings.builder()
                                .put("http.enabled", false)
                                .put("node.local", true)
                                .put("discovery.zen.ping.multicast.enabled", false)
                )
                .build();
        node.start();
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.setName("metric-insights");
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(new MyResource(node.client()));
        ElasticSearchHttpServlet elasticSearchHttpServlet = new ElasticSearchHttpServlet(node);
        environment.addServlet(elasticSearchHttpServlet, "/es/*");

        createIndex(node.client());
        esWriter = new EsWriter(node.client());
        for (ServerConfiguration.RestFetcher restFetcher : configuration.getRestFetchers()) {
            createTimer(restFetcher, configuration.getDefaultInterval());
        }
        new Thread(esWriter).start();

        shutdownHook();
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                for (Timer task : tasks) {
                    task.cancel();
                }
                if (esWriter != null) {
                    esWriter.stop();
                }
                node.stop();
            }
        }));
    }

    private void createIndex(Client client) {
        String indexName = "metrics";
        String type = "metric";
        try {
            XContentBuilder builder = ElasticSearchMapping.getConfig(type);

            if (client.admin().indices().prepareExists(indexName).get().isExists()) {
                client.admin().indices().preparePutMapping(indexName).setType(type).setSource(builder).get();
            } else {
                client.admin().indices().prepareCreate(indexName).addMapping(type, builder).get();
            }
        } catch (IOException e) {
            logger.warn("Failed to create indices", e);
        }
    }

    private void createTimer(ServerConfiguration.RestFetcher restFetcher, Integer defaultInterval) {
        Integer interval = restFetcher.getInterval();
        if (interval == null) {
            interval = defaultInterval;
        }

        logger.info("Creating timer task to fetch data from {} with interval {}", restFetcher.getHost(), interval);
        Timer fetcher = new Timer("fetcher", true);
        MetricMapper mapper = Optional.fromNullable(MAPPERS.get(restFetcher.getMapper())).or(MAPPERS.get(DEFAULT_MAPPER));

        fetcher.schedule(new MetricFetcher(esWriter, new RestClient(restFetcher.getHost()), restFetcher.getTags(), mapper), 1000, interval);
        tasks.add(fetcher);
    }

}
