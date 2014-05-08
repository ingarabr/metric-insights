package com.github.ingarabr.mi;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class ElasticSearchMapping {

    public static XContentBuilder getConfig(String type) throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                    .startObject(type)
                        .startObject("properties")
                            .startObject("@timestamp")
                                .field("type", "date")
                            .endObject()
                            .startObject("application")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("environment")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("host")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

}
