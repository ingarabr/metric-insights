package com.github.ingarabr.mi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

import com.github.ingarabr.mi.mapper.CodahaleMetricV3Mapper;
import com.github.ingarabr.mi.mapper.DefaultMapper;
import com.github.ingarabr.mi.mapper.MetricMapper;
import com.github.ingarabr.mi.servlet.ElasticSearchHttpServlet;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
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

    private static final String DEFAULT_MAPPER = "default";
    private static final ImmutableMap<String, MetricMapper> MAPPERS = ImmutableMap.of(
            DEFAULT_MAPPER, new DefaultMapper(),
            "codahale_v3", new CodahaleMetricV3Mapper()
    );

    private final ArrayList<Timer> tasks = new ArrayList<>();
    private final EsWriter esWriter;
    private Node node;

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
        esWriter = new EsWriter();
    }

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.setName("metric-insights");
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception {
        for (ServerConfiguration.RestFetcher restFetcher : configuration.getRestFetchers()) {
            createTimer(restFetcher, configuration.getDefaultInterval());
        }

        logger.info("Setting up elasticSearch");
        node = NodeBuilder.nodeBuilder()
                .settings(ImmutableSettings.builder()
                                .put("http.enabled", false)
                                .put("node.local", true)
                                .put("discovery.zen.ping.multicast.enabled", false)
                )
                .build();
        node.start();
        environment.addResource(new MyResource(node.client()));

        ElasticSearchHttpServlet elasticSearchHttpServlet = new ElasticSearchHttpServlet(node);
        environment.addServlet(elasticSearchHttpServlet, "/es/*");
        createIndexTemplate(node.client());

        esWriter.setEsClient(node.client());
        new Thread(esWriter).start();

        logger.info("Metric insight is ready");
        shutdownHook();
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                for (Timer task : tasks) {
                    task.cancel();
                }
                esWriter.stop();
                if (node != null) {
                    node.stop();
                }
            }
        }));
    }

    private void createIndexTemplate(Client client) {
        try (InputStream is = getClass().getResourceAsStream("/config/metricTemplate.json")) {
            String template = CharStreams.toString(new InputStreamReader(is));
            client.admin().indices().preparePutTemplate("metricstemplate").setSource(template).execute().get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            logger.error("Failed to create template", e);
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
