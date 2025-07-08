package net.me.event;

@SuppressWarnings("unused")
public abstract class CancellableEvent extends Event {
    boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean shouldBeCancelled) {
        cancelled = shouldBeCancelled;
    }
}
