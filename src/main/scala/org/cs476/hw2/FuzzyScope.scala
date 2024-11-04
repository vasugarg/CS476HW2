package org.cs476.hw2

import scala.collection.mutable
import org.cs476.hw2.utils.CreateLogger
import org.slf4j.Logger

class FuzzyScope(
                      val name: String,
                      val parentScope: Option[FuzzyScope] = None,
                      private val bindings: mutable.Map[String, Any] = mutable.Map.empty,
                      private val children: mutable.ListBuffer[FuzzyScope] = mutable.ListBuffer.empty
                    ):
  val logger: Logger = CreateLogger(this.getClass)

  // Create a binding in the current scope; throw an exception if it already exists
  def createBinding(name: String, value: Any): Unit =
    if bindings.contains(name) then
      logger.info(s"Variable '$name' already exists in scope '${this.name}'")
      throw new Exception(s"Duplicate variable declaration: '$name' in scope '${this.name}'")
    else
      bindings(name) = value
      logger.info(s"Created binding for variable '$name' in scope '${this.name}' with value: $value")

  // Update or set a variable in the current scope
  def setVariable(name: String, value: Any): Unit =
    if bindings.contains(name) then
      bindings(name) = value
      logger.info(s"Updated variable '$name' in scope '${this.name}' with value: $value")
    else
      // Variable doesn't exist in current scope; create it
      createBinding(name, value)

  // Search for a binding in the current scope or parent scopes
  def searchBinding(name: String): Option[Any] =
    bindings.get(name) match
      case Some(value) =>
        logger.info(s"Found variable '$name' in scope '${this.name}' with value: $value")
        Some(value)
      case None =>
        parentScope match
          case Some(parent) =>
            logger.info(s"Variable '$name' not found in scope '${this.name}', checking parent scope")
            parent.searchBinding(name)
          case None =>
            logger.info(s"Variable '$name' not found in any scope")
            None

  def getOrCreateChildScope(scopeName: String): FuzzyScope =
    children.find(_.name == scopeName) match
      case Some(existingChild) =>
        logger.info(s"Found existing child scope '$scopeName' under scope '${this.name}'")
        existingChild
      case None =>
        // Create a new child scope and add it to the children list
        val childScope = new FuzzyScope(scopeName, Some(this))
        children += childScope
        logger.info(s"Created child scope '$scopeName' under scope '${this.name}'")
        childScope

  // Find a scope by its name (searches current and child scopes)
  def findScope(scopeName: String): Option[FuzzyScope] =
    if this.name == scopeName then
      Some(this)
    else
      // Search in child scopes
      children.view.flatMap(_.findScope(scopeName)).headOption

  // Find a scope by a path of scope names
  def findScopeByPath(path: List[String]): Option[FuzzyScope] = path match
    case Nil => Some(this)
    case head :: tail =>
      children.find(_.name == head).flatMap(_.findScopeByPath(tail))

  // Clear all bindings in this scope
  def clearBindings(): Unit =
    bindings.clear()
    logger.info(s"Cleared all bindings in scope '${this.name}'")

  // Method to print the current scope and its variables (for debugging)
  def printScope(indent: Int = 0): Unit =
    val indentation = "  " * indent
    println(s"${indentation}Scope: ${this.name}")
    println(s"${indentation}Variables: ${bindings.map { case (k, v) => s"$k -> $v" }.mkString(", ")}")
    children.foreach(_.printScope(indent + 1))






