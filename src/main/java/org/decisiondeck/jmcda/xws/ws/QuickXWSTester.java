package org.decisiondeck.jmcda.xws.ws;

import java.io.File;

import org.decisiondeck.jmcda.xws.XWSExecutor;
import org.decisiondeck.jmcda.xws.transformer.InputTransformer;

import com.google.common.io.Resources;

public class QuickXWSTester {

	public static void main(String[] args) throws Exception {
		new QuickXWSTester().proceed();
	}

	private void proceed() throws Exception {
		final XWSExecutor exec = new XWSExecutor();
		final InputTransformer transformer = exec.getInputTransformer();
		final MapBasedSource nameToSource = new MapBasedSource();
		nameToSource.put("alternatives.xml",
				Resources.asByteSource(getClass().getResource("/potentialAlternatives.xml")));
		nameToSource.put("preference.xml", Resources.asByteSource(getClass().getResource("/preference.xml")));
		transformer.setNameToSource(nameToSource);
		nameToSource.put("flow_type.xml",
				Resources.asByteSource(getClass().getResource("/StringParameter - Net flow.xml")));
		transformer.setNameToSource(nameToSource);

		exec.setWorker(XWSFlows.class);
		exec.setOutputDirectory(new File("out"));
		exec.setWriteEnabled(true);
		exec.getOutputTransformer().setValidate(false);
		exec.execute();

		// final XWSConcordance worker = (XWSConcordance) exec.getWorker();
		// if (worker.m_exceptions.size() >= 1) {
		// throw worker.m_exceptions.iterator().next();
		// }
	}

}
