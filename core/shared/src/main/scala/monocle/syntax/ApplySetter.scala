package monocle.syntax

import cats.Eq
import monocle.function.Each
import monocle.{std, PIso, PLens, POptional, PPrism, PSetter, PTraversal}

final case class ApplySetter[S, T, A, B](s: S, setter: PSetter[S, T, A, B]) {
  @inline def set(b: B): T         = setter.set(b)(s)
  @inline def modify(f: A => B): T = setter.modify(f)(s)

  def some[A1, B1](implicit ev1: A =:= Option[A1], ev2: B =:= Option[B1]): ApplySetter[S, T, A1, B1] =
    adapt[Option[A1], Option[B1]] composePrism (std.option.pSome)

  private[monocle] def adapt[A1, B1](implicit evA: A =:= A1, evB: B =:= B1): ApplySetter[S, T, A1, B1] =
    evB.substituteCo[ApplySetter[S, T, A1, *]](evA.substituteCo[ApplySetter[S, T, *, B]](this))

  @inline def composeSetter[C, D](other: PSetter[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composeSetter other)
  @inline def composeTraversal[C, D](other: PTraversal[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composeTraversal other)
  @inline def composeOptional[C, D](other: POptional[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composeOptional other)
  @inline def composePrism[C, D](other: PPrism[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composePrism other)
  @inline def composeLens[C, D](other: PLens[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composeLens other)
  @inline def composeIso[C, D](other: PIso[A, B, C, D]): ApplySetter[S, T, C, D] =
    ApplySetter(s, setter composeIso other)

  /** alias to composeTraversal */
  @inline def ^|->>[C, D](other: PTraversal[A, B, C, D]): ApplySetter[S, T, C, D] =
    composeTraversal(other)

  /** alias to composeOptional */
  @inline def ^|-?[C, D](other: POptional[A, B, C, D]): ApplySetter[S, T, C, D] =
    composeOptional(other)

  /** alias to composePrism */
  @inline def ^<-?[C, D](other: PPrism[A, B, C, D]): ApplySetter[S, T, C, D] =
    composePrism(other)

  /** alias to composeLens */
  @inline def ^|->[C, D](other: PLens[A, B, C, D]): ApplySetter[S, T, C, D] =
    composeLens(other)

  /** alias to composeIso */
  @inline def ^<->[C, D](other: PIso[A, B, C, D]): ApplySetter[S, T, C, D] =
    composeIso(other)
}

object ApplySetter {
  implicit def applySetterSyntax[S, A](self: ApplySetter[S, S, A, A]): ApplySetterSyntax[S, A] =
    new ApplySetterSyntax(self)
}

/** Extension methods for monomorphic ApplySetter */
final case class ApplySetterSyntax[S, A](private val self: ApplySetter[S, S, A, A]) extends AnyVal {
  def each[C](implicit evEach: Each[A, C]): ApplySetter[S, S, C, C] =
    self composeTraversal evEach.each

  def withDefault[A1: Eq](defaultValue: A1)(implicit evOpt: A =:= Option[A1]): ApplySetter[S, S, A1, A1] =
    self.adapt[Option[A1], Option[A1]] composeIso (std.option.withDefault(defaultValue))
}
