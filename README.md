metric-insights
===============

Gathering metrics from applications and stores them in ElasticSearch. Uses kibana to query and graph the metrics.
The metric data must be a json structure.


Create a single jar with dependencies:
```bash
mvn clean install -Ponejar
```


Configuration example:

```yml
http:
  rootPath: /service/*
defaultInterval: 60000
restFetchers:
  - host: http://localhost/metrics
    interval: 10000
    mapper: default|codahale_v3
    tags:
      application: metric-insight
      host: localhost
  - host: http://example/metrics
```