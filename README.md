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
 - [a .. b] ** **[c .. d] = [min(a **** c, a * d, b * c, b * d) .. max(a * c, a * d, b * c, b * d)]
 - [a .. b] / [c .. d] = [min(a / c, a / d, b / c, b / d) .. max(a / c, a / d, b / c, b / d)]

### evalBinaryOperator : 
 - evalBinaryOperator is a method that evaluates the binary operations on the intervals with rounding. 
it checks if the left or right intervals are top or bottom and returns the appropriate value. 
then it checks the operator type and calls the appropriate method to evaluate the operation.

### evalNonNullConstant : 
 - it evaluates the constant value and returns the interval with the rounded value of the constant.

### assumeBinaryExpression :
 - it assumes the binary expression and returns the environment with the new state of the variable.

### widening: 
 - widening is a method that returns the interval with the minimum lower bound and the maximum upper bound.

## Examples:

### Example 1:
```python
    basic() {
        def i = 4;
        def j = 2.4;
        def a = i + j;
        def s = i - j;
        def m = i * j;
        def d = i / j;
    }
```
- The result of the above code is:
```python
    i = [4 .. 4]
    j = [2.4 .. 2.4]
    a = [6.4 .. 6.4]
    s = [1.6 .. 1.6]
    m = [9.6 .. 9.6]
    d = [1.66 .. 1.66] // due to 2 precision
```

This program is very basic and it shows the basic operations on the intervals with rounding.
the interesting part is the division operation that returns the interval with the rounded 
value of the division with a precision of 2.


### Example 2: 
```python
    conditional() {
        def x = 10.0;
        def y = 3.0;
        def z = 0.0;
        
        while (z < 10.0) {
            z = z + 1.0;
        }
        if (x > 5.0) {
            y = y * 2.0;
        } else {
            y = y / 2.0;
        }
    }
```
- Widening in Action:

    * Initially, z is [0.00, 0.00].

    * After the first iteration, z becomes [0.00, 1.00].

    * After the second iteration, z becomes [0.00, 2.00].

    * The widening operator detects that the upper bound of z is increasing and sets it to +∞.

- Termination of the Loop:

    * Once z is [0.00, +∞], the condition z < x is no longer guaranteed to hold (since x is [10.00, 10.00]), so the loop terminates.

- Conditional Logic:

    * After the loop, the condition x > 5.0 is true, so y is multiplied by 2.0, resulting in [6.00, 6.00].

- Rounding:

    * All values are rounded to 2 decimal places

## Challenges: 
- The main challenge was to implement the rounding function and the binary operations on the intervals.
- At the moment I tried to `introduce a while loop`, I've faced a problem of an infinit loop, so I had 
to do some search to find the problem and fix it and it was the **widening** method.

