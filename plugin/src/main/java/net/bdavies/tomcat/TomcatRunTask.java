package net.bdavies.tomcat;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.internal.DefaultToolchainJavaLauncher;
import org.gradle.jvm.toolchain.internal.JavaToolchain;
import org.gradle.process.internal.ExecAction;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author ben.davies
 */
@Slf4j
public class TomcatRunTask extends Exec {
    private final Property<JavaLauncher> javaLauncherProperty;

    public TomcatRunTask() {
        this.setStandardInput(System.in);
        ObjectFactory objectFactory = getObjectFactory();
        javaLauncherProperty = objectFactory.property(JavaLauncher.class);
        this.dependsOn("classes");
    }

    @SneakyThrows
    @TaskAction
    public void exec() {
        val plugin = getProject().getExtensions().findByType(JavaPluginExtension.class);
        TomcatSettings settings = Objects.requireNonNull(getProject().getExtensions().getByType(TomcatSettings.class));
        assert plugin != null;
        val sourceSets = plugin.getSourceSets();
        val mainSourceSet = sourceSets.getByName("main");
        val classes= mainSourceSet.getRuntimeClasspath().getFiles().stream().findFirst().orElse(new File(""));
        val sources = mainSourceSet.getAllSource().getSourceDirectories().getFiles();
        val sourceCompat = plugin.getSourceCompatibility();
        val targetCompat = plugin.getTargetCompatibility();

        //Setup Tomcat-Runner Deps
        getProject().getConfigurations().create("runnerDeps").setCanBeResolved(true);
        addDependency("org.apache.tomcat.embed:tomcat-embed-core:8.5.71");
        addDependency("org.apache.tomcat.embed:tomcat-embed-logging-juli:8.5.2");
        addDependency("org.apache.tomcat:tomcat-jasper:8.5.71");
        addDependency("org.eclipse.jdt:ecj:3.28.0");
        addDependency("org.eclipse.jdt:core:3.3.0-v_771");
        addDependency("io.reactivex:rxjava:1.3.8");
        addDependency("org.projectlombok:lombok:1.18.22");
        addDependency("net.bdavies.embedded-tomcat:tomcat-server:0.0.1-SNAPSHOT");
        addDependency("javax.servlet:javax.servlet-api:4.0.1");

        val config = getProject().getConfigurations().getByName("runnerDeps");
        val depLocations = config.files(config.getAllDependencies().toArray(new Dependency[0]));
        val lombokFile = depLocations.stream().filter(f -> f.getAbsolutePath().endsWith("jar")
                        && f.getAbsolutePath().contains("lombok") && f.getAbsolutePath().contains("1.18.22"))
                .findFirst().orElse(new File(""));

        val webAppDir = this.getProject().getProjectDir();

        val mainCp = Stream.concat(depLocations.stream(), mainSourceSet.getRuntimeClasspath().getFiles().stream())
                .collect(Collectors.toList())
                .stream().map(File::getAbsolutePath).collect(Collectors.joining(System.getProperty("path.separator")));


        //Add jars to skip

        List<String> args = new LinkedList<>();
        args.add("-Dname=GradleTomcatRunner");
        args.add("-javaagent:" + lombokFile.getAbsolutePath() + "=EJC");
        args.add("net.bdavies.tomcat.server.TomcatRunner");

        val f = getProject().getBuildDir().toPath().resolve("cmplCp.txt").toFile();
        FileUtils.write(f, mainSourceSet.getCompileClasspath().getAsPath(), StandardCharsets.UTF_8);

        val runtimeCp = getProject().getBuildDir().toPath().resolve("runtimeCp.txt").toFile();
        FileUtils.write(runtimeCp, mainSourceSet.getRuntimeClasspath().getAsPath(), StandardCharsets.UTF_8);

        List<String> jarsToSkip = getJarsToSkip(Stream.concat(depLocations.stream(),
                        mainSourceSet.getRuntimeClasspath().getFiles().stream())
                .collect(Collectors.toList()));
        val jToSkip = getProject().getBuildDir().toPath().resolve("jarsToSkip.txt").toFile();
        FileUtils.write(jToSkip, String.join(",", jarsToSkip), StandardCharsets.UTF_8);

        val jToScan = getProject().getBuildDir().toPath().resolve("jarsToScan.txt").toFile();
        FileUtils.write(jToScan, String.join(",", settings.getJarsToScan()), StandardCharsets.UTF_8);

        //Set the program args
        addArgument(args, "webApp", webAppDir);
        addArgument(args, "webAppResources", settings.getWebAppResources());
        addArgument(args, "classesDir", classes);
        addArgument(args, "compileClasspath", f.getAbsolutePath());
        addArgument(args, "runtimeClasspath", runtimeCp);
        addArgument(args, "jarsToSkip", jToSkip);
        addArgument(args, "jarsToScan", jToScan);
        addArgument(args, "srcDirectories", sources);
        addArgument(args, "applicationProperties", settings.getApplicationProperties() == null ? getProject().file("app.properties") : settings.getApplicationProperties());
        addArgument(args, "port", settings.getPort());
        addArgument(args, "shutdownPort", settings.getShutdownPort());
        if (!settings.getContextPath().isEmpty()) {
            addArgument(args, "contextPath", settings.getContextPath());
        }
        addArgument(args, "sourceCompatability", sourceCompat.toString());
        addArgument(args, "targetCompatability", targetCompat.toString());

        ExecAction javaExecAction = getExecActionFactory().newExecAction();
        javaExecAction.args(args);
        javaExecAction.setArgs(args);
        System.out.println("Setting the CLASSPATH to " + mainCp);
        javaExecAction.setEnvironment(Map.of("CLASSPATH", mainCp));
        javaExecAction.setExecutable(getJavaExecutable());
        val result = javaExecAction.execute();
        System.out.println("Finished executing: java " +
                String.join(" ", args) + " with code: " + result.getExitValue());
    }

    private List<String> getJarsToSkip(List<File> files) {
        return files.stream()
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    private void addArgument(List<String> args, String name, Object value) {
        args.add("-" + name + "=" + value);
    }

    private void addDependency(String name) {
        getProject().getDependencies()
                .add("runnerDeps", name);
    }

    private String getJavaExecutable() {
        if (javaLauncherProperty.isPresent()) {
            return javaLauncherProperty.get().getExecutablePath().toString();
        }
        final String executable = getExecutable();
        if (executable != null) {
            return executable;
        }
        return Jvm.current().getJavaExecutable().getAbsolutePath();
    }

//    private String getJarExecutable() {
//        if (javaLauncherProperty.isPresent()) {
//            return javaLauncherProperty.get().getMetadata()
//                    .getInstallationPath()
//                    .file(OperatingSystem.current()
//                            .getExecutableName("jar"))
//                    .getAsFile()
//                    .getAbsolutePath();
//        }
//        final String executable = getExecutable();
//        if (executable != null) {
//            return executable;
//        }
//        return Jvm.current().getExecutable("jar").getAbsolutePath();
//    }

}
