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
    List<DomainService.NamedSource> src =
        listSourceOf("internal.acme.com", "non-internal.acme.com", "", "  ", "\t ");

    List<DomainService.Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result).hasSize(2);
    assertThat(result).extracting(DomainService.Domain::hasSubDomains).containsOnly(Boolean.FALSE);
  }

  @Test
  public void testSubDomains() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        listSourceOf("internal.acme.com", "one.internal.acme.com", " two.internal.acme.com");

    List<DomainService.Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testSubDomainsBeforeDomain() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        listSourceOf("two.internal.acme.com", "one.internal.acme.com", "internal.acme.com");

    List<DomainService.Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testSubDomainsBeforeAndAfterDomain() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        listSourceOf(
            "two.internal.acme.com", "acme.com", //
            "one.internal.acme.com", "internal.acme.com");

    List<DomainService.Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains().iterator().next().getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("one.internal.acme.com", "two.internal.acme.com");
  }

  @Test
  public void testMergeSubDomains() {
    DomainService domainSrv = new DomainService();

    DomainService.Domain acme = DomainService.Domain.of("acme.com");
    DomainService.Domain one = DomainService.Domain.of("one.internal.acme.com");
    DomainService.Domain two = DomainService.Domain.of("two.internal.acme.com");

    List<DomainService.Domain> list = new ArrayList<>();
    list.add(one);

    assertThat(domainSrv.mergeDomain(list, two)).hasSize(2).containsOnly(one, two);

    assertThat(domainSrv.mergeDomain(list, acme))
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly(acme.getDomainName());
  }

  @Test
  public void testMergeSubDomains2() {
    DomainService domainSrv = new DomainService();

    DomainService.Domain acme = DomainService.Domain.of("acme.com");
    DomainService.Domain one = DomainService.Domain.of("one.internal.acme.com");
    DomainService.Domain two = DomainService.Domain.of("two.internal.acme.com");
    DomainService.Domain internal = DomainService.Domain.of("internal.acme.com");

    acme.addSubDomain(one);
    acme.addSubDomain(two);

    assertThat(domainSrv.mergeDomain(acme.getSubDomains(), internal))
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly(internal.getDomainName());

    assertThat(internal.getSubDomains()).containsOnly(one, two);
  }

  @Test
  public void testFilterBySource() {
    DomainService domainSrv = new DomainService();

    DomainService.Domain one = DomainService.Domain.of("one.internal.acme.com", Paths.get("B"));
    DomainService.Domain internal =
        DomainService.Domain.of("internal.acme.com", Paths.get("A"), one);
    DomainService.Domain acme = DomainService.Domain.of("acme.com", Paths.get("A"), internal);

    List<DomainService.Domain> results =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme));

    assertThat(results)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly(acme.getDomainName());

    assertThat(results.get(0).getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .doesNotContain(internal.getDomainName()) // filtered out
        .containsOnly(one.getDomainName());
  }

  @Test
  public void testFilterBySource2() {
    DomainService domainSrv = new DomainService();

    DomainService.Domain one = DomainService.Domain.of("one.internal.acme.com", Paths.get("A"));
    DomainService.Domain internal =
        DomainService.Domain.of("internal.acme.com", Paths.get("B"), one);
    DomainService.Domain acme = DomainService.Domain.of("acme.com", Paths.get("A"), internal);

    List<DomainService.Domain> results =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme));

    assertThat(results)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly(acme.getDomainName());

    assertThat(results.get(0).getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .doesNotContain(one.getDomainName()) // filtered out
        .containsOnly(internal.getDomainName());
  }

  @Test
  public void testSameSourcesFilteredOut() {
    DomainService domainSrv = new DomainService();

    DomainService.Domain one = DomainService.Domain.of("one.internal.acme.com", Paths.get("A"));
    DomainService.Domain internal =
        DomainService.Domain.of("internal.acme.com", Paths.get("A"), one);
    DomainService.Domain acme = DomainService.Domain.of("acme.com", Paths.get("A"), internal);
    DomainService.Domain some = DomainService.Domain.of("some.com", Paths.get("B"));

    assertThat(domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(Arrays.asList(acme, some)))
        .isEmpty();
  }

  @Test
  public void testSubDomainsAreNormalized() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        listSourceOf(
            "internal.acme.com",
            "One.internal.acme.com",
            "\tone.internal.acme.com",
            "one.internal.acme.com",
            "one.internal.acme.com   ");

    List<DomainService.Domain> result = domainSrv.domainsWithSubDomains(src);
    assertThat(result)
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("internal.acme.com");

    assertThat(result.iterator().next().getSubDomains())
        .hasSize(1)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("one.internal.acme.com");
  }

  @Test
  public void testSampleProblem() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        DomainService.NamedSources.sourcesOf("/domains1.txt", "/domains2.txt", "/domains3.txt");
    List<DomainService.Domain> domains =
        domainSrv.flatMapAndfilterDomainsWithDiffSourceSubDomains(domainSrv.domainsWithSubDomains(src));

    assertThat(domains)
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly("internal.acme.com", "mydb.acme.com");

    assertThat(domains)
        .filteredOn(d -> "internal.acme.com".equals(d.getDomainName()))
        .flatExtracting(d -> d.getSubDomains())
        .extracting(DomainService.Domain::getDomainName)
        .containsOnly(
            "someservice-a.internal.acme.com",
            "someservice-c.internal.acme.com",
            "www.someservice-a.internal.acme.com");

    assertThat(domains)
        .filteredOn(d -> "internal.acme.com".equals(d.getDomainName()))
        .flatExtracting(d -> d.getSubDomains())
        .extracting(DomainService.Domain::getSourceLocation)
        .containsOnly(Paths.get("/domains1.txt"), Paths.get("/domains3.txt"));
  }

  @Test
  public void runPrint() {
    DomainService domainSrv = new DomainService();
    domainSrv.solveProblem("/domains1.txt", "/domains2.txt", "/domains3.txt");
  }

  private List<DomainService.NamedSource> listSourceOf(String... lines) {
    return Arrays.asList(sourceOf(Paths.get("source"), lines));
  }

  private DomainService.NamedSource sourceOf(Path path, String... lines) {
    return DomainService.NamedSources.of(path, Arrays.asList(lines));
  }
}
