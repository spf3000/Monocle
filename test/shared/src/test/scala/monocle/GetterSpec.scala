package monocle

import cats.Semigroupal
import cats.arrow.{Arrow, Category, Choice, Compose, Profunctor}

class GetterSpec extends MonocleSuite {
  case class Bar(i: Int)
  case class Foo(bar: Bar)

  val bar = Getter[Foo, Bar](_.bar)
  val i   = Getter[Bar, Int](_.i)

  // test implicit resolution of type classes

  test("Getter has a Compose instance") {
    Compose[Getter].compose(i, bar).get(Foo(Bar(3))) shouldEqual 3
  }

  test("Getter has a Category instance") {
    Category[Getter].id[Int].get(3) shouldEqual 3
  }

  test("Getter has a Choice instance") {
    Choice[Getter]
      .choice(i, Choice[Getter].id[Int])
      .get(Left(Bar(3))) shouldEqual 3
  }

  test("Getter has a Profunctor instance") {
    Profunctor[Getter].rmap(bar)(_.i).get(Foo(Bar(3))) shouldEqual 3
  }

  test("Getter has a Arrow instance") {
    Arrow[Getter].lift((_: Int) * 2).get(4) shouldEqual 8
  }

  test("Getter has a Semigroupal instance") {
    val length = Getter[String, Int](_.length)
    val upper  = Getter[String, String](_.toUpperCase)
    Semigroupal[Getter[String, *]]
      .product(length, upper)
      .get("helloworld") shouldEqual ((10, "HELLOWORLD"))
  }

  test("get") {
    i.get(Bar(5)) shouldEqual 5
  }

  test("find") {
    i.find(_ > 5)(Bar(9)) shouldEqual Some(9)
    i.find(_ > 5)(Bar(3)) shouldEqual None
  }

  test("exist") {
    i.exist(_ > 5)(Bar(9)) shouldEqual true
    i.exist(_ > 5)(Bar(3)) shouldEqual false
  }

  test("zip") {
    val length = Getter[String, Int](_.length)
    val upper  = Getter[String, String](_.toUpperCase)
    length.zip(upper).get("helloworld") shouldEqual ((10, "HELLOWORLD"))
  }

  test("to") {
    i.to(_.toString()).get(Bar(5)) shouldEqual "5"
  }

  test("some") {
    case class SomeTest(x: Int, y: Option[Int])
    val obj = SomeTest(1, Some(2))

    val getter = Getter((_: SomeTest).y)

    getter.some.getAll(obj) shouldEqual List(2)
    obj.applyGetter(getter).some.getAll shouldEqual List(2)
  }

  test("withDefault") {
    case class SomeTest(x: Int, y: Option[Int])
    val objSome = SomeTest(1, Some(2))
    val objNone = SomeTest(1, None)

    val getter = Getter((_: SomeTest).y)

    getter.withDefault(0).get(objSome) shouldEqual 2
    getter.withDefault(0).get(objNone) shouldEqual 0

    objSome.applyGetter(getter).withDefault(0).get shouldEqual 2
    objNone.applyGetter(getter).withDefault(0).get shouldEqual 0
  }

  test("each") {
    case class SomeTest(x: Int, y: List[Int])
    val obj = SomeTest(1, List(1, 2, 3))

    val getter = Getter((_: SomeTest).y)

    getter.each.getAll(obj) shouldEqual List(1, 2, 3)
    obj.applyGetter(getter).each.getAll shouldEqual List(1, 2, 3)
  }
}
