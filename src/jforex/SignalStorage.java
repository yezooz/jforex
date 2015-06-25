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

package jforex;

import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import helper.Helpers;
import helper.Helpers.DIRECTION;
import server.SendSignal;
import signal.Signal;

import java.util.Date;

public class SignalStorage {

    private Date date;
    private Instrument pair;
    private Period tf;
    private IBar bar;
    private DIRECTION dir;

    private boolean isPin = false;
    private boolean isPerfectPin = false;
    private boolean isPinDiv = false;
    private boolean isMarubozu = false;
    private boolean isClosingMarubozu = false;
    private boolean isDblMarubozu = false;
    private boolean isDblClosingMarubozu = false;
    private boolean isStar = false;
    private boolean isEngulfing = false;
    private boolean isPinMW = false;
    private boolean isDblMarubozuMW = false;
    private boolean isDblPin = false;
    private boolean isTripleMarubozu = false;
    private boolean isTripleClosingMarubozu = false;
    private boolean isPinPin = false;

    public SignalStorage(Instrument pair, Period tf, IBar bar, Date date) {
        this.date = date;
        this.bar = bar;
        this.pair = pair;
        this.tf = tf;
    }

    public void setDirection(DIRECTION dir) {
        this.dir = dir;
    }

    public void setDirection(int dir) {
        if (dir == 100)
            this.dir = DIRECTION.BUY;
        else
            this.dir = DIRECTION.SELL;
    }

    public void store() {

        Signal s = null;
        SendSignal ss = null;

        if (this.isPin) {
            s = new Signal("pin", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isPerfectPin) {
            s = new Signal("pin_perfect", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isPinDiv) {
            s = new Signal("pin_divergence", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isPinPin) {
            s = new Signal("pin_pin", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isMarubozu) {
            s = new Signal("marubozu", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        } else if (this.isClosingMarubozu) {
            s = new Signal("closingmarubozu", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isDblMarubozu) {
            s = new Signal("double_m", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isDblClosingMarubozu) {
            s = new Signal("double_cm", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isStar) {
            s = new Signal("star", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isEngulfing) {
            s = new Signal("engulfing", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isPinMW) {
            s = new Signal("pin_mw", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isDblMarubozuMW) {
            s = new Signal("dblm_mw", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isDblPin) {
            s = new Signal("dbl_pin", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isTripleMarubozu) {
            s = new Signal("triple_m", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }

        if (this.isTripleClosingMarubozu) {
            s = new Signal("triple_cm", this.pair, this.tf, this.bar, Helpers.directionToInt(dir), null);
            ss = new SendSignal(s);
            ss.setDate(date);
            ss.send();
        }
    }

    public void isPin() {
        this.isPin = true;
    }

    public void isPerfectPin() {
        this.isPerfectPin = true;
    }

    public void isMarubozu() {
        this.isMarubozu = true;
    }

    public void isClosingMarubozu() {
        this.isClosingMarubozu = true;
    }

    public void isDblMarubozu() {
        this.isDblMarubozu = true;
    }

    public void isDblClosingMarubozu() {
        this.isDblClosingMarubozu = true;
    }

    public void isStar() {
        this.isStar = true;
    }

    public void isEngulfing() {
        this.isEngulfing = true;
    }

    public void isPinMW() {
        this.isPinMW = true;
    }

    public void isPinDiv() {
        this.isPinDiv = true;
    }

    public void isPinPin() {
        this.isPinPin = true;
    }

    public void isDblMarubozuMW() {
        this.isDblMarubozuMW = true;
    }

    public void isDblPin() {
        this.isDblPin = true;
    }

    public void isTripleClosingMarubozu() {
        this.isTripleClosingMarubozu = true;
    }

    public void isTripleMarubozu() {
        this.isTripleMarubozu = true;
    }
}
