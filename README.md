<p align="center">
   <img src="https://socialify.git.ci/bendavies99/EmbeddedTomcat/image?description=1&font=Source%20Code%20Pro&language=1&name=1&owner=1&pattern=Signal&pulls=1&stargazers=1&theme=Dark" alt="EmbeddedTomcat" width="640" height="320" />
</p>

## Table of Contents

[Getting Started](#getting-started)

* [Installation](#installation)
* [Configuration](#configuration)

[Authors](#authors)

[License](#license)

# Getting Started

This plugin adds the ability to plug and play tomcat into your project with the `tomcatRun` task and you can configure 
it by using the `tomcat` closure please read below the [Configuration](#configuration) settings

## Installation
Add the following line to your build.gradle
```groovy
    apply plugin: 'net.bdavies.embedded-tomcat'
```
You should be able to now run `./gradlew :tomcatRun`

## Configuration
This plugin provides a `tomcat` extension and the properties are:-

Property | Type | Default Value | Description
------ | ------ | ------ | ------
port   | java.lang.Integer |  8080 | The port that server runs on
shutdownPort   | java.lang.Integer |  8082 | The port that will listen for a TCP packet with the ascii message of "SHUTDOWN"
applicationProperties   | java.io.File | ${projectDir}/app.properties | A file of properties to set that you would normally set in the context.xml using <Environment ... />
webAppResources | java.util.List<java.io.File> | empty list | A list of directories or files you wish to add to the tomcat vfs and for watching for live reload
contextPath | java.lang.String | empty string (ROOT) | The path for the context to run by default it uses the (ROOT) path
jarsToScan | java.util.List<java.lang.String> | empty list | A list of jars to scan for servlet API annotations e.g. @WebListener

# Authors

*   **Ben Davies** - *Lead Developer* - [Github](https://github.com/bendavies99)

Currently, there is only me working on EmbeddedTomcat, but it's always open for new ideas and contributions!

See also the list of [contributors](https://github.com/bendavies99/EmbeddedTomcat/contributors) who participated in this project.

# License

This project is licensed under the MIT Licence - see the [LICENSE.md](LICENSE.md) file for details

