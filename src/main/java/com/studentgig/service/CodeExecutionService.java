package com.studentgig.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class CodeExecutionService {
    private static final int MAX_CODE = 30000;
    private static final int MAX_INPUT = 10000;
    private static final int MAX_OUTPUT = 20000;

    public Result execute(String language, String code, String input) {
        String lang = language == null ? "java" : language.trim().toLowerCase(Locale.ROOT);
        if (!lang.equals("java") && !lang.equals("python")) return Result.fail("validation", "Only Java 17+ and Python 3 are supported.");
        if (code == null || code.isBlank()) return Result.fail("validation", "Code cannot be empty.");
        if (code.length() > MAX_CODE) return Result.fail("validation", "Code is too large (30,000 characters maximum).");
        if (input != null && input.length() > MAX_INPUT) return Result.fail("validation", "Input is too large (10,000 characters maximum).");

        Path dir = null;
        try {
            dir = Files.createTempDirectory("studentgig-code-");
            List<String> compile = List.of();
            List<String> run;
            if (lang.equals("java")) {
                String normalized = normalizeJavaMain(code);
                Files.writeString(dir.resolve("Main.java"), normalized, StandardCharsets.UTF_8);
                compile = List.of(javaTool("javac"), "-encoding", "UTF-8", "Main.java");
                run = List.of(javaTool("java"), "-cp", dir.toString(), "Main");
            } else {
                Files.writeString(dir.resolve("main.py"), code, StandardCharsets.UTF_8);
                run = List.of(pythonTool(), dir.resolve("main.py").toString());
            }
            if (!compile.isEmpty()) {
                ProcessResult c = process(compile, dir, "", 8);
                if (!c.finished) return Result.fail("compile", "Compilation timed out.");
                if (c.exitCode != 0) return Result.fail("compile", c.output);
            }
            ProcessResult r = process(run, dir, input == null ? "" : input, 3);
            if (!r.finished) return Result.fail("run", "Execution timed out (3 seconds). Check for an infinite loop.");
            return new Result(r.exitCode == 0, "run", r.output);
        } catch (IOException e) {
            return Result.fail("environment", "Compiler unavailable. Install JDK 17+ (javac/java) and Python 3, then restart StudentGig.");
        } catch (Exception e) {
            return Result.fail("run", "Execution failed: " + e.getMessage());
        } finally {
            if (dir != null) {
                try (var walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(x -> { try { Files.deleteIfExists(x); } catch (IOException ignored) {} });
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean sameOutput(String expected, String actual) {
        return normalizeOutput(expected).equals(normalizeOutput(actual));
    }

    private String normalizeJavaMain(String code) {
        String out = code;
        // The editor contract is Main.java. Make common pasted public class names runnable as Main.
        out = out.replaceFirst("public\\s+class\\s+[A-Za-z_$][A-Za-z0-9_$]*", "public class Main");
        if (!Pattern.compile("\\bclass\\s+Main\\b").matcher(out).find()) {
            out = out.replaceFirst("\\bclass\\s+[A-Za-z_$][A-Za-z0-9_$]*", "class Main");
        }
        return out;
    }

    private ProcessResult process(List<String> command, Path dir, String input, long timeoutSeconds) throws Exception {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        try (OutputStream os = p.getOutputStream()) {
            if (input != null && !input.isEmpty()) os.write(input.getBytes(StandardCharsets.UTF_8));
        }
        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) { p.destroyForcibly(); p.waitFor(1, TimeUnit.SECONDS); }
        String output = readLimited(p.getInputStream(), MAX_OUTPUT);
        return new ProcessResult(finished, finished ? p.exitValue() : -1, output);
    }

    private String readLimited(InputStream in, int max) throws IOException {
        return new String(in.readNBytes(max), StandardCharsets.UTF_8);
    }

    private String normalizeOutput(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").replace('\r', '\n').trim().replaceAll("[ \\t]+", " ");
    }

    private String javaTool(String name) {
        String home = System.getProperty("java.home");
        Path bin = Paths.get(home, "bin", name + (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? ".exe" : ""));
        if (Files.exists(bin)) return bin.toString();
        Path homeBin = bin.getParent();
        Path unix = homeBin == null ? null : homeBin.resolve(name);
        return unix != null && Files.exists(unix) ? unix.toString() : name;
    }

    private String pythonTool() {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            if (commandAvailable("python")) return "python";
            if (commandAvailable("py")) return "py";
            return "python";
        }
        return commandAvailable("python3") ? "python3" : "python";
    }

    private boolean commandAvailable(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            boolean done = p.waitFor(2, TimeUnit.SECONDS);
            if (!done) { p.destroyForcibly(); return false; }
            return p.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public record Result(boolean success, String stage, String output) {
        public static Result fail(String stage, String output) { return new Result(false, stage, output == null ? "" : output); }
    }
    private record ProcessResult(boolean finished, int exitCode, String output) {}
}
