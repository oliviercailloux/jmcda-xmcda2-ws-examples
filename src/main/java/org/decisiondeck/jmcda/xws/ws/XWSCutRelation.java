package org.decisiondeck.jmcda.xws.ws;

import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.utils.matrix.Matrixes;
import org.decision_deck.utils.matrix.SparseMatrixD;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.utils.CutRel;
import org.decisiondeck.jmcda.xws.IXWS;
import org.decisiondeck.jmcda.xws.XWSExceptions;
import org.decisiondeck.jmcda.xws.XWSInput;
import org.decisiondeck.jmcda.xws.XWSOutput;

import com.google.common.collect.Sets;

public class XWSCutRelation implements IXWS {
    @XWSInput(name = "relation.xml")
    public SparseMatrixD<Alternative, Alternative> m_input;

    @XWSOutput(name = "binary_relation.xml")
    public SparseMatrixFuzzy<Alternative, Alternative> m_output;

    @XWSInput(name = "cut_threshold.xml")
    public double m_cutThreshold;

    @XWSOutput(name = "messages.xml")
    @XWSExceptions
    public List<InvalidInputException> m_exceptions;

    @XWSInput(name = "alternatives.xml", optional = true)
    public Set<Alternative> m_alternatives;

    @Override
    public void execute() throws InvalidInputException {
	final Set<Alternative> inputAlternatives = Sets.union(m_input.getRows(), m_input.getColumns());
	final Set<Alternative> toKeep = m_alternatives == null ? inputAlternatives : m_alternatives;
	final SparseMatrixD<Alternative, Alternative> in = Matrixes.newSparseD();
	for (Alternative alternative : toKeep) {
	    for (Alternative column : toKeep) {
		final Double entry = m_input.getEntry(alternative, column);
		if (entry != null) {
		    in.put(alternative, column, entry.doubleValue());
		}
	    }
	}
	m_output = new CutRel<Alternative, Alternative>().cut(in, m_cutThreshold);
    }

}
