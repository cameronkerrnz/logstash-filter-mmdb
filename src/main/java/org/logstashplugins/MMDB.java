package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import co.elastic.logstash.api.PluginHelper;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.maxmind.db.Reader;
import com.maxmind.db.DatabaseRecord;
import com.maxmind.db.Metadata;
import com.maxmind.db.CHMCache;
import com.maxmind.db.NoCache;
import com.maxmind.db.NodeCache;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

// class name must match plugin name
@LogstashPlugin(name = "mmdb")
public class MMDB implements Filter {

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            PluginConfigSpec.requiredStringSetting("source");
    public static final PluginConfigSpec<String> TARGET_CONFIG =
            PluginConfigSpec.requiredStringSetting("target");
    public static final PluginConfigSpec<String> DATABASE_FILENAME_CONFIG =
            PluginConfigSpec.requiredStringSetting("database");
    public static final PluginConfigSpec<Long> CACHE_SIZE_CONFIG =
            PluginConfigSpec.numSetting("cache_size", 0L);
    public static final PluginConfigSpec<List<Object>> FIELDS_CONFIG =
            PluginConfigSpec.arraySetting("fields");
    
    private String id;
    private String sourceField;
    private String targetField;
    private Reader databaseReader;
    private String databaseFilename;
    private String failureTag = "_mmdb_lookup_failure";
    private List<String> fields;

    private NodeCache cache;

    public MMDB(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.sourceField = config.get(SOURCE_CONFIG);
        this.targetField = config.get(TARGET_CONFIG);
        this.databaseFilename = config.get(DATABASE_FILENAME_CONFIG);
        this.fields = null; // null = all fields to be exported

        if (this.databaseFilename == null) {
            throw new IllegalStateException("Must specify database filename");
        }
        
        if (this.sourceField == null) {
            throw new IllegalStateException("Must specify source field");
        }

        if (this.targetField == null) {
            throw new IllegalStateException("Must specify target field");
        }

        List<Object> fieldsTmp = config.get(FIELDS_CONFIG);
        if (fieldsTmp != null) {
            this.fields = new ArrayList<String>();
            for (Object o : fieldsTmp) {
                if (o instanceof String) {
                    this.fields.add( (String) o );
                } else {
                    throw new IllegalStateException("Fields config must only be a list of strings");
                }
            }
        }

        if (config.get(CACHE_SIZE_CONFIG) > 0L) {
            this.cache = new CHMCache(config.get(CACHE_SIZE_CONFIG).intValue());
        } else if (config.get(CACHE_SIZE_CONFIG) == 0L) {
            this.cache = NoCache.getInstance();
        } else {
            throw new IllegalStateException("Cache size must be either >0 to use a cache, or =0 to use no cache");
        }

        File databaseFile = new File(this.databaseFilename);
        try {
            this.databaseReader = new Reader(databaseFile, cache);
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
    private void renderMapIntoEvent(
        Map<String, Object> fields,
        Event e
    ) {
        for (Map.Entry<String, Object> field : fields.entrySet()) {

            if (this.fields != null) {
                if (! this.fields.contains(field.getKey())) {
                    continue;
                }
            }

            String key = "[" + this.targetField + "][" + field.getKey() + "]";

            if (field.getValue() instanceof String) {
                e.setField(key, (String) field.getValue());
            }
            
            else if (field.getValue() instanceof Long) {
                e.setField(key, (Long) field.getValue());
            }
            
            else if (field.getValue() instanceof Float) {
                e.setField(key, (Float) field.getValue());
            }

            else if (field.getValue() instanceof Boolean) {
                e.setField(key, (Boolean) field.getValue());
            }

            // FIXME: Should we support lists and objects?
            else {
                e.tag(this.failureTag);
            }
        }
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            try {
                @SuppressWarnings("unchecked")
                Map<String,Object> recordData = this.databaseReader.get(
                    InetAddress.getByName(
                        e.getField(this.sourceField).toString()),
                    Map.class);

                if (null == recordData) {
                    e.tag(this.failureTag);
                    continue;
                }

                renderMapIntoEvent(recordData, e);

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
        
        // The Java example I was looking at doesn't tell
        // you that you need to include the common config
        // too, nor does it show how.
        // 
        // This form of commonFilterSettings, with an
        // argument, will merge the provided settings with
        // the common ones for filter.
        //
        // Note that the checking of arguments is not done
        // when we run the unit-tests; that's not our
        // code. You may therefore encounter this during
        // integration testing instead.

        return PluginHelper.commonFilterSettings(
            Arrays.asList(
                SOURCE_CONFIG,
                TARGET_CONFIG,
                DATABASE_FILENAME_CONFIG,
                CACHE_SIZE_CONFIG,
                FIELDS_CONFIG));
    }

    @Override
    public String getId() {
        return this.id;
    }
}
