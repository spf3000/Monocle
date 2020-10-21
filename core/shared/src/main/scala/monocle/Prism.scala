package monocle

import cats.{Applicative, Eq, Monoid, Traverse}
import cats.arrow.Category
import cats.evidence.{<~<, Is}
import cats.instances.option._
import cats.syntax.either._
import monocle.function.Each

/** A [[PPrism]] can be seen as a pair of functions:
  *  - `getOrModify: S => Either[T, A]`
  *  - `reverseGet : B => T`
  *
  * A [[PPrism]] could also be defined as a weaker [[PIso]] where get can fail.
  *
  * Typically a [[PPrism]] or [[Prism]] encodes the relation between a Sum or
  * CoProduct type (e.g. sealed trait) and one of its element.
  *
  * [[PPrism]] stands for Polymorphic Prism as it set and modify methods change
  * a type `A` to `B` and `S` to `T`.
  * [[Prism]] is a type alias for [[PPrism]] where the type of target cannot be modified:
  * {{{
  * type Prism[S, A] = PPrism[S, S, A, A]
  * }}}
  *
  * A [[PPrism]] is also a valid  [[Fold]], [[POptional]], [[PTraversal]] and [[PSetter]]
  *
  * @see [[monocle.law.PrismLaws]]
  *
  * @tparam S the source of a [[PPrism]]
  * @tparam T the modified source of a [[PPrism]]
  * @tparam A the target of a [[PPrism]]
  * @tparam B the modified target of a [[PPrism]]
  */
abstract class PPrism[S, T, A, B] extends Serializable { self =>

  /** get the target of a [[PPrism]] or return the original value while allowing the type to change if it does not match */
  def getOrModify(s: S): Either[T, A]

  /** get the modified source of a [[PPrism]] */
  def reverseGet(b: B): T

  /** get the target of a [[PPrism]] or nothing if there is no target */
  def getOption(s: S): Option[A]

  /** modify polymorphically the target of a [[PPrism]] with an Applicative function */
  @inline final def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
    getOrModify(s).fold(
      t => Applicative[F].pure(t),
      a => Applicative[F].map(f(a))(reverseGet)
    )

  /** modify polymorphically the target of a [[PPrism]] with a function */
  @inline final def modify(f: A => B): S => T =
    getOrModify(_).fold(identity, a => reverseGet(f(a)))

  /** modify polymorphically the target of a [[PPrism]] with a function.
    * return empty if the [[PPrism]] is not matching
    */
  @inline final def modifyOption(f: A => B): S => Option[T] =
    s => getOption(s).map(a => reverseGet(f(a)))

  /** set polymorphically the target of a [[PPrism]] with a value */
  @inline final def set(b: B): S => T =
    modify(_ => b)

  /** set polymorphically the target of a [[PPrism]] with a value.
    * return empty if the [[PPrism]] is not matching
    */
  @inline final def setOption(b: B): S => Option[T] =
    modifyOption(_ => b)

  /** check if there is no target */
  @inline final def isEmpty(s: S): Boolean =
    getOption(s).isEmpty

  /** check if there is a target */
  @inline final def nonEmpty(s: S): Boolean =
    getOption(s).isDefined

  /** find if the target satisfies the predicate */
  @inline final def find(p: A => Boolean): S => Option[A] =
    getOption(_).flatMap(a => Some(a).filter(p))

  /** check if there is a target and it satisfies the predicate */
  @inline final def exist(p: A => Boolean): S => Boolean =
    getOption(_).fold(false)(p)

  /** check if there is no target or the target satisfies the predicate */
  @inline final def all(p: A => Boolean): S => Boolean =
    getOption(_).fold(true)(p)

  /** create a [[Getter]] from the modified target to the modified source of a [[PPrism]] */
  @inline final def re: Getter[B, T] =
    Getter(reverseGet)

  @inline final def first[C]: PPrism[(S, C), (T, C), (A, C), (B, C)] =
    PPrism[(S, C), (T, C), (A, C), (B, C)] { case (s, c) =>
      getOrModify(s).bimap(_ -> c, _ -> c)
    } { case (b, c) =>
      (reverseGet(b), c)
    }

  @inline final def second[C]: PPrism[(C, S), (C, T), (C, A), (C, B)] =
    PPrism[(C, S), (C, T), (C, A), (C, B)] { case (c, s) =>
      getOrModify(s).bimap(c -> _, c -> _)
    } { case (c, b) =>
      (c, reverseGet(b))
    }

  @inline final def left[C]: PPrism[Either[S, C], Either[T, C], Either[A, C], Either[B, C]] =
    PPrism[Either[S, C], Either[T, C], Either[A, C], Either[B, C]](
      _.fold(getOrModify(_).bimap(Either.left, Either.left), c => Either.right(Either.right(c)))
    )(_.leftMap(reverseGet))

  @inline final def right[C]: PPrism[Either[C, S], Either[C, T], Either[C, A], Either[C, B]] =
    PPrism[Either[C, S], Either[C, T], Either[C, A], Either[C, B]](
      _.fold(c => Either.right(Either.left(c)), getOrModify(_).bimap(Either.right, Either.right))
    )(_.map(reverseGet))

  def some[A1, B1](implicit ev1: A =:= Option[A1], ev2: B =:= Option[B1]): PPrism[S, T, A1, B1] =
    adapt[Option[A1], Option[B1]] composePrism (std.option.pSome)

  private[monocle] def adapt[A1, B1](implicit evA: A =:= A1, evB: B =:= B1): PPrism[S, T, A1, B1] =
    evB.substituteCo[PPrism[S, T, A1, *]](evA.substituteCo[PPrism[S, T, *, B]](this))

  /** *********************************************************
    */
  /** Compose methods between a [[PPrism]] and another Optics */
  /** *********************************************************
    */
  /** compose a [[PPrism]] with a [[Fold]] */
  @inline final def composeFold[C](other: Fold[A, C]): Fold[S, C] =
    asFold composeFold other

  /** Compose with a function lifted into a Getter */
  @inline def to[C](f: A => C): Fold[S, C] = composeGetter(Getter(f))

  /** compose a [[PPrism]] with a [[Getter]] */
  @inline final def composeGetter[C](other: Getter[A, C]): Fold[S, C] =
    asFold composeGetter other

  /** compose a [[PPrism]] with a [[PSetter]] */
  @inline final def composeSetter[C, D](other: PSetter[A, B, C, D]): PSetter[S, T, C, D] =
    asSetter composeSetter other

  /** compose a [[PPrism]] with a [[PTraversal]] */
  @inline final def composeTraversal[C, D](other: PTraversal[A, B, C, D]): PTraversal[S, T, C, D] =
    asTraversal composeTraversal other

  /** compose a [[PPrism]] with a [[POptional]] */
  @inline final def composeOptional[C, D](other: POptional[A, B, C, D]): POptional[S, T, C, D] =
    asOptional composeOptional other

  /** compose a [[PPrism]] with a [[PLens]] */
  @inline final def composeLens[C, D](other: PLens[A, B, C, D]): POptional[S, T, C, D] =
    asOptional composeOptional other.asOptional

  /** compose a [[PPrism]] with a [[PPrism]] */
  @inline final def composePrism[C, D](other: PPrism[A, B, C, D]): PPrism[S, T, C, D] =
    new PPrism[S, T, C, D] {
      def getOrModify(s: S): Either[T, C] =
        self
          .getOrModify(s)
          .flatMap(a => other.getOrModify(a).bimap(self.set(_)(s), identity))

      def reverseGet(d: D): T =
        self.reverseGet(other.reverseGet(d))

      def getOption(s: S): Option[C] =
        self.getOption(s) flatMap other.getOption
    }

  /** compose a [[PPrism]] with a [[PIso]] */
  @inline final def composeIso[C, D](other: PIso[A, B, C, D]): PPrism[S, T, C, D] =
    composePrism(other.asPrism)

  /** *****************************************
    */
  /** Experimental aliases of compose methods */
  /** *****************************************
    */
  /** alias to composeTraversal */
  @inline final def ^|->>[C, D](other: PTraversal[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other)

  /** alias to composeOptional */
  @inline final def ^|-?[C, D](other: POptional[A, B, C, D]): POptional[S, T, C, D] =
    composeOptional(other)

  /** alias to composePrism */
  @inline final def ^<-?[C, D](other: PPrism[A, B, C, D]): PPrism[S, T, C, D] =
    composePrism(other)

  /** alias to composeLens */
  @inline final def ^|->[C, D](other: PLens[A, B, C, D]): POptional[S, T, C, D] =
    composeLens(other)

  /** alias to composeIso */
  @inline final def ^<->[C, D](other: PIso[A, B, C, D]): PPrism[S, T, C, D] =
    composeIso(other)

  /** ***************************************************************
    */
  /** Transformation methods to view a [[PPrism]] as another Optics */
  /** ***************************************************************
    */
  /** view a [[PPrism]] as a [[Fold]] */
  @inline final def asFold: Fold[S, A] =
    new Fold[S, A] {
      def foldMap[M: Monoid](f: A => M)(s: S): M =
        getOption(s) map f getOrElse Monoid[M].empty
    }

  /** view a [[PPrism]] as a [[Setter]] */
  @inline final def asSetter: PSetter[S, T, A, B] =
    new PSetter[S, T, A, B] {
      def modify(f: A => B): S => T =
        self.modify(f)

      def set(b: B): S => T =
        self.set(b)
    }

  /** view a [[PPrism]] as a [[PTraversal]] */
  @inline final def asTraversal: PTraversal[S, T, A, B] =
    new PTraversal[S, T, A, B] {
      def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
        self.modifyF(f)(s)
    }

  /** view a [[PPrism]] as a [[POptional]] */
  @inline final def asOptional: POptional[S, T, A, B] =
    new POptional[S, T, A, B] {
      def getOrModify(s: S): Either[T, A] =
        self.getOrModify(s)

      def set(b: B): S => T =
        self.set(b)

      def getOption(s: S): Option[A] =
        self.getOption(s)

      def modify(f: A => B): S => T =
        self.modify(f)

      def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
        self.modifyF(f)(s)
    }

  /** **********************************************************************
    */
  /** Apply methods to treat a [[PPrism]] as smart constructors for type T */
  /** **********************************************************************
    */
  def apply()(implicit ev: Is[B, Unit]): T =
    ev.substitute[PPrism[S, T, A, *]](self).reverseGet(())

  def apply(b: B): T = reverseGet(b)

  def apply[C, D](c: C, d: D)(implicit ev: (C, D) <~< B): T = apply(ev((c, d)))

  def apply[C, D, E](c: C, d: D, e: E)(implicit ev: (C, D, E) <~< B): T =
    apply(ev((c, d, e)))

  def apply[C, D, E, F](c: C, d: D, e: E, f: F)(implicit ev: (C, D, E, F) <~< B): T =
    apply(ev((c, d, e, f)))

  def apply[C, D, E, F, G](c: C, d: D, e: E, f: F, g: G)(implicit ev: (C, D, E, F, G) <~< B): T =
    apply(ev((c, d, e, f, g)))

  def apply[C, D, E, F, G, H](c: C, d: D, e: E, f: F, g: G, h: H)(implicit ev: (C, D, E, F, G, H) <~< B): T =
    apply(ev((c, d, e, f, g, h)))

  def unapply(obj: S): Option[A] = getOption(obj)
}

object PPrism extends PrismInstances {
  def id[S, T]: PPrism[S, T, S, T] =
    PIso.id[S, T].asPrism

  /** create a [[PPrism]] using the canonical functions: getOrModify and reverseGet */
  def apply[S, T, A, B](_getOrModify: S => Either[T, A])(_reverseGet: B => T): PPrism[S, T, A, B] =
    new PPrism[S, T, A, B] {
      def getOrModify(s: S): Either[T, A] =
        _getOrModify(s)

      def reverseGet(b: B): T =
        _reverseGet(b)

      def getOption(s: S): Option[A] =
        _getOrModify(s).toOption
    }

  implicit def prismSyntax[S, A](self: Prism[S, A]): PrismSyntax[S, A] =
    new PrismSyntax(self)
}

object Prism {
  def id[A]: Prism[A, A] =
    Iso.id[A].asPrism

  /** alias for [[PPrism]] apply restricted to monomorphic update */
  def apply[S, A](_getOption: S => Option[A])(_reverseGet: A => S): Prism[S, A] =
    new Prism[S, A] {
      def getOrModify(s: S): Either[S, A] =
        _getOption(s).fold[Either[S, A]](Either.left(s))(Either.right)

      def reverseGet(b: A): S =
        _reverseGet(b)

      def getOption(s: S): Option[A] =
        _getOption(s)
    }

  /** Create a Prism using a partial function rather than Option. */
  def partial[S, A](get: PartialFunction[S, A])(reverseGet: A => S): Prism[S, A] =
    Prism[S, A](get.lift)(reverseGet)

  /** a [[Prism]] that checks for equality with a given value */
  def only[A](a: A)(implicit A: Eq[A]): Prism[A, Unit] =
    Prism[A, Unit](a2 => if (A.eqv(a, a2)) Some(()) else None)(_ => a)
}

sealed abstract class PrismInstances {
  implicit val prismCategory: Category[Prism] = new Category[Prism] {
    def id[A]: Prism[A, A] =
      Prism.id

    def compose[A, B, C](f: Prism[B, C], g: Prism[A, B]): Prism[A, C] =
      g composePrism f
  }
}

final case class PrismSyntax[S, A](private val self: Prism[S, A]) extends AnyVal {

  /** lift a [[Prism]] such as it only matches if all elements of `F[S]` are getOrModify */
  def below[F[_]](implicit F: Traverse[F]): Prism[F[S], F[A]] =
    Prism[F[S], F[A]](F.traverse(_)(self.getOption))(F.map(_)(self.reverseGet))

  def each[C](implicit evEach: Each[A, C]): Traversal[S, C] =
    self composeTraversal evEach.each

  def withDefault[A1: Eq](defaultValue: A1)(implicit evOpt: A =:= Option[A1]): Prism[S, A1] =
    self.adapt[Option[A1], Option[A1]] composeIso (std.option.withDefault(defaultValue))
}
