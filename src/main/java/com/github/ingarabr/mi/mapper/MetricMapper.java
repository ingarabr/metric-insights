package com.github.ingarabr.mi.mapper;

import java.util.List;
import java.util.Map;

public interface MetricMapper {

    List<String> map(String metrics, Map<String, String> tags);

}
