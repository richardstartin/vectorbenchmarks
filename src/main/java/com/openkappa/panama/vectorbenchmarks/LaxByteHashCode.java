package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import org.openjdk.jmh.annotations.*;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.B256;
import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static com.openkappa.panama.vectorbenchmarks.Util.newByteArray;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class LaxByteHashCode {

  private static final Unsafe UNSAFE;
  private static final long BYTE_ARRAY_OFFSET;

  static {
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      UNSAFE = (Unsafe) f.get(null);
      BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static final int[] POWERS_OF_31 = new int[] {
          1,
          31,
          31 * 31,
          31 * 31 * 31,
          31 * 31 * 31 * 31,
          31 * 31 * 31 * 31 * 31,
          31 * 31 * 31 * 31 * 31 * 31,
          31 * 31 * 31 * 31 * 31 * 31 * 31,
          31 * 31 * 31 * 31 * 31 * 31 * 31 * 31
  };

  @Param({"128", "256", "512", "1024"})
  int size;

  byte[] data;

  @Setup(Level.Trial)
  public void setup() {
    data = newByteArray(size);
  }

  @Benchmark
  public int arraysHashCode() {
    return Arrays.hashCode(data);
  }

  @Benchmark
  public int hashCodeVectorAPIDependencies() {
    var next = I256.broadcast(POWERS_OF_31[8]);
    var coefficients = IntVector.fromArray(I256, POWERS_OF_31, 0);
    var acc = I256.zero();
    for (int i = 0; i < data.length; i += B256.length()) {
      acc = acc.add(coefficients.mul(ByteVector.fromArray(B256, data, i).reinterpret(I256)));
      coefficients = coefficients.mul(next);
    }
    return acc.addAll();
  }

  @Benchmark
  public int hashCodeVectorAPINoDependencies() {
    var next = I256.broadcast(POWERS_OF_31[8]);
    var coefficients1 = IntVector.fromArray(I256, POWERS_OF_31, 0);
    var coefficients2 = coefficients1.mul(next);
    var coefficients3 = coefficients2.mul(next);
    var coefficients4 = coefficients3.mul(next);
    next = next.mul(next);
    next = next.mul(next);
    var acc1 = I256.zero();
    var acc2 = I256.zero();
    var acc3 = I256.zero();
    var acc4 = I256.zero();
    for (int i = 0; i < data.length; i += B256.length() * 4) {
      acc1 = acc1.add(coefficients1.mul(ByteVector.fromArray(B256, data, i).reinterpret(I256)));
      acc2 = acc2.add(coefficients2.mul(ByteVector.fromArray(B256, data, i + B256.length()).reinterpret(I256)));
      acc3 = acc3.add(coefficients3.mul(ByteVector.fromArray(B256, data, i + 2 * B256.length()).reinterpret(I256)));
      acc4 = acc4.add(coefficients4.mul(ByteVector.fromArray(B256, data, i + 3 * B256.length()).reinterpret(I256)));
      coefficients1 = coefficients1.mul(next);
      coefficients2 = coefficients2.mul(next);
      coefficients3 = coefficients3.mul(next);
      coefficients4 = coefficients4.mul(next);
    }
    return acc1.add(acc2).add(acc3).add(acc4).addAll();
  }

  @Benchmark
  public int nativeHashCode() {
    return nativeHashCode(data);
  }


  public static int nativeHashCode(byte[] value) {
    long h = getIntFromArray(value, 0);
    for (int i = 4; i < value.length; i += 4)
      h = h * M2 + getIntFromArray(value, i);
    h *= M2;
    return (int) h ^ (int) (h >>> 25);
  }


  private static final int M2 = 0x7A646E4D;

  // read 4 bytes at a time from a byte[] assuming Java 9+ Compact Strings
  private static int getIntFromArray(byte[] value, int i) {
    return UNSAFE.getInt(value, BYTE_ARRAY_OFFSET + i);
  }

}
