package net.bdavies.tomcat;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author ben.davies
 */
@Slf4j
public class EmbeddedTomcatPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getConfigurations().create("tomcat");
        project.getExtensions().create("tomcat", TomcatSettings.class);
        this.configureTasks(project);
    }

    private void configureTasks(Project project) {
        TomcatRunTask runTask = project.getTasks().create("tomcatRun", TomcatRunTask.class);
    }
}
