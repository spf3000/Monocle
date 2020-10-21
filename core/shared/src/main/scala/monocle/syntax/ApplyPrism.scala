package monocle.syntax

import cats.{Applicative, Eq}
import monocle.function.Each
import monocle.{std, Fold, PIso, PLens, POptional, PPrism, PSetter, PTraversal}

final case class ApplyPrism[S, T, A, B](s: S, prism: PPrism[S, T, A, B]) {
  @inline def getOption: Option[A] = prism.getOption(s)

  @inline def modify(f: A => B): T = prism.modify(f)(s)
  @inline def modifyF[F[_]: Applicative](f: A => F[B]): F[T] =
    prism.modifyF(f)(s)
  @inline def modifyOption(f: A => B): Option[T] = prism.modifyOption(f)(s)

  @inline def set(b: B): T                     = prism.set(b)(s)
  @inline def setOption(b: B): Option[T]       = prism.setOption(b)(s)
  @inline def isEmpty: Boolean                 = prism.isEmpty(s)
  @inline def nonEmpty: Boolean                = prism.nonEmpty(s)
  @inline def find(p: A => Boolean): Option[A] = prism.find(p)(s)
  @inline def exist(p: A => Boolean): Boolean  = prism.exist(p)(s)
  @inline def all(p: A => Boolean): Boolean    = prism.all(p)(s)

  def some[A1, B1](implicit ev1: A =:= Option[A1], ev2: B =:= Option[B1]): ApplyPrism[S, T, A1, B1] =
    adapt[Option[A1], Option[B1]] composePrism (std.option.pSome)

  private[monocle] def adapt[A1, B1](implicit evA: A =:= A1, evB: B =:= B1): ApplyPrism[S, T, A1, B1] =
    evB.substituteCo[ApplyPrism[S, T, A1, *]](evA.substituteCo[ApplyPrism[S, T, *, B]](this))

  @inline def composeSetter[C, D](other: PSetter[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, prism composeSetter other)
  @inline def composeFold[C](other: Fold[A, C]): ApplyFold[S, C] =
    ApplyFold(s, prism composeFold other)
  @inline def composeTraversal[C, D](other: PTraversal[A, B, C, D]): ApplyTraversal[S, T, C, D] =
    ApplyTraversal(s, prism composeTraversal other)
  @inline def composeOptional[C, D](other: POptional[A, B, C, D]): ApplyOptional[S, T, C, D] =
    ApplyOptional(s, prism composeOptional other)
  @inline def composeLens[C, D](other: PLens[A, B, C, D]): ApplyOptional[S, T, C, D] =
    ApplyOptional(s, prism composeLens other)
  @inline def composePrism[C, D](other: PPrism[A, B, C, D]): ApplyPrism[S, T, C, D] =
    ApplyPrism(s, prism composePrism other)
  @inline def composeIso[C, D](other: PIso[A, B, C, D]): ApplyPrism[S, T, C, D] =
    ApplyPrism(s, prism composeIso other)

  /** alias to composeTraversal */
  @inline def ^|->>[C, D](other: PTraversal[A, B, C, D]): ApplyTraversal[S, T, C, D] =
    composeTraversal(other)

  /** alias to composeOptional */
  @inline def ^|-?[C, D](other: POptional[A, B, C, D]): ApplyOptional[S, T, C, D] =
    composeOptional(other)

  /** alias to composePrism */
  @inline def ^<-?[C, D](other: PPrism[A, B, C, D]): ApplyPrism[S, T, C, D] =
    composePrism(other)

  /** alias to composeLens */
  @inline def ^|->[C, D](other: PLens[A, B, C, D]): ApplyOptional[S, T, C, D] =
    composeLens(other)

  /** alias to composeIso */
  @inline def ^<->[C, D](other: PIso[A, B, C, D]): ApplyPrism[S, T, C, D] =
    composeIso(other)
}

object ApplyPrism {
  implicit def applyPrismSyntax[S, A](self: ApplyPrism[S, S, A, A]): ApplyPrismSyntax[S, A] =
    new ApplyPrismSyntax(self)
}

/** Extension methods for monomorphic ApplyPrism */
final case class ApplyPrismSyntax[S, A](private val self: ApplyPrism[S, S, A, A]) extends AnyVal {
  def each[C](implicit evEach: Each[A, C]): ApplyTraversal[S, S, C, C] =
    self composeTraversal evEach.each

  def withDefault[A1: Eq](defaultValue: A1)(implicit evOpt: A =:= Option[A1]): ApplyPrism[S, S, A1, A1] =
    self.adapt[Option[A1], Option[A1]] composeIso (std.option.withDefault(defaultValue))
}
