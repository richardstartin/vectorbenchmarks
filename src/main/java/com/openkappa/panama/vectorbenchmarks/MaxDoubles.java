package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.DoubleVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.D256;
import static com.openkappa.panama.vectorbenchmarks.Util.newDoubleVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class MaxDoubles {

  private double[] input;
  private double[] output;

  @Param({"1024", "65536"})
  int size;

  @Setup(Level.Iteration)
  public void init() {
    input = newDoubleVector(size);
    output = new double[size];
  }


  @Benchmark
  public void zeroNegatives(Blackhole bh) {
    for (int i = 0; i < size; i += D256.length()) {
      DoubleVector.fromArray(D256, input, i)
             .max(0D)
             .intoArray(output, i);
    }
    bh.consume(output);
  }

  @Benchmark
  public void zeroNegativesScalar(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      output[i] = Math.max(input[i], 0D);
    }
    bh.consume(output);
  }

}
