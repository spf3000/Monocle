package monocle

import monocle.law.discipline.{SetterTests, TraversalTests}
import monocle.macros.GenLens
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary

import cats.Eq
import cats.arrow.{Category, Choice, Compose}
import cats.syntax.either._

class TraversalSpec extends MonocleSuite {
  case class Location(latitude: Int, longitude: Int, name: String)

  val coordinates: Traversal[Location, Int] =
    Traversal.apply2[Location, Int](_.latitude, _.longitude) { case (newLat, newLong, oldLoc) =>
      oldLoc.copy(latitude = newLat, longitude = newLong)
    }

  def eachL[A]: Traversal[List[A], A]               = PTraversal.fromTraverse[List, A, A]
  val eachLi: Traversal[List[Int], Int]             = eachL[Int]
  def eachL2[A, B]: Traversal[List[(A, B)], (A, B)] = eachL[(A, B)]

  implicit val locationGen: Arbitrary[Location] = Arbitrary(for {
    x <- arbitrary[Int]
    y <- arbitrary[Int]
    n <- arbitrary[String]
  } yield Location(x, y, n))

  implicit val exampleEq = Eq.fromUniversalEquals[Location]

  // Below we test a 7-lenses Traversal created using applyN

  // the test object
  case class ManyPropObject(p1: Int, p2: Int, p3: String, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int)

  // the 7 lenses for each int properties of the test object
  val l1: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p1)
  val l2: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p2)
  val l3: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p4)
  val l4: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p5)
  val l5: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p6)
  val l6: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p7)
  val l7: Lens[ManyPropObject, Int] = GenLens[ManyPropObject](_.p8)

  // the 7-lenses Traversal generated using applyN
  val traversalN: Traversal[ManyPropObject, Int] =
    Traversal.applyN(l1, l2, l3, l4, l5, l6, l7)

  // the stub for generating random test objects
  implicit val manyPropObjectGen: Arbitrary[ManyPropObject] = Arbitrary(for {
    p1 <- arbitrary[Int]
    p2 <- arbitrary[Int]
    p3 <- arbitrary[String]
    p4 <- arbitrary[Int]
    p5 <- arbitrary[Int]
    p6 <- arbitrary[Int]
    p7 <- arbitrary[Int]
    p8 <- arbitrary[Int]
  } yield ManyPropObject(p1, p2, p3, p4, p5, p6, p7, p8))

  implicit val eqForManyPropObject = Eq.fromUniversalEquals[ManyPropObject]

  checkAll("apply2 Traversal", TraversalTests(coordinates))
  checkAll("applyN Traversal", TraversalTests(traversalN))
  checkAll("fromTraverse Traversal", TraversalTests(eachLi))

  checkAll("traversal.asSetter", SetterTests(coordinates.asSetter))

  // test implicit resolution of type classes

  test("Traversal has a Compose instance") {
    Compose[Traversal]
      .compose(coordinates, eachL[Location])
      .modify(_ + 1)(List(Location(1, 2, ""), Location(3, 4, ""))) shouldEqual List(
      Location(2, 3, ""),
      Location(4, 5, "")
    )
  }

  test("Traversal has a Category instance") {
    Category[Traversal].id[Int].getAll(3) shouldEqual List(3)
  }

  test("Traversal has a Choice instance") {
    Choice[Traversal]
      .choice(eachL[Int], coordinates)
      .modify(_ + 1)(Left(List(1, 2, 3))) shouldEqual Left(List(2, 3, 4))
  }

  test("foldMap") {
    eachLi.foldMap(_.toString)(List(1, 2, 3, 4, 5)) shouldEqual "12345"
  }

  test("getAll") {
    eachLi.getAll(List(1, 2, 3, 4)) shouldEqual List(1, 2, 3, 4)
  }

  test("headOption") {
    eachLi.headOption(List(1, 2, 3, 4)) shouldEqual Some(1)
  }

  test("lastOption") {
    eachLi.lastOption(List(1, 2, 3, 4)) shouldEqual Some(4)
  }

  test("length") {
    eachLi.length(List(1, 2, 3, 4)) shouldEqual 4
    eachLi.length(Nil) shouldEqual 0
  }

  test("isEmpty") {
    eachLi.isEmpty(List(1, 2, 3, 4)) shouldEqual false
    eachLi.isEmpty(Nil) shouldEqual true
  }

  test("nonEmpty") {
    eachLi.nonEmpty(List(1, 2, 3, 4)) shouldEqual true
    eachLi.nonEmpty(Nil) shouldEqual false
  }

  test("find") {
    eachLi.find(_ > 2)(List(1, 2, 3, 4)) shouldEqual Some(3)
    eachLi.find(_ > 9)(List(1, 2, 3, 4)) shouldEqual None
  }

  test("exist") {
    eachLi.exist(_ > 2)(List(1, 2, 3, 4)) shouldEqual true
    eachLi.exist(_ > 9)(List(1, 2, 3, 4)) shouldEqual false
    eachLi.exist(_ > 9)(Nil) shouldEqual false
  }

  test("all") {
    eachLi.all(_ > 2)(List(1, 2, 3, 4)) shouldEqual false
    eachLi.all(_ > 0)(List(1, 2, 3, 4)) shouldEqual true
    eachLi.all(_ > 0)(Nil) shouldEqual true
  }

  test("set") {
    eachLi.set(0)(List(1, 2, 3, 4)) shouldEqual List(0, 0, 0, 0)
  }

  test("modify") {
    eachLi.modify(_ + 1)(List(1, 2, 3, 4)) shouldEqual List(2, 3, 4, 5)
  }

  test("parModifyF") {
    eachLi.parModifyF[Either[Unit, *]](i => (i + 1).asRight[Unit])(List(1, 2, 3, 4)) shouldEqual Right(List(2, 3, 4, 5))
    // `Left` values should be accumulated through `Validated`.
    eachLi.parModifyF[Either[String, *]](_.toString.asLeft[Int])(List(1, 2, 3, 4)) shouldEqual Left("1234")
  }

  test("to") {
    eachLi.to(_.toString()).getAll(List(1, 2, 3)) shouldEqual List("1", "2", "3")
  }

  test("some") {
    val numbers   = List(Some(1), None, Some(2), None)
    val traversal = Traversal.fromTraverse[List, Option[Int]]

    traversal.some.set(5)(numbers) shouldEqual List(Some(5), None, Some(5), None)
    numbers.applyTraversal(traversal).some.set(5) shouldEqual List(Some(5), None, Some(5), None)
  }

  test("withDefault") {
    val numbers   = List(Some(1), None, Some(2), None)
    val traversal = Traversal.fromTraverse[List, Option[Int]]

    traversal.withDefault(0).modify(_ + 1)(numbers) shouldEqual List(Some(2), Some(1), Some(3), Some(1))
    numbers.applyTraversal(traversal).withDefault(0).modify(_ + 1) shouldEqual List(Some(2), Some(1), Some(3), Some(1))
  }

  test("each") {
    val numbers   = List(List(1, 2, 3), Nil, List(4), Nil)
    val traversal = Traversal.fromTraverse[List, List[Int]]

    traversal.each.getAll(numbers) shouldEqual List(1, 2, 3, 4)
    numbers.applyTraversal(traversal).each.getAll shouldEqual List(1, 2, 3, 4)
  }
}
