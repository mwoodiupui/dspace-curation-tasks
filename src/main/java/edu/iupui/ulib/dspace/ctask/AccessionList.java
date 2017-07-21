/*
 * Copyright 2014 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Oct 16, 2014
 */

package edu.iupui.ulib.dspace.ctask;

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
