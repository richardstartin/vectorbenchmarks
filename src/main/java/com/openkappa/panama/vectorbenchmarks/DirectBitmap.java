package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_LONG;
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

  @Benchmark
  public void intersectVector(Blackhole bh) {
    for (int i = 0; i < size; i += 8 * 4) {
      YMM_LONG.fromByteBuffer(left, i).and(YMM_LONG.fromByteBuffer(right, i)).intoByteBuffer(result, i);
    }
    bh.consume(result);
  }
}
