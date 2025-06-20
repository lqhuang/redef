package redef.util

import scala.annotation.{implicitNotFound, experimental}
import scala.collection.immutable.Seq
import scala.annotation.targetName

sealed abstract class Result[+T, +E] extends Product with Serializable {

  /**
   * Returns `true` if the `Result` is a `Ok`, `false` otherwise.
   */
  def isOk: Boolean

  /**
   * Returns `true` if the `Result` is a `Failure`, `false` otherwise.
   */
  def isFailure: Boolean

  /**
   * Returns the value from this `Ok` or throws the err if this is a `Failure`.
   */
  def get: T

  /**
   * Returns the value from this `Ok` or the given `default` argument if this is
   * a `Failure`.
   *
   * ''Note:'': This will throw an err if it is not a success and default throws
   * an err.
   */
  def getOrElse[U >: T](default: => U): U

  /**
   * Applies `fa` if this is a `Failure` or `fb` if this is a `Ok`. If `fb` is
   * initially applied and throws an err, then `fa` is applied with this err.
   *
   * @param fok
   *   the function to apply if this is a `Ok`
   * @param ffail
   *   the function to apply if this is a `Failure`
   * @return
   *   the results of applying the function
   */
  def fold[O](fok: T => O, ffail: E => O): O

  /**
   * Returns this `Result` if it's a `Ok` or the given `default` argument if
   * this is a `Failure`.
   */
  def orElse[U >: T, F >: E](default: => Result[U, F]): Result[U, F]
  // def orElse[U >: T, F >: E](op: E => Result[U, F]): Result[U, F]

  /**
   * Applies the given function `f` if this is a `Ok`, otherwise returns `Unit`
   * if this is a `Failure`.
   *
   * ''Note:'' If `f` throws, then this method may throw an err.
   */
  def foreach[U](f: T => U): Unit

  /**
   * Returns the given function applied to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F]

  /**
   * Maps the given function to the value from this `Ok` or returns this if this
   * is a `Failure`.
   */
  def map[U](f: T => U): Result[U, E]

  /**
   * Applies the given partial function to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def collect[U >: T](pf: PartialFunction[T, U]): Result[U, E]

  /**
   * Converts this to a `Failure` if the predicate is not satisfied.
   */
  def filter(p: T => Boolean): Result[T, E]

  /**
   * Returns `Ok` with the existing value of `Ok` if this is a `Ok` and the
   * given predicate `p` holds for the right value, or `Failture(zero)` if this
   * is a `Ok` and the given predicate `p` does not hold for the right value, or
   * `Failture` with the existing value of `Failture` if this is a `Failture`.
   */
  @targetName("filterOrElse")
  def ensure[F >: E](p: T => Boolean, default: => F): Result[T, F] =
    this match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => this
    }

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like `flatMap` for the err.
   */
  def recoverWith[U >: T](pf: PartialFunction[E, Result[U, E]]): Result[U, E]

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like map for the err.
   */
  def recover[U >: T](pf: PartialFunction[E, U]): Result[U, E]

  /**
   * Transforms a nested `Result`, ie, a `Result` of type `Result[Result[T,
   * E]]`, into an un-nested `Result`, ie, a `Result` of type `Result[T, E]`.
   */

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
   * Completes this `Result` by applying the function `f` to this if this is of
   * type `Failure`, or conversely, by applying `s` if this is a `Ok`.
   */
  def transform[U >: T, F >: E](
      ok: T => Result[U, F],
      fail: E => Result[U, F]
  ): Result[U, F]

  /**
   * Inverts this `Result`. If this is a `Failure`, returns its err wrapped in a
   * `Ok`. If this is a `Ok`, returns a `Failure` containing an `T`.
   */
  def swap: Result[E, T]

  def exists(p: T => Boolean): Boolean = this match {
    case Ok(t) => p(t)
    case _     => false
  }

  def forall(p: T => Boolean): Boolean = this match {
    case Ok(t) => p(t)
    case _     => true
  }

  def contains[U >: T](x: => U): Boolean = this match {
    case Ok(t) => t == x
    case _     => false
  }

  /**
   * Returns `None` if this is a `Failure` or a `Some` containing the value if
   * this is a `Ok`.
   */
  def toOption: Option[T]

  /**
   * Returns `Left` if this is a `Failure`, otherwise returns `Right` with `Ok`
   * value.
   */
  def toEither: Either[E, T]

  /**
   * Returns a `Seq` containing the `Ok` value if it exists or an empty `Seq` if
   * this is a `Failture`.
   */
  def toSeq: Seq[T]

  def toTry(using ev: A <:< Throwable): Try[T] = this match {
    case Failure(e) => ev(e)
    case _          => this
  }

  def toTry(using ev: A <:< Throwable): Try[T] = this match {
    case Failure(e) => Failure(ev(e))
    case _          => this
  }

  /// new methods

  @experimental
  def or[U >: T, F >: E](default: => Result[U, F]): Result[U, F] = this match {
    case Failure(_) => default
    case _          => this
  }

  /**
   * An alias of [[flatten]] for consistency with `Either` API, analogous to
   * `joinRight`
   *
   * @group Transform
   */
  @experimental
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
  @experimental
  def joinFailure[U >: T, F](using
      @implicitNotFound("${E} is not a Result[${U}, ${F}]")
      ev: E <:< Result[U, F]
  ): Result[U, F] = flatten(using ev)

  @experimental
  def ok: Option[T]

  @experimental
  def failure: Option[E]

  // def toFailureSeq: Seq[E]

  @experimental
  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(this)

  @experimental
  def from[V](using toResult: ToResult[T, E, V]): V = toResult(this)

}

object Result {

  def cond[T, E](
      test: Boolean,
      ok: => T,
      failure: => E
  ): Result[T, E] = if (test) Ok(ok) else Failure(failure)

  def apply[T, E](value: V)(using toResult: ToResult[T, E, V]): Result[T, E] =
    toResult(value)

  /**
   * Allows use of a `merge` method to extract values from Either instances
   * regardless of whether they are Left or Right.
   *
   * {{{
   *  val l = Left(List(1)): Either[List[Int], Vector[Int]]
   *  val r = Right(Vector(1)): Either[List[Int], Vector[Int]]
   *  l.merge: Seq[Int] // List(1)
   *  r.merge: Seq[Int] // Vector(1)
   * }}}
   */
  implicit class MergeOps[A](private val x: Result[A, A]) extends AnyVal {
    def merge: A = x match {
      case Ok(v)      => v
      case Failure(v) => v
    }
  }

  // def apply[T, E, V](value: V)(using
  //     toResult: ToResult[T, E, V]
  // ): Result[T, E] =
  //   toResult(value)

}

case class Ok[+T, +E](value: T) extends Result[T, E] with AnyVal {
  inline def isOk: Boolean = true

  inline def isFailure: Boolean = false

  inline def get: T = value

  inline def getOrElse[U >: T](default: => U): U = get

  inline def orElse[U >: T](default: => Result[U, F]): Result[U, F] = this

  inline def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] = f(value)

  // inline def flatten[U](using ev: T <:< Result[U, E]): Result[U, E] = value

  inline def foreach[U](f: T => U): Unit = f(value)

  inline def transform[U, F](
      ok: T => Result[U, F],
      fail: E => Result[U, F]
  ): Result[U, F] = this flatMap ok

  inline def map[U](f: T => U): Result[U, E] = Ok(f(value))

  inline def collect[U](pf: PartialFunction[T, U]): Result[U, E] =
    val marker = Statics.pfMarker
    try {
      val v =
        pf.applyOrElse(value, ((x: T) => marker).asInstanceOf[Function[T, U]])
      if marker.ne(v.asInstanceOf[AnyRef])
      then Ok(v)
      else
        Failure(NoSuchElementException("Predicate does not hold for " + value))
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def filter(p: T => Boolean): Result[T, E] =
    try {
      if p(value)
      then this
      else
        Failure(NoSuchElementException("Predicate does not hold for " + value))
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def recover[U >: T](pf: PartialFunction[E, U]): Result[U, E] =
    this

  inline def recoverWith[U >: T](
      pf: PartialFunction[E, Result[U, E]]
  ): Result[U, E] = this

  inline def swap: Result[E, T] = Failure(value)

  inline def toOption: Option[T] = Some(value)

  inline def toEither: Either[E, T] = Right(value)

  inline def fold[O](fok: T => O, ffail: E => O): O = fok(value)
}
object Ok {
  val unit: Ok[Nothing, Unit] = Ok(())
}

case class Failure[+T, +E](err: E) extends Result[T, E] with AnyVal {
  def isOk: Boolean = false

  def isFailure: Boolean = true

  inline def getOrElse[U >: T](default: => U): U = default

  inline def orElse[U >: T](default: => Result[U, F]): Result[U, F] = default

  inline def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] =
    this.asInstanceOf[Result[U, E]]

  inline def flatten[U](implicit ev: T <:< Result[U, E]): Result[U, E] =
    this.asInstanceOf[Result[U, E]]

  inline def foreach[U](f: T => U): Unit = ()

  inline def transform[U, F](
      ok: T => Result[U, F],
      fail: E => Result[U, F]
  ): Result[U, F] =
    fail(err)

  inline def map[U](f: T => U): Result[U, E] = this.asInstanceOf[Result[U, E]]

  inline def collect[U](pf: PartialFunction[T, U]): Result[U, E] =
    this.asInstanceOf[Result[U, E]]

  inline def filter(p: T => Boolean): Result[T, E] = this

  inline def recover[U >: T](pf: PartialFunction[E, U]): Result[U, E] =
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(err, (x: E) => marker)
      if (marker ne v.asInstanceOf[AnyRef])
      then Ok(v.asInstanceOf[U])
      else this
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def recoverWith[U >: T](
      pf: PartialFunction[E, Result[U, E]]
  ): Result[U, E] =
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(err, (x: E) => marker)
      if (marker ne v.asInstanceOf[AnyRef])
      then v.asInstanceOf[Result[U, E]]
      else this
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def swap: Result[E, T] = Ok(err)

  inline def toOption: Option[T] = None

  inline def toEither: Either[E, T] = Left(err)

  inline def fold[O](fok: T => O, ffail: E => O): O = ffail(err)

}
object Failure {
  val unit: Failure[Unit, Nothing] = Failure(())
}
