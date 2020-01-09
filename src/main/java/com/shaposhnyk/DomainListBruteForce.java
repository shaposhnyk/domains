package com.shaposhnyk;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/** Simple brute-force list of domains */
public class DomainListBruteForce implements DomainList {
  private final List<Domain> knownDomains;

  public DomainListBruteForce(List<Domain> knownDomains) {
    this.knownDomains = knownDomains;
  }

  @Override
  public void addDomain(Domain domain) {
    knownDomains.add(domain);
  }

  @Override
  public void removeDomain(Domain domain) {
    knownDomains.remove(domain);
  }

  @Override
  public List<Domain> findParentsOf(String domainName) {
    Domain subDomain = Domain.of(domainName);
    return knownDomains.stream().filter(d -> subDomain.isSubDomainOf(d)).collect(toList());
  }

  @Override
  public List<Domain> findSubDomains(String domainName) {
    return knownDomains.stream().filter(sd -> sd.isSubDomainOf(domainName)).collect(toList());
  }

  @Override
  public List<Domain> getDomains() {
    return Collections.unmodifiableList(knownDomains);
  }

  @Override
  public boolean contains(Domain domain) {
    return knownDomains.stream().anyMatch(d -> d.getDomainName().equals(domain.getDomainName()));
  }

  @Override
  public boolean isEmpty() {
    return knownDomains.isEmpty();
  }

  @Override
  public String toString() {
    return knownDomains.toString();
  }
}
