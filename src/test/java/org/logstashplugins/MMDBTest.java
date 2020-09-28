package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.FilterMatchListener;
import org.logstash.plugins.ConfigurationImpl;
import org.logstash.plugins.ContextImpl;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.maxmind.db.Metadata;
import java.net.InetAddress;

public class MMDBTest {

    @Test
    public void testConfigRequiresDatabase() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        try {
            MMDB filter = new MMDB("test-id", config, context);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Must specify database filename"));
        }
    }

    @Test
    public void testConfigRequiresSource() {
        
        HashMap configMap = new HashMap();
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        try {
            MMDB filter = new MMDB("test-id", config, context);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Must specify source field"));
        }
    }

    @Test
    public void testConfigRequiresTarget() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "message");
        configMap.put("database", "samples/demo.mmdb");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        try {
            MMDB filter = new MMDB("test-id", config, context);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Must specify target field"));
        }
    }

    @Test
    public void testConfigCacheSizeMustNotBeNegative() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        configMap.put("cache_size", -1L);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        try {
            MMDB filter = new MMDB("test-id", config, context);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Cache size must be either >0 to use a cache, or =0 to use no cache"));
        }
    }

    @Test
    public void testRequiresValidDatabase() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        MMDB filter = new MMDB("test-id", config, context);
        assertThat(filter.getMetadata().getIpVersion(), is(4));
        assertThat(filter.getMetadata().getDatabaseType(), is("demo-network"));        
    }

    @Test
    public void testDemoBasic() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField("ip", "172.16.0.1");
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        assertNull(e.getField("tags"));
        assertThat(e.getField("[ip]"), is("172.16.0.1"));
        assertThat(e.getField("[info][subnet]"), is("172.16.0.0/12"));
        assertThat(e.getField("[info][name]"), is("DMZ"));
        assertThat(e.getField("[info][vlan_id]"), is(234L));
    }

    @Test
    public void testDemoBasicUnicode() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField("ip", "10.64.1.255");
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        assertNull(e.getField("tags"));
        assertThat(e.getField("[ip]"), is("10.64.1.255"));
        assertThat(e.getField("[info][subnet]"), is("10.64.0.0/23"));
        assertThat(e.getField("[info][name]"), is("Unicode NFKC Test (ﬃ)"));
        assertThat(e.getField("[info][vlan_id]"), is(234L));
    }

    @Test
    public void testDemoNoCache() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        configMap.put("cache_size", 0L);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField("ip", "172.16.0.1");
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        assertNull(e.getField("tags"));
        assertThat(e.getField("[ip]"), is("172.16.0.1"));
        assertThat(e.getField("[info][subnet]"), is("172.16.0.0/12"));
        assertThat(e.getField("[info][name]"), is("DMZ"));
        assertThat(e.getField("[info][vlan_id]"), is(234L));
    }

    @Test
    public void testCacheFill() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        configMap.put("cache_size", 4L);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        String ips[] = new String[] {
            "10.10.1.1", "10.10.1.2", "10.10.1.3", "10.10.1.4", "10.10.1.5"
        };

        TestMatchListener matchListener = new TestMatchListener();

        for (String ip : ips) {
            Event e = new org.logstash.Event();
            e.setField("ip", ip);
            Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

            assertNull(e.getField("tags"));
            assertThat(e.getField("ip"), is(ip));
        }
    }

    @Test
    public void testDemoFields() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        List configFields = new ArrayList<String>();
        configFields.add("name");
        configFields.add("vlan_id");
        configMap.put("fields", configFields);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField("ip", "172.16.0.1");
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        assertNull(e.getField("tags"));
        assertThat(e.getField("[ip]"), is("172.16.0.1"));
        assertNull(e.getField("[info][subnet]"));
        assertThat(e.getField("[info][name]"), is("DMZ"));
        assertThat(e.getField("[info][vlan_id]"), is(234L));
    }

    @Test
    public void testDemoFieldsWithInvalid() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        List configFields = new ArrayList<String>();
        configFields.add("name");
        configFields.add(123);
        configMap.put("fields", configFields);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);

        try {
            MMDB filter = new MMDB("test-id", config, context);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Fields config must only be a list of strings"));
        }
    }

    @Test
    public void testDemoFieldsEmpty() {
        
        HashMap configMap = new HashMap();
        configMap.put("source", "ip");
        configMap.put("target", "info");
        configMap.put("database", "samples/demo.mmdb");
        List configFields = new ArrayList<String>();
        configMap.put("fields", configFields);
        Configuration config = new ConfigurationImpl(configMap);
        Context context = new ContextImpl(null, null);
        MMDB filter = new MMDB("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField("ip", "172.16.0.1");
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        assertNull(e.getField("tags"));
        assertThat(e.getField("[ip]"), is("172.16.0.1"));
        assertNull(e.getField("[info]"));
    }

}

class TestMatchListener implements FilterMatchListener {

    private AtomicInteger matchCount = new AtomicInteger(0);

    @Override
    public void filterMatched(Event event) {
        matchCount.incrementAndGet();
    }

    public int getMatchCount() {
        return matchCount.get();
    }
}