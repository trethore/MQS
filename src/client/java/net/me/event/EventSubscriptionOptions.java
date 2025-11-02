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

package net.me.event;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Locale;

public final class EventSubscriptionOptions {
    private static final String PHASE_PROP = "phase";

    private final EventPhase phase;

    private EventSubscriptionOptions(EventPhase phase) {
        this.phase = phase;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EventSubscriptionOptions of(EventPhase phase) {
        return new EventSubscriptionOptions(phase);
    }

    public static EventSubscriptionOptions fromScript(Value optionsValue, EventPhase defaultPhase) {
        if (optionsValue == null) {
            return of(defaultPhase);
        }
        if (optionsValue.isHostObject()) {
            Object hostObject = optionsValue.asHostObject();
            if (hostObject instanceof EventSubscriptionOptions options) {
                return options;
            }
            if (hostObject instanceof Builder builder) {
                return builder.buildWithDefault(defaultPhase);
            }
            if (hostObject instanceof EventPhase eventPhase) {
                return of(eventPhase);
            }
        }
        if (optionsValue.hasMembers() && optionsValue.hasMember(PHASE_PROP)) {
            EventPhase resolved = resolvePhaseValue(optionsValue.getMember(PHASE_PROP));
            if (resolved != null) {
                return of(resolved);
            }
        } else {
            EventPhase resolved = resolvePhaseValue(optionsValue);
            if (resolved != null) {
                return of(resolved);
            }
        }
        return of(defaultPhase);
    }

    public static EventPhase resolvePhaseValue(Value phaseValue) {
        if (phaseValue == null) {
            return null;
        }
        if (phaseValue.isHostObject()) {
            Object hostObject = phaseValue.asHostObject();
            if (hostObject instanceof EventPhase eventPhase) {
                return eventPhase;
            }
            if (hostObject instanceof EventSubscriptionOptions options) {
                return options.phase();
            }
            if (hostObject instanceof Builder builder) {
                return builder.peekPhase();
            }
        }
        if (phaseValue.isString()) {
            String phaseName = phaseValue.asString();
            if (phaseName != null && !phaseName.isEmpty()) {
                try {
                    return EventPhase.valueOf(phaseName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid phase '" + phaseName + "'. Use PRE or POST.", e);
                }
            }
        }
        return null;
    }

    public EventPhase phase() {
        return phase;
    }

    public static final class Builder {
        private EventPhase phase;

        @HostAccess.Export
        public Builder phase(EventPhase phase) {
            this.phase = phase;
            return this;
        }

        @HostAccess.Export
        public Builder phase(String phaseName) {
            if (phaseName != null && !phaseName.isEmpty()) {
                try {
                    this.phase = EventPhase.valueOf(phaseName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid phase '" + phaseName + "'. Use PRE or POST.", e);
                }
            }
            return this;
        }

        @HostAccess.Export
        public EventSubscriptionOptions build() {
            EventPhase effectivePhase = phase != null ? phase : EventPhase.POST;
            return new EventSubscriptionOptions(effectivePhase);
        }

        public EventSubscriptionOptions buildWithDefault(EventPhase defaultPhase) {
            EventPhase effectivePhase = phase != null ? phase : defaultPhase;
            return new EventSubscriptionOptions(effectivePhase);
        }

        EventPhase peekPhase() {
            return phase;
        }
    }
}
