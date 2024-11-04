package org.cs476.hw2

import org.scalatest.funsuite.AnyFunSuite

class AccessControlTests extends AnyFunSuite {

  import FuzzyExp._
  import FuzzyMath._

  // Helper method to reset global scope and class registry before each test
  def resetEnvironment(): Unit = {
    FuzzyMath.resetGlobalScope()
    FuzzyMath.classRegistry.clear()
    FuzzyMath.accessControlRegistry.clear()
    FuzzyMath.instanceRegistry.clear()
    FuzzyMath.virtualDispatchTable.clear()
  }

  test("Public access control") {
    resetEnvironment()

    // Define TestClass with a public field and method
    val classDef = ClassDef(
      name = "TestClass",
      fields = List(Field("publicField")),
      methods = List(Method(
        m_name = "publicMethod",
        args = List(),
        exp = List(Value(Map("result" -> 1.0)))
      )),
      constructor = Constructor(List()),
      parent = None
    )
    classDef.eval()

    // Apply Public access control
    val publicAccess = Public("TestClass", List("publicField"), List("publicMethod"))
    publicAccess.eval()

    // Instantiate the class
    val instance = Instantiate("t", "TestClass", Map())
    instance.eval()

    // Invoke the public method and capture the result
    val result = MethodInvocation(Var("t"), "publicMethod", Map()).eval()
    assert(result == Map("result" -> 1.0), s"Expected Map('result' -> 1.0) but got $result")
  }

  test("Private access control") {
    resetEnvironment()

    // Define TestClass with a private field and method
    val classDef = ClassDef(
      name = "TestClass",
      fields = List(Field("privateField")),
      methods = List(Method(
        m_name = "privateMethod",
        args = List(),
        exp = List(Value(Map("result" -> 1.0)))
      )),
      constructor = Constructor(List()),
      parent = None
    )
    classDef.eval()

    // Apply Private access control
    val privateAccess = Private("TestClass", List("privateField"), List("privateMethod"))
    privateAccess.eval()

    // Instantiate the class
    val instance = Instantiate("t", "TestClass", Map())
    instance.eval()

    // Attempt to invoke the private method and expect an exception
    assertThrows[Exception] {
      MethodInvocation(Var("t"), "privateMethod", Map()).eval()
    }
  }

  test("Protected access control") {
    resetEnvironment()

    // Define TestClass with a protected field and method
    val classDef = ClassDef(
      name = "TestClass",
      fields = List(Field("protectedField")),
      methods = List(Method(
        m_name = "protectedMethod",
        args = List(),
        exp = List(Value(Map("result" -> 1.0)))
      )),
      constructor = Constructor(List()),
      parent = None
    )
    classDef.eval()

    // Apply Protected access control
    val protectedAccess = Protected("TestClass", List("protectedField"), List("protectedMethod"))
    protectedAccess.eval()

    // Instantiate the class
    val instance = Instantiate("t", "TestClass", Map())
    instance.eval()

    // Invoke the protected method and capture the result
    val result = MethodInvocation(Var("t"), "protectedMethod", Map()).eval()
    assert(result == Map("result" -> 1.0), s"Expected Map('result' -> 1.0) but got $result")
  }
}
