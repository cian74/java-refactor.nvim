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
import java.util.Set;

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
        String profileFile = "/tmp/profile-" + System.currentTimeMillis() + ".txt";
        
        // Warmup run to trigger JIT compilation
        ProcessBuilder warmupPb = new ProcessBuilder("java", "-cp", classpath, mainClass);
        warmupPb.directory(new File("."));
        warmupPb.redirectErrorStream(true);
        Process warmupProc = warmupPb.start();
        try {
            warmupProc.waitFor();
        } catch (InterruptedException e) {
            warmupProc.destroy();
        }
        
        // Run with profiler
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-agentpath:" + PROFILER_LIB + "=start,event=cpu,file=" + profileFile + ",interval=100us");
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
        
        if (new File(profileFile).exists()) {
            String content = Files.readString(Path.of(profileFile));
            Files.deleteIfExists(Path.of(profileFile));
            return formatProfilerOutput(content);
        }
        
        return "No profiling data captured";
    }
    
    private String formatAggregatedFlameGraph(Map<String, Long> samples, long totalSamples) {
        List<Map.Entry<String, Long>> sorted = samples.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(15)
            .toList();
        
        long maxSamples = sorted.get(0).getValue();
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 Flame Graph\n");
        sb.append("═".repeat(50)).append("\n");
        sb.append(String.format("Total samples: %d\n", totalSamples));
        
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
        
        // Warmup run to trigger JIT compilation
        ProcessBuilder warmupPb = new ProcessBuilder("java", "-cp", tempDir, classFileName);
        warmupPb.directory(new File(tempDir));
        warmupPb.redirectErrorStream(true);
        Process warmupProc = warmupPb.start();
        try {
            warmupProc.waitFor();
        } catch (InterruptedException e) {
            warmupProc.destroy();
        }
        
        // Run with profiler
        String profileFile = tempDir + "/profile.txt";
        
        // Use a simple workload to generate CPU
        String wrapperClass = tempDir + "/ProfilerRunner.java";
        String wrapperSource = 
            "public class ProfilerRunner {\n" +
            "    public static void main(String[] args) {\n" +
            "        int sum = 0;\n" +
            "        for (int i = 0; i < 10000000; i++) {\n" +
            "            sum += compute(i);\n" +
            "        }\n" +
            "        System.out.println(\"Result: \" + sum);\n" +
            "    }\n" +
            "    static int compute(int n) {\n" +
            "        return n * n + 1;\n" +
            "    }\n" +
            "}";
        Files.writeString(Path.of(wrapperClass), wrapperSource);
        
        ProcessBuilder compileWrap = new ProcessBuilder("javac", "-cp", tempDir, "-d", tempDir, wrapperClass);
        compileWrap.directory(new File(tempDir));
        compileWrap.start().waitFor();
        
        ProcessBuilder runBuilder = new ProcessBuilder(
            "java",
            "-agentpath:" + PROFILER_LIB + "=start,event=cpu,file=" + profileFile + ",interval=100us",
            "-cp", tempDir,
            "ProfilerRunner"
        );
        
        runBuilder.directory(new File(tempDir));
        runBuilder.redirectErrorStream(true);
        Process runProc = runBuilder.start();
        runProc.waitFor();
        
        if (new File(profileFile).exists()) {
            String content = Files.readString(Path.of(profileFile));
            Files.deleteIfExists(Path.of(profileFile));
            cleanup(tempDir, sourceFile);
            return formatProfilerOutput(content);
        }
        
        cleanup(tempDir, sourceFile);
        return "No profiling data captured";
    }
    
    private String formatProfilerOutput(String content) {
        if (content == null || content.isEmpty()) {
            return "No profiling data captured";
        }
        
        Map<String, Long> samples = new HashMap<>();
        long totalSamples = 0;
        
        // Parse the text output format
        // Example: "--- 2100000 ns (15.67%), 21 samples\n  [ 0] Test.main"
        java.util.regex.Pattern samplePattern = 
            java.util.regex.Pattern.compile("---.*?(\\d+)\\s+samples\\s*$", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Pattern methodPattern = 
            java.util.regex.Pattern.compile("\\[\\s*\\d+\\]\\s+(\\S+)");
        
        String[] lines = content.split("\n");
        long currentSamples = 0;
        
        for (String line : lines) {
            if (line.contains("samples")) {
                java.util.regex.Matcher sampleMatcher = samplePattern.matcher(line);
                if (sampleMatcher.find()) {
                    try {
                        currentSamples = Long.parseLong(sampleMatcher.group(1));
                        totalSamples += currentSamples;
                    } catch (NumberFormatException e) {}
                }
            } else if (line.contains("[") && !line.startsWith("---")) {
                java.util.regex.Matcher methodMatcher = methodPattern.matcher(line);
                if (methodMatcher.find()) {
                    String method = methodMatcher.group(1);
                    if (!isJvmInternal(method) && method.contains(".")) {
                        samples.merge(method, currentSamples, Long::sum);
                    }
                }
            } else if (line.startsWith("---")) {
                currentSamples = 0;
            }
        }
        
        if (samples.isEmpty()) {
            return "No profiling data captured";
        }
        
        return formatAggregatedFlameGraph(samples, totalSamples);
    }
    
    private String formatFlameGraph(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "No profiling data captured";
        }
        
        // Parse collapsed stack format: "frame1;frame2;frame3 count"
        String[] lines = rawOutput.split("\n");
        Map<String, Long> samples = new HashMap<>();
        long totalSamples = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace > 0) {
                try {
                    long count = Long.parseLong(line.substring(lastSpace + 1).trim());
                    String stack = line.substring(0, lastSpace);
                    
                    // Get the topmost user frame (last semicolon-separated frame)
                    String[] frames = stack.split(";");
                    String topFrame = frames[frames.length - 1].trim();
                    
                    // Skip JVM internal frames
                    if (!isJvmInternal(topFrame)) {
                        samples.merge(topFrame, count, Long::sum);
                        totalSamples += count;
                    }
                } catch (NumberFormatException e) {}
            }
        }
        
        if (samples.isEmpty()) {
            return "No profiling data captured (only JVM frames found)";
        }
        
        List<Map.Entry<String, Long>> sorted = samples.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(15)
            .toList();
        
        long maxSamples = sorted.get(0).getValue();
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 Flame Graph\n");
        sb.append("═".repeat(50)).append("\n");
        sb.append(String.format("Total samples: %d\n", totalSamples));
        
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
    
    private boolean isJvmInternal(String frame) {
        if (frame == null || frame.isEmpty()) return true;
        if (frame.startsWith("[") || frame.startsWith("__")) return true;
        if (frame.contains("::") && !frame.contains(".")) return true;
        
        String[] jvmPrefixes = {
            "jdk.", "java.", "javax.", "sun.", "com.sun.",
            "pthread_", "__psynch", "__kernel", "__proc_",
            "os_unfair", "pthread_mutex", "bsearch", "_xzm_",
            "vmSymbols", "ValueRecorder", "JavaThread",
            "InstanceKlass", "LinearScan", "Assembler::",
            "LIR_", "FieldInfo", "RelocIter", "HandleMark",
            "ConstantTable", "MethodCounters", "BytecodeStream",
            "ResourceHashtable", "ciMethod", "Compile::",
            "Connection::", "_platform_", "toBytes"
        };
        for (String prefix : jvmPrefixes) {
            if (frame.startsWith(prefix)) return true;
        }
        return false;
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
