# Logstash MMDB Plugin

This is a MMDB filter plugin for [Logstash](https://github.com/elastic/logstash).
Using a tool such as [json-to-mmdb](https://github.com/cameronkerrnz/json-to-mmdb), you can
take the data that's inside your IP Address Management tool (eg. a spreadsheet) and use that
to get extra value from your logs, enabling you to do cool things with your internal network
ranges, such as:

- enrich an IP with the subnet address or VLAN ID or name
- enrich an IP with extra data that might represent which something significant, such as what type of traffic might be expected from that, or which part of the network it is coming from, such as Datacentre, DMZ, VPN, Wifi, Guest Wifi, Building X, Campus Y, etc.
- locate your private address ranges with coordinates or a geohash

This unlocks the aggregate on really-useful criteria:

- network logs relating to a particular VLAN perhaps?
- which parts of the network are we seeing this error on?
- show me web accesses from our VPN ranges
- are traffic flows still healthy for different parts of the network post-change?
- provides a useful feature for potential real-time machine-learning

You, as the MMDB database creator, are in total control of what goes into the MMDB and which fields
you want to extract inside of Logstash.
