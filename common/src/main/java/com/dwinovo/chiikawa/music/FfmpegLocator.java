package com.dwinovo.chiikawa.music;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class FfmpegLocator {
    private static final boolean WINDOWS = System.getProperty("os.name", "")
        .toLowerCase(Locale.ROOT)
        .contains("win");

    private FfmpegLocator() {
    }

    static String resolve(String configuredPath, Path devTool) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return normalizeConfiguredPath(configuredPath);
        }
        Optional<Path> discovered = findExecutable();
        if (discovered.isPresent()) {
            return discovered.get().toAbsolutePath().toString();
        }
        if (Files.isRegularFile(devTool)) {
            return devTool.toAbsolutePath().toString();
        }
        return WINDOWS ? "ffmpeg.exe" : "ffmpeg";
    }

    private static String normalizeConfiguredPath(String configuredPath) {
        try {
            Path path = Path.of(configuredPath);
            if (Files.isDirectory(path)) {
                Path executable = path.resolve(executableName());
                if (Files.isRegularFile(executable)) {
                    return executable.toAbsolutePath().toString();
                }
            }
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().toString();
            }
        } catch (RuntimeException ignored) {
        }
        return configuredPath;
    }

    static Optional<Path> findExecutable() {
        for (Path directory : searchDirectories()) {
            Path executable = directory.resolve(executableName());
            if (Files.isRegularFile(executable)) {
                return Optional.of(executable);
            }
        }
        return Optional.empty();
    }

    private static List<Path> searchDirectories() {
        Set<Path> directories = new LinkedHashSet<>();
        addPathEntries(directories, System.getenv("PATH"));
        addPathEntries(directories, System.getenv("Path"));
        if (WINDOWS) {
            addWindowsRegistryPath(directories, "HKCU\\Environment");
            addWindowsRegistryPath(directories, "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment");
            addWindowsCommonDirectories(directories);
        } else {
            addPath(directories, "/usr/local/bin");
            addPath(directories, "/usr/bin");
        }
        return new ArrayList<>(directories);
    }

    private static void addWindowsRegistryPath(Set<Path> directories, String key) {
        try {
            Process process = new ProcessBuilder("reg", "query", key, "/v", "Path").start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return;
            }
            if (process.exitValue() != 0) {
                return;
            }
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            for (String line : output.split("\\R")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("Path")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+", 3);
                if (parts.length == 3) {
                    addPathEntries(directories, expandWindowsPath(parts[2]));
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
    }

    private static String expandWindowsPath(String value) {
        String expanded = value;
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            expanded = expanded.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return expanded;
    }

    private static void addWindowsCommonDirectories(Set<Path> directories) {
        addEnvPath(directories, "LOCALAPPDATA", "Microsoft\\WinGet\\Links");
        addEnvPath(directories, "ProgramData", "chocolatey\\bin");
        addEnvPath(directories, "USERPROFILE", "scoop\\shims");
        addPath(directories, "C:\\ffmpeg\\bin");
        addPath(directories, "C:\\Program Files\\ffmpeg\\bin");
    }

    private static void addEnvPath(Set<Path> directories, String envName, String child) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            addPath(directories, Path.of(value).resolve(child).toString());
        }
    }

    private static void addPathEntries(Set<Path> directories, String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            return;
        }
        for (String entry : pathValue.split(java.io.File.pathSeparator)) {
            addPath(directories, entry);
        }
    }

    private static void addPath(Set<Path> directories, String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        try {
            directories.add(Path.of(entry.trim()));
        } catch (RuntimeException ignored) {
        }
    }

    private static String executableName() {
        return WINDOWS ? "ffmpeg.exe" : "ffmpeg";
    }
}
