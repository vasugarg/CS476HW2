package org.cs476.hw2

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestFuzzySetOperations extends AnyFlatSpec with Matchers:

  import FuzzyExp._
  import FuzzyMath._

  behavior of "FuzzySet Operations"

  // Helper method to reset the global scope before each test
  def resetGlobalScope(): Unit =
    FuzzyMath.resetGlobalScope()

  it should "perform addition of two fuzzy sets correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.2, "x2" -> 0.5)
    val fset2 = Map("x1" -> 0.3, "x2" -> 0.4)

    val expected = Map("x1" -> 0.5, "x2" -> 0.9)
    val result = Addition(Value(fset1), Value(fset2)).eval()
    result shouldEqual expected
  }

  it should "perform multiplication of two fuzzy sets correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.6, "x2" -> 0.8)
    val fset2 = Map("x1" -> 0.5, "x2" -> 0.3)

    val expected = Map("x1" -> 0.3, "x2" -> 0.24)
    val result = Multiplication(Value(fset1), Value(fset2)).eval()
    result shouldEqual expected
  }

  it should "perform union of two fuzzy sets correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.2, "x2" -> 0.7)
    val fset2 = Map("x1" -> 0.6, "x3" -> 0.5)

    val expected = Map("x1" -> 0.6, "x2" -> 0.7, "x3" -> 0.5)
    val result = Union(Value(fset1), Value(fset2)).eval()
    result shouldEqual expected
  }

  it should "perform intersection of two fuzzy sets correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.8, "x2" -> 0.4)
    val fset2 = Map("x1" -> 0.5, "x2" -> 0.6)

    val expected = Map("x1" -> 0.5, "x2" -> 0.4)
    val result = Intersection(Value(fset1), Value(fset2)).eval()
    result shouldEqual expected
  }

  it should "perform complement of a fuzzy set correctly" in {
    resetGlobalScope()
    val fset = Map("x1" -> 0.7, "x2" -> 0.2)

    val expected = Map("x1" -> 0.3, "x2" -> 0.8)
    val result = Complement(Value(fset)).eval()
    result shouldEqual expected
  }

  it should "perform XOR of two fuzzy sets correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.7, "x2" -> 0.2)
    val fset2 = Map("x1" -> 0.4, "x2" -> 0.5)

    val expected = Map("x1" -> 0.3, "x2" -> 0.3)
    val result = XOR(Value(fset1), Value(fset2)).eval()
    result shouldEqual expected
  }

  it should "handle variables and assignments correctly" in {
    resetGlobalScope()
    val fset = Map("x1" -> 0.5, "x2" -> 0.7)
    Assign("A", Value(fset)).eval()

    val result = Var("A").eval()
    result shouldEqual fset
  }

  it should "evaluate expressions with variables correctly" in {
    resetGlobalScope()
    val fset1 = Map("x1" -> 0.5, "x2" -> 0.7)
    val fset2 = Map("x1" -> 0.3, "x2" -> 0.6)
    Assign("A", Value(fset1)).eval()
    Assign("B", Value(fset2)).eval()

    val expected = Map("x1" -> 0.8, "x2" -> 1.0)
    val expr = Addition(Var("A"), Var("B"))
    val result = expr.eval()
    result shouldEqual expected
  }

  it should "handle scope correctly" in {
    resetGlobalScope()
    Assign("A", Value(Map("x1" -> 0.5))).eval()
    val scopeExpr = Scope("testScope", Assign("A", Value(Map("x1" -> 0.8))))
    scopeExpr.eval()

    // Variable 'A' in the global scope should remain unchanged
    val globalA = Var("A").eval()
    globalA shouldEqual Map("x1" -> 0.5)

    // Variable 'A' in the scope should be 0.8
    val result = Scope("testScope", Var("A")).eval()
    result shouldEqual Map("x1" -> 0.8)
  }

  it should "throw an exception when variable not found" in {
    resetGlobalScope()
    an [Exception] should be thrownBy {
      Var("NonExistentVar").eval()
    }
  }
