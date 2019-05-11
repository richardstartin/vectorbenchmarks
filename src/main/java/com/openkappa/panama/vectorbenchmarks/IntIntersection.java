package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.newSortedIntArray;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111"})
public class IntIntersection {

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    @Param({"32", "64", "128"})
    int smallSize;

    @Param({"500", "2000", "4000"})
    int largeSize;

    //@Param("0.1")
    double density;

    int[] largeArray;
    int[] result;
    int[] smallArray;

    @Setup(Level.Trial)
    public void init() {
      largeArray = newSortedIntArray(largeSize, 0x80000);
      smallArray = new int[smallSize];
      for (int i = 0; i < smallSize; ++i) {
        smallArray[i] = largeArray[ThreadLocalRandom.current().nextInt(largeSize)];
      }
      Arrays.sort(smallArray);
      result = new int[smallSize];
    }

  }

  int shotgunSearch4(final int[] smallSet, final int[] largeSet, int[] answer) {
    int pos = 0, li = 0, si = 0;
    if (0 == smallSet.length) {
      return 0;
    }
    int index1, index2, index3, index4;
    while ((si + 4 <= smallSet.length) && (li < largeSet.length)) {
      int t1 = smallSet[si];
      int t2 = smallSet[si + 1];
      int t3 = smallSet[si + 2];
      int t4 = smallSet[si + 3];
      int n = largeSet.length - li;
      if (n == 0)
        return pos;
      int base1 = li;
      int base2 = li;
      int base3 = li;
      int base4 = li;
      while (n > 1) {
        int half = n >>> 1;
        base1 = (largeSet[base1 + half] < t1) ? base1 + half : base1;
        base2 = (largeSet[base2 + half] < t2) ? base2 + half : base2;
        base3 = (largeSet[base3 + half] < t3) ? base3 + half : base3;
        base4 = (largeSet[base4 + half] < t4) ? base4 + half : base4;
        n -= half;
      }
      index1 = (largeSet[base1] < t1) ? base1 + 1 : base1;
      index2 = (largeSet[base2] < t2) ? base2 + 1 : base2;
      index3 = (largeSet[base3] < t3) ? base3 + 1 : base3;
      index4 = (largeSet[base4] < t4) ? base4 + 1 : base4;

      if ((index1 < largeSet.length) && (largeSet[index1] == t1)) {
        answer[pos++] = t1;
      }
      if ((index2 < largeSet.length) && (largeSet[index2] == t2)) {
        answer[pos++] = t2;
      }
      if ((index3 < largeSet.length) && (largeSet[index3] == t3)) {
        answer[pos++] = t3;
      }
      if ((index4 < largeSet.length) && (largeSet[index4] == t4)) {
        answer[pos++] = t4;
      }
      si += 4;
      li = index4;
    }
    if ((si < smallSet.length) && (li < largeSet.length)) {
      int sVal = smallSet[si];
      int index = Arrays.binarySearch(largeSet, li, largeSet.length, sVal);
      if (index >= 0)
        answer[pos++] = sVal;
    }
    return pos;
  }

  int shotgunSearch8(final int[] smallSet, final int[] largeSet, int[] answer) {
    int pos = 0, li = 0, si = 0;
    if (0 == smallSet.length) {
      return 0;
    }
    int i1, i2, i3, i4, i5, i6, i7, i8;
    while ((si + 8 <= smallSet.length) && (li < largeSet.length)) {
      int t1 = smallSet[si];
      int t2 = smallSet[si + 1];
      int t3 = smallSet[si + 2];
      int t4 = smallSet[si + 3];
      int t5 = smallSet[si + 4];
      int t6 = smallSet[si + 5];
      int t7 = smallSet[si + 6];
      int t8 = smallSet[si + 7];
      int n = largeSet.length - li;
      if (n == 0)
        return pos;
      int base1 = li;
      int base2 = li;
      int base3 = li;
      int base4 = li;
      int base5 = li;
      int base6 = li;
      int base7 = li;
      int base8 = li;
      while (n > 1) {
        int half = n >>> 1;
        base1 = (largeSet[base1 + half] < t1) ? base1 + half : base1;
        base2 = (largeSet[base2 + half] < t2) ? base2 + half : base2;
        base3 = (largeSet[base3 + half] < t3) ? base3 + half : base3;
        base4 = (largeSet[base4 + half] < t4) ? base4 + half : base4;
        base5 = (largeSet[base5 + half] < t5) ? base5 + half : base5;
        base6 = (largeSet[base6 + half] < t6) ? base6 + half : base6;
        base7 = (largeSet[base7 + half] < t7) ? base7 + half : base7;
        base8 = (largeSet[base8 + half] < t8) ? base8 + half : base8;
        n -= half;
      }
      i1 = (largeSet[base1] < t1) ? base1 + 1 : base1;
      i2 = (largeSet[base2] < t2) ? base2 + 1 : base2;
      i3 = (largeSet[base3] < t3) ? base3 + 1 : base3;
      i4 = (largeSet[base4] < t4) ? base4 + 1 : base4;
      i5 = (largeSet[base5] < t1) ? base5 + 1 : base5;
      i6 = (largeSet[base6] < t2) ? base6 + 1 : base6;
      i7 = (largeSet[base7] < t3) ? base7 + 1 : base7;
      i8 = (largeSet[base8] < t4) ? base8 + 1 : base8;

      if ((i1 < largeSet.length) && (largeSet[i1] == t1)) {
        answer[pos++] = t1;
      }
      if ((i2 < largeSet.length) && (largeSet[i2] == t2)) {
        answer[pos++] = t2;
      }
      if ((i3 < largeSet.length) && (largeSet[i3] == t3)) {
        answer[pos++] = t3;
      }
      if ((i4 < largeSet.length) && (largeSet[i4] == t4)) {
        answer[pos++] = t4;
      }
      if ((i5 < largeSet.length) && (largeSet[i5] == t5)) {
        answer[pos++] = t5;
      }
      if ((i6 < largeSet.length) && (largeSet[i6] == t6)) {
        answer[pos++] = t6;
      }
      if ((i7 < largeSet.length) && (largeSet[i7] == t7)) {
        answer[pos++] = t7;
      }
      if ((i8 < largeSet.length) && (largeSet[i8] == t8)) {
        answer[pos++] = t8;
      }
      si += 8;
      li = i8;
    }
    if ((si < smallSet.length) && (li < largeSet.length)) {
      int sVal = smallSet[si];
      int index = Arrays.binarySearch(largeSet, li, largeSet.length, sVal);
      if (index >= 0)
        answer[pos++] = sVal;
    }
    return pos;
  }



  int shotgunSearch1(final int[] smallSet, final int[] largeSet, int[] answer) {
    int pos = 0, si = 0, li = 0;
    if (0 == smallSet.length) {
      return 0;
    }
    int index1;
    while ((si < smallSet.length) && (li < largeSet.length)) {
      int t1 = smallSet[si];
      int n = largeSet.length - li;
      if (n == 0)
        return pos;
      int base1 = li;
      while (n > 1) {
        int half = n >>> 1;
        base1 = (largeSet[base1 + half] < t1) ? base1 + half : base1;
        n -= half;
      }
      index1 = (largeSet[base1] < t1) ? base1 + 1 : base1;
      if ((index1 < largeSet.length) && (largeSet[index1] == t1)) {
        answer[pos++] = t1;
      }
      si += 1;
      li = index1;
    }
    return pos;
  }


  @Benchmark
  public int shotgun1(BenchmarkState s) {
    return shotgunSearch1(s.smallArray, s.largeArray, s.result);
  }

  @Benchmark
  public int shotgun4(BenchmarkState s) {
    return shotgunSearch4(s.smallArray, s.largeArray, s.result);
  }

  @Benchmark
  public int shotgun8(BenchmarkState s) {
    return shotgunSearch8(s.smallArray, s.largeArray, s.result);
  }

}
