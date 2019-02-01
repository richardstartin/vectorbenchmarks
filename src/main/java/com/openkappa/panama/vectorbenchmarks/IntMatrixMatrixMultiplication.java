package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;

import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static com.openkappa.panama.vectorbenchmarks.Util.newIntRowMatrix;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111", "-XX:-TieredCompilation"})
public class IntMatrixMatrixMultiplication {


  @Param({"64", "512", "1024"})
  int size;

  private int[] left;
  private int[] right;
  private int[] result;

  @Setup(Level.Iteration)
  public void init() {
    this.left = newIntRowMatrix(size);
    this.right = newIntRowMatrix(size);
    this.result = newIntRowMatrix(size);
  }

  @Benchmark
  public void mmmPanama(Blackhole bh) {
    mmulPanama(size, left, right, result);
    bh.consume(result);
  }

  @Benchmark
  public void mmmAutoVectorised(Blackhole bh) {
    fastBuffered(size, left, right, result);
    bh.consume(result);
  }


  private static void mmulPanama(int n, int[] left, int[] right, int[] result) {
    int blockWidth = n >= 256 ? 512 : 256;
    int block_height = n >= 512 ? 8 : n >= 256 ? 16 : 32;
    for (int columnOffset = 0; columnOffset < n; columnOffset += blockWidth) {
      for (int rowOffset = 0; rowOffset < n; rowOffset += block_height) {
        for (int i = 0; i < n; ++i) {
          for (int j = columnOffset; j < columnOffset + blockWidth && j < n; j += 64) {
            var sum1 = I256.fromArray(result, i * n + j);
            var sum2 = I256.fromArray(result, i * n + j + 8);
            var sum3 = I256.fromArray(result, i * n + j + 16);
            var sum4 = I256.fromArray(result, i * n + j + 24);
            var sum5 = I256.fromArray(result, i * n + j + 32);
            var sum6 = I256.fromArray(result, i * n + j + 40);
            var sum7 = I256.fromArray(result, i * n + j + 48);
            var sum8 = I256.fromArray(result, i * n + j + 56);
            for (int k = rowOffset; k < rowOffset + block_height && k < n; ++k) {
              var multiplier = I256.broadcast(left[i * n + k]);
              sum1 = sum1.add(multiplier.mul(I256.fromArray(right, k * n + j)));
              sum2 = sum2.add(multiplier.mul(I256.fromArray(right, k * n + j + 8)));
              sum3 = sum3.add(multiplier.mul(I256.fromArray(right, k * n + j + 16)));
              sum4 = sum4.add(multiplier.mul(I256.fromArray(right, k * n + j + 24)));
              sum5 = sum5.add(multiplier.mul(I256.fromArray(right, k * n + j + 32)));
              sum6 = sum6.add(multiplier.mul(I256.fromArray(right, k * n + j + 40)));
              sum7 = sum7.add(multiplier.mul(I256.fromArray(right, k * n + j + 48)));
              sum8 = sum8.add(multiplier.mul(I256.fromArray(right, k * n + j + 56)));
            }
            sum1.intoArray(result, i * n + j);
            sum2.intoArray(result, i * n + j + 8);
            sum3.intoArray(result, i * n + j + 16);
            sum4.intoArray(result, i * n + j + 24);
            sum5.intoArray(result, i * n + j + 32);
            sum6.intoArray(result, i * n + j + 40);
            sum7.intoArray(result, i * n + j + 48);
            sum8.intoArray(result, i * n + j + 56);
          }
        }
      }
    }
  }

  public void fastBuffered(int n, int[] a, int[] b, int[] c) {
    int[] bBuffer = new int[n];
    int[] cBuffer = new int[n];
    int in = 0;
    for (int i = 0; i < n; ++i) {
      int kn = 0;
      for (int k = 0; k < n; ++k) {
        int aik = a[in + k];
        System.arraycopy(b, kn, bBuffer, 0, n);
        saxpy(n, aik, bBuffer, cBuffer);
        kn += n;
      }
      System.arraycopy(cBuffer, 0, c, in, n);
      Arrays.fill(cBuffer, 0);
      in += n;
    }
  }


  private void saxpy(int n, int aik, int[] b, int[] c) {
    for (int i = 0; i < n; ++i) {
      c[i] += aik * b[i];
    }
  }
}