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