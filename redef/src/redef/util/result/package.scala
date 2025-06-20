package redef.util

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
package object result {}
