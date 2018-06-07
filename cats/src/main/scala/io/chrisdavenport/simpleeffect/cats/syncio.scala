package io.chrisdavenport.simpleeffect.cats

import io.chrisdavenport.simpleeffect.sync.SyncIO

object syncio {
    implicit def syncIOSync = {
    import cats.effect.Sync
    new Sync[SyncIO]{
      // Members declared in cats.Applicative
      def pure[A](x: A): SyncIO[A] = SyncIO.pure(x)
      
      // Members declared in cats.ApplicativeError
      def handleErrorWith[A](fa: SyncIO[A])(f: Throwable => SyncIO[A]): SyncIO[A] =
        SyncIO.handleErrorWith(fa)(f)

      def raiseError[A](e: Throwable): SyncIO[A] = SyncIO.raiseError(e)
      
      // Members declared in cats.FlatMap
      def flatMap[A, B](fa: SyncIO[A])(f: A => SyncIO[B]): SyncIO[B] = SyncIO.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => SyncIO[Either[A,B]]): SyncIO[B] =
        SyncIO.flatMap(f(a)){
          case Left(a) => tailRecM[A, B](a)(f)
          case Right(b) => pure(b)
        }

      // Members declared in cats.effect.Bracket
      def bracketCase[A, B](acquire: SyncIO[A])(use: A => SyncIO[B])(release: (A, cats.effect.ExitCase[Throwable]) => SyncIO[Unit]) : SyncIO[B] = {
        SyncIO.bracket(acquire)(use){a: A => release(a, cats.effect.ExitCase.Completed)}
      }
      
      // Members declared in cats.effect.Sync
      def suspend[A](thunk: => SyncIO[A]): SyncIO[A] = SyncIO.suspend(thunk)
    }
  }
  
}