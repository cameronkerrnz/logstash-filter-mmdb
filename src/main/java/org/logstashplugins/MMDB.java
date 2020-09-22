package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.maxmind.db.Reader;
import com.maxmind.db.Record;
import com.maxmind.db.Metadata;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

// class name must match plugin name
@LogstashPlugin(name = "mmdb")
public class MMDB implements Filter {

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            PluginConfigSpec.stringSetting("source", "message");
    public static final PluginConfigSpec<String> TARGET_CONFIG =
            PluginConfigSpec.stringSetting("target", "mmdb");
    public static final PluginConfigSpec<String> DATABASE_FILENAME_CONFIG =
            PluginConfigSpec.stringSetting("database", null);

    private String id;
    private String sourceField;
    private String targetField;
    private Reader databaseReader;
    private String databaseFilename;
    private String failureTag = "_mmdb_lookup_failure";

    public MMDB(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.sourceField = config.get(SOURCE_CONFIG);
        this.targetField = config.get(TARGET_CONFIG);
        this.databaseFilename = config.get(DATABASE_FILENAME_CONFIG);
        
        if (this.databaseFilename == null) {
            throw new IllegalStateException("Must specify database filename");
        }

        File databaseFile = new File(this.databaseFilename);
        try {
            this.databaseReader = new Reader(databaseFile); // with no caching (FIXME)
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Database does not appear to be a valid database");
        }

        context.getLogger(this).info(databaseReader.getMetadata().toString());
    }

    public Metadata getMetadata() {
        if (null == this.databaseReader) {
            return null;
        }
        return this.databaseReader.getMetadata();
    }

    // This assumes that the fields in the MMDB are a flat structure
    // AND only contain either numbers or strings
    //
    private void renderJsonNodeIntoEvent(
        Iterator<Map.Entry<String,JsonNode>> fields,
        Event e
    ) {
        while (fields.hasNext()) {
            Map.Entry<String,JsonNode> field = fields.next();

            String key = "[" + this.targetField + "][" + field.getKey() + "]";

            switch (field.getValue().getNodeType()) {

                // https://fasterxml.github.io/jackson-databind/javadoc/2.8/com/fasterxml/jackson/databind/node/JsonNodeType.html

                case STRING:
                    e.setField(key, field.getValue().textValue());
                    break;

                case NUMBER:
                    // This could be a short, int, long, float, double or bignum etc.
                    // Conversion will depend on what Logstash does
                    e.setField(key, field.getValue().numberValue());
                    break;

                default:
                    e.tag(this.failureTag);
            }
        }
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            try {
                JsonNode recordData = this.databaseReader.get(
                    InetAddress.getByName(
                        e.getField(this.sourceField).toString()));

                if (null == recordData) {
                    e.tag(this.failureTag);
                    continue;
                }

                Iterator<Map.Entry<String,JsonNode>> fields = recordData.fields();

                renderJsonNodeIntoEvent(fields, e);

                matchListener.filterMatched(e);

            } catch (java.net.UnknownHostException ex) {
                e.tag(this.failureTag);
                continue;
            } catch (IOException ex) {
                e.tag(this.failureTag);
                continue;
            }
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Arrays.asList(
            SOURCE_CONFIG,
            TARGET_CONFIG,
            DATABASE_FILENAME_CONFIG);
    }

    @Override
    public String getId() {
        return this.id;
    }
}
