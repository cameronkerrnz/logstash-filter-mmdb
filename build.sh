#!/bin/bash

echo LOGSTASH_CORE_PATH=/src/logstash/logstash-core > ./gradle.properties
docker run --rm -it -v $PWD:/work cameronkerrnz/logstash-plugin-dev:7.17 \
  -c "
    chmod +x ./gradlew \
    && ./gradlew assemble \
    && ./gradlew test \
    && ./gradlew gem \
    "

ls -ltr ./logstash-filter-mmdb-*.gem
