package edu.cmu.lti.f14.hw3.hw3_yiranf.casconsumers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_yiranf.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yiranf.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yiranf.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  public ArrayList<SentenceItem> mSentenceList;

  public ArrayList<String> mReportList;

  public int count;

  public double mMRR;
  
  public int mDocCount;
  
  public int mTokenCount;

  public class TokenLog implements Comparable<TokenLog> {
    String token = "";

    int noInQ = 0;

    int noInS = 0;

    public TokenLog(String ntoken, int nq, int ns) {
      token = ntoken;
      noInQ = nq;
      noInS = ns;
    }

    @Override
    public int compareTo(TokenLog o) {
      if (this.noInQ > o.noInQ)
        return 1;
      if (this.noInQ < o.noInQ)
        return 0;
      return 0;
    }
  }

  // Sorting the Sentence by qId and Cosine Similarity is not necessary in Task 1.
  // However, this implementation is mainly designed for the error analysis.
  public class SentenceItem implements Comparable<SentenceItem> {
    int qId = -1;

    int rel = -1;

    int rank = -1;

    String text = "";

    double mScore = 0.0;

    Map<String, Integer> tokenMap = null;

    List<TokenLog> tokenLogList = null;

    public SentenceItem(int nqId, int nrel, String ntext, Map<String, Integer> ntokenMap) {
      qId = nqId;
      rel = nrel;
      text = ntext;
      tokenMap = ntokenMap;
    }

    public void setScore(double ncs) {
      mScore = ncs;
    }

    public void setRank(int nrank) {
      rank = nrank;
    }

    public void setTokenLogList(List<TokenLog> nl) {
      tokenLogList = nl;
    }

    @Override
    public int compareTo(SentenceItem o) {
      // TODO Auto-generated method stub
      if (this.qId < o.qId)
        return -1;
      if (this.qId > o.qId)
        return 1;

      if (this.mScore > o.mScore)
        return -1;
      if (this.mScore < o.mScore)
        return 1;

      return 0;
    }
  }

  public void initialize() throws ResourceInitializationException {

    mSentenceList = new ArrayList<SentenceItem>();
    mReportList = new ArrayList<String>();
    mMRR = 0.0;
    mDocCount = 0;
    mTokenCount = 0;
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();
      // System.out.println(doc.getText());

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();

      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      Map<String, Integer> tokenMap = new HashMap<String, Integer>();
      for (Token tk : tokenList) {
        tokenMap.put(tk.getText(), tk.getFrequency());
      }

      SentenceItem sent = new SentenceItem(doc.getQueryID(), doc.getRelevanceValue(),
              doc.getText(), tokenMap);
      mSentenceList.add(sent);
    }
  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure
    Map<String, Integer> queryVector = null;
    Map<String, Integer> docVector = null;

    Iterator it = mSentenceList.iterator();
    while (it.hasNext()) {
      SentenceItem sent = (SentenceItem) it.next();

      if (sent.rel == 99) {
        queryVector = sent.tokenMap;
        sent.setScore(1.0);
      } else {
        docVector = sent.tokenMap;
        // sent.setScore(computeCosineSimilarity(sent, queryVector, docVector));
        // sent.setScore(computeJaccard(queryVector, docVector));
        // sent.setScore(computeDice(queryVector, docVector));
         sent.setScore(computeTversky(queryVector, docVector));
      }
    }

    // Compute the rank of retrieved sentences
    Collections.sort(mSentenceList);
    setSentenceListRank();

    Iterator iter = mSentenceList.iterator();
    while (iter.hasNext()) {
      SentenceItem sent = (SentenceItem) iter.next();

      System.out.println(sent.qId + " " + sent.rel + " " + sent.rank + " " + sent.mScore);
    }

    // Compute the metric:: mean reciprocal rank
    mMRR = compute_mrr();

    printReport();
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(SentenceItem si, Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    double sum = 0.0;
    double lenQuery = 0.0;
    double lenDoc = 0.0;

    // TODO :: compute cosine similarity between two sentences
    Iterator it = queryVector.entrySet().iterator();
    List<TokenLog> tll = new ArrayList<TokenLog>();
    while (it.hasNext()) {
      Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>) it.next();
      lenQuery += pairs.getValue() * pairs.getValue();

      if (docVector.get(pairs.getKey()) == null)
        continue;

      tll.add(new TokenLog(pairs.getKey(), pairs.getValue(), docVector.get(pairs.getKey())));
      sum += docVector.get(pairs.getKey()) * pairs.getValue();
    }

    Collections.sort(tll);
    si.setTokenLogList(tll);

    it = docVector.values().iterator();
    while (it.hasNext()) {
      int val = (Integer) it.next();
      lenDoc += Math.pow(val, 2);
    }

    cosine_similarity = sum / (Math.sqrt(lenDoc) * Math.sqrt(lenQuery));

    System.out.println(cosine_similarity);

    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    double sum = 0.0;
    int totalQuery = 0;

    Iterator iter = mSentenceList.iterator();
    while (iter.hasNext()) {
      SentenceItem sent = (SentenceItem) iter.next();

      if (sent.rel == 99) {
        totalQuery++;
        continue;
      }

      if (sent.rel == 0)
        continue;

      if (sent.rel == 1)
        sum += 1.0 / sent.rank;
    }

    metric_mrr = sum / totalQuery;
    System.out.println("MMR: " + metric_mrr);
    return metric_mrr;
  }

  private void setSentenceListRank() {
    int rank = 0;

    Iterator it = mSentenceList.iterator();
    while (it.hasNext()) {
      SentenceItem sent = (SentenceItem) it.next();

      if (sent.rel == 99) {
        mReportList.add("\n\n");
        sent.setRank(0);
        String toReport = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s\n", sent.mScore,
                sent.rank, sent.qId, sent.rel, sent.text);
        mReportList.add(toReport);
        rank = 1;
        continue;
      }

      sent.setRank(rank++);
      String toReport = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s\n", sent.mScore,
              sent.rank, sent.qId, sent.rel, sent.text);
      mReportList.add(toReport);

      /*
       * mReportList.add(String.format("%-20s%-10s%-10s\n", "TokenName", "NoInQ", "NoInS")); for
       * (TokenLog tl : sent.tokenLogList) { mReportList.add(String.format("%-20s%-10d%-10d\n",
       * tl.token, tl.noInQ, tl.noInS)); }
       */
    }
  }

  private double computeJaccard(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
    double ret = 0.0;

    Set<String> qv = queryVector.keySet();
    Set<String> dv = docVector.keySet();

    int M11 = 0;
    int M10 = 0;
    int M01 = 0;

    Iterator it = qv.iterator();
    while (it.hasNext()) {
      String st = (String) it.next();

      if (dv.contains(st) == false)
        M10++;
      else
        M11++;
    }

    it = dv.iterator();
    while (it.hasNext()) {
      String st = (String) it.next();

      if (qv.contains(st) == false)
        M01++;
    }

    ret = (double) M11 / (M01 + M10 + M11);
    return ret;
  }

  private double computeDice(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
    double ret = 0.0;

    Set<String> qv = queryVector.keySet();
    Set<String> dv = docVector.keySet();

    int M11 = 0;

    Iterator it = qv.iterator();
    while (it.hasNext()) {
      String st = (String) it.next();

      if (dv.contains(st) == true)
        M11++;
    }

    ret = (double) (2 * M11) / (qv.size() + dv.size());
    return ret;
  }

  private double computeTversky(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
    double ret = 0.0;

    Set<String> qv = queryVector.keySet();
    Set<String> dv = docVector.keySet();
    
    double a = 0.2;
    double b = 1 - a;

    int M11 = 0;
    int M10 = 0;
    int M01 = 0;

    Iterator it = qv.iterator();
    while (it.hasNext()) {
      String st = (String) it.next();

      if (dv.contains(st) == false)
        M10++;
      else
        M11++;
    }

    it = dv.iterator();
    while (it.hasNext()) {
      String st = (String) it.next();

      if (qv.contains(st) == false)
        M01++;
    }

    ret = (double) M11 / (a * M10 + b * M01 + M11);
    return ret;
  }
  
  private double computeBM25(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
    double ret = 0.0;
    return ret;
  }

  private void printReport() {
    try {
      FileWriter fw = new FileWriter("task2_report.txt", false);
      for (String line : mReportList) {
        fw.write(line);
      }
      fw.write(String.format("MRR=%.4f\n", mMRR));
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
