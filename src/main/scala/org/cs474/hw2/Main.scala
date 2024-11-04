package org.cs474.hw2

import org.cs474.hw2.FuzzyExp._
import org.cs474.hw2.FuzzyMath._

object Main:

  private def defineClass(name: String, fields: List[Field], methods: List[Method], constructor: Constructor, parent: Option[ClassDef] = None): ClassDef =
    val classDef = ClassDef(name, fields, methods, constructor, parent)
    classDef.eval()
    Public(name, fields.map(_.f_name), methods.map(_.m_name)).eval()
    classDef.asInstanceOf[ClassDef]

  private def instantiateClass(varName: String, className: String, args: Map[String, Value]): Unit =
    val instance = Instantiate(varName, className, args)
    instance.eval()

  private def invokeMethod(instanceVar: Var, methodName: String, arguments: Map[String, Value]): Unit =
    MethodInvocation(instanceVar, methodName, arguments).eval()

  @main def runExample(): Unit =
    val pointClass = defineClass(
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

    instantiateClass("p", "Point", Map(
      "x_init" -> Value(Map("" -> 0.0)),
      "y_init" -> Value(Map("" -> 0.0))
    ))

    invokeMethod(Var("p"), "move", Map(
      "dx" -> Value(Map("" -> 1.0)),
      "dy" -> Value(Map("" -> 2.0))
    ))

    val fields = FuzzyMath.getInstanceFields("p", FuzzyMath.getGlobalScope)
    println(fields)

  @main def runInheritanceExample(): Unit =
    val baseClass = defineClass(
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

    val derivedClass = defineClass(
      name = "ColoredPoint",
      fields = List(Field("color")),
      methods = List(
        Method(
          m_name = "setColor",
          args = List(Assign("newColor", Var("newColor"))),
          exp = List(
            Assign("color", Var("newColor"))
          )
        )
      ),
      constructor = Constructor(
        exp = List(
          Assign("x", Var("x_init")),
          Assign("y", Var("y_init")),
          Assign("color", Var("color_init"))
        )
      ),
      parent = Some(baseClass)
    )

    instantiateClass("cp", "ColoredPoint", Map(
      "x_init" -> Value(Map("" -> 0.0)),
      "y_init" -> Value(Map("" -> 0.0)),
      "color_init" -> Value(Map("" -> 1.0))
    ))

    invokeMethod(Var("cp"), "move", Map(
      "dx" -> Value(Map("" -> 1.0)),
      "dy" -> Value(Map("" -> 2.0))
    ))

    invokeMethod(Var("cp"), "setColor", Map(
      "newColor" -> Value(Map("" -> 2.0))
    ))

    val fields = FuzzyMath.getInstanceFields("cp", FuzzyMath.getGlobalScope)
    println(s"Fields of 'cp': $fields")

  @main def runMacroExample(): Unit =
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

    println(result) // Expected output: {"a" -> 1.0, "b" -> 1.6}