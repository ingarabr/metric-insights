package com.github.ingarabr.mi;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.yammer.dropwizard.config.Configuration;

import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class ServerConfiguration extends Configuration {

    @JsonProperty
    private List<RestFetcher> restFetchers;

    @JsonProperty
    private String esPath;

    @JsonProperty
    private Integer defaultInterval = 60000;

    public List<RestFetcher> getRestFetchers() {
        return restFetchers;
    }

    public String getEsPath() {
        return esPath;
    }

    public Integer getDefaultInterval() {
        return defaultInterval;
    }

    public static class RestFetcher {

        @NotEmpty
        @JsonProperty
        private String host;
        @JsonProperty
        private Map<String, String> tags;

        @JsonProperty
        private Integer interval;

        public String getHost() {
            return host;
        }

        public Integer getInterval() {
            return interval;
        }

        public Map<String, String> getTags() {
            if (tags != null) {
                return tags;
            }
            return Collections.emptyMap();
        }
    }

}
