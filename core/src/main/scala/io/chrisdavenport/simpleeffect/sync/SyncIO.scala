package io.chrisdavenport.simpleeffect.sync

import scala.util.control.NonFatal

sealed abstract class SyncIO[A]{
  private[sync] def unsafeRunSyncEither: Either[Throwable, A]
}
// Minimal Monad Subset
private final case class Pure[A](a: A) extends SyncIO[A]{
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] =
    Right(a)
}
private final case class FlatMap[B, A](init: SyncIO[B], f: B => SyncIO[A]) extends SyncIO[A]{
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] = 
    init.unsafeRunSyncEither.flatMap(f(_).unsafeRunSyncEither)
}

// Minimal Error Signalling
private final case class RaiseError[A](t: Throwable) extends SyncIO[A]{
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] =
    Left(t)
}

// Minimal Bracket Implementations
private final case class Bracket[B, A](
  acquire: SyncIO[B],
  use: B => SyncIO[A],
  release: B => SyncIO[Unit]
) extends SyncIO[A] {
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] = 
    acquire.unsafeRunSyncEither.flatMap{b => 
      try {
        use(b).unsafeRunSyncEither
      } finally {
        release(b).unsafeRunSyncEither
        ()
      }
    }
}

// Minimal Delay
private final case class Delay[A](thunk: () => A) extends SyncIO[A]{
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] =
    try { Right(thunk()) } catch { case NonFatal(e) => Left(e) }
}

private final case class HandleErrorWith[A](
  s: SyncIO[A],
  f: Throwable => SyncIO[A]
) extends SyncIO[A]{
  override private[sync] def unsafeRunSyncEither: Either[Throwable, A] =
    s.unsafeRunSyncEither
      .fold(f, Pure(_))
      .unsafeRunSyncEither
}



object SyncIO {
  def pure[A](a: A): SyncIO[A] = Pure(a)
  def delay[A](a: => A): SyncIO[A] = Delay(() => a)
  def suspend[A](syncio: => SyncIO[A]): SyncIO[A] = flatMap(delay(syncio))(identity)

  def flatMap[A, B](init: SyncIO[A])(f: A => SyncIO[B]): SyncIO[B] = FlatMap(init, f)
  def raiseError[A](e: Throwable): SyncIO[A] = RaiseError(e)
  def bracket[A, B](acquire: SyncIO[A])(use: A => SyncIO[B])(release: A => SyncIO[Unit]): SyncIO[B] = Bracket(acquire, use, release)

  def handleErrorWith[A](fa: SyncIO[A])(f: Throwable => SyncIO[A]): SyncIO[A] = HandleErrorWith(fa, f)

  object unsafe {
    def unsafeRunSyncEither[A]: SyncIO[A] => Either[Throwable, A] =
      _.unsafeRunSyncEither

    def unsafeRunSync[A]: SyncIO[A] => A =
      unsafeRunSyncEither(_).fold(throw _, identity)
  }
  
}