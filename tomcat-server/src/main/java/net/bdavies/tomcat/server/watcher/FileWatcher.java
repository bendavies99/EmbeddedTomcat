package net.bdavies.tomcat.server.watcher;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * @author ben.davies
 */
@Slf4j
public class FileWatcher implements Runnable {
    private final PublishSubject<ChangeType> subject = PublishSubject.create();
    private final Thread thread;
    private volatile boolean isRunning = false;
    private final WatchService service;
    private final Map<WatchKey, Path> keys;
    private final String compileClassPath;
    private final File outputDir;
    private final String srcDir;
    private final String sourceCompat, targetCompat;

    public FileWatcher(List<File> sources, String compileClassPath, File outputDir, String sourceCompat, String targetCompat) throws IOException {
        this.sourceCompat = sourceCompat;
        this.targetCompat = targetCompat;
        thread = new Thread(this, "FileWatcherThread");
        service = FileSystems.getDefault().newWatchService();
        keys = new HashMap<>();
        this.compileClassPath = compileClassPath;
        this.outputDir = outputDir;
        //TODO: Find a better way to know the index of the srcs containing the java files
        srcDir = new ArrayList<>(sources).get(1).getAbsolutePath();
        for (val source : sources) {
            if (source.isDirectory()) {
                registerAll(source.toPath());
            }
        }
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        thread.start();
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        log.info("Shutting down the filewatcher");
        try {
            thread.join(10);
        } catch (InterruptedException e) {
            log.error("Couldn't stop thread {} something went wrong", thread.getName(), e);
        }
    }

    private void registerAll(Path dir) throws IOException {
        if (dir.toString().equals(" ") || dir.toString().isEmpty()) return;
        log.debug("Registering path: {} for file changes", dir);
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        keys.put(key, dir);
    }


    public void subscribeClasses(Runnable onNext) {
        subject.observeOn(Schedulers.immediate())
                .subscribeOn(Schedulers.immediate())
                .filter(ct -> ct == ChangeType.CLASS)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .subscribe(a -> onNext.run());
    }

    public void subscribeResources(Runnable onNext) {
        subject.observeOn(Schedulers.immediate())
                .subscribeOn(Schedulers.immediate())
                .filter(ct -> ct == ChangeType.RESOURCE)
                .subscribe(a -> onNext.run());
    }

    private synchronized void updateObservers(ChangeType type) {
        if (subject.hasObservers()) {
            subject.onNext(type);
        }
    }

    @Override
    public void run() {
        final Debouncer debouncer = new Debouncer();
        final Compiler compiler = new Compiler();
        while (isRunning) {
            try {
                WatchKey key = service.take();
                Path file = keys.get(key);
                if (file == null) {
                    System.err.println("Unknown file returned during watch event");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    val kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    //noinspection unchecked
                    val ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = file.resolve(name);
                    val absPath = child.toFile().getAbsolutePath().replace(".java~", ".java");
                    if (absPath.endsWith(".java")) {
                        if (child.toFile().isFile()) {
                            //Compile it
                            debouncer.debounce(absPath, () -> {
                                log.info("Compiling file: {} to location {}", child, outputDir.getAbsolutePath());
                                compiler.compileFile(child.toFile(), compileClassPath,
                                        sourceCompat, targetCompat, outputDir.getAbsolutePath(), srcDir,
                                        () -> updateObservers(ChangeType.CLASS));
                            }, 500, TimeUnit.MILLISECONDS);
                        }
                    } else if (!absPath.endsWith("~") && child.toFile().isFile()) {
                        debouncer.debounce(absPath, () -> {
                            log.info("A resource has changed {}", absPath);
                            updateObservers(ChangeType.RESOURCE);
                        }, 500, TimeUnit.MILLISECONDS);
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                log.error("Couldn't watch for file changes the thread was interrupted", e);
            }
        }
        debouncer.shutdown();
    }
}
