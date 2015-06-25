/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Marek Mikuliszyn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package indicator;

import com.dukascopy.api.indicators.*;

public class Pin2 implements IIndicator {

    final private double[][] inputs = new double[1][];
    final private double[][] outputs = new double[1][];
    private IIndicator indMACD;
    /**
     * This Indicator Info
     */
    private IndicatorInfo indicatorInfo;

    /**
     * Input data info
     */
    private InputParameterInfo[] inputParameterInfos;

    /**
     * Output (ie. DOT) paramter info like color
     */
    private OutputParameterInfo[] outputParameterInfos;

    /**
     * Optional Input (time priod, X, etc) parameter info
     */
    private OptInputParameterInfo[] optInputParameterInfos;

    public void onStart(IIndicatorContext context) {
        indMACD = context.getIndicatorsProvider().getIndicator("MACD");
//        indMACD.setOptInputParameter(0, 12);
//        indMACD.setOptInputParameter(1, 29);
//        indMACD.setOptInputParameter(2, 9);

        indicatorInfo = new IndicatorInfo(getName(), getName(), getName(), false, false, false, 1, 0, 1);
        // This is data to be passed in Calculate method
        inputParameterInfos = new InputParameterInfo[]{new InputParameterInfo("Input data", InputParameterInfo.Type.DOUBLE)};

        // Setting Indicator input paramter
        optInputParameterInfos = new OptInputParameterInfo[]{};

        // Number of dots
        outputParameterInfos = new OutputParameterInfo[]{opi1("Color", Color.BLUE)};
    }

    private OutputParameterInfo opi1(String title, Color c) {
        OutputParameterInfo opi = new OutputParameterInfo(title, OutputParameterInfo.Type.DOUBLE, OutputParameterInfo.DrawingStyle.LINE);
        opi.setDrawnByIndicator(false);
        return opi;
    }

    /**
     * Calculate Output parameter, Means x,y value for dot
     * <p/>
     * Start and End Index is given by JForex. We have calculate dot value for
     * each index between start and end index (Bar)
     */
    public IndicatorResult calculate(int startIndex, int endIndex) {
        if (startIndex - getLookback() < 0) {
            startIndex -= startIndex - getLookback();
        }

        if (startIndex > endIndex) {
            return new IndicatorResult(0, 0);
        }

//        indMACD.setOptInputParameter(0, 12);
//        indMACD.setOptInputParameter(1, 29);
//        indMACD.setOptInputParameter(2, 9);

        double[] macdA = new double[endIndex + 1 - indMACD.getLookback()];
        double[] macdA2 = new double[endIndex + 1 - indMACD.getLookback()];
        double[] macdA3 = new double[endIndex + 1 - indMACD.getLookback()];
        indMACD.setInputParameter(0, inputs[0]);
        indMACD.setOutputParameter(0, macdA);
        indMACD.setOutputParameter(1, macdA2);
        indMACD.setOutputParameter(2, macdA3);
        IndicatorResult dMACDResult = indMACD.calculate(startIndex, endIndex);

        int i, k;
        for (i = 0, k = dMACDResult.getNumberOfElements(); i < k; i++) {
            outputs[0][i] = macdA3[i];
        }
        return new IndicatorResult(startIndex, i);
    }


    final public IndicatorInfo getIndicatorInfo() {
        return indicatorInfo;
    }

    /**
     * used to passs values[like open, close, high, low] to calculate method
     */
    final public InputParameterInfo getInputParameterInfo(int index) {
        if (index <= inputParameterInfos.length) {
            return inputParameterInfos[index];
        }
        return null;
    }

    /**
     * Used by Indicator selector dialog to create component to accept input
     * parameter
     */
    final public OptInputParameterInfo getOptInputParameterInfo(int index) {
        if (index <= optInputParameterInfos.length) {
            return optInputParameterInfos[index];
        }
        return null;
    }

    /**
     * Dot info
     */
    final public OutputParameterInfo getOutputParameterInfo(int index) {
        if (index <= outputParameterInfos.length) {
            return outputParameterInfos[index];
        }
        return null;
    }

    /**
     * Set current values for Input (array of OHCL data)
     */
    final public void setInputParameter(int index, Object array) {
        inputs[index] = (double[]) array;
    }

    /**
     * Set output parameter
     */
    final public void setOutputParameter(int index, Object array) {
        outputs[index] = (double[]) array;
    }

    public String getName() {
        return "CF-ffff1";
    }

    /**
     * Used while calculating and drawing dot. How much bars needed to calculate
     * current value
     */
    public int getLookback() {
        return indMACD.getLookback();
    }

    @Override
    public int getLookforward() {
        return 0;
    }

    /**
     * Set Input parameter
     */
    public void setOptInputParameter(int index, Object value) {
    }
}