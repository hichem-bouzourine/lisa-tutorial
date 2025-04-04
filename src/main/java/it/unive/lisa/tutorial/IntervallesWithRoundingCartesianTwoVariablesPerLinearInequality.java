package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.combination.CartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A Cartesian product domain combining:
 * 1. TwoVariablesPerLinearInequality (left component): Tracks relational
 * inequalities between variables (a·x + b·y ≤ c)
 * 2. ValueEnvironment<IntervallesWithRounding> (right component): Maintains
 * interval bounds with rounding for individual variables
 * 
 * This combination enables both relational and non-relational analysis
 * simultaneously.
 */
public class IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality extends
        CartesianProduct<IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality, TwoVariablesPerLinearInequality, ValueEnvironment<IntervallesWithRounding>, ValueExpression, Identifier>
        implements ValueDomain<IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality> {

    /**
     * Constructs a new Cartesian product instance.
     * 
     * @param left  The relational inequality component
     *              (TwoVariablesPerLinearInequality)
     * @param right The interval environment component
     *              (ValueEnvironment<IntervallesWithRounding>)
     */
    public IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality(TwoVariablesPerLinearInequality left,
            ValueEnvironment<IntervallesWithRounding> right) {
        super(left, right);
    }

    /**
     * Checks if an identifier is known in either component of the product.
     * 
     * @param id The identifier to check
     * @return true if the identifier exists in either domain, false otherwise
     */
    @Override
    public boolean knowsIdentifier(Identifier id) {
        // Delegate to both components - identifier may be tracked in either
        return left.knowsIdentifier(id) || right.knowsIdentifier(id);
    }

    /**
     * Factory method to create new instances of the Cartesian product.
     * Required by the CartesianProduct framework.
     * 
     * @param left  The updated relational component
     * @param right The updated interval component
     * @return A new instance combining the two components
     */
    @Override
    public IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality mk(TwoVariablesPerLinearInequality left,
            ValueEnvironment<IntervallesWithRounding> right) {
        return new IntervallesWithRoundingCartesianTwoVariablesPerLinearInequality(left, right);
    }

    // Note: Inherits all other ValueDomain operations (lub, glb, assign, assume,
    // etc.)
    // from CartesianProduct which automatically combines both components' behaviors

    // The power of this domain comes from:
    // 1. TwoVariablesPerLinearInequality tracking relationships between variables
    // 2. IntervallesWithRounding maintaining precise numeric bounds
    // Together they can:
    // - Detect when relational constraints contradict interval bounds
    // - Refine interval bounds using relational information
    // - Verify consistency between the two perspectives
}