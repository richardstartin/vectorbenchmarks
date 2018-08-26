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

  public static void main(String[] args) {
    PolynomialHashCode hc = new PolynomialHashCode();
    hc.size = 1024;
    hc.init();
    System.out.println(Arrays.toString(hc.coefficients));
    System.out.println(hc.hashCodeAutoVectorised());
    System.out.println(hc.polynomialHashCodeUnrolled());

  }

  @Setup(Level.Iteration)
  public void init() {
    data = newIntVector(size);
    this.coefficients = new int[size];
    coefficients[0] = 1;
    for (int i = 1; i < coefficients.length; ++i) {
      coefficients[i] = 31 * coefficients[i - 1];
    }
  }

  @Benchmark
  public int hashCodeAutoVectorised() {
    int result = 0;
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
    var next = YMM_INT.broadcast(POWERS_OF_31[8]);
    var coefficients = YMM_INT.fromArray(POWERS_OF_31, 0);
    var acc = YMM_INT.zero();
    for (int i = 0; i < data.length; i += YMM_INT.length()) {
      acc = acc.add(coefficients.mul(YMM_INT.fromArray(data, i)));
      coefficients = coefficients.mul(next);
    }

    return acc.addAll();
  }

  @Benchmark
  public int polynomialHashCodeUnrolled() {
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
    for (int i = 0; i < data.length; i += YMM_INT.length() * 4) {
      acc1 = acc1.add(coefficients1.mul(YMM_INT.fromArray(data, i)));
      acc2 = acc2.add(coefficients2.mul(YMM_INT.fromArray(data, i + YMM_INT.length())));
      acc3 = acc3.add(coefficients3.mul(YMM_INT.fromArray(data, i + 2 * YMM_INT.length())));
      acc4 = acc4.add(coefficients4.mul(YMM_INT.fromArray(data, i + 3 * YMM_INT.length())));
      coefficients1 = coefficients1.mul(next);
      coefficients2 = coefficients2.mul(next);
      coefficients3 = coefficients3.mul(next);
      coefficients4 = coefficients4.mul(next);
    }
    return acc1.add(acc2).add(acc3).add(acc4).addAll();
  }

}
