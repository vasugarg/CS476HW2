package org.cs476.hw2

import org.cs476.hw2.utils.CreateLogger
import org.slf4j.Logger

import scala.collection.mutable

object FuzzyMath:
  val logger: Logger = CreateLogger(this.getClass)
  private type FuzzySet = Map[String, Double]
  import FuzzyExp._

  private val globalScope = new FuzzyScope("global", None)
  private val scopeRegistry: mutable.Map[String, FuzzyScope] = mutable.Map.empty

  // Class Registry to store class definitions
  val classRegistry: mutable.Map[String, mutable.Map[String, Any]] = mutable.Map.empty

  // Access control registry to keep track of Private, Protected, and Public access levels
  val accessControlRegistry: mutable.Map[String, mutable.Map[String, mutable.Map[String, mutable.Set[String]]]] = mutable.Map.empty

  // Instance registry to store instantiated objects
  val instanceRegistry: mutable.Map[String, mutable.Map[String, Any]] = mutable.Map.empty

  // Virtual Dispatch Table for maintaining methods for inherited classes.
  val virtualDispatchTable: mutable.Map[String, mutable.Map[String, Method]] = mutable.Map()

  // Registry to store macros
  val macroRegistry: mutable.Map[String, (List[String], FuzzyExp)] = mutable.Map.empty

  // Provide a method to get the global scope
  def getGlobalScope: FuzzyScope = globalScope

  // Provide a method to get instance fields
  def getInstanceFields(instanceVarName: String, currentScope: FuzzyScope): Map[String, Any] =
    currentScope.searchBinding(instanceVarName) match
      case Some(instanceName: String) =>
        instanceRegistry.get(instanceName) match
          case Some(instanceData) =>
            instanceData("fields").asInstanceOf[mutable.Map[String, Any]].toMap
          case None =>
            throw new Exception(s"Instance '$instanceName' not found")
      case _ =>
        throw new Exception(s"Variable '$instanceVarName' does not contain a valid instance")

  def resetGlobalScope(): Unit =
    globalScope.clearBindings()
    scopeRegistry.clear()

  def round(value: Double, places: Int = 6): Double =
    val scale = math.pow(10, places)
    (value * scale).round / scale

  extension (exp: FuzzyExp)
    def eval(): FuzzySet =
      exp.evalInScope(globalScope)

    private def evalInScope(currentScope: FuzzyScope): FuzzySet = exp match
      case Value(i) => i

      case Var(name) =>
        currentScope.searchBinding(name) match
          case Some(value: Map[String, Double]) => value
          case Some(other) => throw new Exception(s"Variable '$name' is not a FuzzySet")
          case None => throw new Exception(s"Variable '$name' not found in any scope")

      case Assign(name, expr) =>
        val value = expr.evalInScope(currentScope).map { case (k, v) => k -> round(v) }
        currentScope.setVariable(name, value)
        currentScope.logger.info(s"Assigned Variable('$name') in scope '${currentScope.name}' with value: $value")
        value

      case Scope(scopeName, body) =>
        val newScope = scopeRegistry.getOrElseUpdate(scopeName, currentScope.getOrCreateChildScope(scopeName))
        currentScope.logger.info(s"Entering Scope('$scopeName')")
        val result = body.evalInScope(newScope)
        currentScope.logger.info(s"Exiting Scope('$scopeName')")
        result

      case Block(expressions) =>
        val resultHolder = scala.collection.mutable.Map.empty[String, Double]
        expressions.foreach { expr =>
          val result = expr.evalInScope(currentScope)
          resultHolder.clear()
          resultHolder ++= result
        }
        resultHolder.toMap

      case Union(fset1, fset2) =>
        val set1 = fset1.evalInScope(currentScope)
        val set2 = fset2.evalInScope(currentScope)
        set1 ++ set2.map { case (k, v) => k -> round(math.max(v, set1.getOrElse(k, 0.0))) }

      case Intersection(fset1, fset2) =>
        val set1 = fset1.evalInScope(currentScope)
        val set2 = fset2.evalInScope(currentScope)
        set1.filter { case (k, _) => set2.contains(k) }.map { case (k, v) => k -> round(math.min(v, set2(k))) }

      case Addition(fset1, fset2) =>
        val set1 = fset1.evalInScope(currentScope)
        val set2 = fset2.evalInScope(currentScope)
        set1 ++ set2.map { case (k, v) => k -> round(math.min(v + set1.getOrElse(k, 0.0), 1.0)) }

      case Multiplication(fset1, fset2) =>
        val set1 = fset1.evalInScope(currentScope)
        val set2 = fset2.evalInScope(currentScope)
        set1.filter { case (k, _) => set2.contains(k) }.map { case (k, v) => k -> round(v * set2(k)) }

      case Complement(fset) =>
        val set = fset.evalInScope(currentScope)
        set.map { case (k, v) => k -> round(1.0 - v) }

      case XOR(fset1, fset2) =>
        val set1 = fset1.evalInScope(currentScope)
        val set2 = fset2.evalInScope(currentScope)
        val allKeys = set1.keySet ++ set2.keySet
        allKeys.map { key =>
          val val1 = set1.getOrElse(key, 0.0)
          val val2 = set2.getOrElse(key, 0.0)
          val maxVal = math.max(val1, val2)
          val minVal = math.min(val1, val2)
          key -> round(maxVal - minVal)
        }.toMap

      case MacroDef(name, params, body) =>
        // Register the macro
        macroRegistry(name) = (params, body)
        logger.info(s"Defined macro '$name'")
        Map.empty[String, Double]

      case MacroInvoke(name, args) =>
        // Retrieve the macro definition
        val (params, body) = macroRegistry.getOrElse(name, throw new Exception(s"Macro '$name' not found"))

        // Check if the number of arguments matches the number of parameters
        if params.length != args.length then
          throw new Exception(s"Macro '$name' expects ${params.length} arguments, but got ${args.length}")

        // Create a temporary scope for macro execution
        val macroScope = new FuzzyScope(s"${name}_macro_scope", Some(currentScope))

        // Bind arguments to parameters in the macro scope
        params.zip(args).foreach { case (param, arg) =>
          val argValue = arg.evalInScope(currentScope)
          macroScope.createBinding(param, argValue)
        }

        // Evaluate the macro body in the macro scope
        body.evalInScope(macroScope)

      case AbstractClassDef(name, fields, concreteMethods, abstractMethods, constructor) =>
        if classRegistry.contains(name) then
          throw new Exception(s"Class '$name' already defined")

        abstractMethods.foreach {
          case AbstractMethod(name) if concreteMethods.exists(_.m_name == name) =>
            throw new Exception(s"Method '$name' cannot be both final and abstract")
          case _ =>
        }
        val abstractMethodNames = abstractMethods.map(_.name)
        val fieldMap = fields.map(f => f.f_name -> null).to(mutable.Map)
        val methodMap = concreteMethods.map(m => m.m_name -> m).to(mutable.Map)

        classRegistry(name) = mutable.Map(
          "fields" -> fieldMap,
          "methods" -> methodMap,
          "abstract" -> true,
          "abstractMethods" -> abstractMethodNames,
          "constructor" -> constructor
        )

        // Initialize access control for abstract class
        accessControlRegistry(name) = mutable.Map(
          "private" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]()),
          "public" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]()),
          "protected" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]())
        )

        logger.info(s"Defined abstract class '$name' with abstract methods: ${abstractMethodNames.mkString(", ")}")
        Map.empty[String, Double]

      case ClassDef(name, fields, methods, constructor, parent) =>
        if classRegistry.contains(name) then
          throw new Exception(s"Class '$name' already defined")
        else
          val tempClassMap = mutable.Map[String, Any](
            "fields" -> fields.map(f => f.f_name -> null).to(mutable.Map),
            "constructor" -> constructor,
            "methods" -> methods.map(m => m.m_name -> m).to(mutable.Map),
            "inheritance" -> false,
            "abstract" -> false
          )

          // Handle inheritance and check if parent is abstract
          parent.foreach { parentClassDef =>
            parentClassDef match {
              case abstractClass: AbstractClassDef =>
                // Handle abstract class parent by enforcing abstract method implementation
                val abstractMethods = abstractClass.abstractMethods.map(_.name)
                abstractMethods.foreach { methodName =>
                  if (!methods.exists(_.m_name == methodName)) {
                    throw new Exception(s"Class '$name' must implement abstract method '$methodName' from abstract class '${abstractClass.name}'")
                  }
                }

                // Copy fields and methods from abstract class
                tempClassMap("fields").asInstanceOf[mutable.Map[String, Any]] ++= abstractClass.fields.map(f => f.f_name -> null).toMap
                tempClassMap("methods").asInstanceOf[mutable.Map[String, Method]] ++= abstractClass.concreteMethods.map(m => m.m_name -> m).toMap

              case concreteClass: ClassDef =>
                // Handle concrete class parent (existing logic)
                tempClassMap("fields").asInstanceOf[mutable.Map[String, Any]] ++= concreteClass.fields.map(f => f.f_name -> null).toMap
                tempClassMap("methods").asInstanceOf[mutable.Map[String, Method]] ++= concreteClass.methods.map(m => m.m_name -> m).toMap

              case _ =>
                throw new Exception("Invalid parent class type")
            }
          }

          classRegistry(name) = tempClassMap

          // Initialize access control for the class
          accessControlRegistry(name) = mutable.Map(
            "private" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]()),
            "public" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]()),
            "protected" -> mutable.Map("fields" -> mutable.Set[String](), "methods" -> mutable.Set[String]())
          )

          logger.info(s"Defined class '$name'")
          Map.empty[String, Double] // Return empty FuzzySet

      case Public(className, fieldNameList, methodNameList) =>
        if !accessControlRegistry.contains(className) then
          throw new Exception(s"Access control data for class '$className' not found")
        else
          val accessData = accessControlRegistry(className)
          val publicFields = accessData("public")("fields")
          val publicMethods = accessData("public")("methods")
          publicFields ++= fieldNameList
          publicMethods ++= methodNameList
          logger.info(s"Updated public access for class '$className'")
          Map.empty[String, Double]

      case Private(className, fieldNameList, methodNameList) =>
        if !accessControlRegistry.contains(className) then
          throw new Exception(s"Access control data for class '$className' not found")
        else
          val accessData = accessControlRegistry(className)
          val privateFields = accessData("private")("fields")
          val privateMethods = accessData("private")("methods")
          privateFields ++= fieldNameList
          privateMethods ++= methodNameList
          logger.info(s"Updated private access for class '$className'")
          Map.empty[String, Double]

      case Protected(className, fieldNameList, methodNameList) =>
        if !accessControlRegistry.contains(className) then
          throw new Exception(s"Access control data for class '$className' not found")
        else
          val accessData = accessControlRegistry(className)
          val protectedFields = accessData("protected")("fields")
          val protectedMethods = accessData("protected")("methods")
          protectedFields ++= fieldNameList
          protectedMethods ++= methodNameList
          logger.info(s"Updated protected access for class '$className'")
          Map.empty[String, Double]

      case Instantiate(varName, className, args) =>
        if !classRegistry.contains(className) then
          throw new Exception(s"Class '$className' not found")
        if classRegistry(className).getOrElse("abstract", false).asInstanceOf[Boolean] then
          throw new Exception(s"Cannot instantiate abstract class '$className'")

        val instanceName = s"${className}_instance_${instanceRegistry.size}"
        val tempNewObjectMap = mutable.Map[String, Any]("className" -> className)
        val classObject = classRegistry(className)
        val classFields = classObject("fields").asInstanceOf[mutable.Map[String, Any]].clone()
        val classMethods = classObject("methods").asInstanceOf[mutable.Map[String, Method]]

        tempNewObjectMap += ("fields" -> classFields)
        tempNewObjectMap += ("methods" -> classMethods)
        instanceRegistry(instanceName) = tempNewObjectMap

        val constructor = classObject("constructor").asInstanceOf[Constructor]
        val constructorScope = new FuzzyScope(s"${className}_constructor", Some(currentScope))
        classFields.foreach { case (k, v) => constructorScope.createBinding(k, v) }
        args.foreach { case (k, vExp) => constructorScope.createBinding(k, vExp.evalInScope(currentScope)) }

        constructor.exp.foreach { exp => exp.evalInScope(constructorScope) }
        classFields.keys.foreach { k => classFields(k) = constructorScope.searchBinding(k).getOrElse(classFields(k)) }
        currentScope.createBinding(varName, instanceName)

        logger.info(s"Created instance '$instanceName' of class '$className'")
        Map.empty[String, Double]

      case MethodInvocation(instanceVar, methodName, arguments) =>
        val instanceName = instanceVar match
          case Var(name) => currentScope.searchBinding(name) match
            case Some(value: String) => value
            case _ => throw new Exception(s"Variable '$name' does not contain a valid instance")
          case _ => throw new Exception("Invalid instance for method invocation")

        if !instanceRegistry.contains(instanceName) then
          throw new Exception(s"Instance '$instanceName' not found")

        val instanceData = instanceRegistry(instanceName)
        val className = instanceData("className").asInstanceOf[String]

        // Check access control for the method
        val accessData = accessControlRegistry.getOrElse(className, throw new Exception(s"Access control data for class '$className' not found"))
        val isPublic = accessData("public")("methods").contains(methodName)
        val isPrivate = accessData("private")("methods").contains(methodName)
        val isProtected = accessData("protected")("methods").contains(methodName)

        // If the method is private, it should not be accessible outside its defining class
        if isPrivate then throw new Exception(s"Method '$methodName' is private and cannot be accessed")

        // Access the method from either the virtual dispatch table or instance's methods
        val methods = instanceData("methods").asInstanceOf[mutable.Map[String, Method]]
        val methodToInvoke = virtualDispatchTable.getOrElse(className, methods).getOrElse(
          methodName,
          throw new Exception(s"Method '$methodName' not found in class '$className'")
        )

        val methodScope = new FuzzyScope(s"${className}_$methodName", Some(currentScope))

        // Set method arguments in scope
        methodToInvoke.args.foreach {
          case Assign(argName, _) =>
            val argValueExp = arguments.getOrElse(argName, throw new Exception(s"Argument '$argName' not provided"))
            val argValue = argValueExp.evalInScope(currentScope)
            methodScope.createBinding(argName, argValue)
        }

        // Set instance fields in scope
        val instanceFields = instanceData("fields").asInstanceOf[mutable.Map[String, Any]]
        instanceFields.foreach { case (k, v) =>
          methodScope.createBinding(k, v)
        }

        // Execute method body and capture the result of the last expression
        val result = methodToInvoke.exp.map(_.evalInScope(methodScope)).lastOption.getOrElse(Map.empty[String, Double])

        // Update instance fields after method execution
        instanceFields.keys.foreach { k =>
          instanceFields(k) = methodScope.searchBinding(k).getOrElse(instanceFields(k))
        }

        logger.info(s"Invoked method '$methodName' on instance '$instanceName' with result: $result")
        result
