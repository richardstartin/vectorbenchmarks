package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.IntVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntBitmap;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class BitmapLogicals {


  @Param({"2048", "131072"})
  int size;

  private int[] left;
  private int[] right;
  private int[] result;

  @Setup(Level.Iteration)
  public void init() {
    this.left = newIntBitmap(size);
    this.right = newIntBitmap(size);
    this.result = newIntBitmap(size);
  }

  @Benchmark
  public void intersectAutovectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] & right[i];
    }
    bh.consume(result);
  }

  @Benchmark
  public void unionAutovectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] | right[i];
    }
    bh.consume(result);
  }

  @Benchmark
  public void differenceAutovectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] ^ right[i];
    }
    bh.consume(result);
  }

  @Benchmark
  public void leftDifferenceAutovectorised(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result[i] = left[i] & ~right[i];
    }
    bh.consume(result);
  }


  @Benchmark
  public void intersectPanamaInt(Blackhole bh) {
    for (int i = 0; i < size; i += I256.length()) {
      IntVector.fromArray(I256, left, i).and(IntVector.fromArray(I256, right, i)).intoArray(result, i);
    }
    bh.consume(result);
  }

  @Benchmark
  public void unionPanamaInt(Blackhole bh) {
    for (int i = 0; i < size; i += I256.length()) {
      IntVector.fromArray(I256, left, i).or(IntVector.fromArray(I256, right, i)).intoArray(result, i);
    }
    bh.consume(result);
  }

  @Benchmark
  public void differencePanamaInt(Blackhole bh) {
    for (int i = 0; i < size; i += I256.length()) {
      IntVector.fromArray(I256, left, i).xor(IntVector.fromArray(I256, right, i)).intoArray(result, i);
    }
    bh.consume(result);
  }

  @Benchmark
  public void leftDifferencePanamaInt(Blackhole bh) {
    for (int i = 0; i < size; i += I256.length()) {
      IntVector.fromArray(I256, left, i).and(IntVector.fromArray(I256, right, i).not()).intoArray(result, i);
    }
    bh.consume(result);
  }
}
