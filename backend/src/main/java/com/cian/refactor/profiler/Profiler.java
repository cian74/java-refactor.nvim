package com.cian.refactor.profiler;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profiler {
    
    private static String PROFILER_DIR;
    private static String PROFILER_CMD;
    private static String PROFILER_LIB;
    
    static {
        if (new File("/opt/homebrew/opt/async-profiler").exists()) {
            PROFILER_DIR = "/opt/homebrew/opt/async-profiler";
            PROFILER_CMD = PROFILER_DIR + "/bin/asprof";
            PROFILER_LIB = PROFILER_DIR + "/lib/libasyncProfiler.dylib";
        } else if (new File("/usr/local/opt/async-profiler").exists()) {
            PROFILER_DIR = "/usr/local/opt/async-profiler";
            PROFILER_CMD = PROFILER_DIR + "/bin/asprof";
            PROFILER_LIB = PROFILER_DIR + "/lib/libasyncProfiler.so";
        } else {
            PROFILER_DIR = System.getProperty("java.io.tmpdir") + "/async-profiler";
            PROFILER_CMD = PROFILER_DIR + "/profiler.sh";
            PROFILER_LIB = PROFILER_DIR + "/libasyncProfiler.so";
        }
    }
    
    public Refactored profileMethod(String source, String className, String methodName, Integer lineNumber) {
        Refactored result = new Refactored();
        
        try {
            if (!checkProfilerInstalled()) {
                result.error = "async-profiler not found. Install with: brew install async-profiler";
                return result;
            }
            
            String classFileName = className != null ? className : findClassName(source);
            if (classFileName == null) {
                result.error = "Could not find class name in source";
                return result;
            }
            
            if (!hasMainMethod(source)) {
                result.error = "Class must have a main(String[] args) method to profile.";
                return result;
            }
            
            // Check for Maven/Gradle projects
            boolean isMaven = new File("pom.xml").exists();
            boolean isGradle = new File("build.gradle").exists() || new File("build.gradle.kts").exists();
            
            String flameGraph;
            
            if (isMaven) {
                flameGraph = profileMavenProject(classFileName, source);
            } else if (isGradle) {
                flameGraph = profileGradleProject(classFileName, source);
            } else {
                flameGraph = profileSimpleFile(source, classFileName);
            }
            
            result.flame_graph = flameGraph;
            result.new_source = source;
            
        } catch (Exception e) {
            e.printStackTrace();
            result.error = "Profiling failed: " + e.getMessage();
        }
        
        return result;
    }
    
    private boolean checkProfilerInstalled() {
        return new File(PROFILER_CMD).exists() || new File(PROFILER_DIR + "/profiler.jar").exists();
    }
    
    private String findClassName(String source) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean hasMainMethod(String source) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            return cu.findAll(MethodDeclaration.class).stream()
                .anyMatch(m -> m.getNameAsString().equals("main") 
                    && m.isStatic()
                    && m.getParameters().size() == 1
                    && m.getParameter(0).getTypeAsString().equals("String[]"));
        } catch (Exception e) {
            return false;
        }
    }
    
    private String findPackage(String source) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            return cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");
        } catch (Exception e) {
            return "";
        }
    }
    
    private String profileMavenProject(String classFileName, String source) throws Exception {
        String packageName = findPackage(source);
        String fullClassName = packageName.isEmpty() ? classFileName : packageName + "." + classFileName;
        
        // Compile the source file and add to project
        String packagePath = packageName.isEmpty() ? "" : packageName.replace(".", "/");
        if (!packagePath.isEmpty()) {
            String javaDir = "src/main/java/" + packagePath;
            new File(javaDir).mkdirs();
            Files.writeString(Path.of(javaDir + "/" + classFileName + ".java"), source);
        }
        
        // Compile with Maven
        ProcessBuilder compilePb = new ProcessBuilder("mvn", "compile", "-q");
        compilePb.directory(new File("."));
        compilePb.redirectErrorStream(true);
        Process compileProc = compilePb.start();
        compileProc.waitFor();
        
        // Get classpath from Maven
        String cpFile = "/tmp/mvn-cp-" + System.currentTimeMillis() + ".txt";
        ProcessBuilder cpPb = new ProcessBuilder("mvn", "dependency:build-classpath", "-Dmdep.outputFile=" + cpFile, "-q");
        cpPb.directory(new File("."));
        Process cpProc = cpPb.start();
        cpProc.waitFor();
        
        String mavenCp = Files.readString(Path.of(cpFile)).trim();
        Files.deleteIfExists(Path.of(cpFile));
        
        // Build full classpath
        String projectClasses = "target/classes" + (packagePath.isEmpty() ? "" : "/" + packagePath);
        String fullCp = projectClasses + File.pathSeparator + mavenCp;
        
        // Run with profiler
        return runWithClasspath(fullCp, fullClassName);
    }
    
    private String profileGradleProject(String classFileName, String source) throws Exception {
        String packageName = findPackage(source);
        String fullClassName = packageName.isEmpty() ? classFileName : packageName + "." + classFileName;
        
        String packagePath = packageName.isEmpty() ? "" : packageName.replace(".", "/");
        if (!packagePath.isEmpty()) {
            String javaDir = "src/main/java/" + packagePath;
            new File(javaDir).mkdirs();
            Files.writeString(Path.of(javaDir + "/" + classFileName + ".java"), source);
        }
        
        // Compile with Gradle
        ProcessBuilder compilePb = new ProcessBuilder("./gradlew", "compileJava", "-q");
        compilePb.directory(new File("."));
        compilePb.redirectErrorStream(true);
        Process compileProc = compilePb.start();
        compileProc.waitFor();
        
        // For Gradle, just use build/classes and hope for the best
        String projectClasses = "build/classes/java/main" + (packagePath.isEmpty() ? "" : "/" + packagePath);
        
        // Run with profiler
        return runWithClasspath(projectClasses, fullClassName);
    }
    
    private String runWithClasspath(String classpath, String mainClass) throws Exception {
        String collapsedFile = "/tmp/profile-" + System.currentTimeMillis() + ".txt";
        
        // Run Java with profiler agent
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-agentpath:" + PROFILER_LIB + "=start,event=cpu,file=" + collapsedFile + ",interval=1ms");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(mainClass);
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("."));
        pb.redirectErrorStream(true);
        
        Process proc = pb.start();
        
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            proc.destroy();
        }
        
        if (new File(collapsedFile).exists()) {
            String content = Files.readString(Path.of(collapsedFile));
            Files.deleteIfExists(Path.of(collapsedFile));
            return formatFlameGraph(content);
        }
        
        return "No profiling data captured.";
    }
    
    private String profileSimpleFile(String source, String classFileName) throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir") + "/java-refactor-profiler";
        new File(tempDir).mkdirs();
        
        String sourceFile = tempDir + "/" + classFileName + ".java";
        Files.writeString(Path.of(sourceFile), source);
        
        // Compile
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder("javac", "-cp", classpath, "-d", tempDir, sourceFile);
        pb.inheritIO();
        Process p = pb.start();
        if (p.waitFor() != 0) {
            throw new RuntimeException("Compilation failed");
        }
        
        // Run with profiler
        String collapsedFile = tempDir + "/collapsed.txt";
        
        ProcessBuilder runBuilder = new ProcessBuilder(
            "java",
            "-agentpath:" + PROFILER_LIB + "=start,event=cpu,file=" + collapsedFile + ",interval=1ms",
            "-cp", tempDir,
            classFileName
        );
        
        runBuilder.directory(new File(tempDir));
        runBuilder.redirectErrorStream(true);
        Process runProc = runBuilder.start();
        runProc.waitFor();
        
        if (new File(collapsedFile).exists()) {
            String content = Files.readString(Path.of(collapsedFile));
            cleanup(tempDir, sourceFile);
            return formatFlameGraph(content);
        }
        
        cleanup(tempDir, sourceFile);
        return "No profiling data captured.";
    }
    
    private String formatFlameGraph(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "No profiling data captured";
        }
        
        String[] lines = rawOutput.split("\n");
        Map<String, Long> samples = new HashMap<>();
        long totalSamples = 0;
        
        for (String line : lines) {
            if (line.matches("^\\s*\\d+\\s+\\d+\\.\\d+%\\s+\\d+.*")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4) {
                    try {
                        String method = parts[3];
                        long count = Long.parseLong(parts[2]);
                        samples.merge(method, count, Long::sum);
                    } catch (NumberFormatException e) {}
                }
            }
            if (line.contains(" samples")) {
                String[] parts = line.split(",");
                for (String part : parts) {
                    if (part.trim().endsWith("samples")) {
                        try {
                            long count = Long.parseLong(part.trim().replace(" samples", "").replace("(", ""));
                            totalSamples += count;
                        } catch (NumberFormatException e) {}
                    }
                }
            }
        }
        
        if (samples.isEmpty()) {
            return "No profiling data captured";
        }
        
        List<Map.Entry<String, Long>> sorted = samples.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(15)
            .toList();
        
        long maxSamples = sorted.get(0).getValue();
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 Flame Graph\n");
        sb.append("═".repeat(50)).append("\n");
        sb.append(String.format("Total samples: %d\n", totalSamples > 0 ? totalSamples : maxSamples));
        
        for (Map.Entry<String, Long> entry : sorted) {
            double percentage = maxSamples > 0 ? (double) entry.getValue() / maxSamples * 100 : 0;
            int barLength = (int) (percentage / 5);
            String bar = "█".repeat(Math.min(barLength, 20));
            String padding = " ".repeat(Math.max(0, 20 - barLength));
            
            String method = entry.getKey();
            if (method.length() > 35) {
                method = method.substring(0, 32) + "...";
            }
            
            sb.append(String.format("%-38s %s%s (%.1f%%)\n", 
                method, bar, padding, percentage));
        }
        
        sb.append("═".repeat(50)).append("\n");
        sb.append("Press q or Esc to close");
        
        return sb.toString();
    }
    
    private void cleanup(String tempDir, String sourceFile) {
        try {
            Files.deleteIfExists(Path.of(sourceFile));
            File dir = new File(tempDir);
            if (dir.exists()) {
                for (File f : dir.listFiles()) {
                    f.delete();
                }
                dir.delete();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
