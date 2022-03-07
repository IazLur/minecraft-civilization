package edouard.testjava.classes;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;

public class Pathfinder implements Listener {
    public final Location destination;
    public LivingEntity entity;
    public OneArg callback;

    public Pathfinder(Location destination) {
        this.destination = destination;
    }

    public void move(LivingEntity entity, OneArg cb) {
        this.callback = cb;
        this.entity = entity;
    }
}
