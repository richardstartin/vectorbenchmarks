package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.FloatVector;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.F256;
import static com.openkappa.panama.vectorbenchmarks.Util.newFloatVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:TypeProfileLevel=111",
        "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"
})
public class DotProduct {

  @Param({
          "128",
          "256",
          "512",
          "1024",
          "2048",
          "3072",
          "4064",
          "4096",
          "4128",
          "6144",
          "7168",
          "8192",
          "8192",
          "16384",
          "32768",
          "65536"
  })
  int size;

  private float[] left;
  private float[] right;

  @Setup(Level.Trial)
  public void init() {
    this.left = newFloatVector(size);
    this.right = newFloatVector(size);
  }

  @Benchmark
  public float vector() {
    var sum = FloatVector.zero(F256);
    for (int i = 0; i < size; i += F256.length()) {
      var l = FloatVector.fromArray(F256, left, i);
      var r = FloatVector.fromArray(F256, right, i);
      sum = l.fma(r, sum);
    }
    return sum.addLanes();
  }

  @Benchmark
  @Threads(1)
  public float vectorUnrolled1() {
    return vectorUnrolled();
  }

  @Benchmark
  @Threads(2)
  public float vectorUnrolled2() {
    return vectorUnrolled();
  }

  @Benchmark
  @Threads(4)
  public float vectorUnrolled4() {
    return vectorUnrolled();
  }

  @Benchmark
  @Threads(8)
  public float vectorUnrolled8() {
    return vectorUnrolled();
  }

  private float vectorUnrolled() {
    var sum1 = FloatVector.zero(F256);
    var sum2 = FloatVector.zero(F256);
    var sum3 = FloatVector.zero(F256);
    var sum4 = FloatVector.zero(F256);
    int width = F256.length();
    for (int i = 0; i < size; i += width * 4) {
      sum1 = FloatVector.fromArray(F256, left, i).fma(FloatVector.fromArray(F256, right, i), sum1);
      sum2 = FloatVector.fromArray(F256, left, i + width).fma(FloatVector.fromArray(F256, right, i + width), sum2);
      sum3 = FloatVector.fromArray(F256, left, i + width * 2).fma(FloatVector.fromArray(F256, right, i + width * 2), sum3);
      sum4 = FloatVector.fromArray(F256, left, i + width * 3).fma(FloatVector.fromArray(F256, right, i + width * 3), sum4);
    }
    return sum1.addLanes() + sum2.addLanes() + sum3.addLanes() + sum4.addLanes();
  }

  @Benchmark
  public float unrolled() {
    float s0 = 0f;
    float s1 = 0f;
    float s2 = 0f;
    float s3 = 0f;
    float s4 = 0f;
    float s5 = 0f;
    float s6 = 0f;
    float s7 = 0f;
    for (int i = 0; i < size; i += 8) {
      s0 = Math.fma(left[i + 0],  right[i + 0], s0);
      s1 = Math.fma(left[i + 1],  right[i + 1], s1);
      s2 = Math.fma(left[i + 2],  right[i + 2], s2);
      s3 = Math.fma(left[i + 3],  right[i + 3], s3);
      s4 = Math.fma(left[i + 4],  right[i + 4], s4);
      s5 = Math.fma(left[i + 5],  right[i + 5], s5);
      s6 = Math.fma(left[i + 6],  right[i + 6], s6);
      s7 = Math.fma(left[i + 7],  right[i + 7], s7);
    }
    return s0 + s1 + s2 + s3 + s4 + s5 + s6 + s7;
  }

  @Benchmark
  public float vanilla() {
    float sum = 0f;
    for (int i = 0; i < size; ++i) {
      sum = Math.fma(left[i], right[i], sum);
    }
    return sum;
  }

}
