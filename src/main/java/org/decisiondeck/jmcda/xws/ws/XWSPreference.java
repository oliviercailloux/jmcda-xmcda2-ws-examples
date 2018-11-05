package org.decisiondeck.jmcda.xws.ws;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.outranking.Concordance;
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

public class XWSPreference implements IXWS {
    private static final Logger s_logger = LoggerFactory.getLogger(XWSPreference.class);

    @XWSInput(name = "alternatives.xml", optional = true)
    public Set<Alternative> m_alternatives;

    @XWSInput(name = "criteria.xml")
    public Map<Criterion, Interval> m_scales;

    @XWSInput(name = "criteria.xml", optional = true)
    public Thresholds m_thresholds;

    @XWSInput(name = "weights.xml")
    public Weights m_weights;

    @XWSInput(name = "performances.xml")
    public Evaluations m_evaluations;

    @XWSOutput(name = "messages.xml")
    @XWSExceptions
    public List<InvalidInputException> m_exceptions;

    @XWSOutput(name = "preference.xml")
    public SparseMatrixFuzzy<Alternative, Alternative> m_preference;

    @XWSInput(name = "criteria.xml", optional = true)
    public Set<Criterion> m_criteria;

    @Override
    public void execute() throws InvalidInputException {
	s_logger.info("Computing preference.");
	final Predicate<Alternative> inAlts = m_alternatives == null ? null : Predicates.in(m_alternatives);
	final Predicate<Criterion> inCrits = m_criteria == null ? null : Predicates.in(m_criteria);
	final ISortingPreferences preferences = ProblemFactory.newSortingPreferences(m_evaluations, m_scales, null,
		null, m_thresholds, CoalitionsUtils.wrap(m_weights));
	final ISortingPreferences restricted = ProblemViewFactory
		.getRestrictedPreferences(preferences, inAlts, inCrits);
	m_preference = new Concordance().preference(restricted, restricted.getThresholds(), restricted.getCoalitions()
		.getWeights());
	s_logger.info("Finished working.");
    }
}
