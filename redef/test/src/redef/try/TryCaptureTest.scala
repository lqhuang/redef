import annotation.retains
import language.experimental.{erasedDefinitions, captureChecking, pureFunctions}

class CT[E <: Exception]
type CanThrow[E <: Exception] = CT[E] @retains[caps.cap.type]
type Top = Any @retains[caps.cap.type]

infix type throws[R, E <: Exception] = (erased CanThrow[E]) ?=> R

class Fail extends Exception

def raise[E <: Exception](e: E): Nothing throws E = throw e

def foo(x: Boolean): Int throws Fail =
  if x then 1 else raise(Fail())

/// IDE does not understand `^` symbol
// def handle[E <: Exception,  R <: Top](op: CT[E]^ => R)(handler: E => R): R =
//   val x: CT[E] = ???
//   try op(x)
//   catch case ex: E => handler(ex)

def test =
  val a = handle[Exception, CanThrow[Exception]] { // error // error
    (x: CanThrow[Exception]) => x
  } { (ex: Exception) =>
    ???
  }

/// IDE does not understand `->` symbol
//   val b = handle[Exception, () -> Nothing] { // error
//     (x: CanThrow[Exception]) => () => raise(new Exception)(using x)
//   } {
//     (ex: Exception) => ???
//   }

  val xx = handle { // error
    (x: CanThrow[Exception]) => () =>
      raise(new Exception)(using x)
      22
  } { (ex: Exception) => () =>
    22
  }
  val yy = xx :: Nil
  yy // OK

/// IDE does not understand `->` symbol
// val global: () -> Int = handle { // error
//   (x: CanThrow[Exception]) =>
//     () =>
//       raise(new Exception)(using x)
//       22
// } {
//   (ex: Exception) => () => 22
// }
