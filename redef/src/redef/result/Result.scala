package redef

import scala.collection.immutable.Seq
import scala.annotation.implicitNotFound
import scala.language.experimental.saferExceptions
import scala.language.higherKinds
import language.implicitConversions

sealed abstract class Result[+T, +E] extends Product with Serializable {

  def isOk: Boolean

  def isFailure: Boolean

  // def get: T

  def getOrElse[U >: T](default: => U): U = this match {
    case Ok(t)      => t
    case Failure(_) => default
  }
  // def getOrElse[U >: T, F >: E](op: F => U): U = this match {
  //   case Ok(t)      => t
  //   case Failure(e) => op(e)
  // }

  /**
   * Returns `true` if the result is an `Ok` value containing the given value.
   *
   * Examples
   *
   * ```
   * >>> val x: Result[Int, String] = Ok(2)
   * >>> x.contains(2)
   * true
   *
   * >>> val y: Result[Int, String] = Ok(3)
   * >>> y.contains(2)
   * false
   *
   * >>> val z: Result[Int, String] = Err("Some error message")
   * >>> z.contains(2)
   * false
   * ```
   *
   * @group Query
   */
  final def contains[U >: T](x: => U): Boolean

  final def ok: Option[T]

  final def failure: Option[E]

  def toOption: Option[T] = ok

  // def toOptionFailure: Option[E] = failure

  def toSeq: Seq[T]

  // def toFailureSeq: Seq[E]

  def toEither: Either[E, T]

  def toTry(using ev: E <:< Exception): Try[T]

  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(this)

  def failed: Result[E, T]

  /**
   * Converts from `Result[Result[T, E], E]` to `Result[T, E]`
   *
   * Examples
   *
   * ```
   * >>> val x: Result[Result[String, Int], Int] = Ok(Ok("hello"))
   * >>> x.flatten
   * Ok(hello)
   *
   * >>> val y: Result[Result[String, Int], Int] = Ok(Err(6))
   * >>> y.flatten
   * Err(6)
   *
   * >>> val z: Result[Result[String, Int], Int] = Err(6)
   * >>> z.flatten
   * Err(6)
   *
   * // Flattening only removes one level of nesting at a time:
   * >>> val multi: Result[Result[Result[String, Int], Int], Int] = Ok(Ok(Ok("hello")))
   * >>> multi.flatten
   * Ok(Ok(hello))
   * >>> multi.flatten.flatten
   * Ok(hello)
   * ```
   *
   * @group Transform
   */
  def flatten[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      tv: T <:< Result[U, F]
  ): Result[U, F] = this match {
    case Ok(t) =>
      tv(t) match
        case Ok(u)      => Ok(u)
        case Failure(e) => Failure(e)
    case Failure(e) => Failure(e)
  }

  /**
   * Converts from `Result[Option[T], E]` to `Result[T, E]`
   *
   * Examples
   *
   * ```scala
   * >>> val x: Result[Option[String], Int] = Ok(Some("hello"))
   * >>> x.flatten(-1)
   * Ok(hello)
   *
   * >>> val y: Result[Option[String], Int] = Ok(None)
   * >>> y.flatten(-1)
   * Err(-1)
   *
   * >>> val z: Result[Option[String], Int] = Err(6)
   * >>> z.flatten(-1)
   * Err(6)
   *
   * // Flattening only removes one level of nesting at a time:
   * >>> val multi: Result[Option[Option[String]], Int] = Ok(Some(Some("hello")))
   * >>> multi.flatten(-1)
   * Ok(Some(hello))
   * >>> multi.flatten(-1).flatten(-1)
   * Ok(hello)
   * ```
   *
   * @group Transform
   */
  def flatten[U, F >: E](defaultFailure: => F)(using
      @implicitNotFound("${T} is not a Option[${U}]")
      tv: T <:< Option[U]
  ): Result[U, F] = this match {
    case Ok(ok) =>
      tv(ok) match {
        case Some(u) => Ok(u)
        case None    => Failure(defaultFailure)
      }
    case Failure(e) => Failure(e)
  }

  /**
   * Converts from `Result[T, Result[T, E]]` to `Result[T, E]`
   *
   * Examples
   *
   * ```scala
   * >>> val x: Result[Int, Result[Int, String]] = Err(Err("Some Error"))
   * >>> x.flatten
   * Err("Some Error")
   *
   * >>> val y: Result[Int, Result[Int, String]] = Err(Ok(6))
   * >>> y.flatten
   * Ok(6)
   *
   * >>> val z: Result[Int, Result[Int, String]] = Ok(6)
   * >>> z.flatten
   * Ok(6)
   *
   * // Flattening only removes one level of nesting at a time:
   * >>> val multi: Result[Int, Result[Int, Result[Int, String]]] = Err(Err(Err("Some Error")))
   * >>> multi.flatten
   * Err(Err(Some Error))
   * >>> multi.flatten.flatten
   * Err(Some Error)
   * ```
   *
   * @group Transform
   */
  def flatten[U >: T, F](using
      @implicitNotFound("${E} is not a Result[${U}, ${F}]")
      ev: E <:< Result[U, F]
  ): Result[U, F] = orElse(ev)

  /**
   * Converts from `Result[T, Option[E]]` to `Result[T, E]`
   *
   * Examples
   *
   * ```scala
   * >>> val x: Result[Int, Option[String]] = Err(Some("Some Error"))
   * >>> x.flatten(-1)
   * Err("Some Error")
   *
   * >>> val y: Result[Int, Option[String]] = Err(None)
   * >>> y.flatten(-1)
   * Ok(-1)
   *
   * >>> val z: Result[Int, Option[String]] = Ok(6)
   * >>> z.flatten(-1)
   * Ok(6)
   *
   * // Flattening only removes one level of nesting at a time:
   * >>> val multi: Result[Int, Option[Option[String]]] = Err(Some(Some("Some Error")))
   * >>> multi.flatten(-1)
   * Err(Some("Some Error"))
   * >>> multi.flatten(-1).flatten(-2)
   * Err("Some Error")
   * ```
   *
   * @group Transform
   */
  def flatten[U >: T, F](defaultOk: => U)(using
      @implicitNotFound("${E} is not a Option[${F}]")
      ev: E <:< Option[F]
  ): Result[U, F] = this match {
    case Failure(e) =>
      ev(e) match
        case Some(f) => Failure(f)
        case None    => Ok(defaultOk)
    case Ok(t) => Ok(t)
  }

  /**
   * An alias of [[flatten]] for consistency with `Either` API, analogous to
   * `joinRight`
   *
   * @group Transform
   */
  def joinOk[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      tv: T <:< Result[U, F]
  ): Result[U, F] = flatten(using tv)

  /**
   * An alias of [[flatten]] for consistency with `Either` API, analogous to
   * `joinLeft`
   *
   * @group Transform
   */
  def joinFailure[U >: T, F](using
      @implicitNotFound("${E} is not a Result[${U}, ${F}]")
      ev: E <:< Result[U, F]
  ): Result[U, F] = flatten(using ev)

  def map[U](op: T => U): Result[U, E] = this match {
    case Ok(t)      => Ok(op(t))
    case Failure(e) => Failure(e)
  }

  def orElse[U >: T](default: => Result[U, E]): Result[U, E] = this match {
    case Ok(t)      => Ok(t)
    case Failure(_) => default
  }
  // def orElse[U >: T, F >: E](op: E => Result[U, F]): Result[U, F] = this match {
  //   case Ok(t)      => Ok(t)
  //   case Failure(e) => op(e)
  // }

  def filterOrElse[F >: E](p: T => Boolean, default: => F): Result[T, F] =
    this match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => this
    }

  def flatMap[U, F >: E](op: T => Result[U, F]): Result[U, F] = this match {
    case Ok(t)      => op(t)
    case Failure(e) => Failure(e)
  }

  def fold[O](fok: T => O, ferr: E => O): O = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ferr(e)
  }

  def transform[U >: T, F >: E](
      fok: T => Result[U, F],
      ferr: E => Result[U, F]
  ): Result[U, F] = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ferr(e)
  }

  def foreach[U](op: T => U): Unit = this match {
    case Ok(t) => op(t)
    case _     => ()
  }

  // def inspect[U](op: T => U): Unit = foreach(op)

  def forall(f: T => Boolean): Boolean = this match {
    case Ok(t) => f(t)
    case _     => true
  }

  def exists(p: T => Boolean): Boolean = this match {
    case Ok(t) => p(t)
    case _     => false
  }

}

object Result {

  def cond[T, E <: Throwable](
      test: Boolean,
      ok: => T,
      failure: => E
  ): Result[T, E] = {
    if (test) Ok(ok) else Failure(failure)
  }

  def apply[T, E, V](value: V)(using
      toResult: ToResult[T, E, V]
  ): Result[T, E] =
    toResult(value)

}

case class Ok[+T, +E](v: T) extends Result[T, E] {
  def isOk: Boolean = true

  def isFailure: Boolean = false

  def get: T = v

  def intoOk: T = v

  def withOk[U >: T]: Result[U, E] = this
}
object Ok {
  val unit: Ok[Nothing, Unit] = Ok(())
}

case class Failure[+T, +E <: Throwable](e: E) extends Result[T, E] {
  def isOk: Boolean = false

  def isFailure: Boolean = true

  def get: T = throw e

  def intoFailure: E = e

  def withFailure[F >: T]: Result[F, E] = this
}
object Failure {
  val unit: Failure[Unit, Nothing] = Failure(())
}
