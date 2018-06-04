package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_FLOAT;
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
          "1024",
          "2048",
          "3072",
          "4096",
          "6144",
          "7168",
          "8192",
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
    var sum = YMM_FLOAT.zero();
    for (int i = 0; i < size; i += YMM_FLOAT.length()) {
      var l = YMM_FLOAT.fromArray(left, i);
      var r = YMM_FLOAT.fromArray(right, i);
      sum = l.fma(r, sum);
    }
    return sum.addAll();
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
    var sum1 = YMM_FLOAT.zero();
    var sum2 = YMM_FLOAT.zero();
    var sum3 = YMM_FLOAT.zero();
    var sum4 = YMM_FLOAT.zero();
    int width = YMM_FLOAT.length();
    for (int i = 0; i < size; i += width * 4) {
      sum1 = sum1.add(YMM_FLOAT.fromArray(left, i).mul(YMM_FLOAT.fromArray(right, i)));
      sum2 = sum2.add(YMM_FLOAT.fromArray(left, i + width).mul(YMM_FLOAT.fromArray(right, i + width)));
      sum3 = sum3.add(YMM_FLOAT.fromArray(left, i + width * 2).mul(YMM_FLOAT.fromArray(right, i + width * 2)));
      sum4 = sum4.add(YMM_FLOAT.fromArray(left, i + width * 3).mul(YMM_FLOAT.fromArray(right, i + width * 3)));
    }
    return sum1.addAll() + sum2.addAll() + sum3.addAll() + sum4.addAll();
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
      s0 += left[i] * right[i];
      s1 += left[i + 1] * right[i + 1];
      s2 += left[i + 2] * right[i + 2];
      s3 += left[i + 3] * right[i + 3];
      s4 += left[i + 4] * right[i + 4];
      s5 += left[i + 5] * right[i + 5];
      s6 += left[i + 6] * right[i + 6];
      s7 += left[i + 7] * right[i + 7];
    }
    return s0 + s1 + s2 + s3 + s4 + s5 + s6 + s7;
  }

  @Benchmark
  public float vanilla() {
    float sum = 0f;
    for (int i = 0; i < size; ++i) {
      sum += left[i] * right[i];
    }
    return sum;
  }

}
