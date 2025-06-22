package redef.util

import scala.annotation.{implicitNotFound, experimental}
import scala.collection.immutable.Seq
import scala.annotation.targetName
import scala.runtime.Statics

sealed abstract trait Result[+T, +E] extends Product with Serializable {

  /**
   * Returns `true` if the `Result` is a `Ok`, `false` otherwise.
   */
  def isOk: Boolean

  /**
   * Returns `true` if the `Result` is a `Failure`, `false` otherwise.
   */
  def isFailure: Boolean

  /**
   * Returns the value from this `Ok` or the given `default` argument if this is
   * a `Failure`.
   *
   * ''Note:'': This will throw an err if it is not a success and default throws
   * an err.
   */
  def getOrElse[U >: T](default: => U): U = this match {
    case Ok(t)      => t
    case Failure(e) => default // if default throws, then this will throw
  }

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
  def fold[O](fok: T => O, ffail: E => O): O = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  /**
   * Returns this `Result` if it's a `Ok` or the given `default` argument if
   * this is a `Failure`.
   */
  def orElse[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    this match {
      case Ok(_)      => this
      case Failure(_) => default
    }

  /**
   * Applies the given function `f` if this is a `Ok`, otherwise returns `Unit`
   * if this is a `Failure`.
   */
  def foreach[U](f: T => U): Unit = this match {
    case Ok(t)      => f(t)
    case Failure(_) => ()
  }

  /**
   * Returns the given function applied to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] = this match {
    case Ok(t)      => f(t)
    case Failure(e) => Failure(e)
  }

  /**
   * Maps the given function to the value from this `Ok` or returns this if this
   * is a `Failure`.
   */
  def map[U](f: T => U): Result[U, E] = this match {
    case Ok(t)      => Ok(f(t))
    case Failure(e) => Failure(e)
  }

  /**
   * Returns `Ok` with the existing value of `Ok` if this is a `Ok` and the
   * given predicate `p` holds for the right value, or `Failture(zero)` if this
   * is a `Ok` and the given predicate `p` does not hold for the right value, or
   * `Failture` with the existing value of `Failture` if this is a `Failture`.
   */
  def filterOrElse[F >: E](p: T => Boolean, default: => F): Result[T, F] =
    this match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => this
    }

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
      ev: T <:< Result[U, F]
  ): Result[U, F] = flatMap(ev)

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
      ev: T <:< Option[U]
  ): Result[U, F] = this match {
    case Ok(ok) =>
      ev(ok) match
        case Some(u) => Ok(u)
        case None    => Failure(defaultFailure)
    case Failure(e) => Failure(e)
  }

  /**
   * Completes this `Result` by applying the function `f` to this if this is of
   * type `Failure`, or conversely, by applying `s` if this is a `Ok`.
   */
  def transform[U >: T, F >: E](
      fok: T => Result[U, F],
      ffail: E => Result[U, F]
  ): Result[U, F] = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  /**
   * Inverts this `Result`. If this is a `Failure`, returns its err wrapped in a
   * `Ok`. If this is a `Ok`, returns a `Failure` containing an `T`.
   */
  def swap: Result[E, T] = this match {
    case Ok(t)      => Failure(t)
    case Failure(e) => Ok(e)
  }

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

  @experimental
  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(this)

  /**
   * Returns `None` if this is a `Failure` or a `Some` containing the value if
   * this is a `Ok`.
   */
  def toOption: Option[T] =
    to[Option[T]](using FromResult.optionFromResult[T, E])

  /**
   * Returns `Left` if this is a `Failure`, otherwise returns `Right` with `Ok`
   * value.
   */
  def toEither: Either[E, T] =
    to[Either[E, T]](using FromResult.eitherFromResult[T, E])

  /**
   * Returns a `Seq` containing the `Ok` value if it exists or an empty `Seq` if
   * this is a `Failture`.
   */
  def toSeq: Seq[T] = to[Seq[T]](using FromResult.seqFromResult[T, E])

  // def toTry(using ev: A <:< Throwable): Try[T] = this match {
  //   case Failure(e) => ev(e)
  //   case _          => this
  // }

  // def toSaferTry(using ev: A <:< Throwable): Try[T] = this match {
  //   case Failure(e) => Failure(ev(e))
  //   case _          => this
  // }

  /// new methods

  @experimental
  def or[U >: T, F >: E](default: => Result[U, F]): Result[U, F] = this match {
    case Failure(_) => default
    case _          => this
  }

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like `flatMap` for the exception.
   *
   * ===Examples===
   *
   * {{{
   * >>> def sq(x: Int): Result[Int, Int] = { Ok(x * x) }
   * >>> def fail(x: Int): Result[Int, Int] = { Failure(x) }
   *
   * >>> Ok(2).recoverWith(sq).recoverWith(sq)
   * Ok(2)
   *
   * >>> Ok(2).recoverWith(fail).recoverWith(sq)
   * Ok(2)
   *
   * >>> Failure(3).recoverWith(sq).recoverWith(fail)
   * Ok(9)
   *
   * >>> Failure(3).recoverWith(fail).recoverWith(fail)
   * Failure(3)
   * }}}
   */
  def recoverWith[U >: T, F](rf: E => Result[U, F]): Result[U, F] = this match {
    case Ok(t)      => Ok(t)
    case Failure(e) => rf(e)
  }

  /**
   * Applies the given function `f` if this is a `Failure`, otherwise returns
   * this if this is a `Ok`. This is like map for the exception.
   *
   * Maps a `Result[E, T]` to `Result[F, T]` by applying a function to a
   * contained `Err` value, leaving an `Ok` value untouched.
   *
   * This function can be used to pass through a successful result while
   * handling an error.
   *
   * ==Examples==
   *
   * {{{
   * >>> def square(i: Int) = i * i
   *
   * >>> Err(1).mapErr(square(_))
   * Err(1)
   *
   * >>> Err(2).mapErr(square(_))
   * Err(4)
   *
   * >>> Ok[Int, String]("Some Value").mapErr(square(_))
   * Ok(Some Value)
   * }}}
   */
  def recover[F](rf: E => F): Result[T, F] = this match {
    case Ok(t)      => Ok(t)
    case Failure(e) => Failure(rf(e))
  }

  /**
   * `joinOk` is analogous to `joinRight` for consistency with `Either` API
   *
   * @group Transform
   */
  def joinOk[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      ev: T <:< Result[U, F]
  ): Result[U, F] = flatten(using ev)

  /**
   * `joinFailure` is analogous to `joinLeft` for consistency with `Either` API
   *
   * @group Transform
   */
  def joinFailure[U >: T, F](using
      @implicitNotFound("${E} is not a Result[${U}, ${F}]")
      ev: E <:< Result[U, F]
  ): Result[U, F] = this match {
    case Ok(t) => this.asInstanceOf[Result[U, F]]
    case Failure(e) =>
      ev(e) match {
        case Ok(u)      => Ok(u)
        case Failure(f) => Failure(f)
      }
  }

  @experimental
  def ok: Option[T] = this match {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  @experimental
  def failure: Option[E] = this match {
    case Ok(_)      => None
    case Failure(e) => Some(e)
  }

}

object Result {

  def cond[T, E](
      test: Boolean,
      ok: => T,
      failure: => E
  ): Result[T, E] = if (test) Ok(ok) else Failure(failure)

  def from[T, E, V](v: V)(using toResult: ToResult[T, E, V]): Result[T, E] =
    toResult(v)

  import ToResult.*

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
  implicit class MergeOps[A](private val r: Result[A, A]) extends AnyVal {
    def merge: A = r match {
      case Ok(v)      => v
      case Failure(v) => v
    }
  }

  def ok[T, E](value: T): Result[T, E] = Ok(value)

  def failure[T, E](err: E): Result[T, E] = Failure(err)
}

case class Ok[+T, +E](value: T) extends Result[T, E] {
  def isOk: Boolean = true

  def isFailure: Boolean = false

  def intoOk: T = value

  /**
   * Upcasts this `Ok[T, E]` to `Result[T, F]`
   */
  def withFailure[F >: E]: Result[T, F] = this
}
object Ok {
  val unit: Result[Unit, Nothing] = Ok(())
}

case class Failure[+T, +E](err: E) extends Result[T, E] {
  def isOk: Boolean = false

  def isFailure: Boolean = true

  def intoFailure: E = err

  /**
   * Upcasts this `Failure[T, E]` to `Result[U, E]`
   */
  def withOk[U >: T]: Result[U, E] = this
}
object Failure {
  val unit: Result[Nothing, Unit] = Failure(())
}

/**
 * Used to convert a `Result[T, E]` to a value of type `V`
 *
 * This interface is leveraged by the [[Result.to]] method.
 */
trait FromResult[-T, -E, +V] {
  def apply(result: Result[T, E]): V
}

object FromResult {

  /**
   * Converts `Result[T, E]` into `Option[T]`
   *
   * ===Examples===
   *
   * {{{
   * >>> val ok = Ok(1)
   * >>> ok.to[Option[Int]] == Some(1)
   * true
   *
   * >>> val err = Err("Error")
   * >>> err.to[Option[Int]] == None
   * true
   * }}}
   */
  implicit def optionFromResult[T, E]: FromResult[T, E, Option[T]] = {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  /**
   * Converts `Result[T, E]` into `Either[E, T]`
   *
   * ===Examples===
   *
   * {{{
   * >>> val ok = Ok(1)
   * >>> ok.to[Either[String, Int]] == Right(1)
   * true
   *
   * >>> val err = Err("Error")
   * >>> err.to[Either[String, Int]] == Left("Error")
   * true
   * }}}
   */
  implicit def eitherFromResult[T, E]: FromResult[T, E, Either[E, T]] = {
    case Ok(t)      => Right(t)
    case Failure(e) => Left(e)
  }

  implicit def seqFromResult[T, E]: FromResult[T, E, Seq[T]] = {
    case Ok(t)      => Seq(t)
    case Failure(_) => Seq.empty
  }

  /**
   * Converts `Result[Throwable, T]` into `Try[T]`
   *
   * ===Examples===
   *
   * {{{
   * >>> val ok = Ok(1)
   * >>> ok.to[scala.util.Try[Int]] == scala.util.Success(1)
   * true
   *
   * >>> val ex: Exception = new Exception("Error")
   * >>> val err = Err(ex)
   * >>> err.to[scala.util.Try[Int]] == scala.util.Failure(ex)
   * true
   * }}}
   */

  // implicit def tryFromResult[T]: FromResult[T, Exception, Try[T]] = {
  //   case Ok(t)      => Success(t)
  //   case Failure(e) => Failure(e)
  // }

  implicit def scuTryFromResult[T]
      : FromResult[T, Throwable, scala.util.Try[T]] = {
    case Ok(t)      => scala.util.Success(t)
    case Failure(e) => scala.util.Failure(e)
  }

}

/**
 * Used to convert a value of type `V` to a `Result[T, E]`
 *
 * This interface is leveraged by the [[Result.apply]] method and
 * [[extensions.all.Ops.toResult]].
 */
trait ToResult[+T, +E, -V] {
  def apply(value: V): Result[T, E]
}
object ToResult {

  /**
   * Converts `Either[E, T]` into `Result[T, E]`
   *
   * ===Examples===
   *
   * {{{
   * >>> Result(Right(1)) == Ok(1)
   * true
   *
   * >>> Result(Left("Error")) == Failue("Error")
   * true
   * }}}
   */
  implicit def eitherToResult[T, E]: ToResult[T, E, Either[E, T]] = {
    case Right(ok) => Ok(ok)
    case Left(e)   => Failure(e)
  }

  /**
   * Converts `scala.util.Try[T]` into `Result[T, Throwable]`
   *
   * ===Examples===
   *
   * {{{
   * >>> Result(scala.util.Success(1)) == Ok(1)
   * true
   *
   * >>> val ex: Exception = new Exception("Error")
   * >>> Result(scala.util.Failure(ex)) == Err(ex)
   * true
   * }}}
   */
  implicit def scuTryToResult[T]: ToResult[T, Throwable, scala.util.Try[T]] = {
    case scala.util.Success(v) => Ok(v)
    case scala.util.Failure(e) => Failure(e)
  }

  /**
   * Converts `Boolean` into `Result[Unit, Unit]`
   *
   *   - `true` is `Ok`
   *   - `false is `Err`
   *
   * ===Examples===
   *
   * {{{
   * >>> Result(true) == Ok.unit
   * true
   *
   * >>> Result(false) == Err.unit
   * true
   * }}}
   */
  implicit val booleanToResult: ToResult[Unit, Unit, Boolean] = {
    case true  => Ok.unit
    case false => Failure.unit
  }
}
