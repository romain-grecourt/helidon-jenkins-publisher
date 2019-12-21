## Publisher Jenkins plugin

Build:
```bash
mvn clean package
```

The unit tests are slow, so you can skip them:
```bash
mvn clean package -DskipTests
```

Assuming you have your Jenkins home at `~/.jenkins`, you can update the plugin
 this way:
```bash
cp target/helidon-build-publisher-plugin.hpi ~/.jenkins/plugins/helidon-build-publisher-plugin.jpi
```

Start Jenkins in debug mode:
```
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9008 -jar ~/jenkins.war
```

### Logging

Created a log recorder, either with the UI or by dropping an XML file at
 `~/.jenkins/log/io.helidon.build.publisher.xml`:

```xml
<?xml version='1.1' encoding='UTF-8'?>
<log>
  <name>io.helidon.build.publisher</name>
  <targets>
    <target>
      <name>io.helidon.build.publisher</name>
      <level>-2147483648</level>
    </target>
  </targets>
```

This will log all logs at the `FINE` level.

Install the [Support Core Plugin](https://wiki.jenkins.io/display/JENKINS/Support+Core+Plugin)
 and make sure to restart Jenkins.

The logs for the recorder will show up under
 `~/.jenkins/logs/custom/io.helidon.build.publisher.log`, they are rotated
 automatically.