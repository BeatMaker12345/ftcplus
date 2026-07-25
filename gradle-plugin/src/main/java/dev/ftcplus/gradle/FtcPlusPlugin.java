package dev.ftcplus.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

public class FtcPlusPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // register the generation task
        project.getTasks().register("generateFtcPlusRegistry", GenerateRegistryTask.class, task -> {
            task.setGroup("ftcplus");
            task.setDescription("Scans compiled classes for FTC+ annotations and generates the registry.");

            // run after java compilation
            task.dependsOn(project.getTasks().named("compileJava"));

            // input: compiled classes
            task.getClassesDir().set(
                    project.getLayout().getBuildDirectory().dir("classes/java/main")
            );

            // output: generated source
            task.getOutputDir().set(
                    project.getLayout().getBuildDirectory().dir("generated/ftcplus")
            );
        });

        // wire generated sources into compilation
        project.getTasks().named("compileJava", JavaCompile.class, task -> {
            task.source(project.getLayout().getBuildDirectory().dir("generated/ftcplus"));
        });
    }
}