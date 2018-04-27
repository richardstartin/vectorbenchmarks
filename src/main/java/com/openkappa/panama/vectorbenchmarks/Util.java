package com.openkappa.panama.vectorbenchmarks;

import jdk.incubator.vector.*;

import java.util.concurrent.ThreadLocalRandom;

public class Util {
  public static float[] newFloatVector(int size) {
    float[] vector = new float[size];
    for (int i = 0; i < vector.length; ++i) {
      vector[i] = ThreadLocalRandom.current().nextFloat();
    }
    return vector;
  }

  public static double[] newDoubleVector(int size) {
    double[] vector = new double[size];
    for (int i = 0; i < vector.length; ++i) {
      vector[i] = ThreadLocalRandom.current().nextDouble();
    }
    return vector;
  }

  public static int[] newIntVector(int size) {
    int[] vector = new int[size];
    for (int i = 0; i < vector.length; ++i) {
      vector[i] = ThreadLocalRandom.current().nextInt();
    }
    return vector;
  }

  public static final IntVector.IntSpecies<Shapes.S256Bit> YMM_INT =
          (IntVector.IntSpecies<Shapes.S256Bit>) Vector.species(int.class, Shapes.S_256_BIT);

  public static final FloatVector.FloatSpecies<Shapes.S256Bit> YMM_FLOAT =
          (FloatVector.FloatSpecies<Shapes.S256Bit>) Vector.species(float.class, Shapes.S_256_BIT);

  public static final DoubleVector.DoubleSpecies<Shapes.S256Bit> YMM_DOUBLE =
          (DoubleVector.DoubleSpecies<Shapes.S256Bit>) Vector.species(double.class, Shapes.S_256_BIT);
}
