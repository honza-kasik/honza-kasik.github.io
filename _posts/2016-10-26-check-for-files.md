---
title: Remote files discovery – my first script in Python
excerpt: A few years ago I ran into problem. And by solving this problem I was very enriched of new experience and it was also my first practice in open source.
---

A few years ago I ran into problem. And by solving this problem I was very enriched of new experience and it was also my first practice in open source. The problem was simple: I was given SFTP access to a server and I was ordered to check regularly for new files. First few weeks I've been doing this, but after a while I started to forgot. Back then, it was my first job as a web admin and JavaScript was closest to programming so far.

[My question on askubuntu.com](http://askubuntu.com/questions/576927/get-notified-about-new-files-on-sftp-server/689242) didn't bring a solution, so I have decided to make a script to check for new files for me. Since I didn't want to use solution on server side (which I didn't have access to), because the maintaining would be more difficult, I have chosen to write a client script in Python for it.

The script works flawlessly now, using TinyDB to store differences between locally saved state and current state of files on server. You just need to have your public SSH key on server and correctly configure the script in `configuration.ini` file. The scripts connect to server and it will do everything by itself.

It was running on Raspberry Pi 2 and probably would run on an original Pi too, I just bought this one, since it was just released and after I bought it I didn't have to run it on my machine again. Just one Cron entry on Raspbian and that was it.

If you run into any problems, logging is used and you can look in file `last-log.log` for any issues.

The script can be downloaded on my GitHub: [https://github.com/honza-kasik/check-for-files](https://github.com/honza-kasik/check-for-files).
