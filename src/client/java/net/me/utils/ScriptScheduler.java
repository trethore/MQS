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

package net.me.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptScheduler {

    private final ScriptManager scriptManager;
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private final Map<RunningScript, Set<ScheduledTask>> tasksByScript = new HashMap<>();
    private long tickCounter = 0L;

    public ScriptScheduler(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        ClientTickEvents.END_CLIENT_TICK.register(_ -> onTick());
    }

    public Runnable scheduleTickTimeout(RunningScript owner, Value callback, int delayTicks) {
        int ticks = Math.max(0, delayTicks);
        long executeAt;
        synchronized (tasks) {
            executeAt = tickCounter + ticks;
        }
        ScheduledTask task = ScheduledTask.tickTimeout(owner, callback, executeAt);
        return registerTask(task);
    }

    public Runnable scheduleMsTimeout(RunningScript owner, Value callback, long delayMillis) {
        long dueTime = System.currentTimeMillis() + Math.max(0L, delayMillis);
        ScheduledTask task = ScheduledTask.msTimeout(owner, callback, dueTime);
        return registerTask(task);
    }

    public Runnable scheduleTickInterval(RunningScript owner, Value callback, int intervalTicks) {
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("Interval must be greater than zero ticks.");
        }
        long nextTick;
        synchronized (tasks) {
            nextTick = tickCounter + intervalTicks;
        }
        ScheduledTask task = ScheduledTask.tickInterval(owner, callback, intervalTicks, nextTick);
        return registerTask(task);
    }

    public void cancelAllFor(RunningScript owner) {
        Set<ScheduledTask> owned;
        synchronized (tasks) {
            owned = tasksByScript.remove(owner);
            if (owned == null || owned.isEmpty()) {
                return;
            }
            for (ScheduledTask task : owned) {
                task.markCancelled();
                tasks.remove(task);
            }
        }
    }

    private Runnable registerTask(ScheduledTask task) {
        AtomicBoolean disposed = new AtomicBoolean(false);
        synchronized (tasks) {
            tasks.add(task);
            tasksByScript.computeIfAbsent(task.owner(), _ -> new HashSet<>()).add(task);
        }
        return () -> cancelTask(task, disposed);
    }

    private void cancelTask(ScheduledTask task, AtomicBoolean guard) {
        if (!guard.compareAndSet(false, true)) {
            return;
        }
        synchronized (tasks) {
            if (tasks.remove(task)) {
                Set<ScheduledTask> owned = tasksByScript.get(task.owner());
                if (owned != null) {
                    owned.remove(task);
                    if (owned.isEmpty()) {
                        tasksByScript.remove(task.owner());
                    }
                }
            }
        }
        task.markCancelled();
    }

    private void cancelTaskInternal(ScheduledTask task) {
        synchronized (tasks) {
            if (tasks.remove(task)) {
                Set<ScheduledTask> owned = tasksByScript.get(task.owner());
                if (owned != null) {
                    owned.remove(task);
                    if (owned.isEmpty()) {
                        tasksByScript.remove(task.owner());
                    }
                }
            }
        }
        task.markCancelled();
    }

    private void onTick() {
        List<ScheduledTask> snapshot;
        long now = System.currentTimeMillis();
        synchronized (tasks) {
            tickCounter++;
            if (tasks.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(tasks);
        }

        for (ScheduledTask task : snapshot) {
            if (task.isCancelled()) {
                continue;
            }
            try {
                if (!task.shouldRun(tickCounter, now)) {
                    continue;
                }
                executeTask(task);
                if (task.isInterval()) {
                    task.reschedule(tickCounter, now);
                } else {
                    cancelTaskInternal(task);
                }
            } catch (Exception e) {
                Main.LOGGER.error("Error running scheduled task for script '{}'", task.owner().getName(), e);
                cancelTaskInternal(task);
            }
        }
    }

    private void executeTask(ScheduledTask task) {
        Value callback = task.callback();
        if (callback == null || !callback.canExecute()) {
            return;
        }
        RunningScript owner = task.owner();
        RunningScript previous = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            callback.execute();
        } catch (IllegalStateException _) {
            // Script engine has been disposed.
        } catch (Exception e) {
            Main.LOGGER.error("Scheduled callback threw for script '{}'", owner.getName(), e);
        } finally {
            if (previous != null) {
                scriptManager.setCurrentScript(previous);
            } else {
                scriptManager.clearCurrentScript();
            }
        }
    }

    private static final class ScheduledTask {
        private final RunningScript owner;
        private final Value callback;
        private final Mode mode;
        private final int tickInterval;
        private final long msInterval;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private long nextTick;
        private long nextTime;

        private ScheduledTask(RunningScript owner, Value callback, Mode mode, int tickInterval, long msInterval, long nextTick, long nextTime) {
            this.owner = owner;
            this.callback = callback;
            this.mode = mode;
            this.tickInterval = tickInterval;
            this.msInterval = msInterval;
            this.nextTick = nextTick;
            this.nextTime = nextTime;
        }

        static ScheduledTask tickTimeout(RunningScript owner, Value callback, long executeAt) {
            return new ScheduledTask(owner, callback, Mode.TICK_TIMEOUT, 0, 0L, executeAt, 0L);
        }

        static ScheduledTask msTimeout(RunningScript owner, Value callback, long dueTime) {
            return new ScheduledTask(owner, callback, Mode.MS_TIMEOUT, 0, 0L, 0L, dueTime);
        }

        static ScheduledTask tickInterval(RunningScript owner, Value callback, int intervalTicks, long nextTick) {
            return new ScheduledTask(owner, callback, Mode.TICK_INTERVAL, intervalTicks, 0L, nextTick, 0L);
        }

        RunningScript owner() {
            return owner;
        }

        Value callback() {
            return callback;
        }

        boolean shouldRun(long currentTick, long currentTime) {
            return switch (mode) {
                case TICK_TIMEOUT, TICK_INTERVAL -> currentTick >= nextTick;
                case MS_TIMEOUT -> currentTime >= nextTime;
            };
        }

        boolean isInterval() {
            return mode == Mode.TICK_INTERVAL;
        }

        void reschedule(long currentTick, long currentTime) {
            if (mode == Mode.TICK_INTERVAL) {
                nextTick = currentTick + tickInterval;
            } else if (mode == Mode.MS_TIMEOUT) {
                nextTime = currentTime + msInterval;
            }
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        void markCancelled() {
            cancelled.set(true);
        }

        private enum Mode {
            TICK_TIMEOUT,
            MS_TIMEOUT,
            TICK_INTERVAL
        }
    }
}
