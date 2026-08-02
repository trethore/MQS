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
package io.github.trethore.myqolpackages.command.commands.permissions;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class PermissionCommandSupport {
  private static final String NO_PERMISSION_NAME = "NONE";

  private PermissionCommandSupport() {}

  static Component formatPermission(Enum<?> value) {
    return Component.literal(formatValue(value)).withStyle(permissionColor(value));
  }

  static String formatValues(Enum<?>[] values) {
    StringJoiner formattedValues = new StringJoiner(", ");
    for (int index = values.length - 1; index >= 0; index--) {
      formattedValues.add(formatValue(values[index]));
    }
    return formattedValues.toString();
  }

  static CompletableFuture<Suggestions> suggestValues(
      SuggestionsBuilder builder, Enum<?>[] values) {
    String remaining = builder.getRemainingLowerCase();
    StringRange range = StringRange.between(builder.getStart(), builder.getInput().length());
    List<Suggestion> suggestions = new ArrayList<>();
    for (int index = values.length - 1; index >= 0; index--) {
      String name = formatValue(values[index]);
      if (name.startsWith(remaining) && !name.equals(remaining)) {
        suggestions.add(new Suggestion(range, name));
      }
    }
    return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
  }

  private static String formatValue(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }

  private static ChatFormatting permissionColor(Enum<?> value) {
    return value.name().equals(NO_PERMISSION_NAME) ? ChatFormatting.RED : ChatFormatting.GREEN;
  }
}
