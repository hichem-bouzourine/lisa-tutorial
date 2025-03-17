package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Set;

public class Intervalles implements BaseNonRelationalValueDomain<Intervalles> {
    public static final Intervalles TOP = new Intervalles(IntOrInf.infinite, IntOrInf.infinite);
    public static final Intervalles BOTTOM = new Intervalles(new IntOrInf(1), new IntOrInf(-1));

    private final IntOrInf min, max;

    Set<Integer> values;

    private Intervalles(IntOrInf min, IntOrInf max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Intervalles lubAux(Intervalles intervalles) throws SemanticException {
        return new Intervalles(IntOrInf.min(intervalles.min, min), IntOrInf.max(intervalles.max, max));
    }

    @Override
    public Intervalles wideningAux(Intervalles other) throws SemanticException {
        IntOrInf min, max;
        if (IntOrInf.infinite.less(other.min, this.min))
            min = IntOrInf.infinite;
        else
            min = this.min;
        if (IntOrInf.infinite.less(this.max, other.max))
            max = IntOrInf.infinite;
        else
            max = this.max;
        return new Intervalles(min, max);
    }

    // dynamo partial ??
    @Override
    public boolean lessOrEqualAux(Intervalles other) throws SemanticException {
        return this.min.greaterOrEqual(other.min) && other.max.greaterOrEqual(this.max);
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

    @Override
    public Intervalles evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            return new Intervalles(new IntOrInf((Integer) constant.getValue()),
                    new IntOrInf((Integer) constant.getValue()));
        }
        return top();
    };

    @Override
    public Intervalles evalBinaryExpression(BinaryOperator operator, Intervalles left, Intervalles right,
            ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof AdditionOperator) {
            return new Intervalles(IntOrInf.add(left.min, right.max), IntOrInf.add(left.max, right.min));
        }

        return BaseNonRelationalValueDomain.super.evalBinaryExpression(operator, left, right, pp, oracle);
    };

    private static class IntOrInf {
        boolean inf = false;
        final int value;

        static IntOrInf infinite = new IntOrInf();

        public IntOrInf(int value) {
            this.value = value;
        }

        private IntOrInf() {
            this.inf = true;
            this.value = Integer.MAX_VALUE;
        }

        public boolean isInf() {
            return this.inf;
        }

        static IntOrInf min(IntOrInf left, IntOrInf right) {
            if (left.inf || right.inf) {
                return left.infinite;
            }
            return new IntOrInf(Math.min(left.value, right.value));
        }

        static IntOrInf max(IntOrInf left, IntOrInf right) {
            if (left.inf || right.inf) {
                return left.infinite;
            }
            return new IntOrInf(Math.max(left.value, right.value));
        }

        @Override
        public String toString() {
            if (inf)
                return "inf";

            return "" + value;
        }

        public boolean greaterOrEqual(IntOrInf min) {
            return this.inf || (!min.inf && this.value >= min.value);
        }

        public boolean less(IntOrInf max, IntOrInf max1) {
            return max1.inf || (!max.inf && max.value < max1.value);
        }

        public static IntOrInf add(IntOrInf left, IntOrInf right) {
            if (left.inf || right.inf) {
                return left.infinite;
            }
            return new IntOrInf(left.value + right.value);
        }
    }
}
