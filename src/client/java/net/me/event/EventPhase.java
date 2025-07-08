package net.me.event;

public enum EventPhase {
    /**
     * The first phase to run. Primarily used for cancellation checks. Listeners in this
     * phase should not modify the event's data. If the event is cancelled here,
     * subsequent phases will not run.
     */
    PRE,

    /**
     * The second phase to run. This is the primary phase for modifying the event's
     * data, such as changing a packet.
     */
    MODIFY,

    /**
     * The final phase to run. Listeners here can react to the final state of the event
     * after all cancellations and modifications have occurred.
     */
    POST
}