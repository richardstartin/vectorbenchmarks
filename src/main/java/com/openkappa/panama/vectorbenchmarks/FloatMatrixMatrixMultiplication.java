package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;

import static com.openkappa.panama.vectorbenchmarks.Util.YMM_FLOAT;
import static com.openkappa.panama.vectorbenchmarks.Util.newFloatRowMajorMatrix;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector",
        "-XX:TypeProfileLevel=111", "-XX:-TieredCompilation", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class FloatMatrixMatrixMultiplication {


  @Param({"64", "512", "1024"})
  int size;

  private float[] left;
  private float[] right;
  private float[] result;

  @Setup(Level.Iteration)
  public void init() {
    this.left = newFloatRowMajorMatrix(size);
    this.right = newFloatRowMajorMatrix(size);
    this.result = newFloatRowMajorMatrix(size);
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


  private static void mmulPanama(int n, float[] left, float[] right, float[] result) {
    int blockWidth = n >= 256 ? 512 : 256;
    int blockHeight = n >= 512 ? 8 : n >= 256 ? 16 : 32;
    for (int columnOffset = 0; columnOffset < n; columnOffset += blockWidth) {
      for (int rowOffset = 0; rowOffset < n; rowOffset += blockHeight) {
        for (int i = 0; i < n; ++i) {
          for (int j = columnOffset; j < columnOffset + blockWidth && j < n; j += 64) {
            var sum1 = YMM_FLOAT.fromArray(result, i * n + j);
            var sum2 = YMM_FLOAT.fromArray(result, i * n + j + 8);
            var sum3 = YMM_FLOAT.fromArray(result, i * n + j + 16);
            var sum4 = YMM_FLOAT.fromArray(result, i * n + j + 24);
            var sum5 = YMM_FLOAT.fromArray(result, i * n + j + 32);
            var sum6 = YMM_FLOAT.fromArray(result, i * n + j + 40);
            var sum7 = YMM_FLOAT.fromArray(result, i * n + j + 48);
            var sum8 = YMM_FLOAT.fromArray(result, i * n + j + 56);
            for (int k = rowOffset; k < rowOffset + blockHeight && k < n; ++k) {
              var multiplier = YMM_FLOAT.broadcast(left[i * n + k]);
              sum1 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j), sum1);
              sum2 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 8), sum2);
              sum3 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 16), sum3);
              sum4 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 24), sum4);
              sum5 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 32), sum5);
              sum6 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 40), sum6);
              sum7 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 48), sum7);
              sum8 = multiplier.fma(YMM_FLOAT.fromArray(right, k * n + j + 56), sum8);
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

  public void fastBuffered(int n, float[] a, float[] b, float[] c) {
    float[] bBuffer = new float[n];
    float[] cBuffer = new float[n];
    int in = 0;
    for (int i = 0; i < n; ++i) {
      int kn = 0;
      for (int k = 0; k < n; ++k) {
        float aik = a[in + k];
        System.arraycopy(b, kn, bBuffer, 0, n);
        saxpy(n, aik, bBuffer, cBuffer);
        kn += n;
      }
      System.arraycopy(cBuffer, 0, c, in, n);
      Arrays.fill(cBuffer, 0f);
      in += n;
    }
  }


  private void saxpy(int n, float aik, float[] b, float[] c) {
    for (int i = 0; i < n; ++i) {
      c[i] = Math.fma(aik, b[i], c[i]);
    }
  }
}