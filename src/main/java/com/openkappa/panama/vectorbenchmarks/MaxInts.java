package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class MaxInts {

  private int[] input;
  private int[] output;

  @Param({"1024", "65536"})
  int size;

  @Setup(Level.Iteration)
  public void init() {
    input = newIntVector(size);
    output = new int[size];
  }


  @Benchmark
  public void zeroNegatives(Blackhole bh) {
    for (int i = 0; i < size; i += YMM_INT.length()) {
      YMM_INT.fromArray(input, i)
              .max(0)
              .intoArray(output, i);
    }
    bh.consume(output);
  }

  @Benchmark
  public void zeroNegativesScalar(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      output[i] = Math.max(input[i], 0);
    }
    bh.consume(output);
  }

}
