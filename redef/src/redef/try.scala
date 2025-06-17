package redef

import language.experimental.{
  captureChecking,
  saferExceptions,
  erasedDefinitions
}

import scala.runtime.Statics
import scala.util.control.NonFatal

sealed abstract class Try[+T] extends Result[T, E] with CanThrow[E] {

  def orElse[U >: T](default: => Try[U]): Try[U]

  def failed: Try[Exception]

  def foreach[U](f: T => U): Unit

  def flatMap[U](f: T => Try[U]): Try[U]

  def map[U](f: T => U): Try[U]

  def collect[U](pf: PartialFunction[T, U]): Try[U]

  def transform[U](s: T => Try[U], f: E => Try[U]): Try[U]

  def fold[U](fa: E => U, fb: T => U): U

  def filter(p: T => Boolean): Try[T]

  @inline final def withFilter(p: T => Boolean): WithFilter = WithFilter(p)

  final class WithFilter(p: T => Boolean) {
    def map[U](f: T => U): Try[U] = Try.this filter p map f
    def flatMap[U](f: T => Try[U]): Try[U] = Try.this filter p flatMap f
    def foreach[U](f: T => U): Unit = Try.this filter p foreach f
    def withFilter(q: T => Boolean): WithFilter = WithFilter(x => p(x) && q(x))
  }

  def recoverWith[U >: T](pf: PartialFunction[E, Try[U]]): Try[U]

  def recover[U >: T](pf: PartialFunction[E, U]): Try[U]

  def toOption: Option[T]

  def toEither: Either[E, T]

}

object Try {
  def apply[T](r: => T): Try[T] =
    try {
      val r1 = r
      Ok(r1)
    } catch {
      case NonFatal(e) => return Failure(e)
    }
}

final case class Failure[E](exc: Exception) extends Try[T] {

  override def isOk: Boolean = false

  override def isFailure: Boolean = true

  override def get: T = throw Ex(exc)

  override def failed: Try[Exception] = Ok(exc)

  override def getOrElse[U >: T](default: => U): U = default

  override def orElse[U >: T](default: => Try[U]): Try[U] =
    try default
    catch { case Exc(e) => Failure(e) }

  override def flatMap[U](f: T => Try[U]): Try[U] =
    this.asInstanceOf[Try[U]]

  override def foreach[U](f: T => U): Unit = ()

  override def transform[U](s: T => Try[U], f: Exception => Try[U]): Try[U] =
    try f(exc)
    catch { case NonFatal(e) => Failure(e) }

  override def map[U](f: T => U): Try[U] =
    this.asInstanceOf[Try[U]]

  override def collect[U](pf: PartialFunction[T, U]): Try[U] =
    this.asInstanceOf[Try[U]]

  override def fold[U](fa: Exception => U, fb: T => U): U = fa(exc)

  override def filter(p: T => Boolean): Try[T] = this

  override def recover[U >: T](pf: PartialFunction[Exception, U]): Try[U] = {
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(exc, (x: Exception) => marker)
      if (marker ne v.asInstanceOf[AnyRef]) Ok(v.asInstanceOf[U]) else this
    } catch { case NonFatal(e) => Failure(e) }
  }

  override def recoverWith[U >: T](
      pf: PartialFunction[Exception, Try[U]]
  ): Try[U] = {
    val marker = Statics.pfMarker
    try {
      val v = pf.applyOrElse(exc, (x: Exception) => marker)
      if (marker ne v.asInstanceOf[AnyRef]) v.asInstanceOf[Try[U]] else this
    } catch { case NonFatal(e) => Failure(e) }
  }

  override def toOption: Option[T] = None

  override def toEither: Either[Exception, T] = Left(exc)

}

final case class Ok[+T](value: T) extends Try[T] {
  override def isFailure: Boolean = false

  override def isOk: Boolean = true

  override def get = value

  override def getOrElse[U >: T](default: => U): U = get

  override def orElse[U >: T](default: => Try[U]): Try[U] = this

  override def flatMap[U](f: T => Try[U]): Try[U] =
    try f(value)
    catch { case NonFatal(e) => Failure(e) }

  override def flatten[U](implicit ev: T <:< Try[U]): Try[U] = value

  override def foreach[U](f: T => U): Unit = f(value)

  override def transform[U](s: T => Try[U], f: Exception => Try[U]): Try[U] =
    this flatMap s

  override def map[U](f: T => U): Try[U] = Try[U](f(value))

  override def collect[U](pf: PartialFunction[T, U]): Try[U] = {
    val marker = Statics.pfMarker
    try {
      val v =
        pf.applyOrElse(value, ((x: T) => marker).asInstanceOf[Function[T, U]])
      if (marker ne v.asInstanceOf[AnyRef]) Ok(v)
      else
        Failure(
          new NoSuchElementException("Predicate does not hold for " + value)
        )
    } catch { case NonFatal(e) => Failure(e) }
  }

  override def filter(p: T => Boolean): Try[T] =
    try {
      if (p(value)) this
      else
        Failure(
          new NoSuchElementException("Predicate does not hold for " + value)
        )
    } catch { case NonFatal(e) => Failure(e) }

  override def recover[U >: T](pf: PartialFunction[Exception, U]): Try[U] = this

  override def recoverWith[U >: T](
      pf: PartialFunction[Exception, Try[U]]
  ): Try[U] = this

  override def failed: Try[Exception] = Failure(
    new UnsupportedOperationException("Ok.failed")
  )

  override def toOption: Option[T] = Some(value)

  override def toEither: Either[Exception, T] = Right(value)

  override def fold[U](fa: Exception => U, fb: T => U): U =
    try { fb(value) }
    catch { case NonFatal(e) => fa(e) }
}
