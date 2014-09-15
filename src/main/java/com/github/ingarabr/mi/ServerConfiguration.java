package com.github.ingarabr.mi;


import com.yammer.dropwizard.config.Configuration;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerConfiguration extends Configuration {

    @JsonProperty
    private List<RestFetcher> restFetchers;

    @JsonProperty
    private String esPath;

    @JsonProperty
    private Integer defaultInterval = 60000;

    private Es es = new Es();

    public List<RestFetcher> getRestFetchers() {
        return restFetchers;
    }

    public String getEsPath() {
        return esPath;
    }

    public Integer getDefaultInterval() {
        return defaultInterval;
    }

    public Es getEs() {
        return es;
    }

    public static class Es {
        @JsonProperty
        private boolean embedded = true;

        @JsonProperty
        private String clusterName;

        @JsonProperty
        private List<EsHost> hosts = new ArrayList<>();

        public boolean isEmbedded() {
            return embedded;
        }

        public List<EsHost> getHosts() {
            return hosts;
        }

        public String getClusterName() {
            return clusterName;
        }
    }

    public static class EsHost {
        @JsonProperty
        private String host;

        @JsonProperty
        private Integer port;

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }
    }

    public static class RestFetcher {

        @NotEmpty
        @JsonProperty
        private String host;
        @JsonProperty
        private Map<String, String> tags;
        @JsonProperty
        private Integer interval;
        @JsonProperty
        private String mapper;

        public String getHost() {
            return host;
        }

        public Integer getInterval() {
            return interval;
        }

        public String getMapper() {
            return mapper;
        }

        public Map<String, String> getTags() {
            if (tags != null) {
                return tags;
            }
            return Collections.emptyMap();
        }
    }

}
