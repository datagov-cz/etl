package com.linkedpipes.etl.rdf.rdf4j;

import com.linkedpipes.etl.rdf.utils.model.BackendRdfValue;
import com.linkedpipes.etl.rdf.utils.RdfUtilsException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.util.Calendar;

class Rdf4jValue implements BackendRdfValue {

    private final Value value;

    public Rdf4jValue(Value value) {
        this.value = value;
    }

    @Override
    public String asString() {
        return value.stringValue();
    }

    @Override
    public long asLong() throws RdfUtilsException {
        return asLiteral().longValue();
    }

    private Literal asLiteral() throws RdfUtilsException {
        if (value instanceof  Literal) {
            return (Literal)value;
        } else {
            throw new RdfUtilsException("Invalid value type.");
        }
    }

    @Override
    public boolean asBoolean() throws RdfUtilsException {
        return asLiteral().booleanValue();
    }

    @Override
    public String getType() {
        if (value instanceof  Literal) {
            return ((Literal) value).getDatatype().stringValue();
        }
        return null;
    }

    @Override
    public String getLanguage() {
        if (value instanceof  Literal) {
            return ((Literal) value).getLanguage().orElseGet(() -> null);
        }
        return null;
    }

    @Override
    public boolean isIri() {
        return value instanceof Resource;
    }

    @Override
    public Double asDouble() {
        if (value instanceof  Literal) {
            return ((Literal) value).doubleValue();
        }
        return null;
    }

    @Override
    public Calendar asCalendar() {
        if (value instanceof  Literal) {
            return ((Literal) value).calendarValue().toGregorianCalendar();
        }
        return null;
    }

    @Override
    public boolean isBlankNode() {
        return value instanceof BNode;
    }
}
