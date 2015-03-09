import org.scalatest.FunSuite

import scala.collection.mutable

class SetTest extends FunSuite {
  case class Element(id: String, num: BigDecimal) extends Ordered[Element] {
    override def compare(that: Element): Int = num compare that.num

    override def equals(obj: scala.Any): Boolean = obj match {
      case e: Element => e.id equals id
      case _ => false
    }

    override def hashCode(): Int = id.hashCode
  }

  test("Equality test") {
    assert(Element("a", 1) === Element("a", 2))
  }
  
  test("TreeSet test") {
    val set = new mutable.TreeSet[Element]()
    set.add(Element("b", 1))
    set.add(Element("b", 2))
    set.add(Element("a", 2))
    set.add(Element("a", 3))
    assert(set.size === 2)
    set.remove(Element("b", 4))
    assert(set.size === 1)
  }

}
