package com.github.ingarabr.mi;


import java.util.List;

import com.yammer.dropwizard.config.Configuration;

import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class ServerConfiguration extends Configuration {

    @JsonProperty
    private List<RestFetcher> restFetchers;

    @JsonProperty
    private String esPath;

    public List<RestFetcher> getRestFetchers() {
        return restFetchers;
    }

    public String getEsPath() {
        return esPath;
    }

    public static class RestFetcher {

        @NotEmpty
        @JsonProperty
        private String host;

        @JsonProperty
        private int interval = 10000;

        public String getHost() {
            return host;
        }

        public int getInterval() {
            return interval;
        }
    }

}
