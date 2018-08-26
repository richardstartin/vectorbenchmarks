package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_BYTE;
import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;
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
    var next = YMM_INT.broadcast(POWERS_OF_31[8]);
    var coefficients = YMM_INT.fromArray(POWERS_OF_31, 0);
    var acc = YMM_INT.zero();
    for (int i = 0; i < data.length; i += YMM_BYTE.length()) {
      acc = acc.add(coefficients.mul(YMM_BYTE.fromArray(data, i).rebracket(YMM_INT)));
      coefficients = coefficients.mul(next);
    }
    return acc.addAll();
  }

  @Benchmark
  public int hashCodeVectorAPINoDependencies() {
    var next = YMM_INT.broadcast(POWERS_OF_31[8]);
    var coefficients1 = YMM_INT.fromArray(POWERS_OF_31, 0);
    var coefficients2 = coefficients1.mul(next);
    var coefficients3 = coefficients2.mul(next);
    var coefficients4 = coefficients3.mul(next);
    next = next.mul(next);
    next = next.mul(next);
    var acc1 = YMM_INT.zero();
    var acc2 = YMM_INT.zero();
    var acc3 = YMM_INT.zero();
    var acc4 = YMM_INT.zero();
    for (int i = 0; i < data.length; i += YMM_BYTE.length() * 4) {
      acc1 = acc1.add(coefficients1.mul(YMM_BYTE.fromArray(data, i).rebracket(YMM_INT)));
      acc2 = acc2.add(coefficients2.mul(YMM_BYTE.fromArray(data, i + YMM_BYTE.length()).rebracket(YMM_INT)));
      acc3 = acc3.add(coefficients3.mul(YMM_BYTE.fromArray(data, i + 2 * YMM_BYTE.length()).rebracket(YMM_INT)));
      acc4 = acc4.add(coefficients4.mul(YMM_BYTE.fromArray(data, i + 3 * YMM_BYTE.length()).rebracket(YMM_INT)));
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