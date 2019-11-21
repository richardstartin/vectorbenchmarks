package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.LongVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.L256;
import static com.openkappa.panama.vectorbenchmarks.Util.newDirectBitmap;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class DirectBitmap {

  @Param(
          {
                  "1024"
          })
  int size;

  private ByteBuffer left;
  private ByteBuffer right;
  private ByteBuffer result;

  @Setup(Level.Trial)
  public void init() {
    this.left = newDirectBitmap(size);
    this.right = newDirectBitmap(size);
    this.result = newDirectBitmap(size);
  }

  @Benchmark
  public void intersect(Blackhole bh) {
    for (int i = 0; i < size; ++i) {
      result.putLong(i, left.getLong(i) & right.getLong(i));
    }
    bh.consume(result);
  }
}
