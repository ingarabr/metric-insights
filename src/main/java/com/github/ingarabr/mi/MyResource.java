package com.github.ingarabr.mi;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;


@Path("/")
public class MyResource {

    private final Client client;

    public MyResource(Client client) {
        this.client = client;
    }

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }

    @POST
    @Path("/data/{index}")
    public Response put(@PathParam("index") String index, @QueryParam("type") String type, String json) {
        IndexResponse response = client.prepareIndex(index, type).setSource(json).get();
        if (response.isCreated()) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }
}
