import cc.spray.json._

class JSON {

  def stringToAST(source: String): String = {
    JsonParser(source).prettyPrint
  }

}

object JSON extends App {
  // Simple json object creation
  val source = """ {"menu": { "id": "file", "value": "File", "popup": { "menuitem": [ {"value": "New", "onclick": "CreateNewDoc()"}]}}}"""
  val json = source.asJson
  println(json.prettyPrint)

  // Convert Scala Object to json

  import DefaultJsonProtocol._

  val jsonAst = Map("one" -> 1, "two" -> 2, "three" -> 3).toJson
  println(jsonAst)


  // Convert from/to custom class
  case class Person(name: String, age: Option[Int])

  object MyJsonProtocol extends DefaultJsonProtocol with NullOptions {
    implicit val personFormat = jsonFormat2(Person)
  }

  import MyJsonProtocol._

  val jsonPerson = Person("Who", Some(55)).toJson
  println(jsonPerson.prettyPrint)
  val person = jsonPerson.convertTo[Person]
  println(person)
  println(Person("Anonymous", None).toJson.prettyPrint)


  // Convert standard case class
  class Color(val name: String, val red: Int, val green: Int, val blue: Int)

  object ColorJsonProtocol extends DefaultJsonProtocol {

    implicit object ColorJsonFormat extends RootJsonFormat[Color] {
      def write(c: Color) =
        JsArray(JsString(c.name), JsNumber(c.red), JsNumber(c.green), JsNumber(c.blue))

      def read(value: JsValue) = value match {
        case JsArray(JsString(name) :: JsNumber(red) :: JsNumber(green) :: JsNumber(blue) :: Nil) =>
          new Color(name, red.toInt, green.toInt, blue.toInt)
        case _ => deserializationError("Color expected")
      }
    }

  }

  import ColorJsonProtocol._

  val jsonColor = new Color("CadetBlue", 95, 158, 160).toJson
  println(jsonColor.prettyPrint)
  val color = jsonColor.convertTo[Color]

}
