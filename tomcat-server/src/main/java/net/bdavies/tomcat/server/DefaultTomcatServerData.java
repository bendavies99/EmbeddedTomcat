package net.bdavies.tomcat.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author ben.davies
 */
@Slf4j
@Getter
@Setter(AccessLevel.PACKAGE)
@ToString
public class DefaultTomcatServerData implements TomcatServerData {
    private Properties applicationProperties;
    private String servletPath;
    private File compiledLocation;
    private final File webAppBaseDirectory;
    private final String compileClasspath, runtimeClasspath;
    private List<File> srcDirectories, webAppResources;
    private int port, shutdownPort;
    private String sourceCompatability, targetCompatability;
    private String jarsToSkip, jarsToScan;

    public DefaultTomcatServerData(File webappBaseDirectory, String compileClasspath, String runtimeClasspath) {
        this.webAppBaseDirectory = webappBaseDirectory;
        this.compileClasspath = compileClasspath;
        this.runtimeClasspath = runtimeClasspath;
        this.applicationProperties = new Properties();
        this.servletPath = ""; //Use ROOT by default
        this.compiledLocation = webappBaseDirectory.toPath().resolve(Path.of("WEB-INF", "classes")).toFile(); //Use WEB-INF/classes by default
        this.srcDirectories = new LinkedList<>(); //Empty by default
        this.port = 8080; //8080 by default
        this.shutdownPort = 8082; //8082 by default
        this.sourceCompatability = "11"; //Java 11 by default
        this.targetCompatability = "11"; //Java 11 by default
        this.webAppResources = new LinkedList<>();
        this.jarsToSkip = "--";
        this.jarsToScan = "--";
    }
}
