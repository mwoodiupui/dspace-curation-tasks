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
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure that an object has all of the identifiers that it should, minting them
 * as necessary.
 *
 * @author Mark H. Wood {@literal <mwood@iupui.edu>}
 */
public class CreateMissingIdentifiers
        extends AbstractCurationTask
{
    private static final Logger LOG
            = LoggerFactory.getLogger(CreateMissingIdentifiers.class);

    @Override
    public int perform(DSpaceObject dso)
            throws IOException
    {
        String typeText = dso.getTypeText();

        // Right kind of object?
        if (dso instanceof Bitstream)
        {
            // Only update original bitstreams
            Bitstream bitstream = (Bitstream) dso;
            if (!isInBundle(bitstream, "ORIGINAL"))
                return Curator.CURATE_SKIP;

            // Get a Context
            Context context;
            try {
                context = Curator.curationContext();
            } catch (SQLException ex) {
                report("Could not get a Context:  " + ex.getMessage());
                return Curator.CURATE_ERROR;
            }

            // Find the IdentifierService implementation
            IdentifierService identifierService = new DSpace()
                    .getServiceManager()
                    .getServiceByName(null, IdentifierService.class);

            // Register any missing identifiers.
            try {
                identifierService.register(context, dso);
            } catch (AuthorizeException | IdentifierException | SQLException ex) {
                String message = ex.getMessage();
                report(String.format("Identifier(s) not minted for %s %d:  %s%n",
                        typeText, dso.getID(), message));
                LOG.error("Identifier(s) not minted:  {}", message);
                return Curator.CURATE_ERROR;
            }

            // Success!
            report(String.format("%s %d registered.%n", typeText, dso.getID()));
            return Curator.CURATE_SUCCESS;
        }
        else
        {
            return Curator.CURATE_SKIP;
        }
    }

    /**
     * Search a {@code Bitstream}'s {@code Bundle}s by name.
     *
     * @param bitstream an interesting Bitstream.
     * @param name the name of the interesting Bundle.
     * @return true if {@code bitstream} is in a bundle named {@code name}.
     */
    private boolean isInBundle(Bitstream bitstream, String name)
    {
        if (null == name)
            return false;
        Bundle[] bundles;
        try {
            bundles = bitstream.getBundles();
        } catch (SQLException ex) {
            LOG.error("Could not get bundles for Bitstream {}; returning false:  {}",
                    bitstream.getID(), ex.getMessage());
            return false;
        }
        for (Bundle bundle : bundles)
            if (name.equals(bundle.getName()))
                return true;
        return false;
    }
}
