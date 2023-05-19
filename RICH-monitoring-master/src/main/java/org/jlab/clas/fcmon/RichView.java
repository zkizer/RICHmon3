package org.jlab.clas.fcmon;

import java.util.Arrays;

import org.jlab.detector.view.DetectorShape2D;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.view.DetectorPane2D;

/**
 *
 * @author kenjo
 */
public class RichView extends DetectorPane2D {

    private final Integer twotilers[] = {3, 5, 7, 12, 15, 19, 24, 28, 33, 39, 44, 50, 57, 63, 70, 78, 85, 93, 102, 110, 119, 129, 138};
    private final int nleftTile[] = {2, 5, 8, 11, 15, 19, 23, 28, 33, 38, 44, 50, 56, 63, 70, 77, 85, 93, 101, 110, 119, 128, 138};
    private final double pmtW = 8;
    private RICHtile[] rtile = new RICHtile[nleftTile[nleftTile.length - 1]];
    private PixelXY[][] pixXY = new PixelXY[nleftTile[nleftTile.length - 1]][192];

    public RichView() {
        super();
        this.initUI();

        double y0 = 0;
        for (int irow = 0, itile = 0; irow < nleftTile.length; irow++) {
            double x0 = (3 + irow * 0.5) * pmtW + Math.ceil((6 + irow) / 6.0);
            for (; itile < nleftTile[irow]; itile++) {
                RICHtile r1 = new RICHtile(itile + 1, Arrays.asList(twotilers).contains(itile + 1) ? 2 : 3);
                r1.setPosition(x0 - r1.getWidth(), y0);
                for (DetectorShape2D pmt : r1.pmts) {
                    this.getView().addShape("RICH", pmt);
                }
                rtile[itile] = r1;
                x0 -= r1.getWidth() + 1;
            }
            y0 -= pmtW + 1;
        }
    }

    public PixelXY getPixel(int itile, int imaroc, int ipix) {
        return rtile[itile].getPixel(imaroc, ipix);
    }

    public RICHtile getTile(int itile) {
        return rtile[itile];
    }
    
    public RICHtile[] getTiles() {
        return rtile;
    }

    public class PixelXY {

        public double x, y;

        PixelXY(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public class RICHtile {

        private int nmapmts;
        private DetectorShape2D pmts[];
        PixelXY[] pxy = new PixelXY[192];

        RICHtile(int id) {
            this(id, 3);
        }

        RICHtile(int id, int nmapmts) {
            this.nmapmts = nmapmts;
            pmts = new DetectorShape2D[nmapmts];

            for (int imaroc = 0; imaroc < nmapmts; imaroc++) {
                pmts[imaroc] = new DetectorShape2D();
                pmts[imaroc].getDescriptor().setType(DetectorType.RICH);
                pmts[imaroc].getDescriptor().setSectorLayerComponent(1, id, nmapmts == 2 && imaroc == 1 ? imaroc + 1 : imaroc);
                pmts[imaroc].createBarXY(pmtW, pmtW);
            }
        }

        double getWidth() {
            return pmtW * nmapmts;
        }

        void setPosition(double x0, double y0) {
            for (int imaroc = 0; imaroc < nmapmts; imaroc++) {
                double x1 = x0 + (nmapmts - imaroc - 1) * pmtW;
                pmts[imaroc].getShapePath().translateXYZ(x1, y0, 0.0);
                for (int irow = -4; irow < 4; irow++) {
                    for (int icol = -4; icol < 4; icol++) {
                        pxy[imaroc * 64 + (irow+4) * 8 + (icol+4)] = new PixelXY(x1 + icol, y0 + irow);
                    }
                }
            }
        }

        public DetectorShape2D getPMT(int imaroc) {
            return pmts[imaroc];
        }
        
        public DetectorShape2D[] getPMTs() {
            return pmts;
        }

        public PixelXY getPixel(int imaroc, int ipix) {
            if (nmapmts == imaroc) {
                imaroc--;
            }
            return pxy[imaroc * 64 + ipix];
        }
    }
}
