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

/**
 * A no-action progress listener. All methods are empty and
 * {@link #getUpdateInterval()} returns {@code Long.MAX_VALUE} so that 
 * the runtime object never bothers calling.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class NullProgressListener extends AbstractProgressListener {

    @Override
    public void start() {}

    public void update(long numPixels) {}

    public void finish() {}

    /**
     * {@inheritDoc}
     * @return Always returns {@link Long#MAX_VALUE} which effectively means
     *         "don't bother calling me".
     */
    @Override
    public long getUpdateInterval() { return Long.MAX_VALUE; }

}
