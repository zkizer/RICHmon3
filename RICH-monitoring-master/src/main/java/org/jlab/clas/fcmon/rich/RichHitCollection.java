/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.fcmon.rich;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author kenjo
 */
public final class RichHitCollection {

    public enum Edge {
        LEADING, TRAILING
    };

    public int itile;
    public int imaroc;
    public int ipix;
    public double x, y;
    public List<RichTDC> tdcList = new ArrayList<>();
    public List<RichHit> hitList = new ArrayList<>();

    public RichHitCollection(int tileId, int imaroc, int ipix) {
        this.itile = tileId - 1;
        this.imaroc = imaroc;
        this.ipix = ipix;
    }

    public void setXY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void fill(int edge, int tdc) {
        tdcList.add(new RichTDC(edge, tdc));
    }

    public void processHits() {
        tdcList.sort((RichTDC t1, RichTDC t2) -> t1.tdc - t2.tdc);

        ListIterator<RichTDC> iter = tdcList.listIterator();
        while (iter.hasNext()) {
            RichTDC rlead = iter.next();
            if (rlead.edge==Edge.LEADING && iter.hasNext()) {
                RichTDC rtrail = iter.next();
                if (rtrail.edge == Edge.TRAILING) {
                    hitList.add(new RichHit(rlead.tdc, rtrail.tdc - rlead.tdc));
                } else {
                    iter.previous();
                }
            }
        }
    }

    public class RichTDC {

        public Edge edge = Edge.LEADING;
        public int tdc;

        RichTDC(int iedge, int tdc) {
            if (iedge == 0) {
                this.edge = Edge.TRAILING;
            }
            this.tdc = tdc;
        }
    }

    public class RichHit {

        public int time, delta;

        RichHit(int t0, int dt) {
            time = t0;
            delta = dt;
        }
    }
}
