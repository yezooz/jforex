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


import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.system.tester.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trade.TradeManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.dukascopy.api.DataType.*;
import static com.dukascopy.api.TickBarSize.*;


/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy in GUI mode
 */
@SuppressWarnings("serial")
public class TesterWithGUI extends JFrame implements ITesterUserInterface, ITesterExecution {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesterWithGUI.class);
    // url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    private static String pass = "";
    private static String userName = "DEMO2" + pass;
    private static String password = pass;
    private final int frameWidth = 1200;
    private final int frameHeight = 600;
    private final int controlPanelHeight = 40;
    private JPanel currentChartPanel = null;
    private ITesterExecutionControl executionControl = null;
    private JPanel controlPanel = null;
    private JButton startStrategyButton = null;
    private JButton pauseButton = null;
    private JButton continueButton = null;
    private JButton cancelButton = null;
    private FeedDescriptorPanel feedDescriptorPanel;
    private ITesterChartController chartController;
    private IChart currentChart;
    private IFeedDescriptor feedDescriptor = new FeedDescriptor();

    public TesterWithGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    }

    public static void main(String[] args) throws Exception {
        TesterWithGUI testerMainGUI = new TesterWithGUI();
        testerMainGUI.showChartFrame();
    }

    public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
        if (chartPanels != null && chartPanels.size() > 0) {

            IChart chart = chartPanels.keySet().iterator().next();

            //Note we assume we work with only one chart;
            currentChart = chart;
            currentChart.setDataPresentationType(DataPresentationType.CANDLE);
            chartController = chartPanels.get(chart).getTesterChartController();

//                        setTitle("Chart type example");

            chartController.setFeedDescriptor(feedDescriptor);
            JPanel chartPanel = chartPanels.get(chart).getChartPanel();
            addChartPanel(chartPanel);
        }
    }

    public void setExecutionControl(ITesterExecutionControl executionControl) {
        this.executionControl = executionControl;
    }

    public void startStrategy() throws Exception {
        //get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {

            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
                updateButtons();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                resetButtons();
            }

            public void onConnect() {
                LOGGER.info("Connected");
            }

            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        for (Instrument pair : TradeManager.defaultInstruments()) {
            instruments.add(pair);
        }

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        //setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryCurrency(), 10000);

        // period
        client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, TradeManager.defaultStartDate().getTime(), TradeManager.defaultEndDate().getTime());

        //load data
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        //wait for downloading to complete
        future.get();
        //start the strategy
        LOGGER.info("Starting strategy");

        client.startStrategy(
                new Strategy(),
                new LoadingProgressListener() {
                    public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                        LOGGER.info(information);
                    }

                    public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                        LOGGER.info("" + allDataLoaded);
                    }

                    public boolean stopJob() {
                        return false;
                    }
                }, this, this
        );
        //now it's running

        //In the current implementation it takes prolonged time for some chart types to load (e.g. range bars, renko),
        //so we hold up execution for maximum 5 minutes till the chart gets loaded.
        //For quicker loading please manually decrease chart's horizontal scale - it gets printed every second.
        Runnable r2 = new Runnable() {
            public void run() {
                try {
                    int waitTimeSecs = 300;
                    LOGGER.info("Pause execution for max " + waitTimeSecs + " secs till chart gets loaded. " +
                            "For quicker loading please decrease currentChart.getBarsCount() - manually decrease chart's horizontal scale.");
//                executionControl.pauseExecution();
                    updateButtons();
                    long startTime = System.currentTimeMillis();
                    try {
                        while ((currentChart == null || Math.abs(currentChart.priceMin(0)) < 0.00001) && System.currentTimeMillis() - startTime < waitTimeSecs * 1000) {
                            if (currentChart != null) {
                                int secsLeft = (int) (waitTimeSecs - (System.currentTimeMillis() - startTime) / 1000);
                                LOGGER.info(String.format("Min price=%.5f, bar count on chart=%s, time left=%s secs", currentChart.priceMin(0), currentChart.getBarsCount(), secsLeft));
                            }
                            Thread.sleep(1000);
                        }
                    } catch (Exception e2) {
                        LOGGER.error(e2.getMessage(), e2);
                        e2.printStackTrace();
                    }

                    LOGGER.info("Chart loaded after " + ((System.currentTimeMillis() - startTime) / 1000) + " secs. Please press continue.");
                } catch (Exception e2) {
                    LOGGER.error(e2.getMessage(), e2);
                    e2.printStackTrace();
                }
            }
        };
        Thread t2 = new Thread(r2);
        t2.start();

    }

    /**
     * Center a frame on the screen
     */
    private void centerFrame() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int screenHeight = screenSize.height;
        int screenWidth = screenSize.width;
        setSize(1000, screenHeight / 2);
        setLocation(screenWidth / 4, screenHeight / 4);
    }

    /**
     * Add chart panel to the frame
     *
     * @param panel
     */
    private void addChartPanel(JPanel chartPanel) {
        removecurrentChartPanel();

        this.currentChartPanel = chartPanel;
        chartPanel.setPreferredSize(new Dimension(frameWidth, frameHeight - controlPanelHeight));
        chartPanel.setMinimumSize(new Dimension(frameWidth, 200));
        chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        getContentPane().add(chartPanel);
        this.validate();
        chartPanel.repaint();
    }

    /**
     * Add buttons to start/pause/continue/cancel actions
     */
    private void addControlPanel() {

        controlPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        controlPanel.setLayout(flowLayout);
        controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelHeight));

        startStrategyButton = new JButton("Start strategy");
        startStrategyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startStrategyButton.setEnabled(false);
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            startStrategy();
                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                            resetButtons();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (executionControl != null) {
                    executionControl.pauseExecution();
                    updateButtons();
                }
            }
        });

        continueButton = new JButton("Continue");
        continueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (executionControl != null) {
                    executionControl.continueExecution();
                    updateButtons();
                }
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (executionControl != null) {
                    executionControl.cancelExecution();
                    updateButtons();
                }
            }
        });

        controlPanel.add(startStrategyButton);
        controlPanel.add(pauseButton);
        controlPanel.add(continueButton);
        controlPanel.add(cancelButton);
        getContentPane().add(controlPanel);
        feedDescriptorPanel = new FeedDescriptorPanel();
        getContentPane().add(feedDescriptorPanel);

        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private void updateButtons() {
        if (executionControl != null) {
            startStrategyButton.setEnabled(executionControl.isExecutionCanceled());
            pauseButton.setEnabled(!executionControl.isExecutionPaused() && !executionControl.isExecutionCanceled());
            cancelButton.setEnabled(!executionControl.isExecutionCanceled());
            continueButton.setEnabled(executionControl.isExecutionPaused());
        }
    }

    private void resetButtons() {
        startStrategyButton.setEnabled(true);
        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private void removecurrentChartPanel() {
        if (this.currentChartPanel != null) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        TesterWithGUI.this.getContentPane().remove(TesterWithGUI.this.currentChartPanel);
                        TesterWithGUI.this.getContentPane().repaint();
                    }
                });
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void showChartFrame() {
        setSize(frameWidth, frameHeight);
        centerFrame();
        addControlPanel();
        setVisible(true);
    }

    private class FeedDescriptorPanel extends JPanel {

        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxDataType;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxInstrument;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxOfferSide;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxFilter;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxPeriod;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxPriceRange;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxReversalAmount;
        @SuppressWarnings("rawtypes")
        private JComboBox comboBoxTickBarSize;
        private JButton buttonApplyChanges;

        public FeedDescriptorPanel() {

            this.setLayout(new FlowLayout(FlowLayout.LEFT));

            comboBoxDataType = setupComboBox(DataType.values(), "Data type", DataType.TIME_PERIOD_AGGREGATION);
            comboBoxInstrument = setupComboBox(TradeManager.defaultInstruments().toArray(), "Instrument", (Instrument) TradeManager.defaultInstruments().toArray()[0]);
            comboBoxOfferSide = setupComboBox(OfferSide.values(), "Offer Side", OfferSide.BID);
            comboBoxFilter = setupComboBox(Filter.values(), "Filter", Filter.WEEKENDS);
            comboBoxPeriod = setupComboBox(new Period[]{
                    Period.createCustomPeriod(Unit.Minute, 1),
                    Period.createCustomPeriod(Unit.Minute, 3),
                    Period.createCustomPeriod(Unit.Minute, 5),
                    Period.createCustomPeriod(Unit.Minute, 6),
                    Period.createCustomPeriod(Unit.Minute, 8),
                    Period.createCustomPeriod(Unit.Minute, 10),
                    Period.createCustomPeriod(Unit.Minute, 12),
                    Period.createCustomPeriod(Unit.Minute, 15),
                    Period.createCustomPeriod(Unit.Minute, 16),
                    Period.createCustomPeriod(Unit.Minute, 18),
                    Period.createCustomPeriod(Unit.Minute, 20),
                    Period.createCustomPeriod(Unit.Minute, 24),
                    Period.createCustomPeriod(Unit.Minute, 30),
                    Period.createCustomPeriod(Unit.Minute, 32),
                    Period.createCustomPeriod(Unit.Minute, 36),
                    Period.createCustomPeriod(Unit.Minute, 40),
                    Period.createCustomPeriod(Unit.Minute, 45),
                    Period.createCustomPeriod(Unit.Minute, 48),
                    Period.createCustomPeriod(Unit.Hour, 1),
                    Period.createCustomPeriod(Unit.Hour, 2),
                    Period.createCustomPeriod(Unit.Hour, 3),
                    Period.createCustomPeriod(Unit.Hour, 4),
                    Period.createCustomPeriod(Unit.Hour, 6),
                    Period.createCustomPeriod(Unit.Hour, 8),
                    Period.createCustomPeriod(Unit.Hour, 12),
                    Period.createCustomPeriod(Unit.Day, 1),
                    Period.createCustomPeriod(Unit.Day, 2),
                    Period.createCustomPeriod(Unit.Day, 3),
                    Period.createCustomPeriod(Unit.Day, 4),
                    Period.createCustomPeriod(Unit.Day, 5),
                    Period.createCustomPeriod(Unit.Day, 6),
                    Period.createCustomPeriod(Unit.Week, 1),
            }, "Period", TradeManager.defaultPeriods().toArray()[0]);
            comboBoxPriceRange = setupComboBox(new PriceRange[]{
                    PriceRange.valueOf(1),
                    PriceRange.valueOf(2),
                    PriceRange.valueOf(3),
                    PriceRange.valueOf(4),
                    PriceRange.valueOf(5),
                    PriceRange.valueOf(6),
                    PriceRange.valueOf(7),
                    PriceRange.valueOf(8),
                    PriceRange.valueOf(9),
                    PriceRange.valueOf(10),
                    PriceRange.valueOf(11),
                    PriceRange.valueOf(12),
                    PriceRange.valueOf(13),
                    PriceRange.valueOf(14),
                    PriceRange.valueOf(15),
                    PriceRange.valueOf(16),
                    PriceRange.valueOf(17),
                    PriceRange.valueOf(18),
                    PriceRange.valueOf(19),
                    PriceRange.valueOf(20),
                    PriceRange.valueOf(25),
                    PriceRange.valueOf(30),
                    PriceRange.valueOf(35),
                    PriceRange.valueOf(40),
                    PriceRange.valueOf(45),
                    PriceRange.valueOf(50),
                    PriceRange.valueOf(60),
                    PriceRange.valueOf(70),
                    PriceRange.valueOf(80),
                    PriceRange.valueOf(90),
                    PriceRange.valueOf(100),
                    PriceRange.valueOf(120),
                    PriceRange.valueOf(140),
                    PriceRange.valueOf(160),
                    PriceRange.valueOf(180),
                    PriceRange.valueOf(200),
                    PriceRange.valueOf(250),
                    PriceRange.valueOf(300),
                    PriceRange.valueOf(350),
                    PriceRange.valueOf(400),
                    PriceRange.valueOf(450),
                    PriceRange.valueOf(500),
            }, "Price Range", PriceRange.valueOf(10)); // TradeManager.defaultPriceRanges().toArray()[0]
            comboBoxReversalAmount = setupComboBox(new ReversalAmount[]{ReversalAmount.ONE, ReversalAmount.TWO, ReversalAmount.THREE}, "Reversal Amount", ReversalAmount.TWO);
            comboBoxTickBarSize = setupComboBox(new TickBarSize[]{TWO, THREE, FOUR, FIVE}, "Tick Bar Size", THREE);

            add(comboBoxDataType);
            add(comboBoxPeriod);
            add(comboBoxInstrument);
            add(comboBoxOfferSide);
            add(comboBoxFilter);
            add(comboBoxPriceRange);
            add(comboBoxReversalAmount);
            add(comboBoxTickBarSize);

            buttonApplyChanges = new JButton("Apply changes");
            buttonApplyChanges.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateFeedDesciptor();

                }
            });

            add(buttonApplyChanges);

            updateFeedDesciptor();
            updateComboBoxes();
        }

        @SuppressWarnings("rawtypes")
        private JComboBox setupComboBox(final Object items[], String name, Object defaultValue) {
            @SuppressWarnings("unchecked")
            JComboBox comboBox = new JComboBox(items);
            comboBox.setSelectedItem(defaultValue);
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateComboBoxes();
                }
            });
            comboBox.setToolTipText(name);
            return comboBox;
        }

        private void updateComboBoxes() {

            DataType dataType = (DataType) comboBoxDataType.getSelectedItem();

            //visibility conditions according to IFeedDescription interface documentation
            comboBoxDataType.setVisible(true);
            comboBoxInstrument.setVisible(true);
            comboBoxOfferSide.setVisible(dataType != TICKS);
            comboBoxFilter.setVisible(dataType == TIME_PERIOD_AGGREGATION);
            comboBoxPeriod.setVisible(dataType == TIME_PERIOD_AGGREGATION);
            comboBoxPriceRange.setVisible(dataType == PRICE_RANGE_AGGREGATION
                    || dataType == POINT_AND_FIGURE
                    || dataType == RENKO);
            comboBoxReversalAmount.setVisible(dataType == POINT_AND_FIGURE);
            comboBoxTickBarSize.setVisible(dataType == TICK_BAR);

        }

        private void updateFeedDesciptor() {

            feedDescriptor.setDataType((DataType) comboBoxDataType.getSelectedItem());
            feedDescriptor.setInstrument((Instrument) comboBoxInstrument.getSelectedItem());
            feedDescriptor.setPeriod((Period) comboBoxPeriod.getSelectedItem());
            feedDescriptor.setOfferSide((OfferSide) comboBoxOfferSide.getSelectedItem());
            feedDescriptor.setFilter((Filter) comboBoxFilter.getSelectedItem());
            feedDescriptor.setPriceRange((PriceRange) comboBoxPriceRange.getSelectedItem());
            feedDescriptor.setReversalAmount((ReversalAmount) comboBoxReversalAmount.getSelectedItem());
            feedDescriptor.setTickBarSize((TickBarSize) comboBoxTickBarSize.getSelectedItem());

            if (chartController != null)
                chartController.setFeedDescriptor(feedDescriptor);
        }

    }
}