package io.chrisdavenport.simpleeffect.sync

import scala.util.control.NonFatal

sealed trait SyncIO[+A]
// Minimal Monad Subset
private final case class Pure[A](a: A) extends SyncIO[A]
private final case class FlatMap[B, A](init: SyncIO[B], f: B => SyncIO[A]) extends SyncIO[A]

// Minimal Error Signalling
private final case class RaiseError(t: Throwable) extends SyncIO[Nothing]

// Minimal Bracket Implementations
private final case class Bracket[B, A](
  acquire: SyncIO[B],
  use: B => SyncIO[A],
  release: B => SyncIO[Unit]
) extends SyncIO[A]

// Minimal Delay
private final case class Delay[A](thunk: () => A) extends SyncIO[A]

object SyncIO {
  def pure[A](a: A): SyncIO[A] = Pure(a)
  def delay[A](a: => A): SyncIO[A] = Delay(() => a)
  def suspend[A](syncio: => SyncIO[A]): SyncIO[A] = flatMap(Delay(() => syncio))(identity)

  def flatMap[A, B](init: SyncIO[A])(f: A => SyncIO[B]): SyncIO[B] = FlatMap(init, f)
  def raiseError[A](e: Throwable): SyncIO[A] = RaiseError(e)
  def bracket[A, B](acquire: SyncIO[A])(use: A => SyncIO[B])(release: A => SyncIO[Unit]): SyncIO[B] = Bracket(acquire, use, release)

  def handleErrorWith[A](fa: SyncIO[A])(f: Throwable => SyncIO[A]): SyncIO[A] = fa match {
    case RaiseError(e) => f(e)
    case a => a
  }

  object unsafe {
    def unsafeRunSyncEither[A]: SyncIO[A] => Either[Throwable, A] = {
      case Pure(a) => Right(a)
      case RaiseError(e) => Left(e)
      case Delay(thunk) => 
        try {
          Right(thunk())
        } catch {
          case NonFatal(e) => Left(e)
        }
      case FlatMap(init, f) => unsafeRunSyncEither(init).flatMap{a => 
        unsafeRunSyncEither((f(a)))
      }
      case Bracket(acquire, use, release) =>
        unsafeRunSyncEither(acquire).flatMap{b => 
          try {
            unsafeRunSyncEither(use(b))
          } finally {
            unsafeRunSyncEither(release(b))
            // Explicitly Ignore Errors in release
            ()
          }
        }
    }
    def unsafeRunSync[A]: SyncIO[A] => A = io => 
      unsafeRunSyncEither(io).fold(throw _, identity)
  }
  
}