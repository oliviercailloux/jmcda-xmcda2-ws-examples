package org.decisiondeck.jmcda.xws.ws;

import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.utils.matrix.Matrixes;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.xws.IXWS;
import org.decisiondeck.jmcda.xws.XWSExceptions;
import org.decisiondeck.jmcda.xws.XWSInput;
import org.decisiondeck.jmcda.xws.XWSOutput;
import org.decisiondeck.xmcda_oo.services.flow.Flow;
import org.decisiondeck.xmcda_oo.services.flow.FlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class XWSFlows implements IXWS {
	private static final Logger s_logger = LoggerFactory.getLogger(XWSFlows.class);

	@XWSInput(name = "preference.xml")
	public SparseMatrixFuzzy<Alternative, Alternative> m_preference;

	@XWSInput(name = "flow_type.xml")
	public String m_flowType;

	@XWSOutput(name = "flows.xml")
	public AlternativesScores m_flows;

	@XWSOutput(name = "messages.xml")
	@XWSExceptions
	public List<InvalidInputException> m_exceptions;

	@XWSInput(name = "alternatives.xml", optional = true)
	public Set<Alternative> m_alternatives;

	@Override
	public void execute() throws InvalidInputException {
		s_logger.info("Computing flows.");
		if (!FlowType.strings().contains(m_flowType)) {
			throw new InvalidInputException("Unexpected flow type: " + m_flowType + ".");
		}
		final FlowType type = FlowType.valueOf(m_flowType);
		final Set<Alternative> inputAlternatives = Sets.union(m_preference.getRows(), m_preference.getColumns());
		final Set<Alternative> toKeep = m_alternatives == null ? inputAlternatives : m_alternatives;
		final SparseMatrixD<Alternative, Alternative> preference = Matrixes.newSparseD();
		for (Alternative alternative : toKeep) {
			for (Alternative column : toKeep) {
				final Double entry = m_preference.getEntry(alternative, column);
				if (entry != null) {
					preference.put(alternative, column, entry.doubleValue());
				}
			}
		}
		if (preference.isEmpty()) {
			throw new InvalidInputException("Empty input preferences: nothing to compute.");
		}
		m_flows = new Flow().getFlows(type, preference);
		s_logger.info("Finished working.");
	}
}
