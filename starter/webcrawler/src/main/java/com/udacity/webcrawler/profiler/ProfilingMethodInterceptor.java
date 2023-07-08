package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState profilingState;
  private final Object target;//目标对象

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, ProfilingState profilingState, Object target) {
    this.clock = Objects.requireNonNull(clock);
    this.profilingState = Objects.requireNonNull(profilingState);
    this.target = Objects.requireNonNull(target);
  }
  public Boolean ifMethodProfiled(Method method){
    if(method.getAnnotation(Profiled.class)==null) {
      return false;
    }
    return true;
  }
  //invoke是与代理对象解耦的增强代码,当前的proxy调用方法时,就会将proxy,该method,方法参数 传递给invoke方法,对该方法进行补充
  //但需要在此invoke中使用反射执行该method,为method.invoke(target,args)
  //在本项目中,即为分析调用到该方法执行的时间
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    Object result = null;
    //如果该方法不需要profile,则按照目标对象执行方法
    Boolean isProfiled =ifMethodProfiled(method);
    Instant start = null;
    if(isProfiled){
      start = clock.instant();//开始计时
    }

    try{
      result = method.invoke(target,args);//args为该接口方法method的参数列表,基本数据类型都为primitive wrapper class
    }catch (Throwable t){
      throw t.getCause();//抛出目标对象的异常
    }finally {
      if(isProfiled){
        Duration duration = Duration.between(start,clock.instant());
        profilingState.record(target.getClass(),method,duration);//目标对象,被统计的方法,方法执行的时间
      }
    }
    return result;
  }
}
