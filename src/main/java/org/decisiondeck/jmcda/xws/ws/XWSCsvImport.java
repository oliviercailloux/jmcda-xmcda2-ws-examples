package org.decisiondeck.jmcda.xws.ws;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.text.CsvImporterEvaluations;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.jmcda.xws.IXWS;
import org.decisiondeck.jmcda.xws.XWSExceptions;
import org.decisiondeck.jmcda.xws.XWSInput;
import org.decisiondeck.jmcda.xws.XWSOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

public class XWSCsvImport implements IXWS {
	private static final Logger s_logger = LoggerFactory.getLogger(XWSCsvImport.class);

	@XWSOutput(name = "alternatives.xml")
	public Set<Alternative> m_alternatives;
	@XWSOutput(name = "criteria.xml")
	public Set<Criterion> m_criteria;

	@XWSOutput(name = "performances.xml")
	public Evaluations m_evaluations;

	@XWSOutput(name = "messages.xml")
	@XWSExceptions
	public List<InvalidInputException> m_exceptions;

	@XWSInput(name = "performances.csv")
	public ByteSource m_source;

	@Override
	public void execute() throws InvalidInputException {
		s_logger.info("Reading CSV.");
		final CsvImporterEvaluations importer = new CsvImporterEvaluations(m_source.asCharSource(Charsets.UTF_8));
		final IProblemData readData;
		try {
			readData = importer.read();
		} catch (IOException exc) {
			throw new InvalidInputException(exc);
		}
		m_alternatives = readData.getAlternatives();
		m_criteria = readData.getCriteria();
		final EvaluationsRead readEvaluations = readData.getAlternativesEvaluations();
		if (readEvaluations.isEmpty()) {
			m_evaluations = null;
		} else {
			m_evaluations = EvaluationsUtils.newEvaluationMatrix(readEvaluations);
		}
		s_logger.info("Finished working.");
	}
}
