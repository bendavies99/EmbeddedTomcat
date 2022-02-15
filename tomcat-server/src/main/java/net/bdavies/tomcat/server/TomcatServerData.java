package net.bdavies.tomcat.server;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author ben.davies
 */
public interface TomcatServerData {
    /**
     * Location to the app.properties which sets the {@link javax.naming.InitialContext} data
     *
     * @return the properties data
     */
    Properties getApplicationProperties();

    /**
     * Get the servlet path i.e. /location/
     *
     * @return the path
     */
    String getServletPath();

    /**
     * Get the compiled directory for the web app classes
     *
     * @return the file
     */
    File getCompiledLocation();

    /**
     * Get the webapp base directory
     *
     * @return the file
     */
    File getWebAppBaseDirectory();

    /**
     * Get the webapp runtime classpath
     *
     * @return the file
     */
    String getCompileClasspath();

    /**
     * Get the webapp runtime classpath
     *
     * @return the file
     */
    String getRuntimeClasspath();

    /**
     * Get the webapp jars to skip scanning
     *
     * @return the string ,
     */
    String getJarsToSkip();

    /**
     * Get the webapp jars to ensure scanning
     *
     * @return the string ,
     */
    String getJarsToScan();

    /**
     * The target java version
     *
     * @return a java version
     */
    String getTargetCompatability();

    /**
     * The source java version
     *
     * @return a java version
     */
    String getSourceCompatability();

    /**
     * Get the src directories that web application uses for the auto reload on file change
     *
     * @return the source directories
     */
    List<File> getSrcDirectories();

    /**
     * Get the webapp resource directories e.g. /css /images /js
     * they will be mapped to the context root /dirName e.g. {host}:{port}/dirName/file.ext
     * an actual example http://localhost:8080/css/main.css
     *
     * @return the web app resources
     */
    List<File> getWebAppResources();

    /**
     * Get the server port
     *
     * @return the server port
     */
    int getPort();

    /**
     * Get the shutdown port
     *
     * @return the shutdown port
     */
    int getShutdownPort();
}
