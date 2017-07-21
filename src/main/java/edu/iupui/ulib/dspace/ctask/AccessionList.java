/*
 * Copyright 2014 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Oct 16, 2014
 */

package edu.iupui.ulib.dspace.ctask;

/*-
 * #%L
 * CurationTasks
 * %%
 * Copyright (C) 2014 - 2017 Indiana University
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emit an Item's id, handle, title, and date of accession in CSV format.
 *
 * @author mwood
 */
public class AccessionList
        extends AbstractCurationTask
{
    private static final Logger log = LoggerFactory.getLogger(AccessionList.class);

    @Override
    public int perform(DSpaceObject dso)
            throws IOException
    {
        log.debug("perform on {} {}", dso.getTypeText(), dso.getID());
        if (dso instanceof Item)
        {
            Item item = (Item) dso;

            Metadatum[] titles = item.getMetadata("dc", "title", null, Item.ANY);
            String title = titles.length > 0 ? titles[0].value : "";

            Metadatum[] dates = item.getMetadata("dc", "date", "accessioned", Item.ANY);
            String date = dates.length > 0 ? dates[0].value : "";

            report(String.format("%d,\"%s\",\"%s\",%s",
                    item.getID(), item.getHandle(), title, date));
            return Curator.CURATE_SUCCESS;
        }
        else
            return Curator.CURATE_SKIP;
    }
}
