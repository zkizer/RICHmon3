/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.fcmon.rich;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.jlab.clas.fcmon.rich.RichHitCollection.Edge;
import org.jlab.groot.data.H1F;
import org.jlab.detector.view.DetectorShape2D;

/**
 *
 * @author kenjo
 * RICH 2 added by Zachary Nickischer at Duquesne Univeristy
 */
public final class RichPlotMultiplicity extends RichPlot {

    private class HistTDC {

        H1F[] htdc = {new H1F("RICH TDC0", "TDC", 100, 0, 100),
            new H1F("RICH TDC1", "TDC", 100, 0, 100),
            new H1F("RICH TDC2", "TDC", 100, 0, 100)};

        public void setTitle(String title) {
            for (H1F h1 : htdc) {
                String[] titles = title.split(";");
                h1.setTitle(titles[0]);
                if (titles.length > 1) {
                    h1.setTitleX(titles[1]);
                }
                if (titles.length > 2) {
                    h1.setTitleY(titles[2]);
                }
            }
        }
    }

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JComboBox tdcBox, lvlBox;
    private int selectedITile = 0, selectedIMaroc = 0;
    private final int ntiles = 138, nmarocs = 3, npixs = 64;

    private HistTDC hdet = new HistTDC();
    private HistTDC[][] hpmt = new HistTDC[ntiles][nmarocs];

    public RichPlotMultiplicity(String name) {
        lvlBox = new JComboBox(new String[]{"detector", "pmt"});
        lvlBox.addActionListener(ev -> redraw());
        tdcBox = new JComboBox(new String[]{"leading", "trailing", "both"});
        tdcBox.addActionListener(ev -> redraw());

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout());
        toolBar.add(tdcBox);
        toolBar.add(lvlBox);

        mainPanel.add(toolBar, BorderLayout.PAGE_START);
        mainPanel.add(canvas, BorderLayout.CENTER);
        mainPanel.setName(name);

        reset();
    }

    private void redraw() {
        canvas.clear();

        int itdc = tdcBox.getSelectedIndex();
        if (lvlBox.getSelectedIndex() == 0) {
            canvas.draw(hdet.htdc[itdc]);
        } else {
            canvas.draw(hpmt[selectedITile][selectedIMaroc].htdc[itdc]);
        }
    }

    @Override
    public void reset() {
        hdet = new HistTDC();
        hpmt = new HistTDC[ntiles][nmarocs];

        hdet = new HistTDC();
        hdet.setTitle("Integrated over RICH;number of hits");

        for (int itile = 0; itile < ntiles; itile++) {
            for (int imaroc = 0; imaroc < nmarocs; imaroc++) {
                hpmt[itile][imaroc] = new HistTDC();
                hpmt[itile][imaroc].setTitle("Integrated over PMT: tile " + (itile + 1) + " maroc " + imaroc + ";number of hits");
            }
        }

        redraw();
    }

    @Override
    public void fill(Map<Integer, RichHitCollection> rhits) {
        int ndetleadings = 0, ndettrailings = 0, ndethits = 0;
        int[] nleadings = new int[2000];
        int[] ntrailings = new int[2000];
        int[] nhits = new int[2000];

        for (RichHitCollection rhit : rhits.values()) {
            int id = rhit.itile * 10 + rhit.imaroc;
            for (RichHitCollection.RichTDC rtdc : rhit.tdcList) {
                if (rtdc.edge == Edge.LEADING) {
                    nleadings[id]++;
                } else {
                    ntrailings[id]++;
                }
            }
            nhits[id] += rhit.hitList.size();

            ndethits += nhits[id];
            ndettrailings += ntrailings[id];
            ndetleadings += nleadings[id];

        }

        for (int id = 0; id < 2000; id++) {
            int itile = id / 10;
            int imaroc = id % 10;
            if (nleadings[id] != 0) {
                hpmt[itile][imaroc].htdc[0].fill(nleadings[id]);
            }
            if (ntrailings[id] != 0) {
                hpmt[itile][imaroc].htdc[1].fill(ntrailings[id]);
            }
            if (nhits[id] != 0) {
                hpmt[itile][imaroc].htdc[2].fill(nhits[id]);
            }
        }

        if (ndetleadings != 0) {
            hdet.htdc[0].fill(ndetleadings);
        }
        if (ndettrailings != 0) {
            hdet.htdc[1].fill(ndettrailings);
        }
        if (ndethits != 0) {
            hdet.htdc[2].fill(ndethits);
        }
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    public void processShape(DetectorShape2D shape) {
        selectedITile = shape.getDescriptor().getLayer() - 1;
        selectedIMaroc = shape.getDescriptor().getComponent();
        redraw();
    }
}
