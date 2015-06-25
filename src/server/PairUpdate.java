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

import com.dukascopy.api.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class PairUpdate {

    public static void send(Instrument pair, IContext context) {

        Server server = new Server();

        HttpClient httpclient = new DefaultHttpClient();
        try {
            JFUtils utils = context.getUtils();
            double pip_worth_usd = utils.convertPipToCurrency(pair, Instrument.GBPUSD.getSecondaryCurrency());
            double pip_worth_gbp = utils.convertPipToCurrency(pair, Instrument.GBPUSD.getPrimaryCurrency());

            ITick lastTick = context.getHistory().getLastTick(pair);

            String req = String.format("%s%spair?broker=JForex&account=0&pair=%s&pip_worth_usd=%s&pip_worth_gbp=%s&pip_value=%s&digits=%s&spread=%s",
                    server.getPath(),
                    server.getWriterPath(),
                    pair.name(),
                    pip_worth_usd,
                    pip_worth_gbp,
                    pair.getPipValue(),
                    pair.getPipScale(),
                    (lastTick.getAsk() - lastTick.getBid())
            );

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
        } catch (JFException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }
}
