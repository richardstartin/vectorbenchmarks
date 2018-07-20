package com.openkappa.panama.vectorbenchmarks;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntBitmap;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:TypeProfileLevel=111",
        "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"
})
public class Hashing {

  private int[] hashes;
  private int[] values;
  private int mask;

  @Param({"1024", "8192", "65536"})
  int size;

  @Setup(Level.Trial)
  public void setup() {
    hashes = new int[size];
    mask = ThreadLocalRandom.current().nextInt();
    values = newIntBitmap(size);
  }

  @Benchmark
  public void getHashPosition(Blackhole bh) {
    for (int i = 0; i < values.length; ++i) {
      hashes[i] = getHashPosition(values[i], mask);
    }
    bh.consume(hashes);
  }

  @Benchmark
  public void getHashPositionVector(Blackhole bh) {
    var c1 = YMM_INT.broadcast(0xed558ccd);
    var c2 = YMM_INT.broadcast(0x1a85ec53);
    var vectorMask = YMM_INT.broadcast(mask);
    for (int i = 0; i < values.length; i += YMM_INT.length()) {
      var vector = YMM_INT.fromArray(values, i);
      vector = vector.xor(vector.shiftR(15)).mul(c1);
      vector = vector.xor(vector.shiftR(15)).mul(c2);
      vector.xor(vector.shiftR(15)).and(vectorMask).intoArray(hashes, i);
    }
    bh.consume(hashes);
  }



  // see http://prestodb.rocks/code/simd/
  private static int getHashPosition(int rawHash, int mask) {
    rawHash ^= rawHash >>> 15;
    rawHash *= 0xed558ccd;
    rawHash ^= rawHash >>> 15;
    rawHash *= 0x1a85ec53;
    rawHash ^= rawHash >>> 15;
    return rawHash & mask;
  }
}
