// Ported from Scala standard library

package redef.util

import scala.runtime.Statics
import scala.util.control.NonFatal
import scala.util.Either

/**
 * The `Try` type represents a computation that may fail during evaluation by
 * raising an exception. It holds either a successfully computed value or the
 * exception that was thrown. This is similar to the [[redef.util.Result]] type,
 * but with different semantics.
 *
 * Instances of `Try[T]` are an instance of either [[redef.util.Ok]][T] or
 * [[redef.util.Failure]][T].
 *
 * For example, consider a computation that performs division on user-defined
 * input. `Try` can reduce or eliminate the need for explicit exception handling
 * in all of the places where an exception might be thrown.
 *
 * Example:
 * {{{
 *   import scala.io.StdIn
 *   import redef.util.{Try, Ok, Failure}
 *
 *   def divide: Try[Int] = {
 *     val dividend = Try(StdIn.readLine("Enter an Int that you'd like to divide:\n").toInt)
 *     val divisor = Try(StdIn.readLine("Enter an Int that you'd like to divide by:\n").toInt)
 *     val problem = dividend.flatMap(x => divisor.map(y => x/y))
 *     problem match {
 *       case Ok(v) =>
 *         println("Result of " + dividend.get + "/"+ divisor.get +" is: " + v)
 *         Ok(v)
 *       case Failure(e) =>
 *         println("You must've divided by zero or entered something that's not an Int. Try again!")
 *         println("Info from the exception: " + e.getMessage)
 *         divide
 *     }
 *   }
 *
 * }}}
 *
 * An important property of `Try` shown in the above example is its ability to
 * ''pipeline'', or chain, operations, catching exceptions along the way. The
 * `flatMap` and `map` combinators in the above example each essentially pass
 * off either their successfully completed value, wrapped in the `Ok` type for
 * it to be further operated upon by the next combinator in the chain, or the
 * exception wrapped in the `Failure` type usually to be simply passed on down
 * the chain. Combinators such as `recover` and `recoverWith` are designed to
 * provide some type of default behavior in the case of failure.
 *
 * ''Note'': only non-fatal exceptions are caught by the combinators on `Try`
 * (see [[scala.util.control.NonFatal]]). Serious system errors, on the other
 * hand, will be thrown.
 *
 * ''Note:'': all Try combinators will catch exceptions and return failure
 * unless otherwise specified in the documentation.
 */
sealed abstract class Try[+T] extends Result[T, Throwable] {

  def get: T = this match {
    case Ok(value)          => value.asInstanceOf[T]
    case Failure(exception) => throw (exception.asInstanceOf[Throwable])
  }

  /**
   * Creates a non-strict filter, which eventually converts this to a `Failure`
   * if the predicate is not satisfied.
   *
   * Note: unlike filter, withFilter does not create a new Try. Instead, it
   * restricts the domain of subsequent `map`, `flatMap`, `foreach`, and
   * `withFilter` operations.
   *
   * As Try is a one-element collection, this may be a bit overkill, but it's
   * consistent with withFilter on Option and the other collections.
   *
   * @param p
   *   the predicate used to test elements.
   * @return
   *   an object of class `WithFilter`, which supports `map`, `flatMap`,
   *   `foreach`, and `withFilter` operations. All these operations apply to
   *   those elements of this Try which satisfy the predicate `p`.
   */
  inline final def withFilter(p: T => Boolean): WithFilter = WithFilter(p)

  /**
   * We need a whole WithFilter class to honor the "doesn't create a new
   * collection" contract even though it seems unlikely to matter much in a
   * collection with max size 1.
   */
  private final class WithFilter(p: T => Boolean) {
    def map[U](f: T => U): Result[U, E] = Result.this filter p map f

    def flatMap[U](f: T => Result[U, E]): Result[U, E] =
      Result.this filter p flatMap f

    def foreach[U](f: T => U): Unit = Result.this filter p foreach f

    def withFilter(q: T => Boolean): WithFilter = WithFilter(x => p(x) && q(x))
  }

}

object Try {

  /**
   * Constructs a `Try` using the by-name parameter as a result value.
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
  def apply[T](r: => T): Try[T] =
    try {
      val r1 = r
      Ok(r1)
    } catch {
      case NonFatal(e) => Failure(e)
    }
}
