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
package io.github.trethore.myqolpackages.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class HiddenClientCommand {
  private HiddenClientCommand() {}

  public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
    return new HiddenLiteralArgumentBuilder<>(name);
  }

  private static final class HiddenLiteralArgumentBuilder<S> extends LiteralArgumentBuilder<S> {
    private HiddenLiteralArgumentBuilder(String literal) {
      super(literal);
    }

    @Override
    public LiteralCommandNode<S> build() {
      HiddenLiteralCommandNode<S> result =
          new HiddenLiteralCommandNode<>(
              getLiteral(),
              getCommand(),
              getRequirement(),
              getRedirect(),
              getRedirectModifier(),
              isFork());
      for (CommandNode<S> argument : getArguments()) {
        result.addChild(argument);
      }
      return result;
    }
  }

  private static final class HiddenLiteralCommandNode<S> extends LiteralCommandNode<S> {
    private HiddenLiteralCommandNode(
        String literal,
        Command<S> command,
        Predicate<S> requirement,
        CommandNode<S> redirect,
        RedirectModifier<S> modifier,
        boolean forks) {
      super(literal, command, requirement, redirect, modifier, forks);
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
    }

    @Override
    public LiteralArgumentBuilder<S> createBuilder() {
      HiddenLiteralArgumentBuilder<S> builder = new HiddenLiteralArgumentBuilder<>(getLiteral());
      builder.requires(getRequirement());
      builder.forward(getRedirect(), getRedirectModifier(), isFork());
      if (getCommand() != null) {
        builder.executes(getCommand());
      }
      return builder;
    }
  }
}
