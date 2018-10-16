---
title: RadurGun basics
excerpt: An introduction to framework for distributed testing. Lets see its gains and losses!
tags:
- Diploma thesis
- Distributed testing
---

## What is Radargun?

According to [RadarGun webpage](http://radargun.github.io/radargun/index.html), it depends on which version are you looking:

> Radargun 1.x was a tool for comparing the performance of different caching/data grid products (e.g. Infinispan, EHCache, Coherence). Since version 2.0 it has become a general distributed system testing tool, although the primary focus on implementation is still on clustered caches. Version 3.0 (development in progress) will bring support for testing other areas as well (e.g. JPA).

What I want it to be? Why I am even looking in it?

I am looking for a solution to test load balancing scenarios on multiple machines (I really don't care whether they are physical, cloud or anything else...).

## Where to start

Everyone say for themselves that they should start on page of the product they are trying to use. Well, in this case I wouldn't recommend it, but its the only way to go. Documentation there isn't really helpful. I understood its hierarchy after a while and it seems, that necessary information is really there, but for a new user it is really confusing.

The tutorial contains only very basic information. You only get to know how to start the benchmark. But what if I don't want to benchmark distributed system? Just run some integration tests?

## Architecture

### Nodes

<div class="mermaid">
graph TD;
    Slave1-- registers -->Master;
    Slave2-- registers -->Master;
    Slave3-- registers -->Master;
    SlaveN-- registers -->Master;
</div>

There is always one `master` node which controls execution and produces reports. The master is started as a server, slaves connect to it. `slaves` are nodes that are actually executing something, gathering statistics and other info. Slave starts as a `service`. There can be many `slaves`.

`master` has following responsibilities:
* Parse configuration and based on that, give work to `slaves` (run stages on them)
* After starting stages on all `slaves`, `master` will block, until all stages on `slaves` finish and they all acknowledge `master`, that stage has finished.

### Stages

Stage is a core Radargun concept. After all Slaves connects to `master`, `master` starts scenario consisting of sequence of `stages`. In every moment, same `stage` is executed by each `slave`, synchronized by `master`.

MasterStage is executed only on Master, DistStage is distributed to Slaves. Below you can see which methods are run in init and execute phases:

| stage   | MasterStage | DistStage                        |
|---------|-------------|----------------------------------|
| init    | `init()`      | `initOnMaster()`<br/>`initOnSlave()` |
| execute | `execute()`   | `executeOnSlave()`                 |

The main difference is what happens after execution phase. `executeOnSlave()` returns either `DistStageAck` which is an acknowledgment, that Stage run successfully or failed or any serializable object which extends it, allowing to add some payload – e.g. test statis=./p00 . Master collects all these return values and runs `processAcksOnMaster()`.

#### Stage properties

Each Stage can have Properties configurable from XML configuration file. TODO

### Plugins, Services, Traits

<div class="mermaid">
graph LR;
    Plugin-- provides -->Service;
    Service-- provides -->Trait;
</div>

#### Plugin

A module to adapt any distributed system used in Radargun. Sometimes refers to distributed system itself.

#### Service

A plugin may provide many Services. This service may for example cover various modes provided by Plugin implementation (embedded mode, client...).

#### Trait

Each Plugin usually provides multiple features (lifecycle, cluster information, transactions...). Trait is a handle for such feature. It is annotated with `@Trait` and having its implementation binded by Plugin.

Service can have several no-arg methods annotated with `@ProvidesTrait` which are called after service is created and returned objects are analyzed. This way Radargun knows which Traits Service provides.

### Statistics

TODO

### Reporters

TODO

### Extensions

TODO

## Configuration

I didn't find the configuration convenient. Main reason is, that everything is configured inside XML files. At first, it makes sense. But on the other hand: Is your testsuite already Java based? Well, too bad, there is _NO_ Java API. At least, I found none. Since I don't want to repeat myself over this part of documentation, which is actually made nicely, I will leave it on disclosure of each reader whether they want to [get deeper](http://radargun.github.io/radargun/benchmark_configuration/general.html).

## Short example on integration tests

Is this even possible?

## Bonus – name clash

https://github.com/SoerenHenning/RadarGun/issues/12

## Resources

* [Official documentation](http://radargun.github.io/radargun/)
* Probably none other

<script src="/assets/js/mermaid.min.js"></script>
