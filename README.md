# Build Config Maven Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.grmek.maven/build-config-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/dev.grmek.maven/build-config-plugin)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=plastic)](LICENSE)

`build-config-plugin` is a Maven plugin for generating `.java` source files based on project properties.
Common scenario is to generate `Version.java` with project version.

## Examples

### Simple example

Generate `Version.java` with Maven project version

```xml
<plugin>
    <groupId>dev.grmek.maven</groupId>
    <artifactId>build-config-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <fields>
            <field>
                <name>VERSION</name>
                <type>String</type>
                <value>"${project.version}"</value>
            </field>
        </fields>
    </configuration>
</plugin>
```

Output will be generated Java class:

```java
package org.example;

import java.lang.String;

public final class BuildConfig {
  public static final String VERSION = "1.0-SNAPSHOT";
}
```

### Complex example

Generate class with enum for development and production environment.

```xml
<plugin>
    <groupId>dev.grmek.maven</groupId>
    <artifactId>build-config-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <includeArtifactIdInPackageName>true</includeArtifactIdInPackageName>
        <enums>
            <enum>
                <name>Env</name>
                <constants>
                    <constant>PROD</constant>
                    <constant>DEV</constant>
                </constants>
            </enum>
        </enums>
        <fields>
            <field>
                <name>ENVIRONMENT</name>
                <type>Env</type>
                <value>PROD</value>
            </field>
            <field>
                <name>LEVEL</name>
                <type>Integer</type>
                <value>2</value>
            </field>
            <field>
                <name>DEBUG</name>
                <type>boolean</type>
                <value>false</value>
            </field>
            <field>
                <name>VERSION</name>
                <type>String</type>
                <value>"${project.version}"</value>
            </field>
            <field>
                <name>TEMPLATE</name>
                <type>StringBuilder</type>
                <value>new StringBuilder("some tomeplate")</value>
                <comment>This is javadoc comment for TEMPLATE field</comment>
            </field>
        </fields>
        <includes>
            <include>java.lang.StringBuilder</include>
        </includes>
    </configuration>
</plugin>
```

Will generate following Java class:

```java
package org.example.untitled;

import java.lang.Integer;
import java.lang.String;
import java.lang.StringBuilder;

public final class BuildConfig {
  public static final Env ENVIRONMENT = Env.PROD;

  public static final Integer LEVEL = 2;

  public static final boolean DEBUG = false;

  public static final String VERSION = "1.0-SNAPSHOT";

  /**
   * This is javadoc comment for TEMPLATE field
   */
  public static final StringBuilder TEMPLATE = new StringBuilder("some tomeplate");

  public enum Env {
    PROD,

    DEV
  }
}
```

## Options

Parameters:
- `fields` - list of fields to generate. For details see [Field definition](#field-definition). 
- `className` - generated class name. Default value is `BuildConfig`.
- `finalClass` - set to `true` if you want to generate final class.
- `packageName` - package name of generated class. Default value is project `groupId`.
- `includeArtifactIdInPackageName` - append project `artifactId` to generated class package name. Project `artifactId` will be converted to camel-case.
- `includes` - list of including packages. If you want to define field with specific type, you should include the package name in this list.
- `outputDirectory` - output directory. Default value is `target/generated-sources`.
- `enums` - list of enum to generate. For details see [Enum definition](#enum-definition).

### Field definition

Parameters:
- `type` - field type. You can use custom types, but you need to include it in `includes`. 
- `name` - field name.
- `value` - field value.
- `modifiers` - field modifier. Default value: `PUBLIC, STATIC, FINAL`. For more details see [javax.lang.model.element.Modifier](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Modifier.html). 
- `comment` - field javadoc comment.


### Enum definition

You can generate simple enum classes and use it in the class.

Parameters:
- `name` - enum class name.
- `modifiers` - enum modifier. Default value: `PUBLIC`. For more details see [javax.lang.model.element.Modifier](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Modifier.html).
- `constants` - set of enum constants.

## License

    Copyright (C) 2022 Martin Grmek

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
