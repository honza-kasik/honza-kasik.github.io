---
title: Log usage of resources by process in bash
---
One of my responsibilities in Red Hat is Management Console testing in Wildfly. In early development build of one of earlier releases we observed strange behavior in our test environment. Although the machine had a large amount of memory available at the beginning of tests, it always failed on `java.lang.OutOfMemoryError` when the test suite was running. We had to find out in which parts of tests this happens.

Since we are using Selenium and the tests are running in Firefox, the obvious solution would be to track amount of memory which the browser is using. But how? We agreed, that the ideal output for our use would be graph showing dependency of used memory on time.

After I googled a bit I found a [question on stackoverflow.com](https://stackoverflow.com/questions/7998302/graphing-a-processs-memory-usage) posted by user who had similar problem. I though, that I could just use solution from the [top voted answer](http://stackoverflow.com/a/12595110/4402950) or solution posted by [Bruno Girin on his blog](http://brunogirin.blogspot.com/2010/09/memory-usage-graphs-with-ps-and-gnuplot.html) and that's it. But it was not suitable for our usage. In our test suite there is used a brand new, clean instance of Firefox for each test. Because of that, I had to come up with solution which would track instances of Firefox which pops up on start of each test.

In script, which can you find on [my GitHub](https://github.com/honza-kasik/cpu-mem-usage-monitor), output of `ps` command with `-o` option to format custom output is used to track these instances. Output in format `<pid>,<memory-size>,<cpu-usage>` is then saved in `usage.log` file by default. Path to target file can be changed by `-l <PATH>` option. Only other output is diagnostic log of script, which outputs to `stdout` and `stderr` by default so you can find out if there are any errors.

I documented all available options in readme and I believe that the code is simple enough to be self-explanatory if you need any further help. If you have not enabled creating graph simultaneously with log, you can create it anytime later with `gnuplot -c gnuplot.script usage.log usage.png`. In case you need bigger resolution, just edit first line in `gnuplot.script` and replace `800,600` in `set term png small size 800,600` by desired resolution and run the command again.

## Useful links

* [Script repository on my GitHub](https://github.com/honza-kasik/cpu-mem-usage-monitor)
* [Inspiration on Stack Overflow](https://stackoverflow.com/questions/7998302/graphing-a-processs-memory-usage)
* [Inspiration by Bruno Girin on his blog](http://brunogirin.blogspot.com/2010/09/memory-usage-graphs-with-ps-and-gnuplot.html)
