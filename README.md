# Explanation of Base Non-relational value domain (Intervals with rounding):

## Class code Explanation:
- **IntervallesWithRounding**: This class is the base
class for a non-relational value domains. It is an abstract class
that defines the interface for all non-relational value domains that have rounding.

in my code I've defined a helping class (`IntOrInf`) that represents an integer or infinity.
and it extends the representation of `+∞` and `-∞`, there I define the rounding function that rounds 
the value with a given precision.

in the core class we find a TOP value that represents the top of the domain , and a BOTTOM value 
that represents the bottom of the domain that I respectively defined as `+∞` and `-∞`. and I've defined a 
default precision of 2 for the rounding function.

our least-upper-bottom is defined as the interval with the minimum lower bound and the maximum upper bound.
our greatest-lower-bound is defined as the interval with the maximum lower bound and the minimum upper bound.

our intervals are represented as a tuple of two `IntOrInf` values: `[a .. b]` where `a` is the lower bound and `b` is the upper bound.

the Add, sub, mul, and div operations are defined as the following:
 - [a .. b] + [c .. d] = [a + c .. b + d]
 - [a .. b] - [c .. d] = [a - d .. b - c]
 - [a .. b] * [c .. d] = [min(a * c, a * d, b * c, b * d) .. max(a * c, a * d, b * c, b * d)]
 - [a .. b] / [c .. d] = [min(a / c, a / d, b / c, b / d) .. max(a / c, a / d, b / c, b / d)]

## evalBinaryOperator : 
 - evalBinaryOperator is a method that evaluates the binary operations on the intervals with rounding. 
it checks if the left or right intervals are top or bottom and returns the appropriate value. 
then it checks the operator type and calls the appropriate method to evaluate the operation.

## evalNonNullConstant : 
 - it evaluates the constant value and returns the interval with the rounded value of the constant.

## assumeBinaryExpression :
 - it assumes the binary expression and returns the environment with the new state of the variable.