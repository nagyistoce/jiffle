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

package jaitools.jiffle.parser;

/**
 * Used by the Jiffle compiler to force early exit from one of its parsers.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @source $URL: https://jai-tools.googlecode.com/svn/trunk/jiffle/src/main/java/jaitools/jiffle/parser/ConstantLookup.java $
 * @version $Id: ConstantLookup.java 1299 2011-01-18 03:26:15Z michael.bedward $
 */
public class CompilerExitException extends RuntimeException {

    public CompilerExitException() {}
    
}
