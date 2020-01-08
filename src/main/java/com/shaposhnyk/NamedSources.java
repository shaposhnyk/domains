package com.shaposhnyk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * NamedSource factory class. Normally should be into a separate java file. Suppose that all our
 * files are UTF8-encoded
 */
public final class NamedSources {
  private static final Logger logger = LoggerFactory.getLogger(NamedSources.class);

  public static List<NamedSource> sourcesOf(List<Path> fileNames) {
    return fileNames.stream().map(NamedSources::of).collect(toList());
  }

  /** @return file to a NamedSource */
  public static NamedSource of(Path path) {
    Objects.requireNonNull(path);
    return new NamedSource() {
      @Override
      public Path name() {
        return path;
      }

      @Override
      public Stream<String> lines() {
        try {
          return Files.lines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
          logger.warn("Unable to read source file");
          return Stream.empty(); // depending on cases, rethrowing an exception may be more
          // appropriate
        }
      }
    };
  }

  public static List<NamedSource> sourcesOf(String... resourceNames) {
    return Arrays.stream(resourceNames).map(NamedSources::of).collect(toList());
  }

  /** @return resource to a NamedSource */
  public static NamedSource of(String resourceName) {
    Objects.requireNonNull(resourceName);
    Path resourcePath = Paths.get(resourceName);
    return new NamedSource() {
      @Override
      public Path name() {
        return resourcePath;
      }

      @Override
      public Stream<String> lines() {
        InputStream resource = DomainService.class.getResourceAsStream(resourceName);
        return new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))
            .lines();
      }
    };
  }

  /** @return predefined lines */
  // @VisibleForTesting
  public static NamedSource of(Path name, Collection<String> lines) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(lines);
    return new NamedSource() {
      @Override
      public Path name() {
        return name;
      }

      @Override
      public Stream<String> lines() {
        return lines.stream();
      }
    };
  }
}
