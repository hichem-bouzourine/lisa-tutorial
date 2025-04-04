package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A relational abstract domain that tracks linear inequalities between pairs
 * of program variables in the form:
 * 
 * a·x + b·y ≤ c
 * 
 * Key Features:
 * 
 * 1. **Matrix-Inspired Representation**:
 * Constraints are stored explicitly as coefficient pairs, making the
 * representation intuitive and efficient for linear inequalities.
 * 
 * 2. **Immutable Design**:
 * Operations on the domain return new instances rather than modifying
 * the existing state, ensuring thread safety and functional-style usage.
 * 
 * 3. **Simplified Lattice Operations**:
 * The least upper bound (lub) and greatest lower bound (glb) operations
 * are implemented using set-based logic without requiring explicit
 * transitive closure computations.
 * 
 * 4. **Direct Constraint Handling**:
 * Specialized methods are provided for handling common patterns such as
 * assignments and assumptions, enabling efficient and precise updates
 * to the domain state.
 */
public class TwoVariablesPerLinearInequality implements ValueDomain<TwoVariablesPerLinearInequality> {

    // Special values
    public static final TwoVariablesPerLinearInequality TOP = new TwoVariablesPerLinearInequality(State.TOP);
    public static final TwoVariablesPerLinearInequality BOTTOM = new TwoVariablesPerLinearInequality(State.BOTTOM);

    // Internal state representation
    private final State state;

    // Private constructor
    private TwoVariablesPerLinearInequality(State state) {
        this.state = state;
    }

    // Core representation of the domain state
    private static class State {
        static final State TOP = new State(Collections.emptySet(), true, false);
        static final State BOTTOM = new State(Collections.emptySet(), false, true);

        final Set<Constraint> constraints; // Set of active inequalities
        final boolean isTop;
        final boolean isBottom;

        State(Set<Constraint> constraints, boolean isTop, boolean isBottom) {
            this.constraints = Collections.unmodifiableSet(constraints);
            this.isTop = isTop;
            this.isBottom = isBottom;
        }

        State(Set<Constraint> constraints) {
            this(constraints, false, false);
        }
    }

    // Constraint representation: a*x + b*y ≤ c
    private static class Constraint {
        final Identifier x, y;
        final double a, b, c;

        Constraint(Identifier x, Identifier y, double a, double b, double c) {
            this.x = x;
            this.y = y;
            this.a = a;
            this.b = b;
            this.c = c;
        }

        boolean involves(Identifier id) {
            return x.equals(id) || y.equals(id);
        }

        @Override
        public String toString() {
            // Format the constraint as a string
            return String.format("%.2f*%s + %.2f*%s ≤ %.2f", a, x, b, y, c);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Constraint))
                return false;
            Constraint that = (Constraint) o;
            return Double.compare(that.a, a) == 0 &&
                    Double.compare(that.b, b) == 0 &&
                    Double.compare(that.c, c) == 0 &&
                    x.equals(that.x) &&
                    y.equals(that.y);
        }

        @Override
        public int hashCode() {
            // Generate a hash code based on the constraint's properties
            // This is important for using the constraint in hash-based collections
            // like HashSet or HashMap
            return Objects.hash(x, y, a, b, c);
        }
    }

    // Factory method for creating new constraints
    private static Optional<Constraint> createConstraint(Identifier x, Identifier y, double a, double b, double c) {
        if (a == 0 && b == 0)
            return Optional.empty();
        return Optional.of(new Constraint(x, y, a, b, c));
    }

    @Override
    public TwoVariablesPerLinearInequality top() {
        return TOP;
    }

    @Override
    public TwoVariablesPerLinearInequality bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isTop() {
        return state.isTop;
    }

    @Override
    public boolean isBottom() {
        return state.isBottom;
    }

    // Combine two states, keeping the most restrictive constraints.
    // This is the least upper bound (lub) operation.
    // It merges the constraints from both states, ensuring that the
    // resulting state is the most restrictive one.
    // This is done by taking the union of the constraints from both states.
    @Override
    public TwoVariablesPerLinearInequality lub(TwoVariablesPerLinearInequality other) throws SemanticException {
        if (isBottom())
            return other;
        if (other.isBottom())
            return this;
        if (isTop() || other.isTop())
            return TOP;

        // Combine constraints and keep the most restrictive ones
        Set<Constraint> combined = new HashSet<>();
        combined.addAll(state.constraints);
        combined.addAll(other.state.constraints);

        return new TwoVariablesPerLinearInequality(new State(combined));
    }

    // Intersect two states, keeping only shared constraints.
    // This is the greatest lower bound (glb) operation.
    // It retains only the constraints that are present in both states.
    // This is done by taking the intersection of the constraints from both states.
    // This is useful for narrowing down the possible values of variables
    @Override
    public TwoVariablesPerLinearInequality glb(TwoVariablesPerLinearInequality other) throws SemanticException {
        if (isTop())
            return other;
        if (other.isTop())
            return this;
        if (isBottom() || other.isBottom())
            return BOTTOM;

        // Only keep constraints that appear in both
        Set<Constraint> intersection = state.constraints.stream()
                .filter(other.state.constraints::contains)
                .collect(Collectors.toSet());

        return new TwoVariablesPerLinearInequality(new State(intersection));
    }

    @Override
    public boolean lessOrEqual(TwoVariablesPerLinearInequality other) throws SemanticException {
        if (isBottom())
            return true;
        if (other.isTop())
            return true;
        if (isTop() || other.isBottom())
            return false;

        return other.state.constraints.containsAll(state.constraints);
    }

    /**
     * Handles statements like:
     * - x = y → Adds x - y ≤ 0 and y - x ≤ 0 (equality).
     * - x = y + c → Adds x - y ≤ c.
     * - x = a*y + b → Adds x - a*y ≤ b.
     */
    @Override
    public TwoVariablesPerLinearInequality assign(Identifier id, ValueExpression expression, ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (isTop() || isBottom())
            return this;
        if (isSpecialIdentifier(id))
            return this;

        if (expression instanceof Identifier) {
            return handleIdentifierAssignment(id, (Identifier) expression);
        } else if (expression instanceof BinaryExpression) {
            return handleBinaryAssignment(id, (BinaryExpression) expression);
        }

        return this;
    }

    private TwoVariablesPerLinearInequality handleIdentifierAssignment(Identifier x, Identifier y) {
        // x = y → x - y ≤ 0 and y - x ≤ 0
        Set<Constraint> newConstraints = new HashSet<>(state.constraints);
        createConstraint(x, y, 1, -1, 0).ifPresent(newConstraints::add);
        createConstraint(y, x, 1, -1, 0).ifPresent(newConstraints::add);
        return new TwoVariablesPerLinearInequality(new State(newConstraints));
    }

    private TwoVariablesPerLinearInequality handleBinaryAssignment(Identifier x, BinaryExpression expr) {
        // Handle cases like x = y + c or x = a*y + b
        if (expr.getOperator() instanceof AdditionOperator) {
            if (expr.getLeft() instanceof Identifier && expr.getRight() instanceof Constant) {
                Identifier y = (Identifier) expr.getLeft();
                double c = ((Number) ((Constant) expr.getRight()).getValue()).doubleValue();
                return addConstraint(x, y, 1, -1, c);
            } else if (expr.getRight() instanceof Constant && expr.getLeft() instanceof BinaryExpression) {
                BinaryExpression left = (BinaryExpression) expr.getLeft();
                if (left.getOperator() instanceof MultiplicationOperator &&
                        left.getLeft() instanceof Constant &&
                        left.getRight() instanceof Identifier) {

                    double a = ((Number) ((Constant) left.getLeft()).getValue()).doubleValue();
                    Identifier y = (Identifier) left.getRight();
                    double c = ((Number) ((Constant) expr.getRight()).getValue()).doubleValue();
                    return addConstraint(x, y, 1, -a, c);
                }
            }
        }
        return this;
    }

    // Adds a new constraint to the state, returning a new instance
    // of the domain with the updated constraints.
    // This is done by creating a new set of constraints that includes
    private TwoVariablesPerLinearInequality addConstraint(Identifier x, Identifier y, double a, double b, double c) {
        Set<Constraint> newConstraints = new HashSet<>(state.constraints);
        createConstraint(x, y, a, b, c).ifPresent(newConstraints::add);
        return new TwoVariablesPerLinearInequality(new State(newConstraints));
    }

    /**
     * Handles assumptions like:
     * - x ≤ y → Adds x - y ≤ 0.
     * - a*x + b*y ≤ c → Adds a*x + b*y ≤ c.
     */
    @Override
    public TwoVariablesPerLinearInequality assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest,
            SemanticOracle oracle)
            throws SemanticException {
        if (!(expression instanceof BinaryExpression))
            return this;

        BinaryExpression binary = (BinaryExpression) expression;
        if (!(binary.getOperator() instanceof ComparisonLe))
            return this;

        SymbolicExpression left = binary.getLeft();
        SymbolicExpression right = binary.getRight();

        // Handle x ≤ y
        if (left instanceof Identifier && right instanceof Identifier) {
            return addConstraint(
                    (Identifier) left,
                    (Identifier) right,
                    1, -1, 0);
        }
        // Handle a*x + b*y ≤ c
        else if (left instanceof BinaryExpression && right instanceof Constant) {
            BinaryExpression leftExpr = (BinaryExpression) left;
            if (leftExpr.getOperator() instanceof AdditionOperator) {
                return handleLinearInequality(leftExpr, (Constant) right);
            }
        }

        return this;
    }

    private TwoVariablesPerLinearInequality handleLinearInequality(BinaryExpression expr, Constant c) {
        // Try to extract a*x + b*y ≤ c
        if (expr.getLeft() instanceof BinaryExpression && expr.getRight() instanceof BinaryExpression) {
            BinaryExpression left = (BinaryExpression) expr.getLeft();
            BinaryExpression right = (BinaryExpression) expr.getRight();

            if (left.getOperator() instanceof MultiplicationOperator &&
                    right.getOperator() instanceof MultiplicationOperator &&
                    left.getLeft() instanceof Constant &&
                    left.getRight() instanceof Identifier &&
                    right.getLeft() instanceof Constant &&
                    right.getRight() instanceof Identifier) {

                double a = ((Number) ((Constant) left.getLeft()).getValue()).doubleValue();
                Identifier x = (Identifier) left.getRight();
                double b = ((Number) ((Constant) right.getLeft()).getValue()).doubleValue();
                Identifier y = (Identifier) right.getRight();
                double constant = ((Number) c.getValue()).doubleValue();

                return addConstraint(x, y, a, b, constant);
            }
        }
        return this;
    }

    @Override
    public StructuredRepresentation representation() {
        if (isTop())
            return new StringRepresentation("⊤");
        if (isBottom())
            return new StringRepresentation("⊥");
        return new StringRepresentation(state.constraints.toString());
    }

    // recommended by a colleague in the lab
    private static boolean isSpecialIdentifier(Identifier id) {
        return id.getName().contains("heap") ||
                id.getName().contains("this") ||
                id.getName().startsWith("&pp@");
    }

    // Required but unused methods
    @Override
    public Satisfiability satisfies(ValueExpression e, ProgramPoint pp, SemanticOracle o) {
        return Satisfiability.UNKNOWN;
    }

    @Override
    public TwoVariablesPerLinearInequality forgetIdentifier(Identifier id) {
        return this;
    }

    @Override
    public TwoVariablesPerLinearInequality pushScope(ScopeToken t) {
        return this;
    }

    @Override
    public TwoVariablesPerLinearInequality popScope(ScopeToken t) {
        return this;
    }

    @Override
    public TwoVariablesPerLinearInequality forgetIdentifiersIf(Predicate<Identifier> p) {
        return this;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return false;
    }

    @Override
    public TwoVariablesPerLinearInequality smallStepSemantics(ValueExpression e, ProgramPoint pp, SemanticOracle o) {
        return this;
    }
}