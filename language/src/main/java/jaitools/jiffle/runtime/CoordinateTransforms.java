/*
 * Copyright 2011 Michael Bedward
 * 
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package jaitools.jiffle.runtime;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Utility class to create {@link CoordinateTransform} objects for simple cases.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class CoordinateTransforms {
    
    /**
     * Creates an identity transform.
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform identity() {
        return new IdentityCoordinateTransform();
    }
    
    /**
     * Creates a scaling transform.
     * 
     * @param xscale scale on the X axis
     * @param yscale scale on the Y axis
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform scale(double xscale, double yscale) {
        return new AffineCoordinateTransform(AffineTransform.getScaleInstance(xscale, yscale));
    }
    
    /**
     * Creates a translation transform.
     * 
     * @param dx translation in the X direction
     * @param dy translation in the Y direction
     * 
     * @return a new transform instance
     */
    public static CoordinateTransform translation(double dx, double dy) {
        return new AffineCoordinateTransform(AffineTransform.getTranslateInstance(dx, dy));
    }

    /**
     * Creates a transform for working in the unit rectangle, ie. proportional
     * image coordinates where both X and Y ordinates vary from 0 to 1.
     * 
     * @param imageBounds the image bounds
     * 
     * @return a new transform instance
     * 
     * @throws IllegalArgumentException if {@code imageBounds} is {@code null} or empty
     */
    public static CoordinateTransform unitBounds(Rectangle imageBounds) {
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }

        return getTransform(new Rectangle(0, 0, 1, 1), imageBounds);
    }

    /**
     * Gets the transform which converts from {@code worldBounds} to {@code imageBounds}.
     * 
     * @param worldBounds
     * @param imageBounds
     * 
     * @return a new transform instance
     * 
     * @throws IllegalArgumentException if either argument is {@code null} or empty
     */
    public static CoordinateTransform getTransform(Rectangle2D worldBounds, Rectangle imageBounds) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("worldBounds must not be null or empty");
        }
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }

        double xscale = (imageBounds.getMaxX() - imageBounds.getMinX()) / 
                (worldBounds.getMaxX() - worldBounds.getMinX());
        
        double xoff = imageBounds.getMinX() - xscale * worldBounds.getMinX();
        
        double yscale = (imageBounds.getMaxY() - imageBounds.getMinY()) / 
                (worldBounds.getMaxY() - worldBounds.getMinY());
        
        double yoff = imageBounds.getMinY() - yscale * worldBounds.getMinY();
        
        return new AffineCoordinateTransform(new AffineTransform(xscale, 0, 0, yscale, xoff, yoff));
    }
}
