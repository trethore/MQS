/*
 * My QOL Packages - Client-side Minecraft modding at runtime
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.trethore.myqolpackages.api.config;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record InternetPermissions(InternetAccessPermission access, List<String> domains) {
  private static final InternetPermissions NONE =
      new InternetPermissions(InternetAccessPermission.NONE, List.of());

  public InternetPermissions {
    access = access == null ? InternetAccessPermission.NONE : access;
    domains = access == InternetAccessPermission.DOMAINS ? normalizeDomains(domains) : List.of();
  }

  public static InternetPermissions none() {
    return NONE;
  }

  public static InternetPermissions domains(Collection<String> domains) {
    return new InternetPermissions(InternetAccessPermission.DOMAINS, new ArrayList<>(domains));
  }

  public static InternetPermissions full() {
    return new InternetPermissions(InternetAccessPermission.FULL, List.of());
  }

  public boolean allows(InternetPermissions requested) {
    Objects.requireNonNull(requested, "requested");
    if (requested.access == InternetAccessPermission.NONE
        || access == InternetAccessPermission.FULL) {
      return true;
    }
    if (access != InternetAccessPermission.DOMAINS
        || requested.access != InternetAccessPermission.DOMAINS) {
      return false;
    }
    for (String requestedDomain : requested.domains) {
      if (!allowsPattern(requestedDomain)) {
        return false;
      }
    }
    return true;
  }

  public boolean allowsHost(String host) {
    if (access == InternetAccessPermission.NONE) {
      return false;
    }
    if (access == InternetAccessPermission.FULL) {
      return true;
    }
    String normalizedHost = normalizeHost(host);
    for (String domain : domains) {
      if (matches(domain, normalizedHost)) {
        return true;
      }
    }
    return false;
  }

  public InternetPermissions addDomain(String domain) {
    List<String> updatedDomains = new ArrayList<>(requireDomainsMode());
    updatedDomains.add(domain);
    return domains(updatedDomains);
  }

  public InternetPermissions removeDomain(String domain) {
    String normalizedDomain = normalizePattern(domain);
    List<String> updatedDomains = new ArrayList<>(requireDomainsMode());
    updatedDomains.remove(normalizedDomain);
    return domains(updatedDomains);
  }

  public InternetPermissions clearDomains() {
    requireDomainsMode();
    return domains(List.of());
  }

  public static String normalizeHost(String host) {
    String normalizedHost = normalizeDomainName(host);
    if (normalizedHost.indexOf('*') >= 0) {
      throw new IllegalArgumentException("Internet host must not contain a wildcard: " + host);
    }
    return normalizedHost;
  }

  public static String normalizePattern(String pattern) {
    String normalizedPattern = Objects.requireNonNull(pattern, "pattern").trim();
    if (normalizedPattern.startsWith("*.")) {
      String suffix = normalizeDomainName(normalizedPattern.substring(2));
      if (suffix.indexOf('*') >= 0 || isIpLiteral(suffix)) {
        throw new IllegalArgumentException("Invalid wildcard internet domain: " + pattern);
      }
      return "*." + suffix;
    }
    String domain = normalizeDomainName(normalizedPattern);
    if (domain.indexOf('*') >= 0) {
      throw new IllegalArgumentException(
          "Wildcard must be the complete leftmost label: " + pattern);
    }
    return domain;
  }

  private boolean allowsPattern(String requestedPattern) {
    for (String grantedPattern : domains) {
      if (covers(grantedPattern, requestedPattern)) {
        return true;
      }
    }
    return false;
  }

  private List<String> requireDomainsMode() {
    if (access != InternetAccessPermission.DOMAINS) {
      throw new IllegalStateException("Internet access mode must be domains");
    }
    return domains;
  }

  private static List<String> normalizeDomains(List<String> domains) {
    if (domains == null || domains.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> normalizedDomains = new LinkedHashSet<>();
    for (String domain : domains) {
      normalizedDomains.add(normalizePattern(domain));
    }
    return List.copyOf(normalizedDomains);
  }

  private static boolean covers(String grantedPattern, String requestedPattern) {
    boolean grantedWildcard = grantedPattern.startsWith("*.");
    boolean requestedWildcard = requestedPattern.startsWith("*.");
    if (!grantedWildcard) {
      return !requestedWildcard && grantedPattern.equals(requestedPattern);
    }
    String grantedSuffix = grantedPattern.substring(2);
    String requestedSuffix = requestedWildcard ? requestedPattern.substring(2) : requestedPattern;
    if (requestedSuffix.equals(grantedSuffix)) {
      return requestedWildcard;
    }
    return requestedSuffix.endsWith("." + grantedSuffix);
  }

  private static boolean matches(String pattern, String host) {
    if (!pattern.startsWith("*.")) {
      return pattern.equals(host);
    }
    String suffix = pattern.substring(2);
    return !host.equals(suffix) && host.endsWith("." + suffix);
  }

  private static String normalizeDomainName(String input) {
    String domain = Objects.requireNonNull(input, "domain").trim().toLowerCase(Locale.ROOT);
    while (domain.endsWith(".")) {
      domain = domain.substring(0, domain.length() - 1);
    }
    if (domain.isEmpty()
        || domain.contains("://")
        || domain.indexOf('/') >= 0
        || domain.indexOf('@') >= 0
        || domain.indexOf(':') >= 0) {
      throw new IllegalArgumentException("Invalid internet domain: " + input);
    }
    try {
      String ascii = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
      if (ascii.length() > 253) {
        throw new IllegalArgumentException("Internet domain is too long: " + input);
      }
      return ascii;
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid internet domain: " + input, exception);
    }
  }

  private static boolean isIpLiteral(String value) {
    if (value.indexOf(':') >= 0) {
      return true;
    }
    String[] labels = value.split("\\.", -1);
    if (labels.length != 4) {
      return false;
    }
    for (String label : labels) {
      try {
        int octet = Integer.parseInt(label);
        if (octet < 0 || octet > 255) {
          return false;
        }
      } catch (NumberFormatException exception) {
        return false;
      }
    }
    return true;
  }
}
