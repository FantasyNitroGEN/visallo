package org.visallo.core.cmdline.converters;

import com.beust.jcommander.IStringConverter;
import org.semanticweb.owlapi.model.IRI;

public class IRIConverter implements IStringConverter<IRI> {
    @Override
    public IRI convert(String s) {
        return IRI.create(s);
    }
}
