package org.cs476.hw2

import org.scalatest.funsuite.AnyFunSuite

class ClassTests extends AnyFunSuite {

  import FuzzyExp._
  import FuzzyMath._

  def resetEnvironment(): Unit = {
    FuzzyMath.resetGlobalScope()
    FuzzyMath.classRegistry.clear()
    FuzzyMath.accessControlRegistry.clear()
    FuzzyMath.instanceRegistry.clear()
    FuzzyMath.virtualDispatchTable.clear()
  }

  test("Simple class definition and instantiation") {
    resetEnvironment()

    // Define a simple class with one field and one method
    val simpleClass = ClassDef(
      name = "SimpleClass",
      fields = List(Field("value")),
      methods = List(
        Method(
          m_name = "increment",
          args = List(),
          exp = List(
            Assign("value", Addition(Var("value"), Value(Map("" -> 1.0))))
          )
        )
      ),
      constructor = Constructor(
        exp = List(
          Assign("value", Var("initialValue"))
        )
      ),
      parent = None
    )
    simpleClass.eval()

    // Instantiate the class
    val instance = Instantiate("sc", "SimpleClass", Map(
      "initialValue" -> Value(Map("" -> 0.0))
    ))
    instance.eval()

    // Invoke the method
    MethodInvocation(Var("sc"), "increment", Map()).eval()

    // Check the field value
    val fields = FuzzyMath.getInstanceFields("sc", FuzzyMath.getGlobalScope)
    assert(fields == Map("value" -> Map("" -> 1.0)))
  }
}