package uk.ac.manchester.cs.factplusplus.owlapiv3;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.datatype.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.Version;
import org.semanticweb.owlapi.vocab.*;

import uk.ac.manchester.cs.factplusplus.*;

/*
 * Copyright (C) 2009-2010, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Information Management Group<br>
 * Date: 29-Dec-2009
 * 
 * Synchronization policy: all methods for OWLReasoner are synchronized, except
 * the methods which do not touch the kernel or only affect threadsafe data
 * structures. inner private classes are not synchronized since methods from
 * those classes cannot be invoked from outsize synchronized methods.
 * 
 */
public class FaCTPlusPlusReasoner implements OWLReasoner, OWLOntologyChangeListener {
	public static final String REASONER_NAME = "FaCT++";
	public static final Version VERSION = new Version(1, 6, 2, 0);
	protected final AtomicBoolean interrupted = new AtomicBoolean(false);
	private final FaCTPlusPlus kernel = new FaCTPlusPlus();
	private volatile AxiomTranslator axiomTranslator = new AxiomTranslator();
	private volatile ClassExpressionTranslator classExpressionTranslator;
	private volatile DataRangeTranslator dataRangeTranslator;
	private volatile ObjectPropertyTranslator objectPropertyTranslator;
	private volatile DataPropertyTranslator dataPropertyTranslator;
	private volatile IndividualTranslator individualTranslator;
	private volatile EntailmentChecker entailmentChecker;
	private final Map<OWLAxiom, AxiomPointer> axiom2PtrMap = new HashMap<OWLAxiom, AxiomPointer>();
	private final Map<AxiomPointer, OWLAxiom> ptr2AxiomMap = new HashMap<AxiomPointer, OWLAxiom>();
	private static final Set<InferenceType> SupportedInferenceTypes = new HashSet<InferenceType>(Arrays.asList(
			InferenceType.CLASS_ASSERTIONS, InferenceType.CLASS_HIERARCHY, InferenceType.DATA_PROPERTY_HIERARCHY,
			InferenceType.OBJECT_PROPERTY_HIERARCHY, InferenceType.SAME_INDIVIDUAL));
	private final OWLOntologyManager manager;
	private final OWLOntology rootOntology;
	private final BufferingMode bufferingMode;
	private final List<OWLOntologyChange> rawChanges = new ArrayList<OWLOntologyChange>();
	// private final ReentrantReadWriteLock rawChangesLock = new
	// ReentrantReadWriteLock();
	private final List<OWLAxiom> reasonerAxioms = new ArrayList<OWLAxiom>();
	private final long timeOut;
	private final OWLReasonerConfiguration configuration;

	public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
		handleRawOntologyChanges(changes);
	}

	public OWLReasonerConfiguration getReasonerConfiguration() {
		return configuration;
	}

	public BufferingMode getBufferingMode() {
		return bufferingMode;
	}

	public long getTimeOut() {
		return timeOut;
	}

	public OWLOntology getRootOntology() {
		return rootOntology;
	}

	/**
	 * Handles raw ontology changes. If the reasoner is a buffering reasoner
	 * then the changes will be stored in a buffer. If the reasoner is a
	 * non-buffering reasoner then the changes will be automatically flushed
	 * through to the change filter and passed on to the reasoner.
	 * 
	 * @param changes
	 *            The list of raw changes.
	 */
	private final boolean log = false;

	private synchronized void handleRawOntologyChanges(List<? extends OWLOntologyChange> changes) {
		if (log) {
			System.out.println(Thread.currentThread().getName() + " OWLReasonerBase.handleRawOntologyChanges() "
					+ changes);
		}
		rawChanges.addAll(changes);
		// We auto-flush the changes if the reasoner is non-buffering
		if (bufferingMode.equals(BufferingMode.NON_BUFFERING)) {
			flush();
		}
	}

	public synchronized List<OWLOntologyChange> getPendingChanges() {
		return new ArrayList<OWLOntologyChange>(rawChanges);
	}

	public synchronized Set<OWLAxiom> getPendingAxiomAdditions() {
		if (rawChanges.size() > 0) {
			Set<OWLAxiom> added = new HashSet<OWLAxiom>();
			computeDiff(added, new HashSet<OWLAxiom>());
			return added;
		}
		return Collections.emptySet();
	}

	public synchronized Set<OWLAxiom> getPendingAxiomRemovals() {
		if (rawChanges.size() > 0) {
			Set<OWLAxiom> removed = new HashSet<OWLAxiom>();
			computeDiff(new HashSet<OWLAxiom>(), removed);
			return removed;
		}
		return Collections.emptySet();
	}

	/**
	 * Flushes the pending changes from the pending change list. The changes
	 * will be analysed to dermine which axioms have actually been added and
	 * removed from the imports closure of the root ontology and then the
	 * reasoner will be asked to handle these changes via the
	 * {@link #handleChanges(java.util.Set, java.util.Set)} method.
	 */
	public synchronized void flush() {
		// Process the changes
		if (rawChanges.size() > 0) {
			final Set<OWLAxiom> added = new HashSet<OWLAxiom>();
			final Set<OWLAxiom> removed = new HashSet<OWLAxiom>();
			computeDiff(added, removed);
			reasonerAxioms.removeAll(removed);
			reasonerAxioms.addAll(added);
			rawChanges.clear();
			if (!added.isEmpty() || !removed.isEmpty()) {
				handleChanges(added, removed);
			}
		}
	}

	/**
	 * Computes a diff of what axioms have been added and what axioms have been
	 * removed from the list of pending changes. Note that even if the list of
	 * pending changes is non-empty then there may be no changes for the
	 * reasoner to deal with.
	 * 
	 * @param added
	 *            The logical axioms that have been added to the imports closure
	 *            of the reasoner root ontology
	 * @param removed
	 *            The logical axioms that have been removed from the imports
	 *            closure of the reasoner root ontology
	 */
	private synchronized void computeDiff(Set<OWLAxiom> added, Set<OWLAxiom> removed) {
		for (OWLOntology ont : rootOntology.getImportsClosure()) {
			for (OWLAxiom ax : ont.getLogicalAxioms()) {
				if (!reasonerAxioms.contains(ax.getAxiomWithoutAnnotations())) {
					added.add(ax);
				}
			}
			for (OWLAxiom ax : ont.getAxioms(AxiomType.DECLARATION)) {
				if (!reasonerAxioms.contains(ax.getAxiomWithoutAnnotations())) {
					added.add(ax);
				}
			}
		}
		for (OWLAxiom ax : reasonerAxioms) {
			if (!rootOntology.containsAxiomIgnoreAnnotations(ax, true)) {
				removed.add(ax);
			}
		}
	}

	/**
	 * Gets the axioms that should be currently being reasoned over.
	 * 
	 * @return A collections of axioms (not containing duplicates) that the
	 *         reasoner should be taking into consideration when reasoning. This
	 *         set of axioms many not correspond to the current state of the
	 *         imports closure of the reasoner root ontology if the reasoner is
	 *         buffered.
	 */
	public synchronized Collection<OWLAxiom> getReasonerAxioms() {
		return new ArrayList<OWLAxiom>(reasonerAxioms);
	}

	public FreshEntityPolicy getFreshEntityPolicy() {
		return configuration.getFreshEntityPolicy();
	}

	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
		return configuration.getIndividualNodeSetPolicy();
	}

	public OWLDataFactory getOWLDataFactory() {
		return rootOntology.getOWLOntologyManager().getOWLDataFactory();
	}

	public FaCTPlusPlusReasoner(OWLOntology rootOntology, OWLReasonerConfiguration configuration,
			BufferingMode bufferingMode) {
		this.rootOntology = rootOntology;
		this.bufferingMode = bufferingMode;
		this.configuration = configuration;
		this.timeOut = configuration.getTimeOut();
		manager = rootOntology.getOWLOntologyManager();
		for (OWLOntology ont : rootOntology.getImportsClosure()) {
			for (OWLAxiom ax : ont.getLogicalAxioms()) {
				reasonerAxioms.add(ax.getAxiomWithoutAnnotations());
			}
			for (OWLAxiom ax : ont.getAxioms(AxiomType.DECLARATION)) {
				reasonerAxioms.add(ax.getAxiomWithoutAnnotations());
			}
		}
		axiomTranslator = new AxiomTranslator();
		classExpressionTranslator = new ClassExpressionTranslator();
		dataRangeTranslator = new DataRangeTranslator();
		objectPropertyTranslator = new ObjectPropertyTranslator();
		dataPropertyTranslator = new DataPropertyTranslator();
		individualTranslator = new IndividualTranslator();
		entailmentChecker = new EntailmentChecker();
		kernel.setTopBottomPropertyNames("http://www.w3.org/2002/07/owl#topObjectProperty",
				"http://www.w3.org/2002/07/owl#bottomObjectProperty", "http://www.w3.org/2002/07/owl#topDataProperty",
				"http://www.w3.org/2002/07/owl#bottomDataProperty");
		kernel.setProgressMonitor(new ProgressMonitorAdapter(configuration.getProgressMonitor(), interrupted));
		long millis = configuration.getTimeOut();
		if (millis == Long.MAX_VALUE)
			millis = 0;
		kernel.setOperationTimeout(millis);
		kernel.setFreshEntityPolicy(configuration.getFreshEntityPolicy() == FreshEntityPolicy.ALLOW);
		loadReasonerAxioms();
	}

	// /////////////////////////////////////////////////////////////////////////
	//
	// load/retract axioms
	//
	// /////////////////////////////////////////////////////////////////////////
	private void loadAxiom(OWLAxiom axiom) {
		if (axiom2PtrMap.containsKey(axiom)) {
			return;
		}
		final AxiomPointer axiomPointer = axiom.accept(axiomTranslator);
		if (axiomPointer != null) {
			axiom2PtrMap.put(axiom, axiomPointer);
			ptr2AxiomMap.put(axiomPointer, axiom);
		}
	}

	private void retractAxiom(OWLAxiom axiom) {
		final AxiomPointer ptr = axiom2PtrMap.get(axiom);
		if (ptr != null) {
			kernel.retract(ptr);
			axiom2PtrMap.remove(axiom);
			ptr2AxiomMap.remove(ptr);
		}
	}

	/**
	 * Asks the reasoner implementation to handle axiom additions and removals
	 * from the imports closure of the root ontology. The changes will not
	 * include annotation axiom additions and removals.
	 * 
	 * @param addAxioms
	 *            The axioms to be added to the reasoner.
	 * @param removeAxioms
	 *            The axioms to be removed from the reasoner
	 */
	protected void handleChanges(Set<OWLAxiom> addAxioms, Set<OWLAxiom> removeAxioms) {
		kernel.startChanges();
		for (OWLAxiom ax_a : addAxioms)
			loadAxiom(ax_a);
		for (OWLAxiom ax_r : removeAxioms)
			retractAxiom(ax_r);
		kernel.endChanges();
	}

	private void loadReasonerAxioms() {
		getReasonerConfiguration().getProgressMonitor().reasonerTaskStarted(ReasonerProgressMonitor.LOADING);
		getReasonerConfiguration().getProgressMonitor().reasonerTaskBusy();
		kernel.clearKernel();
		axiomTranslator = new AxiomTranslator();
		classExpressionTranslator = new ClassExpressionTranslator();
		dataRangeTranslator = new DataRangeTranslator();
		objectPropertyTranslator = new ObjectPropertyTranslator();
		dataPropertyTranslator = new DataPropertyTranslator();
		individualTranslator = new IndividualTranslator();
		axiom2PtrMap.clear();
		ptr2AxiomMap.clear();
		for (OWLAxiom ax : getReasonerAxioms()) {
			loadAxiom(ax);
		}
		getReasonerConfiguration().getProgressMonitor().reasonerTaskStopped();
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ////
	// ////
	// //// Implementation of reasoner interfaces
	// ////
	// ////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getReasonerName() {
		return REASONER_NAME;
	}

	public Version getReasonerVersion() {
		return VERSION;
	}

	public void interrupt() {
		interrupted.set(true);
	}

	// precompute inferences
	public synchronized void precomputeInferences(InferenceType... inferenceTypes) throws ReasonerInterruptedException,
			TimeOutException, InconsistentOntologyException {
		for (InferenceType it : inferenceTypes)
			if (SupportedInferenceTypes.contains(it)) {
				kernel.realise();
				return;
			}
	}

	public boolean isPrecomputed(InferenceType inferenceType) {
		if (SupportedInferenceTypes.contains(inferenceType)) {
			return kernel.isRealised();
		}
		return true;
	}

	public Set<InferenceType> getPrecomputableInferenceTypes() {
		return SupportedInferenceTypes;
	}

	// consistency
	public synchronized boolean isConsistent() throws ReasonerInterruptedException, TimeOutException {
		return kernel.isKBConsistent();
	}

	private void checkConsistency() {
		if (interrupted.get()) {
			throw new ReasonerInterruptedException();
		}
		if (!isConsistent()) {
			throw new InconsistentOntologyException();
		}
	}

	public synchronized boolean isSatisfiable(OWLClassExpression classExpression) throws ReasonerInterruptedException,
			TimeOutException, ClassExpressionNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		checkConsistency();
		return kernel.isClassSatisfiable(toClassPointer(classExpression));
	}

	public Node<OWLClass> getUnsatisfiableClasses() throws ReasonerInterruptedException, TimeOutException,
			InconsistentOntologyException {
		return getBottomClassNode();
	}

	// entailments
	public synchronized boolean isEntailed(OWLAxiom axiom) throws ReasonerInterruptedException,
			UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		checkConsistency();
		// if(rootOntology.containsAxiom(axiom, true)) {
		// return true;
		// }
		Boolean entailed = axiom.accept(entailmentChecker);
		if (entailed == null) {
			throw new UnsupportedEntailmentTypeException(axiom);
		}
		return entailed;
	}

	public synchronized boolean isEntailed(Set<? extends OWLAxiom> axioms) throws ReasonerInterruptedException,
			UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		for (OWLAxiom ax : axioms) {
			if (!isEntailed(ax)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
		if (axiomType.equals(AxiomType.SWRL_RULE)) {
			return false;
		}
		// FIXME!! check later
		return true;
	}

	// tracing
	/*
	 * Return tracing set (set of axioms that were participate in achieving
	 * result) for a given entailment. Return empty set if the axiom is not
	 * entailed.
	 */
	public Set<OWLAxiom> getTrace(OWLAxiom axiom) {
		kernel.needTracing();
		Set<OWLAxiom> ret = new HashSet<OWLAxiom>();
		if (isEntailed(axiom)) {
			for (AxiomPointer ap : kernel.getTrace())
				ret.add(ptr2AxiomMap.get(ap));
		}
		return ret;
	}

	// classes
	public Node<OWLClass> getTopClassNode() {
		return getEquivalentClasses(getOWLDataFactory().getOWLThing());
	}

	public Node<OWLClass> getBottomClassNode() {
		return getEquivalentClasses(getOWLDataFactory().getOWLNothing());
	}

	public synchronized NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
			throws ReasonerInterruptedException, TimeOutException, FreshEntitiesException,
			InconsistentOntologyException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askSubClasses(toClassPointer(ce), direct));
	}

	public synchronized NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce, boolean direct)
			throws InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException,
			TimeOutException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askSuperClasses(toClassPointer(ce), direct));
	}

	public synchronized Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
			throws InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException,
			TimeOutException {
		checkConsistency();
		ClassPointer[] pointers = kernel.askEquivalentClasses(toClassPointer(ce));
		return classExpressionTranslator.getNodeFromPointers(pointers);
	}

	public synchronized NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askDisjointClasses(toClassPointer(ce)));
	}

	// object properties
	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
		return getEquivalentObjectProperties(getOWLDataFactory().getOWLTopObjectProperty());
	}

	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
		return getEquivalentObjectProperties(getOWLDataFactory().getOWLBottomObjectProperty());
	}

	public synchronized NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe,
			boolean direct) throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return objectPropertyTranslator.getNodeSetFromPointers(kernel.askSubObjectProperties(
				toObjectPropertyPointer(pe), direct));
	}

	public synchronized NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe,
			boolean direct) throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return objectPropertyTranslator.getNodeSetFromPointers(kernel.askSuperObjectProperties(
				toObjectPropertyPointer(pe), direct));
	}

	public synchronized Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return objectPropertyTranslator.getNodeFromPointers(kernel
				.askEquivalentObjectProperties(toObjectPropertyPointer(pe)));
	}

	public synchronized NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		// TODO: incomplete
		OWLObjectPropertyNodeSet toReturn = new OWLObjectPropertyNodeSet();
		toReturn.addNode(getBottomObjectPropertyNode());
		return toReturn;
	}

	public synchronized Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return objectPropertyTranslator.getNodeFromPointers(kernel
				.askEquivalentObjectProperties(toObjectPropertyPointer(pe.getInverseProperty())));
	}

	public synchronized NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askObjectPropertyDomain(
				objectPropertyTranslator.createPointerForEntity(pe), direct));
	}

	public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askObjectPropertyRange(
				objectPropertyTranslator.getPointerFromEntity(pe), direct));
	}

	// data properties
	public Node<OWLDataProperty> getTopDataPropertyNode() {
		OWLDataPropertyNode toReturn = new OWLDataPropertyNode();
		toReturn.add(getOWLDataFactory().getOWLTopDataProperty());
		return toReturn;
	}

	public Node<OWLDataProperty> getBottomDataPropertyNode() {
		return getEquivalentDataProperties(getOWLDataFactory().getOWLBottomDataProperty());
	}

	public synchronized NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return dataPropertyTranslator.getNodeSetFromPointers(kernel.askSubDataProperties(toDataPropertyPointer(pe),
				direct));
	}

	public synchronized NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return dataPropertyTranslator.getNodeSetFromPointers(kernel.askSuperDataProperties(toDataPropertyPointer(pe),
				direct));
	}

	public synchronized Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return dataPropertyTranslator
				.getNodeFromPointers(kernel.askEquivalentDataProperties(toDataPropertyPointer(pe)));
	}

	public synchronized NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		// TODO:
		return new OWLDataPropertyNodeSet();
	}

	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askDataPropertyDomain(
				dataPropertyTranslator.createPointerForEntity(pe), direct));
	}

	// individuals
	public synchronized NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return classExpressionTranslator.getNodeSetFromPointers(kernel.askIndividualTypes(toIndividualPointer(ind),
				direct));
	}

	public synchronized NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce, boolean direct)
			throws InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException,
			TimeOutException {
		checkConsistency();
		return translateIndividualPointersToNodeSet(kernel.askInstances(toClassPointer(ce), direct));
	}

	public synchronized NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind,
			OWLObjectPropertyExpression pe) throws InconsistentOntologyException, ReasonerInterruptedException,
			TimeOutException {
		checkConsistency();
		return translateIndividualPointersToNodeSet(kernel.askRelatedIndividuals(toIndividualPointer(ind),
				toObjectPropertyPointer(pe)));
	}

	public synchronized Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		// TODO:
		checkConsistency();
		return Collections.emptySet();
	}

	public synchronized Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		checkConsistency();
		return individualTranslator.getNodeFromPointers(kernel.askSameAs(toIndividualPointer(ind)));
	}

	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind)
			throws InconsistentOntologyException, ReasonerInterruptedException, TimeOutException {
		OWLClassExpression ce = getOWLDataFactory().getOWLObjectOneOf(ind).getObjectComplementOf();
		return getInstances(ce, false);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// //
	// // Translation to FaCT++ structures and back
	// //
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private abstract class OWLEntityTranslator<E extends OWLObject, P extends Pointer> {
		private final Map<E, P> entity2PointerMap = new ConcurrentHashMap<E, P>();
		protected final Map<P, E> pointer2EntityMap = new ConcurrentHashMap<P, E>();

		protected void fillEntityPointerMaps(E entity, P pointer) {
			entity2PointerMap.put(entity, pointer);
			pointer2EntityMap.put(pointer, entity);
		}

		protected OWLEntityTranslator() {
			E topEntity = getTopEntity();
			if (topEntity != null) {
				fillEntityPointerMaps(topEntity, getTopEntityPointer());
			}
			E bottomEntity = getBottomEntity();
			if (bottomEntity != null) {
				fillEntityPointerMaps(bottomEntity, getBottomEntityPointer());
			}
		}

		protected P registerNewEntity(E entity) {
			P pointer = createPointerForEntity(entity);
			fillEntityPointerMaps(entity, pointer);
			return pointer;
		}

		public E getEntityFromPointer(P pointer) {
			return pointer2EntityMap.get(pointer);
		}

		public P getPointerFromEntity(E entity) {
			if (entity.isTopEntity()) {
				return getTopEntityPointer();
			} else if (entity.isBottomEntity()) {
				return getBottomEntityPointer();
			} else {
				P pointer = entity2PointerMap.get(entity);
				if (pointer == null) {
					pointer = registerNewEntity(entity);
				}
				return pointer;
			}
		}

		public Node<E> getNodeFromPointers(P[] pointers) {
			DefaultNode<E> node = createDefaultNode();
			for (P pointer : pointers) {
				final E entityFromPointer = getEntityFromPointer(pointer);
				if (entityFromPointer != null) {
					node.add(entityFromPointer);
				}
			}
			return node;
		}

		public NodeSet<E> getNodeSetFromPointers(P[][] pointers) {
			DefaultNodeSet<E> nodeSet = createDefaultNodeSet();
			for (P[] pointerArray : pointers) {
				nodeSet.addNode(getNodeFromPointers(pointerArray));
			}
			return nodeSet;
		}

		protected abstract DefaultNode<E> createDefaultNode();

		protected abstract DefaultNodeSet<E> createDefaultNodeSet();

		protected abstract P getTopEntityPointer();

		protected abstract P getBottomEntityPointer();

		protected abstract P createPointerForEntity(E entity);

		protected abstract E getTopEntity();

		protected abstract E getBottomEntity();
	}

	protected ClassPointer toClassPointer(OWLClassExpression classExpression) {
		return classExpression.accept(classExpressionTranslator);
	}

	protected DataTypeExpressionPointer toDataTypeExpressionPointer(OWLDataRange dataRange) {
		return dataRange.accept(dataRangeTranslator);
	}

	protected ObjectPropertyPointer toObjectPropertyPointer(OWLObjectPropertyExpression propertyExpression) {
		OWLObjectPropertyExpression simp = propertyExpression.getSimplified();
		if (simp.isAnonymous()) {
			OWLObjectInverseOf inv = (OWLObjectInverseOf) simp;
			return kernel.getInverseProperty(objectPropertyTranslator.getPointerFromEntity(inv.getInverse()
					.asOWLObjectProperty()));
		} else {
			return objectPropertyTranslator.getPointerFromEntity(simp.asOWLObjectProperty());
		}
	}

	protected DataPropertyPointer toDataPropertyPointer(OWLDataPropertyExpression propertyExpression) {
		return dataPropertyTranslator.getPointerFromEntity(propertyExpression.asOWLDataProperty());
	}

	protected synchronized IndividualPointer toIndividualPointer(OWLIndividual individual) {
		if (!individual.isAnonymous()) {
			return individualTranslator.getPointerFromEntity(individual.asOWLNamedIndividual());
		} else {
			return kernel.getIndividual(individual.toStringID());
		}
	}

	protected synchronized DataTypePointer toDataTypePointer(OWLDatatype datatype) {
		if (datatype == null) {
			throw new IllegalArgumentException("datatype cannot be null");
		}
		String name = checkDateTime(datatype);

		return kernel.getBuiltInDataType(name);
	}

	protected static final String checkDateTime(OWLDatatype datatype) {
		String name = datatype.toStringID();
		if (datatype.isBuiltIn()) {
			OWL2Datatype builtInDatatype = datatype.getBuiltInDatatype();
			OWL2Datatype xsdDateTime = OWL2Datatype.XSD_DATE_TIME;
			if (builtInDatatype == xsdDateTime) {
				name = name + "AsLong";
			}
		}
		return name;
	}

	protected synchronized DataValuePointer toDataValuePointer(OWLLiteral literal) {
		String value = literal.getLiteral();
		if (literal.isRDFPlainLiteral()) {
			value = value + "@" + literal.getLang();
		}
		if (literal.getDatatype().isBuiltIn()
				&& literal.getDatatype().getBuiltInDatatype() == OWL2Datatype.XSD_DATE_TIME) {
			return kernel.getDataValue(convertToLongDateTime(value), toDataTypePointer(literal.getDatatype()));
		}
		return kernel.getDataValue(value, toDataTypePointer(literal.getDatatype()));
	}

	private static final String convertToLongDateTime(String input) {
		XMLGregorianCalendar calendar;
		try {
			calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(input);
			if (calendar.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
				// set it to 0 (UTC) in this case; not perfect but avoids
				// indeterminate situations where two datetime literals cannot
				// be compared
				calendar.setTimezone(0);
			}
			long l = calendar.toGregorianCalendar().getTimeInMillis();
			System.out.println("FaCTPlusPlusReasoner.convertToLongDateTime()\n" + input + "\n" + Long.toString(l));
			return Long.toString(l);
		} catch (DatatypeConfigurationException e) {
			throw new OWLRuntimeException("Error: the datatype support in the Java VM is broken! Cannot parse: "
					+ input, e);
		}
	}

	private NodeSet<OWLNamedIndividual> translateIndividualPointersToNodeSet(IndividualPointer[] pointers) {
		OWLNamedIndividualNodeSet ns = new OWLNamedIndividualNodeSet();
		for (IndividualPointer pointer : pointers) {
			if (pointer != null) {
				OWLNamedIndividual ind = individualTranslator.getEntityFromPointer(pointer);
				// XXX skipping anonymous individuals - counterintuitive but
				// that's the specs for you
				if (ind != null) {
					ns.addEntity(ind);
				}
			}
		}
		return ns;
	}

	protected synchronized void translateIndividualSet(Set<OWLIndividual> inds) {
		kernel.initArgList();
		for (OWLIndividual ind : inds) {
			IndividualPointer ip = toIndividualPointer(ind);
			kernel.addArg(ip);
		}
		kernel.closeArgList();
	}

	private class ClassExpressionTranslator extends OWLEntityTranslator<OWLClass, ClassPointer> implements
			OWLClassExpressionVisitorEx<ClassPointer> {
		public ClassExpressionTranslator() {
		}

		@Override
		protected ClassPointer getTopEntityPointer() {
			return kernel.getThing();
		}

		@Override
		protected ClassPointer getBottomEntityPointer() {
			return kernel.getNothing();
		}

		@Override
		protected OWLClass getTopEntity() {
			return getOWLDataFactory().getOWLThing();
		}

		@Override
		protected OWLClass getBottomEntity() {
			return getOWLDataFactory().getOWLNothing();
		}

		@Override
		protected ClassPointer createPointerForEntity(OWLClass entity) {
			return kernel.getNamedClass(entity.toStringID());
		}

		@Override
		protected DefaultNode<OWLClass> createDefaultNode() {
			return new OWLClassNode();
		}

		@Override
		protected DefaultNodeSet<OWLClass> createDefaultNodeSet() {
			return new OWLClassNodeSet();
		}

		public ClassPointer visit(OWLClass desc) {
			return getPointerFromEntity(desc);
		}

		public ClassPointer visit(OWLObjectIntersectionOf desc) {
			translateClassExpressionSet(desc.getOperands());
			return kernel.getConceptAnd();
		}

		private void translateClassExpressionSet(Set<OWLClassExpression> classExpressions) {
			kernel.initArgList();
			for (OWLClassExpression ce : classExpressions) {
				ClassPointer cp = ce.accept(this);
				kernel.addArg(cp);
			}
			kernel.closeArgList();
		}

		public ClassPointer visit(OWLObjectUnionOf desc) {
			translateClassExpressionSet(desc.getOperands());
			return kernel.getConceptOr();
		}

		public ClassPointer visit(OWLObjectComplementOf desc) {
			return kernel.getConceptNot(desc.getOperand().accept(this));
		}

		public ClassPointer visit(OWLObjectSomeValuesFrom desc) {
			return kernel.getObjectSome(toObjectPropertyPointer(desc.getProperty()), desc.getFiller().accept(this));
		}

		public ClassPointer visit(OWLObjectAllValuesFrom desc) {
			return kernel.getObjectAll(toObjectPropertyPointer(desc.getProperty()), desc.getFiller().accept(this));
		}

		public ClassPointer visit(OWLObjectHasValue desc) {
			return kernel.getObjectValue(toObjectPropertyPointer(desc.getProperty()),
					toIndividualPointer(desc.getValue()));
		}

		public ClassPointer visit(OWLObjectMinCardinality desc) {
			return kernel.getObjectAtLeast(desc.getCardinality(), toObjectPropertyPointer(desc.getProperty()), desc
					.getFiller().accept(this));
		}

		public ClassPointer visit(OWLObjectExactCardinality desc) {
			return kernel.getObjectExact(desc.getCardinality(), toObjectPropertyPointer(desc.getProperty()), desc
					.getFiller().accept(this));
		}

		public ClassPointer visit(OWLObjectMaxCardinality desc) {
			return kernel.getObjectAtMost(desc.getCardinality(), toObjectPropertyPointer(desc.getProperty()), desc
					.getFiller().accept(this));
		}

		public ClassPointer visit(OWLObjectHasSelf desc) {
			return kernel.getSelf(toObjectPropertyPointer(desc.getProperty()));
		}

		public ClassPointer visit(OWLObjectOneOf desc) {
			translateIndividualSet(desc.getIndividuals());
			return kernel.getOneOf();
		}

		public ClassPointer visit(OWLDataSomeValuesFrom desc) {
			return kernel.getDataSome(toDataPropertyPointer(desc.getProperty()),
					toDataTypeExpressionPointer(desc.getFiller()));
		}

		public ClassPointer visit(OWLDataAllValuesFrom desc) {
			return kernel.getDataAll(toDataPropertyPointer(desc.getProperty()),
					toDataTypeExpressionPointer(desc.getFiller()));
		}

		public ClassPointer visit(OWLDataHasValue desc) {
			return kernel.getDataValue(toDataPropertyPointer(desc.getProperty()), toDataValuePointer(desc.getValue()));
		}

		public ClassPointer visit(OWLDataMinCardinality desc) {
			return kernel.getDataAtLeast(desc.getCardinality(), toDataPropertyPointer(desc.getProperty()),
					toDataTypeExpressionPointer(desc.getFiller()));
		}

		public ClassPointer visit(OWLDataExactCardinality desc) {
			return kernel.getDataExact(desc.getCardinality(), toDataPropertyPointer(desc.getProperty()),
					toDataTypeExpressionPointer(desc.getFiller()));
		}

		public ClassPointer visit(OWLDataMaxCardinality desc) {
			return kernel.getDataAtMost(desc.getCardinality(), toDataPropertyPointer(desc.getProperty()),
					toDataTypeExpressionPointer(desc.getFiller()));
		}
	}

	private class DataRangeTranslator extends OWLEntityTranslator<OWLDatatype, DataTypePointer> implements
			OWLDataRangeVisitorEx<DataTypeExpressionPointer> {
		public DataRangeTranslator() {
		}

		@Override
		protected DataTypePointer getTopEntityPointer() {
			return kernel.getDataTop();
		}

		@Override
		protected DataTypePointer getBottomEntityPointer() {
			return null;
		}

		@Override
		protected DefaultNode<OWLDatatype> createDefaultNode() {
			return new OWLDatatypeNode();
		}

		@Override
		protected OWLDatatype getTopEntity() {
			return getOWLDataFactory().getTopDatatype();
		}

		@Override
		protected OWLDatatype getBottomEntity() {
			return null;
		}

		@Override
		protected DefaultNodeSet<OWLDatatype> createDefaultNodeSet() {
			return new OWLDatatypeNodeSet();
		}

		@Override
		protected DataTypePointer createPointerForEntity(OWLDatatype entity) {
			return kernel.getBuiltInDataType(checkDateTime(entity));
		}

		public DataTypeExpressionPointer visit(OWLDatatype node) {
			return kernel.getBuiltInDataType(checkDateTime(node));
		}

		public DataTypeExpressionPointer visit(OWLDataOneOf node) {
			kernel.initArgList();
			for (OWLLiteral literal : node.getValues()) {
				DataValuePointer dvp = toDataValuePointer(literal);
				kernel.addArg(dvp);
			}
			kernel.closeArgList();
			return kernel.getDataEnumeration();
		}

		public DataTypeExpressionPointer visit(OWLDataComplementOf node) {
			return kernel.getDataNot(node.getDataRange().accept(this));
		}

		public DataTypeExpressionPointer visit(OWLDataIntersectionOf node) {
			translateDataRangeSet(node.getOperands());
			return kernel.getDataIntersectionOf();
		}

		private void translateDataRangeSet(Set<OWLDataRange> dataRanges) {
			kernel.initArgList();
			for (OWLDataRange op : dataRanges) {
				DataTypeExpressionPointer dtp = op.accept(this);
				kernel.addArg(dtp);
			}
			kernel.closeArgList();
		}

		public DataTypeExpressionPointer visit(OWLDataUnionOf node) {
			translateDataRangeSet(node.getOperands());
			return kernel.getDataUnionOf();
		}

		public DataTypeExpressionPointer visit(OWLDatatypeRestriction node) {
			DataTypeExpressionPointer dte = node.getDatatype().accept(this);
			for (OWLFacetRestriction restriction : node.getFacetRestrictions()) {
				DataValuePointer dv = toDataValuePointer(restriction.getFacetValue());
				DataTypeFacet facet;
				if (restriction.getFacet().equals(OWLFacet.MIN_INCLUSIVE)) {
					facet = kernel.getMinInclusiveFacet(dv);
				} else if (restriction.getFacet().equals(OWLFacet.MAX_INCLUSIVE)) {
					facet = kernel.getMaxInclusiveFacet(dv);
				} else if (restriction.getFacet().equals(OWLFacet.MIN_EXCLUSIVE)) {
					facet = kernel.getMinExclusiveFacet(dv);
				} else if (restriction.getFacet().equals(OWLFacet.MAX_EXCLUSIVE)) {
					facet = kernel.getMaxExclusiveFacet(dv);
				} else if (restriction.getFacet().equals(OWLFacet.LENGTH)) {
					facet = kernel.getLength(dv);
				} else if (restriction.getFacet().equals(OWLFacet.MIN_LENGTH)) {
					facet = kernel.getMinLength(dv);
				} else if (restriction.getFacet().equals(OWLFacet.MAX_LENGTH)) {
					facet = kernel.getMaxLength(dv);
				} else if (restriction.getFacet().equals(OWLFacet.FRACTION_DIGITS)) {
					facet = kernel.getFractionDigitsFacet(dv);
				} else if (restriction.getFacet().equals(OWLFacet.PATTERN)) {
					facet = kernel.getPattern(dv);
				} else if (restriction.getFacet().equals(OWLFacet.TOTAL_DIGITS)) {
					facet = kernel.getTotalDigitsFacet(dv);
				} else {
					throw new OWLRuntimeException("Unsupported facet: " + restriction.getFacet());
				}
				dte = kernel.getRestrictedDataType(dte, facet);
			}
			return dte;
		}
	}

	private class IndividualTranslator extends OWLEntityTranslator<OWLNamedIndividual, IndividualPointer> {
		public IndividualTranslator() {
		}

		@Override
		protected IndividualPointer getTopEntityPointer() {
			return null;
		}

		@Override
		protected IndividualPointer getBottomEntityPointer() {
			return null;
		}

		@Override
		protected IndividualPointer createPointerForEntity(OWLNamedIndividual entity) {
			return kernel.getIndividual(entity.toStringID());
		}

		@Override
		protected OWLNamedIndividual getTopEntity() {
			return null;
		}

		@Override
		protected OWLNamedIndividual getBottomEntity() {
			return null;
		}

		@Override
		protected DefaultNode<OWLNamedIndividual> createDefaultNode() {
			return new OWLNamedIndividualNode();
		}

		@Override
		protected DefaultNodeSet<OWLNamedIndividual> createDefaultNodeSet() {
			return new OWLNamedIndividualNodeSet();
		}
	}

	private class ObjectPropertyTranslator extends
			OWLEntityTranslator<OWLObjectPropertyExpression, ObjectPropertyPointer> {
		public ObjectPropertyTranslator() {
		}

		@Override
		protected ObjectPropertyPointer getTopEntityPointer() {
			return kernel.getTopObjectProperty();
		}

		@Override
		protected ObjectPropertyPointer getBottomEntityPointer() {
			return kernel.getBottomObjectProperty();
		}

		// TODO: add implementation of registerNewEntity
		@Override
		protected ObjectPropertyPointer registerNewEntity(OWLObjectPropertyExpression entity) {
			ObjectPropertyPointer pointer = createPointerForEntity(entity);
			fillEntityPointerMaps(entity, pointer);
			entity = entity.getInverseProperty().getSimplified();
			fillEntityPointerMaps(entity, createPointerForEntity(entity));
			return pointer;
		}

		@Override
		protected ObjectPropertyPointer createPointerForEntity(OWLObjectPropertyExpression entity) {
			// FIXME!! think later!!
			ObjectPropertyPointer p = kernel.getObjectProperty(entity.getNamedProperty().toStringID());
			if (entity.isAnonymous()) // inverse!
				p = kernel.getInverseProperty(p);
			return p;
		}

		@Override
		protected OWLObjectProperty getTopEntity() {
			return getOWLDataFactory().getOWLTopObjectProperty();
		}

		@Override
		protected OWLObjectProperty getBottomEntity() {
			return getOWLDataFactory().getOWLBottomObjectProperty();
		}

		@Override
		protected DefaultNode<OWLObjectPropertyExpression> createDefaultNode() {
			return new OWLObjectPropertyNode();
		}

		@Override
		protected DefaultNodeSet<OWLObjectPropertyExpression> createDefaultNodeSet() {
			return new OWLObjectPropertyNodeSet();
		}
	}

	private class DataPropertyTranslator extends OWLEntityTranslator<OWLDataProperty, DataPropertyPointer> {
		public DataPropertyTranslator() {
		}

		@Override
		protected DataPropertyPointer getTopEntityPointer() {
			return kernel.getTopDataProperty();
		}

		@Override
		protected DataPropertyPointer getBottomEntityPointer() {
			return kernel.getBottomDataProperty();
		}

		@Override
		protected DataPropertyPointer createPointerForEntity(OWLDataProperty entity) {
			return kernel.getDataProperty(entity.toStringID());
		}

		@Override
		protected OWLDataProperty getTopEntity() {
			return getOWLDataFactory().getOWLTopDataProperty();
		}

		@Override
		protected OWLDataProperty getBottomEntity() {
			return getOWLDataFactory().getOWLBottomDataProperty();
		}

		@Override
		protected DefaultNode<OWLDataProperty> createDefaultNode() {
			return new OWLDataPropertyNode();
		}

		@Override
		protected DefaultNodeSet<OWLDataProperty> createDefaultNodeSet() {
			return new OWLDataPropertyNodeSet();
		}
	}

	private class AxiomTranslator implements OWLAxiomVisitorEx<AxiomPointer> {
		private final class DeclarationVisitorEx implements OWLEntityVisitorEx<AxiomPointer> {
			public AxiomPointer visit(OWLClass cls) {
				return kernel.tellClassDeclaration(toClassPointer(cls));
			}

			public AxiomPointer visit(OWLObjectProperty property) {
				return kernel.tellObjectPropertyDeclaration(toObjectPropertyPointer(property));
			}

			public AxiomPointer visit(OWLDataProperty property) {
				return kernel.tellDataPropertyDeclaration(toDataPropertyPointer(property));
			}

			public AxiomPointer visit(OWLNamedIndividual individual) {
				return kernel.tellIndividualDeclaration(toIndividualPointer(individual));
			}

			public AxiomPointer visit(OWLDatatype datatype) {
				return kernel.tellDatatypeDeclaration(toDataTypePointer(datatype));
			}

			public AxiomPointer visit(OWLAnnotationProperty property) {
				return null;
			}
		}

		private final DeclarationVisitorEx v;

		public AxiomTranslator() {
			v = new DeclarationVisitorEx();
		}

		public AxiomPointer visit(OWLSubClassOfAxiom axiom) {
			return kernel.tellSubClassOf(toClassPointer(axiom.getSubClass()), toClassPointer(axiom.getSuperClass()));
		}

		public AxiomPointer visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
			return kernel.tellNotRelatedIndividuals(toIndividualPointer(axiom.getSubject()),
					toObjectPropertyPointer(axiom.getProperty()), toIndividualPointer(axiom.getObject()));
		}

		public AxiomPointer visit(OWLAsymmetricObjectPropertyAxiom axiom) {
			return kernel.tellAsymmetricObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLReflexiveObjectPropertyAxiom axiom) {
			return kernel.tellReflexiveObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLDisjointClassesAxiom axiom) {
			translateClassExpressionSet(axiom.getClassExpressions());
			return kernel.tellDisjointClasses();
		}

		private void translateClassExpressionSet(Collection<OWLClassExpression> classExpressions) {
			kernel.initArgList();
			for (OWLClassExpression ce : classExpressions) {
				ClassPointer cp = toClassPointer(ce);
				kernel.addArg(cp);
			}
			kernel.closeArgList();
		}

		public AxiomPointer visit(OWLDataPropertyDomainAxiom axiom) {
			return kernel.tellDataPropertyDomain(toDataPropertyPointer(axiom.getProperty()),
					toClassPointer(axiom.getDomain()));
		}

		public AxiomPointer visit(OWLObjectPropertyDomainAxiom axiom) {
			return kernel.tellObjectPropertyDomain(toObjectPropertyPointer(axiom.getProperty()),
					toClassPointer(axiom.getDomain()));
		}

		public AxiomPointer visit(OWLEquivalentObjectPropertiesAxiom axiom) {
			translateObjectPropertySet(axiom.getProperties());
			return kernel.tellEquivalentObjectProperties();
		}

		private void translateObjectPropertySet(Collection<OWLObjectPropertyExpression> properties) {
			kernel.initArgList();
			for (OWLObjectPropertyExpression property : properties) {
				ObjectPropertyPointer opp = toObjectPropertyPointer(property);
				kernel.addArg(opp);
			}
			kernel.closeArgList();
		}

		public AxiomPointer visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
			return kernel.tellNotRelatedIndividualValue(toIndividualPointer(axiom.getSubject()),
					toDataPropertyPointer(axiom.getProperty()), toDataValuePointer(axiom.getObject()));
		}

		public AxiomPointer visit(OWLDifferentIndividualsAxiom axiom) {
			translateIndividualSet(axiom.getIndividuals());
			return kernel.tellDifferentIndividuals();
		}

		public AxiomPointer visit(OWLDisjointDataPropertiesAxiom axiom) {
			translateDataPropertySet(axiom.getProperties());
			return kernel.tellDisjointDataProperties();
		}

		private void translateDataPropertySet(Set<OWLDataPropertyExpression> properties) {
			kernel.initArgList();
			for (OWLDataPropertyExpression property : properties) {
				DataPropertyPointer dpp = toDataPropertyPointer(property);
				kernel.addArg(dpp);
			}
			kernel.closeArgList();
		}

		public AxiomPointer visit(OWLDisjointObjectPropertiesAxiom axiom) {
			translateObjectPropertySet(axiom.getProperties());
			return kernel.tellDisjointObjectProperties();
		}

		public AxiomPointer visit(OWLObjectPropertyRangeAxiom axiom) {
			return kernel.tellObjectPropertyRange(toObjectPropertyPointer(axiom.getProperty()),
					toClassPointer(axiom.getRange()));
		}

		public AxiomPointer visit(OWLObjectPropertyAssertionAxiom axiom) {
			return kernel.tellRelatedIndividuals(toIndividualPointer(axiom.getSubject()),
					toObjectPropertyPointer(axiom.getProperty()), toIndividualPointer(axiom.getObject()));
		}

		public AxiomPointer visit(OWLFunctionalObjectPropertyAxiom axiom) {
			return kernel.tellFunctionalObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLSubObjectPropertyOfAxiom axiom) {
			return kernel.tellSubObjectProperties(toObjectPropertyPointer(axiom.getSubProperty()),
					toObjectPropertyPointer(axiom.getSuperProperty()));
		}

		public AxiomPointer visit(OWLDisjointUnionAxiom axiom) {
			translateClassExpressionSet(axiom.getClassExpressions());
			return kernel.tellDisjointUnion(toClassPointer(axiom.getOWLClass()));
		}

		public AxiomPointer visit(OWLDeclarationAxiom axiom) {
			OWLEntity entity = axiom.getEntity();
			return entity.accept(v);
		}

		public AxiomPointer visit(OWLAnnotationAssertionAxiom axiom) {
			// Ignore
			return null;
		}

		public AxiomPointer visit(OWLSymmetricObjectPropertyAxiom axiom) {
			return kernel.tellSymmetricObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLDataPropertyRangeAxiom axiom) {
			return kernel.tellDataPropertyRange(toDataPropertyPointer(axiom.getProperty()),
					toDataTypeExpressionPointer(axiom.getRange()));
		}

		public AxiomPointer visit(OWLFunctionalDataPropertyAxiom axiom) {
			return kernel.tellFunctionalDataProperty(toDataPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLEquivalentDataPropertiesAxiom axiom) {
			translateDataPropertySet(axiom.getProperties());
			return kernel.tellEquivalentDataProperties();
		}

		public AxiomPointer visit(OWLClassAssertionAxiom axiom) {
			return kernel.tellIndividualType(toIndividualPointer(axiom.getIndividual()),
					toClassPointer(axiom.getClassExpression()));
		}

		public AxiomPointer visit(OWLEquivalentClassesAxiom axiom) {
			translateClassExpressionSet(axiom.getClassExpressions());
			return kernel.tellEquivalentClass();
		}

		public AxiomPointer visit(OWLDataPropertyAssertionAxiom axiom) {
			return kernel.tellRelatedIndividualValue(toIndividualPointer(axiom.getSubject()),
					toDataPropertyPointer(axiom.getProperty()), toDataValuePointer(axiom.getObject()));
		}

		public AxiomPointer visit(OWLTransitiveObjectPropertyAxiom axiom) {
			return kernel.tellTransitiveObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
			return kernel.tellIrreflexiveObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLSubDataPropertyOfAxiom axiom) {
			return kernel.tellSubDataProperties(toDataPropertyPointer(axiom.getSubProperty()),
					toDataPropertyPointer(axiom.getSuperProperty()));
		}

		public AxiomPointer visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
			return kernel.tellInverseFunctionalObjectProperty(toObjectPropertyPointer(axiom.getProperty()));
		}

		public AxiomPointer visit(OWLSameIndividualAxiom axiom) {
			translateIndividualSet(axiom.getIndividuals());
			return kernel.tellSameIndividuals();
		}

		public AxiomPointer visit(OWLSubPropertyChainOfAxiom axiom) {
			translateObjectPropertySet(axiom.getPropertyChain());
			return kernel.tellSubObjectProperties(kernel.getPropertyComposition(),
					toObjectPropertyPointer(axiom.getSuperProperty()));
		}

		public AxiomPointer visit(OWLInverseObjectPropertiesAxiom axiom) {
			return kernel.tellInverseProperties(toObjectPropertyPointer(axiom.getFirstProperty()),
					toObjectPropertyPointer(axiom.getSecondProperty()));
		}

		public AxiomPointer visit(OWLHasKeyAxiom axiom) {
			translateObjectPropertySet(axiom.getObjectPropertyExpressions());
			ObjectPropertyPointer objectPropertyPointer = kernel.getObjectPropertyKey();
			translateDataPropertySet(axiom.getDataPropertyExpressions());
			DataPropertyPointer dataPropertyPointer = kernel.getDataPropertyKey();
			return kernel.tellHasKey(toClassPointer(axiom.getClassExpression()), dataPropertyPointer,
					objectPropertyPointer);
		}

		public AxiomPointer visit(OWLDatatypeDefinitionAxiom axiom) {
			kernel.getDataSubType(axiom.getDatatype().getIRI().toString(),
					toDataTypeExpressionPointer(axiom.getDataRange()));
			return null;
		}

		public AxiomPointer visit(SWRLRule rule) {
			// Ignore
			return null;
		}

		public AxiomPointer visit(OWLSubAnnotationPropertyOfAxiom axiom) {
			// Ignore
			return null;
		}

		public AxiomPointer visit(OWLAnnotationPropertyDomainAxiom axiom) {
			// Ignore
			return null;
		}

		public AxiomPointer visit(OWLAnnotationPropertyRangeAxiom axiom) {
			// Ignore
			return null;
		}
	}

	private class EntailmentChecker implements OWLAxiomVisitorEx<Boolean> {
		public EntailmentChecker() {
		}

		public Boolean visit(OWLSubClassOfAxiom axiom) {
			if (axiom.getSuperClass().equals(getOWLDataFactory().getOWLThing())) {
				return true;
			}
			if (axiom.getSubClass().equals(getOWLDataFactory().getOWLNothing())) {
				return true;
			}
			return kernel.isClassSubsumedBy(toClassPointer(axiom.getSubClass()), toClassPointer(axiom.getSuperClass()));
		}

		public Boolean visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLAsymmetricObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyAsymmetric(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLReflexiveObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyReflexive(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLDisjointClassesAxiom axiom) {
			Set<OWLClassExpression> classExpressions = axiom.getClassExpressions();
			if (classExpressions.size() == 2) {
				Iterator<OWLClassExpression> it = classExpressions.iterator();
				return kernel.isClassDisjointWith(toClassPointer(it.next()), toClassPointer(it.next()));
			} else {
				for (OWLAxiom ax : axiom.asOWLSubClassOfAxioms()) {
					if (!ax.accept(this)) {
						return false;
					}
				}
				return true;
			}
		}

		public Boolean visit(OWLDataPropertyDomainAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLObjectPropertyDomainAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLEquivalentObjectPropertiesAxiom axiom) {
			for (OWLAxiom ax : axiom.asSubObjectPropertyOfAxioms()) {
				if (!ax.accept(this)) {
					return false;
				}
			}
			return true;
		}

		public Boolean visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLDifferentIndividualsAxiom axiom) {
			for (OWLSubClassOfAxiom ax : axiom.asOWLSubClassOfAxioms()) {
				if (!ax.accept(this)) {
					return false;
				}
			}
			return true;
		}

		// TODO: this check is incomplete
		public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
			kernel.initArgList();
			for (OWLDataPropertyExpression p : axiom.getProperties()) {
				kernel.addArg(toDataPropertyPointer(p));
			}
			kernel.closeArgList();
			return kernel.arePropertiesDisjoint();
		}

		public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
			kernel.initArgList();
			for (OWLObjectPropertyExpression p : axiom.getProperties()) {
				kernel.addArg(toObjectPropertyPointer(p));
			}
			kernel.closeArgList();
			return kernel.arePropertiesDisjoint();
		}

		public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLFunctionalObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyFunctional(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLSubObjectPropertyOfAxiom axiom) {
			return kernel.isObjectSubPropertyOf(toObjectPropertyPointer(axiom.getSubProperty()),
					toObjectPropertyPointer(axiom.getSuperProperty()));
		}

		public Boolean visit(OWLDisjointUnionAxiom axiom) {
			return axiom.getOWLEquivalentClassesAxiom().accept(this) && axiom.getOWLDisjointClassesAxiom().accept(this);
		}

		public Boolean visit(OWLDeclarationAxiom axiom) {
			// TODO uhm might be needed?
			return false;
		}

		public Boolean visit(OWLAnnotationAssertionAxiom axiom) {
			return false;
		}

		public Boolean visit(OWLSymmetricObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertySymmetric(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLDataPropertyRangeAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLFunctionalDataPropertyAxiom axiom) {
			return kernel.isDataPropertyFunctional(toDataPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLEquivalentDataPropertiesAxiom axiom) {
			// TODO check
			// this is not implemented in OWL API
			// for (OWLAxiom ax : axiom.asSubDataPropertyOfAxioms()) {
			// if (!ax.accept(this)) {
			// return false;
			// }
			// }
			// return true;

			return null;
		}

		public Boolean visit(OWLClassAssertionAxiom axiom) {
			return kernel.isInstanceOf(toIndividualPointer(axiom.getIndividual()),
					toClassPointer(axiom.getClassExpression()));
		}

		public Boolean visit(OWLEquivalentClassesAxiom axiom) {
			Set<OWLClassExpression> classExpressionSet = axiom.getClassExpressions();
			if (classExpressionSet.size() == 2) {
				Iterator<OWLClassExpression> it = classExpressionSet.iterator();
				return kernel.isClassEquivalentTo(toClassPointer(it.next()), toClassPointer(it.next()));
			} else {
				for (OWLAxiom ax : axiom.asOWLSubClassOfAxioms()) {
					if (!ax.accept(this)) {
						return false;
					}
				}
				return true;
			}
		}

		public Boolean visit(OWLDataPropertyAssertionAxiom axiom) {
			return axiom.asOWLSubClassOfAxiom().accept(this);
		}

		public Boolean visit(OWLTransitiveObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyTransitive(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyIrreflexive(toObjectPropertyPointer(axiom.getProperty()));
		}

		// TODO: this is incomplete
		public Boolean visit(OWLSubDataPropertyOfAxiom axiom) {
			return kernel.isDataSubPropertyOf(toDataPropertyPointer(axiom.getSubProperty()),
					toDataPropertyPointer(axiom.getSuperProperty()));
		}

		public Boolean visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
			return kernel.isObjectPropertyInverseFunctional(toObjectPropertyPointer(axiom.getProperty()));
		}

		public Boolean visit(OWLSameIndividualAxiom axiom) {
			for (OWLSameIndividualAxiom ax : axiom.asPairwiseAxioms()) {
				Iterator<OWLIndividual> it = ax.getIndividuals().iterator();
				OWLIndividual indA = it.next();
				OWLIndividual indB = it.next();
				if (!kernel.isSameAs(toIndividualPointer(indA), toIndividualPointer(indB))) {
					return false;
				}
			}
			return true;
		}

		public Boolean visit(OWLSubPropertyChainOfAxiom axiom) {
			kernel.initArgList();
			for (OWLObjectPropertyExpression p : axiom.getPropertyChain()) {
				kernel.addArg(toObjectPropertyPointer(p));
			}
			kernel.closeArgList();
			return kernel.isSubPropertyChainOf(toObjectPropertyPointer(axiom.getSuperProperty()));
		}

		public Boolean visit(OWLInverseObjectPropertiesAxiom axiom) {
			for (OWLAxiom ax : axiom.asSubObjectPropertyOfAxioms()) {
				if (!ax.accept(this)) {
					return false;
				}
			}
			return true;
		}

		public Boolean visit(OWLHasKeyAxiom axiom) {
			// FIXME!! unsupported by FaCT++ ATM
			return null;
		}

		public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
			// FIXME!! unsupported by FaCT++ ATM
			return null;
		}

		public Boolean visit(SWRLRule rule) {
			// FIXME!! unsupported by FaCT++ ATM
			return null;
		}

		public Boolean visit(OWLSubAnnotationPropertyOfAxiom axiom) {
			return false;
		}

		public Boolean visit(OWLAnnotationPropertyDomainAxiom axiom) {
			return false;
		}

		public Boolean visit(OWLAnnotationPropertyRangeAxiom axiom) {
			return false;
		}
	}

	public synchronized void dispose() {
		manager.removeOntologyChangeListener(this);
		axiomTranslator = null;
		classExpressionTranslator = null;
		dataRangeTranslator = null;
		objectPropertyTranslator = null;
		dataPropertyTranslator = null;
		individualTranslator = null;
		entailmentChecker = null;
		axiom2PtrMap.clear();
		ptr2AxiomMap.clear();
		rawChanges.clear();
		reasonerAxioms.clear();
		kernel.dispose();
	}

	private static class ProgressMonitorAdapter implements FaCTPlusPlusProgressMonitor {
		private int count = 0;
		private int total = 0;
		private final ReasonerProgressMonitor progressMonitor;
		private final AtomicBoolean interrupted;

		public ProgressMonitorAdapter(ReasonerProgressMonitor p, AtomicBoolean interr) {
			this.progressMonitor = p;
			this.interrupted = interr;
		}

		public void setClassificationStarted(int classCount) {
			count = 0;
			total = classCount;
			progressMonitor.reasonerTaskStarted(ReasonerProgressMonitor.CLASSIFYING);
			progressMonitor.reasonerTaskProgressChanged(count, classCount);
		}

		public void nextClass() {
			count++;
			progressMonitor.reasonerTaskProgressChanged(count, total);
		}

		public void setFinished() {
			progressMonitor.reasonerTaskStopped();
		}

		public boolean isCancelled() {
			if (interrupted.get()) {
				throw new ReasonerInterruptedException();
			}
			return false;
		}
	}

	public void dumpClassHierarchy(PrintStream pw, boolean includeBottomNode) {
		dumpSubClasses(getTopClassNode(), pw, 0, includeBottomNode);
	}

	private void dumpSubClasses(Node<OWLClass> node, PrintStream pw, int depth, boolean includeBottomNode) {
		if (includeBottomNode || !node.isBottomNode()) {
			for (int i = 0; i < depth; i++) {
				pw.print("    ");
			}
			pw.println(node);
			for (Node<OWLClass> sub : getSubClasses(node.getRepresentativeElement(), true)) {
				dumpSubClasses(sub, pw, depth + 1, includeBottomNode);
			}
		}
	}

	public NodePointer getRoot(OWLClassExpression expression) {
		return kernel.buildCompletionTree(toClassPointer(expression));
	}

	public Node<? extends OWLObjectPropertyExpression> getObjectNeighbours(NodePointer object, boolean deterministicOnly) {
		return objectPropertyTranslator.getNodeFromPointers(kernel.getObjectNeighbours(object, deterministicOnly));
	}

	public Node<OWLDataProperty> getDataNeighbours(NodePointer object, boolean deterministicOnly) {
		return dataPropertyTranslator.getNodeFromPointers(kernel.getDataNeighbours(object, deterministicOnly));
	}

	public Collection<NodePointer> getObjectNeighbours(NodePointer n, OWLObjectProperty property) {
		return Arrays.asList(kernel.getObjectNeighbours(n, toObjectPropertyPointer(property)));
	}

	public Collection<NodePointer> getDataNeighbours(NodePointer n, OWLDataProperty property) {
		return Arrays.asList(kernel.getDataNeighbours(n, toDataPropertyPointer(property)));
	}

	public Node<? extends OWLClassExpression> getObjectLabel(NodePointer object, boolean deterministicOnly) {
		return classExpressionTranslator.getNodeFromPointers(kernel.getObjectLabel(object, deterministicOnly));
	}

	public Node<? extends OWLDataRange> getDataLabel(NodePointer object, boolean deterministicOnly) {
		return dataRangeTranslator.getNodeFromPointers(kernel.getDataLabel(object, deterministicOnly));
	}

	/**
	 * Build an atomic decomposition using syntactic/semantic locality checking
	 * 
	 * @param useSemantic
	 *            if true, use semantic locality checking; if false, use
	 *            syntactic one
	 * @param moduleType
	 *            if 0, use \bot modules; if 1, use \top modules; if 2, use STAR
	 *            modules
	 * @return the size of the constructed atomic decomposition
	 */
	public int getAtomicDecompositionSize(boolean useSemantic, int moduleType) {
		return kernel.getAtomicDecompositionSize(useSemantic, moduleType);
	}

	public Set<OWLAxiom> getAtomAxioms(int index) {
		AxiomPointer[] axioms = kernel.getAtomAxioms(index);
		return axiomsToSet(axioms);
	}

	private Set<OWLAxiom> axiomsToSet(AxiomPointer[] axioms) {
		Set<OWLAxiom> toReturn = new HashSet<OWLAxiom>();
		for (AxiomPointer p : axioms) {
			final OWLAxiom owlAxiom = ptr2AxiomMap.get(p);
			if (owlAxiom != null) {
				toReturn.add(owlAxiom);
			}
		}
		return toReturn;
	}

	public int[] getAtomDependents(int index) {
		return kernel.getAtomDependents(index);
	}

	private final class EntityVisitorEx implements OWLEntityVisitorEx<Pointer> {
		public Pointer visit(OWLClass cls) {
			return toClassPointer(cls);
		}

		public Pointer visit(OWLObjectProperty property) {
			return toObjectPropertyPointer(property);
		}

		public Pointer visit(OWLDataProperty property) {
			return toDataPropertyPointer(property);
		}

		public Pointer visit(OWLNamedIndividual individual) {
			return toIndividualPointer(individual);
		}

		public Pointer visit(OWLDatatype datatype) {
			return null;
		}

		public Pointer visit(OWLAnnotationProperty property) {
			return null;
		}
	}

	final EntityVisitorEx entityTranslator = new EntityVisitorEx();

	/**
	 * 
	 * @param signature
	 *            if true, use semantic locality checking; if false, use
	 *            syntactic one
	 * @param moduleType
	 *            if 0, use \bot modules; if 1, use \top modules; if 2, use STAR
	 *            modules
	 * @return
	 */
	public Set<OWLAxiom> getModule(Set<OWLEntity> signature, boolean useSemantic, int moduleType) {
		kernel.initArgList();
		for (OWLEntity entity : signature)
			kernel.addArg(entity.accept(entityTranslator));
		AxiomPointer[] axioms = kernel.getModule(useSemantic, moduleType);
		return axiomsToSet(axioms);
	}

	public Set<OWLAxiom> getNonLocal(Set<OWLEntity> signature, boolean useSemantic, int moduleType) {
		kernel.initArgList();
		for (OWLEntity entity : signature)
			kernel.addArg(entity.accept(entityTranslator));
		AxiomPointer[] axioms = kernel.getNonLocal(useSemantic, moduleType);
		return axiomsToSet(axioms);
	}
}
