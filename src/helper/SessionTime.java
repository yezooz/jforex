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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SessionTime {

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    Calendar cal = Calendar.getInstance();

    Date summerStart;
    Date summerEnd;

    public SessionTime() {

        try {
            summerStart = format.parse("2013-03-31");
            summerEnd = format.parse("2013-10-27");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public int getLondonStartHour(Date date) {
        if (date.before(summerStart) || date.after(summerEnd)) {
            return 8;
        }
        return 9;
    }

    public int getLondonEndHour(Date date) {
        if (date.before(summerStart) || date.after(summerEnd)) {
            return 17;
        }
        return 18;
    }

    public boolean inUKSession(Date date) {
        cal.setTime(date);
        if (cal.get(Calendar.HOUR_OF_DAY) >= getLondonEndHour(date) || cal.get(Calendar.HOUR_OF_DAY) < getLondonStartHour(date)) {
            return false;
        }

        return true;
    }


}
