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
package io.github.trethore.myqolpackages.internal.trust;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public final class PackageFingerprintService {
  private static final String ALGORITHM = "SHA-256";
  private static final String DIGEST_PREFIX = "sha256:";

  public String fingerprint(Path packageDirectory) throws PackageFingerprintException {
    Path normalizedDirectory = packageDirectory.toAbsolutePath().normalize();
    if (Files.isSymbolicLink(normalizedDirectory)) {
      throw new PackageFingerprintException("Package directory must not be a symbolic link");
    }
    try {
      MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
      try (DigestOutputStream digestOutput =
              new DigestOutputStream(java.io.OutputStream.nullOutputStream(), messageDigest);
          DataOutputStream framedOutput = new DataOutputStream(digestOutput)) {
        for (Path path : listPackageFiles(normalizedDirectory)) {
          hashFile(normalizedDirectory, path, framedOutput);
        }
      }
      return DIGEST_PREFIX + HexFormat.of().formatHex(messageDigest.digest());
    } catch (IOException exception) {
      throw new PackageFingerprintException(
          "Could not fingerprint package: " + exception.getMessage(), exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public static boolean isValidDigest(String digest) {
    if (digest == null || !digest.startsWith(DIGEST_PREFIX) || digest.length() != 71) {
      return false;
    }
    for (int index = DIGEST_PREFIX.length(); index < digest.length(); index++) {
      char character = digest.charAt(index);
      if (!((character >= '0' && character <= '9') || (character >= 'a' && character <= 'f'))) {
        return false;
      }
    }
    return true;
  }

  private static List<Path> listPackageFiles(Path packageDirectory)
      throws IOException, PackageFingerprintException {
    try (Stream<Path> paths = Files.walk(packageDirectory)) {
      List<Path> packagePaths =
          paths
              .filter(path -> !path.equals(packageDirectory))
              .sorted(Comparator.comparing(path -> normalizeRelativePath(packageDirectory, path)))
              .toList();
      for (Path path : packagePaths) {
        if (Files.isSymbolicLink(path)) {
          throw new PackageFingerprintException(
              "Package contains a symbolic link: " + normalizeRelativePath(packageDirectory, path));
        }
        BasicFileAttributes attributes =
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() && !attributes.isRegularFile()) {
          throw new PackageFingerprintException(
              "Package contains an unsupported file: "
                  + normalizeRelativePath(packageDirectory, path));
        }
      }
      return packagePaths.stream().filter(Files::isRegularFile).toList();
    }
  }

  private static void hashFile(Path packageDirectory, Path file, DataOutputStream output)
      throws IOException, PackageFingerprintException {
    BasicFileAttributes before =
        Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    String relativePath = normalizeRelativePath(packageDirectory, file);
    byte[] pathBytes = relativePath.getBytes(StandardCharsets.UTF_8);
    output.writeByte(1);
    output.writeInt(pathBytes.length);
    output.write(pathBytes);
    output.writeLong(before.size());
    try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
      input.transferTo(output);
    }
    BasicFileAttributes after =
        Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (before.size() != after.size()
        || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
      throw new PackageFingerprintException(
          "Package changed while being fingerprinted: " + relativePath);
    }
  }

  private static String normalizeRelativePath(Path packageDirectory, Path path) {
    return packageDirectory
        .relativize(path)
        .toString()
        .replace(path.getFileSystem().getSeparator(), "/");
  }
}
