package com.github.ingarabr.mi.mapper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import org.junit.Test;

public class CodahaleMetricV3MapperTest {

    private CodahaleMetricV3Mapper mapper = new CodahaleMetricV3Mapper();

    @Test
    public void shouldMapToCustomFormat() throws Exception {
        InputStream is = getClass().getResourceAsStream("/codahale_metric_v3.json");

        List<String> map = mapper.map(getStringFromIs(is), ImmutableMap.of("host", "localhost"));

        assertThat(map.get(0), not(containsString("gauges\":{\"gc.PS-MarkSweep.count\"")));
        assertThat(map.get(0), containsString("\"name\":\"gc.PS-MarkSweep.count\""));
    }

    @Test(expected = RuntimeException.class)
    public void shouldCastExceptionIfTheVersionIsWrong() throws Exception {
        mapper.map("{\"version\": \"2.0.0\"}", ImmutableMap.of("host", "localhost"));
    }

    private String getStringFromIs(final InputStream is) throws IOException {
        return CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
            public InputStream getInput() throws IOException {
                return is;
            }
        }, Charsets.UTF_8));
    }

}
