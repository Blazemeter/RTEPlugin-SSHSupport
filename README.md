# JMeter-RTE-plugin

![labs-logo](docs/src/.vuepress/public/blazemeter-labs-logo.png)

This project implements a JMeter plugin to **support RTE (Remote Terminal Emulation) protocols** by providing a recorder for automatic test plan creation, and config and sampler for protocol interactions.

Nowadays, the plugin supports **IBM protocol's TN5250, TN3270 and VT420** by using embedded [xtn5250](https://sourceforge.net/projects/xtn5250/), [dm3270](http://dmolony.github.io/) and [jvt220](https://github.com/jawi/jVT220) emulators, with some modifications on [xtn5250 fork](https://github.com/abstracta/xtn5250), [dm3270 fork](https://github.com/abstracta/dm3270) and  [jvt220 fork](https://github.com/Blazemeter/jVT220) to better accommodate to the plugin usage (exception handling, logging, external dependencies, etc).

## Usage

Check our complete and detailed documentation [here](https://blazemter.github.io/rte-plugin/) for usage instructions.

## Compatibility

The plugin is tested with Jmeter 3.1, 3.2, 3.3, 4.0 in Java 8 and 11. Code base is implemented in Java 1.8, so lower versions of JVM are not supported.

## Contributing

If you find any issue or something that is not supported by this plugin, please report it, and we will try to fix it. It will help a lot if you send us the JMeter logs with **debug log level** enabled.

*Debug log level* could be enabled by configuring the Log4j 2 Configuration File (adding `<Logger name="com.blazemeter.jmeter.rte" level="debug" />`) or via JMeter menu, how to do it from both ways are explained [here](https://www.blazemeter.com/blog/how-to-configure-jmeter-logging).

Got **something interesting** you'd like to **share**? Learn about [contributing](https://blazemeter.github.io/rte-plugin/contributing/).