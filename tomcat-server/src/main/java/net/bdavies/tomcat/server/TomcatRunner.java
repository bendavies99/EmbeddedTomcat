package net.bdavies.tomcat.server;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bdavies.tomcat.server.livereload.Constants;
import net.bdavies.tomcat.server.livereload.Server;
import net.bdavies.tomcat.server.watcher.ChangeType;
import net.bdavies.tomcat.server.watcher.FileWatcher;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.FileResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tomcat Server setup
 *
 * Required Libs:
 *  - Tomcat: 8.5.71 (Jasper, Juli)
 *  - Lombok: 1.18.22
 *  - ECJ Compiler 3.28.0
 *  - Eclipse Core
 *  - RxJava
 *  - SLF4J Api 1.7.30
 *
 *  - A Logger implementation (up to the plugin consumer)
 *
 *  Required Javaagent:
 *    - Lombok 1.18.22
 *
 * @author ben.davies
 */
@Slf4j
public class TomcatRunner implements Runnable {

    private final Thread thread;
    private final TomcatServerData data;
    private boolean isRunning;
    private final ShutdownHandle handle;
    private final Server server;
//    private final ClassLoader runtimeClassloader;

    public TomcatRunner(TomcatServerData data) {
        this.data = data;
        this.thread = new Thread(this, "TomcatRunner-" + data.getPort());
        this.handle = new ShutdownHandle();
        this.server = new Server(Constants.DEFAULT_LR_PORT);
//        this.runtimeClassloader = getClassLoaderFromClassPath(data.getRuntimeClasspath());
//        thread.setContextClassLoader(classLoader);
    }

    private synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        thread.start();
        handle.subscribe(this::stop);
    }


    private synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        log.info("Shutting down the application");
        try {
            thread.join(10);
        } catch (InterruptedException e) {
            log.error("Couldn't stop thread {} something went wrong", thread.getName(), e);
        }
    }

    private URLClassLoader getClassLoaderFromClassPath(String mainCp) {
        List<File> files = Arrays.stream(mainCp.split(System.getProperty("path.separator"))).map(File::new)
                .collect(Collectors.toList());
        return new URLClassLoader(files.stream().map(f -> {
            try {
                return f.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }).toArray(URL[]::new));
    }

    public static void main(String[] args) {
        ArgParser argParser = new ArgParser(args);
        TomcatServerData data = argParser.getData();
        log.info("Data: {}", data);
        val runner = new TomcatRunner(data);
        runner.start();
    }

    @Override
    public void run() {
        log.info("Start of the application");
        Tomcat tomcat = setupTomcat();
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            log.error("Unable to start tomcat something went wrong", e);
            stop();
        }
        tomcat.getServer().setPort(data.getShutdownPort());
        tomcat.getServer().await();
        //Janky hack mate
        Observable.just("").delay(3, TimeUnit.SECONDS).subscribe(a -> System.exit(0));
        handle.runShutdownHooks();
    }

    private Tomcat setupTomcat() {
        val tomcat = new Tomcat();
        tomcat.setPort(data.getPort());

        //Enable naming for META-INF/context.xml
        tomcat.enableNaming();
        StandardContext context = (StandardContext) tomcat.addWebapp(data.getServletPath(),
                data.getWebAppBaseDirectory().getAbsolutePath());
        setupResources(context);
        context.addLifecycleListener(new TomcatStartListener(data));

        StandardJarScanner scanner = new StandardJarScanner();
        StandardJarScanFilter filter = new StandardJarScanFilter();
        if (!data.getJarsToSkip().equals("--")) {
            filter.setPluggabilitySkip(data.getJarsToSkip());
            filter.setTldSkip(data.getJarsToSkip());
        }
        if (!data.getJarsToScan().equals("--")) {
            filter.setPluggabilityScan(data.getJarsToScan());
            filter.setTldScan(data.getJarsToScan());
        }
        scanner.setJarScanFilter(filter);
        context.setJarScanner(scanner);

        //Silence Tomcat
        tomcat.setSilent(true);
        setupLiveReload();
        setupFileWatching(context);

        //Setup Environment variables
        data.getApplicationProperties().put("installDir", data.getWebAppBaseDirectory().getAbsolutePath());
        data.getApplicationProperties().forEach((k, v) -> {
            ContextEnvironment environment = new ContextEnvironment();
            environment.setName(String.valueOf(k));
            environment.setType(v.getClass().getCanonicalName());
            environment.setValue(String.valueOf(v));
            context.getNamingResources().addEnvironment(environment);
        });
        return tomcat;
    }

    private void setupLiveReload() {
        server.start();
        handle.subscribe(server::stop);
    }

    private void setupResources(StandardContext context) {
        StandardRoot root = new StandardRoot(context);
        val absPath = data.getCompiledLocation().getAbsolutePath();
        DirResourceSet set = new DirResourceSet(root, "/WEB-INF/classes", absPath, "/");
        root.addPreResources(set);
        data.getWebAppResources().forEach(f -> {
            if (f.isFile()) {
                val rSet = new FileResourceSet(root, "/", f.getAbsolutePath(), "/");
                root.addPreResources(rSet);
            } else {
                val rSet = new DirResourceSet(root, "/", f.getAbsolutePath(), "/");
                root.addPreResources(rSet);
            }
        });
        context.setResources(root);
    }

    private synchronized void setupFileWatching(StandardContext context) {
        try {
            FileWatcher watcher = new FileWatcher(Stream.concat(data.getSrcDirectories().stream(), data.getWebAppResources().stream())
                    .collect(Collectors.toList()),
                    data.getCompileClasspath(), data.getCompiledLocation(), data.getSourceCompatability(),
                    data.getTargetCompatability());
            val cName = data.getServletPath().isEmpty() ? "ROOT" : data.getServletPath();
            watcher.subscribeClasses(() -> {
                log.info("Reloading context [{}] because of file changes", cName);
                context.reload();
                log.info("Context [{}] has been reloaded", cName);
                server.publishChangeToConnections();
            });
            watcher.subscribeResources(server::publishChangeToConnections);
            watcher.start();
            handle.subscribe(watcher::stop);
        } catch (IOException e) {
            log.error("Unable to setup the file watching service", e);
        }
    }
}
