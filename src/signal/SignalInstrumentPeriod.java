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

package signal;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.PriceRange;

public class SignalInstrumentPeriod {

    private Instrument instrument;
    private PriceRange priceRange;
    private Period period;
    private Signal signal;

    public SignalInstrumentPeriod(Instrument instrument, PriceRange priceRange, Signal signal) {
        this.instrument = instrument;
        this.priceRange = priceRange;
        this.signal = signal;
    }

    public SignalInstrumentPeriod(Instrument instrument, Period period, Signal signal) {
        this.instrument = instrument;
        this.period = period;
        this.signal = signal;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public PriceRange getPriceRange() {
        return priceRange;
    }

    public Period getPeriod() {
        return period;
    }

    public Signal getSignal() {
        return signal;
    }

}
