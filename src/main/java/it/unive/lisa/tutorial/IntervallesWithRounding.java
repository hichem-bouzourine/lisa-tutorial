package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Set;

public class IntervallesWithRounding implements BaseNonRelationalValueDomain<IntervallesWithRounding> {
    public static final IntervallesWithRounding BOTTOM = new IntervallesWithRounding(Double.MIN_VALUE,
            Double.MAX_VALUE);
    public static final IntervallesWithRounding TOP = new IntervallesWithRounding(Double.MIN_VALUE,
            Double.MAX_VALUE);

    private final double min, max;

    Set<Double> values;

    private IntervallesWithRounding(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public IntervallesWithRounding lubAux(IntervallesWithRounding intervallesWithRounding) throws SemanticException {
        return new IntervallesWithRounding(Math.min(this.min, intervallesWithRounding.min),
                Math.max(this.max, intervallesWithRounding.max));
    }

    @Override
    public IntervallesWithRounding glbAux(IntervallesWithRounding other) throws SemanticException {
        return new IntervallesWithRounding(Math.max(this.min, other.min),
                Math.min(this.max, other.max));
    }

    @Override
    public boolean lessOrEqualAux(IntervallesWithRounding other) throws SemanticException {
        return this.min <= other.min && this.max >= other.max;
    }

    @Override
    public IntervallesWithRounding top() {
        return TOP;
    }

    @Override
    public IntervallesWithRounding bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return this.min == Double.MIN_VALUE;
    }

    @Override
    public boolean isTop() {
        return this.max == Double.MAX_VALUE;
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation("[" + min + " .. " + max + "]");
    }

    public IntervallesWithRounding add(IntervallesWithRounding other) {
        // [a, b] + [c, d] = [a + c, b + d]
        double newMin = this.min + other.min;
        double newMax = this.max + other.max;
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding subtract(IntervallesWithRounding other) {
        // [a, b] - [c, d] = [a - d, b - c] ------------------ ! à vérifier
        double newMin = this.min - other.max;
        double newMax = this.max - other.min;
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding multiply(IntervallesWithRounding other) {
        // [a, b] * [c, d] = [min(ac, ad, bc, bd), max(ac, ad, bc, bd)]
        double[] bounds = {
                this.min * other.min,
                this.min * other.max,
                this.max * other.min,
                this.max * other.max
        };
        double newMin = Math.min(Math.min(bounds[0], bounds[1]), Math.min(bounds[2],
                bounds[3]));
        double newMax = Math.max(Math.max(bounds[0], bounds[1]), Math.max(bounds[2],
                bounds[3]));
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding divide(IntervallesWithRounding other) throws SemanticException {
        if (other.min <= 0 && other.max >= 0) {
            throw new SemanticException("Division by an interval containing zero is undefined.");
        }
        double[] bounds = {
                round(this.min / other.min),
                round(this.min / other.max),
                round(this.max / other.min),
                round(this.max / other.max)
        };
        double newMin = Math.min(Math.min(bounds[0], bounds[1]), Math.min(bounds[2],
                bounds[3]));
        double newMax = Math.max(Math.max(bounds[0], bounds[1]), Math.max(bounds[2],
                bounds[3]));
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding round() {
        double newMin = Math.round(this.min);
        double newMax = Math.round(this.max);
        return new IntervallesWithRounding(newMin, newMax);
    }

    private double round(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return value;
        }
        return Math.round(value);
    }

}
