/*
 * Coordinate Transformations Suite (abridged CTS)  is a library developped to 
 * perform Coordinate Transformations using well known geodetic algorithms 
 * and parameter sets. 
 * Its main focus are simplicity, flexibility, interoperability, in this order.
 *
 * This library has been originaled developed by Michael Michaud under the JGeod
 * name. It has been renamed CTS in 2009 and shared to the community from 
 * the Atelier SIG code repository.
 * 
 * Since them, CTS is supported by the Atelier SIG team in collaboration with Michael 
 * Michaud.
 * The new CTS has been funded  by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-08-VILL-0005-01 and the regional council 
 * "Région Pays de La Loire" under the projet SOGVILLE (Système d'Orbservation 
 * Géographique de la Ville).
 *
 * CTS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * CTS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * CTS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <https://github.com/irstv/cts/>
 */
package org.cts.op.projection;

import java.util.Map;
import org.cts.util.Complex;
import org.cts.CoordinateDimensionException;
import org.cts.CoordinateOperation;
import org.cts.Ellipsoid;
import org.cts.Identifier;
import org.cts.units.Measure;
import org.cts.NonInvertibleOperationException;

/**
 * A map projection is any method used in cartography (mapmaking) to represent
 * the two-dimensional curved surface of the earth or other body on a plane. The
 * term "projection" here refers to any function defined on the earth's surface
 * and with values on the plane, and not necessarily a geometric projection.<p>
 *
 * @author Michael Michaud
 */
public class UniversalTransverseMercator extends Projection {

    public static final Identifier UTM =
            new Identifier("EPSG", "9824", "Transverse Mercator Zoned Grid System", "UTM");
    protected final double x0, // false easting
            lon0, // the reference longitude (from the datum prime meridian)
            n, // projection exponent
            xs, // x coordinate of the pole
            ys;   // y coordinate of the pole
    protected final double[] dircoeff, invcoeff;

    public UniversalTransverseMercator(final Ellipsoid ellipsoid,
            final Map<String, Measure> parameters) {
        super(UTM, ellipsoid, parameters);
        x0 = 500000;
        double y0 = getFalseNorthing();
        double k0 = 0.9996;
        lon0 = getCentralMeridian();
        double lat0 = 0.0;
        //C    = 0.0; // ????????
        n = k0 * ellipsoid.getSemiMajorAxis();
        xs = x0;
        ys = y0 - n * ellipsoid.curvilinearAbscissa(lat0);
        dircoeff = ellipsoid.getDirectUTMCoeff();
        invcoeff = ellipsoid.getInverseUTMCoeff();
    }

    /**
     * Transform coord using a Lambert Conformal Conic projection. Input coord
     * is supposed to be a geographic latitude / longitude coordinate in
     * radians.
     *
     * @param coord coordinate to transform
     * @throws CoordinateDimensionException if
     * <code>coord</code> length is not compatible with this
     * <code>CoordinateOperation</code>.
     */
    @Override
    public double[] transform(double[] coord) throws CoordinateDimensionException {
        double latIsoPhi = ellipsoid.isometricLatitude(coord[0]);
        double PHI = Math.asin(Math.sin(coord[1] - lon0) / Math.cosh(latIsoPhi));
        double latIsoPHI = Ellipsoid.SPHERE.isometricLatitude(PHI);
        double lambda = Math.atan(Math.sinh(latIsoPhi) / Math.cos(coord[1] - lon0));
        Complex z = new Complex(lambda, latIsoPHI);
        Complex Z = z.times(n * dircoeff[0]);
        for (int i = 1; i < 5; i++) {
            Z = Z.plus(Complex.sin(z.times(2.0 * i)).times(n * dircoeff[i]));
        }
        coord[0] = xs + Z.im();
        coord[1] = ys + Z.re();
        return coord;
    }

    /**
     * Creates the inverse CoordinateOperation.
     */
    @Override
    public CoordinateOperation inverse() throws NonInvertibleOperationException {
        return new LambertConicConformal1SP(ellipsoid, parameters) {

            @Override
            public double[] transform(double[] coord) throws CoordinateDimensionException {
                Complex z = new Complex((coord[1] - ys) / (n * invcoeff[0]),
                        (coord[0] - xs) / (n * invcoeff[0]));
                Complex Z = z;
                for (int i = 1; i < 5; i++) {
                    Z = Z.plus(Complex.sin(z.times((2.0 * i))).times(-invcoeff[i]));
                }
                double lon = lon0 + Math.atan(Math.sinh(Z.im()) / Math.cos(Z.re()));
                double PHI = Math.asin(Math.sin(Z.re()) / Math.cosh(Z.im()));
                double latIso = ellipsoid.isometricLatitude(PHI);
                double lat = ellipsoid.latitude(latIso);
                coord[0] = lat;
                coord[1] = lon;
                return coord;
            }
        };
    }

    /**
     * Return the
     * <code>Surface</code> type of this
     * <code>Projection</code>.
     */
    @Override
    public Surface getSurface() {
        return Projection.Surface.CYLINDRICAL;
    }

    /**
     * Return the
     * <code>Property</code> of this
     * <code>Projection</code>.
     */
    @Override
    public Property getProperty() {
        return Projection.Property.CONFORMAL;
    }

    /**
     * Return the
     * <code>Orientation</code> of this
     * <code>Projection</code>.
     */
    @Override
    public Orientation getOrientation() {
        return Projection.Orientation.TRANSVERSE;
    }
}