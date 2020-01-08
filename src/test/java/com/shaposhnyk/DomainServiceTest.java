/*
 * Copyright (c) 2020 by Bank Lombard Odier & Co Ltd, Geneva, Switzerland. This software is subject
 * to copyright protection under the laws of Switzerland and other countries. ALL RIGHTS RESERVED.
 *
 */

package com.shaposhnyk;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
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
  @Ignore
  public void testSampleProblem() {
    DomainService domainSrv = new DomainService();
    List<DomainService.NamedSource> src =
        DomainService.NamedSources.sourcesOf("/domains1.txt", "/domains2.txt", "/domains3.txt");
    List<DomainService.Domain> domains =
        domainSrv.filterDomainsWithDiffSourceSubDomains(domainSrv.domainsWithSubDomains(src));

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

  private List<DomainService.NamedSource> listSourceOf(String... lines) {
    return Arrays.asList(sourceOf(Paths.get("source"), lines));
  }

  private DomainService.NamedSource sourceOf(Path path, String... lines) {
    return DomainService.NamedSources.of(path, Arrays.asList(lines));
  }
}
