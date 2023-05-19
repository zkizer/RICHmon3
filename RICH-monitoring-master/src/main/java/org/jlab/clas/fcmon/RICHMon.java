package org.jlab.clas.fcmon;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.Timer;
import org.jlab.clas.fcmon.rich.RichHitCollection;
import org.jlab.clas.fcmon.rich.RichPlotOccupancy;
import org.jlab.clas.fcmon.rich.RichPlot;
import org.jlab.clas.fcmon.rich.RichPlotMultiplicity;
import org.jlab.clas.fcmon.rich.RichPlotTDC;

import org.jlab.detector.decode.CLASDecoder;
import org.jlab.detector.view.DetectorListener;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.detector.base.DetectorType;
import org.jlab.groot.base.GStyle;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;

/**
 *
 * @author kenjo
 * RICH 2 added by Zachary Nickischer at Duquesne Univeristy
 */
public class RICHMon implements IDataEventListener, DetectorListener {

    JPanel mainPanel;
    private final JTabbedPane tabPanel;
    private final DataSourceProcessorPane processorPane;
    private final RichView detectorView;
    private final CLASDecoder clasDecoder = new CLASDecoder();

    private final int chan2pix[] = {60, 58, 59, 57, 52, 50, 51, 49, 44, 42, 43, 41, 36, 34, 35, 33, 28, 26, 27, 25, 20, 18, 19, 17, 12, 10, 11, 9, 4, 2, 3, 1, 5, 7, 6, 8, 13, 15, 14, 16, 21, 23, 22, 24, 29, 31, 30, 32, 37, 39, 38, 40, 45, 47, 46, 48, 53, 55, 54, 56, 61, 63, 62, 64};

    private int canvasUpdateTime = 1000;
    private final int analysisUpdateTime = 100;
    private int runNumber = 0;
    private int eventNumber = 0;

    private final JLabel evntLbl = new JLabel();
    private final JLabel evntLbl2 = new JLabel();

    private final RichPlot[] richPlots = {
        new RichPlotOccupancy("RICH 2 Occupancy"),
        new RichPlotTDC("RICH 2 TDC"),
        new RichPlotMultiplicity("RICH 2 Multiplicity")
    };
    private final RichPlot[] richPlots2 = {
        new RichPlotOccupancy("RICH 1 Occupancy"),
        new RichPlotTDC("RICH 1 TDC"),
        new RichPlotMultiplicity("RICH 1 Multiplicity")
    };

    public RICHMon() {
        GStyle.getAxisAttributesX().setTitleFontSize(18);
        GStyle.getAxisAttributesX().setLabelFontSize(14);
        GStyle.getAxisAttributesY().setTitleFontSize(18);
        GStyle.getAxisAttributesY().setLabelFontSize(14);

        processorPane = new DataSourceProcessorPane();
        processorPane.setUpdateRate(analysisUpdateTime);

        JPanel leftPanel = new JPanel(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout());
        toolBar.add(evntLbl);

        detectorView = new RichView();

        leftPanel.add(toolBar, BorderLayout.PAGE_START);
        leftPanel.add(detectorView, BorderLayout.CENTER);

        tabPanel = new JTabbedPane();
        for (RichPlot rplot : richPlots) {
             tabPanel.add(rplot.getPanel());
            
        }
        for (RichPlot rplot : richPlots2) {
             tabPanel.add(rplot.getPanel());
            
        }



        JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(tabPanel);
        splitPane.setResizeWeight(0.25);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(processorPane, BorderLayout.PAGE_END);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        //mainPanel.add(tabPanel, BorderLayout.CENTER);
        detectorView.getView().addDetectorListener(this);
        processorPane.addEventListener(this);

        this.setCanvasUpdate(canvasUpdateTime);
    }

    private int getRunNumber(DataEvent event) {
        if (event.hasBank("RUN::config")) {
            return event.getBank("RUN::config").getInt("run", 0);
        }
        return runNumber;
    }

    @Override
    public void dataEventAction(DataEvent event) {
        if (event != null) {
            DataEventType evType = event.getType();

            if (event instanceof EvioDataEvent) {
                event = clasDecoder.getDataEvent(event);
                DataBank header = clasDecoder.createHeaderBank(event, 0, eventNumber, 0, 0);
                DataBank trigger = clasDecoder.createTriggerBank(event);
                event.appendBanks(header);
                event.appendBanks(trigger);
            }

            if (this.runNumber != this.getRunNumber(event)) {
                this.runNumber = this.getRunNumber(event);
                this.eventNumber = 0;
                resetEventListener();
            }
            if (evType == DataEventType.EVENT_SINGLE) {
                resetEventListener();
            }

            this.eventNumber++;

            Boolean res = processEvent((HipoDataEvent) event, 1);

            if (evType == DataEventType.EVENT_SINGLE) {
                if (res) {
                    evntLbl.repaint();
                    tabPanel.getSelectedComponent().repaint();
                } else {
                    processorPane.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "PlayNext"));
                }
            }
            
            Boolean res2 = processEvent((HipoDataEvent) event, 4);

            if (evType == DataEventType.EVENT_SINGLE) {
                if (res2) {
                    evntLbl.repaint();
                    tabPanel.getSelectedComponent().repaint();
                } else {
                    processorPane.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "PlayNext"));
                }
            }
            
        }
    }

    public Boolean processEvent(HipoDataEvent event, int sectorNumber) {
        if (event.hasBank("RICH::tdc") == true) {

            Map<Integer, RichHitCollection> rhitMap = new HashMap<>();
            DataBank bank = event.getBank("RICH::tdc");
            int rows = bank.rows();
            for (int irow = 0; irow < rows; irow++) {
                int sec = bank.getByte("sector", irow);
                //RICH 2 sector 1
                if (sectorNumber == 1 && sec == 1){
                
                    int tileID = bank.getByte("layer", irow) & 0xFF;
                    short channel = bank.getShort("component", irow);

                    int imaroc = (channel - 1) / 64;
                    int ipix = chan2pix[(channel - 1) % 64] - 1;
                    Integer id = tileID * 1000 + imaroc * 100 + ipix;


                    if (!rhitMap.containsKey(id)) {
                        RichView.PixelXY pxy = detectorView.getPixel(tileID - 1, imaroc, ipix);
                        RichHitCollection rhit = new RichHitCollection(tileID, imaroc, ipix);
                        rhit.setXY(pxy.x, pxy.y);
                        rhitMap.put(id, rhit);
                    }

                    int edge = bank.getByte("order", irow);
                    int TDC = bank.getInt("TDC", irow);
                    rhitMap.get(id).fill(edge, TDC);
                }
                
                //Rich 1 sector 4
                if (sectorNumber == 4 && sec == 4){
                
                    int tileID = bank.getByte("layer", irow) & 0xFF;
                    short channel = bank.getShort("component", irow);

                    int imaroc = (channel - 1) / 64;
                    int ipix = chan2pix[(channel - 1) % 64] - 1;
                    Integer id = tileID * 1000 + imaroc * 100 + ipix;


                    if (!rhitMap.containsKey(id)) {
                        RichView.PixelXY pxy = detectorView.getPixel(tileID - 1, imaroc, ipix);
                        RichHitCollection rhit = new RichHitCollection(tileID, imaroc, ipix);
                        rhit.setXY(pxy.x, pxy.y);
                        rhitMap.put(id, rhit);
                    }

                    int edge = bank.getByte("order", irow);
                    int TDC = bank.getInt("TDC", irow);
                    rhitMap.get(id).fill(edge, TDC);
                }
            }

            if (sectorNumber == 1){
                for (RichHitCollection rhit : rhitMap.values()) {
                    rhit.processHits();
                }

                for (RichPlot rplot : richPlots) {
                    rplot.process(rhitMap);
                }

                for (RichPlot rplot : richPlots) {
                    rplot.fill(rhitMap);
                }
            }
            
            if (sectorNumber == 4){
                for (RichHitCollection rhit : rhitMap.values()) {
                    rhit.processHits();
                }

                for (RichPlot rplot : richPlots2) {
                    rplot.process(rhitMap);
                }

                for (RichPlot rplot : richPlots2) {
                    rplot.fill(rhitMap);
                }
            }
            return !rhitMap.isEmpty();
        }
        return false;
    }

    @Override
    public void processShape(DetectorShape2D shape) {
        System.out.println("SHAPE SELECTED = " + shape.getDescriptor());
        if (shape.getDescriptor().getType() == DetectorType.RICH) {
            for (RichPlot rplot : richPlots) {
                rplot.processShape(shape);
            }
            for (RichPlot rplot : richPlots2) {
                rplot.processShape(shape);
            }
        }
    }

    public void repaint() {
        for (RichPlot rplot : richPlots) {
            rplot.getPanel().repaint();
        }
        for (RichPlot rplot : richPlots2) {
            rplot.getPanel().repaint();
        }
    }

    @Override
    public void resetEventListener() {
        for (RichPlot rplot : richPlots) {
            rplot.reset();
        }
        for (RichPlot rplot : richPlots2) {
            rplot.reset();
        }
    }

    public final void setCanvasUpdate(int time) {
        System.out.println("Setting " + time + " ms update interval");
        this.canvasUpdateTime = time;
        for (RichPlot rplot : richPlots) {
            rplot.setCanvasUpdate(time);
        }
        for (RichPlot rplot : richPlots2) {
            rplot.setCanvasUpdate(time);
        }

        Timer updateTimer = new Timer(time, ev -> {
            evntLbl.setText("run: " + this.runNumber + " event: " + this.eventNumber);
            evntLbl.repaint();
        });
        updateTimer.start();
    }

    @Override
    public void timerUpdate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MON12");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        RICHMon viewer = new RICHMon();
        //frame.add(viewer.getPanel());
        frame.add(viewer.mainPanel);
        frame.setSize(1400, 800);
        frame.setVisible(true);
    }

}
