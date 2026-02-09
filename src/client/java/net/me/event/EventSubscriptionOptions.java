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

        EventSubscriptionOptions fromHost = tryExtractFromHost(optionsValue, defaultPhase);
        if (fromHost != null) {
            return fromHost;
        }

        Value phaseSource = optionsValue.hasMembers() && optionsValue.hasMember(PHASE_PROP)
                ? optionsValue.getMember(PHASE_PROP)
                : optionsValue;

        EventPhase resolved = resolvePhaseValue(phaseSource);
        return of(resolved != null ? resolved : defaultPhase);
    }

    private static EventSubscriptionOptions tryExtractFromHost(Value value, EventPhase defaultPhase) {
        if (!value.isHostObject()) {
            return null;
        }
        return switch (value.asHostObject()) {
            case EventSubscriptionOptions options -> options;
            case Builder builder -> builder.buildWithDefault(defaultPhase);
            case EventPhase eventPhase -> of(eventPhase);
            default -> null;
        };
    }

    public static EventPhase resolvePhaseValue(Value phaseValue) {
        if (phaseValue == null) {
            return null;
        }

        if (phaseValue.isHostObject()) {
            return switch (phaseValue.asHostObject()) {
                case EventPhase eventPhase -> eventPhase;
                case EventSubscriptionOptions options -> options.phase();
                case Builder builder -> builder.peekPhase();
                default -> null;
            };
        }

        if (phaseValue.isString()) {
            return parsePhaseString(phaseValue.asString());
        }

        return null;
    }

    private static EventPhase parsePhaseString(String phaseName) {
        if (phaseName == null || phaseName.isEmpty()) {
            return null;
        }
        try {
            return EventPhase.valueOf(phaseName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid phase '" + phaseName + "'. Use PRE or POST.", e);
        }
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
