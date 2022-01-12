package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final ForkJoinPool pool;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          PageParserFactory parserFactory,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @TargetParallelism int threadCount) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }


  public final class CrawlRecursiveAction extends RecursiveAction{
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;

    public CrawlRecursiveAction(
            String url,
            Instant deadline,
            int maxDepth,
            Map<String, Integer> counts,
            Set<String> visitedUrls) {
      this.url = url;
      this.deadline = deadline;
      this.maxDepth = maxDepth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {
      if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
        return;
      }
      for (Pattern pattern : ignoredUrls) {
        if (pattern.matcher(url).matches()) {
          return;
        }
      }
      if (visitedUrls.contains(url)) {
        return;
      }
      visitedUrls.add(url);
      //拿到当前url的result
      PageParser.Result result = parserFactory.get(url).parse();
      for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
        if (counts.containsKey(e.getKey())) {
          counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
        } else {
          counts.put(e.getKey(), e.getValue());
        }
      }
      /*
      *compute方法,参数为k与二元函数,重新映射，remapping function,
      * 如果在已有的counts里，没有当前url的wordCounts结果里的一组映射的k,则添加该k,v
      * 如果有该k,则更新该已有的映射的v值
      */
//      for(ConcurrentHashMap.Entry<String, Integer> e : result.getWordCounts().entrySet()){
//        counts.compute(e.getKey(),(k,v)->(v==null)?e.getValue():v+e.getValue());
//      }
      List<CrawlRecursiveAction> actions = new ArrayList<>();
      for(String url : result.getLinks()){
        actions.add(new CrawlRecursiveAction(url,deadline,maxDepth-1,counts,visitedUrls));
      }
      invokeAll(actions);
    }

  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> counts = new ConcurrentHashMap<>();
//  Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    Set<String> visitedUrls = new HashSet<>();
    for(String url:startingUrls){//forkjoinpool执行
      pool.invoke(new CrawlRecursiveAction(url,deadline,maxDepth,counts,visitedUrls));
    }
    //并行执行结束后
    if(counts.isEmpty()){
      return new CrawlResult
              .Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }
    return new CrawlResult
            .Builder()
            .setWordCounts(WordCounts.sort(counts,popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
