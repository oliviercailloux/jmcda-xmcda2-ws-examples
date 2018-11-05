package org.decisiondeck.jmcda.xws.ws;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.outranking.Outranking;
import org.decisiondeck.jmcda.xws.IXWS;
import org.decisiondeck.jmcda.xws.XWSExceptions;
import org.decisiondeck.jmcda.xws.XWSInput;
import org.decisiondeck.jmcda.xws.XWSOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class XWSOutranking implements IXWS {
    private static final Logger s_logger = LoggerFactory.getLogger(XWSOutranking.class);

    @XWSOutput(name = "messages.xml")
    @XWSExceptions
    public List<InvalidInputException> m_exceptions;

    @XWSInput(name = "concordance.xml")
    public SparseMatrixFuzzy<Alternative, Alternative> m_concordance;

    @XWSInput(name = "discordances.xml")
    public Map<Criterion, SparseAlternativesMatrixFuzzy> m_discordances;

    @XWSOutput(name = "outranking.xml")
    public SparseMatrixFuzzy<Alternative, Alternative> m_outranking;

    @XWSInput(name = "alternatives.xml", optional = true)
    public Set<Alternative> m_alternatives;

    @XWSInput(name = "criteria.xml", optional = true)
    public Set<Criterion> m_criteria;

    @Override
    public void execute() throws InvalidInputException {
	s_logger.info("Computing outranking.");
	final Outranking outranking = new Outranking();
	outranking.setTolerance(0);
	final Set<Alternative> inputAlternatives = Sets.union(m_concordance.getRows(), m_concordance.getColumns());
	final Set<Alternative> alternatives = m_alternatives == null ? inputAlternatives : m_alternatives;
	final Set<Criterion> inputCriteria = m_discordances.keySet();
	final Set<Criterion> criteria = m_criteria == null ? inputCriteria : m_criteria;
	m_outranking = outranking.getOutranking(alternatives, criteria, m_concordance,
		m_discordances);
	s_logger.info("Finished working.");
    }
}
