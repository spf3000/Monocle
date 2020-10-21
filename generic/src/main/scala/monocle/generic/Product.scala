package monocle.generic

import monocle.PTraversal
import monocle.function.Each
import monocle.{Iso, Traversal}
import monocle.generic.internal.TupleGeneric
import cats.Applicative
import cats.syntax.apply._
import shapeless.{::, Generic, HList, HNil}

object product extends ProductOptics

trait ProductOptics {
  def productToTuple[S <: Product](implicit ev: TupleGeneric[S]): Iso[S, ev.Repr] =
    Iso[S, ev.Repr](s => ev.to(s))(t => ev.from(t))

  implicit def hNilEach[A] =
    new Each[HNil, A] {
      def each: Traversal[HNil, A] = Traversal.void[HNil, A]
    }

  implicit def hConsEach[A, Rest <: HList](implicit restEach: Each[Rest, A]) =
    new Each[A :: Rest, A] {
      def each: Traversal[A :: Rest, A] =
        new PTraversal[A :: Rest, A :: Rest, A, A] {
          def modifyF[F[_]: Applicative](f: A => F[A])(s: A :: Rest): F[A :: Rest] =
            (f(s.head), restEach.each.modifyF(f)(s.tail)).mapN(_ :: _)
        }
    }

  implicit def productEach[S, SGen <: HList, A](implicit
    gen: Generic.Aux[S, SGen],
    genEach: Each[SGen, A]
  ): Each[S, A] =
    new Each[S, A] {
      def each: Traversal[S, A] =
        new Traversal[S, A] {
          def modifyF[F[_]: Applicative](f: A => F[A])(s: S): F[S] =
            Applicative[F].map(genEach.each.modifyF(f)(gen.to(s)))(gen.from)
        }
    }
}
