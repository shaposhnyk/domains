package com.shaposhnyk;

import java.nio.file.Path;
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
    DomainList topDomains = new DomainListMap();

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

  /**
   * Modifies input lists, removing from it all subDomains of a newDomain OR adding newDomain as
   * subDomain to the one of existing domains
   */
  public List<Domain> mergeDomain(DomainList knownDomains, Domain newDomain) {
    return knownDomains.mergeDomain(newDomain);
  }
}
