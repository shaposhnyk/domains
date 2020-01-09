package com.shaposhnyk;

import java.util.List;

public interface DomainList {
  void addDomain(Domain domain);

  void removeDomain(Domain domain);

  List<Domain> findParentsOf(String domainName);

  List<Domain> findSubDomains(String domainName);

  List<Domain> getDomains();

  boolean contains(Domain domain);

  boolean isEmpty();

  /**
   * Modifies input lists, removing from it all subDomains of a newDomain OR adding newDomain as
   * subDomain to the one of existing domains
   */
  default List<Domain> mergeDomain(Domain newDomain) {
    if (contains(newDomain)) { // skip duplicates
      return getDomains();
    }

    Domain parentDomain =
        findParentsOf(newDomain.getDomainName()).stream()
            .findFirst() // there should 1 parent or 0
            .orElse(null);

    if (parentDomain != null) {
      // just add newDomain as subDomain of the parent
      // I use brute force merger, but I should use DomainListMap for subDomains
      parentDomain.mergeDomain(newDomain);
      return getDomains();
    }

    List<Domain> subDomains = findSubDomains(newDomain.getDomainName());
    if (!subDomains.isEmpty()) {
      subDomains.forEach(this::removeDomain);
      subDomains.forEach(subDomain -> newDomain.mergeDomain(subDomain));
    }

    addDomain(newDomain);
    return getDomains();
  }
}
