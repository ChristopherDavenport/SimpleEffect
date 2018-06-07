package io.chrisdavenport.simpleeffect.cats

import io.chrisdavenport.simpleeffect.sync.SyncIO
import io.chrisdavenport.simpleeffect.cats.setup.CatsAsyncSuite
import org.scalacheck._
import cats._
import cats.implicits._
import cats.effect.laws.discipline.SyncTests

class SyncLaws extends CatsAsyncSuite {
  import io.chrisdavenport.simpleeffect.cats.syncio._
  import SyncLaws.SyncIOScalaCheckInstances._

  checkAllAsync("Sync[SyncIO]", implicit e => SyncTests[SyncIO].sync[Int, Int, Int])


}

object SyncLaws {

  object SyncIOScalaCheckInstances {
    implicit def cogenTask[A]: Cogen[SyncIO[A]] = Cogen[Unit].contramap(_ => ())

    implicit def syncIOEq[A: Eq](implicit E: Eq[Throwable]): Eq[SyncIO[A]] =
      new Eq[SyncIO[A]] {
        def eqv(x: SyncIO[A], y: SyncIO[A]): Boolean = {
          val xatt: Either[Throwable, A] = SyncIO.unsafe.unsafeRunSyncEither(x)
          val yatt: Either[Throwable, A] = SyncIO.unsafe.unsafeRunSyncEither(y)

          (xatt, yatt) match {
            case (Right(x), Right(y)) => x === y
            case (Left(xf), Left(yf)) => xf === yf
            case _ => false
          }
        }
      }

    implicit def arbSyncIO[A: Arbitrary: Cogen] : Arbitrary[SyncIO[A]] = 
      Arbitrary(Gen.delay(genSyncIO[A]))

    def genSyncIO[A: Arbitrary: Cogen]: Gen[SyncIO[A]] = {
      Gen.frequency(
        5 -> genPure[A],
        5 -> genDelay[A],
        1 -> genFail[A],
        10 -> genFlatMap[A]
      )
    }

    def genPure[A: Arbitrary]: Gen[SyncIO[A]] = 
      Arbitrary.arbitrary[A].map(SyncIO.pure)

    def genDelay[A: Arbitrary]: Gen[SyncIO[A]] = 
      Arbitrary.arbitrary[A].map(SyncIO.delay(_))

    def genFail[A]: Gen[SyncIO[A]] = 
      Arbitrary.arbitrary[Throwable].map(SyncIO.raiseError[A](_))

    def genFlatMap[A: Arbitrary: Cogen]: Gen[SyncIO[A]] = 
      for {
        ioa <- Arbitrary.arbitrary[SyncIO[A]]
        f <- Arbitrary.arbitrary[A => SyncIO[A]]
      } yield SyncIO.flatMap(ioa)(f)



  }
}