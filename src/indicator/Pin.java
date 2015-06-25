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
import signal.GBar;

import java.util.Date;

enum Mode {
    NO_FILTER,
    EVERY_PIN,
    WITH_CONFIRMATION,
    DOWN_PIN_UP,
    DOUBLE_PIN
};

@RequiresFullAccess
@Library("c:/libs/lib.jar")
public class Pin implements IIndicator {
    private IndicatorInfo indicatorInfo;
    private InputParameterInfo[] inputParameterInfos;
    private OptInputParameterInfo[] optInputParameterInfos;
    private OutputParameterInfo[] outputParameterInfos;
    private double[][][] inputs = new double[1][][];
    private int[][] outputs = new int[1][];
    private int[][] macd = new int[1][];
    private IIndicator macdIndicator;
    private IIndicatorContext context;

    private Mode useMode = Mode.DOWN_PIN_UP;
    private boolean perfectPin = true;
    private double pinBodyToCandle = .25;
    private double pinShortToBody = .15;
    private boolean withDivergence = true;

    public void onStart(IIndicatorContext context) {
        indicatorInfo = new IndicatorInfo("Pin", "Pin", "Pattern Recognition", true, false, true, 1, 5, 1);
        inputParameterInfos = new InputParameterInfo[]{new InputParameterInfo("Price", InputParameterInfo.Type.PRICE)};
        optInputParameterInfos = new OptInputParameterInfo[]{
                new OptInputParameterInfo("Mode (0=NF, 1=All, 2=w/Conf, 3=Star, 4=DBL)", OptInputParameterInfo.Type.OTHER, new IntegerRangeDescription(1, 0, 4, 1)),
                new OptInputParameterInfo("pinBodyToCandle", OptInputParameterInfo.Type.OTHER, new IntegerRangeDescription(25, 0, 100, 5)),
                new OptInputParameterInfo("pinShortToBody", OptInputParameterInfo.Type.OTHER, new IntegerRangeDescription(15, 0, 100, 5)),
                new OptInputParameterInfo("Perfect Pin", OptInputParameterInfo.Type.OTHER, new IntegerRangeDescription(1, 0, 1, 1)),
                new OptInputParameterInfo("With Divergence", OptInputParameterInfo.Type.OTHER, new IntegerRangeDescription(1, 0, 1, 1)),
        };
        outputParameterInfos = new OutputParameterInfo[]{
                new OutputParameterInfo("Pin", OutputParameterInfo.Type.INT, OutputParameterInfo.DrawingStyle.PATTERN_BULL_BEAR),
        };

        IIndicatorsProvider indicatorsProvider = context.getIndicatorsProvider();
        macdIndicator = indicatorsProvider.getIndicator("MACD");

        this.context = context;
    }

    public IndicatorResult calculate(int startIndex, int endIndex) {
        //calculating startIndex taking into account lookback value
        if (startIndex - getLookback() < 0) {
            startIndex -= startIndex - getLookback();
        }
        if (startIndex > endIndex) {
            return new IndicatorResult(0, 0);
        }

        double[] macdOutput = new double[endIndex - startIndex + 1];
        macdIndicator.setInputParameter(0, new Object[]{12, 26, 9});
        macdIndicator.setInputParameter(1, 26);
        macdIndicator.setInputParameter(2, 3);
        macdIndicator.setOutputParameter(0, macdOutput);
        macdIndicator.calculate(startIndex, endIndex);

        // Inputs: 0 open, 1 close, 2 high, 3 low, 4 volume
        int i, j;
        for (i = startIndex, j = 0; i <= endIndex; i++, j++) {
            if (useMode == Mode.NO_FILTER ||
                    useMode == Mode.EVERY_PIN ||
                    useMode == Mode.DOUBLE_PIN) checkForPin(i, j);
            else if (useMode == Mode.WITH_CONFIRMATION) checkForPinWithConfirmation(i, j);
            else if (useMode == Mode.DOWN_PIN_UP) checkForDownPinUp(i, j);
        }

        return new IndicatorResult(startIndex, j);
    }

    private int isPin(int i) {
        boolean isPin = false;
        int pinDir = 0;

        GBar bar = new GBar(inputs[0][2][i], inputs[0][3][i], inputs[0][0][i], inputs[0][1][i], new Date().getTime());

        if (bar.bodySize() <= bar.candleSize() * pinBodyToCandle) {
            if (bar.getHigh() - bar.getOpen() <= bar.candleSize() * pinShortToBody || bar.getHigh() - bar.getClose() <= bar.candleSize() * pinShortToBody) {
                isPin = true;
                pinDir = 1;
            }
            if (bar.getOpen() - bar.getLow() <= bar.candleSize() * pinShortToBody || bar.getClose() - bar.getLow() <= bar.candleSize() * pinShortToBody) {
                isPin = true;
                pinDir = -1;
            }
        }

        GBar bar2 = new GBar(inputs[0][2][i - 1], inputs[0][3][i - 1], inputs[0][0][i - 1], inputs[0][1][i - 1], new Date().getTime());

        if (perfectPin) {
            if (pinDir == 1 && bar2.getOpen() > bar2.getClose() && bar.getOpen() < bar.getClose() && bar.getOpen() >= bar2.getClose() && bar.getHigh() < bar2.getOpen()) {
            } else if (pinDir == -1 && bar2.getOpen() < bar2.getClose() && bar.getOpen() > bar.getClose() && bar.getOpen() <= bar2.getClose() && bar.getLow() > bar2.getOpen()) {
            } else {
                isPin = false;
            }
        }

        if (isPin && withDivergence) {
            if (pinDir == 1 && isBullishDivergence(i)) {
                return 1;
            } else if (pinDir == -1 && isBearishDivergence(i)) {
                return -1;
            }
            return 0;
        }

        if (!isPin) return 0;
        return pinDir;
    }

    private void checkForPin(int i, int j) {
        int pinDir = isPin(i);

        if (useMode == Mode.NO_FILTER) {
            if (pinDir == 1)
                outputs[0][j] = 100;
            else if (pinDir == -1)
                outputs[0][j] = -100;

            return;
        }

        if (useMode == Mode.DOUBLE_PIN) {
            int pinDir2 = isPin(i - 1);

            if (pinDir == 1 && pinDir2 == 1) {
                outputs[0][j] = 100;
            } else if (pinDir == -1 && pinDir2 == -1) {
                outputs[0][j] = -100;
            }

            return;
        }

        if (pinDir == 1 && inputs[0][3][i] < inputs[0][3][i - 1] && inputs[0][3][i] < inputs[0][3][i - 2])
            outputs[0][j] = 100;
        else if (pinDir == -1 && inputs[0][2][i] > inputs[0][2][i - 1] && inputs[0][2][i] > inputs[0][2][i - 2])
            outputs[0][j] = -100;
    }

    private void checkForPinWithConfirmation(int i, int j) {
        int pinDir = isPin(i - 1);

        if (pinDir == 1 && inputs[0][0][i] < inputs[0][1][i])
            outputs[0][j] = 100;
        else if (pinDir == -1 && inputs[0][0][i] > inputs[0][1][i])
            outputs[0][j] = -100;
    }

    private void checkForDownPinUp(int i, int j) {
        int pinDir = pinDir = isPin(i - 1);
        if (perfectPin) {
            perfectPin = false;
            pinDir = isPin(i - 1);

            double max_ratio = 0.75;

            double high_1 = inputs[0][2][i];
            double low_1 = inputs[0][3][i];
            double open_1 = inputs[0][0][i];
            double close_1 = inputs[0][1][i];
            double size_1 = Math.abs(high_1 - low_1);
            double body_1 = Math.abs(open_1 - close_1);

            double high_3 = inputs[0][2][i - 2];
            double low_3 = inputs[0][3][i - 2];
            double open_3 = inputs[0][0][i - 2];
            double close_3 = inputs[0][1][i - 2];
            double size_3 = Math.abs(high_3 - low_3);
            double body_3 = Math.abs(open_3 - close_3);

            if (body_1 / size_1 < max_ratio) {
                pinDir = 0;
            }
            if (body_3 / size_3 < max_ratio) {
                pinDir = 0;
            }

            perfectPin = true;
        }

        if (pinDir == 1 && inputs[0][0][i - 2] > inputs[0][1][i - 2] && inputs[0][0][i] < inputs[0][1][i])
            outputs[0][j] = 100;
        else if (pinDir == -1 && inputs[0][0][i - 2] < inputs[0][1][i - 2] && inputs[0][0][i] > inputs[0][1][i])
            outputs[0][j] = -100;
    }

    private boolean isBullishDivergence(int shift) {
        if (!isDip(shift)) return false;

        int lastDip = getLastDip(shift);

        if (macd[0][shift] > macd[0][lastDip] && inputs[0][3][shift] < inputs[0][3][lastDip]) {
            return true;
        }

        return false;

    }

    private boolean isBearishDivergence(int shift) {
        if (!isPeak(shift)) return false;

        int lastPeak = getLastPeak(shift);

        if (macd[0][shift] < macd[0][lastPeak] && inputs[0][2][shift] > inputs[0][2][lastPeak]) {
            return true;
        }

        return false;
    }

    private boolean isPeak(int shift) {
        if (macd[1][shift] >= macd[1][shift + 1] && macd[1][shift + 1] >= macd[1][shift + 2]) {
            return true;
        }
        return false;
    }

    private boolean isDip(int shift) {
        if (macd[1][shift] <= macd[1][shift + 1] && macd[1][shift + 1] <= macd[1][shift + 2]) {
            return true;
        }
        return false;
    }

    private int getLastPeak(int shift) {
        for (int i = shift + 5; i < shift + 300; i++) {
            if (macd[1][i] >= macd[1][i + 1] && macd[1][i] >= macd[1][i + 2] &&
                    macd[1][i] >= macd[1][i - 1] && macd[1][i] >= macd[1][i - 2]) {
                for (int j = i; j < shift + 300; j++) {
                    if (macd[0][j] >= macd[0][j + 1] && macd[0][j] > macd[0][j + 2] &&
                            macd[0][j] >= macd[0][j - 1] && macd[0][j] > macd[0][j - 2])
                        return j;
                }
            }
        }

        return 0;
    }

    private int getLastDip(int shift) {
        for (int i = shift + 5; i < shift + 300; i++) {
            if (macd[1][i] <= macd[1][i + 1] && macd[1][i] <= macd[1][i + 2] &&
                    macd[1][i] <= macd[1][i - 1] && macd[1][i] <= macd[1][i - 2]) {
                for (int j = i; j < shift + 300; j++) {
                    if (macd[0][j] <= macd[0][j + 1] && macd[0][j] < macd[0][j + 2] &&
                            macd[0][j] <= macd[0][j - 1] && macd[0][j] < macd[0][j - 2])
                        return j;
                }
            }
        }

        return 0;
    }

    public IndicatorInfo getIndicatorInfo() {
        return indicatorInfo;
    }

    public InputParameterInfo getInputParameterInfo(int index) {
        if (index <= inputParameterInfos.length) {
            return inputParameterInfos[index];
        }
        return null;
    }

    public int getLookback() {
        return 3;
    }

    public int getLookforward() {
        return 0;
    }

    public OptInputParameterInfo getOptInputParameterInfo(int index) {
        if (index <= optInputParameterInfos.length) {
            return optInputParameterInfos[index];
        }
        return null;
    }

    public OutputParameterInfo getOutputParameterInfo(int index) {
        if (index <= outputParameterInfos.length) {
            return outputParameterInfos[index];
        }
        return null;
    }

    public void setInputParameter(int index, Object array) {
        inputs[index] = (double[][]) array;
    }

    public void setOptInputParameter(int index, Object value) {
        int v = (Integer) value;

        if (index == 0) {
            if (v == 0) useMode = Mode.NO_FILTER;
            else if (v == 1) useMode = Mode.EVERY_PIN;
            else if (v == 2) useMode = Mode.WITH_CONFIRMATION;
            else if (v == 3) useMode = Mode.DOWN_PIN_UP;
            else if (v == 4) useMode = Mode.DOUBLE_PIN;

        } else {
            if (index == 1) {
                pinBodyToCandle = (double) v / 100;
            }
            if (index == 2) {
                pinShortToBody = (double) v / 100;
            }
            if (index == 3) {
                if (v == 1)
                    this.perfectPin = true;
                else
                    this.perfectPin = false;
            }
            if (index == 4) {
                if (v == 1)
                    this.withDivergence = true;
                else
                    this.withDivergence = false;
            }
        }
    }

    public void setOutputParameter(int index, Object array) {
        outputs[index] = (int[]) array;
    }
}
