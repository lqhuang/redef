/**
 * =Use Case=
 *
 * Error handling with the [[Result]] type.
 *
 * `Result` is a type used for returning and propagating errors. It is an
 * disjoint union with the variants, [[Ok]], representing success and containing
 * a value, and [[Err]], representing error and containing an error value.
 *
 * Functions should return `Result` whenever errors are expected and
 * recoverable.
 *
 * For simplicity many examples make use of primitives such as `String` and
 * `Int` for the error type. It is recommended that in practice developers
 * should try to make use of more structured types to allow for improved error
 * handling. As opposed to relying on a stringly-typed interface or integer
 * error codes.
 *
 * A simple function returning `Result` might be defined and used like so:
 *
 * {{{
 * >>> sealed trait MajorVersion
 * >>> object MajorVersion {
 * ...   case object V1 extends MajorVersion
 * ...   case object V2 extends MajorVersion
 * ... }
 *
 * >>> sealed trait ParseError
 * >>> object ParseError {
 * ...  case object InvalidHeaderLength extends ParseError
 * ...  case object UnsupportedVersion extends ParseError
 * ... }
 *
 * >>> def parseMajorVersion(header: List[Int]): Result[ParseError, MajorVersion] =
 * ...   header.headOption match {
 * ...     case None    => Failure(ParseError.InvalidHeaderLength)
 * ...     case Some(1) => Ok(MajorVersion.V1)
 * ...     case Some(2) => Ok(MajorVersion.V2)
 * ...     case _       => Failure(ParseError.UnsupportedVersion)
 * ...   }
 *
 * >>> val version = parseMajorVersion(List(1, 2, 3, 4))
 * >>> version match {
 * ...   case Ok(v)  => "working with version: " + v.toString
 * ...   case Failure(e) => "error parsing header: " + e.toString
 * ... }
 * working with version: V1
 *
 * }}}
 *
 * Pattern matching on `Result`s is clear and straightforward for simple cases,
 * but `Result` comes with some convenience methods that make working with it
 * more succinct.
 *
 * {{{
 * >>> val goodResult: Result[String, Int] = Ok(10);
 * >>> val badResult: Result[String, Int] = Failure("Some Error")
 *
 * // The `isOk` and `isFailure` methods do what they say.
 *
 * >>> goodResult.isOk && !goodResult.isFailure
 * true
 *
 * >>> badResult.isFailure && !badResult.isOk
 * true
 *
 * // `map` replaces the `Ok` value of a `Result` with the result of the provided function
 * >>> goodResult.map(_ + 1)
 * Ok(11)
 *
 * // `map` leaves an `Failure` value of a `Result` as it was, ignoring the provided function
 * >>> badResult.map(_ - 1)
 * Failure(Some Error)
 *
 * // Use `orElse` to handle the error.
 * scala> badResult.orElse {
 *      |   case "Anticipated Error" => Ok(0)
 *      |   case "Some Error"        => Failure(true)
 *      |   case _                   => Failure(false)
 *      | }
 * res1: Result[Boolean, Int] = Failure(true)
 * }}}
 *
 * =Method overview=
 *
 * In addition to working with pattern matching, `Result` provides a wide
 * variety of different methods.
 *
 * ==Querying the variant==
 *
 * The [[Result.isOk isOk]] and [[Result.isFailure isFailure]] methods return
 * `true` if the `Result` is `Ok` or `Failure`, respectively.
 *
 * The [[Result.contains contains]] methods take in a value and return `true` if
 * it matches the inner `Ok` value.
 *
 * ==Transforming contained values==
 *
 * These methods transform `Result` to `Option`:
 *
 *   - [[Result.failure failure]] transforms `Result[T, E]` into `Option[E]`,
 *     mapping `Failure(e)` to `Some(e)` and `Ok(v)` to `None`
 *   - [[Result.ok ok]] transforms `Result[T, E]` into `Option[T]`, mapping
 *     `Ok(v)` to `Some(v)` and `Failure(e)` to `None`
 *   - [[Result.transposeOption transposeOption]] transposes a `Result[E,
 *     Option[T]]` into an `Option[Result[T, E]]`
 *
 * This method transforms the contained value of the `Ok` variant:
 *
 *   - [[Result.map map]] transforms `Result[T, E]` into `Result[U, E]` by
 *     applying the provided function to the contained value of `Ok` and leaving
 *     `Failure` values unchanged
 *
 * These methods transform a `Result[T, E]` into a value of a possibly different
 * type `U`:
 *
 *   - [[Result.mapOr mapOr]] and [[Result.mapErrOr mapErrOr]] applies the
 *     provided function to the contained value of `Ok` or `Failure`
 *     respecitively, or returns the provided default value.
 *
 * ==Boolean operators==
 *
 * These methods treat the `Result` as a boolean value, where `Ok` acts like
 * `true` and `Failure` acts like `false`. There are two categories of these
 * methods: ones that take a `Result` as input, and ones that take a function as
 * input (to be lazily evaluated).
 *
 * The [[Result.and and]] and [[Result.or or]] methods take another `Result` as
 * input, and produce a `Result` as output. The `and` method can produce a
 * `Result[U, E]` value having a different inner type `U` than `Result[T, E]`.
 * The `or` method can produce a `Result[T, F]` value having a different error
 * type `F` than `Result[T, E]`.
 *
 * | method | self         | input        | output       |
 * |:-------|:-------------|:-------------|:-------------|
 * | `and`  | `Failure(e)` | (ignored)    | `Failure(e)` |
 * | `and`  | `Ok(x)`      | `Failure(d)` | `Failure(d)` |
 * | `and`  | `Ok(x)`      | `Ok(y)`      | `Ok(y)`      |
 * | `or`   | `Failure(e)` | `Failure(d)` | `Failure(d)` |
 * | `or`   | `Failure(e)` | `Ok(y)`      | `Ok(y)`      |
 * | `or`   | `Ok(x)`      | (ignored)    | `Ok(x)`      |
 *
 * The [[Result.andThen andThen]] and [[Result.orElse orElse]] methods take a
 * function as input, and only evaluate the function when they need to produce a
 * new value. The `andThen` method can produce a `Result[U, E]` value having a
 * different inner type `U` than `Result[T, E]`. The `orElse` method can produce
 * a `Result[T, F]` value having a different error type `F` than `Result[T, E]`.
 *
 * NOTE: [[Result.flatMap flatMap]] is equivalent to `andThen` and it is
 * provided for consistency with typical Scala conventions.
 *
 * | method    | self         | function input | function result | output       |
 * |:----------|:-------------|:---------------|:----------------|:-------------|
 * | `andThen` | `Failure(e)` | (not provided) | (not evaluated) | `Failure(e)` |
 * | `andThen` | `Ok(x)`      | `x`            | `Failure(d)`    | `Failure(d)` |
 * | `andThen` | `Ok(x)`      | `x`            | `Ok(y)`         | `Ok(y)`      |
 * | `orElse`  | `Failure(e)` | `e`            | `Failure(d)`    | `Failure(d)` |
 * | `orElse`  | `Failure(e)` | `e`            | `Ok(y)`         | `Ok(y)`      |
 * | `orElse`  | `Ok(x)`      | (not provided) | (not evaluated) | `Ok(x)`      |
 *
 * ==Implicits==
 *
 * Extension methods are provided to facilitate conversion of several types to a
 * `Result`. They can imported using `import dev.jsbrucker.result.implicits._`
 *   - All types get some extension methods out of the box. This includes:
 *     - [[extensions.all.Ops.asOk asOk]]
 *     - [[extensions.all.Ops.asFailure asFailure]]
 *     - [[extensions.all.Ops.toResult toResult]] - For types with an implicit
 *       definition of [[ToResult]] in scope.
 *   - `Option` gets a number of additional helpers. See:
 *     [[extensions.option.Ops]]
 */

package redef.util

import scala.annotation.{implicitNotFound, experimental}
import scala.collection.immutable.Seq
import scala.annotation.targetName
import scala.runtime.Statics

case class Ok[+T](value: T) extends AnyVal {
  def intoOk: T = value

  /**
   * Upcasts this `Ok[T, E]` to `Result[T, F]`
   */
  def withFailure[E]: Result[T, E] = this
}
object Ok {
  val unit: Result[Unit, Nothing] = Ok(())
}

case class Failure[+E](err: E) extends AnyVal {
  def intoFailure: E = err

  /**
   * Upcasts this `Failure[T, E]` to `Result[U, E]`
   */
  def withOk[T]: Result[T, E] = this
}
object Failure {
  val unit: Result[Nothing, Unit] = Failure(())
}

type Result[+T, +E] = Ok[T] | Failure[E]

extension [T, E](result: Result[T, E]) {

  /**
   * Returns `true` if the `Result` is a `Ok`, `false` otherwise.
   */
  def isOk: Boolean = result match {
    case Ok(_)      => true
    case Failure(_) => false
  }

  /**
   * Returns `true` if the `Result` is a `Failure`, `false` otherwise.
   */
  def isFailure: Boolean = result match {
    case Ok(_)      => false
    case Failure(_) => true
  }

  /**
   * Returns the value from this `Ok` or the given `default` argument if this is
   * a `Failure`.
   *
   * ''Note:'': This will throw an err if it is not a success and default throws
   * an err.
   */
  def getOrElse[U >: T](default: => U): U = result match {
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
  def fold[O](fok: T => O, ffail: E => O): O = result match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  /**
   * Returns this `Result` if it's a `Ok` or the given `default` argument if
   * this is a `Failure`.
   */
  def orElse[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    result match {
      case Ok(_)      => result
      case Failure(_) => default
    }

  /**
   * Applies the given function `f` if this is a `Ok`, otherwise returns `Unit`
   * if this is a `Failure`.
   */
  def foreach[U](f: T => U): Unit = result match {
    case Ok(t)      => f(t)
    case Failure(_) => ()
  }

  /**
   * Returns the given function applied to the value from this `Ok` or returns
   * this if this is a `Failure`.
   */
  def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] = result match {
    case Ok(t)      => f(t)
    case Failure(e) => Failure(e)
  }

  /**
   * Maps the given function to the value from this `Ok` or returns this if this
   * is a `Failure`.
   */
  def map[U](f: T => U): Result[U, E] = result match {
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
    result match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => result
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
  ): Result[U, F] = result match {
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
  ): Result[U, F] = result match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  /**
   * Inverts this `Result`. If this is a `Failure`, returns its err wrapped in a
   * `Ok`. If this is a `Ok`, returns a `Failure` containing an `T`.
   */
  def swap: Result[E, T] = result match {
    case Ok(t)      => Failure(t)
    case Failure(e) => Ok(e)
  }

  def exists(p: T => Boolean): Boolean = result match {
    case Ok(t) => p(t)
    case _     => false
  }

  def forall(p: T => Boolean): Boolean = result match {
    case Ok(t) => p(t)
    case _     => true
  }

  def contains[U >: T](x: => U): Boolean = result match {
    case Ok(t) => t == x
    case _     => false
  }

  @experimental
  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(result)

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

  // def toTry(using ev: A <:< Throwable): Try[T] = result match {
  //   case Failure(e) => ev(e)
  //   case _          => this
  // }

  // def toSaferTry(using ev: A <:< Throwable): Try[T] = result match {
  //   case Failure(e) => Failure(ev(e))
  //   case _          => this
  // }

  /// new methods

  @experimental
  def or[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    result match {
      case Failure(_) => default
      case _          => result
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
  def recoverWith[U >: T, F](rf: E => Result[U, F]): Result[U, F] =
    result match {
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
  def recover[F](rf: E => F): Result[T, F] = result match {
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
  ): Result[U, F] = result match {
    case Ok(t) => this.asInstanceOf[Result[U, F]]
    case Failure(e) =>
      ev(e) match {
        case Ok(u)      => Ok(u)
        case Failure(f) => Failure(f)
      }
  }

  @experimental
  def ok: Option[T] = result match {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  @experimental
  def failure: Option[E] = result match {
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
