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



# Explanation of Base Relational value domain (Two Variables Per Linear inequality):
This project implements a relational abstract domain that analyzes programs by tracking inequalities of the form:

a·x + b·y ≤ c
where x and y are program variables, and a, b, c are constants.

Key Features:
	- Relational Analysis: Tracks relationships between variables (not just intervals).
	- Handles Assignments: Converts assignments (x = y + 1) into inequalities (x - y ≤ 1).
	- Conditional Support: Refines constraints in if conditions (if (x ≤ y)).
	- Widening for Loops: Approximates loop invariants to ensure termination.

## Key operations
	- assign:	Converts assignments (x = y + 1) into inequalities (x - y ≤ 1).
	- assume:	Refines state using conditions (if (x ≤ y) → adds x - y ≤ 0).
	- lub (join):	Combines two states, keeping the most restrictive constraints.
	- glb (meet):	Intersects two states, keeping only shared constraints.
	- widening:	Approximates loop invariants to ensure termination.

## Examples Explanation (Step-by-Step Analysis):

### Example 1 (basic):
1. Initial State: TOP (no constraints)

2. Assignment x = 1:
    - Not tracked directly (we focus on relationships between variables)
    - State remains TOP

3. Assignment y = x + 1:
    - Triggers assign() method
    - Recognized as y = 1*x + 1
    - Creates constraint: y - x ≤ 1
    - New state: { y - x ≤ 1 }

4. Assignment z = 2*x + 1:
    - Recognized as z = 2*x + 1
    - Creates constraint: z - 2x ≤ 1
    - New state: { y - x ≤ 1, z - 2x ≤ 1 }

5. Conditional if (x <= y):
    - Triggers assume() method
    - Parses as x - y ≤ 0
    - Adds constraint: x - y ≤ 0
    - New state: { y - x ≤ 1, z - 2x ≤ 1, x - y ≤ 0 }

* Key Method Behavior:
	- assign(): Converts arithmetic assignments to inequalities
	- assume(): Adds conditional constraints to the state
	- No lub/glb needed here (single execution path)

### Example 2 (transitivity):
1. Assignment y = -1*x + 1:
    - Creates constraint: y + x ≤ 1 (rewritten from y = -x + 1)

2. Assignment z = x + 3:
    - Creates constraint: z - x ≤ 3

3. Transitive Inference:
    - The domain automatically detects:
    - From y + x ≤ 1 and z - x ≤ 3
    - Can derive y + z ≤ 4 (by adding the two inequalities)
    - Final state: { y + x ≤ 1, z - x ≤ 3, y + z ≤ 4 }

* Key Method Behavior:
	- The lub() operation performs transitive closure when merging states

	- Special handling in Constraint class to detect common variables

	- Automatic derivation of new relationships through constraint solving
### Example 3 (Loop Handling):
1. Initial State: TOP

2. Loop Analysis:
    - First iteration:
        - x = 0, y = 1
        - After body: x = 1, y = 2
        - Constraints: x ≤ 1, y - x ≤ 1
    - Second iteration:
        - x = 2, y = 3
        - Constraints: x ≤ 2, y - x ≤ 1
    - Widening kicks in:
        - Recognizes x is increasing
        - Sets upper bound to +∞ for x
        - Final loop invariant: y - x ≤ 1

1. Post-Loop:
    - From condition x < 5 and widening:
        - Infers x ≥ 5 after loop
    - From y - x ≤ 1:
        - Derives y ≥ 6 (since x ≥ 5)
    - Final state: { x ≥ 5, y - x ≤ 1 }

* Key Method Behavior:
	- Special widening operator prevents infinite analysis

	- Tracks relationships between variables across iterations

	- Infers loop invariants and post-loop conditions

	- Uses assign() for loop body updates

	- assume() processes loop condition

### Example 4 (nestedConditionals):
1. Initial State: TOP

2. First Conditional 3x + 2y ≤ 3:
    - Triggers assume()
    - Parses complex inequality
    - Adds constraint: 3x + 2y ≤ 3
    - State inside first if: { 3x + 2y ≤ 3 }

3. Second Conditional x ≤ y:
    - Nested assume() call
    - Adds constraint: x - y ≤ 0
    - State inside nested if: { 3x + 2y ≤ 3, x - y ≤ 0 }

4. Path Merging:
    - After if blocks, uses lub() to merge paths
    - But since there's no else, state remains refined

* Key Method Behavior:
	- assume() recursively processes nested conditionals
	- Maintains separate states for different paths
	- Shows how constraints accumulate in scoped blocks
## Challenges:
1. Handling Loops Without Infinite Analysis
- Problem: A loop like while (x < 5) { x = x + 1 } could cause infinite refinement (x = 0, 1, 2, ...).
- Solution: Widening: Force convergence by setting x to [0, +∞] after a few iterations.

2. Transitive Constraint Propagation
- Problem: Given x ≤ y and y ≤ z, we should infer x ≤ z, but naive implementations miss this.
- Solution: Transitive Closure: Detect common variables and derive new constraints (e.g., y + z ≤ 4 in Example 2).

3. Precision vs. Performance Trade-off: 
- Problem: Tracking all possible inequalities is expensive.
- Solution: Focus on 2-Variable Inequalities: Limit constraints to a·x + b·y ≤ c for scalability.


