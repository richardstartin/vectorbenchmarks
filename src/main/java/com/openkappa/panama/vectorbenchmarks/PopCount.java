package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.*;
import static jdk.incubator.vector.VectorOperators.*;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector",
         "-XX:-TieredCompilation", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class PopCount {

  @Param({"1024"})
  private int size;


  public static void main(String... args) {
    PopCount benchmark = new PopCount();
    benchmark.size = 1024;
    benchmark.init();
    Arrays.fill(benchmark.data, -1);
    while (true) {
      System.out.println(benchmark.scalar());
      System.out.println(benchmark.vectorBitCount());
      benchmark.init();
    }
//    System.out.println(benchmark.harleySeal());
  }

  @Setup(Level.Trial)
  public void init() {
    data = newLongBitmap(size);
  }

  private static byte[] NIBBLE_COUNTS = new byte[] {
          0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  long[] data;

  @Benchmark
  public int vectorBitCount() {
    int bitCount = 0;
    int block = 256;
    for (int i = 0; i < data.length; i += block) {
      var lo1 = IntVector.zero(I256);
      var hi1 = IntVector.zero(I256);
      var lo2 = IntVector.zero(I256);
      var hi2 = IntVector.zero(I256);
      var counts = ByteVector.fromArray(B256, NIBBLE_COUNTS, 0);
      for (int j = 0; j < block; j += 8) {
        var v1 = (IntVector) LongVector.fromArray(L256, data, i + j).reinterpretAsInts();
        var v2 = (IntVector) LongVector.fromArray(L256, data, i + j + 4).reinterpretAsInts();
        lo1 = lo1.add(counts.rearrange(v1.and(0x0F0F0F0F).reinterpretAsBytes().toShuffle()).reinterpretAsInts());
        hi1 = hi1.add(counts.rearrange(v1.lanewise(LSHR, 4).and(0x0F0F0F0F).reinterpretAsBytes().toShuffle()).reinterpretAsInts());
        lo2 = lo2.add(counts.rearrange(v2.and(0x0F0F0F0F).reinterpretAsBytes().toShuffle()).reinterpretAsInts());
        hi2 = hi2.add(counts.rearrange(v2.lanewise(LSHR, 4).and(0x0F0F0F0F).reinterpretAsBytes().toShuffle()).reinterpretAsInts());
      }
      bitCount += unsignedSum(lo1);
      bitCount += unsignedSum(hi1);
      bitCount += unsignedSum(lo2);
      bitCount += unsignedSum(hi2);
    }
    return bitCount;
  }

  private int unsignedSum(IntVector bv) {
    // convert to LongVector because Vector.get is slow
    var lv = (LongVector) bv.reinterpretAsLongs();
    return sumBytes(lv.lane(0))
         + sumBytes(lv.lane(1))
         + sumBytes(lv.lane(2))
         + sumBytes(lv.lane(3));
  }

  private int sumBytes(long w) {
    return ((int)w & 0xFF)
            + (((int)(w >>> 8))  & 0xFF)
            + (((int)(w >>> 16)) & 0xFF)
            + (((int)(w >>> 24)) & 0xFF)
            + (((int)(w >>> 32)) & 0xFF)
            + (((int)(w >>> 40)) & 0xFF)
            + (((int)(w >>> 48)) & 0xFF)
            + (((int)(w >>> 56)) & 0xFF);
  }

//  @Benchmark
//  public int harleySeal() {
//    return harleySeal(data);
//  }


  @Benchmark
  public int scalar() {
    int bitCount = 0;
    for (long datum : data) {
      bitCount += Long.bitCount(datum);
    }
    return bitCount;
  }


  public int harleySeal(long[] data) {
    var total = LongVector.zero(L256);
    var ones = LongVector.zero(L256);
    var twos = LongVector.zero(L256);
    var fours = LongVector.zero(L256);
    var eights = LongVector.zero(L256);
    LongVector sixteens;
    LongVector twosA;
    LongVector twosB;
    LongVector foursA;
    LongVector foursB;
    LongVector eightsA;
    LongVector eightsB;
    for (int i = 0; i < data.length >>> 2; i += 16 * 4) {
      var a1 = LongVector.fromArray(L256, data, i);
      var b1 = ones;
      var c1 = LongVector.fromArray(L256, data, i + 1 * 4);
      var u1 = a1.lanewise(XOR, b1);
      twosA = a1.and(b1).or(u1.and(c1));
      ones = u1.lanewise(XOR, c1);
      var a2 = ones;
      var b2 = LongVector.fromArray(L256, data, i + 2 * 4);
      var c2 = LongVector.fromArray(L256, data, i + 3 * 4);
      var u2 = a2.lanewise(XOR, b2);
      twosB = a2.and(b2).or(u2.and(c2));
      ones = u2.lanewise(XOR, c2);
      var a3 = twos;
      var b3 = twosA;
      var c3 = twosB;
      var u3 = a3.lanewise(XOR, b3);
      foursA = a3.and(b3).or(u3.and(c3));
      twos = u3.lanewise(XOR, c3);
      var a4 = ones;
      var b4 = LongVector.fromArray(L256, data, i + 4 * 4);
      var c4 = LongVector.fromArray(L256, data, i + 5 * 4);
      var u4 = a4.lanewise(XOR, b4);
      twosA = a4.and(b4).or(u4.and(c4));
      ones = u4.lanewise(XOR, c4);
      var a5 = ones;
      var b5 = LongVector.fromArray(L256, data, i + 6 * 4);
      var c5 = LongVector.fromArray(L256, data, i + 7 * 4);
      var u5 = a5.lanewise(XOR, b5);
      twosB = a5.and(b5).or(u5.and(c5));
      ones = u5.lanewise(XOR, c5);
      var a6 = twos;
      var b6 = twosA;
      var c6 = twosB;
      var u6 = a6.lanewise(XOR, b6);
      foursB = a6.and(b6).or(u6.and(c6));
      twos = u6.lanewise(XOR, c6);
      var a7 = fours;
      var b7 = foursA;
      var c7 = foursB;
      var u7 = a7.lanewise(XOR, b7);
      eightsA = a7.and(b7).or(u7.and(c7));
      fours = u7.lanewise(XOR, c7);
      var a8 = ones;
      var b8 = LongVector.fromArray(L256, data, i + 8 * 4);
      var c8 = LongVector.fromArray(L256, data, i + 9 * 4);
      var u8 = a8.lanewise(XOR, b8);
      twosA = a8.and(b8).or(u8.and(c8));
      ones = u8.lanewise(XOR, c8);
      var a9 = ones;
      var b9 = LongVector.fromArray(L256, data, i + 10 * 4);
      var c9 = LongVector.fromArray(L256, data, i + 11 * 4);
      var u9 = a9.lanewise(XOR, b9);
      twosB = a9.and(b9).or(u9.and(c9));
      ones = u9.lanewise(XOR, c9);
      var a10 = twos;
      var b10 = twosA;
      var c10 = twosB;
      var u10 = a10.lanewise(XOR, b10);
      foursA = a10.and(b10).or(u10.and(c10));
      twos = u10.lanewise(XOR, c10);
      var a11 = ones;
      var b11 = LongVector.fromArray(L256, data, i + 12 * 4);
      var c11 = LongVector.fromArray(L256, data, i + 13 * 4);
      var u11 = a11.lanewise(XOR, b11);
      twosA = a11.and(b11).or(u11.and(c11));
      ones = u11.lanewise(XOR, c11);
      var a12 = ones;
      var b12 = LongVector.fromArray(L256, data, i + 14 * 4);
      var c12 = LongVector.fromArray(L256, data, i + 15 * 4);
      var u12 = a12.lanewise(XOR, b12);
      twosB = a12.and(b12).or(u12.and(c12));
      ones = u12.lanewise(XOR, c12);
      var a13 = twos;
      var b13 = twosA;
      var c13 = twosB;
      var u13 = a13.lanewise(XOR, b13);
      foursB = a13.and(b13).or(u13.and(c13));
      twos = u13.lanewise(XOR, c13);
      var a14 = fours;
      var b14 = foursA;
      var c14 = foursB;
      var u14 = a14.lanewise(XOR, b14);
      eightsB = a14.and(b14).or(u14.and(c14));
      fours = u14.lanewise(XOR, c14);
      var a15 = eights;
      var b15 = eightsA;
      var c15 = eightsB;
      var u15 = a15.lanewise(XOR, b15);
      sixteens = a15.and(b15).or(u15.and(c15));
      eights = u15.lanewise(XOR, c15);
      total = total.add(popcount256(sixteens));
    }
    total = total.lanewise(LSHL, 4);
    total = total.add(popcount256(eights).lanewise(LSHL,3));
    total = total.add(popcount256(fours).lanewise(LSHL,2));
    total = total.add(popcount256(twos).lanewise(LSHL,1));
    total = total.add(popcount256(ones));
    return (int)total.reduceLanes(ADD);
  }

  private LongVector popcount256(LongVector vector) {
    var bytes = (ByteVector)vector.reinterpretAsBytes();
    var lookupPos = ByteVector.fromArray(B256, LOOKUP_POS, 0);
    var lookupNeg = ByteVector.fromArray(B256, LOOKUP_NEG, 0);
    var lowMask = ByteVector.broadcast(B256, (byte)0x0F);
    return (LongVector)lookupPos.rearrange(bytes.and(lowMask).toShuffle())
            .add(lookupNeg.rearrange(bytes.lanewise(LSHR, 4).and(lowMask).toShuffle()))
            .reinterpretAsLongs();
  }

  private static byte[] LOOKUP_POS = new byte[] {
          /* 0 */ 4 + 0, /* 1 */ 4 + 1, /* 2 */ 4 + 1, /* 3 */ 4 + 2,
          /* 4 */ 4 + 1, /* 5 */ 4 + 2, /* 6 */ 4 + 2, /* 7 */ 4 + 3,
          /* 8 */ 4 + 1, /* 9 */ 4 + 2, /* a */ 4 + 2, /* b */ 4 + 3,
          /* c */ 4 + 2, /* d */ 4 + 3, /* e */ 4 + 3, /* f */ 4 + 4,
          /* 0 */ 4 + 0, /* 1 */ 4 + 1, /* 2 */ 4 + 1, /* 3 */ 4 + 2,
          /* 4 */ 4 + 1, /* 5 */ 4 + 2, /* 6 */ 4 + 2, /* 7 */ 4 + 3,
          /* 8 */ 4 + 1, /* 9 */ 4 + 2, /* a */ 4 + 2, /* b */ 4 + 3,
          /* c */ 4 + 2, /* d */ 4 + 3, /* e */ 4 + 3, /* f */ 4 + 4
  };

  private static byte[] LOOKUP_NEG = new byte[] {
          /* 0 */ 4 - 0, /* 1 */ 4 - 1, /* 2 */ 4 - 1, /* 3 */ 4 - 2,
          /* 4 */ 4 - 1, /* 5 */ 4 - 2, /* 6 */ 4 - 2, /* 7 */ 4 - 3,
          /* 8 */ 4 - 1, /* 9 */ 4 - 2, /* a */ 4 - 2, /* b */ 4 - 3,
          /* c */ 4 - 2, /* d */ 4 - 3, /* e */ 4 - 3, /* f */ 4 - 4,
          /* 0 */ 4 - 0, /* 1 */ 4 - 1, /* 2 */ 4 - 1, /* 3 */ 4 - 2,
          /* 4 */ 4 - 1, /* 5 */ 4 - 2, /* 6 */ 4 - 2, /* 7 */ 4 - 3,
          /* 8 */ 4 - 1, /* 9 */ 4 - 2, /* a */ 4 - 2, /* b */ 4 - 3,
          /* c */ 4 - 2, /* d */ 4 - 3, /* e */ 4 - 3, /* f */ 4 - 4
  };
}
