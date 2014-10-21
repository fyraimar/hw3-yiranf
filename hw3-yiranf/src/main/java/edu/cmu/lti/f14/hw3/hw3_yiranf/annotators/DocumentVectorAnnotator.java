package edu.cmu.lti.f14.hw3.hw3_yiranf.annotators;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.internal.util.TextTokenizer;
import org.apache.uima.jcas.JCas;
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
      
      //createTermFreqVector(jcas, doc);
      luceneCreateTermFreqVector(jcas, doc);
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

  private void luceneCreateTermFreqVector(JCas jcas, Document doc) {
    
    StringReader reader = new StringReader(doc.getText());
    StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_36, reader);
    
    CharTermAttribute charTermAttrib = tokenizer.getAttribute(CharTermAttribute.class);
    
    Map<String, Token> tokens = new HashMap<String, Token>();
    try {
      tokenizer.reset();
      
      while (tokenizer.incrementToken()) {
        Token target = tokens.get(charTermAttrib.toString());
        if (target == null) {
          target = new Token(jcas);
          target.setText(charTermAttrib.toString());
          target.setFrequency(1);
          tokens.put(charTermAttrib.toString(), target);
          continue;
        }

        target.setFrequency(target.getFrequency() + 1);
      }
      
      tokenizer.end();
      tokenizer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    Collection<Token> values = tokens.values();
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, values));
  }

}
