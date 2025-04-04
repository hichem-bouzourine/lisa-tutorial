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

public class IntervallesWithRounding implements BaseNonRelationalValueDomain<IntervallesWithRounding> {
    public static final IntervallesWithRounding TOP = new IntervallesWithRounding(IntOrInf.negativeInfinity(),
            IntOrInf.positiveInfinity());
    public static final IntervallesWithRounding BOTTOM = new IntervallesWithRounding(IntOrInf.positiveInfinity(),
            IntOrInf.negativeInfinity());

    private final IntOrInf min, max;
    private static final int PRECISION = 2; // Number of decimal places for rounding

    public IntervallesWithRounding(IntOrInf min, IntOrInf max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public IntervallesWithRounding lubAux(IntervallesWithRounding other) throws SemanticException {
        return new IntervallesWithRounding(IntOrInf.min(this.min, other.min), IntOrInf.max(this.max, other.max));
    }

    @Override
    public IntervallesWithRounding glbAux(IntervallesWithRounding other) throws SemanticException {
        return new IntervallesWithRounding(IntOrInf.max(this.min, other.min), IntOrInf.min(this.max, other.max));
    }

    @Override
    public boolean lessOrEqualAux(IntervallesWithRounding other) throws SemanticException {
        return this.min.greaterOrEqual(other.min) && this.max.lessOrEqual(other.max);
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
        return this.min.greaterThan(this.max);
    }

    @Override
    public boolean isTop() {
        return this.min.isNegativeInfinity() && this.max.isPositiveInfinity();
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation("[" + min + " .. " + max + "]");
    }

    public IntervallesWithRounding add(IntervallesWithRounding other) {
        IntOrInf newMin = IntOrInf.add(this.min, other.min).round(PRECISION);
        IntOrInf newMax = IntOrInf.add(this.max, other.max).round(PRECISION);
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding subtract(IntervallesWithRounding other) {
        // [a .. b] - [c .. d] = [a - d .. b - c]
        IntOrInf newMin = IntOrInf.subtract(this.min, other.max).round(PRECISION);
        IntOrInf newMax = IntOrInf.subtract(this.max, other.min).round(PRECISION);
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding multiply(IntervallesWithRounding other) {
        IntOrInf[] bounds = { // [a .. b] * [c .. d] = [min(ac, ad, bc, bd) .. max(ac, ad, bc, bd)]
                IntOrInf.multiply(this.min, other.min).round(PRECISION),
                IntOrInf.multiply(this.min, other.max).round(PRECISION),
                IntOrInf.multiply(this.max, other.min).round(PRECISION),
                IntOrInf.multiply(this.max, other.max).round(PRECISION)
        };
        IntOrInf newMin = IntOrInf.min(bounds[0], IntOrInf.min(bounds[1], IntOrInf.min(bounds[2], bounds[3])));
        IntOrInf newMax = IntOrInf.max(bounds[0], IntOrInf.max(bounds[1], IntOrInf.max(bounds[2], bounds[3])));
        return new IntervallesWithRounding(newMin, newMax);
    }

    public IntervallesWithRounding divide(IntervallesWithRounding other) throws SemanticException {
        // if (other.min.value <= 0 && other.max.value >= 0) {
        // throw new SemanticException("Division by an interval containing zero is
        // undefined.");
        // }
        IntOrInf[] bounds = { // [a .. b] / [c .. d] = { a/c, a/d, b/c, b/d }
                IntOrInf.divide(this.min, other.min).round(PRECISION),
                IntOrInf.divide(this.min, other.max).round(PRECISION),
                IntOrInf.divide(this.max, other.min).round(PRECISION),
                IntOrInf.divide(this.max, other.max).round(PRECISION)
        };
        IntOrInf newMin = IntOrInf.min(bounds[0], IntOrInf.min(bounds[1], IntOrInf.min(bounds[2], bounds[3])));
        IntOrInf newMax = IntOrInf.max(bounds[0], IntOrInf.max(bounds[1], IntOrInf.max(bounds[2], bounds[3])));
        return new IntervallesWithRounding(newMin, newMax);
    }

    @Override
    public IntervallesWithRounding widening(IntervallesWithRounding other) throws SemanticException {
        IntOrInf newMin = this.min.lessOrEqual(other.min) ? this.min : IntOrInf.negativeInfinity();
        IntOrInf newMax = this.max.greaterOrEqual(other.max) ? this.max : IntOrInf.positiveInfinity();

        return new IntervallesWithRounding(newMin, newMax);
    }

    @Override
    public IntervallesWithRounding evalBinaryExpression(BinaryOperator operator,
            IntervallesWithRounding left,
            IntervallesWithRounding right,
            ProgramPoint pp,
            SemanticOracle oracle) throws SemanticException {

        if (left.isBottom() || right.isBottom())
            return bottom();

        if (left.isTop() || right.isTop())
            return top();

        if (operator instanceof AdditionOperator) {
            return left.add(right);
        } else if (operator instanceof SubtractionOperator) {
            return left.subtract(right);
        } else if (operator instanceof MultiplicationOperator) {
            return left.multiply(right);
        } else if (operator instanceof DivisionOperator) {
            return left.divide(right);
        }
        return top();
    }

    @Override
    public IntervallesWithRounding evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Number) {
            double value = ((Number) constant.getValue()).doubleValue();
            return new IntervallesWithRounding(new IntOrInf(value).round(PRECISION),
                    new IntOrInf(value).round(PRECISION));
        }
        return top();
    }

    @Override
    public ValueEnvironment<IntervallesWithRounding> assumeBinaryExpression(
            ValueEnvironment<IntervallesWithRounding> environment, BinaryOperator operator, ValueExpression left,
            ValueExpression right, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
            throws SemanticException {

        if (left instanceof Variable && right instanceof Constant) {
            Variable x = (Variable) left;
            Constant y = (Constant) right;
            if (y.getValue() instanceof Number) {
                double value = ((Number) y.getValue()).doubleValue();
                environment.putState(x, new IntervallesWithRounding(new IntOrInf(value).round(PRECISION),
                        new IntOrInf(value).round(PRECISION)));
                return environment;
            }
        }
        return environment;
    }

    public static class IntOrInf {
        private final double value;
        private final boolean isPositiveInfinity;
        private final boolean isNegativeInfinity;

        private IntOrInf(double value) {
            this.value = value;
            this.isPositiveInfinity = false;
            this.isNegativeInfinity = false;
        }

        public IntOrInf(boolean isPositiveInfinity, boolean isNegativeInfinity) {
            this.value = 0;
            this.isPositiveInfinity = isPositiveInfinity;
            this.isNegativeInfinity = isNegativeInfinity;
        }

        public static IntOrInf positiveInfinity() {
            return new IntOrInf(true, false);
        }

        public static IntOrInf negativeInfinity() {
            return new IntOrInf(false, true);
        }

        public boolean isPositiveInfinity() {
            return isPositiveInfinity;
        }

        public boolean isNegativeInfinity() {
            return isNegativeInfinity;
        }

        public boolean isFinite() {
            return !isPositiveInfinity && !isNegativeInfinity;
        }

        public boolean greaterOrEqual(IntOrInf other) {
            if (this.isPositiveInfinity)
                return true;
            if (other.isPositiveInfinity)
                return false;
            if (this.isNegativeInfinity)
                return other.isNegativeInfinity;
            if (other.isNegativeInfinity)
                return true;
            return this.value >= other.value;
        }

        public boolean lessOrEqual(IntOrInf other) {
            if (this.isNegativeInfinity)
                return true;
            if (other.isNegativeInfinity)
                return false;
            if (this.isPositiveInfinity)
                return other.isPositiveInfinity;
            if (other.isPositiveInfinity)
                return true;
            return this.value <= other.value;
        }

        public boolean greaterThan(IntOrInf other) {
            return !this.lessOrEqual(other);
        }

        public static IntOrInf min(IntOrInf a, IntOrInf b) {
            if (a.isNegativeInfinity || b.isNegativeInfinity)
                return negativeInfinity();
            if (a.isPositiveInfinity)
                return b;
            if (b.isPositiveInfinity)
                return a;
            return new IntOrInf(Math.min(a.value, b.value));
        }

        public static IntOrInf max(IntOrInf a, IntOrInf b) {
            if (a.isPositiveInfinity || b.isPositiveInfinity)
                return positiveInfinity();
            if (a.isNegativeInfinity)
                return b;
            if (b.isNegativeInfinity)
                return a;
            return new IntOrInf(Math.max(a.value, b.value));
        }

        public static IntOrInf add(IntOrInf a, IntOrInf b) {
            if (a.isNegativeInfinity || b.isNegativeInfinity)
                return negativeInfinity();
            if (a.isPositiveInfinity || b.isPositiveInfinity)
                return positiveInfinity();
            return new IntOrInf(a.value + b.value);
        }

        public static IntOrInf subtract(IntOrInf a, IntOrInf b) {
            if (a.isNegativeInfinity || b.isPositiveInfinity)
                return negativeInfinity();
            if (a.isPositiveInfinity || b.isNegativeInfinity)
                return positiveInfinity();
            return new IntOrInf(a.value - b.value);
        }

        public static IntOrInf multiply(IntOrInf a, IntOrInf b) {
            if (a.isNegativeInfinity || b.isNegativeInfinity)
                return positiveInfinity();
            if (a.isPositiveInfinity || b.isPositiveInfinity)
                return positiveInfinity();
            return new IntOrInf(a.value * b.value);
        }

        public static IntOrInf divide(IntOrInf a, IntOrInf b) {
            if (a.isNegativeInfinity || b.isNegativeInfinity)
                return positiveInfinity();
            if (a.isPositiveInfinity || b.isPositiveInfinity)
                return positiveInfinity();
            return new IntOrInf(a.value / b.value);
        }

        public IntOrInf round(int precision) {
            if (isPositiveInfinity || isNegativeInfinity)
                return this;
            double scale = Math.pow(10, precision);
            double roundedValue = Math.round(value * scale) / scale;
            return new IntOrInf(roundedValue);
        }

        @Override
        public String toString() {
            if (isPositiveInfinity)
                return "+∞";
            if (isNegativeInfinity)
                return "-∞";
            return Double.toString(value);
        }
    }
}