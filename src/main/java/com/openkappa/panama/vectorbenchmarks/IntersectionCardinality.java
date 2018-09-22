package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_LONG;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class IntersectionCardinality {


  @Param({"1024"})
  int size;

  private final long[] left = new long[1024];
  private final long[] right = new long[1024];
  private long[] buffer = new long[64];

  @Setup(Level.Iteration)
  public void fill() {
    Arrays.fill(left, 0, size, ThreadLocalRandom.current().nextLong());
    Arrays.fill(right, 0, size, ThreadLocalRandom.current().nextLong());
  }

  @Benchmark
  public int vanilla() {
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; ++i) {
      cardinality += Long.bitCount(left[i] & right[i]);
    }
    return cardinality;
  }

  @Benchmark
  public int unrolled() {
    int cardinality1 = 0;
    int cardinality2 = 0;
    int cardinality3 = 0;
    int cardinality4 = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 4) {
      cardinality1 += Long.bitCount(left[i+0] & right[i+0]);
      cardinality2 += Long.bitCount(left[i+1] & right[i+1]);
      cardinality3 += Long.bitCount(left[i+2] & right[i+2]);
      cardinality4 += Long.bitCount(left[i+3] & right[i+3]);
    }
    return cardinality1 + cardinality2 + cardinality3 + cardinality4;
  }

  @Benchmark
  public int mixed() {
    long[] intersections = buffer;
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 4) {
      YMM_LONG.fromArray(left, i).and(YMM_LONG.fromArray(right, i)).intoArray(intersections, 0);
      cardinality += Long.bitCount(intersections[0]);
      cardinality += Long.bitCount(intersections[1]);
      cardinality += Long.bitCount(intersections[2]);
      cardinality += Long.bitCount(intersections[3]);
    }
    return cardinality;
  }


  @Benchmark
  public int mixedUnrolled() {
    long[] intersections = buffer;
    int cardinality1 = 0;
    int cardinality2 = 0;
    int cardinality3 = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 8) {
      YMM_LONG.fromArray(left, i).and(YMM_LONG.fromArray(right, i)).intoArray(intersections, 0);
      YMM_LONG.fromArray(left, i + 4).and(YMM_LONG.fromArray(right, i + 4)).intoArray(intersections, 4);
      cardinality1 += Long.bitCount(intersections[0]);
      cardinality2 += Long.bitCount(intersections[1]);
      cardinality3 += Long.bitCount(intersections[2]);
      cardinality1 += Long.bitCount(intersections[3]);
      cardinality2 += Long.bitCount(intersections[4]);
      cardinality3 += Long.bitCount(intersections[5]);
      cardinality1 += Long.bitCount(intersections[6]);
      cardinality2 += Long.bitCount(intersections[7]);
    }
    return cardinality1 + cardinality2 + cardinality3;
  }


  @Benchmark
  public int mixedStagedLoop() {
    long[] intersections = buffer;
    int cardinality = 0;
    for (int i = 0; i < size && i < left.length && i < right.length; i += 64) {
      for (int j = 0; j < 64; j += 4) {
        YMM_LONG.fromArray(left, i + j).and(YMM_LONG.fromArray(right, i + j)).intoArray(intersections, j);
      }
      for (int j = 0; j < 64; ++j) {
        cardinality += Long.bitCount(intersections[j]);
      }
    }
    return cardinality;
  }


}
