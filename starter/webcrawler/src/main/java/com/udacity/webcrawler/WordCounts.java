package com.udacity.webcrawler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 * <p>TODO: Reimplement the sort() method using only the Stream API and lambdas and/or method
 *          references.
 */
final class WordCounts {

  /**
   * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
   * to the provided {@link WordCountComparator}, and includes only the top
   * {@param popluarWordCount} words and counts.
   *
   * <p>TODO: Reimplement this method using only the Stream API and lambdas and/or method
   *          references.
   *
   * @param wordCounts       the unsorted map of word counts.
   * @param popularWordCount the number of popular words to include in the result map.
   * @return a map containing the top {@param popularWordCount} words and counts in the right order.
   */
  static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

    // TODO: Reimplement this method using only the Stream API and lambdas and/or method references.
    Set<Map.Entry<String,Integer>> wordCountSet = wordCounts.entrySet();//把map的映射转化为set的元素,即Map.Entry
   /*
   在stream的collect步骤里,使用Collectors.toMap将元素转化为map,所以需要提供map的keyMapper和valueMapper作为参数,
   即键值对,因为元素是Map.Entry类型的，所以使用Map.Entry的方法引用

   If the stream is parallel, and the Collector is concurrent,
   and either the stream is unordered or the collector is unordered,
   then a concurrent reduction will be performed (see Collector for details on concurrent reduction.)

   toMap里的mergeFunction,即当collect到重复的key值时,处理values的函数,这里使用相加

   toMap方法在并发状态下不保证按照sorted方法排好序的映射在收集时仍保证顺序,
   所以可以用该方法的参数mapSupplier - a function which returns a new, empty Map into which the results will be inserted
   * */

    Map<String, Integer> topCounts =
            wordCountSet
                        .stream()
                        .sorted(new WordCountComparator())
                        .limit(Math.min(popularWordCount,wordCounts.size()))
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(kv1,kv2)->kv1+kv2,LinkedHashMap::new));
    return topCounts;
//    PriorityQueue<Map.Entry<String, Integer>> sortedCounts =
//        new PriorityQueue<>(wordCounts.size(), new WordCountComparator());
//    sortedCounts.addAll(wordCounts.entrySet());
//    Map<String, Integer> topCounts = new LinkedHashMap<>();
//    for (int i = 0; i < Math.min(popularWordCount, wordCounts.size()); i++) {
//      Map.Entry<String, Integer> entry = sortedCounts.poll();
//      topCounts.put(entry.getKey(), entry.getValue());
//    }
//    return topCounts;
  }

  /**
   * A {@link Comparator} that sorts word count pairs correctly:
   *
   * <p>
   * <ol>
   *   <li>First sorting by word count, ranking more frequent words higher.</li>
   *   <li>Then sorting by word length, ranking longer words higher.</li>
   *   <li>Finally, breaking ties using alphabetical order.</li>
   * </ol>
   */
  private static final class WordCountComparator implements Comparator<Map.Entry<String, Integer>> {
    @Override
    public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
      if (!a.getValue().equals(b.getValue())) {
        return b.getValue() - a.getValue();
      }
      if (a.getKey().length() != b.getKey().length()) {
        return b.getKey().length() - a.getKey().length();
      }
      return a.getKey().compareTo(b.getKey());
    }
  }

  private WordCounts() {
    // This class cannot be instantiated
  }
}