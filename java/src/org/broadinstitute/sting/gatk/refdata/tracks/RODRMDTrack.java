/*
 * Copyright (c) 2010.  The Broad Institute
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.refdata.tracks;

import org.broadinstitute.sting.gatk.refdata.ReferenceOrderedData;
import org.broadinstitute.sting.gatk.refdata.SeekableRODIterator;
import org.broadinstitute.sting.gatk.refdata.utils.GATKFeature;
import org.broadinstitute.sting.gatk.refdata.utils.LocationAwareSeekableRODIterator;
import org.broadinstitute.sting.gatk.refdata.utils.RODRecordList;

import java.io.File;
import java.util.Iterator;



/**
 * 
 * @author aaron 
 * 
 * Class RODRMDTrack
 *
 * wrap a reference ordered data object in the new track style. This will hopefully be phased-out as we move to
 * a FeatureReader based system.
 */
public class RODRMDTrack extends RMDTrack {

    // our ROD
    private ReferenceOrderedData data;

    /**
     * Create a track
     *
     * @param type the type of track, used for track lookup
     * @param name the name of this specific track
     * @param file the associated file, for reference or recreating the reader
     * @param data the ROD to use as the underlying data source for this track
     */
    public RODRMDTrack(Class type, String name, File file, ReferenceOrderedData data) {
        super(type, name, file);
        this.data = data;
    }

    /**
     * @return how to get an iterator of the underlying data.  This is all a track has to support,
     *         but other more advanced tracks support the query interface
     */
    @Override
    public Iterator<GATKFeature> getIterator() {
        return new SRIToIterator(data.iterator());
    }
}

class SRIToIterator implements Iterator<GATKFeature> {
    private RODRecordList list = null;
    private LocationAwareSeekableRODIterator iterator = null;

    SRIToIterator(LocationAwareSeekableRODIterator iter) {
            iterator = iter;
    }

    public boolean hasNext() {
        if (this.list != null && list.size() > 0) return true;
        return iterator.hasNext();
    }

    public GATKFeature next() {
        if (this.list != null && list.size() > 0) {
            GATKFeature f = new GATKFeature.RODGATKFeature(list.get(0));
            list.remove(0);
            return f;
        }
        else {
            list = iterator.next();
            return next();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("not supported");
    }
}