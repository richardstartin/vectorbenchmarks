package com.openkappa.panama.vectorbenchmarks;

import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.openkappa.panama.vectorbenchmarks.Util.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector", "-XX:TypeProfileLevel=111",
        "-XX:-TieredCompilation", "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=0"})
public class VerticalSum {

  @State(Scope.Benchmark)
  public static class VerticalSumState {
    @Param({"1024"})
    int size;

    @Param({"0", "0.001", "0.01"})
    double probabilityOfNaN;
  }

  public static void main(String[] args) {
    VerticalSum vs = new VerticalSum();

    System.out.println(Arrays.toString(vs.verticalSum(newState())));
    System.out.println(Arrays.toString(vs.verticalSumPanamaArray(newState())));

    System.out.println(Arrays.toString(vs.verticalSumNaNCheckOptimistic(newState())));
    System.out.println(Arrays.toString(vs.verticalSumPanamaArrayNaNCheckOptimistic(newState())));

    System.out.println(Arrays.toString(vs.verticalSumNaNCheckPessimistic(newState())));
    System.out.println(Arrays.toString(vs.verticalSumPanamaArrayNaNCheckPessimistic(newState())));

  }

  private static VeriticalSumFloatArrayState newState() {
    var state = new VeriticalSumFloatArrayState();
    state.size = 1024;
    state.probabilityOfNaN = 0.1;
    state.init();
    return state;
  }

  public static class VeriticalSumFloatBufferState extends VerticalSumState {
    public ByteBuffer left;
    public ByteBuffer right;
    public ByteBuffer result;

    @Setup(Level.Trial)
    public void init() {
      this.left = newFloatBuffer(size, probabilityOfNaN);
      this.right = newFloatBuffer(size, probabilityOfNaN);
      this.result = newFloatBuffer(size, 0);
    }
  }

  public static class VeriticalSumFloatArrayState extends VerticalSumState {
    public float[] left;
    public float[] right;
    public float[] result;

    @Setup(Level.Trial)
    public void init() {
      this.left = newFloatVector(size, probabilityOfNaN);
      this.right = newFloatVector(size, probabilityOfNaN);
      this.result = newFloatVector(size, 0);
    }
  }

  @Benchmark
  public float[] verticalSumPanamaArray(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; i += YMM_FLOAT.length()) {
      YMM_FLOAT.fromArray(state.left, i)
               .add(YMM_FLOAT.fromArray(state.right, i))
               .intoArray(state.result, i);
    }
    return state.result;
  }

  @Benchmark
  public float[] verticalSumPanamaArrayNaNCheckPessimistic(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; i += YMM_FLOAT.length()) {
      var l = YMM_FLOAT.fromArray(state.left, i);
      var r = YMM_FLOAT.fromArray(state.right, i);
      var mask = l.notEqual(l).or(r.notEqual(r)).not();
      l.add(r).intoArray(state.result, i, mask);
    }
    return state.result;
  }

  @Benchmark
  public float[] verticalSumPanamaArrayNaNCheckOptimistic(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; i += YMM_FLOAT.length()) {
      var l = YMM_FLOAT.fromArray(state.left, i);
      var r = YMM_FLOAT.fromArray(state.right, i);
      l.blend(0f, l.notEqual(l)).add(r.blend(0f, r.notEqual(r))).intoArray(state.result, i);
    }
    return state.result;
  }

  @Benchmark
  public float[] verticalSum(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; ++i) {
      state.result[i] = state.left[i] + state.right[i];
    }
    return state.result;
  }

  @Benchmark
  public float[] verticalSumNaNCheckPessimistic(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; ++i) {
      if (!Float.isNaN(state.left[i]) && !Float.isNaN(state.right[i])) {
        state.result[i] = state.left[i] + state.right[i];
      }
    }
    return state.result;
  }

  @Benchmark
  public float[] verticalSumNaNCheckOptimistic(VeriticalSumFloatArrayState state) {
    for (int i = 0; i < state.size; ++i) {
      boolean leftIsNaN = Float.isNaN(state.left[i]);
      boolean rightIsNaN = Float.isNaN(state.left[i]);
      if (leftIsNaN && !rightIsNaN) {
        state.result[i] = state.right[i];
      } else if (rightIsNaN && !leftIsNaN) {
        state.result[i] = state.left[i];
      } else if (!leftIsNaN) {
        state.result[i] = state.left[i] + state.right[i];
      }
    }
    return state.result;
  }

  @Benchmark
  public ByteBuffer verticalSumBuffer(VeriticalSumFloatBufferState state) {
    for (int i = 0; i < state.size * 2; i += 2) {
      state.result.putFloat(i,  state.left.getFloat(i) + state.right.getFloat(i));
    }
    return state.result;
  }

  @Benchmark
  public ByteBuffer verticalSumBufferNaNCheckPessimistic(VeriticalSumFloatBufferState state) {
    for (int i = 0; i < state.size * 2; i += 2) {
      float l = state.left.getFloat(i);
      float r = state.right.getFloat(i);
      if (!Float.isNaN(l) && !Float.isNaN(r)) {
        state.result.putFloat(i, l + r);
      }
    }
    return state.result;
  }

  @Benchmark
  public ByteBuffer verticalSumBufferNaNCheckOptimistic(VeriticalSumFloatBufferState state) {
    for (int i = 0; i < state.size * 2; i += 2) {
      float l = state.left.getFloat(i);
      float r = state.right.getFloat(i);
      boolean leftIsNaN = Float.isNaN(l);
      boolean rightIsNaN = Float.isNaN(r);
      if (leftIsNaN && !rightIsNaN) {
        state.result.putFloat(i, r);
      } else if (rightIsNaN && !leftIsNaN) {
        state.result.putFloat(i, l);
      } else if (!leftIsNaN) {
        state.result.putFloat(i, l + r);
      }
    }
    return state.result;
  }
}
