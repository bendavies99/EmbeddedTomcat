package net.bdavies.tomcat.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import net.bdavies.tomcat.server.exceptions.ArgumentNotPresentException;
import org.eclipse.core.runtime.Path;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ben.davies
 */
@Slf4j
public class ArgParser {
    private final String[] args;
    private final String[] requiredArgs = new String[] {"webApp","compileClasspath","runtimeClasspath"};
    private final Map<String, String> argMap;

    public ArgParser(String[] args) {
        this.args = args;
        this.argMap = getArgumentMap();
    }

    public TomcatServerData getData() {
        val sd = new DefaultTomcatServerData(
                argumentToFile(argMap.get("webApp")),
                readFileFromArgument("compileClasspath").orElse(""),
                readFileFromArgument("runtimeClasspath").orElse("")
        );
        getFile("classesDir").ifPresent(sd::setCompiledLocation);
        getArgument("contextPath").ifPresent(sd::setServletPath);
        getProperties("applicationProperties").ifPresent(sd::setApplicationProperties);
        getFiles("srcDirectories").ifPresent(sd::setSrcDirectories);
        getFiles("webAppResources").ifPresent(sd::setWebAppResources);
        getInteger("port").ifPresent(sd::setPort);
        getInteger("shutdownPort").ifPresent(sd::setShutdownPort);
        getArgument("sourceCompatability").ifPresent(sd::setSourceCompatability);
        getArgument("targetCompatability").ifPresent(sd::setTargetCompatability);
        readFileFromArgument("jarsToSkip").ifPresent(sd::setJarsToSkip);
        readFileFromArgument("jarsToScan").ifPresent(sd::setJarsToScan);
        return sd;
    }

    private File argumentToFile(String value) {
        return Path.fromOSString(value).toFile();
    }

    private Optional<String> readFileFromArgument(String key) {
        return getFile(key).map(f -> {
           StringBuilder sb = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        });
    }

    private Optional<File> getFile(String key) {
        return getArgument(key).map(this::argumentToFile);
    }

    private Optional<List<File>> getFiles(String key) {
        return getArgumentArray(key).map(dirs -> Arrays.stream(dirs).map(this::argumentToFile)
                .collect(Collectors.toList()));
    }

    private Optional<Properties> getProperties(String key) {
        return getArgument(key).map(this::argumentToFile).map(v -> {
            val p = new Properties();
            try {
                p.load(new FileInputStream(v));
            } catch (IOException e) {
                log.error("Could not read file: {}", v);
            }
            return p;
        });
    }

    private Optional<String> getArgument(String key) {
        return Optional.ofNullable(argMap.get(key));
    }

    private Optional<Integer> getInteger(String key) {
        return getArgument(key).map(Integer::parseInt);
    }

    private Optional<String[]> getArgumentArray(String key) {
        return getArgument(key).map(v -> Arrays.stream(v.split(","))
                .map(String::trim).toArray(String[]::new));
    }

    private Map<String, String> getArgumentMap() {
        log.debug("Processing arguments: {}", Arrays.toString(args));
        Map<String, String> map = new LinkedHashMap<>();
        Arrays.stream(args).forEach(arg -> {
            val tokens = arg.split("=");
            val name = tokens[0].replace("-", "");
            var value = tokens[1];
            if (value.startsWith("[") && value.endsWith("]")) {
                log.debug("Found Collection<?>.toString for argument {} so will convert to comma separated string", name);
                value = value.replace("[", "").replace("]", "");
            }
            if (map.containsKey(name)) {
                log.debug("Found multiple of argument {} so will convert to an array", name);
                map.put(name, map.get(name) + "," + value);
            }
            map.putIfAbsent(name, value);
        });
        Arrays.stream(requiredArgs).forEach(ra -> {
            if (!map.containsKey(ra)) {
                throw new ArgumentNotPresentException(ra);
            }
        });
        return map;
    }
}
