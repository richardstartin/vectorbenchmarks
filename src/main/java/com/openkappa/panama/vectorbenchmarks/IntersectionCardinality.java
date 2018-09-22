package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_LONG;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
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
    for (int i = 0; i + 3 < size && i + 3 < left.length && i + 3 < right.length; i += 4) {
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
    for (int i = 0; i + 3 < size && i + 3 < left.length && i + 3 < right.length; i += 4) {
      YMM_LONG.fromArray(left, i).and(YMM_LONG.fromArray(right, i)).intoArray(intersections, 0);
      cardinality += Long.bitCount(intersections[0]);
      cardinality += Long.bitCount(intersections[1]);
      cardinality += Long.bitCount(intersections[2]);
      cardinality += Long.bitCount(intersections[3]);
    }
    return cardinality;
  }

  @Benchmark
  public int mixedStaged() {
    long[] intersections = buffer;
    int cardinality = 0;
    for (int i = 0; i + 63 < size && i + 63 < left.length && i + 63 < right.length; i += 64) {
      YMM_LONG.fromArray(left, i).and(YMM_LONG.fromArray(right, i)).intoArray(intersections, 0);
      YMM_LONG.fromArray(left, i + 4).and(YMM_LONG.fromArray(right, i + 4)).intoArray(intersections, 4);
      YMM_LONG.fromArray(left, i + 8).and(YMM_LONG.fromArray(right, i + 8)).intoArray(intersections, 8);
      YMM_LONG.fromArray(left, i + 12).and(YMM_LONG.fromArray(right, i + 12)).intoArray(intersections, 12);
      YMM_LONG.fromArray(left, i + 16).and(YMM_LONG.fromArray(right, i + 16)).intoArray(intersections, 16);
      YMM_LONG.fromArray(left, i + 20).and(YMM_LONG.fromArray(right, i + 20)).intoArray(intersections, 20);
      YMM_LONG.fromArray(left, i + 24).and(YMM_LONG.fromArray(right, i + 24)).intoArray(intersections, 24);
      YMM_LONG.fromArray(left, i + 28).and(YMM_LONG.fromArray(right, i + 28)).intoArray(intersections, 28);
      YMM_LONG.fromArray(left, i + 32).and(YMM_LONG.fromArray(right, i + 32)).intoArray(intersections, 32);
      YMM_LONG.fromArray(left, i + 36).and(YMM_LONG.fromArray(right, i + 36)).intoArray(intersections, 36);
      YMM_LONG.fromArray(left, i + 40).and(YMM_LONG.fromArray(right, i + 40)).intoArray(intersections, 40);
      YMM_LONG.fromArray(left, i + 44).and(YMM_LONG.fromArray(right, i + 44)).intoArray(intersections, 44);
      YMM_LONG.fromArray(left, i + 48).and(YMM_LONG.fromArray(right, i + 48)).intoArray(intersections, 48);
      YMM_LONG.fromArray(left, i + 52).and(YMM_LONG.fromArray(right, i + 52)).intoArray(intersections, 52);
      YMM_LONG.fromArray(left, i + 56).and(YMM_LONG.fromArray(right, i + 56)).intoArray(intersections, 56);
      YMM_LONG.fromArray(left, i + 60).and(YMM_LONG.fromArray(right, i + 60)).intoArray(intersections, 60);
      for (int j = 0; j < 64; ++j) {
        cardinality += Long.bitCount(intersections[j]);
      }
    }
    return cardinality;
  }


}
