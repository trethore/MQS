/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.screen.screens.viewmodel;

import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AllScriptsViewModel {

    private final ScriptingService scriptingService;
    private final List<ScriptDescriptor> allScripts;
    public boolean isRefreshing = false;
    private List<ScriptDescriptor> filteredScripts;
    private String searchText = "";

    public AllScriptsViewModel(ScriptingService scriptingService) {
        this.scriptingService = scriptingService;
        this.allScripts = new ArrayList<>(scriptingService.listAvailable());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));
        this.filteredScripts = new ArrayList<>(this.allScripts);
    }

    public String getSearchText() {
        return searchText;
    }

    public List<ScriptDescriptor> getFilteredScripts() {
        return this.filteredScripts;
    }

    public boolean hasNoFilteredScripts() {
        return filteredScripts.isEmpty();
    }

    public void onSearchTextChanged(String newSearchText) {
        this.searchText = newSearchText.toLowerCase();
        this.filteredScripts = this.allScripts.stream()
                .filter(script -> script.moduleName().toLowerCase().contains(this.searchText))
                .collect(Collectors.toList());
    }

    public void refreshAndReenableScripts() {
        isRefreshing = true;
        scriptingService.refreshAndReenable();

        this.allScripts.clear();
        this.allScripts.addAll(scriptingService.listAvailable());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));

        onSearchTextChanged(this.searchText);
        isRefreshing = false;
    }

    public void disableAllScripts() {
        scriptingService.disableAll();
    }
}