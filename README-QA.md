# You must provide your own sample data for QA performance tests

The testing data I used came from a production environment and is not suitable
for sharing publicly.

Depending on your use-case you may prefer to obtain sample data in a variety of
ways; one thing to consider is the effect on IP locality and the liklihood of a
cache hit. Behaviour will likely be quite different when querying client IPs
from a public website compared to syslog traffic inside a datacentre where most
things will have either a private address range or appear in a small range of
public addresses.

## Example: getting IPs from log files

If you don't already have parsed logs (eg. using grok on existing log files),
then you could simply just grep for any IPs that appear in a log file.

Here's one example of that; the regular expression doesn't need to be terribly
complex, depending on the log file used. In this example, I have a very large
daily file containing both public IPs and private IPs.

Because the log file is so large, I only need a suitable portion to get a
representative sample; I want this for performance analysis, so better to take
too much rather than too less, so I'm taking the first million lines. I'll grep
out each instance of what looks like an IPv4 address; there may be multiple per
line; and then take the first million of those.

We will emit metrics every 100,000 rows processed.

```sh
head -n1000000 big-log-file \
  | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' \
  | head -n1000000 > sample-ips.txt
```

You'll end up with one IP per line, which is perfect for doing a simple stdin
input with a 'plain' codec.

## Running the metrics tests

First build the container image:

    PS> docker build -t logstash-filter-mmdb:qa -f .\Dockerfile.qa .

Then you can run it either using the internal 'demo' data, which is only useful
as a brief smoke-test of the container itself:


Or you provide the input data (IPs and MMDB) as mapped into the container:

(Note the below is in Powershell, and ` is like \ in Bash)

```
PS> docker run --name qa --rm -it `
    -v ${PWD}/../private-test-data:/qa/inputs `
    logstash-filter-mmdb:qa `
    /qa/inputs/ip-sampling.log /qa/inputs/uoo-addressing.mmdb
```

The key lines of output will the ones that look like the following:

    Processed 99375 events in 70ms: 0.7μs / event

Note that performance will get better as more records are processed
due to the JVM Hotspot compilation etc.

You'll also notice a spot-check record being emitted every 20k rows.
If you're running docker with a TTY (ie. `-it`), then these lines
will be dimmed on your terminal so they don't get in the way of the
primary lines of interest.
