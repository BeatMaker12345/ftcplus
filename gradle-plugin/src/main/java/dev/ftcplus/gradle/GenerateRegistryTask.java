package dev.ftcplus.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class GenerateRegistryTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getClassesDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws IOException {
        List<String> robotClasses     = new ArrayList<>();
        List<String> settingFields    = new ArrayList<>();
        List<String> diagnosticMethods = new ArrayList<>();

        File classesDir = getClassesDir().get().getAsFile();
        scanClasses(classesDir, classesDir, robotClasses, settingFields, diagnosticMethods);

        File outputDir = new File(getOutputDir().get().getAsFile(), "dev/ftcplus/generated");
        outputDir.mkdirs();

        writeRegistry(outputDir, robotClasses, settingFields, diagnosticMethods);
    }

    private void scanClasses(
            File root,
            File dir,
            List<String> robots,
            List<String> settings,
            List<String> diagnostics
    ) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanClasses(root, file, robots, settings, diagnostics);
            } else if (file.getName().endsWith(".class")) {
                scanClass(file, robots, settings, diagnostics);
            }
        }
    }

    private void scanClass(
            File classFile,
            List<String> robots,
            List<String> settings,
            List<String> diagnostics
    ) throws IOException {
        try (FileInputStream in = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                private String className;
                private boolean isRobot = false;

                @Override
                public void visit(int version, int access, String name,
                                  String signature, String superName, String[] interfaces) {
                    this.className = name.replace('/', '.');
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals("Ldev/ftcplus/core/Robot;")) {
                        isRobot = true;
                        robots.add(className);
                    }
                    return null;
                }

                @Override
                public org.objectweb.asm.FieldVisitor visitField(
                        int access, String name, String descriptor,
                        String signature, Object value
                ) {
                    return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (desc.equals("Ldev/ftcplus/core/Setting;")) {
                                settings.add(className + "#" + name);
                            }
                            return null;
                        }
                    };
                }

                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(
                        int access, String name, String descriptor,
                        String signature, String[] exceptions
                ) {
                    return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (desc.equals("Ldev/ftcplus/core/Diagnostic;")) {
                                diagnostics.add(className + "#" + name);
                            }
                            return null;
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }
    }

    private void writeRegistry(
            File outputDir,
            List<String> robots,
            List<String> settings,
            List<String> diagnostics
    ) throws IOException {
        File out = new File(outputDir, "FtcPlusRegistry.java");

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out.toPath()))) {
            w.println("package dev.ftcplus.generated;");
            w.println();
            w.println("// AUTO-GENERATED by FTC+ gradle plugin. Do not edit.");
            w.println("public final class FtcPlusRegistry {");
            w.println();
            w.println("    private FtcPlusRegistry() {}");
            w.println();

            // robots
            w.println("    public static Class<?>[] getRobots() {");
            w.println("        return new Class<?>[] {");
            for (String c : robots) {
                w.println("            " + c + ".class,");
            }
            w.println("        };");
            w.println("    }");
            w.println();

            // settings
            w.println("    public static String[] getSettingFields() {");
            w.println("        return new String[] {");
            for (String s : settings) {
                w.println("            \"" + s + "\",");
            }
            w.println("        };");
            w.println("    }");
            w.println();

            // diagnostics
            w.println("    public static String[] getDiagnosticMethods() {");
            w.println("        return new String[] {");
            for (String d : diagnostics) {
                w.println("            \"" + d + "\",");
            }
            w.println("        };");
            w.println("    }");
            w.println("}");
        }
    }
}