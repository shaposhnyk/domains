package com.shaposhnyk;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * A list of domains which optimizes search of sub-domains. Given structure complexity, I should
 * really measure if it worth to be used
 */
public class DomainListMap implements DomainList {
  // perf-wise side, it's better to use HashSet, use LinkedHashSet to preserve insertion order
  private final Set<Domain> knownDomains = new LinkedHashSet<>(0);
  private final Map<String, Set<Domain>> domainsBySuffix = new HashMap<>();

  /** Adds new independent domain (i.e. not parent of an existing one, nor a child) to the list */
  @Override
  public void addDomain(Domain domain) {
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
  @Override
  public void removeDomain(Domain domain) {
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
  @Override
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

  @Override
  public List<Domain> findSubDomains(String domainName) {
    Set<Domain> domains = domainsBySuffix.get(domainName);
    return domains == null ? Collections.emptyList() : new ArrayList<>(domains);
  }

  @Override
  public List<Domain> getDomains() {
    return new ArrayList<>(knownDomains);
  }

  @Override
  public String toString() {
    return knownDomains.toString();
  }
}
