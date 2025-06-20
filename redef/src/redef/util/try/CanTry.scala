// Ported from Scala standard library

package redef.util

import scala.runtime.Statics
import scala.util.control.NonFatal

sealed abstract class CanTry[+T, +E <: Exception](using CanThrow[E])
    extends Result[T, E]
    with Product
    with Serializable {

  /**
   * Returns `true` if the `CanTry` is a `Failure`, `false` otherwise.
   */
  def isFailure: Boolean

  /**
   * Returns `true` if the `CanTry` is a `Ok`, `false` otherwise.
   */
  def isOk: Boolean

  /**
   * Returns the value from this `Ok` or the given `default` argument if this is
   * a `Failure`.
   *
   * ''Note:'': This will throw an exception if it is not a success and default
   * throws an exception.
   */
  def getOrElse[U >: T](default: => U): U

  /**
   * Returns this `CanTry` if it's a `Ok` or the given `default` argument if
   * this is a `Failure`.
   */
  def orElse[U >: T](default: => CanTry[U]): CanTry[U]

  /**
   * Returns the value from this `Ok` or throws the exception if this is a
   * `Failure`.
   */
  def get: T

  /**
   * Applies the given function `f` if this is a `Ok`, otherwise returns `Unit`
   * if this is a `Failure`.
   *
   * ''Note:'' If `f` throws, then this method may throw an exception.
   */
  def foreach[U](f: T => U): Unit

  /**
   * Returns the given function applied to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def flatMap[U](f: T => CanTry[U]): CanTry[U]

  /**
   * Maps the given function to the value from this `Ok` or returns this if this
   * is a `Failure`.
   */
  def map[U](f: T => U): CanTry[U]

  /**
   * Applies the given partial function to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def collect[U](pf: PartialFunction[T, U]): CanTry[U]

  /**
   * Converts this to a `Failure` if the predicate is not satisfied.
   */
  def filter(p: T => Boolean): CanTry[T]

  /**
   * Creates a non-strict filter, which eventually converts this to a `Failure`
   * if the predicate is not satisfied.
   *
   * Note: unlike filter, withFilter does not create a new CanTry. Instead, it
   * restricts the domain of subsequent `map`, `flatMap`, `foreach`, and
   * `withFilter` operations.
   *
   * As CanTry is a one-element collection, this may be a bit overkill, but it's
   * consistent with withFilter on Option and the other collections.
   *
   * @param p
   *   the predicate used to test elements.
   * @return
   *   an object of class `WithFilter`, which supports `map`, `flatMap`,
   *   `foreach`, and `withFilter` operations. All these operations apply to
   *   those elements of this CanTry which satisfy the predicate `p`.
   */
  inline final def withFilter(p: T => Boolean): WithFilter = WithFilter(p)

  private final class WithFilter(p: T => Boolean) {
    def map[U](f: T => U): CanTry[U] = CanTry.this filter p map f
    def flatMap[U](f: T => CanTry[U]): CanTry[U] =
      CanTry.this filter p flatMap f
    def foreach[U](f: T => U): Unit = CanTry.this filter p foreach f
    def withFilter(q: T => Boolean): WithFilter = WithFilter(x => p(x) && q(x))
  }

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like `flatMap` for the exception.
   */
  def recoverWith[U >: T](rf: Function[E, CanTry[U]]): CanTry[U]

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like map for the exception.
   */
  def recover[U >: T](rf: Function[E, U]): CanTry[U]

  /**
   * Returns `None` if this is a `Failure` or a `Some` containing the value if
   * this is a `Ok`.
   */
  def toOption: Option[T]

  /**
   * Transforms a nested `CanTry`, ie, a `CanTry` of type `CanTry[CanTry[T]]`,
   * into an un-nested `CanTry`, ie, a `CanTry` of type `CanTry[T]`.
   */
  def flatten[U](implicit ev: T <:< CanTry[U]): CanTry[U]

  /**
   * Inverts this `CanTry`. If this is a `Failure`, returns its exception
   * wrapped in a `Ok`. If this is a `Ok`, returns a `Failure` containing an
   * `UnsupportedOperationException`.
   */
  def failed: CanTry[Throwable]

  /**
   * Completes this `CanTry` by applying the function `f` to this if this is of
   * type `Failure`, or conversely, by applying `s` if this is a `Ok`.
   */
  def transform[U](s: T => CanTry[U], f: Throwable => CanTry[U]): CanTry[U]

  /**
   * Returns `Left` with `Throwable` if this is a `Failure`, otherwise returns
   * `Right` with `Ok` value.
   */
  def toEither: Either[Throwable, T]

  /**
   * Applies `fa` if this is a `Failure` or `fb` if this is a `Ok`. If `fb` is
   * initially applied and throws an exception, then `fa` is applied with this
   * exception.
   *
   * @example
   *   {{{ val result: CanTry[Int] = CanTry { string.toInt } log(result.fold( ex
   *   \=> "Operation failed with " + ex, v => "Operation produced value: " + v
   *   )) }}}
   *
   * @param fa
   *   the function to apply if this is a `Failure`
   * @param fb
   *   the function to apply if this is a `Ok`
   * @return
   *   the results of applying the function
   */
  def fold[U](fa: Throwable => U, fb: T => U): U

}

object CanTry {

  /**
   * Constructs a `CanTry` using the by-name parameter as a result value.
   *
   * The evaluation of `r` is attempted once.
   *
   * Any non-fatal exception is caught and results in a `Failure` that holds the
   * exception.
   *
   * @param r
   *   the result value to compute
   * @return
   *   the result of evaluating the value, as a `Ok` or `Failure`
   */
  def apply[T](r: => T): CanTry[T] =
    try {
      val r1 = r
      Ok(r1)
    } catch {
      case NonFatal(e) => Failure(e)
    }
}

final case class Ok[+T](value: T) extends CanTry[T] {
  inline def isFailure: Boolean = false

  inline def isOk: Boolean = true

  inline def get = value

  inline def getOrElse[U >: T](default: => U): U = get

  inline def orElse[U >: T](default: => CanTry[U]): CanTry[U] = this

  inline def flatMap[U](f: T => CanTry[U]): CanTry[U] =
    try f(value)
    catch { case NonFatal(e) => Failure(e) }

  inline def flatten[U](using ev: T <:< CanTry[U]): CanTry[U] = value

  inline def foreach[U](f: T => U): Unit = f(value)

  inline def transform[U](
      s: T => CanTry[U],
      f: Throwable => CanTry[U]
  ): CanTry[U] =
    this flatMap s

  inline def map[U](f: T => U): CanTry[U] = CanTry[U](f(value))

  inline def collect[U](pf: PartialFunction[T, U]): CanTry[U] =
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

  inline def filter(p: T => Boolean): CanTry[T] =
    try {
      if p(value)
      then this
      else
        Failure(NoSuchElementException("Predicate does not hold for " + value))
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def recover[U >: T](pf: PartialFunction[Throwable, U]): CanTry[U] =
    this

  inline def recoverWith[U >: T](
      pf: PartialFunction[Throwable, CanTry[U]]
  ): CanTry[U] = this

  inline def failed: CanTry[Throwable] =
    Failure(UnsupportedOperationException("Ok.failed"))

  inline def toOption: Option[T] = Some(value)

  inline def toEither: Either[Throwable, T] = Right(value)

  inline def fold[U](fa: Throwable => U, fb: T => U): U =
    try { fb(value) }
    catch { case NonFatal(e) => fa(e) }
}

final case class Failure[+T](exception: Throwable) extends CanTry[T] {
  inline def isFailure: Boolean = true

  inline def isOk: Boolean = false

  inline def get: T = throw exception

  inline def getOrElse[U >: T](default: => U): U = default

  inline def orElse[U >: T](default: => CanTry[U]): CanTry[U] =
    try default
    catch { case NonFatal(e) => Failure(e) }

  inline def flatMap[U](f: T => CanTry[U]): CanTry[U] =
    this.asInstanceOf[CanTry[U]]

  inline def flatten[U](implicit ev: T <:< CanTry[U]): CanTry[U] =
    this.asInstanceOf[CanTry[U]]

  inline def foreach[U](f: T => U): Unit = ()

  inline def transform[U](
      s: T => CanTry[U],
      f: Throwable => CanTry[U]
  ): CanTry[U] =
    try f(exception)
    catch { case NonFatal(e) => Failure(e) }

  inline def map[U](f: T => U): CanTry[U] = this.asInstanceOf[CanTry[U]]

  inline def collect[U](pf: PartialFunction[T, U]): CanTry[U] =
    this.asInstanceOf[CanTry[U]]

  inline def filter(p: T => Boolean): CanTry[T] = this

  inline def recover[U >: T](pf: PartialFunction[Throwable, U]): CanTry[U] =
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(exception, (x: Throwable) => marker)
      if (marker ne v.asInstanceOf[AnyRef])
      then Ok(v.asInstanceOf[U])
      else this
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def recoverWith[U >: T](
      pf: PartialFunction[Throwable, CanTry[U]]
  ): CanTry[U] =
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(exception, (x: Throwable) => marker)
      if (marker ne v.asInstanceOf[AnyRef])
      then v.asInstanceOf[CanTry[U]]
      else this
    } catch {
      case NonFatal(e) => Failure(e)
    }

  inline def failed: CanTry[Throwable] = Ok(exception)

  inline def toOption: Option[T] = None

  inline def toEither: Either[Throwable, T] = Left(exception)

  inline def fold[U](fa: Throwable => U, fb: T => U): U = fa(exception)
}
