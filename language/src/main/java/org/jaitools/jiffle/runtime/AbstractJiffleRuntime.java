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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jaitools.jiffle.Jiffle;
import org.jaitools.jiffle.JiffleException;


/**
 * Provides default implementations of {@link JiffleRuntime} methods plus 
 * some common fields. The fields include those involved in handling image-scope
 * variables and script options; an instance of {@link JiffleFunctions}; and an
 * integer stack used in evaluating {@code con} statements.
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public abstract class AbstractJiffleRuntime implements JiffleRuntime {
    private static final double EPS = 1.0e-8d;

    private enum Dim { XDIM, YDIM };
    
    private Map<String, Jiffle.ImageRole> _imageParams;
    
    /** Processing area bounds in world units. */
    private Rectangle2D _worldBounds;
    
    /** Pixel width in world units. */
    private double _xres;
    
    /** Pixel height in world units. */
    private double _yres;

    /** Flags whether bounds and pixel dimensions have been set. */
    private boolean _worldSet;
    
    /** Number of pixels calculated from bounds and pixel dimensions. */
    private long _numPixels;
    
    private class TransformInfo {
        CoordinateTransform transform;
        boolean isDefault;
    }

    /** 
     * A default transform to apply to all images set without an explicit
     * transform. 
     */
    private CoordinateTransform _defaultTransform = new IdentityCoordinateTransform();
    
    /** World to image coordinate transforms with image name as key. */
    private Map<String, TransformInfo> _transformLookup;

    /** 
     * Holds information about an image-scope variable. 
     * This class is only public to work around a problem in the 
     * Janino compiler involving private nested classes. It is
     * not intended for client use.
     */
    public class ImageScopeVar {
        
        /** Variable name. */
        public String name;
        
        /** Whether a default value was provided in the script init block. */
        public boolean hasDefaultValue;

        /** Whether a run-time value has been set. */
        public boolean isSet;

        /** The current value. */
        public double value;

        /**
         * Constructor.
         * @param name variable name
         * @param hasDefaultValue whether a default value is defined in the script
         */
        public ImageScopeVar(String name, boolean hasDefaultValue) {
            this.name = name;
            this.hasDefaultValue = hasDefaultValue;
        }
    }

    // Used to size / resize the _vars array as required
    private static final int VAR_ARRAY_CHUNK = 100;
    
    /** Image-scope variables. */
    protected ImageScopeVar[] _vars = new ImageScopeVar[VAR_ARRAY_CHUNK];
    
    /** Whether the image-scope variables have been initialized. */
    protected boolean _imageScopeVarsInitialized;

    /** The number of image-scope variables defined. */
    protected int _numVars;
    
    /** Advertizes the image-scope variable getter syntax to source generators. */
    public static final String VAR_STRING = "_vars[_VAR_].value";
    
    /** Whether the <i>outside</i> option is set. */
    protected boolean _outsideValueSet;
    
    /** 
     * The value to return for out-of-bounds image data requests if the
     * <i>outside</i> option is set.
     */
    protected double _outsideValue;

    /** 
     * A stack of integer values used in the evaluation of if statements.
     */
    protected IntegerStack _stk;
    
    /** 
     * Provides runtime function support.
     */
    protected final JiffleFunctions _FN;

    /**
     * Creates a new instance of this class and initializes its 
     * {@link JiffleFunctions} and {@link IntegerStack} objects.
     */
    public AbstractJiffleRuntime() {
        _FN = new JiffleFunctions();
        _stk = new IntegerStack();
        
        _transformLookup = new HashMap<String, TransformInfo>();
        _xres = Double.NaN;
        _yres = Double.NaN;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setImageParams(Map imageParams) {
        this._imageParams = new HashMap<String, Jiffle.ImageRole>();
        for (Object oname : imageParams.keySet()) {
            String name = (String) oname;
            Jiffle.ImageRole role = (Jiffle.ImageRole) imageParams.get(oname);
            this._imageParams.put(name, role);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getSourceVarNames() {
        return doGetImageVarNames(Jiffle.ImageRole.SOURCE);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDestinationVarNames() {
        return doGetImageVarNames(Jiffle.ImageRole.DEST);
    }
    
    private String[] doGetImageVarNames(Jiffle.ImageRole role) {
        List<String> names = new ArrayList<String>();
        for (String name : _imageParams.keySet()) {
            if (_imageParams.get(name) == role) {
                names.add(name);
            }
        }

        return names.toArray(new String[0]);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setWorldByResolution(Rectangle2D bounds, double xres, double yres) {
        if (bounds == null || bounds.isEmpty()) {
            throw new IllegalArgumentException("bounds cannot be null or empty");
        }
        if (xres < EPS || yres < EPS) {
            throw new IllegalArgumentException("xres and yres but must be greater than 0");
        }
        
        doSetWorld(bounds, xres, yres);
    }

    /**
     * {@inheritDoc}
     */
    public void setWorldByNumPixels(Rectangle2D bounds, int numX, int numY) {
        if (bounds == null || bounds.isEmpty()) {
            throw new IllegalArgumentException("bounds cannot be null or empty");
        }
        if (numX <= 0 || numY <= 0) {
            throw new IllegalArgumentException("numX and numY must be greater than 0");
        }
        
        doSetWorld(bounds, bounds.getWidth() / numX, bounds.getHeight() / numY);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isWorldSet() {
        return _worldSet;
    }

    /**
     * {@inheritDoc}
     */
    public Double getVar(String varName) {
        int index = getVarIndex(varName);
        if (index < 0) {
            return null;
        }
        
        return _vars[index].isSet ? _vars[index].value : null; 
    }

    /**
     * {@inheritDoc}
     */
    public void setVar(String varName, Double value) throws JiffleRuntimeException {
        int index = getVarIndex(varName);
        if (index < 0) {
            throw new JiffleRuntimeException("Undefined variable: " + varName);
        }
        setVarValue(index, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getVarNames() {
        String[] names = new String[_numVars];
        for (int i = 0; i < _numVars; i++) {
            names[i] = _vars[i].name;
        }
        return names;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinX() {
        return _worldBounds.getMinX();
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxX() {
        return _worldBounds.getMaxX();
    }

    /**
     * {@inheritDoc}
     */
    public double getMinY() {
        return _worldBounds.getMinY();
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxY() {
        return _worldBounds.getMaxY();
    }
    
    /**
     * {@inheritDoc}
     */
    public double getWidth() {
        return _worldBounds.getWidth();
    }
    
    /**
     * {@inheritDoc}
     */
    public double getHeight() {
        return _worldBounds.getHeight();
    }

    /**
     * {@inheritDoc}
     */
    public double getXRes() {
        return _xres;
    }

    /**
     * {@inheritDoc}
     */
    public double getYRes() {
        return _yres;
    }
    
    public long getNumPixels() {
        if (!_worldSet) {
            throw new IllegalStateException("Processing area has not been set");
        }
        return _numPixels;
    }
    
    /**
     * Sets a coordinate transform to use with the image represented by
     * {@code imageVarName}.
     * 
     * @param imageVarName variable name
     * @param tr the transform or {@code null} for the default transform
     * 
     * @throws WorldNotSetException if world bounds and resolution are not yet set
     */
    protected void setTransform(String imageVarName, CoordinateTransform tr) 
            throws WorldNotSetException {
        
        TransformInfo info = new TransformInfo();
        
        if (tr == null) {
            info.transform = _defaultTransform;
            info.isDefault = true;
            
        } else {
            if (!isWorldSet()) {
                throw new WorldNotSetException();
            }
            
            info.transform = tr;
            info.isDefault = false;
        }
        
        _transformLookup.put(imageVarName, info);
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultTransform(CoordinateTransform tr) throws JiffleException {
        if (tr != null) {
            if (!isWorldSet()) {
                throw new JiffleException(
                        "Setting a default coordinate tranform without having "
                        + "first set the world bounds and resolution");
            }
            
        } else {
            tr = new IdentityCoordinateTransform();
        }
        _defaultTransform = tr;
        
        for (String name : _transformLookup.keySet()) {
            TransformInfo info = _transformLookup.get(name);
            if (info.isDefault) {
                info.transform = _defaultTransform;
                _transformLookup.put(name, info);
            }
        }
    }
    
    
    
    /**
     * Gets the coordinate transform to use with the image represented by
     * {@code imageVarName}.
     * 
     * @param imageVarName variable name
     * 
     * @return the coordinate transform
     */
    protected CoordinateTransform getTransform(String imageVarName) {
        return _transformLookup.get(imageVarName).transform;
    }

    /**
     * Sets the value of an image-scope variable. If {@code value} is {@code null}
     * the variable is set to its default value if one is defined, otherwise an
     * exception is thrown.
     * 
     * @param index variable index
     * @param value the new value or {@code null} for default value
     * @throws JiffleRuntimeException if {@code value} is {@code null} but no default
     *         value is defined for the variable
     */
    protected void setVarValue(int index, Double value) throws JiffleRuntimeException {
        if (value == null) {
            if (!_vars[index].hasDefaultValue) {
                throw new JiffleRuntimeException(
                        "Value cannot be null for variable with no default: " + _vars[index].name);
            }
            
            _imageScopeVarsInitialized = false;
            _vars[index].isSet = false;
            
        } else {
            _vars[index].value = value;
            _vars[index].isSet = true;
        }
    }

    /**
     * Gets the index for an image-scope variable by name.
     * 
     * @param varName variable name
     * @return the index or -1 if the name is not found
     */
    protected int getVarIndex(String varName) {
        for (int i = 0; i < _numVars; i++) {
            if (_vars[i].name.equals(varName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Initializes image-scope variables. These are fields in the runtime class.
     * They are initialized in a separate method rather than the constructor
     * because they may depend on expressions involving values which are not
     * known until the processing area is set (e.g. Jiffle's width() function).
     * 
     * @throws JiffleRuntimeException if any variables do not have either a
     *         default or provided value
     */
    protected void initImageScopeVars() {
        for (int i = 0; i < _numVars; i++) {
            if (!_vars[i].isSet) {
                Double value = getDefaultValue(i);
                if (value == null) {
                    throw new JiffleRuntimeException(
                            "No default value set for " + _vars[i].name);
                }
                _vars[i].value = value;
                _vars[i].isSet = true;
            }
        }
        _imageScopeVarsInitialized = true;
    }
    
    /**
     * Gets the default value for an image-scope variable. This method is 
     * overridden as part of the generated run-time class code.
     * 
     * @param index the index of the variable
     * @return the default value or {@code null} if one is not defined
     */
    protected abstract Double getDefaultValue(int index);

    /**
     * Initializes runtime class fields related to Jiffle script options.
     */
    protected abstract void initOptionVars();

    /**
     * Registers a variable as having image scope.
     * 
     * @param name variable name
     * @param hasDefault whether the variable has a default value
     */
    protected void registerVar(String name, boolean hasDefault) {
        // check that the variable is not already registered
        if (getVarIndex(name) >= 0) {
            throw new JiffleRuntimeException("Variable already defined: " + name);
        }
        
        _numVars++ ;
        ImageScopeVar var = new ImageScopeVar(name, hasDefault);
        if (_numVars > _vars.length) {
            growVarsArray();
        }
        _vars[_numVars - 1] = var;
    }
    
    private void growVarsArray() {
        ImageScopeVar[] temp = _vars;
        _vars = new ImageScopeVar[_vars.length + VAR_ARRAY_CHUNK];
        System.arraycopy(temp, 0, _vars, 0, temp.length);
    }

    /**
     * Helper for {@link #setWorldByNumPixels(Rectangle2D, int, int)} and
     * {@link #setWorldByResolution(Rectangle2D, double, double)} methods.
     * 
     * @param bounds world bounds
     * @param xres pixel width
     * @param yres pixel height
     */
    private void doSetWorld(Rectangle2D bounds, double xres, double yres) {
        checkResValue(xres, Dim.XDIM, bounds);
        checkResValue(yres, Dim.YDIM, bounds);
        
        _worldBounds = new Rectangle2D.Double(
                bounds.getMinX(), bounds.getMinY(),
                bounds.getWidth(), bounds.getHeight());
        
        _xres = xres;
        _yres = yres;
        
        _worldSet = true;
    }
    
    /**
     * Helper method for {@link #setWorldByResolution(Rectangle2D, double, double)} to
     * check the validity of a pixel dimension.
     * 
     * @param value dimension in world units
     * @param dim axis: Dim.XDIM or Dim.YDIM
     * @param bounds world area bounds
     */
    private void checkResValue(double value, Dim dim, Rectangle2D bounds) {
        String name = dim == Dim.XDIM ? "xres" : "yres";
        
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " cannot be infinite");
        }
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(name + " cannot be NaN");
        }
        
        if (dim == Dim.XDIM && value > bounds.getWidth()) {
            throw new IllegalArgumentException(name + "should be less than processing area width");
            
        } else if (dim == Dim.YDIM && value > bounds.getHeight()) {
            throw new IllegalArgumentException(name + "should be less than processing area height");
        }
    }
    
}
