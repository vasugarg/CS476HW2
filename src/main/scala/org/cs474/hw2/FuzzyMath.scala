package org.cs474.hw2

import org.slf4j.Logger
import org.cs474.hw2.utils.CreateLogger

import scala.collection.mutable

object FuzzyMath:
  val logger: Logger = CreateLogger(this.getClass)
  private type FuzzySet = Map[String, Double]
  import FuzzyExp._

  private val globalScope = new FuzzyScope("global", None)
  private val scopeRegistry: mutable.Map[String, FuzzyScope] = mutable.Map.empty

  // Class Registry to store class definitions
  private val classRegistry: mutable.Map[String, mutable.Map[String, Any]] = mutable.Map.empty

  // Access control registry to keep track of Private, Protected, and Public access levels
  private val accessControlRegistry: mutable.Map[String, mutable.Map[String, mutable.Map[String, mutable.Set[String]]]] = mutable.Map.empty

  // Instance registry to store instantiated objects
  private val instanceRegistry: mutable.Map[String, mutable.Map[String, Any]] = mutable.Map.empty

  // Virtual Dispatch Table for maintaining methods for inherited classes.
  private val virtualDispatchTable: mutable.Map[String, mutable.Map[String, Method]] = mutable.Map()

  // Registry to store macros
  private val macroRegistry: mutable.Map[String, (List[String], FuzzyExp)] = mutable.Map.empty


//  // Class Registry to store class definitions
//  private val classRegistry: mutable.Map[String, ClassDef] = mutable.Map.empty
//
//  // Access control registry to keep track of Private, Protected, and Public access levels
//  private val accessControlRegistry: mutable.Map[Any, Any] = mutable.Map.empty
//
//  // Instance registry to store instantiated objects
//  private val instanceRegistry: mutable.Map[Any, Any] = mutable.Map.empty
//
//  // Virtual Dispatch Table for maintaining methods for inherited classes.
//  private val virtualDispatchTable: scala.collection.mutable.Map[Any, Any] = mutable.Map.empty

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

        // Ensure abstract methods are not marked as final
        abstractMethods.foreach {
          case AbstractMethod(name) if concreteMethods.exists(_.m_name == name) =>
            throw new Exception(s"Method '$name' cannot be both final and abstract")
          case _ =>
        }

        // Register the abstract class and store abstract methods
        val abstractMethodNames = abstractMethods.map(_.name)
        val fieldMap = fields.map(f => f.f_name -> null).to(mutable.Map)
        val methodMap = concreteMethods.map(m => m.m_name -> m).to(mutable.Map)

        classRegistry(name) = mutable.Map(
          "fields" -> fieldMap,
          "methods" -> methodMap,
          "abstract" -> true,  // Indicate that this is an abstract class
          "abstractMethods" -> abstractMethodNames,
          "constructor" -> constructor
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
            "abstractDef" -> false
          )

          // Handle inheritance
          parent.foreach { parentClassDef =>
              val parentName = parentClassDef.name
              val parentClassMap = classRegistry.getOrElse(parentName, throw new Exception(s"Parent class '$parentName' not found"))
              tempClassMap("inheritance") = true
              tempClassMap("fields").asInstanceOf[mutable.Map[String, Any]] ++= parentClassMap("fields").asInstanceOf[mutable.Map[String, Any]]
              tempClassMap("methods").asInstanceOf[mutable.Map[String, Method]] ++= parentClassMap("methods").asInstanceOf[mutable.Map[String, Method]]
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

      // Implementing Public access modifier
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

      // Implementing Private access modifier
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

      // Implementing Protected access modifier
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
        else
          // Generate a unique instance name
          val instanceName = s"${className}_instance_${instanceRegistry.size}"

          // Create a map for the instance to store class name, fields, and methods
          val tempNewObjectMap = mutable.Map[String, Any]()
          tempNewObjectMap += ("className" -> className)

          // Extract class object from classRegistry
          val classObject = classRegistry(className)

          // Extract fields of the class
          val classFields = classObject("fields").asInstanceOf[mutable.Map[String, Any]].clone()

          // Extract methods of the class
          val classMethods = classObject("methods").asInstanceOf[mutable.Map[String, Method]]

          // Create a copy of the fields and methods in the instance
          tempNewObjectMap += ("fields" -> classFields)
          tempNewObjectMap += ("methods" -> classMethods)

          // Bind the instance name to the instance map
          instanceRegistry(instanceName) = tempNewObjectMap

          // Execute constructor
          val constructor = classObject("constructor").asInstanceOf[Constructor]
          val constructorScope = new FuzzyScope(s"${className}_constructor", Some(currentScope))

          // Set fields in constructor scope
          classFields.foreach { case (k, v) =>
            constructorScope.createBinding(k, v)
          }

          // Evaluate arguments and store them in the constructor scope
          args.foreach { case (k, vExp) =>
            val value = vExp.evalInScope(currentScope)
            constructorScope.createBinding(k, value)
          }

          // Execute constructor expressions
          constructor.exp.foreach { exp =>
            exp.evalInScope(constructorScope)
          }

          // Update instance fields after constructor execution
          classFields.keys.foreach { k =>
            classFields(k) = constructorScope.searchBinding(k).getOrElse(classFields(k))
          }
          currentScope.createBinding(varName, instanceName)

          logger.info(s"Created instance '$instanceName' of class '$className'")
          Map.empty[String, Double]

      // Implementing MethodInvocation
      case MethodInvocation(instanceVar, methodName, arguments) =>
        // Evaluate the instance expression to get the instance name
        val instanceName = instanceVar match
          case Var(name) =>
            currentScope.searchBinding(name) match
              case Some(value: String) => value
              case _ => throw new Exception(s"Variable '$name' does not contain a valid instance")
          case _ => throw new Exception("Invalid instance for method invocation")

        if !instanceRegistry.contains(instanceName) then
          throw new Exception(s"Instance '$instanceName' not found")
        else
          val instanceData = instanceRegistry(instanceName)

          // Check if method exists
          val methods = instanceData("methods").asInstanceOf[mutable.Map[String, Method]]

          // Check Virtual Dispatch Table for overridden methods
          val className = instanceData("className").asInstanceOf[String]
          val methodToInvoke = if virtualDispatchTable.contains(className) && virtualDispatchTable(className).contains(methodName) then
            virtualDispatchTable(className)(methodName)
          else if methods.contains(methodName) then
            methods(methodName)
          else
            throw new Exception(s"Method '$methodName' not found in class '$className'")

          val method = methodToInvoke

          // Create a new scope for method execution
          val methodScope = new FuzzyScope(s"${className}_$methodName", Some(currentScope))

          // Set method arguments in scope
          method.args.foreach {
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

          // Execute method body
          method.exp.foreach { exp =>
            exp.evalInScope(methodScope)
          }

          // Update instance fields after method execution
          instanceFields.keys.foreach { k =>
            instanceFields(k) = methodScope.searchBinding(k).getOrElse(instanceFields(k))
          }

          logger.info(s"Invoked method '$methodName' on instance '$instanceName'")
          Map.empty[String, Double]

      case _ =>
        throw new Exception(s"Unhandled case: $exp")