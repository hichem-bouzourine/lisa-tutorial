package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Set;

public class Intervalles implements BaseNonRelationalValueDomain<Intervalles> {
    public static final Intervalles BOTTOM = new Intervalles(Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final Intervalles TOP = new Intervalles(Integer.MIN_VALUE, Integer.MAX_VALUE);

    private final int min, max;

    Set<Integer> values;

    private Intervalles(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Intervalles lubAux(Intervalles intervalles) throws SemanticException {
        return new Intervalles(Math.min(intervalles.min, min), Math.max(intervalles.max, max));
    }

    @Override
    public boolean lessOrEqualAux(Intervalles other) throws SemanticException {
        return false;
    }

    @Override
    public Intervalles top() {
        return TOP;
    }

    @Override
    public Intervalles bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return BaseNonRelationalValueDomain.super.isBottom();
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation("[" + min + " .. " + max + "]");
    }
}
