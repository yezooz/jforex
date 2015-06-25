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

package server;

import com.dukascopy.api.IBar;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import signal.Signal;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SendSignal {

    private Server server;
    private Signal signal;
    private SimpleDateFormat date_format;
    private SimpleDateFormat time_format;
    private Date date = null;

    public SendSignal(Signal signal) {
        this.signal = signal;
        this.server = new Server();

        this.date_format = new SimpleDateFormat("yyyy-MM-dd");
        this.time_format = new SimpleDateFormat("HH:mm");
    }

    public void send() {

        IBar bar = signal.getBar();

        if (this.date == null) {
            this.date = new Date(bar.getTime());
        }

        String req = String.format("%s%s?csv=%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%s%s,%s,%s,%s",
                server.getPath(),
                server.getWriterPath(),
                signal.getStrategy(),
                "1",
                signal.getInstrument().name(),
                signal.getTf(),
                (signal.isBearish() ? "SELL" : "BUY"),
                bar.getOpen(),
                bar.getHigh(),
                bar.getLow(),
                bar.getClose(),
                date_format.format(this.date),
                "%20",
                time_format.format(this.date),
                "JForex",
                "0",
                ""
        );

        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(req);

            System.out.println("request " + httpget.getURI());

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpget, responseHandler);

            if (responseBody.startsWith("OK")) {
                // Stored!
            } else {
                System.out.println("FAILED - " + responseBody);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
