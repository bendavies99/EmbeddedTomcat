package net.bdavies.tomcat;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.JavaVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ben.davies
 */
@Slf4j
@Data
public class TomcatSettings {
    private int port = 8080;
    private File applicationProperties = null;
    private List<File> webAppResources = new LinkedList<>();
    private int shutdownPort = 8082;
    private String contextPath = "";
    private List<String> jarsToScan = new ArrayList<>();
}
