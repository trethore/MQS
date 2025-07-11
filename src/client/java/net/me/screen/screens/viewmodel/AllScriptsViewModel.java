package net.me.screen.screens.viewmodel;

import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AllScriptsViewModel {

    private static final int ITEMS_PER_PAGE = 4;

    private final ScriptingService scriptingService;
    private final List<ScriptDescriptor> allScripts;
    public boolean isRefreshing = false;
    private List<ScriptDescriptor> filteredScripts;
    private String searchText = "";
    private int currentPage = 0;
    private int totalPages = 1;

    public AllScriptsViewModel(ScriptingService scriptingService) {
        this.scriptingService = scriptingService;
        this.allScripts = new ArrayList<>(scriptingService.listAvailable());
        this.allScripts.sort(Comparator.comparing(ScriptDescriptor::moduleName, String.CASE_INSENSITIVE_ORDER));
        this.filteredScripts = new ArrayList<>(this.allScripts);
        updatePagination();
    }

    public String getSearchText() {
        return searchText;
    }

    public List<ScriptDescriptor> getScriptsForCurrentPage() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredScripts.size());
        if (startIndex >= endIndex) {
            return new ArrayList<>();
        }
        return filteredScripts.subList(startIndex, endIndex);
    }

    public boolean hasNoFilteredScripts() {
        return filteredScripts.isEmpty();
    }

    public String getPageNumberText() {
        return (currentPage + 1) + " / " + totalPages;
    }

    public boolean isPreviousButtonActive() {
        return currentPage > 0;
    }

    public boolean isNextButtonActive() {
        return currentPage < totalPages - 1;
    }

    public void onSearchTextChanged(String newSearchText) {
        this.searchText = newSearchText.toLowerCase();
        this.filteredScripts = this.allScripts.stream()
                .filter(script -> script.moduleName().toLowerCase().contains(this.searchText))
                .collect(Collectors.toList());
        this.currentPage = 0;
        updatePagination();
    }

    public void nextPage() {
        if (isNextButtonActive()) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (isPreviousButtonActive()) {
            currentPage--;
        }
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

    public void forceRefresh() {
        refreshAndReenableScripts();
    }

    public void disableAllScripts() {
        scriptingService.disableAll();
    }

    private void updatePagination() {
        this.totalPages = (int) Math.ceil((double) this.filteredScripts.size() / ITEMS_PER_PAGE);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }
        this.currentPage = Math.max(0, Math.min(this.currentPage, this.totalPages - 1));
    }
}