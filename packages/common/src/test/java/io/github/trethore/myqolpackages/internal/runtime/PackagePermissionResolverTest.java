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
package io.github.trethore.myqolpackages.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.api.config.FileSystemPermissionOverrides;
import io.github.trethore.myqolpackages.api.config.FileSystemPermissions;
import io.github.trethore.myqolpackages.api.config.FileSystemReadPermission;
import io.github.trethore.myqolpackages.api.config.FileSystemWritePermission;
import io.github.trethore.myqolpackages.api.config.HostAccessPermission;
import io.github.trethore.myqolpackages.api.config.HostClassLookupPermission;
import io.github.trethore.myqolpackages.api.config.InternetPermissions;
import io.github.trethore.myqolpackages.api.config.MqpPermissionsConfig;
import io.github.trethore.myqolpackages.api.config.PackagePermissionOverrides;
import io.github.trethore.myqolpackages.api.config.PackagePermissions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PackagePermissionResolverTest {
  @Test
  void keepsRequestedPermissionsWhenGrantIsBroader() throws PackageLifecycleException {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            new FileSystemPermissions(
                FileSystemReadPermission.PACKAGE, FileSystemWritePermission.DATA));
    MqpPermissionsConfig configuration =
        new MqpPermissionsConfig(
            null,
            Map.of(
                "example-package",
                new PackagePermissionOverrides(
                    HostAccessPermission.FULL,
                    HostClassLookupPermission.ALL,
                    new FileSystemPermissionOverrides(
                        FileSystemReadPermission.ALL, FileSystemWritePermission.ALL))));

    assertEquals(
        requested, PackagePermissionResolver.resolve("example-package", requested, configuration));
  }

  @Test
  void combinesDefaultsAndPackageOverrides() throws PackageLifecycleException {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            FileSystemPermissions.none());
    MqpPermissionsConfig configuration =
        new MqpPermissionsConfig(
            new PackagePermissionOverrides(HostAccessPermission.FULL, null, null),
            Map.of(
                "example-package",
                new PackagePermissionOverrides(null, HostClassLookupPermission.MINECRAFT, null)));

    assertEquals(
        requested, PackagePermissionResolver.resolve("example-package", requested, configuration));
  }

  @Test
  void rejectsPermissionsAboveGrant() {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.FULL,
            HostClassLookupPermission.MINECRAFT,
            new FileSystemPermissions(
                FileSystemReadPermission.PACKAGE, FileSystemWritePermission.DATA));

    PackageLifecycleException exception =
        assertThrows(
            PackageLifecycleException.class,
            () ->
                PackagePermissionResolver.resolve(
                    "example-package", requested, MqpPermissionsConfig.restricted()));

    assertTrue(exception.getMessage().contains("hostAccess=full"));
    assertTrue(exception.getMessage().contains("hostClassLookup=minecraft"));
    assertTrue(exception.getMessage().contains("filesystem.read=package"));
    assertTrue(exception.getMessage().contains("filesystem.write=data"));
  }

  @Test
  void writeGrantAlsoGrantsReadAccessToItsScope() throws PackageLifecycleException {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            new FileSystemPermissions(
                FileSystemReadPermission.MQP, FileSystemWritePermission.NONE));
    MqpPermissionsConfig configuration =
        new MqpPermissionsConfig(
            new PackagePermissionOverrides(
                null,
                null,
                new FileSystemPermissionOverrides(
                    FileSystemReadPermission.NONE, FileSystemWritePermission.MQP)),
            Map.of());

    assertEquals(
        requested, PackagePermissionResolver.resolve("example-package", requested, configuration));
  }

  @Test
  void acceptsRequestedInternetDomainsCoveredByGrant() throws PackageLifecycleException {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            FileSystemPermissions.none(),
            InternetPermissions.domains(List.of("api.example.com", "*.assets.example.com")));
    MqpPermissionsConfig configuration =
        new MqpPermissionsConfig(
            new PackagePermissionOverrides(
                null, null, null, InternetPermissions.domains(List.of("*.example.com"))),
            Map.of());

    assertEquals(
        requested, PackagePermissionResolver.resolve("example-package", requested, configuration));
  }

  @Test
  void rejectsInternetDomainsOutsideGrant() {
    PackagePermissions requested =
        new PackagePermissions(
            HostAccessPermission.NONE,
            HostClassLookupPermission.NONE,
            FileSystemPermissions.none(),
            InternetPermissions.domains(List.of("api.example.org")));
    MqpPermissionsConfig configuration =
        new MqpPermissionsConfig(
            new PackagePermissionOverrides(
                null, null, null, InternetPermissions.domains(List.of("*.example.com"))),
            Map.of());

    PackageLifecycleException exception =
        assertThrows(
            PackageLifecycleException.class,
            () -> PackagePermissionResolver.resolve("example-package", requested, configuration));

    assertTrue(exception.getMessage().contains("internet=domains"));
  }
}
