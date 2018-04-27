package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=121"})
public class BitmapLogicals {


  @Param({"2048", "131072"})
  int size;

  private int[] left;
  private int[] right;
  private int[] result;

  @Setup(Level.Iteration)
  public void init() {
    this.left = newBitmap(size);
    this.right = newBitmap(size);
    this.result = newBitmap(size);
  }

  @Benchmark
  public void intersectAutovectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] & right[i];
    }
    bh.consume(result);
  }

// Seems not to have been intrinsified :(
//  @Benchmark
//  public void intersectPanamaLong(Blackhole bh) {
//    for (int i = 0; i < size; i += 4) {
//      SPECIES.fromArray(left, i).and(SPECIES.fromArray(right, i)).intoArray(result, i);
//    }
//    bh.consume(result);
//  }


  @Benchmark
  public void intersectPanamaInt(Blackhole bh) {
    for (int i = 0; i < size; i += YMM_INT.length()) {
      YMM_INT.fromArray(left, i).and(YMM_INT.fromArray(right, i)).intoArray(result, i);
    }
    bh.consume(result);
  }

  private static int[] newBitmap(int size) {
    int[] bitmap = new int[size];
    for (int i = 0; i < bitmap.length; ++i) {
      bitmap[i] = ThreadLocalRandom.current().nextInt();
    }
    return bitmap;
  }
}
