package com.openkappa.panama.vectorbenchmarks;


import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.Shapes;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_INT;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=121"})
public class PolynomialHashCode {

  @Param({"1024", "65536"})
  int size;

  private int[] coefficients;
  private int[] data;

  public static void main(String[] args) {
    PolynomialHashCode hc = new PolynomialHashCode();
    hc.size = 1024;
    hc.init();
    System.out.println(hc.hashCodeAutoVectorised());
    System.out.println(hc.hashCodePanama());

  }

  @Setup(Level.Iteration)
  public void init() {
    data = intArray(size);
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
  public int hashCodePanama() {
    IntVector<Shapes.S256Bit> next = YMM_INT.broadcast(31 * 31 * 31 * 31 * 31 * 31 * 31 * 31);
    IntVector<Shapes.S256Bit> coefficients = YMM_INT.fromArray(this.coefficients, 0);
    IntVector<Shapes.S256Bit> acc = YMM_INT.zero();
    for (int i = 0; i < data.length; i += YMM_INT.length()) {
      acc = acc.add(coefficients.mul(YMM_INT.fromArray(data, i)));
      coefficients = coefficients.mul(next);
    }

    return acc.addAll();
  }


  private static int[] intArray(int size) {
    int[] array = new int[size];
    for (int i = 0; i < array.length; ++i) {
      array[i] = ThreadLocalRandom.current().nextInt();
    }
    return array;
  }

}
