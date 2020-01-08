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
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/** A service allowing to group domains with their sub-domains */
public class DomainService {

  /** Reads files and prints only domains with subDomains from different sources */
  public void solveProblem(String... files) {
    List<NamedSource> sources = NamedSources.sourcesOf(files);
    List<Domain> topDomains = domainsWithSubDomains(sources);
    List<Domain> domainsDiffSources = flatMapAndfilterDomainsWithDiffSourceSubDomains(topDomains);

    // we build an arbitrary-level hierarchy
    // but then in flatMapAndFilter() all subDomains are brought to the second level
    printWithIdent("", domainsDiffSources, true);
  }

  private void printWithIdent(String ident, List<Domain> domains, boolean printChildren) {
    for (Domain d : domains) {
      Path location = d.getSourceLocation().getFileName();
      System.out.println(String.format("%s%s (%s)", ident, d.getDomainName(), location));
      if (printChildren) {
        printWithIdent(ident + "  ", d.getSubDomains(), false);
      }
    }
  }

  /** @return filters out domains which have all their sub-domains in the same source */
  public List<Domain> flatMapAndfilterDomainsWithDiffSourceSubDomains(List<Domain> topDomains) {
    return topDomains.stream()
        .flatMap(DomainService::domainWithSubDomainsIfHasDifferentSources)
        .collect(toList());
  }

  /**
   * It is assumed that 1) source file is in UTF-8 encoding 2) contains a list of domain names, one
   * domain name per line 3) suppose that domains are well formed, but may contain leading or
   * trailing white-spaces
   *
   * @return group all domains with theirs sub-domains
   */
  public List<Domain> domainsWithSubDomains(List<NamedSource> sources) {
    DomainList topDomains = new DomainList();

    for (NamedSource source : sources) {
      source
          .lines()
          .map(String::trim)
          .filter(s -> !s.isEmpty()) // skip blank lines
          .filter(s -> !s.startsWith(".")) // make sure there is no malformed domains
          .map(domainName -> Domain.of(domainName, source.name()))
          .forEach(currentDomain -> mergeDomain(topDomains, currentDomain));
    }

    return topDomains.getDomains();
  }

  /**
   * @return an empty stream if domain and all it's sub-domains are from the same source, stream of
   *     domain otherwise
   */
  private static Stream<Domain> domainWithSubDomainsIfHasDifferentSources(Domain d) {
    Collection<Domain> sd = allSubDomainsWithSourceDifferent(d.getSourceLocation(), d);
    if (sd.isEmpty()) {
      return Stream.empty();
    }
    return Stream.of(Domain.of(d.getDomainName(), d.getSourceLocation(), sd));
  }

  /** @return list of sub-domains located in sources different from the given one */
  private static Collection<Domain> allSubDomainsWithSourceDifferent(Path source, Domain domain) {
    if (domain.getSubDomains().isEmpty()) {
      return Collections.emptyList();
    }

    List<Domain> list = new ArrayList<>();
    for (Domain subDomain : domain.getSubDomains()) {
      if (!source.equals(subDomain.getSourceLocation())) {
        list.add(subDomain);
      }
      list.addAll(allSubDomainsWithSourceDifferent(source, subDomain));
    }

    return list;
  }

  /** A list of domains which optimizes search of sub-domains */
  public static class DomainList {
    // perf-wise side, it's better to use HashSet, use LinkedHashSet to preserve insertion order
    private final Set<Domain> knownDomains = new LinkedHashSet<>(0);
    private final Map<String, Set<Domain>> domainsBySuffix = new HashMap<>();

    /** Adds new independent domain (i.e. not parent of an existing one, nor a child) to the list */
    void addDomain(Domain domain) {
      knownDomains.add(domain);

      String name = domain.getDomainName();
      addDomainByKey(name, domain);
      for (int i = 0; i < name.length(); i++) {
        if (name.charAt(i) == '.') {
          addDomainByKey(name.substring(i + 1), domain);
        }
      }
    }

    /**
     * Removes a domain which is already in the list
     *
     * @param domain
     */
    void removeDomain(Domain domain) {
      knownDomains.remove(domain);
      String name = domain.getDomainName();
      removeDomainByKey(name, domain);
      for (int i = 0; i < name.length(); i++) {
        if (name.charAt(i) == '.') {
          removeDomainByKey(name.substring(i + 1), domain);
        }
      }
    }

    private void addDomainByKey(String name, Domain domain) {
      Set<Domain> domains = domainsBySuffix.computeIfAbsent(name, key -> new LinkedHashSet<>());
      domains.add(domain);
    }

    private void removeDomainByKey(String name, Domain domain) {
      domainsBySuffix.get(name).remove(domain);
    }

    /** @return most specific parent for a given domainName */
    public List<Domain> findParentsOf(String domainName) {
      String currentName = domainName;
      int idx = currentName.indexOf(".");
      while (idx > 0) {
        currentName = currentName.substring(idx + 1);
        Set<Domain> potentialParents = domainsBySuffix.get(currentName);
        if (potentialParents != null) {
          int length = currentName.length();
          List<Domain> parents =
              potentialParents.stream()
                  .filter(pp -> pp.getDomainName().length() == length)
                  .collect(toList());
          if (!parents.isEmpty()) {
            return parents;
          }
        }
        idx = currentName.indexOf(".");
      }
      return Collections.emptyList();
    }

    public List<Domain> findSubDomains(String domainName) {
      Set<Domain> domains = domainsBySuffix.get(domainName);
      return domains == null ? Collections.emptyList() : new ArrayList<>(domains);
    }

    public List<Domain> getDomains() {
      return new ArrayList<>(knownDomains);
    }

    @Override
    public String toString() {
      return knownDomains.toString();
    }
  }

  /**
   * Modifies input lists, removing from it all subDomains of a newDomain OR adding newDomain as
   * subDomain to the one of existing domains
   */
  public List<Domain> mergeDomain(DomainList knownDomains, Domain newDomain) {
    Domain parentDomain =
        knownDomains.findParentsOf(newDomain.getDomainName()).stream()
            .findFirst() // there should 1 parent or 0
            .orElse(null);

    if (parentDomain != null) {
      // just add newDomain as subDomain of the parent
      // I use brute force merger, but I should use DomainList for subDomains
      mergeDomainV0(parentDomain.getSubDomains(), newDomain);
      return knownDomains.getDomains();
    }

    List<Domain> subDomains = knownDomains.findSubDomains(newDomain.getDomainName());
    if (!subDomains.isEmpty()) {
      subDomains.forEach(knownDomains::removeDomain);
      subDomains.forEach(subDomain -> mergeDomainV0(newDomain.getSubDomains(), subDomain));
    }

    knownDomains.addDomain(newDomain);
    return knownDomains.getDomains();
  }

  /**
   * Modifies input lists, removing from it all subDomains of a newDomain OR adding newDomain as
   * subDomain to the one of existing domains. Brute-foce version of algorithm
   */
  public List<Domain> mergeDomainV0(List<Domain> knownDomains, Domain newDomain) {
    // this for-loop will be very inefficient on lange number of domains
    // should be replaced with a more efficient method, like tree-search or hashing

    boolean matchFound = false;
    List<Domain> allSubDomainsOfNewDomain = new ArrayList<>();
    for (int i = knownDomains.size() - 1; i >= 0; i--) {
      Domain d = knownDomains.get(i);
      if (d.getDomainName().equals(newDomain.getDomainName())) {
        return knownDomains; // nothing to do - newDomain is a duplicate of existing one
      } else if (d.isSubDomainOf(newDomain)) {
        knownDomains.remove(i);
        allSubDomainsOfNewDomain.add(d);
      } else if (newDomain.isSubDomainOf(d)) {
        mergeDomainV0(d.getSubDomains(), newDomain);
        matchFound = true;
        break;
      }
    }

    if (!matchFound) {
      knownDomains.add(newDomain);
    }
    for (Domain subDomain : allSubDomainsOfNewDomain) {
      mergeDomainV0(newDomain.getSubDomains(), subDomain);
    }
    return knownDomains;
  }

  /** NamedSource - and abstraction over files or resources (for testing) containing text lines */
  public interface NamedSource {
    Path name();

    Stream<String> lines();
  }

  /**
   * NamedSource factory class. Normally should be into a separate java file. Suppose that all our
   * files are UTF8-encoded
   */
  public static final class NamedSources {
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

  /**
   * Domain with a mutable list of sub-domains. And it's source location.
   *
   * <p>Depending on tasks, it may be better to have a Domain(domainName,subDomains) and a value
   * class Sourced<T>, then we would use a composition to create a domain with a source, i.e.
   * Sourced<Domain>
   *
   * <p>I would use here a Kotlin's data class, or java 14 ;) data class
   */
  public static class Domain {
    private final String domainName;

    private final Path sourceLocation;

    private final List<Domain> subDomains;

    Domain(String domainName, Path sourceLocation, List<Domain> subDomains) {
      this.domainName = Objects.requireNonNull(domainName);
      this.sourceLocation = sourceLocation;
      this.subDomains = subDomains;
    }

    // @VisibleForTesting
    public static Domain of(String domainName) {
      return of(domainName, Paths.get(""));
    }

    /** @return domain w/o sub-domains from a location */
    public static Domain of(String domainName, Path path) {
      return of(domainName, path, new ArrayList<>(0));
    }

    /**
     * Creates a domain from it's name, path and a list of sub-domains Domain name will be converted
     * to lowercase
     *
     * @return domain (from a location) with sub-domains
     */
    public static Domain of(String domainName, Path path, Domain... subDomains) {
      return of(domainName, path, Arrays.asList(subDomains));
    }

    public static Domain of(String domainName, Path path, Collection<Domain> subDomains) {
      // if we aim to support IDN's, then probably usage of toLowerCase() should be
      // reconsidered
      return new Domain(
          domainName.toLowerCase(), path, new ArrayList<>(subDomains)); // preserve-order
    }

    public String getDomainName() {
      return domainName;
    }

    public Path getSourceLocation() {
      return sourceLocation;
    }

    public List<Domain> getSubDomains() {
      // it would be good to retourn here Collections.unmodifiableList(), if possible
      return subDomains;
    }

    public boolean isSubDomainOf(Domain domain) {
      return isSubDomainOf(domain.getDomainName());
    }

    /** @return true if supposedParent is a parent domain of this domain */
    public boolean isSubDomainOf(String supposedParent) {
      return this.domainName.length() > supposedParent.length()
          // there MUST be a point just before parent domain
          && '.' == this.domainName.charAt(this.domainName.length() - supposedParent.length() - 1)
          && this.domainName.endsWith(supposedParent);
    }

    public void addSubDomain(Domain d) {
      this.subDomains.add(d);
    }

    public boolean hasSubDomains() {
      return !this.subDomains.isEmpty();
    }

    @Override
    public int hashCode() {
      return Objects.hash(domainName, sourceLocation);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Domain another = (Domain) obj;
      return Objects.equals(domainName, another.domainName)
          && Objects.equals(sourceLocation, another.sourceLocation)
          && Objects.equals(subDomains, another.subDomains);
    }

    @Override
    public String toString() {
      return domainName;
    }
  }
}
