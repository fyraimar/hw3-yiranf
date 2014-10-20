package edu.cmu.lti.f14.hw3.hw3_yiranf.annotators;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_yiranf.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yiranf.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yiranf.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }
  }

  /**
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();

    Map<String, Token> tokens = new HashMap<String, Token>();

    String[] candidates = docText.split("\\s+");
    for (String candidate : candidates) {
      Token target = tokens.get(candidate);
      if (target == null) {
        target = new Token(jcas);
        target.setText(candidate);
        target.setFrequency(1);
        tokens.put(candidate, target);
        continue;
      }

      target.setFrequency(target.getFrequency() + 1);
    }

    Collection<Token> values = tokens.values();
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, values));
  }

}
