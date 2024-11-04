package org.cs476.hw2

import org.scalatest.funsuite.AnyFunSuite

class MacroTests extends AnyFunSuite {
  
  import FuzzyExp._
  import FuzzyMath._
  
  test("Macro definition and invocation") {
    val scaleMacro = MacroDef(
      name = "scale",
      params = List("factor", "input"),
      body = Block(List(
        Assign("scaled_a", Value(Map("a" -> 0.5 * 2.0))),
        Assign("scaled_b", Value(Map("b" -> 0.8 * 2.0))),
        Union(Var("scaled_a"), Var("scaled_b"))
      ))
    )
    scaleMacro.eval()

    val scaledSet = MacroInvoke(
      name = "scale",
      args = List(Value(Map("" -> 2.0)), Value(Map("a" -> 0.5, "b" -> 0.8)))
    )
    val result = scaledSet.eval()

    assert(result == Map("a" -> 1.0, "b" -> 1.6))
  }
}
