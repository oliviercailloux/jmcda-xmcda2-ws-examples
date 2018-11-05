package org.decisiondeck.jmcda.xws.ws;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decisiondeck.jmcda.exc.InputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.sorting.SortingFull;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.view.ProblemViewFactory;
import org.decisiondeck.jmcda.xws.IXWS;
import org.decisiondeck.jmcda.xws.XWSExceptions;
import org.decisiondeck.jmcda.xws.XWSInput;
import org.decisiondeck.jmcda.xws.XWSOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

public class XWSSorting implements IXWS {

    private static final Logger s_logger = LoggerFactory.getLogger(XWSSorting.class);

    @XWSInput(name = "alternatives.xml", optional = true)
    public Set<Alternative> m_alternatives;
    @XWSInput(name = "categories_profiles.xml")
    public CatsAndProfs m_cats;
    @XWSOutput(name = "affectations.xml")
    public IOrderedAssignmentsToMultipleRead m_affectations;

    @XWSInput(name = "criteria.xml")
    public Map<Criterion, Interval> m_scales;

    @XWSInput(name = "criteria.xml", optional = true)
    public Thresholds m_thresholds;

    @XWSInput(name = "weights.xml")
    public Weights m_weights;

    @XWSInput(name = "performances_alternatives.xml", transformer = ReadPerformancesReal.class)
    public Evaluations m_alternativesEvaluations;
    @XWSInput(name = "performances_profiles.xml", transformer = ReadPerformancesFictive.class)
    public Evaluations m_profilesEvaluations;

    @XWSOutput(name = "messages.xml")
    @XWSExceptions
    public List<InvalidInputException> m_exceptions;

    @XWSInput(name = "criteria.xml", optional = true)
    public Set<Criterion> m_criteria;

    @XWSInput(name = "cut_threshold.xml")
    public double m_cutThreshold;

    @XWSInput(name = "sorting_mode.xml")
    public String m_sortingMode;

    @XWSInput(name = "crisp_vetoes.xml", optional = true)
    public Boolean m_useCrispVetoesInput;

    @Override
    public void execute() throws InvalidInputException {
	s_logger.info("Computing sorting.");
	InputCheck.check(Sets.intersection(m_alternativesEvaluations.getRows(), m_profilesEvaluations.getRows())
		.isEmpty(), "Found alternatives which are also profiles in the evaluations.");
	checkState(m_alternativesEvaluations != null);
	checkState(m_cats != null);
	InputCheck.check(Sets.intersection(m_alternativesEvaluations.getRows(), m_cats.getProfiles()).isEmpty(),
		"Found evaluated alternatives which are also used as profiles in the categories.");
	final Coalitions coalitions = CoalitionsUtils.wrap(m_weights);
	coalitions.setMajorityThreshold(m_cutThreshold);
	final ISortingPreferences sortingPreferences = ProblemFactory.newSortingPreferences(
		m_alternativesEvaluations,
		m_scales, m_cats, m_profilesEvaluations, m_thresholds, coalitions);
	final Predicate<Alternative> inAlts = m_alternatives == null ? null : Predicates.in(m_alternatives);
	final Predicate<Criterion> inCrits = m_criteria == null ? null : Predicates.in(m_criteria);
	final ISortingPreferences restricted = ProblemViewFactory.getRestrictedPreferences(sortingPreferences, inAlts,
		inCrits);

	if (restricted.getAlternatives().isEmpty()) {
	    throw new InvalidInputException("No alternatives to be sorted.");
	}
	if (!SortingMode.strings().contains(m_sortingMode)) {
	    throw new InvalidInputException("Unexpected sorting mode: " + m_sortingMode + ".");
	}
	final SortingMode mode = SortingMode.valueOf(m_sortingMode);

	final SortingFull sorting = new SortingFull();
	final boolean useCrispVetoes = m_useCrispVetoesInput == null ? false : m_useCrispVetoesInput.booleanValue();
	sorting.setSharpVetoes(useCrispVetoes);
	sorting.setTolerance(0);
	m_affectations = sorting.assign(mode, restricted);
	s_logger.info("Finished working.");
    }
}
