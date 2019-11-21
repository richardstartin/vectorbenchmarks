package com.openkappa.panama.vectorbenchmarks;


import jdk.incubator.vector.IntVector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntBitmap;
import static jdk.incubator.vector.VectorOperators.LSHR;
import static jdk.incubator.vector.VectorOperators.XOR;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {
        "--add-modules=jdk.incubator.vector",
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
    var c1 = IntVector.broadcast(I256, 0xed558ccd);
    var c2 = IntVector.broadcast(I256, 0x1a85ec53);
    var vectorMask = IntVector.broadcast(I256, mask);
    for (int i = 0; i < values.length; i += I256.length()) {
      var vector = IntVector.fromArray(I256, values, i);
      vector = vector.lanewise(XOR, vector.lanewise(LSHR, 15)).mul(c1);
      vector = vector.lanewise(XOR, vector.lanewise(LSHR, 15)).mul(c2);
      vector.lanewise(XOR, vector.lanewise(LSHR, 15)).and(vectorMask).intoArray(hashes, i);
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
