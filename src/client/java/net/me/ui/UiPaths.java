/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.ui;

public final class UiPaths {
    public static final String ROOT_PAGE_FILE_NAME = "index.html";
    public static final String DEV_HTTP_ENTRYPOINT = "/" + ROOT_PAGE_FILE_NAME;
    public static final String DEV_HTTP_SPA_FALLBACK = DEV_HTTP_ENTRYPOINT;
    public static final String PROD_ASSET_ROOT = "web/";
    public static final String PROD_APP_ENTRYPOINT = PROD_ASSET_ROOT + ROOT_PAGE_FILE_NAME;

    private UiPaths() {
    }
}
