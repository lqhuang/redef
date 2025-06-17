package redef.result

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
   * >>> Result(Left("Error")) == Err("Error")
   * true
   * }}}
   */
  implicit def eitherToResult[T, E]: ToResult[T, E, Either[E, T]] = {
    case Right(ok) => Ok(ok)
    case Left(e)   => Err(e)
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
  implicit def scuTryToResult[T]: ToResult[Throwable, T, scala.util.Try[T]] = {
    case scala.util.Success(ok) => Ok(ok)
    case scala.util.Failure(e)  => Err(e)
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
    case false => Err.unit
  }
}
