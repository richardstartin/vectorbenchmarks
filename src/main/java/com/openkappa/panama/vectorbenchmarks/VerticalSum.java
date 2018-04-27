package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_FLOAT;
import static com.openkappa.panama.vectorbenchmarks.Util.newFloatVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=121"})
public class VerticalSum {

  @Param({"1024", "65536"})
  int size;

  private float[] left;
  private float[] right;
  private float[] result;


  @Setup(Level.Iteration)
  public void init() {
    this.left = newFloatVector(size);
    this.right = newFloatVector(size);
    this.result = newFloatVector(size);
  }

  @Benchmark
  public void verticalSumPanama(Blackhole bh) {
    for (int i = 0; i < size; i += YMM_FLOAT.length()) {
      YMM_FLOAT.fromArray(left, i).add(YMM_FLOAT.fromArray(right, i)).intoArray(result, i);
    }
    bh.consume(result);
  }

  @Benchmark
  public void verticalSumAutoVectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] + right[i];
    }
    bh.consume(result);
  }
}
