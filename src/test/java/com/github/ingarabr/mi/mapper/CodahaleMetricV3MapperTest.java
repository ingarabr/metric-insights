package com.github.ingarabr.mi.mapper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import com.google.common.io.Resources;
import org.junit.Test;

public class CodahaleMetricV3MapperTest {

    private CodahaleMetricV3Mapper mapper = new CodahaleMetricV3Mapper();

    @Test
    public void shouldMapToCustomFormat() throws Exception {
        String metric = getResourceAsString();

        List<String> map = mapper.map(metric, ImmutableMap.of("host", "localhost"));

        assertThat(map.get(0), not(containsString("gauges\":{\"gc.PS-MarkSweep.count\"")));
        assertThat(map.get(0), containsString("\"name\":\"gc.PS-MarkSweep.count\""));
    }

    @Test(expected = RuntimeException.class)
    public void shouldCastExceptionIfTheVersionIsWrong() throws Exception {
        mapper.map("{\"version\": \"2.0.0\"}", ImmutableMap.of("host", "localhost"));
    }

    private String getResourceAsString() throws IOException {
        URL resource = Resources.getResource("codahale_metric_v3.json");
        return Resources.toString(resource, Charsets.UTF_8);
    }

}
