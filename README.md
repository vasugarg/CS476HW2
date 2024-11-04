# FuzzyLang

## Overview
FuzzyLang is a domain-specific language designed to support both fuzzy logic operations and object-oriented programming constructs. The project is built to offer a robust framework for designers working with logic gates, fuzzy set operations, and object-oriented principles like encapsulation, inheritance, and polymorphism. With FuzzyLang, developers can define classes, abstract classes, and methods while performing fuzzy set operations, making it ideal for complex scenarios that require both logical computations and structured code organization.

## Key Features
### 1. Fuzzy Logic Operations
- **Fuzzy Sets**: Support for sets where elements have varying degrees of membership, allowing non-binary (gradual) set operations.
- **Operations Supported**:
    - **Union**: Combines two fuzzy sets by taking the maximum membership value for each element.
    - **Intersection**: Merges sets by taking the minimum membership value for each element.
    - **Addition**: Adds membership values of two sets element-wise, capped at a maximum value of 1.
    - **Multiplication**: Multiplies membership values of two sets element-wise.
- **Complement and XOR**:
    - **Complement**: Inverts the membership value of each element in the fuzzy set.
    - **XOR**: Performs symmetric difference by taking the difference between the maximum and minimum membership values.

### 2. Object-Oriented Programming Constructs
- **Class and Abstract Class Definitions**:
    - **Concrete Classes**: Define fully implemented classes with fields and methods.
    - **Abstract Classes**: Define classes with unimplemented (abstract) methods that subclasses must implement.
- **Inheritance**: Allows classes to inherit fields and methods from parent classes, enabling polymorphism and code reuse.
- **Access Modifiers**:
    - **Public**: Members accessible from any scope.
    - **Private**: Members only accessible within the class.
    - **Protected**: Members accessible within the class and subclasses.
- **Instance Creation**:
    - Supports instantiation of concrete classes.
    - Prevents instantiation of abstract classes, enforcing the requirement that subclasses must implement abstract methods.

### 3. Macros for Reusability
- **Macro Definition**: Allows creation of reusable code snippets that can accept parameters and perform complex operations.
- **Macro Invocation**: Enables the invocation of defined macros with specific arguments, promoting code reuse and reducing repetition.

### 4. Scoped Execution and Blocks
- **Scope**: Isolates variables within a specific execution context, preventing conflicts with outer scope variables.
- **Block Constructs**: Group operations or expressions together, ensuring variables within the block are independent of the outer scope.

### 5. Dynamic Method Invocation
- **Instance Method Invocation**: Supports invoking methods dynamically on instantiated objects with specified arguments.
- **Virtual Dispatch Table**: Manages method overriding for inherited classes, ensuring correct method execution in cases of polymorphism.
### 6. Logging 
- Integrated logging for tracking variable creation, scope entry/exit, and operation execution.


## Prerequisites

- **Scala**: Ensure you have Scala installed.
- **sbt**: The Simple Build Tool for Scala projects.

## Getting Started
### Installation
### Prerequisites
- **Scala 3.x**: This project uses Scala 3 features.
- **sbt (Scala Build Tool)**: For building the project and running tests.
- **Java Development Kit (JDK) 8, 11 or higher**: Required to run Scala applications.

### Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/vasugarg/CS476HW2.git
   ```
2. **Navigate to the Project Directory**

   ```bash
   cd CS476HW2
   ```
3. **Compile the Project**

   ```bash
   sbt compile
   ```
4. **Run Tests**

   ```bash
   sbt test
   ```

## FuzzyLang Constructs

FuzzyLang offers a comprehensive set of constructs for logic gate designers and object-oriented programming. Below is the complete list of constructs available in FuzzyLang:

<details>
<summary>Click to expand list of FuzzyLang Constructs</summary>

- [Value](#value)
- [Var](#var)
- [Assign](#assign)
- [Scope](#scope)
- [Block](#block)
- [Union](#union)
- [Intersection](#intersection)
- [Addition](#addition)
- [Multiplication](#multiplication)
- [Complement](#complement)
- [XOR](#xor)
- [MacroDef](#macrodef)
- [MacroInvoke](#macroinvoke)
- [AbstractClassDef](#abstractclassdef)
- [ClassDef](#classdef)
- [Public](#public)
- [Private](#private)
- [Protected](#protected)
- [Instantiate](#instantiate)
- [MethodInvocation](#methodinvocation)

</details>

---

### Value
`Value` represents a fuzzy set, which is a map of elements to their membership values. This construct can directly hold values of elements and is used for creating and manipulating fuzzy sets.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5, "c" -> 0.3))
```

### Var
The `Var` construct is used to reference a variable within a scope. It allows you to retrieve values assigned to variables in expressions.

Example:
```scala
val variable = Var("someVariable")
```

### Assign
`Assign` assigns the evaluated result of an expression to a variable. This is essential for modifying variable values within a scope.

Example:
```scala
Assign("x", Value(Map("a" -> 0.5)))
```

### Scope
`Scope`defines a new scope, an isolated environment where variables and operations are independent of other scopes. This is particularly useful for nested expressions and ensuring no interference between variables in different scopes.

Example:
```scala
Scope("innerScope", Block(List(Assign("y", Value(Map("b" -> 0.6))))))
```

### Block
A `Block` is a sequence of expressions executed in the same scope. It allows grouping of multiple expressions, which are evaluated in order.

Example:
```scala
Block(List(
Assign("x", Value(Map("a" -> 0.5))),
Assign("y", Value(Map("b" -> 0.8)))
))
```

### Union
Performs a union operation between two fuzzy sets, selecting the maximum membership value for each element. This is the `OR` operation in fuzzy logic.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5))
val setB = Value(Map("b" -> 0.6, "c" -> 0.8))
val unionExp = Union(setA, setB)
```

### Intersection
Computes the intersection of two fuzzy sets, using the minimum membership value for each element.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5))
val setB = Value(Map("b" -> 0.6, "c" -> 0.8))
val intersectionExp = Intersection(setA, setB)
```

### Addition
Adds two fuzzy sets by combining values element-wise, with the result capped at a maximum value of 1.0.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5))
val setB = Value(Map("b" -> 0.6, "c" -> 0.8))
val additionExp = Addition(setA, setB)
```

### Multiplication
Multiplies two fuzzy sets by combining values element-wise.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5))
val setB = Value(Map("b" -> 0.6, "c" -> 0.8))
val multiplicationExp = Multiplication(setA, setB)
```

### Complement
Calculates the complement of a fuzzy set, where each membership value is subtracted from 1.

Example:
```scala
val set = Value(Map("a" -> 0.7, "b" -> 0.5))
val complementExp = Complement(set)
```

### XOR
Calculates the symmetric difference (exclusive OR) between two fuzzy sets.

Example:
```scala
val setA = Value(Map("a" -> 0.7, "b" -> 0.5))
val setB = Value(Map("b" -> 0.6, "c" -> 0.8))
val xorExp = XOR(setA, setB)
```


### MacroDef
Defines a reusable macro with a name, parameters, and body, allowing repetitive operations to be encapsulated.

Example:
```scala
val scaleMacro = MacroDef(
  name = "scale",
  params = List("factor", "input"),
  body = Block(List(
    Assign("scaled_a", Multiplication(Value(Map("a" -> 0.5)), Var("factor"))),
    Assign("scaled_b", Multiplication(Value(Map("b" -> 0.8)), Var("factor"))),
    Union(Var("scaled_a"), Var("scaled_b"))
  ))
)
```

### MacroInvoke
Invokes a previously defined macro with specified arguments. This runs the macro’s body using the passed values for each parameter.

Example:
```scala
MacroInvoke("scale", List(Value(Map("" -> 2.0)), Value(Map("a" -> 0.5, "b" -> 0.8))))
```

### AbstractClassDef
Defines an abstract class, allowing you to specify both abstract and concrete methods. Abstract methods are method signatures without implementations, which must be implemented by subclasses.
    
Example:
```scala
val shapeClass = AbstractClassDef(
  name = "Shape",
  fields = List(Field("x"), Field("y")),
  concreteMethods = List(),
  abstractMethods = List(AbstractMethod("area")),
  constructor = Constructor(
    exp = List(
      Assign("x", Var("x_init")),
      Assign("y", Var("y_init"))
    )
  )
)
```

### ClassDef
Defines a concrete class with fields, methods, a constructor, and optional inheritance from other classes. Supports object-oriented features like encapsulation and inheritance.

Example:
```scala
val pointClass = ClassDef(
  name = "Point",
  fields = List(Field("x"), Field("y")),
  methods = List(
    Method(
      m_name = "move",
      args = List(Assign("dx", Var("dx")), Assign("dy", Var("dy"))),
      exp = List(
        Assign("x", Addition(Var("x"), Var("dx"))),
        Assign("y", Addition(Var("y"), Var("dy")))
      )
    )
  ),
  constructor = Constructor(
    exp = List(
      Assign("x", Var("x_init")),
      Assign("y", Var("y_init"))
    )
  )
)
```

### Public, Private, and Protected
Control access to fields and methods in classes, allowing encapsulation of functionality. These modifiers restrict the visibility and accessibility of class members.

Example:
```scala
Public("Point", List("x", "y"), List("move"))
Private("Point", List("x"), List("move"))
Protected("Point", List("y"), List("move"))
```

### Instantiate
Creates an instance of a concrete class, binding it to a variable in the global scope. An error is raised if instantiation of an abstract class is attempted.

Example:
```scala
Instantiate("p", "Point", Map(
  "x_init" -> Value(Map("" -> 0.0)),
  "y_init" -> Value(Map("" -> 0.0))
))
```

### Method Invocation
Invokes a method on an instance, passing arguments as needed.

Example:
```scala
MethodInvocation(Var("p"), "move", Map(
  "dx" -> Value(Map("" -> 1.0)),
  "dy" -> Value(Map("" -> 2.0))
))
```

## Logging

The project uses SLF4J for logging.

## Conclusion

FuzzyLang provides a powerful set of features for users looking to work with both fuzzy logic and structured object-oriented designs. With its constructs for class and method definitions, abstract classes, inheritance, and fuzzy set operations, FuzzyLang is well-suited for applications requiring both logical precision and organized, extensible code structure.