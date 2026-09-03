---
layout: post
title: "mod_cluster vs WildFly clustering - when you need both"
date: 2026-09-03
categories: [wildfly, mod_cluster, clustering, java, load balancing]
excerpt: "mod_cluster and WildFly clustering solve different problems: one routes HTTP requests, while the other keeps session state available across nodes."
toc: true
---

## Contents
{: .no_toc }

* TOC
{:toc}

## WildFly clustering needs a client

Clustering has a lot to offer: cluster membership, failure detection and, for distributable web applications, replicated or distributed HTTP session state. If one WildFly node disappears, another node can have the state needed to continue processing the user's session.

There is one thing clustering does not provide, though: HTTP load balancing. Unfortunately, browsers are not WildFly-aware clients. They just keep sending HTTP requests to the address they were given.

Some WildFly clients can do exactly that. A remote EJB client, for example, can learn the cluster topology from WildFly and use it for routing and failover. An HTTP client cannot, so something else has to make that decision for it.

This is where a load balancer -- and mod_cluster -- enters the story.

## HTTP load balancing doesn't save your session

We established the need for a load balancer. Then, one might ask "It has _cluster_ in its name, why not use just mod_cluster on its own?".

A load balancer knows which nodes are available, which applications they host and how requests should be distributed between them taking into account the load reported by each node. With sticky sessions, it can also use the JVM route encoded in the session cookie to keep sending a client to the same node.

What it does not know is the application state stored inside that user's HTTP session.

Usually, users expect more than simply reaching another healthy server after a node failure. They expect to remain logged in, keep the items in their shopping cart and preserve other session-scoped state.

For that, we need session replication -- and that is where WildFly clustering comes back into the picture.

## First example: mod_cluster without session replication

Let’s start without distributable session state. We will use two WildFly nodes running the same web application and a mod_cluster load balancer in front of them.

```
HTTP client -> mod_cluster LB --> WildFly A
                              \-> WildFly B
```

The application keeps a counter in the HTTP session, but its deployment descriptor does not contain `<distributable/>`.

The test demonstrates two separate behaviors:

- mod_cluster keeps requests for a session on one worker while that worker is available;
- mod_cluster can route to the surviving worker after the original worker becomes unavailable, but it cannot preserve a non-distributable HTTP session.

All client requests use one URL:

```text
"http://$MOD_CLUSTER_IP:8080/session-counter/"
```

### Prerequisites

Install JDK 17 or newer, Maven, `curl`, and `unzip`.

#### Get the example application

The application used in this post is available in the examples/session-counter directory of this site's Git repository.

Clone the repository and switch to the example directory:

```bash
git clone https://github.com/honza-kasik/honza-kasik.github.io.git
cd honza-kasik.github.io/examples/session-counter
```

All commands below assume this directory as the current working directory.

#### Download WildFly and create three installations

Download the WildFly 41.0.1.Final ZIP from the WildFly release:

```bash
curl -fLO https://github.com/wildfly/wildfly/releases/download/41.0.1.Final/wildfly-41.0.1.Final.zip
unzip wildfly-41.0.1.Final.zip
```

Keep the extracted installation as the load balancer and make two copies for the application servers:

```bash
cp -R wildfly-41.0.1.Final node-a
cp -R wildfly-41.0.1.Final node-b
```

This produces the following layout:

```text
wildfly-41.0.1.Final/  load balancer
node-a/                application server A
node-b/                application server B
```

No WildFly XML files need to be edited. The load balancer uses WildFly's `standalone-load-balancer.xml` profile, while the workers use `standalone-ha.xml`.

#### Build and deploy the application

Build the default, non-distributable variant:

```bash
mvn clean package
cp target/session-counter.war node-a/standalone/deployments/
cp target/session-counter.war node-b/standalone/deployments/
```

The default WAR deliberately does not contain `<distributable/>`. Although the workers run the HA server profile, this application's HTTP session remains local to the worker that created it.

#### Choose the mod_cluster address

Set `MOD_CLUSTER_IP` to a non-loopback IP address of the machine running the load balancer. The address must belong to an active network interface. Set the variable in every terminal because each WildFly process is started separately.

```bash
export MOD_CLUSTER_IP=192.168.1.42
```

Replace `192.168.1.42` with the address of your machine. Choosing the address manually avoids relying on platform-specific commands or interface names such as `en0` or `eth0`.

### Start the load balancer

In terminal 1, set `MOD_CLUSTER_IP` and start WildFly with the load-balancer profile:

```bash
./wildfly-41.0.1.Final/bin/standalone.sh \
  --server-config=standalone-load-balancer.xml \
  -Djboss.node.name=load-balancer \
  -b "$MOD_CLUSTER_IP" \
  -bprivate "$MOD_CLUSTER_IP"
```

The client-facing HTTP listener is available at `$MOD_CLUSTER_IP:8080`. The MCMP management listener used by the workers is available at `$MOD_CLUSTER_IP:8090`.

### Start worker A

In terminal 2, set `MOD_CLUSTER_IP` and start the first worker:

```bash
./node-a/bin/standalone.sh \
  --server-config=standalone-ha.xml \
  -Djboss.node.name=node-a \
  -Djboss.socket.binding.port-offset=100 \
  -b "$MOD_CLUSTER_IP" \
  -bprivate "$MOD_CLUSTER_IP"
```

The port offset moves node A's HTTP listener to `$MOD_CLUSTER_IP:8180` and its management listener to `127.0.0.1:10090`.

### Start worker B

In terminal 3, set `MOD_CLUSTER_IP` and start the second worker:

```bash
./node-b/bin/standalone.sh \
  --server-config=standalone-ha.xml \
  -Djboss.node.name=node-b \
  -Djboss.socket.binding.port-offset=200 \
  -b "$MOD_CLUSTER_IP" \
  -bprivate "$MOD_CLUSTER_IP"
```

Node B listens for application requests on `$MOD_CLUSTER_IP:8280` and for management requests on `127.0.0.1:10190`.

### Register both workers with mod_cluster

The load balancer cannot route a request until it knows about at least one worker. Before registration, requesting the application through the load balancer therefore returns `404 - Not Found`:

```bash
curl "http://$MOD_CLUSTER_IP:8080/session-counter/"
```

By default, the workers discover the load balancer from its UDP multicast advertisements. This requires multicast traffic on the selected network interface to reach the workers. Automatic discovery did not work in the tested setup, so this example uses explicit registration for deterministic behavior.

In terminal 4, set `MOD_CLUSTER_IP`, then run:

```bash
./node-a/bin/jboss-cli.sh \
  --connect \
  --controller=127.0.0.1:10090 \
  --command="/subsystem=modcluster/proxy=default:add-proxy(host=$MOD_CLUSTER_IP,port=8090)"

./node-b/bin/jboss-cli.sh \
  --connect \
  --controller=127.0.0.1:10190 \
  --command="/subsystem=modcluster/proxy=default:add-proxy(host=$MOD_CLUSTER_IP,port=8090)"
```

Both commands should return:

```text
{"outcome" => "success"}
```

`add-proxy` updates only the running worker's in-memory proxy list and takes effect immediately. It does not edit the WildFly configuration or require a reload. Because that runtime list is lost when the worker stops, run the command again after each worker start in this explicit-registration setup.

### Observe sticky sessions

Use one cookie jar for every request:

```bash
COOKIE_JAR=$(mktemp)
```

Make several requests through the load balancer:

```bash
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
```

A typical result looks like this:

```text
node=node-b
count=1
session=v0Aff37Dp0dubdLX_eEZxlZY-ovhJALKkkW8Nkwx
resumed=false

node=node-b
count=2
session=v0Aff37Dp0dubdLX_eEZxlZY-ovhJALKkkW8Nkwx
resumed=true

node=node-b
count=3
session=v0Aff37Dp0dubdLX_eEZxlZY-ovhJALKkkW8Nkwx
resumed=true
```

The exact node and session ID will vary. The important behavior is that the node and session ID remain unchanged while the counter increases. mod_cluster uses the session's route information to keep sending the client to the same
worker.

### Stop the active worker

Now comes the traditional high-availability test: make one of the servers disappear. Look at the `node` value in the response and stop that WildFly process with Ctrl+C. For example, if the response says `node=node-b`, stop node B in terminal 3.

Repeat the same request with the same cookie jar:

```bash
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
```

The application survived. The session did not:

```text
node=node-a
count=1
session=AQ4rJN1pNDVOJKvjQU6Z5c3hmCInkRrjNRAFIWY1
resumed=false
```

This is the intended result. mod_cluster stopped routing requests to the original worker and routed the next request to the remaining worker. The counter restarted because the application is non-distributable and the surviving worker did not have the original HTTP session.

In other words, mod_cluster provides load balancing, sticky routing, and worker failover. Preserving the session across that failure requires WildFly session clustering, which is deliberately absent from this first example.

## Second example: The mighty session replication

Now we're going to employ the `<distributable/>` configuration in web.xml.

```
HTTP client -> mod_cluster LB --> WildFly A
                             |
                             |     in
                             |   cluster
                             |    with
                             |
                              \-> WildFly B

```

The setup is the same but the application preparation step includes `distributable` profile. Start with all servers stopped:

```bash
mvn clean package -Pdistributable
cp target/session-counter.war node-a/standalone/deployments/
cp target/session-counter.war node-b/standalone/deployments/
```

then

1. start the load balancer again
2. start both workers again
3. run the `add-proxy` commands again

### Observe sticky sessions

Use one cookie jar for every request:

```bash
COOKIE_JAR=$(mktemp)
```

Make several requests through the load balancer:

```bash
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
```

A typical result looks like this, same as in the previous example:

```text
node=node-b
count=1
session=ztvjJ9i0gkO3voZW5BjQHnhA6v03xumrEfRELeS
resumed=false

node=node-b
count=2
session=ztvjJ9i0gkO3voZW5BjQHnhA6v03xumrEfRELeS
resumed=true

node=node-b
count=3
session=ztvjJ9i0gkO3voZW5BjQHnhA6v03xumrEfRELeS
resumed=true
```

### Stop the active worker and repeat request

Stop the active worker in the same way as you did previously. Then repeat the request:

```bash
curl -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://$MOD_CLUSTER_IP:8080/session-counter/"
```

This time, nobody lost their shopping cart:

```
node=node-a
count=4
session=ztvjJ9i0gkO3voZW5BjQHnhA6v03xumrEfRELeS
resumed=true
```

The important difference is that the counter continues from 3 to 4 and resumed=true. The application-visible session ID also remains unchanged. The session state was available on the surviving cluster member, while mod_cluster routed the request to that member.

This is where the two mechanisms complement each other: mod_cluster keeps the application reachable, while WildFly clustering keeps the user's session state available.

## Conclusion

mod_cluster and WildFly clustering solve different parts of the same availability problem.

mod_cluster keeps requests flowing by routing them to an available worker. WildFly clustering keeps distributable session state available when one of those workers disappears.

Used separately, each mechanism has clear limits. Used together, they provide both request failover and session continuity -- which is usually what users actually expect from a highly available web application.
