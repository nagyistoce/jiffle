/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.jaitools.jiffle.runtime;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.jaitools.jiffle.JiffleException;


/**
 * The default abstract base class for runtime classes that implement
 * direct evaluation.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class AbstractDirectRuntime extends AbstractJiffleRuntime implements JiffleDirectRuntime {

    private static final double EPS = 1.0e-10d;
    
    /* 
     * Note: not using generics here because they are not
     * supported by the Janino compiler.
     */
    
    /** 
     * Maps image variable names ({@link String}) to images
     * ({@link RenderedImage}).
     * 
     */
    protected Map images = new HashMap();
    
    /** 
     * Maps source image variable names ({@link String}) to image
     * iterators ({@link RandomIter}).
     */
    protected Map readers = new LinkedHashMap();
    
    /**
     * Maps destination image variable names ({@link String} to
     * image iterators ({@link WritableRandomIter}).
     */
    protected Map writers = new LinkedHashMap();

    /**
     * Creates a new instance and initializes script-option variables.
     */
    public AbstractDirectRuntime() {
        initOptionVars();
    }

    /**
     * {@inheritDoc}
     */
    public void setDestinationImage(String varName, WritableRenderedImage image) {
        try {
            doSetDestinationImage(varName, image, null);
        } catch (WorldNotSetException ex) {
            // No exception can be caused by a null transform
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDestinationImage(String varName, WritableRenderedImage image, 
            CoordinateTransform tr) throws JiffleException {
        
        try {
            doSetDestinationImage(varName, image, tr);
            
        } catch (WorldNotSetException ex) {
            throw new JiffleException(String.format(
                    "Setting a coordinate tranform for a destination (%s) without"
                    + "having first set the world bounds and resolution", varName));
        }
    }
    
    private void doSetDestinationImage(String varName, WritableRenderedImage image, 
            CoordinateTransform tr) throws WorldNotSetException {
        
        images.put(varName, image);
        writers.put(varName, RandomIterFactory.createWritable(image, null));
        setTransform(varName, tr);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setSourceImage(String varName, RenderedImage image) {
        try {
            doSetSourceImage(varName, image, null);
        } catch (WorldNotSetException ex) {
            // No exception can be caused by a null transform
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setSourceImage(String varName, RenderedImage image, CoordinateTransform tr) 
            throws JiffleException {
        try {
            doSetSourceImage(varName, image, tr);
            
        } catch (WorldNotSetException ex) {
            throw new JiffleException(String.format(
                    "Setting a coordinate tranform for a source (%s) without"
                    + "having first set the world bounds and resolution", varName));
        }
    }
    
    private void doSetSourceImage(String varName, RenderedImage image, CoordinateTransform tr)
            throws WorldNotSetException {
        
        images.put(varName, image);
        readers.put(varName, RandomIterFactory.create(image, null));
        setTransform(varName, tr);
    }

    /**
     * {@inheritDoc}
     */
    public void evaluateAll(JiffleProgressListener pl) {
        JiffleProgressListener listener = pl == null ? new NullProgressListener() : pl;
        
        if (!isWorldSet()) {
            setDefaultBounds();
        }

        final long numPixels = getNumPixels();
        listener.setTaskSize(numPixels);
        
        long count = 0;
        long sinceLastUpdate = 0;
        final long updateInterval = listener.getUpdateInterval();
        
        final double minX = getMinX();
        final double maxX = getMaxX();
        final double resX = getXRes();
        
        final double minY = getMinY();
        final double maxY = getMaxY();
        final double resY = getYRes();
        
        listener.start();
        for (double y = minY; y < maxY - EPS; y += resY) {
            for (double x = minX; x < maxX - EPS; x += resX) {
                evaluate(x, y);
                
                count++ ;
                sinceLastUpdate++;
                if (sinceLastUpdate >= updateInterval) {
                    listener.update( count );
                    sinceLastUpdate = 0;
                }
            }
        }
        listener.finish();
    }
    
    /**
     * {@inheritDoc}
     */
    public double readFromImage(String srcImageName, double x, double y, int band) {
        boolean inside = true;
        RenderedImage img = (RenderedImage) images.get(srcImageName);
        CoordinateTransform tr = getTransform(srcImageName);
        
        Point imgPos = tr.worldToImage(x, y, null);
        
        int xx = imgPos.x - img.getMinX();
        if (xx < 0 || xx >= img.getWidth()) {
            inside = false;
        } else {
            int yy = imgPos.y - img.getMinY();
            if (yy < 0 || yy >= img.getHeight()) {
                inside = false;
            }
        }
        
        if (!inside) {
            if (_outsideValueSet) {
                return _outsideValue;
            } else {
                throw new JiffleRuntimeException( String.format(
                        "Position %.4f %.4f is outside bounds of image: %s", 
                        x, y, srcImageName));
            }
        }
        
        RandomIter iter = (RandomIter) readers.get(srcImageName);
        return iter.getSampleDouble(imgPos.x, imgPos.y, band);
    }
    
    /**
     * {@inheritDoc}
     */
    public void writeToImage(String destImageName, double x, double y, int band, double value) {
        WritableRandomIter iter = (WritableRandomIter) writers.get(destImageName);
        CoordinateTransform tr = getTransform(destImageName);
        Point imgPos = tr.worldToImage(x, y, null);
        iter.setSample(imgPos.x, imgPos.y, band, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultBounds() {
        RenderedImage refImage = null;
        String imageName = null;
        
        if (!writers.isEmpty()) {
            imageName = (String) writers.keySet().iterator().next();
            refImage = (RenderedImage) images.get(imageName);
        } else {
            imageName = (String) readers.keySet().iterator().next();
            refImage = (RenderedImage) images.get(imageName);
        }
        
        Rectangle rect = new Rectangle(
                refImage.getMinX(), refImage.getMinY(), 
                refImage.getWidth(), refImage.getHeight());
        
        setWorldByResolution(rect, 1, 1);
    }

    /**
     * Returns the images set for this runtime object as a {@code Map} with
     * variable name as key and iamge as value. The returned {@code Map} is
     * a copy of the one held by this object, so it can be safely modified
     * by the caller.
     * 
     * @return images keyed by variable name
     */
    public Map getImages() {
        Map copy = new HashMap();
        copy.putAll(images);
        return copy;
    }

}
