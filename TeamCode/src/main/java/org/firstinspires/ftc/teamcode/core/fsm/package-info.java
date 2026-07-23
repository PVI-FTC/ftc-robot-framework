/**
 * Reusable finite-state-machine interfaces and behavior.
 *
 * <p>For example, a small state machine can be started with
 * {@code FSM fsm = new FSM(idleState);}, configured with
 * {@code fsm.addTransition(new Transition(idleState, runningState, () -> startRequested));},
 * initialized with {@code fsm.initialize();}, and updated once each loop with
 * {@code fsm.update();}. The state implementations can contain any ordinary Java behavior; this
 * shared package does not depend on FTC hardware.</p>
 */
package org.firstinspires.ftc.teamcode.core.fsm;
