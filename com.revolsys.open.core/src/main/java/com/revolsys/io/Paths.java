package com.revolsys.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.List;

import com.revolsys.util.WrappedException;

public class Paths {
  public static boolean exists(final Path path) {
    return Files.exists(path);
  }

  public static Path get(final File file) {
    if (file != null) {
      final File parentFile = file.getParentFile();
      parentFile.mkdirs();
      return file.toPath();
    }
    return null;
  }

  public static Path get(final String first, final String... more) {
    return java.nio.file.Paths.get(first, more);
  }

  public static String getBaseName(final java.nio.file.Path path) {
    final String fileName = getFileName(path);
    return FileNames.getBaseName(fileName);
  }

  public static String getFileName(final Path path) {
    if (path.getNameCount() == 0) {
      final String fileName = path.toString();
      if (fileName.endsWith("\\") || fileName.endsWith("\\")) {
        return fileName.substring(0, fileName.length() - 1);
      } else {
        return fileName;
      }
    } else {
      final Path fileNamePath = path.getFileName();
      final String fileName = fileNamePath.toString();
      if (fileName.endsWith("\\") || fileName.endsWith("/")) {
        return fileName.substring(0, fileName.length() - 1);
      } else {
        return fileName;
      }
    }
  }

  public static String getFileNameExtension(final Path path) {
    final String fileName = getFileName(path);
    return FileNames.getFileNameExtension(fileName);
  }

  public static List<String> getFileNameExtensions(final Path path) {
    final String fileName = getFileName(path);
    return FileNames.getFileNameExtensions(fileName);
  }

  public static Path getPath(final Path path) {
    return path.toAbsolutePath();
  }

  public static Path getPath(final Path parent, final String path) {
    final Path childPath = parent.resolve(path);
    return getPath(childPath);
  }

  public static Path getPath(final String name) {
    final Path path = Paths.get(name);
    return getPath(path);
  }

  public static boolean isHidden(final Path path) {
    try {
      if (Files.exists(path)) {
        final Path root = path.getRoot();
        if (!root.equals(path)) {
          final BasicFileAttributes attributes = Files.readAttributes(path,
            BasicFileAttributes.class);
          if (attributes instanceof DosFileAttributes) {
            final DosFileAttributes dosAttributes = (DosFileAttributes)attributes;
            return dosAttributes.isHidden();
          } else {
            final File file = path.toFile();
            return file.isHidden();
          }
        }
      }
    } catch (final Throwable e) {
      return false;
    }
    return false;
  }

  public static OutputStream outputStream(final Path path) {
    try {
      return Files.newOutputStream(path);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  public static Path withExtension(final Path path, final String extension) {
    final String baseName = getBaseName(path);
    final String newFileName = baseName + "." + extension;
    final Path parent = path.getParent();
    return parent.resolve(newFileName);
  }

  public static Writer writer(final Path path) {
    try {
      return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }
}
