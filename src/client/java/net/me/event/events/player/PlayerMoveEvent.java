package net.me.event.events.player;

import net.me.event.CancellableEvent;
import net.me.event.Events;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

@SuppressWarnings("unused")
public class PlayerMoveEvent extends CancellableEvent {
    private final ClientPlayerEntity player;
    private final MovementType type;
    private final Vec3d movement;

    public PlayerMoveEvent(ClientPlayerEntity player, MovementType type, Vec3d movement) {
        this.player = player;
        this.type = type;
        this.movement = movement;
    }

    public ClientPlayerEntity getPlayer() {
        return player;
    }

    public MovementType getMouvementType() {
        return type;
    }

    public Vec3d getMovement() {
        return movement;
    }

    @Override
    public Events getType() {
        return Events.PlayerMoveEvent;
    }
}
