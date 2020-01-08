/*
 * Copyright (c) 2020 by Bank Lombard Odier & Co Ltd, Geneva, Switzerland. This software is subject
 * to copyright protection under the laws of Switzerland and other countries. ALL RIGHTS RESERVED.
 *
 */

package com.shaposhnyk;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DomainServiceTest {

  @Test
  public void testDistinctDomains() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        listSourceOf("internal.acme.com", "non-internal.acme.com", "", "  ", "\t ");

    List<Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result).hasSize(2);
    assertThat(result).extracting(Domain::hasSubDomains).containsOnly(Boolean.FALSE);
  }

  @Test
  public void testSubDomains() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        listSourceOf("internal.acme.com", "one.internal.acme.com", " two.internal.acme.com");

    List<Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testSubDomainsBeforeDomain() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        listSourceOf("two.internal.acme.com", "one.internal.acme.com", "internal.acme.com");

    List<Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testSubDomainsBeforeAndAfterDomain() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        listSourceOf(
            "two.internal.acme.com", "acme.com", //
            "one.internal.acme.com", "internal.acme.com");

    List<Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result).hasSize(1).extracting(Domain::getDomainName).containsOnly("acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains().iterator().next().getSubDomains())
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testMergeSubDomains() {
    DomainService domainSrv = new DomainService();

    Domain acme = Domain.of("acme.com");
    Domain one = Domain.of("one.internal.acme.com");
    Domain two = Domain.of("two.internal.acme.com");

    DomainList list = new DomainListBruteForce(new ArrayList<>());
    list.addDomain(one);

    assertThat(domainSrv.mergeDomain(list, two)).hasSize(2).containsOnly(one, two);

    assertThat(domainSrv.mergeDomain(list, acme))
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly(acme.getDomainName());
  }

  @Test
  public void testMergeSubDomains2() {
    DomainService domainSrv = new DomainService();

    Domain acme = Domain.of("acme.com");
    Domain one = Domain.of("one.internal.acme.com");
    Domain two = Domain.of("two.internal.acme.com");
    Domain internal = Domain.of("internal.acme.com");

    acme.addSubDomain(one);
    acme.addSubDomain(two);

    assertThat(domainSrv.mergeDomain(new DomainListBruteForce(acme.getSubDomains()), internal))
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly(internal.getDomainName());

    assertThat(internal.getSubDomains()).containsOnly(one, two);
  }

  @Test
  public void testFilterBySource() {
    DomainService domainSrv = new DomainService();

    Domain one = Domain.of("one.internal.acme.com", Paths.get("B"));
    Domain internal = Domain.of("internal.acme.com", Paths.get("A"), one);
    Domain acme = Domain.of("acme.com", Paths.get("A"), internal);

    List<Domain> results =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme));

    assertThat(results).extracting(Domain::getDomainName).containsOnly(acme.getDomainName());

    assertThat(results.get(0).getSubDomains())
        .extracting(Domain::getDomainName)
        .doesNotContain(internal.getDomainName()) // filtered out
        .containsOnly(one.getDomainName());
  }

  @Test
  public void testFilterBySource2() {
    DomainService domainSrv = new DomainService();

    Domain one = Domain.of("one.internal.acme.com", Paths.get("A"));
    Domain internal = Domain.of("internal.acme.com", Paths.get("B"), one);
    Domain acme = Domain.of("acme.com", Paths.get("A"), internal);

    List<Domain> results =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme));

    assertThat(results).extracting(Domain::getDomainName).containsOnly(acme.getDomainName());

    assertThat(results.get(0).getSubDomains())
        .extracting(Domain::getDomainName)
        .doesNotContain(one.getDomainName()) // filtered out
        .containsOnly(internal.getDomainName());
  }

  @Test
  public void testSameSourcesFilteredOut() {
    DomainService domainSrv = new DomainService();

    Domain one = Domain.of("one.internal.acme.com", Paths.get("A"));
    Domain internal = Domain.of("internal.acme.com", Paths.get("A"), one);
    Domain acme = Domain.of("acme.com", Paths.get("A"), internal);
    Domain some = Domain.of("some.com", Paths.get("B"));

    assertThat(domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme, some)))
        .isEmpty();
  }

  @Test
  public void testSubDomainsAreNormalized() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        listSourceOf(
            "internal.acme.com",
            "One.internal.acme.com",
            "\tone.internal.acme.com",
            "one.internal.acme.com",
            "one.internal.acme.com   ");

    List<Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com");
  }

  @Test
  public void testSampleProblem() {
    DomainService domainSrv = new DomainService();
    List<NamedSource> src =
        NamedSources.sourcesOf("/domains1.txt", "/domains2.txt", "/domains3.txt");
    List<Domain> domains =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(
            domainSrv.domainsWithSubDomains(src));

    assertThat(domains)
        .extracting(Domain::getDomainName)
        .containsOnly("internal.acme.com", "mydb.acme.com");

    assertThat(domains)
        .filteredOn(d -> "internal.acme.com".equals(d.getDomainName()))
        .flatExtracting(d -> d.getSubDomains())
        .extracting(Domain::getDomainName)
        .containsOnly(
            "someservice-a.internal.acme.com",
            "someservice-c.internal.acme.com",
            "www.someservice-a.internal.acme.com");

    assertThat(domains)
        .filteredOn(d -> "internal.acme.com".equals(d.getDomainName()))
        .flatExtracting(d -> d.getSubDomains())
        .extracting(Domain::getSourceLocation)
        .containsOnly(Paths.get("/domains1.txt"), Paths.get("/domains3.txt"));
  }

  @Test
  public void testFindSubDomains() {
    DomainListMap list = new DomainListMap();
    list.addDomain(Domain.of("one.internal.acme.com"));
    list.addDomain(Domain.of("two.internal.acme.com"));
    list.addDomain(Domain.of("some.com"));

    assertThat(list.findSubDomains("acme.com"))
        .hasSize(2)
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");

    assertThat(list.findSubDomains("internal.acme.com"))
        .hasSize(2)
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");

    assertThat(list.findSubDomains("one.internal.acme.com"))
        .hasSize(1)
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com");

    assertThat(list.findSubDomains("ne.internal.acme.com")).isEmpty();
    assertThat(list.findSubDomains("some-one.internal.acme.com")).isEmpty();
  }

  @Test
  public void testFindParent() {
    DomainListMap list = new DomainListMap();
    list.addDomain(Domain.of("one.internal.acme.com"));
    list.addDomain(Domain.of("two.internal.acme.com"));
    list.addDomain(Domain.of("some.com"));

    assertThat(list.findParentsOf("some.one.internal.some.com"))
        .extracting(Domain::getDomainName)
        .containsOnly("some.com");

    assertThat(list.findParentsOf("some.one.internal.acme.com"))
        .extracting(Domain::getDomainName)
        .containsOnly("one.internal.acme.com");

    assertThat(list.findParentsOf("some-one.internal.acme.com")).isEmpty();
    assertThat(list.findParentsOf("one.internal.acme.com")).isEmpty();
    assertThat(list.findParentsOf("internal.acme.com")).isEmpty();
    assertThat(list.findParentsOf("acme.com")).isEmpty();
  }

  @Test
  public void runPrint() {
    DomainService domainSrv = new DomainService();
    domainSrv.solveProblem("/domains1.txt", "/domains2.txt", "/domains3.txt");
  }

  private List<NamedSource> listSourceOf(String... lines) {
    return Arrays.asList(sourceOf(Paths.get("source"), lines));
  }

  private NamedSource sourceOf(Path path, String... lines) {
    return NamedSources.of(path, Arrays.asList(lines));
  }
}
