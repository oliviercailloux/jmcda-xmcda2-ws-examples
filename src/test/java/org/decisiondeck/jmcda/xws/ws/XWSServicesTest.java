package org.decisiondeck.jmcda.xws.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.exc.InvalidInvocationException;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.xws.XWSExecutor;
import org.decisiondeck.jmcda.xws.transformer.InputTransformer;
import org.junit.Test;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XWSServicesTest {

	private static class LegacySources extends MapBasedSource implements FunctionWithInputCheck<String, ByteSource> {
		private final boolean m_checkInputs;

		public LegacySources() {
			m_checkInputs = true;
			setDefaultSources();
		}

		public LegacySources(boolean checkInputs) {
			m_checkInputs = checkInputs;
			setDefaultSources();
		}

		@Override
		public ByteSource apply(String input) throws InvalidInputException {
			checkArgument(!m_checkInputs || containsKey(input), "Invalid " + input + ".");
			return get(input);
		}

		private void setDefaultSources() {
			put("alternatives.xml",
					Resources.asByteSource(getClass().getResource("SixRealCars - All alternatives.xml")));
			put("criteria.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Criteria.xml")));
			put("performances_alternatives.xml",
					Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives performances.xml")));
			put("performances_profiles.xml",
					Resources.asByteSource(getClass().getResource("SixRealCars - Profiles performances.xml")));
			put("performances.xml",
					Resources.asByteSource(getClass().getResource("SixRealCars - Mixed performances.xml")));
			put("categories.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Categories.xml")));
			put("categories_profiles.xml",
					Resources.asByteSource(getClass().getResource("SixRealCars - Categories profiles.xml")));
			put("weights.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Weights.xml")));
			put("concordance.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Concordance.xml")));
			put("discordances.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Discordances.xml")));
		}
	}

	private static class SixRealCarsSource extends MapBasedSource
			implements FunctionWithInputCheck<String, ByteSource> {
		private final String m_fileName;

		public SixRealCarsSource() {
			m_fileName = "SixRealCars with criteriaValues.xml";
		}

		@Override
		public ByteSource apply(String input) throws InvalidInputException {
			if (containsKey(input)) {
				return get(input);
			}
			return Resources.asByteSource(getClass().getResource(m_fileName));
		}
	}

	@Test
	public void testWSConc() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new SixRealCarsSource();
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_concordance;
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getConcordance();
		assertTrue(computed.approxEquals(expected, 0.00005f));
	}

	@Test
	public void testWSConcEmpty() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new LegacySources(false);
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Empty.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_concordance;
		assertTrue(computed.isEmpty());
	}

	@Test
	public void testWSConcInvalidSources() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new SixRealCarsSource();
		/** No alternatives: not an error because is optional. */
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Criteria.xml")));
		nameToSource.put("performances.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives.xml")));
		nameToSource.put("weights.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Criteria.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("out_err"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		assertEquals("" + worker.m_exceptions, 2, worker.m_exceptions.size());
	}

	@Test
	public void testWSConcLegacyNoAlternatives() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new LegacySources(false);
		nameToSource.remove("alternatives.xml");
		nameToSource.put("performances.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives performances.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computedConcordance = worker.m_concordance;
		final SparseAlternativesMatrixFuzzy expectedConcordance = SixRealCars.getInstance().getConcordance();
		assertTrue(computedConcordance.approxEquals(expectedConcordance, 5e-5d));
	}

	@Test
	public void testWSConcLegacyRestrictedAlternatives() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new LegacySources(false);
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Three.xml")));
		nameToSource.put("criteria.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Criteria - Two.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computedConcordance = worker.m_concordance;
		final SparseAlternativesMatrixFuzzy expectedConcordance = SixRealCars.getInstance().getConcordanceRestricted();
		assertTrue(computedConcordance.approxEquals(expectedConcordance, 5e-5d));
	}

	@Test
	public void testWSConcMissingSource() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new LegacySources(false);
		nameToSource.remove("performances.xml");
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSConcordance.class);
		exec.setOutputDirectory(new File("out_err"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		assertEquals("" + worker.m_exceptions, 1, worker.m_exceptions.size());
	}

	@Test
	public void testWSCsvImport() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new SixRealCarsSource();
		nameToSource.put("performances.csv", Resources.asByteSource(getClass().getResource("SixRealCars.csv")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSCsvImport.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(true);
		exec.execute();

		final XWSCsvImport worker = (XWSCsvImport) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}

		final SixRealCars data = SixRealCars.getInstance();

		final Set<Alternative> outAlternatives = worker.m_alternatives;
		assertEquals(data.getAlternativesWithNiceNames(), outAlternatives);

		final Set<Criterion> outCriteria = worker.m_criteria;
		assertEquals(data.getCriteriaWithNiceNames(), outCriteria);

		final EvaluationsRead expectedEvaluations = data.getAlternativesEvaluations();
		final Evaluations expWithNiceNames = EvaluationsUtils.newEvaluationMatrix();
		for (Alternative alternative : expectedEvaluations.getRows()) {
			for (Criterion criterion : expectedEvaluations.getColumns()) {
				final double value = expectedEvaluations.getEntry(alternative, criterion).doubleValue();
				final String niceAlternativeName = data.getAlternativeNames().get(alternative);
				final String niceCriterionName = data.getCriteriaNames().get(criterion);
				expWithNiceNames.put(new Alternative(niceAlternativeName), new Criterion(niceCriterionName), value);
			}
		}
		assertTrue(expWithNiceNames.approxEquals(worker.m_evaluations, 1e-5));
	}

	@Test
	public void testWSCsvImportWithNoPerformances() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new SixRealCarsSource();
		nameToSource.put("performances.csv", Resources.asByteSource(getClass().getResource("SixRealCars - Empty.csv")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSCsvImport.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(true);
		exec.execute();

		final XWSCsvImport worker = (XWSCsvImport) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}

		final SixRealCars data = SixRealCars.getInstance();

		final Set<Alternative> outAlternatives = worker.m_alternatives;
		assertEquals(data.getAlternativesWithNiceNames(), outAlternatives);

		final Set<Criterion> outCriteria = worker.m_criteria;
		assertEquals(data.getCriteriaWithNiceNames(), outCriteria);

		final EvaluationsRead expectedEvaluations = data.getAlternativesEvaluations();
		final Evaluations expWithNiceNames = EvaluationsUtils.newEvaluationMatrix();
		for (Alternative alternative : expectedEvaluations.getRows()) {
			for (Criterion criterion : expectedEvaluations.getColumns()) {
				final double value = expectedEvaluations.getEntry(alternative, criterion).doubleValue();
				final String niceAlternativeName = data.getAlternativeNames().get(alternative);
				final String niceCriterionName = data.getCriteriaNames().get(criterion);
				expWithNiceNames.put(new Alternative(niceAlternativeName), new Criterion(niceCriterionName), value);
			}
		}
		assertNull(worker.m_evaluations);
	}

	@Test
	public void testWSCut() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("relation.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Outranking.xml")));
		nameToSource.put("cut_threshold.xml",
				Resources.asByteSource(getClass().getResource("DoubleParameter - 0.70.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSCutRelation.class);
		// exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSCutRelation worker = (XWSCutRelation) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_output;
		final SparseAlternativesMatrixFuzzy expectedOutranking = SixRealCars.getInstance().getOutrankingAtDotSeven();
		assertTrue("Relations do not match.", expectedOutranking.approxEquals(computed, 5e-5d));
	}

	@Test
	public void testWSCutFiltered() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Three.xml")));
		nameToSource.put("relation.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Outranking.xml")));
		nameToSource.put("cut_threshold.xml",
				Resources.asByteSource(getClass().getResource("DoubleParameter - 0.70.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSCutRelation.class);
		// exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSCutRelation worker = (XWSCutRelation) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_output;
		assertEquals(9, computed.getValueCount());
		// final IAltZeroToOneMatrix expectedOutranking =
		// SixRealCars.getInstance().getOutrankingAtDotSeven();
		// assertTrue("Relations do not match.",
		// expectedOutranking.approxEquals(computed, 5e-5d));
	}

	@Test
	public void testWSDisc() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		transformer.setNameToSource(new SixRealCarsSource());

		exec.setWorker(XWSDiscordances.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSDiscordances worker = (XWSDiscordances) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final Map<Criterion, SparseAlternativesMatrixFuzzy> computed = worker.m_discordances;
		final Map<Criterion, SparseAlternativesMatrixFuzzy> expected = SixRealCars.getInstance()
				.getDiscordanceByCriteria();
		assertEquals(expected.keySet(), computed.keySet());
		for (Criterion criterion : expected.keySet()) {
			assertTrue(computed.get(criterion).approxEquals(expected.get(criterion), 5e-5d));
		}
	}

	@Test
	public void testWSFlows() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("flow_type.xml",
				Resources.asByteSource(getClass().getResource("StringParameter - Positive flow.xml")));
		nameToSource.put("preference.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Preference.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSFlows.class);
		// exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSFlows worker = (XWSFlows) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final AlternativesScores computed = worker.m_flows;
		final AlternativesScores expected = SixRealCars.getInstance().getPositiveFlows();
		assertTrue("Results do not match.", expected.approxEquals(computed, 5e-5d));
	}

	@Test
	public void testWSFlowsFiltered() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Three.xml")));
		nameToSource.put("flow_type.xml",
				Resources.asByteSource(getClass().getResource("StringParameter - Positive flow.xml")));
		nameToSource.put("preference.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Preference.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSFlows.class);
		// exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSFlows worker = (XWSFlows) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final AlternativesScores computed = worker.m_flows;
		assertEquals(3, computed.size());
	}

	/**
	 * Filtered, but the filter has no effect as it contains all alternatives.
	 * 
	 * @throws Exception
	 *             exc
	 */
	@Test
	public void testWSFlowsFilteredNotReally() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives.xml")));
		nameToSource.put("flow_type.xml",
				Resources.asByteSource(getClass().getResource("StringParameter - Positive flow.xml")));
		nameToSource.put("preference.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Preference.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSFlows.class);
		// exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSFlows worker = (XWSFlows) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final AlternativesScores computed = worker.m_flows;
		final AlternativesScores expected = SixRealCars.getInstance().getPositiveFlows();
		assertTrue("Results do not match.", expected.approxEquals(computed, 5e-5d));
	}

	@Test
	public void testWSOutranking() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("concordance.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Concordance.xml")));
		nameToSource.put("discordances.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Discordances.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSOutranking.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSOutranking worker = (XWSOutranking) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_outranking;
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getOutranking();
		assertTrue(computed.approxEquals(expected, 0.00005f));
	}

	@Test
	public void testWSOutrankingFiltered() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final SixRealCarsSource nameToSource = new SixRealCarsSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Three.xml")));
		nameToSource.put("criteria.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Criteria - Two.xml")));
		nameToSource.put("concordance.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Concordance.xml")));
		nameToSource.put("discordances.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Discordances.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSOutranking.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSOutranking worker = (XWSOutranking) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_outranking;
		assertEquals(9, computed.getValueCount());
		// final IAltZeroToOneMatrix expected =
		// SixRealCars.getInstance().getOutranking();
		// assertTrue(computed.approxEquals(expected, 0.00005f));
	}

	@Test
	public void testWSOutrankingLegacy() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final LegacySources nameToSource = new LegacySources();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSOutranking.class);
		exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSOutranking worker = (XWSOutranking) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_outranking;
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getOutranking();
		assertTrue(computed.approxEquals(expected, 5e-5d));
	}

	@Test
	public void testWSPreference() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new SixRealCarsSource();
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSPreference.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSPreference worker = (XWSPreference) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final SparseMatrixFuzzy<Alternative, Alternative> computed = worker.m_preference;
		final SparseAlternativesMatrixFuzzy expected = SixRealCars.getInstance().getPreference();
		assertTrue(computed.approxEquals(expected, 0.00005f));
	}

	@Test
	public void testWSPrometheeProfiles() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		transformer.setNameToSource(new SixRealCarsSource());

		exec.setWorker(XWSPrometheeProfiles.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSPrometheeProfiles worker = (XWSPrometheeProfiles) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final Evaluations computed = worker.m_profiles;
		final Evaluations expected = SixRealCars.getInstance().getPrometheeProfiles();
		assertTrue(computed.approxEquals(expected, 5e-5d));
	}

	@Test
	public void testWSSorting() throws Exception {
		final SixRealCars sixRealCars = SixRealCars.getInstance();
		final String threshold55 = "DoubleParameter - 0.55.xml";
		final String modeBoth = "StringParameter - Sorting mode both.xml";
		// final String threshold70 = "DoubleParameter - 0.70.xml";
		final String modeOpt = "StringParameter - Sorting mode optimistic.xml";
		final String threshold75 = "DoubleParameter - 0.75.xml";
		final String modePess = "StringParameter - Sorting mode pessimistic.xml";
		testWSSorting(threshold55, modeBoth, sixRealCars.getAssignments55());
		testWSSorting(threshold55, modeOpt, sixRealCars.getAssignments55());
		testWSSorting(threshold55, modePess, sixRealCars.getAssignments55());
		testWSSorting(threshold75, modeBoth, sixRealCars.getAssignments75Both());
		testWSSorting(threshold75, modeOpt, sixRealCars.getAssignments75Optimistic());
		testWSSorting(threshold75, modePess, sixRealCars.getAssignments75Pessimistic());
	}

	@Test
	public void testWSSortingEmpty() throws Exception {
		final String threshold55 = "DoubleParameter - 0.55.xml";
		final String modeBoth = "StringParameter - Sorting mode both.xml";
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final SixRealCarsSource nameToSource = new SixRealCarsSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Alternatives - Empty.xml")));
		nameToSource.put("profiles.xml", Resources.asByteSource(getClass().getResource("SixRealCars - Profiles.xml")));
		nameToSource.put("performances_profiles.xml",
				Resources.asByteSource(getClass().getResource("SixRealCars - Profiles performances.xml")));
		nameToSource.put("cut_threshold.xml", Resources.asByteSource(getClass().getResource(threshold55)));
		nameToSource.put("sorting_mode.xml", Resources.asByteSource(getClass().getResource(modeBoth)));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSSorting.class);
		// exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSSorting worker = (XWSSorting) exec.getWorker();
		assertEquals(1, worker.m_exceptions.size());
	}

	@Test
	public void testWSSortingFromFile() throws Exception {
		final IOrderedAssignmentsToMultipleRead expected = SixRealCars.getInstance().getAssignments75Pessimistic();
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("criteria.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Assignments pessimistic, threshold 75.xml"));
		nameToSource.put("categories_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Assignments pessimistic, threshold 75.xml"));
		nameToSource.put("performances_alternatives.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Assignments pessimistic, threshold 75.xml"));
		nameToSource.put("performances_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Assignments pessimistic, threshold 75.xml"));
		nameToSource.put("weights.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Assignments pessimistic, threshold 75.xml"));

		nameToSource.put("cut_threshold.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("Parameters/DoubleParameter - 0.75.xml"));
		nameToSource.put("sorting_mode.xml", new XMCDAReadUtils()
				.getSampleAsInputSupplier("Parameters/StringParameter - Sorting mode pessimistic.xml"));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSSorting.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(true);
		exec.execute();

		final XWSSorting worker = (XWSSorting) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw new IllegalStateException(
					"Exceptions happened: " + worker.m_exceptions.size() + ", chaining the first one.",
					worker.m_exceptions.iterator().next());
		}
		final IOrderedAssignmentsToMultipleRead computed = worker.m_affectations;
		assertTrue(computed.equals(expected));
	}

	@Test
	public void testWSSortingLegacy() throws Exception {
		final IOrderedAssignmentsToMultipleRead expected = SixRealCars.getInstance().getAssignments55();
		final String threshold = "DoubleParameter - 0.55.xml";
		final String mode = "StringParameter - Sorting mode both.xml";
		testWSSortingLegacy(threshold, mode, expected);
	}

	@Test
	public void testWSSortingRestricted() throws Exception {
		final SixRealCars sixRealCars = SixRealCars.getInstance();
		final String threshold55 = "DoubleParameter - 0.55.xml";
		final String modeBoth = "StringParameter - Sorting mode both.xml";
		IOrderedAssignmentsRead expected = sixRealCars.getAssignments55();
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Alternatives - Three.xml"));
		nameToSource.put("criteria.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("categories_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("profiles.xml", new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Profiles.xml"));
		nameToSource.put("performances_alternatives.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("performances_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Profiles performances.xml"));
		nameToSource.put("weights.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("cut_threshold.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("Parameters/" + threshold55));
		nameToSource.put("sorting_mode.xml", new XMCDAReadUtils().getSampleAsInputSupplier("Parameters/" + modeBoth));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSSorting.class);
		// exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSSorting worker = (XWSSorting) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final IOrderedAssignmentsToMultipleRead computed = worker.m_affectations;
		final IOrderedAssignmentsToMultiple expectedRestricted = AssignmentsFactory.newOrderedAssignmentsToMultiple();
		AssignmentsUtils.copyOrderedAssignmentsToMultipleToTarget(expected, expectedRestricted);
		expectedRestricted.setCategories(new Alternative("a04"), null);
		expectedRestricted.setCategories(new Alternative("a05"), null);
		expectedRestricted.setCategories(new Alternative("a06"), null);
		assertEquals(3, computed.getAlternatives().size());
		AssignmentsUtils.assertEqual(expectedRestricted, computed, "exp", "comp");
		assertTrue(computed.equals(expectedRestricted));
	}

	private void testWSSorting(String thresholdFile, String modeFile, IOrderedAssignmentsToMultipleRead expected)
			throws InvalidInvocationException, IOException, InvalidInputException {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Alternatives.xml"));
		nameToSource.put("criteria.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("categories_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("profiles.xml", new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Profiles.xml"));
		nameToSource.put("performances_alternatives.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("performances_profiles.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/Profiles performances.xml"));
		nameToSource.put("weights.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("SixRealCars/SixRealCars with criteriaValues.xml"));
		nameToSource.put("cut_threshold.xml",
				new XMCDAReadUtils().getSampleAsInputSupplier("Parameters/" + thresholdFile));
		nameToSource.put("sorting_mode.xml", new XMCDAReadUtils().getSampleAsInputSupplier("Parameters/" + modeFile));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSSorting.class);
		// exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSSorting worker = (XWSSorting) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final IOrderedAssignmentsToMultipleRead computed = worker.m_affectations;
		AssignmentsUtils.assertEqual(expected, computed, "exp", "comp");
		assertTrue(computed.equals(expected));
	}

	private void testWSSortingLegacy(final String thresholdFile, final String modeFile,
			final IOrderedAssignmentsToMultipleRead expected)
			throws InvalidInvocationException, IOException, InvalidInputException {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final LegacySources nameToSource = new LegacySources(false);
		nameToSource.put("cut_threshold.xml", Resources.asByteSource(getClass().getResource(thresholdFile)));
		nameToSource.put("sorting_mode.xml", Resources.asByteSource(getClass().getResource(modeFile)));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSSorting.class);
		// exec.setOutputDirectory(new File("legacy"));
		exec.setWriteEnabled(false);
		exec.execute();

		final XWSSorting worker = (XWSSorting) exec.getWorker();
		if (worker.m_exceptions.size() >= 1) {
			throw worker.m_exceptions.iterator().next();
		}
		final IOrderedAssignmentsToMultipleRead computed = worker.m_affectations;
		assertTrue(computed.equals(expected));
	}

}
