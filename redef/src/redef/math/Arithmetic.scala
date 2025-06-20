package redef.math

import scala.annotation.targetName
import scala.language.experimental.saferExceptions

import redef.util.Try

/// TODO(@lqhuang):
///   metal not supports syntax like "T throws E" yet, so we use CanThrow[E] instead.
///   Improve it when metal supports it.

trait Arithmetic[T](using CanThrow[ArithmeticException]) {

  inline def negateExact(x: T): T
  inline def addExact(x: T, y: T): T
  inline def subtractExact(x: T, y: T): T
  inline def multiplyExact(x: T, y: T): T
  inline def floorDiv(x: T, y: T): T
  inline def floorMod(x: T, y: T): T

  extension (x: T) {
    inline def unary_-(using CanThrow[E]): T =
      negateExact(x)

    inline def +(y: T)(using CanThrow[E]): T =
      addExact(x, y)

    @targetName("maybeAdd")
    inline def ?+(y: T)(using CanThrow[E]): Option[T] =
      try Option(addExact(x, y))
      catch case ArithmeticException => None

    @targetName("tryAdd")
    inline def +?(y: T): Try[T] =
      Try(addExact(x, y))

    inline def -(y: T)(using CanThrow[E]): T =
      subtractExact(x, y)

    @targetName("maybeSubtract")
    inline def ?-(y: T)(using CanThrow[E]): Option[T] =
      try Option(subtractExact(x, y))
      catch case ArithmeticException => None

    @targetName("trySubtract")
    inline def -?(y: T): Try[T] =
      Try(subtractExact(x, y))

    inline def *(y: T)(using CanThrow[E]): T =
      multiplyExact(x, y)

    @targetName("maybeMultiply")
    inline def ?*(y: T): Option[T] =
      try Option(multiplyExact(x, y))
      catch case ArithmeticException => None

    @targetName("tryMultiply")
    inline def *?(y: T): Try[T] =
      Try(multiplyExact(x, y))

    inline def /(y: T)(using CanThrow[E]): T =
      floorDiv(x, y)

    @targetName("maybeDivide")
    inline def ?/(y: T)(using CanThrow[E]): Option[T] =
      try Option(floorDiv(x, y))
      catch case ArithmeticException => None

    @targetName("tryDivide")
    inline def /?(y: T): Try[T] =
      Try(floorDiv(x, y))

    inline def %(y: T)(using CanThrow[E]): T =
      floorMod(x, y)

    @targetName("maybeMod")
    inline def ?%(y: T)(using CanThrow[E]): Option[T] =
      try Option(floorMod(x, y))
      catch case ArithmeticException => None

    @targetName("tryMod")
    inline def %?(y: T): Try[T] =
      Try(floorMod(x, y))

    inline def abs(using CanThrow[E]): T =
      return if x < 0 then negateExact(x) else x

    inline def absOption(using CanThrow[E]): T =
      return if x < 0 then negateExact(x) else x

    inline def absOption: Option[T] =
      return Try(if x < 0 then negateExact(x) else x).toOption

    inline def absTry: Try[T] =
      Try(if x < 0 then negateExact(x) else x)

  }
}

given IntArithmetic(using
    CanThrow[ArithmeticException]
): Arithmetic[Int] with {
  inline def negateExact(x: Int): Int = math.negateExact(x)
  inline def addExact(x: Int, y: Int): Int = math.addExact(x, y)
  inline def subtractExact(x: Int, y: Int): Int = math.subtractExact(x, y)
  inline def multiplyExact(x: Int, y: Int): Int = math.multiplyExact(x, y)
  inline def floorDiv(x: Int, y: Int): Int = math.floorDiv(x, y)
  inline def floorMod(x: Int, y: Int): Int = math.floorMod(x, y)
  inline def abs(x: Int): Int = if x < 0 then math.negateExact(x) else x
}

given LongArithmetic(using
    CanThrow[ArithmeticException]
): Arithmetic[Long] with {
  inline def negateExact(x: Long): Long = math.negateExact(x)
  inline def addExact(x: Long, y: Long): Long = math.addExact(x, y)
  inline def subtractExact(x: Long, y: Long): Long = math.subtractExact(x, y)
  inline def multiplyExact(x: Long, y: Long): Long = math.multiplyExact(x, y)
  inline def floorDiv(x: Long, y: Long): Long = math.floorDiv(x, y)
  inline def floorMod(x: Long, y: Long): Long = math.floorMod(x, y)
  inline def abs(x: Long): Long = if x < 0 then math.negateExact(x) else x
}
