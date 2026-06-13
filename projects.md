---
layout: page
title: Projects
permalink: /projects/
---

These are the main public projects, tools, and experiments I maintain or keep around outside my day job.

## Litovle.cz

[Litovle.cz](https://litovle.cz/) is my main public project: an independent civic information hub for Litovel.

It collects selected public information, tools, and links in one place. It is not an official city website and does not try to replace official communication channels. The goal is to make already public information easier to find, search, and use.

Related repositories:

* [litovle.cz](https://github.com/honza-kasik/litovle.cz) - source code and publishing workflow for the main website.
* [usneseni-mesta](https://github.com/honza-kasik/usneseni-mesta) - pipeline for processing public PDF documents, extracting resolutions, building search indexes, and exporting static data.
* [meeting-summarizer](https://github.com/honza-kasik/meeting-summarizer) - deterministic tooling for processing municipal meeting recordings into structured materials.
* [svoz_odpadu_litovel](https://github.com/honza-kasik/svoz_odpadu_litovel) - source files and tooling for the Litovel waste collection overview.

The boring parts are deliberate: static output, public sources, reproducible processing, and simple interfaces that should keep working over time.

## Svoz odpadu Litovel

[Svoz odpadu Litovel](https://svoz.litovle.cz/) is a small practical tool for looking up waste collection dates by street in Litovel and its local parts.

It started as a simple way to avoid digging through PDF calendars and turned into a public website and a companion Android app.

Links:

* [Website](https://svoz.litovle.cz/)
* [Source code](https://github.com/honza-kasik/svoz_odpadu_litovel)
* [Android app on Google Play](https://play.google.com/store/apps/details?id=cz.litovle.svoz)

## Work-related open source

A part of my public open-source work is related to WildFly, mod_cluster, testing infrastructure, and Java tooling.

I was behind the rewrite of the [mod_cluster testsuite](https://github.com/modcluster/mod_cluster-testsuite), which is used for validating mod_cluster behavior in distributed application server environments.

I also maintain a couple of WildFly-related tools:

* [Creaper](https://github.com/wildfly-extras/creaper) - a WildFly Extras project for working with WildFly server configuration from Java.
* [dist-diff](https://github.com/wildfly/dist-diff) - a tool for comparing Java-based application distributions and reporting differences between them.

## Home automation and hardware

I also publish some of the practical home automation and hardware work I do around my house.

Selected project:

* [water-cistern](https://github.com/honza-kasik/water-cistern) - ESPHome configuration for measuring the water level in a rainwater tank and exposing it to Home Assistant.

Related write-ups on this blog cover things like Home Assistant, ESPHome, PoE devices, wired Samsung air conditioning, TapHome, and I/O modules.

## Older and smaller projects

Some of my older public repositories are small tools, experiments, or one-off utilities. They are not all actively maintained, but I keep them public in case they are useful or interesting to someone.

Selected examples:

* [OlomouckyBazen](https://github.com/honza-kasik/OlomouckyBazen) - unofficial Android app showing occupancy and available lanes at the Olomouc swimming pool.
* [cpu-mem-usage-monitor](https://github.com/honza-kasik/cpu-mem-usage-monitor) - small script for tracking CPU and memory usage over time.
* [check-for-files](https://github.com/honza-kasik/check-for-files) - utility script for checking new files on an SFTP server.
* [lzss](https://github.com/honza-kasik/lzss) - a small implementation of the LZSS compression algorithm.

## GitHub

Most of my public code, experiments, and supporting repositories are available on [GitHub](https://github.com/honza-kasik).
