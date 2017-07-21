/*
 * Copyright 2017 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Jul 20, 2017
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

import edu.iupui.ulib.dspace.schema.RequiredMetadata;
import edu.iupui.ulib.dspace.schema.RequiredMetadata.Field;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * Check Items for the presence of minimal metadata.  Required fields are defined
 * in an XML document:
 *
 * <pre>{@code
 * <required-metadata>
 *   <field schema='dc' element='title'/>
 *   <field schema='dc' element='date' qualifier='published' language='*'/>
 * </required-metadata>
 * }</pre>
 *
 * The {@code <field>} element may have only the attributes shown above.
 * An omitted attribute requires that property to be null.
 * An attribute value of "*" permits any value in the property.
 * The {@code schema} and {@code element} attributes are required.
 *
 * <p>The path to the configuration document is given by the task property
 * {@code configDocument} and is understood relative to the DSpace configuration
 * directory {@code [DSpace]/config}.
 *
 * @author mwood
 */
public class MinimumMetadata
        extends AbstractCurationTask
{
    private static RequiredMetadata requiredFields;

    /**
     * Lazily initializes the configuration.
     *
     * @throws JAXBException passed through.
     * @throws SAXException passed through.
     */
    public MinimumMetadata()
            throws JAXBException, SAXException
    {
        if (null == requiredFields)
            requiredFields = loadRequiredFields();
    }

    @Override
    public int perform(DSpaceObject dso)
            throws IOException
    {
        if (!(dso instanceof Item))
            return Curator.CURATE_SKIP;

        Item item = (Item)dso;
        long badItems = 0;
        for (Field rf : requiredFields.getField())
        {
            String schema = rf.getSchema();

            String element = rf.getElement();

            String qualifier = rf.getQualifier();
            if ("*".equals(qualifier))
                qualifier = Item.ANY;

            String language = rf.getLanguage();
            if ("*".equals(language))
                language = Item.ANY;

            Metadatum[] md = item.getMetadata(schema, element, qualifier, language);
            if (md.length <= 0)
            {
                badItems++;
                report(String.format("Item %d lacks %s.%s.%s.%s",
                        item.getID(), schema, element, qualifier, language));
            }
        }

        if (badItems > 0)
        {
            setResult("Some required metadata fields were not found.");
            return Curator.CURATE_ERROR;
        }
        else
        {
            return Curator.CURATE_SUCCESS;
        }
    }

    /**
     * Load the XML document which describes the required fields.
     *
     * @return the semantic analysis of the document.
     * @throws JAXBException passed through.
     * @throws SAXException passed through.
     */
    private RequiredMetadata loadRequiredFields()
            throws JAXBException, SAXException
    {
        ConfigurationService cfg = new DSpace().getConfigurationService();
        Path configPath = Paths.get(
                cfg.getProperty("dspace.dir"),
                "config",
                taskProperty("configDocument"));

        URL schemaDocument = MinimumMetadata.class.getResource("xsd/MinimumMetadata.xsd");
        Schema schema = SchemaFactory.newInstance(null).newSchema(schemaDocument);

        JAXBContext jctx = JAXBContext.newInstance(RequiredMetadata.class);
        Unmarshaller unmarshaller = jctx.createUnmarshaller();
        unmarshaller.setSchema(schema);

        return (RequiredMetadata) unmarshaller.unmarshal(configPath.toFile());
    }
}
