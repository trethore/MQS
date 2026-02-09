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

package net.me.event;

public enum EventPhase {
    /**
     * The first phase to run. This phase is primarily intended for cancelling
     * cancellable events. For non-cancellable events, this phase serves as an
     * early notification point, running before all POST listeners.
     */
    PRE,

    /**
     * The final phase to run. Listeners here can react to the final state of an event
     * after all PRE-phase listeners have run and cancellation checks have occurred.
     */
    POST
}