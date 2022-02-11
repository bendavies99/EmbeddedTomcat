package net.bdavies.tomcat.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * @author ben.davies
 */
@Slf4j
@RequiredArgsConstructor
public class TomcatStartListener implements LifecycleListener {
    private final TomcatServerData data;
    /**
     * Acknowledge the occurrence of the specified event.
     *
     * @param event LifecycleEvent that has occurred
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType().equals("after_start")) {
            log.info("Started Tomcat Server");
            log.info("Server started at http://localhost:{}/{}", data.getPort(), data.getServletPath());
            log.info("To cleanly shutdown Tomcat - send \"SHUTDOWN\" as a TCP request to the port: {}", data.getShutdownPort());
        }
    }
}
