package org.firstinspires.ftc.teamcode.core.robot;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class that owns and runs a robot's subsystems.
 *
 * <p>Subclasses register their subsystems during construction by calling
 * {@link #registerSubsystem(Subsystem)}. This class then calls each subsystem in registration
 * order: {@link Subsystem#initialize()} once, {@link Subsystem#update()} once per FTC loop, and
 * {@link Subsystem#stop()} when the robot stops.</p>
 *
 * <p>The same subsystem instance cannot be registered twice. Registration is also closed after
 * initialization so every registered subsystem has a complete lifecycle.</p>
 */
public abstract class Robot {
    private final List<Subsystem> subsystems = new ArrayList<>();
    private boolean initialized;

    /**
     * Registers a subsystem for this robot's lifecycle.
     *
     * <p>Subclasses should call this during construction. Subsystems are updated in the same order
     * that they are registered.</p>
     *
     * @param subsystem the subsystem to register
     * @throws IllegalArgumentException if {@code subsystem} is null or is already registered
     * @throws IllegalStateException if initialization has already started
     */
    protected final void registerSubsystem(Subsystem subsystem) {
        if (subsystem == null) {
            throw new IllegalArgumentException("A robot cannot register a null subsystem.");
        }

        if (initialized) {
            throw new IllegalStateException("Subsystems must be registered before robot initialization.");
        }

        for (Subsystem registeredSubsystem : subsystems) {
            if (registeredSubsystem == subsystem) {
                throw new IllegalArgumentException(
                        "The subsystem is already registered: " + subsystem.getName());
            }
        }

        subsystems.add(subsystem);
    }

    /**
     * Initializes every registered subsystem once, in registration order.
     *
     * <p>Later calls do nothing so a subsystem is never initialized twice by this robot.</p>
     */
    public final void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        for (Subsystem subsystem : subsystems) {
            subsystem.initialize();
        }
    }

    /**
     * Updates every registered subsystem once in registration order.
     *
     * <p>Call this once from each FTC loop after the robot has been initialized.</p>
     */
    public final void update() {
        for (Subsystem subsystem : subsystems) {
            subsystem.update();
        }
    }

    /**
     * Stops every registered subsystem in registration order.
     *
     * <p>After the subsystems stop, {@link #onStop()} gives subclasses a narrow place for any
     * remaining robot-level safety work.</p>
     */
    public final void stop() {
        for (Subsystem subsystem : subsystems) {
            subsystem.stop();
        }

        onStop();
    }

    /**
     * Performs robot-level safety work after all registered subsystems have stopped.
     *
     * <p>Subclasses may override this only when a safety action does not belong to one of their
     * registered subsystems.</p>
     */
    protected void onStop() {
        // Subclasses may override when they have robot-level safety work.
    }
}
