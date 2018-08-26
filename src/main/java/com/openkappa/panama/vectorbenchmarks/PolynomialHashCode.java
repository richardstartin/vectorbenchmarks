package com.openkappa.panama.vectorbenchmarks;

import java.util.Arrays;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntVector;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class PolynomialHashCode {

  @Param({"1024", "65536"})
  int size;

  private int[] coefficients;
  private int[] data;
  private int seed;
  private static final int[] POWERS_OF_31_BACKWARDS = new int[33];
  static {
    POWERS_OF_31_BACKWARDS[POWERS_OF_31_BACKWARDS.length - 1] = 1;
    for (int i = POWERS_OF_31_BACKWARDS.length - 2; i >= 0; --i) {
      POWERS_OF_31_BACKWARDS[i] = 31 * POWERS_OF_31_BACKWARDS[i + 1];
    }
  }

  public static void main(String[] args) {
    PolynomialHashCode hc = new PolynomialHashCode();
    hc.size = 1024;
    hc.init();
    System.out.println(hc.arraysHashCode());
    System.out.println(hc.hashCodeAutoVectorised());
    System.out.println(hc.polynomialHashCode());
    System.out.println(hc.polynomialHashCodeUnrolled());
  }

  @Setup(Level.Iteration)
  public void init() {
    data = newIntVector(size);
    this.coefficients = new int[size];
    coefficients[size - 1] = 1;
    for (int i = size - 2; i >= 0; --i) {
      coefficients[i] = 31 * coefficients[i + 1];
    }
    seed = 31 * coefficients[0];
  }

  @Benchmark
  public int hashCodeAutoVectorised() {
    int result = seed;
    for (int i = 0; i < data.length && i < coefficients.length; ++i) {
      result += coefficients[i] * data[i];
    }
    return result;
  }

  @Benchmark
  public int arraysHashCode() {
    return Arrays.hashCode(data);
  }

  @Benchmark
  public int polynomialHashCode() {
    var next = YMM_INT.broadcast(POWERS_OF_31_BACKWARDS[33 - 9]);
    var coefficients = YMM_INT.fromArray(POWERS_OF_31_BACKWARDS, 33 - 8);
    var acc = YMM_INT.zero();
    for (int i = data.length; i - YMM_INT.length() >= 0; i -= YMM_INT.length()) {
      acc = acc.add(coefficients.mul(YMM_INT.fromArray(data, i - YMM_INT.length())));
      coefficients = coefficients.mul(next);
    }
    return acc.addAll() + coefficients.get(7);
  }

  @Benchmark
  public int polynomialHashCodeUnrolled() {
    var next = YMM_INT.broadcast(POWERS_OF_31_BACKWARDS[0]);
    var coefficients1 = YMM_INT.fromArray(POWERS_OF_31_BACKWARDS, 33 - 8);
    var coefficients2 = YMM_INT.fromArray(POWERS_OF_31_BACKWARDS, 33 - 16);
    var coefficients3 = YMM_INT.fromArray(POWERS_OF_31_BACKWARDS, 33 - 24);
    var coefficients4 = YMM_INT.fromArray(POWERS_OF_31_BACKWARDS, 33 - 32);
    var acc1 = YMM_INT.zero();
    var acc2 = YMM_INT.zero();
    var acc3 = YMM_INT.zero();
    var acc4 = YMM_INT.zero();
    for (int i = data.length; i - 4 * YMM_INT.length() >= 0; i -= YMM_INT.length() * 4) {
      acc1 = acc1.add(coefficients1.mul(YMM_INT.fromArray(data, i - YMM_INT.length())));
      acc2 = acc2.add(coefficients2.mul(YMM_INT.fromArray(data, i - 2 * YMM_INT.length())));
      acc3 = acc3.add(coefficients3.mul(YMM_INT.fromArray(data, i - 3 * YMM_INT.length())));
      acc4 = acc4.add(coefficients4.mul(YMM_INT.fromArray(data, i - 4 * YMM_INT.length())));
      coefficients1 = coefficients1.mul(next);
      coefficients2 = coefficients2.mul(next);
      coefficients3 = coefficients3.mul(next);
      coefficients4 = coefficients4.mul(next);
    }
    return acc1.add(acc2).add(acc3).add(acc4).addAll() + coefficients1.get(7);
  }

}
