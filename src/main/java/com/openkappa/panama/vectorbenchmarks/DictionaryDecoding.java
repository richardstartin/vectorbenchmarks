package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.util.SplittableRandom;

import static com.openkappa.panama.vectorbenchmarks.Util.I256;
import static jdk.incubator.vector.VectorOperators.AND;
import static jdk.incubator.vector.VectorOperators.LSHR;

@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
public class DictionaryDecoding {

  /*
  Benchmark                    (size)  Mode  Cnt    Score   Error  Units
DictionaryDecoding.scalar1  1000000  avgt    5  167.318 ± 0.229  us/op
DictionaryDecoding.scalar2  1000000  avgt    5  172.398 ± 0.243  us/op
DictionaryDecoding.scalar3  1000000  avgt    5  181.625 ± 0.325  us/op
DictionaryDecoding.scalar4  1000000  avgt    5  175.840 ± 0.065  us/op
DictionaryDecoding.vector1  1000000  avgt    5   74.040 ± 0.087  us/op
DictionaryDecoding.vector2  1000000  avgt    5   84.859 ± 0.055  us/op
DictionaryDecoding.vector3  1000000  avgt    5  157.815 ± 0.713  us/op
DictionaryDecoding.vector4  1000000  avgt    5   98.945 ± 0.386  us/op

   */

  private static final int[] SHIFTS_1 = {31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
  private static final int[] SHIFTS_2 = {30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0};
  private static final int[] SHIFTS_3 = {29, 26, 23, 20, 17, 14, 11, 8, 5, 2, 31, 28, 25, 22, 19, 16, 13, 10, 7, 4, 1, 30, 27, 24, 21, 18, 15, 12, 9, 6, 3, 0};
  private static final int[] SHIFTS_4 = {28, 24, 20, 16, 12, 8, 4, 0};

  @Param("1000000")
  int size;

  private ByteBuffer values;
  private int[] buffer;
  private int[] buffer2;

  @Setup(Level.Trial)
  public void setup() {
    SplittableRandom random = new SplittableRandom(0);
    buffer = new int[32];
    buffer2 = new int[32];
    values = ByteBuffer.allocateDirect((size + 3) & -4);
    for (int i = 0; i < size / 4; i++)
      values.putInt(random.nextInt());
  }


  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void vector1(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read32Vector1(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void scalar1(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read321(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void vector2(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read32Vector2(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void scalar2(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read322(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void vector3(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read32Vector3(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void scalar3(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read323(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void vector4(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read32Vector4(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  public void scalar4(Blackhole bh) {
    for (int i = 0; i < size / 4; i += 32) {
      read324(values, i, buffer, 0);
      bh.consume(buffer);
    }
  }

  public void read32Vector1(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 3;
    int value = in.getInt(offset);
    IntVector MASK = IntVector.broadcast(I256, 1);
    IntVector.broadcast(I256, value)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_1, 0))
            .lanewise(AND, MASK)
            .intoArray(out, outPos);
    IntVector.broadcast(I256, value)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_1, 8))
            .lanewise(AND, MASK)
            .intoArray(out, outPos + 8);
    IntVector.broadcast(I256, value)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_1, 16))
            .lanewise(AND, MASK)
            .intoArray(out, outPos + 16);
    IntVector.broadcast(I256, value)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_1, 24))
            .lanewise(AND, MASK)
            .intoArray(out, outPos + 24);
  }

  public void read321(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 3;
    int i0 = in.getInt(offset);
    out[outPos] = i0 >>> 31;
    out[outPos + 1] = (i0 >>> 30) & 0x1;
    out[outPos + 2] = (i0 >>> 29) & 0x1;
    out[outPos + 3] = (i0 >>> 28) & 0x1;
    out[outPos + 4] = (i0 >>> 27) & 0x1;
    out[outPos + 5] = (i0 >>> 26) & 0x1;
    out[outPos + 6] = (i0 >>> 25) & 0x1;
    out[outPos + 7] = (i0 >>> 24) & 0x1;
    out[outPos + 8] = (i0 >>> 23) & 0x1;
    out[outPos + 9] = (i0 >>> 22) & 0x1;
    out[outPos + 10] = (i0 >>> 21) & 0x1;
    out[outPos + 11] = (i0 >>> 20) & 0x1;
    out[outPos + 12] = (i0 >>> 19) & 0x1;
    out[outPos + 13] = (i0 >>> 18) & 0x1;
    out[outPos + 14] = (i0 >>> 17) & 0x1;
    out[outPos + 15] = (i0 >>> 16) & 0x1;
    out[outPos + 16] = (i0 >>> 15) & 0x1;
    out[outPos + 17] = (i0 >>> 14) & 0x1;
    out[outPos + 18] = (i0 >>> 13) & 0x1;
    out[outPos + 19] = (i0 >>> 12) & 0x1;
    out[outPos + 20] = (i0 >>> 11) & 0x1;
    out[outPos + 21] = (i0 >>> 10) & 0x1;
    out[outPos + 22] = (i0 >>> 9) & 0x1;
    out[outPos + 23] = (i0 >>> 8) & 0x1;
    out[outPos + 24] = (i0 >>> 7) & 0x1;
    out[outPos + 25] = (i0 >>> 6) & 0x1;
    out[outPos + 26] = (i0 >>> 5) & 0x1;
    out[outPos + 27] = (i0 >>> 4) & 0x1;
    out[outPos + 28] = (i0 >>> 3) & 0x1;
    out[outPos + 29] = (i0 >>> 2) & 0x1;
    out[outPos + 30] = (i0 >>> 1) & 0x1;
    out[outPos + 31] = i0 & 0x1;
  }

  public void read32Vector2(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 2;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    IntVector LSB = IntVector.broadcast(I256, 3);
    IntVector.broadcast(I256, i0)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_2, 0))
            .lanewise(AND, LSB)
            .intoArray(out, outPos);
    IntVector.broadcast(I256, i0)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_2, 8))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 8);
    IntVector.broadcast(I256, i1)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_2, 0))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 16);
    IntVector.broadcast(I256, i1)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_2, 8))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 24);
  }


  public void read322(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 2;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    out[outPos] = i0 >>> 30;
    out[outPos + 1] = (i0 >>> 28) & 0x3;
    out[outPos + 2] = (i0 >>> 26) & 0x3;
    out[outPos + 3] = (i0 >>> 24) & 0x3;
    out[outPos + 4] = (i0 >>> 22) & 0x3;
    out[outPos + 5] = (i0 >>> 20) & 0x3;
    out[outPos + 6] = (i0 >>> 18) & 0x3;
    out[outPos + 7] = (i0 >>> 16) & 0x3;
    out[outPos + 8] = (i0 >>> 14) & 0x3;
    out[outPos + 9] = (i0 >>> 12) & 0x3;
    out[outPos + 10] = (i0 >>> 10) & 0x3;
    out[outPos + 11] = (i0 >>> 8) & 0x3;
    out[outPos + 12] = (i0 >>> 6) & 0x3;
    out[outPos + 13] = (i0 >>> 4) & 0x3;
    out[outPos + 14] = (i0 >>> 2) & 0x3;
    out[outPos + 15] = i0 & 0x3;
    out[outPos + 16] = i1 >>> 30;
    out[outPos + 17] = (i1 >>> 28) & 0x3;
    out[outPos + 18] = (i1 >>> 26) & 0x3;
    out[outPos + 19] = (i1 >>> 24) & 0x3;
    out[outPos + 20] = (i1 >>> 22) & 0x3;
    out[outPos + 21] = (i1 >>> 20) & 0x3;
    out[outPos + 22] = (i1 >>> 18) & 0x3;
    out[outPos + 23] = (i1 >>> 16) & 0x3;
    out[outPos + 24] = (i1 >>> 14) & 0x3;
    out[outPos + 25] = (i1 >>> 12) & 0x3;
    out[outPos + 26] = (i1 >>> 10) & 0x3;
    out[outPos + 27] = (i1 >>> 8) & 0x3;
    out[outPos + 28] = (i1 >>> 6) & 0x3;
    out[outPos + 29] = (i1 >>> 4) & 0x3;
    out[outPos + 30] = (i1 >>> 2) & 0x3;
    out[outPos + 31] = i1 & 0x3;
  }

  public void read32Vector3(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = (index >>> 3) * 3;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    int i2 = in.getInt(offset + 8);
    IntVector LSB = IntVector.broadcast(I256, 7);
    IntVector.broadcast(I256, i0)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_3, 0))
            .lanewise(AND, LSB)
            .intoArray(out, outPos);
    IntVector.broadcast(I256, i0).blend(IntVector.broadcast(I256, i1), VectorMask.fromLong(I256, 0b11111100))
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_3, 8))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 8);
    IntVector.broadcast(I256, i1).blend(IntVector.broadcast(I256, i2), VectorMask.fromLong(I256, 0b11100000))
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_3, 16))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 16);
    IntVector.broadcast(I256, i2)
            .lanewise(LSHR, IntVector.fromArray(I256, SHIFTS_3, 24))
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 24);
    // fixup overlaps
    out[outPos + 10] |= ((i0 & 0x3) << 1);
    out[outPos + 21] |= ((i1 & 0x1) << 2);
  }

  public void read323(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = (index >>> 3) * 3;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    int i2 = in.getInt(offset + 8);
    out[outPos] = i0 >>> 29;
    out[outPos + 1] = (i0 >>> 26) & 0x7;
    out[outPos + 2] = (i0 >>> 23) & 0x7;
    out[outPos + 3] = (i0 >>> 20) & 0x7;
    out[outPos + 4] = (i0 >>> 17) & 0x7;
    out[outPos + 5] = (i0 >>> 14) & 0x7;
    out[outPos + 6] = (i0 >>> 11) & 0x7;
    out[outPos + 7] = (i0 >>> 8) & 0x7;

    out[outPos + 8] = (i0 >>> 5) & 0x7;
    out[outPos + 9] = (i0 >>> 2) & 0x7;
    out[outPos + 10] = ((i0 & 0x3) << 1) | (i1 >>> 31);
    out[outPos + 11] = (i1 >>> 28) & 0x7;
    out[outPos + 12] = (i1 >>> 25) & 0x7;
    out[outPos + 13] = (i1 >>> 22) & 0x7;
    out[outPos + 14] = (i1 >>> 19) & 0x7;
    out[outPos + 15] = (i1 >>> 16) & 0x7;

    out[outPos + 16] = (i1 >>> 13) & 0x7;
    out[outPos + 17] = (i1 >>> 10) & 0x7;
    out[outPos + 18] = (i1 >>> 7) & 0x7;
    out[outPos + 19] = (i1 >>> 4) & 0x7;
    out[outPos + 20] = (i1 >>> 1) & 0x7;
    out[outPos + 21] = ((i1 & 0x1) << 2) | (i2 >>> 30);
    out[outPos + 22] = (i2 >>> 27) & 0x7;
    out[outPos + 23] = (i2 >>> 24) & 0x7;

    out[outPos + 24] = (i2 >>> 21) & 0x7;
    out[outPos + 25] = (i2 >>> 18) & 0x7;
    out[outPos + 26] = (i2 >>> 15) & 0x7;
    out[outPos + 27] = (i2 >>> 12) & 0x7;
    out[outPos + 28] = (i2 >>> 9) & 0x7;
    out[outPos + 29] = (i2 >>> 6) & 0x7;
    out[outPos + 30] = (i2 >>> 3) & 0x7;
    out[outPos + 31] = i2 & 0x7;
  }

  public void read32Vector4(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 1;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    int i2 = in.getInt(offset + 8);
    int i3 = in.getInt(offset + 12);
    IntVector LSB = IntVector.broadcast(I256, 0xF);
    var shifts = IntVector.fromArray(I256, SHIFTS_4, 0);
    IntVector.broadcast(I256, i0)
            .lanewise(LSHR, shifts)
            .lanewise(AND, LSB)
            .intoArray(out, outPos);
    IntVector.broadcast(I256, i1)
            .lanewise(LSHR, shifts)
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 8);
    IntVector.broadcast(I256, i2)
            .lanewise(LSHR, shifts)
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 16);
    IntVector.broadcast(I256, i3)
            .lanewise(LSHR, shifts)
            .lanewise(AND, LSB)
            .intoArray(out, outPos + 24);
  }

  public void read324(ByteBuffer in, int index, int[] out, int outPos) {
    int offset = index >>> 1;
    int i0 = in.getInt(offset);
    int i1 = in.getInt(offset + 4);
    int i2 = in.getInt(offset + 8);
    int i3 = in.getInt(offset + 12);
    out[outPos] = i0 >>> 28;
    out[outPos + 1] = (i0 >>> 24) & 0xf;
    out[outPos + 2] = (i0 >>> 20) & 0xf;
    out[outPos + 3] = (i0 >>> 16) & 0xf;
    out[outPos + 4] = (i0 >>> 12) & 0xf;
    out[outPos + 5] = (i0 >>> 8) & 0xf;
    out[outPos + 6] = (i0 >>> 4) & 0xf;
    out[outPos + 7] = i0 & 0xf;
    out[outPos + 8] = i1 >>> 28;
    out[outPos + 9] = (i1 >>> 24) & 0xf;
    out[outPos + 10] = (i1 >>> 20) & 0xf;
    out[outPos + 11] = (i1 >>> 16) & 0xf;
    out[outPos + 12] = (i1 >>> 12) & 0xf;
    out[outPos + 13] = (i1 >>> 8) & 0xf;
    out[outPos + 14] = (i1 >>> 4) & 0xf;
    out[outPos + 15] = i1 & 0xf;
    out[outPos + 16] = i2 >>> 28;
    out[outPos + 17] = (i2 >>> 24) & 0xf;
    out[outPos + 18] = (i2 >>> 20) & 0xf;
    out[outPos + 19] = (i2 >>> 16) & 0xf;
    out[outPos + 20] = (i2 >>> 12) & 0xf;
    out[outPos + 21] = (i2 >>> 8) & 0xf;
    out[outPos + 22] = (i2 >>> 4) & 0xf;
    out[outPos + 23] = i2 & 0xf;
    out[outPos + 24] = i3 >>> 28;
    out[outPos + 25] = (i3 >>> 24) & 0xf;
    out[outPos + 26] = (i3 >>> 20) & 0xf;
    out[outPos + 27] = (i3 >>> 16) & 0xf;
    out[outPos + 28] = (i3 >>> 12) & 0xf;
    out[outPos + 29] = (i3 >>> 8) & 0xf;
    out[outPos + 30] = (i3 >>> 4) & 0xf;
    out[outPos + 31] = i3 & 0xf;
  }
}
