package com.github.ingarabr.mi;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import java.util.concurrent.TimeUnit;


@Path("/")
public class MyResource {

    private final Client client;
    private final ObjectMapper metricsMapper;
    private final MetricRegistry metricRegistry;

    public MyResource(Client client, MetricRegistry metricRegistry) {
        this.client = client;
        this.metricRegistry = metricRegistry;
        metricsMapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
    }

    @GET
    @Path("ping")
    public String ping() {
        return "pong";
    }

    @POST
    @Path("data/{index}")
    public Response put(@PathParam("index") String index, @QueryParam("type") String type, String json) {
        IndexResponse response = client.prepareIndex(index, type).setSource(json).get();
        if (response.isCreated()) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("metrics")
    public Response metrics() throws JsonProcessingException {
        return Response.ok()
                .entity(metricsMapper.writeValueAsString(metricRegistry))
                .header("Cache-Control", "must-revalidate,no-cache,no-store")
                .build();
    }
}
