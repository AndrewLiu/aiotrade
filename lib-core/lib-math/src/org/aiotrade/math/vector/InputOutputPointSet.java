/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.math.vector;

import java.util.Random;

/**
 * @author Caoyuan Deng
 */
public class InputOutputPointSet {
    
    private double[] inputMeans;
    private double[] inputStdDeviations;
    private boolean[] inputNormalized;
    
    private double[] outputMeans;
    private double[] outputStdDeviations;
    private boolean[] outputNormalized;
    
    private InputOutputPoint[] inputOutputPoints;
    
    public InputOutputPointSet(InputOutputPoint[] iops) {
        this.inputOutputPoints = iops;
        
        int inputDimension = iops[0].input.dimension();
        
        this.inputMeans = new double[inputDimension];
        this.inputStdDeviations = new double[inputDimension];
        this.inputNormalized = new boolean[inputDimension];
        
        int outputDimension = iops[0].output.dimension();
        
        this.outputMeans = new double[outputDimension];
        this.outputStdDeviations = new double[outputDimension];
        this.outputNormalized = new boolean[outputDimension];
    }
    
    public InputOutputPoint[] toArray() {
        return inputOutputPoints;
    }
    
    public InputOutputPoint get(int idx) {
        return inputOutputPoints[idx];
    }
    
    public void set(int idx, InputOutputPoint iop) {
        inputOutputPoints[idx] = iop;
    }
    
    public int size() {
        return inputOutputPoints.length;
    }
    
    public void randomizeOrder() {
        int size = size();
        
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < size; i++) {
            int next = random.nextInt(size - i);
            
            InputOutputPoint iop = inputOutputPoints[next];
            
            inputOutputPoints[next] = inputOutputPoints[i];
            inputOutputPoints[i] = iop;
        }
    }
    
    public InputOutputPointSet cloneWithRandomizedOrder() {
        int size = size();
        
        InputOutputPoint[] newPoints = new InputOutputPoint[size];
        System.arraycopy(inputOutputPoints, 0, newPoints, 0, size);
        
        InputOutputPointSet newSet = new InputOutputPointSet(newPoints);
        
        newSet.inputMeans = new double[inputMeans.length];
        System.arraycopy(inputMeans, 0, newSet.inputMeans, 0, inputMeans.length);
        
        newSet.inputStdDeviations = new double[inputStdDeviations.length];
        System.arraycopy(inputStdDeviations, 0, newSet.inputStdDeviations, 0, inputStdDeviations.length);
        
        newSet.outputMeans = new double[outputMeans.length];
        System.arraycopy(outputMeans, 0, newSet.outputMeans, 0, outputMeans.length);
        
        newSet.outputStdDeviations = new double[outputStdDeviations.length];
        System.arraycopy(outputStdDeviations, 0, newSet.outputStdDeviations, 0, outputStdDeviations.length);
        
        newSet.randomizeOrder();
        
        return newSet ;
    }
    
    /**
     * Normalize values to:
     *   mean: 0
     *   standard deviation: 1
     *   range: about [-1, 1]
     */
    public void normalizeInputs(int dimensionIdx) {
        int num = inputOutputPoints.length;
        
        double[] values = new double[num];
        for (int i = 0; i < num; i++) {
            values[i] = inputOutputPoints[i].input.get(dimensionIdx);
        }
        
        double[] normalized = normalize_ZScore(values);
        
        for (int i = 0; i < num; i++) {
            inputOutputPoints[i].input.set(dimensionIdx, normalized[i]);
        }
        
        inputMeans[dimensionIdx] = normalized[num];
        inputStdDeviations[dimensionIdx] = normalized[num + 1];
        inputNormalized[dimensionIdx] = true;
    }
    
    /**
     * Normalize values to:
     *   mean: 0
     *   standard deviation: 1
     *   range: about [-1, 1]
     *
     * @NOTICE
     * If the output layer uses linear neurons as y = x, the y will be 0 symmetry.
     * the output can be < 0 in same probabilty as > 0, so we should also normalize
     * outputs to [-1, 1] instead of positively?
     *
     * 1. If the hidden neurons' outputs are positive-polarity (such as: LogiSigmoidNeuron)
     * when the mean of initial weights is about 0, the output will be around 0.5,
     * so, we'd better to normalize the outputs to [0, 1], or, with 0.5 mean and 0.5 stdDeviation
     *
     * 2. If the hidden neurons' outputs are double-polarity (such as: TanhSigmoidNeuron) 
     * when the mean of initial weights is about 0, the output will be around 0,
     * so, we'd better to normalize the outputs to [-1, 1], or, with 0 mean and 1 stdDeviation
     *
     * Experience: If normalize ouput to [-1, 1], will cause a slower convergence.
     */
    public void normalizeOutputs(int dimensionIdx) {
        int num = inputOutputPoints.length;
        
        double[] values = new double[num];
        for (int i = 0; i < num; i++) {
            values[i] = inputOutputPoints[i].output.get(dimensionIdx);
        }
        
        double[] normalized = normalize_ZScore(values);
        
        for (int i = 0; i < num; i++) {
            inputOutputPoints[i].output.set(dimensionIdx, normalized[i]);
        }
        
        double mu = normalized[num];
        double sigma = normalized[num + 1];
        outputMeans[dimensionIdx] = normalized[num];
        outputStdDeviations[dimensionIdx] = normalized[num + 1];
        outputNormalized[dimensionIdx] = true;
    }
    
    /**
     * Normalize values to:
     *   mean: 0.5
     *   standard deviation: 0.5
     *   range: about [0, 1]
     */
    public void normalizeOutputsPositively(int dimensionIdx) {
        int num = inputOutputPoints.length;
        
        double[] values = new double[num];
        for (int i = 0; i < num; i++) {
            values[i] = inputOutputPoints[i].output.get(dimensionIdx);
        }
        
        double[] normalized = normalize_ZScore(values);
        
        for (int i = 0; i < num; i++) {
            /** transform to mean: 0.5, standar deviation: 0.5 */
            inputOutputPoints[i].output.set(dimensionIdx, normalized[i] * 0.5 + 0.5);
        }
        
        /**
         * When doing normalize_ZScore(),
         *   y = (x - mu) / sigma
         * Here, we again,
         *   v = y * 0.5 + 0.5
         * So,
         *   v = ((x - mu) / sigma) * 0.5 + 0.5
         *     = ((x - mu) + 0.5 * sigma / 0.5) / (sigma / 0.5)
         *     = (x - (mu - sigma)) / (sigma / 0.5)
         *     = (x - mu') / sigma'
         * where
         *   mu' = mu - sigma
         *   sigma' = sigma / 0.5
         */
        double mu = normalized[num];
        double sigma = normalized[num + 1];
        outputMeans[dimensionIdx] = mu - sigma;
        outputStdDeviations[dimensionIdx] = sigma / 0.5;
        outputNormalized[dimensionIdx] = true;
    }
    
    public void normalizeAllInputs() {
        for (int i = 0, n = get(0).input.dimension(); i < n; i++) {
            normalizeInputs(i);
        }
    }
    
    public void normalizeAllOutputs() {
        for (int i = 0, n = get(0).output.dimension(); i < n; i++) {
            normalizeOutputs(i);
        }
    }
    
    public void normalizePositivelyAllOutputs() {
        for (int dimension = 0, n = get(0).output.dimension(); dimension < n; dimension++) {
            normalizeOutputsPositively(dimension);
        }
    }
    
    public void normalizeInput(Vec input) {
        for (int i = 0, n = input.dimension(); i < n; i++) {
            double value = input.get(i);
            input.set(i, normalizeInput(i, value));
        }
    }
    
    public void normalizeOutput(Vec output) {
        for (int i = 0, n = output.dimension(); i < n; i++) {
            double value = output.get(i);
            output.set(i, normalizeOutput(i, value));
        }
    }
    
    public void normalizePositivelyOutput(Vec output) {
        /** as we have considered the mean and stdDeviation in positive case, it's same as: */
        normalizeOutput(output);
    }
    
    public double normalizeInput(int dimensionIdx, double value) {
        if (inputNormalized[dimensionIdx] == true) {
            return (value - inputMeans[dimensionIdx]) / inputStdDeviations[dimensionIdx];
        } else {
            /** the mean and stdDeviation of this dimensionIdx are not computed yet */
            return value;
        }
    }
    
    public double normalizeOutput(int dimensionIdx, double value) {
        if (outputNormalized[dimensionIdx] == true) {
            return (value - outputMeans[dimensionIdx]) / outputStdDeviations[dimensionIdx];
        } else {
            /** the mean and stdDeviation of this dimensionIdx are not computed yet */
            return value;
        }
    }
    
    public double normalizePositivelyOutput(int dimensionIdx, double value) {
        /** as we have considered the mean and stdDeviation in positive case, it's same as: */
        return normalizeOutput(dimensionIdx, value);
    }
    
    public void revertInput(Vec input) {
        for (int i = 0, n = input.dimension(); i < n; i++) {
            double value = input.get(i);
            
            value = value * inputStdDeviations[i] + inputMeans[i];
            input.set(i, value);
        }
    }
    
    public void revertOutput(Vec output) {
        for (int i = 0, n = output.dimension(); i < n; i++) {
            double value = output.get(i);
            
            value = value * outputStdDeviations[i] + outputMeans[i];;
            output.set(i, value);
        }
    }
    
    
    public double revertInput(int dimensionIdx, double value) {
        return value * inputStdDeviations[dimensionIdx] + inputMeans[dimensionIdx];
    }
    
    
    public double revertOutput(int dimensionIdx, double value) {
        return value * outputStdDeviations[dimensionIdx] + outputMeans[dimensionIdx];
    }
    
    /**
     * Normalize values to:
     *   mean: 0
     *   standard deviation: 1
     *   range: about [-1, 1]
     */
    private double[] normalize_ZScore(double[] values) {
        int num = values.length;
        
        /** compute mean value */
        double sum = 0;
        for (int i = 0; i < num; i++) {
            sum += values[i];
        }
        double mean = sum / (double)num;
        
        /** compute standard deviation */
        double deviation_square_sum = 0;
        for (int i = 0; i < num; i++) {
            double deviation = values[i] - mean;
            deviation_square_sum += deviation * deviation;
        }
        double stdDeviation = Math.sqrt(deviation_square_sum / (double)num);
        
        System.out.println("Mean: " + mean + " Standard Deviation: " + stdDeviation);
        
        if (stdDeviation == 0) {
            stdDeviation = 1;
        }
        
        /**
         * do 'Z Score' normalization.
         * 2 more dimensions are added to store mean and stdDeviation
         */
        double[] normalized = new double[num + 2];
        for (int i = 0; i < num; i++) {
            normalized[i] = (values[i] - mean) / stdDeviation;
        }
        
        normalized[num] = mean;
        normalized[num + 1] = stdDeviation;
        
        return normalized;
    }
    
    /**
     * y = (0.9 - 0.1) / (xmax - xmin) * x + (0.9 - (0.9 - 0.1) / (xmax - xmin) * xmax)
     *   = 0.8 / (xmax - xmin) * x + (0.9 - 0.8 / (xmax - xmin) * xmax)
     */
    private double[] normalizePositively_MinMax(double[] values) {
        int num = values.length;
        
        /** compute min max value */
        double min = +Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < num; i++) {
            double value = values[i];
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        
        double mean = min;
        
        double stdDeviation = max - min;
        
        System.out.println("normOutput: " + mean + " deviationOutput: " + stdDeviation);
        
        /** do 'min max' normalization */
        double[] normalized = new double[num + 2];
        for (int i = 0; i < num; i++) {
            normalized[i] = (values[i] - mean) / stdDeviation;
        }
        
        normalized[num] = mean;
        normalized[num + 1] = stdDeviation;
        
        return normalized;
    }
    
    private double[] normalizePositively_CustomMinMax(double[] values) {
        int num = values.length;
        
        /** compute max min value */
        double max = 30000;
        double min = 0;
        
        double mean = min;
        
        double stdDeviation = max - min;
        
        /** do 'maxmin' standardization */
        double[] normalized = new double[num + 2];
        for (int i = 0; i < num; i++) {
            normalized[i] = (values[i] - mean) / stdDeviation;
        }
        
        normalized[num] = mean;
        normalized[num + 1] = stdDeviation;
        
        return normalized;
    }
}
