package com.udacity.webcrawler.profiler;


import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  //invocationHandler什么时候被实例化了?为什么可以确定目标对象传参到handler?
  //wrap方法与invocationHandler耦合,在创建代理对象时,实例化handler,handler需要目标对象,handler独立扩展增强代码
  //但是属于代理对象,Proxy中有invocationHandler的field
  //如果invocationHandler为依赖注入,没有意义
  //只有clock需要依赖注入
  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  //判断目标对象有没有需要统计的方法
  public <T> Boolean ifClassProfiled(Class<T> klass){
    Method[] methods = klass.getDeclaredMethods();
    if(methods.length==0) return false;
    for(Method m:methods){
      if(m.getAnnotation(Profiled.class)!=null) return true;
    }
    return false;
  }

  @Override //wrap也可以写在其他的类里,不一定要写在profile实现类里
  public <T> T wrap(Class<T> klass, T delegate){ //通过传参,对需要统计的object进行动态代理
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
    if(!ifClassProfiled(klass)){
      throw new IllegalArgumentException("no profiled method in the class");
    }
    InvocationHandler interceptor = new ProfilingMethodInterceptor(clock,state,delegate);
    @SuppressWarnings("unchecked")
    T proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(),new Class<?>[]{klass},interceptor);//interface参数为proxy实现的接口class对象
    return proxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    try(//try-resource
      Writer writer = Files.newBufferedWriter(path,
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              StandardOpenOption.APPEND)){

        state.write(writer);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
