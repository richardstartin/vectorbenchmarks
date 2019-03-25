package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.FloatVector;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.F256;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {
        "--add-modules=jdk.incubator.vector",
        "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"
})
public class PostLoops {

  @State(Scope.Benchmark)
  public static class DotProductState {
    @Param({"1000", "1010", "1024", "1030", "1040"})
    int size;

    float[] left;
    float[] right;
    float[] buffer = new float[4 * F256.length()];

    @Setup(Level.Trial)
    public void init() {
      left = Util.newFloatVector(size);
      right = Util.newFloatVector(size);
    }

  }

  @Benchmark
  public float scalarPostLoopDotProduct(DotProductState state) {
    float[] left = state.left;
    float[] right = state.right;
    int limit = state.size & -(4 * F256.length());
    return mainLoop(limit, left, right)
            + scalarPostLoop(limit, left, right);
  }

  @Benchmark
  public float paddedPostLoopDotProduct(DotProductState state) {
    float[] left = state.left;
    float[] right = state.right;
    float[] buffer = state.buffer;
    int limit = state.size & -(4 * F256.length());
    return mainLoop(limit, left, right)
            + paddedPostLoop(limit, left, right, buffer);
  }

  @Benchmark
  public float fusedPaddedPostLoopDotProduct(DotProductState state) {
    float[] left = state.left;
    float[] right = state.right;
    float[] buffer = state.buffer;
    var sum1 = F256.zero();
    var sum2 = F256.zero();
    var sum3 = F256.zero();
    var sum4 = F256.zero();
    int width = F256.length();
    int limit = state.size & -(4 * F256.length());
    for (int i = 0; i < limit; i += width * 4) {
      sum1 = FloatVector.fromArray(F256, left, i).fma(FloatVector.fromArray(F256, right, i), sum1);
      sum2 = FloatVector.fromArray(F256, left, i + width).fma(FloatVector.fromArray(F256, right, i + width), sum2);
      sum3 = FloatVector.fromArray(F256, left, i + width * 2).fma(FloatVector.fromArray(F256, right, i + width * 2), sum3);
      sum4 = FloatVector.fromArray(F256, left, i + width * 3).fma(FloatVector.fromArray(F256, right, i + width * 3), sum4);
    }
    System.arraycopy(left, 0, buffer, 0, left.length - limit);
    var l1 = FloatVector.fromArray(F256, buffer, 0);
    var l2 = FloatVector.fromArray(F256, buffer, F256.length());
    var l3 = FloatVector.fromArray(F256, buffer, F256.length() * 2);
    var l4 = FloatVector.fromArray(F256, buffer, F256.length() * 3);
    System.arraycopy(right, 0, buffer, 0, right.length - limit);
    var r1 = FloatVector.fromArray(F256, buffer, 0);
    var r2 = FloatVector.fromArray(F256, buffer, F256.length());
    var r3 = FloatVector.fromArray(F256, buffer, F256.length() * 2);
    var r4 = FloatVector.fromArray(F256, buffer, F256.length() * 3);
    l1 = l1.fma(r1, l1);
    l2 = l2.fma(r2, l2);
    l3 = l3.fma(r3, l3);
    l4 = l4.fma(r4, l4);
    F256.zero().intoArray(buffer, 0);
    F256.zero().intoArray(buffer, F256.length());
    F256.zero().intoArray(buffer, F256.length() * 2);
    F256.zero().intoArray(buffer, F256.length() * 3);
    return sum1.add(l1).add(sum2.add(l2)).add(sum3.add(l3).add(sum4.add(l4))).addAll();
  }

  private float mainLoop(int size, float[] left, float[] right) {
    var sum1 = F256.zero();
    var sum2 = F256.zero();
    var sum3 = F256.zero();
    var sum4 = F256.zero();
    int width = F256.length();
    for (int i = 0; i < size; i += width * 4) {
      sum1 = FloatVector.fromArray(F256, left, i).fma(FloatVector.fromArray(F256, right, i), sum1);
      sum2 = FloatVector.fromArray(F256, left, i + width).fma(FloatVector.fromArray(F256, right, i + width), sum2);
      sum3 = FloatVector.fromArray(F256, left, i + width * 2).fma(FloatVector.fromArray(F256, right, i + width * 2), sum3);
      sum4 = FloatVector.fromArray(F256, left, i + width * 3).fma(FloatVector.fromArray(F256, right, i + width * 3), sum4);
    }
    return sum1.add(sum2).add(sum3.add(sum4)).addAll();
  }

  private float scalarPostLoop(int start, float[] left, float[] right) {
    float sum = 0;
    for (int i = start; i < Math.min(left.length, right.length); ++i) {
      sum += left[i] * right[i];
    }
    return sum;
  }

  private float paddedPostLoop(int start, float[] left, float[] right, float[] buffer) {
    System.arraycopy(left, 0, buffer, 0, left.length - start);
    var l1 = FloatVector.fromArray(F256, buffer, 0);
    var l2 = FloatVector.fromArray(F256, buffer, F256.length());
    var l3 = FloatVector.fromArray(F256, buffer, F256.length() * 2);
    var l4 = FloatVector.fromArray(F256, buffer, F256.length() * 3);
    System.arraycopy(right, 0, buffer, 0, right.length - start);
    var r1 = FloatVector.fromArray(F256, buffer, 0);
    var r2 = FloatVector.fromArray(F256, buffer, F256.length());
    var r3 = FloatVector.fromArray(F256, buffer, F256.length() * 2);
    var r4 = FloatVector.fromArray(F256, buffer, F256.length() * 3);
    l1 = l1.fma(r1, l1);
    l2 = l2.fma(r2, l2);
    l3 = l3.fma(r3, l3);
    l4 = l4.fma(r4, l4);
    F256.zero().intoArray(buffer, 0);
    F256.zero().intoArray(buffer, F256.length());
    F256.zero().intoArray(buffer, F256.length() * 2);
    F256.zero().intoArray(buffer, F256.length() * 3);
    return l1.add(l2).add(l3.add(l4)).addAll();
  }
}
