package io.github.gaming32.allaudiofiles;

import org.bytedeco.javacpp.Loader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class JavacppWrapper {
    private static final String MAVEN_URL = "https://repo.maven.apache.org/maven2/";

    private static final List<Path> LIB_ROOTS = new ArrayList<>();

    public static void init() {
        if (!LIB_ROOTS.isEmpty()) return;

        System.setProperty("org.bytedeco.javacpp.logger", "slf4j");

        final String javacppVersion;
        try {
            javacppVersion = Loader.getVersion();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        AllAudioFiles.LOGGER.info("JavaCPP version: {}", javacppVersion);

        LIB_ROOTS.add(getLibRoot(downloadArtifact(
            "org.bytedeco", "javacpp", javacppVersion, Loader.getPlatform()
        )));
        LIB_ROOTS.add(getLibRoot(downloadArtifact(
            "org.bytedeco", "ffmpeg", "5.1.2-" + javacppVersion, Loader.getPlatform()
        )));
    }

    public static Enumeration<URL> getResources(String path, @Nullable Class<?> owner) {
        if (path.contains("drm")) {
            // Hack to fix weird libdrm conflict with GLFW on non-NVIDIA graphics cards
            return Collections.emptyEnumeration();
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        } else if (owner != null) {
            path = owner.getPackageName().replace('.', '/') + '/' + path;
        }
        final String fpath = path;
        final Iterator<URL> itResult = LIB_ROOTS.stream()
            .map(root -> {
                final Path resultPath = root.resolve(fpath);
                if (Files.exists(resultPath)) {
                    try {
                        return resultPath.toUri().toURL();
                    } catch (MalformedURLException e) {
                        AllAudioFiles.LOGGER.warn("Failed to convert path {} to URL", resultPath, e);
                    }
                }
                return null;
            })
            .filter(Objects::nonNull)
            .iterator();
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return itResult.hasNext();
            }

            @Override
            public URL nextElement() {
                return itResult.next();
            }
        };
    }

    @Nullable
    public static URL getResource(String path, @Nullable Class<?> owner) {
        final Enumeration<URL> result = getResources(path, owner);
        return result.hasMoreElements() ? result.nextElement() : null;
    }

    @Nullable
    public static InputStream getResourceAsStream(String path, @Nullable Class<?> owner) {
        final URL resource = getResource(path, owner);
        if (resource == null) {
            return null;
        }
        try {
            return resource.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    private static Path getLibRoot(Path fsPath) {
        try {
            return FileSystems.getFileSystem(URI.create("jar:" + fsPath.toUri() + "!/")).getRootDirectories().iterator().next();
        } catch (FileSystemNotFoundException e) {
            try {
                return FileSystems.newFileSystem(fsPath).getRootDirectories().iterator().next();
            } catch (IOException e1) {
                throw new UncheckedIOException(e1);
            }
        }
    }

    private static Path downloadArtifact(
        String group,
        String artifact,
        String version,
        @Nullable String classifier
    ) {
        final String prettyName = group + ':' + artifact + ':' + version + (classifier != null ? ':' + classifier : "");
        final String mavenPath = group.replace('.', '/') + '/' +
            artifact + '/' +
            version + '/' +
            artifact + '-' + version + (classifier != null ? '-' + classifier : "") + ".jar";
        final Path destPath = Path.of(System.getProperty("user.home")).resolve(".m2/repository").resolve(mavenPath);
        if (Files.isRegularFile(destPath)) {
            AllAudioFiles.LOGGER.info("Skipping download of {}, as it already exists in Maven Local", prettyName);
            return destPath;
        }
        AllAudioFiles.LOGGER.info("Downloading {} from Maven Central", prettyName);
        try {
            Files.createDirectories(destPath.getParent());
            final URL downloadUrl = new URL(MAVEN_URL + mavenPath);
            try (InputStream is = downloadUrl.openStream()) {
                Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            AllAudioFiles.LOGGER.error("Failed to download {} from Maven Central", prettyName, e);
            final RuntimeException throwException = e instanceof RuntimeException re ? re : new RuntimeException(e);
            try {
                Files.delete(destPath);
            } catch (IOException e1) {
                throwException.addSuppressed(e1);
            }
            throw throwException;
        }
        AllAudioFiles.LOGGER.info("Downloaded {} from Maven Central", prettyName);
        return destPath;
    }
}
