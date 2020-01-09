package com.shaposhnyk;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Domain with a mutable list of sub-domains. And it's source location.
 *
 * <p>Depending on tasks, it may be better to have a Domain(domainName,subDomains) and a value class
 * Sourced<T>, then we would use a composition to create a domain with a source, i.e.
 * Sourced<Domain>
 *
 * <p>I would use here a Kotlin's data class, or java 14 ;) data class
 */
public class Domain {
  private final String domainName;

  private final Path sourceLocation;

  private final DomainList subDomains;

  Domain(String domainName, Path sourceLocation, List<Domain> subDomains) {
    this.domainName = Objects.requireNonNull(domainName);
    this.sourceLocation = sourceLocation;
    this.subDomains = new DomainListMap();
    subDomains.forEach(this.subDomains::addDomain);
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
    return subDomains.getDomains();
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
    this.subDomains.mergeDomain(d);
  }

  public boolean hasSubDomains() {
    return !this.subDomains.getDomains().isEmpty();
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

  public void mergeDomain(Domain newDomain) {
    this.subDomains.mergeDomain(newDomain);
  }
}
