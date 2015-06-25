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

package helper;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.drawings.IRectangleChartObject;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IRangeBar;
import trade.TradeManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Helpers {

    public static final SimpleDateFormat sdf_sql = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static List<Period> TFs() {
        List<Period> list = new ArrayList<Period>();
        list.add(Period.createCustomPeriod(Unit.Minute, 5));
        list.add(Period.createCustomPeriod(Unit.Minute, 6));
        list.add(Period.createCustomPeriod(Unit.Minute, 8));
        list.add(Period.createCustomPeriod(Unit.Minute, 10));
        list.add(Period.createCustomPeriod(Unit.Minute, 12));
        list.add(Period.createCustomPeriod(Unit.Minute, 15));
        list.add(Period.createCustomPeriod(Unit.Minute, 16));
        list.add(Period.createCustomPeriod(Unit.Minute, 18));
        list.add(Period.createCustomPeriod(Unit.Minute, 20));
        list.add(Period.createCustomPeriod(Unit.Minute, 24));
        list.add(Period.createCustomPeriod(Unit.Minute, 30));
        list.add(Period.createCustomPeriod(Unit.Minute, 32));
        list.add(Period.createCustomPeriod(Unit.Minute, 36));
        list.add(Period.createCustomPeriod(Unit.Minute, 40));
        list.add(Period.createCustomPeriod(Unit.Minute, 45));
        list.add(Period.createCustomPeriod(Unit.Minute, 48));
        list.add(Period.createCustomPeriod(Unit.Hour, 1));
        list.add(Period.createCustomPeriod(Unit.Hour, 2));
        list.add(Period.createCustomPeriod(Unit.Hour, 3));
        list.add(Period.createCustomPeriod(Unit.Hour, 4));
        list.add(Period.createCustomPeriod(Unit.Hour, 6));
        list.add(Period.createCustomPeriod(Unit.Hour, 8));
        list.add(Period.createCustomPeriod(Unit.Day, 1));
        list.add(Period.createCustomPeriod(Unit.Day, 2));
        list.add(Period.createCustomPeriod(Unit.Day, 3));
        list.add(Period.createCustomPeriod(Unit.Day, 4));
        list.add(Period.createCustomPeriod(Unit.Day, 5));
        list.add(Period.createCustomPeriod(Unit.Day, 6));
        list.add(Period.createCustomPeriod(Unit.Week, 1));
        list.add(Period.createCustomPeriod(Unit.Week, 2));
        list.add(Period.createCustomPeriod(Unit.Week, 3));
        list.add(Period.createCustomPeriod(Unit.Week, 4));

        return list;
    }

    public static DIRECTION getCandleDirection(IBar bar) {
        if (bar.getOpen() == bar.getClose()) return DIRECTION.DOJI;
        if (bar.getOpen() < bar.getClose()) return DIRECTION.BUY;
        return DIRECTION.SELL;
    }

    public static int directionToInt(DIRECTION dir) {
        if (dir == DIRECTION.BUY)
            return 1;
        else if (dir == DIRECTION.SELL)
            return -1;
        return 0;
    }

    public static boolean isBetweenValues(double value, double high, double low) {
        if (low > high) {
            double temp = low;
            low = high;
            high = temp;
        }

        return (value <= high && value >= low);
    }

    public static boolean areBetweenValues(double h1, double l1, double h2, double l2) {
        return isBetweenValues(h1, h2, l2) || isBetweenValues(l1, h2, l2) || isBetweenValues(h2, h1, l1) || isBetweenValues(l2, h1, l1);
    }

    public static double getCandleShadowSize(IBar bar, boolean topPart) {
        double shadow;
        double candle = bar.getHigh() - bar.getLow();
        DIRECTION dir = getCandleDirection(bar);

        if (dir == DIRECTION.BUY && topPart) shadow = bar.getHigh() - bar.getClose();
        else if (dir != DIRECTION.BUY && topPart) shadow = bar.getHigh() - bar.getOpen();
        else if (dir == DIRECTION.BUY && !topPart) shadow = bar.getClose() - bar.getLow();
        else if (dir != DIRECTION.BUY && !topPart) shadow = bar.getOpen() - bar.getLow();
        else return 0;

        return (shadow / candle);
    }

    public static boolean isMomentumBar(IBar bar) {
        double body = Math.abs(bar.getOpen() - bar.getClose());
        double candle = bar.getHigh() - bar.getLow();

        return (body / candle > 0.8);


//	   if (getCandleDirection(bar) == DIRECTION.DOJI) return false;

//	   DIRECTION dir = getCandleDirection(bar);

//	   IIndicator atr = context.getIndicators().getIndicator("ATR");

	   /*
       if (getCandleSizeToATR(i) > customMomentumATR && (MathAbs(Open[i] - Close[i]) / (High[i] - Low[i])) >= momentumBodyToCandle) {}
	   else return (false);

	   // check left side
	   for (int j=1; j<=10; j++)
	   {
	      if ((direction == -1 && Low[i] >= Low[i+j]) || (direction == 1 && High[i] <= High[i+j])) return (false);
	   }
	   */
    }

    public static void drawRectangle(IChart chart, long time1, double price1, long time2, double price2) {
        drawRectangle(chart, time1, price1, time2, price2, Color.YELLOW, "");
    }

    public static void drawRectangle(IChart chart, long time1, double price1, long time2, double price2, Color color) {
        drawRectangle(chart, time1, price1, time2, price2, color, "");
    }

    public static void drawRectangle(IChart chart, long time1, double price1, long time2, double price2, Color color, String label) {
        if (chart == null) return;

        String key = "rectangle_" + UUID.randomUUID().toString().replace('-', '0');
        IRectangleChartObject rectangle = chart.getChartObjectFactory().createRectangle(key, time1, price1, time2, price2);
        rectangle.setColor(color);
        rectangle.setFillColor(Color.GRAY);
        rectangle.setFillOpacity((float) 0.3);
        rectangle.setText(label);
        chart.add(rectangle);
    }

    public static void drawRectangle(IChart chart, IRangeBar bar, Color color) {
        if (chart == null) return;

        String key = "rectangle_" + UUID.randomUUID().toString().replace('-', '0');
        IRectangleChartObject rectangle = chart.getChartObjectFactory().createRectangle(key, bar.getTime(), bar.getHigh(), bar.getEndTime(), bar.getLow());
        rectangle.setColor(color);
        rectangle.setFillColor(color);
        chart.add(rectangle);
    }

    public static List<Double> getZZPoints(IContext context, IFeedDescriptor feedDescriptor, long time) {
        return getZZPoints(context, feedDescriptor, time, 50);
    }

    public static List<Double> getZZPoints(IContext context, IFeedDescriptor feedDescriptor, long time, int limit) {
        return getZZPoints(context, feedDescriptor, time, limit, TradeManager.getInstance().getZZSettings());
    }

    public static List<Double> getZZPoints(IContext context, IFeedDescriptor fD, long time, int limit, Object[] zzSettings) {
        List<Double> zz = new ArrayList<Double>();

        try {
            double zigzag[] = context.getIndicators().zigzag(fD.getInstrument(), fD.getPeriod(), OfferSide.BID, (Integer) zzSettings[0], (Integer) zzSettings[1], (Integer) zzSettings[2], Filter.ALL_FLATS, limit, time, 0);

            for (int i = 0; i < zigzag.length; i++) {
                if (!Double.isNaN(zigzag[i])) {
                    zz.add(zigzag[i]);
                }
            }

            Collections.reverse(zz);
        } catch (JFException e) {
            e.printStackTrace();
        }

        return zz;
    }

    public static List<Double> getZZPoints(IContext context, Instrument instrument, Period period, long time) {
        return getZZPoints(context, instrument, period, time, 50);
    }

    public static List<Double> getZZPoints(IContext context, Instrument instrument, Period period, long time, int limit) {
        return getZZPoints(context, instrument, period, time, limit, TradeManager.getInstance().getZZSettings());
    }

    public static List<Double> getZZPoints(IContext context, Instrument instrument, Period period, long time, int limit, Object[] zzSettings) {

        List<Double> zz = new ArrayList<Double>();

        try {
            Object[] zzFeed = context.getIndicators().calculateIndicator(instrument, period, new OfferSide[]{OfferSide.BID}, "ZIGZAG", new AppliedPrice[]{AppliedPrice.CLOSE}, zzSettings, Filter.ALL_FLATS, limit, time, 0);
            for (double output : (double[]) zzFeed[0]) {
                Double o = new Double(output);
                if (!o.isNaN())
                    zz.add(o);
            }

            Collections.reverse(zz);

            if (zz.size() < 2) return new ArrayList<Double>();
        } catch (JFException e) {
            e.printStackTrace();
        }

        return zz;
    }

    public static double[] getUpperShadow(IBar bar) {
        if (Helpers.getCandleDirection(bar) == DIRECTION.BUY) {
            return new double[]{bar.getHigh(), bar.getClose()};
        }
        return new double[]{bar.getHigh(), bar.getOpen()};
    }

    public static double[] getLowerShadow(IBar bar) {
        if (Helpers.getCandleDirection(bar) == DIRECTION.BUY) {
            return new double[]{bar.getOpen(), bar.getLow()};
        }
        return new double[]{bar.getClose(), bar.getLow()};
    }

    public static double getUpperShadowProportion(IBar bar) {
        if (Helpers.getCandleDirection(bar) == DIRECTION.BUY) {
            return (bar.getHigh() - bar.getClose()) / (bar.getHigh() - bar.getLow());
        }
        return (bar.getHigh() - bar.getOpen()) / (bar.getHigh() - bar.getLow());
    }

    public static double getLower(double v1, double v2) {
        return (v1 < v2 ? v1 : v2);
    }

    public static double getHigher(double v1, double v2) {
        return (v1 > v2 ? v1 : v2);
    }

    public static double getLowerShadowProportion(IBar bar) {
        if (Helpers.getCandleDirection(bar) == DIRECTION.BUY) {
            return (bar.getOpen() - bar.getLow()) / (bar.getHigh() - bar.getLow());
        }
        return (bar.getClose() - bar.getLow()) / (bar.getHigh() - bar.getLow());
    }

    public static Date dateFromString(String string) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getKey(String str) {
        return str + UUID.randomUUID().toString().replace('-', '0');
    }

    public static String arrayToString(List<Double> arr) {
        String str = "";
        for (int r = 0; r < arr.size(); r++) {
            str += String.format("[%s] %.5f; ", r, arr.get(r));
        }
        return str;
    }

    public static String arrayToString(double[] arr) {
        String str = "";
        for (int r = 0; r < arr.length; r++) {
            str += String.format("[%s] %.5f; ", r, arr[r]);
        }
        return str;
    }

    public static String tfToString(Period tf) {
        return tf.getUnit().name().charAt(0) + "" + tf.getNumOfUnits();
    }

    public static Period StringToTF(String tf) {
        Unit unit = null;
        int num = Integer.parseInt(tf.substring(1, tf.length()));

        if (tf.startsWith("W")) {
            unit = Unit.Week;
        } else if (tf.startsWith("D")) {
            unit = Unit.Day;
        } else if (tf.startsWith("H")) {
            unit = Unit.Hour;
        } else if (tf.startsWith("M")) {
            unit = Unit.Minute;
        } else {
            return null;
        }

        return Period.createCustomPeriod(unit, num);
    }

    public static void print(Object txt) {
        System.out.println(txt);
    }

    public static void print(IBar txt) {
        System.out.println(sdf_sql.format(new Date(txt.getTime())) + " " + txt.toString());
    }

    public static IBar findBarAtZZ(int i, IBar bar, IContext context, IFeedDescriptor fD) {
        List<Double> zz = Helpers.getZZPoints(context, fD, bar.getTime(), 300, new Object[]{12, 5, 3});

        return Helpers.findBarAtZZ(i, bar, context, fD, zz);
    }

    public static IBar findBarAtZZ(int i, IBar bar, IContext context, IFeedDescriptor fD, List<Double> zz) {

        List<IBar> bars = new ArrayList<IBar>();
        try {
            bars = context.getHistory().getBars(fD.getInstrument(), fD.getPeriod(), OfferSide.BID, Filter.ALL_FLATS, 300, bar.getTime(), 0);

            Collections.reverse(bars);
            for (IBar b : bars) {
                if (zz.get(i - 1) > zz.get(i) && b.getLow() == zz.get(i)) {
                    return b;
                }
                if (zz.get(i - 1) < zz.get(i) && b.getHigh() == zz.get(i)) {
                    return b;
                }
            }
        } catch (JFException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String formatDate(Date date) {
        return sdf.format(date);
    }

    public static String formatDate(long date) {
        return formatDate(new Date(date));
    }

    public static String formatDateTime(Date date) {
        return sdf_sql.format(date);
    }

    public static String formatDateTime(long date) {
        return formatDateTime(new Date(date));
    }

    public static Period highestTF(List<Period> list) {
        int h = 0;
        for (Period tf : list) {
            if (Helpers.TFs().indexOf(tf) > h) {
                h = Helpers.TFs().indexOf(tf);
            }
        }

        return Helpers.TFs().get(h);
    }

    public static Period lowestTF(List<Period> list) {
        int l = Helpers.TFs().size() - 1;
        for (Period tf : list) {
            if (Helpers.TFs().indexOf(tf) < l) {
                l = Helpers.TFs().indexOf(tf);
            }
        }

        return Helpers.TFs().get(l);
    }

    public static enum DIRECTION {BUY, SELL, DOJI}
}
