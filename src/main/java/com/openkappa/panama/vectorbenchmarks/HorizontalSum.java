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
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class HorizontalSum {

  @Param({"1024", "65536"})
  int size;

  private float[] data;


  @Setup(Level.Iteration)
  public void init() {
    this.data = newVector(size);
  }

  @Benchmark
  public float horizontalSumPanama() {
    FloatVector<Shapes.S256Bit> sum = YMM_FLOAT.zero();
    for (int i = 0; i < size; i += YMM_FLOAT.length()) {
      FloatVector<Shapes.S256Bit> l = YMM_FLOAT.fromArray(data, i);
      sum = sum.add(l);
    }
    return sum.addAll();
  }

  @Benchmark
  public float horizontalSumUnrolled() {
    float s0 = 0f;
    float s1 = 0f;
    float s2 = 0f;
    float s3 = 0f;
    float s4 = 0f;
    float s5 = 0f;
    float s6 = 0f;
    float s7 = 0f;
    for (int i = 0; i < size; i += 8) {
      s0 += data[i];
      s1 += data[i + 1];
      s2 += data[i + 2];
      s3 += data[i + 3];
      s4 += data[i + 4];
      s5 += data[i + 5];
      s6 += data[i + 6];
      s7 += data[i + 7];
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
