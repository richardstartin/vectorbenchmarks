package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import org.openjdk.jmh.annotations.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.*;
import static jdk.incubator.vector.VectorOperators.NE;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsPrepend = {
        "--add-modules=jdk.incubator.vector",
        "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"
})
public class ByteSearch {

    private static final VarHandle TO_LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    @State(Scope.Benchmark)
    public static class FindByteState {

        @Param({//"8", "16",
                "32", "256", "1024"})
        int size;

        @Param({"7", "8", "9", "10", "11", "12", "13", "14", "15"})
        int logPermutations;

        @Param({"1"})
        int seed = 0;

        int permutations;

        byte[][] data;
        private int i;

        @Setup(Level.Trial)
        public void init() {
            SplittableRandom random = new SplittableRandom(seed);
            permutations = 1 << logPermutations;
            this.data = new byte[permutations][];
            for (int i = 0; i < permutations; ++i) {
                data[i] = new byte[size];
                random.nextBytes(data[i]);
                for (int j = 0; j < size; ++j) {
                    if (data[i][j] == 0) {
                        data[i][j] = 1;
                    }
                }
                data[i][random.nextInt(size - 8, size)] = 0;
            }
        }

        byte[] getData() {
            return data[i++ & (permutations - 1)];
        }
    }

    @Benchmark
    public int vector(FindByteState state) {
        var data = state.getData();
        int offset = 0;
        var holes = IntVector.broadcast(I256, 0x7F7F7F7F);
        var zero = IntVector.zero(I256);
        while (offset < data.length) {
            var vector = ByteVector.fromArray(B256, data, offset).reinterpretAsInts();
            offset += B256.length();
            var tmp = vector.and(holes)
                            .add(holes)
                            .or(vector)
                            .or(holes)
                            .not();
            if (!tmp.eq(zero).allTrue()) {
                var longs = tmp.reinterpretAsLongs();
                for (int i = 0; i < 4; ++i) {
                    long word = longs.lane(i);
                    if (word != 0) {
                        return offset - (i * Long.BYTES + Long.numberOfLeadingZeros(word) >>> 3);
                    }
                }
            }
        }
        return -1;
    }

    @Benchmark
    public int swar(FindByteState state) {
        var data = state.getData();
        int offset = 0;
        while (offset < data.length) {
            int index = firstNonZeroByte(getWord(data, offset));
            offset += Long.BYTES;
            if (index < Long.BYTES) {
                return offset - index - 1;
            }
        }
        return -1;
    }

    @Benchmark
    public int scan(FindByteState state) {
        var data = state.getData();
        for (int i = 0; i < data.length; ++i) {
            if (data[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int firstNonZeroByte(ByteVector vector) {
        var holes = LongVector.broadcast(L256, 0x7F7F7F7F7F7F7F7FL);
        var tmp = vector.reinterpretAsLongs().and(holes).add(holes);
        tmp = tmp.or(vector.reinterpretAsLongs()).or(holes).not();
        if (tmp.compare(NE, LongVector.zero(L256)).anyTrue()) {
            for (int i = 0; i < 4; ++i) {
                long word = tmp.lane(i);
                if (word != 0) {
                    return i * Long.BYTES + Long.numberOfLeadingZeros(word) >>> 3;
                }
            }
        }
        return -1;
    }

    private static int firstNonZeroByte(long word) {
        long tmp = (word & 0x7F7F7F7F7F7F7F7FL) + 0x7F7F7F7F7F7F7F7FL;
        tmp = ~(tmp | word | 0x7F7F7F7F7F7F7F7FL);
        return Long.numberOfLeadingZeros(tmp) >>> 3;
    }

    private static long getWord(byte[] data, int index) {
        return (long) TO_LONG.get(data, index);
    }
}
