package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Shapes;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_FLOAT;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=121"})
public class DotProduct {

  @Param({"1024", "65536"})
  int size;

  private float[] left;
  private float[] right;


  @Setup(Level.Iteration)
  public void init() {
    this.left = newVector(size);
    this.right = newVector(size);
  }

  @Benchmark
  public float dpPanama() {
    FloatVector<Shapes.S256Bit> sum = YMM_FLOAT.zero();
    for (int i = 0; i < size; i += YMM_FLOAT.length()) {
      FloatVector<Shapes.S256Bit> l = YMM_FLOAT.fromArray(left, i);
      FloatVector<Shapes.S256Bit> r = YMM_FLOAT.fromArray(right, i);
      sum = sum.add(l.mul(r));
    }
    return sum.addAll();
  }

  @Benchmark
  public float dpUnrolled() {
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

  private static float[] newVector(int size) {
    float[] matrix = new float[size];
    for (int i = 0; i < matrix.length; ++i) {
      matrix[i] = ThreadLocalRandom.current().nextFloat();
    }
    return matrix;
  }
}
