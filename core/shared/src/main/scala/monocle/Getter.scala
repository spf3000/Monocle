package monocle

import cats.{Eq, Monoid, Semigroupal}
import cats.arrow.{Arrow, Choice}
import cats.implicits._
import monocle.function.Each

/** A [[Getter]] can be seen as a glorified get method between
  * a type S and a type A.
  *
  * A [[Getter]] is also a valid [[Fold]]
  *
  * @tparam S the source of a [[Getter]]
  * @tparam A the target of a [[Getter]]
  */
abstract class Getter[S, A] extends Serializable { self =>

  /** get the target of a [[Getter]] */
  def get(s: S): A

  /** find if the target satisfies the predicate */
  @inline final def find(p: A => Boolean): S => Option[A] =
    s => Some(get(s)).filter(p)

  /** check if the target satisfies the predicate */
  @inline final def exist(p: A => Boolean): S => Boolean =
    p compose get

  /** join two [[Getter]] with the same target */
  @inline final def choice[S1](other: Getter[S1, A]): Getter[Either[S, S1], A] =
    Getter[Either[S, S1], A](_.fold(self.get, other.get))

  /** pair two disjoint [[Getter]] */
  @inline final def split[S1, A1](other: Getter[S1, A1]): Getter[(S, S1), (A, A1)] =
    Getter[(S, S1), (A, A1)] { case (s, s1) => (self.get(s), other.get(s1)) }

  @inline final def zip[A1](other: Getter[S, A1]): Getter[S, (A, A1)] =
    Getter[S, (A, A1)](s => (self.get(s), other.get(s)))

  @inline final def first[B]: Getter[(S, B), (A, B)] =
    Getter[(S, B), (A, B)] { case (s, b) => (self.get(s), b) }

  @inline final def second[B]: Getter[(B, S), (B, A)] =
    Getter[(B, S), (B, A)] { case (b, s) => (b, self.get(s)) }

  @inline final def left[C]: Getter[Either[S, C], Either[A, C]] =
    Getter[Either[S, C], Either[A, C]](_.leftMap(get))

  @inline final def right[C]: Getter[Either[C, S], Either[C, A]] =
    Getter[Either[C, S], Either[C, A]](_.map(get))

  def each[C](implicit evEach: Each[A, C]): Fold[S, C] =
    composeTraversal(evEach.each)

  def some[A1](implicit ev1: A =:= Option[A1]): Fold[S, A1] =
    adapt[Option[A1]] composePrism (std.option.pSome)

  def withDefault[A1: Eq](defaultValue: A1)(implicit ev1: A =:= Option[A1]): Getter[S, A1] =
    adapt[Option[A1]] composeIso (std.option.withDefault(defaultValue))

  private def adapt[A1](implicit evA: A =:= A1): Getter[S, A1] =
    evA.substituteCo[Getter[S, *]](this)

  /** **********************************************************
    */
  /** Compose methods between a [[Getter]] and another Optics */
  /** **********************************************************
    */
  /** compose a [[Getter]] with a [[Fold]] */
  @inline final def composeFold[B](other: Fold[A, B]): Fold[S, B] =
    asFold composeFold other

  /** Compose with a function lifted into a Getter */
  @inline def to[C](f: A => C): Getter[S, C] = composeGetter(Getter(f))

  /** compose a [[Getter]] with a [[Getter]] */
  @inline final def composeGetter[B](other: Getter[A, B]): Getter[S, B] =
    (s: S) => other.get(self.get(s))

  /** compose a [[Getter]] with a [[PTraversal]] */
  @inline final def composeTraversal[B, C, D](other: PTraversal[A, B, C, D]): Fold[S, C] =
    asFold composeTraversal other

  /** compose a [[Getter]] with a [[POptional]] */
  @inline final def composeOptional[B, C, D](other: POptional[A, B, C, D]): Fold[S, C] =
    asFold composeOptional other

  /** compose a [[Getter]] with a [[PPrism]] */
  @inline final def composePrism[B, C, D](other: PPrism[A, B, C, D]): Fold[S, C] =
    asFold composePrism other

  /** compose a [[Getter]] with a [[PLens]] */
  @inline final def composeLens[B, C, D](other: PLens[A, B, C, D]): Getter[S, C] =
    composeGetter(other.asGetter)

  /** compose a [[Getter]] with a [[PIso]] */
  @inline final def composeIso[B, C, D](other: PIso[A, B, C, D]): Getter[S, C] =
    composeGetter(other.asGetter)

  /** *****************************************
    */
  /** Experimental aliases of compose methods */
  /** *****************************************
    */
  /** alias to composeTraversal */
  @inline final def ^|->>[B, C, D](other: PTraversal[A, B, C, D]): Fold[S, C] =
    composeTraversal(other)

  /** alias to composeOptional */
  @inline final def ^|-?[B, C, D](other: POptional[A, B, C, D]): Fold[S, C] =
    composeOptional(other)

  /** alias to composePrism */
  @inline final def ^<-?[B, C, D](other: PPrism[A, B, C, D]): Fold[S, C] =
    composePrism(other)

  /** alias to composeLens */
  @inline final def ^|->[B, C, D](other: PLens[A, B, C, D]): Getter[S, C] =
    composeLens(other)

  /** alias to composeIso */
  @inline final def ^<->[B, C, D](other: PIso[A, B, C, D]): Getter[S, C] =
    composeIso(other)

  /** ***************************************************************
    */
  /** Transformation methods to view a [[Getter]] as another Optics */
  /** ***************************************************************
    */
  /** view a [[Getter]] with a [[Fold]] */
  @inline final def asFold: Fold[S, A] =
    new Fold[S, A] {
      def foldMap[M: Monoid](f: A => M)(s: S): M =
        f(get(s))
    }
}

object Getter extends GetterInstances {
  def id[A]: Getter[A, A] =
    Iso.id[A].asGetter

  def codiagonal[A]: Getter[Either[A, A], A] =
    Getter[Either[A, A], A](_.fold(identity, identity))

  def apply[S, A](_get: S => A): Getter[S, A] =
    (s: S) => _get(s)
}

sealed abstract class GetterInstances extends GetterInstances0 {
  implicit val getterArrow: Arrow[Getter] = new Arrow[Getter] {
    def lift[A, B](f: (A) => B): Getter[A, B] =
      Getter(f)

    def first[A, B, C](f: Getter[A, B]): Getter[(A, C), (B, C)] =
      f.first

    override def second[A, B, C](f: Getter[A, B]): Getter[(C, A), (C, B)] =
      f.second

    override def id[A]: Getter[A, A] =
      Getter.id

    def compose[A, B, C](f: Getter[B, C], g: Getter[A, B]): Getter[A, C] =
      g composeGetter f
  }

  implicit def getterSemigroupal[S]: Semigroupal[Getter[S, *]] =
    new Semigroupal[Getter[S, *]] {
      override def product[A, B](a: Getter[S, A], b: Getter[S, B]) = a zip b
    }
}

sealed abstract class GetterInstances0 {
  implicit val getterChoice: Choice[Getter] = new Choice[Getter] {
    def choice[A, B, C](f: Getter[A, C], g: Getter[B, C]): Getter[Either[A, B], C] =
      f choice g

    def id[A]: Getter[A, A] =
      Getter.id

    def compose[A, B, C](f: Getter[B, C], g: Getter[A, B]): Getter[A, C] =
      g composeGetter f
  }
}
