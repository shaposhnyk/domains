package com.shaposhnyk;

import java.nio.file.Path;
import java.util.stream.Stream;

/** NamedSource - and abstraction over files or resources (for testing) containing text lines */
public interface NamedSource {
  Path name();

  Stream<String> lines();
}
