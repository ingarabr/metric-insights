package com.github.ingarabr.mi.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CpuMetricSet implements MetricSet {

    private final OperatingSystemMXBean osBean;

    public CpuMetricSet() {
        this(ManagementFactory.getOperatingSystemMXBean());
    }

    public CpuMetricSet(OperatingSystemMXBean osBean) {
        this.osBean = osBean;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("system.load.avg", new Gauge<Double>() {
            @Override
            public Double getValue() {
                return osBean.getSystemLoadAverage();
            }
        });

        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            gauges.put("system.load.percent", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
                }
            });

            gauges.put("process.load.percent", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                }
            });
        }

        return Collections.unmodifiableMap(gauges);
    }
}
