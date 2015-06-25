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

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IRenkoBar;

public class PinBar extends Signal {

    public PinBar(Instrument instrument, PriceRange priceRange, IBar bar1, IBar bar2, int direction, IContext context) {
        super("", instrument, priceRange, bar1, bar2, direction, context);
    }

    public PinBar(Instrument instrument, PriceRange priceRange, IBar bar, int direction, IContext context) {
        super("", instrument, priceRange, bar, direction, context);
    }

    public PinBar(String strategy, Instrument instrument, PriceRange priceRange, IBar bar1, IBar bar2, int direction, IContext context) {
        super(strategy, instrument, priceRange, bar1, bar2, direction, context);
    }

    public PinBar(String strategy, Instrument instrument, PriceRange priceRange, IBar bar, int direction, IContext context) {
        super(strategy, instrument, priceRange, bar, direction, context);
    }

    public PinBar(Instrument instrument, Period period, IBar bar1, IBar bar2, int direction, IContext context) {
        super("", instrument, period, bar1, bar2, direction, context);
    }

    public PinBar(Instrument instrument, Period period, IBar bar, int direction, IContext context) {
        super("", instrument, period, bar, direction, context);
    }

    public PinBar(String strategy, Instrument instrument, Period period, IBar bar1, IBar bar2, int direction, IContext context) {
        super(strategy, instrument, period, bar1, bar2, direction, context);
    }

    public PinBar(String strategy, Instrument instrument, Period period, IBar bar, int direction, IContext context) {
        super(strategy, instrument, period, bar, direction, context);
    }

    public PinBar(String strategy, Instrument instrument, Period period, IRenkoBar bar, int direction, IContext context) {
        super(strategy, instrument, period, bar, direction, context);
    }
}
