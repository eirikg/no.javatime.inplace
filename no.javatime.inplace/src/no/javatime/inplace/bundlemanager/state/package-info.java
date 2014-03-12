/**
 * 
 *
 */
/**
 * Finite State Machine of bundle life cycle states implemented using the State Design and Flyweight patterns.
 * <ol>
 * <li>Implicit transitions issued by the OSGI framework: 
 * <ol>
 * <li>There is no explicit transition to move a lazy activated bundle from state STARTING to state ACTIVE when
 * classes are loaded on demand in the lazy activated bundle
 * <li> There is no explicit transition to unresolve (or move the bundle to state INSTALLED) when the initial state
 * is RESOLVED. Unresolve is a transition that is part of the refresh, resolve, uninstall and update transitions.
 * </ol>
 * </ol>
 * <p> 
 * For BundleManager operations (transitions) moving bundles over multiple states, intermediate states are excluded in the state machine. 
 * <ol>
 * <li>This involves the following BundleManager transitions and intermediates states:
 * </ol>
 * <ol>
 * <li>Transition: Refresh. Initial state: Resolved. States UNRESOLVED->RESOLVED. Excluded: UNRESOLVED
 * <li>Transition: Resolve. Initial state: Installed. States: RESOLVED-><<LAZY>> Excluded: RESOLVED
 * <li>Transition: Start. Initial state: Resolved. States STARTING->ACTIVE. Excluded: STARTING
 * <li>Transition: Start. Initial state: <<LAZY>>. States STARTING->ACTIVE. Excluded: STARTING
 * <li>Transition: Stop. Initial state: Active. States STOPPING->RESOLVED. Excluded: STOPPING
 * </ol>
 */
package no.javatime.inplace.bundlemanager.state;
