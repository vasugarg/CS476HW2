package org.cs476.hw2

import org.scalatest.funsuite.AnyFunSuite

class AbstractClassTests extends AnyFunSuite {

  import FuzzyExp._
  import FuzzyMath._

  test("Abstract class definition and instantiation with concrete subclass") {
    // Define the abstract class 'Shape'
    val abstractClass = AbstractClassDef(
      name = "Shape",
      fields = List(Field("color")),
      concreteMethods = List(),
      abstractMethods = List(AbstractMethod("area")),
      constructor = Constructor(List())
    )
    abstractClass.eval()

    // Define the concrete class 'Circle' that extends 'Shape' and implements 'area'
    val concreteClass = ClassDef(
      name = "Circle",
      fields = List(Field("radius")),
      methods = List(
        Method(
          m_name = "area",
          args = List(),
          exp = List(Assign("area", Value(Map("area" -> 3.14))))
        )
      ),
      constructor = Constructor(List()),
      parent = Some(abstractClass) // No casting needed
    )
    concreteClass.eval()

    // Instantiate 'Circle' and invoke the 'area' method
    val instance = Instantiate("c", "Circle", Map())
    instance.eval()

    val result = MethodInvocation(Var("c"), "area", Map()).eval()
    assert(result == Map("area" -> 3.14))
  }
}
