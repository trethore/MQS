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
package io.github.trethore.myqolpackages.internal.runtime.http;

import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

final class HttpDestinationValidator {
  private HttpDestinationValidator() {}

  static URI validate(URI uri, InternetPermissions permissions) {
    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
    }
    if (uri.getRawUserInfo() != null) {
      throw new IllegalArgumentException("URL credentials are not allowed");
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("URL must contain a valid host");
    }
    String permissionHost = stripIpv6Brackets(host);
    if (!permissions.allowsHost(permissionHost)) {
      throw new SecurityException("Internet access is not permitted for host: " + permissionHost);
    }
    String normalizedHost = permissionHost.toLowerCase(Locale.ROOT);
    if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")) {
      throw new SecurityException("Local network destinations are not allowed");
    }
    try {
      InetAddress[] addresses = InetAddress.getAllByName(permissionHost);
      for (InetAddress address : addresses) {
        if (!isPublic(address)) {
          throw new SecurityException("Local or private network destinations are not allowed");
        }
      }
    } catch (UnknownHostException exception) {
      throw new IllegalArgumentException(
          "Could not resolve URL host: " + permissionHost, exception);
    }
    return uri;
  }

  private static boolean isPublic(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return false;
    }
    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address) {
      int first = Byte.toUnsignedInt(bytes[0]);
      int second = Byte.toUnsignedInt(bytes[1]);
      int third = Byte.toUnsignedInt(bytes[2]);
      return first != 0
          && first != 10
          && first != 127
          && !(first == 100 && second >= 64 && second <= 127)
          && !(first == 169 && second == 254)
          && !(first == 172 && second >= 16 && second <= 31)
          && !(first == 192 && second == 0 && (third == 0 || third == 2))
          && !(first == 192 && second == 168)
          && !(first == 198 && (second == 18 || second == 19))
          && !(first == 198 && second == 51 && third == 100)
          && !(first == 203 && second == 0 && third == 113)
          && first < 224;
    }
    if (address instanceof Inet6Address) {
      int first = Byte.toUnsignedInt(bytes[0]);
      return (first & 0xfe) != 0xfc;
    }
    return false;
  }

  private static String stripIpv6Brackets(String host) {
    return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
  }
}
