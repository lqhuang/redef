package redef.math

import scala.annotation.targetName
import scala.math.{Numeric, Ordered}

import redef.util.SafeTry

class OverflowException extends Exception

trait SafeArithmetic[T, E <: OverflowException](using Numeric[T]) {
  def negateExact(x: T): T throws E
  def addExact(x: T, y: T): T throws E
  def subtractExact(x: T, y: T): T throws E
  def multiplyExact(x: T, y: T): T throws E
  def floorDivExact(x: T, y: T): T throws E
  def floorModExact(x: T, y: T): T throws E

  extension (x: T) {
    def unary_- : T throws E = negateExact(x)

    def +(y: T): T throws E = addExact(x, y)

    @targetName("tryAdd")
    def +?(y: T): SafeTry[T, E] = SafeTry(addExact(x, y))

    def -(y: T): T throws E = subtractExact(x, y)

    @targetName("trySubtract")
    def -?(y: T): SafeTry[T, E] = SafeTry(subtractExact(x, y))

    def *(y: T): T throws E = multiplyExact(x, y)

    @targetName("tryMultiply")
    def *?(y: T): SafeTry[T, E] = SafeTry(multiplyExact(x, y))

    def /(y: T): T throws E = floorDivExact(x, y)

    @targetName("tryDivide")
    def /?(y: T): SafeTry[T, E] = SafeTry(floorDivExact(x, y))

    def %(y: T): T throws E = floorModExact(x, y)

    @targetName("tryMod")
    def %?(y: T): SafeTry[T, E] = SafeTry(floorModExact(x, y))

    def abs: T throws E =
      if Numeric[T].lt(x, Numeric[T].zero) then negateExact(x) else x

    def absTry: SafeTry[T, E] = SafeTry(abs)
  }
}


given SafeArithmetic[Int, OverflowException](using Numeric[Int]):
  def negateExact(x: Int): Int throws OverflowException  =
    try {
      math.negateExact(x)
    } catch {
      case e: ArithmeticException => throw new OverflowException
    }
  def addExact(x: Int, y: Int): Int throws OverflowException = math.addExact(x, y)
  def subtractExact(x: Int, y: Int): Int throws OverflowException = math.subtractExact(x, y)
  def multiplyExact(x: Int, y: Int): Int throws OverflowException = math.multiplyExact(x, y)
  def floorDivExact(x: Int, y: Int): Int throws OverflowException = math.floorDiv(x, y)
  def floorModExact(x: Int, y: Int): Int throws OverflowException = math.floorMod(x, y)


given SafeArithmetic[Long, OverflowException](using Numeric[Long]):
  def negateExact(x: Long): Long throws OverflowException = math.negateExact(x)
  def addExact(x: Long, y: Long): Long throws OverflowException = math.addExact(x, y)
  def subtractExact(x: Long, y: Long): Long throws OverflowException = math.subtractExact(x, y)
  def multiplyExact(x: Long, y: Long): Long throws OverflowException = math.multiplyExact(x, y)
  def floorDivExact(x: Long, y: Long): Long throws OverflowException = math.floorDiv(x, y)
  def floorModExact(x: Long, y: Long): Long throws OverflowException = math.floorMod(x, y)

