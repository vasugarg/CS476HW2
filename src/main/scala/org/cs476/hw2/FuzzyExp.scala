package org.cs476.hw2

enum FuzzyExp:
  case Value(i: Map[String, Double])
  case Var(s: String)
  case Union(fset1: FuzzyExp, fset2: FuzzyExp)
  case Intersection(fset1: FuzzyExp, fset2: FuzzyExp)
  case XOR(fset1: FuzzyExp, fset2: FuzzyExp)
  case Complement(fset: FuzzyExp)
  case Addition(fset1: FuzzyExp, fset2: FuzzyExp)
  case Multiplication(fset1: FuzzyExp, fset2: FuzzyExp)
  case Scope(name: String, body: FuzzyExp)
  case Assign(name: String, expr: FuzzyExp)
  case Block(expressions: List[FuzzyExp])

  case MacroDef(name: String, params: List[String], body: FuzzyExp)
  case MacroInvoke(name: String, args: List[FuzzyExp])

  case Field(f_name: String)
  case Method(m_name: String, args: List[Assign], exp:List[FuzzyExp])
  case Constructor(exp:List[FuzzyExp])
  case ClassDef(
                 name: String,
                 fields: List[Field],
                 methods: List[Method],
                 constructor: Constructor,
                 parent: Option[FuzzyExp]
               )
  case Instantiate(varName: String, className: String, args: Map[String, FuzzyExp])
  case MethodInvocation(instanceVar: Var, methodName: String, arguments: Map[String, FuzzyExp])

  case Public(name: String, fieldNameList: List[String], methodNameList: List[String])
  case Private(name: String, fieldNameList: List[String], methodNameList: List[String])
  case Protected(name: String, fieldNameList: List[String], methodNameList: List[String])

  case AbstractMethod(name: String)
  case AbstractClassDef(name: String, fields: List[Field], concreteMethods: List[Method], abstractMethods: List[AbstractMethod], constructor: Constructor)




