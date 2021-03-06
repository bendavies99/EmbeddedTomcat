package net.bdavies.tomcat.server.watcher;

import lombok.extern.java.Log;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.slf4j.helpers.SubstituteLoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ben.davies
 */
public class Compiler {
    public void compileFile(File fileToCompile, String compileClassPath, String sourceCompatability, String targetCompatability,
                            String outputDir, String sourcePath, Runnable onComplete) {
        List<String> cmdArgs = new LinkedList<>();
        //Set source
        cmdArgs.add("-source");
        cmdArgs.add(sourceCompatability);

        //Set target
        cmdArgs.add("-target");
        cmdArgs.add(targetCompatability);

        //Remove warnings to output less logs
        cmdArgs.add("-nowarn");

        //Set the classpath
        cmdArgs.add("-cp");
        cmdArgs.add(compileClassPath);

        //Set the sourcepath
        cmdArgs.add("-sourcepath");
        cmdArgs.add(sourcePath);

        //Set the output path
        cmdArgs.add("-d");
        cmdArgs.add(outputDir);

        //Set the file to compile
        cmdArgs.add(fileToCompile.getAbsolutePath().replace(".java~", ".java"));
        BatchCompiler.compile(cmdArgs.toArray(new String[0]), new PrintWriter(System.out),
                new PrintWriter(System.err), new CompilationProgress() {
                    @Override
                    public void begin(int remainingWork) {
                        System.out.println("Started compile of: " + fileToCompile.getName());
                        System.out.println("-- remaining work: " + remainingWork);
                    }

                    @Override
                    public void done() {
                        System.out.println("Completed compile of: " + fileToCompile.getName());
                        onComplete.run();
                    }

                    @Override
                    public boolean isCanceled() {
                        return false;
                    }

                    @Override
                    public void setTaskName(String name) {
                        System.out.println("Current Compile task: " + name);
                    }

                    @Override
                    public void worked(int workIncrement, int remainingWork) {
                    }
                });
    }
}
