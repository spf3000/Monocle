package monocle.function

import monocle.MonocleSuite
import monocle.refined._
import shapeless.test.illTyped
import eu.timepit.refined.auto._

import scala.collection.immutable.SortedMap

class AtExample extends MonocleSuite {
  test("at creates a Lens from a Map, SortedMap to an optional value") {
    (Map("One" -> 2, "Two" -> 2) applyLens at("Two") get) shouldEqual Some(2)
    (SortedMap("One" -> 2, "Two" -> 2) applyLens at("Two") get) shouldEqual Some(2)

    (Map("One" -> 1, "Two" -> 2) applyLens at("One") set Some(-1)) shouldEqual Map("One" -> -1, "Two" -> 2)

    // can delete a value
    (Map("One" -> 1, "Two" -> 2) applyLens at("Two") set None) shouldEqual Map("One" -> 1)

    // add a new value
    (Map("One" -> 1, "Two" -> 2) applyLens at("Three") set Some(3)) shouldEqual Map(
      "One"   -> 1,
      "Two"   -> 2,
      "Three" -> 3
    )
  }

  test("at creates a Lens from a Set to an optional element of the Set") {
    (Set(1, 2, 3) applyLens at(2) get) shouldEqual true
    (Set(1, 2, 3) applyLens at(4) get) shouldEqual false

    (Set(1, 2, 3) applyLens at(4) set true) shouldEqual Set(1, 2, 3, 4)
    (Set(1, 2, 3) applyLens at(2) set false) shouldEqual Set(1, 3)
  }

  test("at creates a Lens from Int to one of its bit") {
    (3 applyLens at(0: IntBits) get) shouldEqual true  // true  means bit is 1
    (4 applyLens at(0: IntBits) get) shouldEqual false // false means bit is 0

    (32 applyLens at(0: IntBits) set true) shouldEqual 33
    (3 applyLens at(1: IntBits) modify (!_)) shouldEqual 1 // toggle 2nd bit

    illTyped("""0 applyLens at(79: IntBits) get""", "Right predicate.*fail.*")
    illTyped("""0 applyLens at(-1: IntBits) get""", "Left predicate.*fail.*")
  }

  test("at creates a Lens from Char to one of its bit") {
    ('x' applyLens at(0: CharBits) get) shouldEqual false
    ('x' applyLens at(0: CharBits) set true) shouldEqual 'y'
  }

  test("remove deletes an element of a Map") {
    remove("Foo")(Map("Foo" -> 1, "Bar" -> 2)) shouldEqual Map("Bar" -> 2)
  }
}
