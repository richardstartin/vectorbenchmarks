package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.IntVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
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
    for (int i = 0; i < size; i += I256.length()) {
      IntVector.fromArray(I256, input, i)
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
