package com.github.ingarabr.mi;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class RestClient {

    private final Client httpClient;
    private final String path;

    public RestClient(String path) {
        this.path = path;
        httpClient = Client.create();
        DefaultClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    }

    public String fetchData() {
        return httpClient.resource(path).get(String.class);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("%s [path: '%s']", getClass().getSimpleName(), path);
    }
}
