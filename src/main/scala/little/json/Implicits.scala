/*
 * Copyright 2018 Carlos Conyers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package little.json

import javax.json._
import javax.json.stream.{ JsonGenerator, JsonParser }

import scala.collection.GenTraversableOnce
import scala.collection.convert.ImplicitConversionsToScala.`iterable AsScalaIterable`
import scala.collection.generic.CanBuildFrom
import scala.language.{ higherKinds, implicitConversions }
import scala.util.Try

/** Provides implicit values and types. */
object Implicits {
  /** Converts JsonValue to String. */
  implicit val stringFromJson: FromJson[String] = {
    case json: JsonString => json.getString
    case json => throw new IllegalArgumentException(s"required STRING but found ${json.getValueType}")
  }

  /** Converts JsonValue to Boolean. */
  implicit val booleanFromJson: FromJson[Boolean] = {
    case JsonValue.TRUE => true
    case JsonValue.FALSE => false
    case json => throw new IllegalArgumentException(s"required TRUE or FALSE but found ${json.getValueType}")
  }

  /** Converts JsonValue to Int (exact). */
  implicit val intFromJson: FromJson[Int] = {
    case json: JsonNumber => json.intValueExact
    case json => throw new IllegalArgumentException(s"required NUMBER but found ${json.getValueType}")
  }

  /** Converts JsonValue to Long (exact). */
  implicit val longFromJson: FromJson[Long] = {
    case json: JsonNumber => json.longValueExact
    case json => throw new IllegalArgumentException(s"required NUMBER but found ${json.getValueType}")
  }

  /** Converts JsonValue to Double. */
  implicit val doubleFromJson: FromJson[Double] = {
    case json: JsonNumber => json.doubleValue
    case json => throw new IllegalArgumentException(s"required NUMBER but found ${json.getValueType}")
  }

  /** Converts JsonValue to BigInt (exact). */
  implicit val bigIntFromJson: FromJson[BigInt] = {
    case json: JsonNumber => json.bigIntegerValueExact
    case json => throw new IllegalArgumentException(s"required NUMBER but found ${json.getValueType}")
  }

  /** Converts JsonValue to BigDecimal. */
  implicit val bigDecimalFromJson: FromJson[BigDecimal] = {
    case json: JsonNumber => json.bigDecimalValue
    case json => throw new IllegalArgumentException(s"required NUMBER but found ${json.getValueType}")
  }

  /**
   * Creates FromJson for converting JsonValue to Option.
   *
   * The instance of FromJson returns `Some` if the value is successfully
   * converted, and `None` if the value is JSON null (i.e., `JsonValue.NULL`);
   * otherwise it throws the exception thrown by the implicit `convert`.
   *
   * <strong>Note:</strong> This behavior is different from
   * [[JsonValueType.asOption]], which returns `Some` if the value is
   * successfully converted and returns `None` if the value is JSON null or if
   * an exception was thrown during conversion.
   */
  implicit def optionFromJson[T](implicit convert: FromJson[T]) =
    new FromJson[Option[T]] {
      def apply(json: JsonValue): Option[T] =
        if (json == JsonValue.NULL)
          None
        else Some(convert(json))
    }

  /** Creates FromJson for converting JsonValue to Try. */
  implicit def tryFromJson[T](implicit convert: FromJson[T]) =
    new FromJson[Try[T]] {
      def apply(json: JsonValue): Try[T] = Try(convert(json))
    }

  /** Creates FromJson for converting JsonArray to collection. */
  implicit def collectionFromJson[T, M[T]](implicit convert: FromJson[T], build: CanBuildFrom[Nothing, T, M[T]]) =
    new FromJson[M[T]] {
      def apply(json: JsonValue): M[T] =
        if (json.isInstanceOf[JsonArray]) json.asArray.map(_.as[T]).to[M]
        else throw new IllegalArgumentException(s"required ARRAY found ${json.getValueType}")
    }

  /** Converts String to JsonValue. */
  implicit val stringToJson: ToJson[String] = (value) => new JsonStringImpl(value)

  /** Converts Boolean to JsonValue. */
  implicit val booleanToJson: ToJson[Boolean] = (value) => if (value) JsonValue.TRUE else JsonValue.FALSE

  /** Converts Int to JsonValue. */
  implicit val intToJson: ToJson[Int] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Long to JsonValue. */
  implicit val longToJson: ToJson[Long] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Double to JsonValue. */
  implicit val doubleToJson: ToJson[Double] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts BigInt to JsonValue. */
  implicit val bigIntToJson: ToJson[BigInt] = (value) => JsonNumberImpl(new java.math.BigDecimal(value.bigInteger))

  /** Converts BigDecimal to JsonValue. */
  implicit val bigDecimalToJson: ToJson[BigDecimal] = (value) => JsonNumberImpl(value.bigDecimal)

  /** Creates ToJson for converting Option to JsonValue. */
  implicit def optionToJson[T, M[T] <: Option[T]](implicit convert: ToJson[T]) =
    new ToJson[M[T]] {
      def apply(value: M[T]): JsonValue =
        value.fold(JsonValue.NULL)(convert)
    }

  /** Creates ToJson for converting Try to JsonValue. */
  implicit def tryToJson[T, M[T] <: Try[T]](implicit convert: ToJson[T]) =
    new ToJson[M[T]] {
      def apply(value: M[T]): JsonValue =
        value.fold(_ => JsonValue.NULL, convert)
    }

  /** Creates ToJson for converting GenTraversableOnce to JsonArray. */
  implicit def genTraversableOnceToJson[T, M[T] <: GenTraversableOnce[T]](implicit convert: ToJson[T]) =
    new ToJson[M[T]] {
      def apply(values: M[T]): JsonValue =
        values.foldLeft(Json.createArrayBuilder())(_.add(_)).build()
    }

  /** Creates ToJson for converting Array to JsonArray. */
  implicit def arrayToJson[T](implicit convert: ToJson[T]) =
    new ToJson[Array[T]] {
      def apply(values: Array[T]): JsonValue =
        values.foldLeft(Json.createArrayBuilder())(_.add(_)).build()
    }

  /** Converts container M[T] to JsonValue. */
  implicit def containerAsJson[T, M[T]](value: M[T])(implicit convert: ToJson[M[T]]): JsonValue =
    convert(value)

  /** Converts Either to JsonValue. */
  implicit def eitherAsJson[L, R](value: Either[L, R])(implicit left: ToJson[L], right: ToJson[R]) =
    value.fold(left, right)

  /**
   * Provides extension methods to {@code javax.json.JsonValue}.
   *
   * @see [[JsonArrayType]], [[JsonObjectType]]
   */
  implicit class JsonValueType(val json: JsonValue) extends AnyVal {
    /**
     * Gets value in JsonArray.
     *
     * Alias to {@code get(index)}.
     */
    def \(index: Int): JsonValue = get(index)

    /**
     * Gets value in JsonObject.
     *
     * Alias to {@code get(name)}.
     */
    def \(name: String): JsonValue = get(name)

    /**
     * Gets value of all fields with specified name.
     *
     * A lookup is performed in current JsonValue and all descendents.
     */
    def \\(name: String): Seq[JsonValue] =
      json match {
        case arr: JsonArray => arr.flatMap(_ \\ name).toSeq

        case obj: JsonObject =>
          Option(obj.get(name)).toSeq ++: obj.values.flatMap(_ \\ name).toSeq

        case _ => Nil
      }

    /** Gets value in JsonArray. */
    def get(index: Int): JsonValue = asArray.get(index)

    /** Gets value in JsonObject. */
    def get(name: String): JsonValue = asObject.get(name)

    /** Converts json to requested type. */
    def as[T](implicit convert: FromJson[T]): T =
      convert(json)

    /** Optionally converts json to requested type. */
    def asOption[T](implicit convert: FromJson[T]): Option[T] =
      asTry[T].toOption

    /** Tries to convert json to requested type. */
    def asTry[T](implicit convert: FromJson[T]): Try[T] =
      Try(as[T])

    /** Casts json to JsonString. */
    def asString: JsonString =
      json.asInstanceOf[JsonString]

    /** Casts json to JsonNumber. */
    def asNumber: JsonNumber =
      json.asInstanceOf[JsonNumber]

    /** Casts json to JsonStructure. */
    def asStructure: JsonStructure =
      json.asInstanceOf[JsonStructure]

    /** Casts json to JsonArray. */
    def asArray: JsonArray =
      json.asInstanceOf[JsonArray]

    /** Casts json to JsonObject. */
    def asObject: JsonObject =
      json.asInstanceOf[JsonObject]
  }

  /**
   * Provides extension methods to {@code javax.json.JsonArray}.
   *
   * @see [[JsonValueType]], [[JsonObjectType]]
   */
  implicit class JsonArrayType(val json: JsonArray) extends AnyVal {
    /**
     * Gets value from array and converts it to requested type or returns
     * evaluated default.
     */
    def getOrElse[T](index: Int, default: => T)(implicit convert: FromJson[T]): T =
      getTry[T](index).getOrElse(default)

    /** Optionally gets value from array and converts it to requested type. */
    def getOption[T](index: Int)(implicit convert: FromJson[T]): Option[T] =
      getTry[T](index).toOption

    /** Tries to get value from array and convert it to requested type. */
    def getTry[T](index: Int)(implicit convert: FromJson[T]): Try[T] =
      Try(json.get(index).as[T])

    /** Gets Long from array. */
    def getLong(index: Int): Long =
      json.getJsonNumber(index).longValue

    /** Gets Long from array or returns default. */
    def getLong(index: Int, default: Long): Long =
      Try(getLong(index)).getOrElse(default)

    /** Gets Double from array. */
    def getDouble(index: Int): Double =
      json.getJsonNumber(index).doubleValue

    /** Gets Double from array or returns default. */
    def getDouble(index: Int, default: Double): Double =
      Try(getDouble(index)).getOrElse(default)

    /** Gets BigInt from array. */
    def getBigInt(index: Int): BigInt =
      json.getJsonNumber(index).bigIntegerValue

    /** Gets BigInt from array or returns default. */
    def getBigInt(index: Int, default: BigInt): BigInt =
      Try(getBigInt(index)).getOrElse(default)

    /** Gets BigDecimal from array. */
    def getBigDecimal(index: Int): BigDecimal =
      json.getJsonNumber(index).bigDecimalValue

    /** Gets BigDecimal from array or returns default. */
    def getBigDecimal(index: Int, default: BigDecimal): BigDecimal =
      Try(getBigDecimal(index)).getOrElse(default)

    /** Tests whether value in array is integral. */
    def isIntegral(index: Int): Boolean =
      Try(json.getJsonNumber(index).isIntegral).getOrElse(false)

    /** Creates new JsonArray with additional value. */
    def %%(value: JsonValue): JsonArray =
      new CombinedJsonArray(json, Json.arr(value))

    /** Creates new JsonArray by concatenating other array to this array. */
    def ++(other: JsonArray): JsonArray =
      new CombinedJsonArray(json, other)
  }

  /**
   * Provides extension methods to {@code javax.json.JsonObject}.
   *
   * @see [[JsonValueType]], [[JsonArrayType]]
   */
  implicit class JsonObjectType(val json: JsonObject) extends AnyVal {
    /**
     * Gets value from object and converts it to requested type or returns
     * evaluated default.
     */
    def getOrElse[T](name: String, default: => T)(implicit convert: FromJson[T]): T =
      getTry[T](name).getOrElse(default)

    /** Optionally gets value from object and converts it to requested type. */
    def getOption[T](name: String)(implicit convert: FromJson[T]): Option[T] =
      getTry[T](name).toOption

    /** Tries to get value from object and convert it to requested type. */
    def getTry[T](name: String)(implicit convert: FromJson[T]): Try[T] =
      Try(json.get(name).as[T])

    /** Gets Long from object. */
    def getLong(name: String): Long =
      json.getJsonNumber(name).longValue

    /** Gets Long from object or returns default. */
    def getLong(name: String, default: Long): Long =
      Try(getLong(name)).getOrElse(default)

    /** Gets Double from object. */
    def getDouble(name: String): Double =
      json.getJsonNumber(name).doubleValue

    /** Gets Double from object or returns default. */
    def getDouble(name: String, default: Double): Double =
      Try(getDouble(name)).getOrElse(default)

    /** Gets BigInt from object. */
    def getBigInt(name: String): BigInt =
      json.getJsonNumber(name).bigIntegerValue

    /** Gets BigInt from object or returns default. */
    def getBigInt(name: String, default: BigInt): BigInt =
      Try(getBigInt(name)).getOrElse(default)

    /** Gets BigDecimal from object. */
    def getBigDecimal(name: String): BigDecimal =
      json.getJsonNumber(name).bigDecimalValue

    /** Gets BigDecimal from object or returns default. */
    def getBigDecimal(name: String, default: BigDecimal): BigDecimal =
      Try(getBigDecimal(name)).getOrElse(default)

    /** Tests whether value in object is integral. */
    def isIntegral(name: String): Boolean =
      Try(json.getJsonNumber(name).isIntegral).getOrElse(false)

    /**
     * Creates new JsonObject with additional field.
     *
     * If the field already exists, its value is replaced.
     */
    def %%(field: (String, JsonValue)): JsonObject =
      new MergedJsonObject(json, Json.obj(field))

    /**
     * Creates new JsonObject by combining this object with other.
     *
     * If a field exists in both objects, the field from `other` is used.
     */
    def ++(other: JsonObject): JsonObject =
      new MergedJsonObject(json, other)
  }

  /**
   * Provides extension methods to {@code javax.json.JsonArrayBuilder}.
   *
   * @see [[JsonObjectBuilderType]]
   */
  implicit class JsonArrayBuilderType(val builder: JsonArrayBuilder) extends AnyVal {
    /** Adds value to array builder if `Some`; otherwise, adds null if `None`. */
    def add(value: Option[JsonValue]): JsonArrayBuilder =
      value.fold(builder.addNull()) { builder.add(_) }

    /** Adds value to array builder if `Success`; otherwise, adds null if `Failure`. */
    def add(value: Try[JsonValue]): JsonArrayBuilder =
      value.fold(_ => builder.addNull(), builder.add(_))

    /** Adds value to array builder. */
    def add[T](value: T)(implicit companion: ArrayBuilderCompanion[T]): JsonArrayBuilder =
      companion.add(value)(builder)

    /** Adds value to array builder or adds null if value is null. */
    def addNullable[T](value: T)(implicit companion: ArrayBuilderCompanion[T]): JsonArrayBuilder =
      if (value == null) builder.addNull()
      else companion.add(value)(builder)
  }

  /**
   * Provides extension methods to {@code javax.json.JsonObjectBuilder}.
   *
   * @see [[JsonArrayBuilderType]]
   */
  implicit class JsonObjectBuilderType(val builder: JsonObjectBuilder) extends AnyVal {
    /** Adds value to object builder if `Some`; otherwise, adds null if `None`. */
    def add(name: String, value: Option[JsonValue]): JsonObjectBuilder =
      value.fold(builder.addNull(name)) { builder.add(name, _) }

    /** Adds value to object builder if `Success`; otherwise, adds null if `Failure`. */
    def add(name: String, value: Try[JsonValue]): JsonObjectBuilder =
      value.fold(_ => builder.addNull(name), builder.add(name, _))

    /** Adds value to object builder. */
    def add[T](name: String, value: T)(implicit companion: ObjectBuilderCompanion[T]): JsonObjectBuilder =
      companion.add(name, value)(builder)

    /** Adds value to object builder or adds null if value is null. */
    def addNullable[T](name: String, value: T)(implicit companion: ObjectBuilderCompanion[T]): JsonObjectBuilder =
      if (value == null) builder.addNull(name)
      else companion.add(name, value)(builder)
  }

  /**
   * Provides extension methods to {@code javax.json.stream.JsonGenerator}.
   *
   * @see [[JsonParserType]]
   */
  implicit class JsonGeneratorType(val generator: JsonGenerator) extends AnyVal {
    /** Writes value to array context if `Some`; otherwise, writes null if `None`. */
    def write(value: Option[JsonValue]): JsonGenerator =
      value.fold(generator.writeNull()) { generator.write(_) }

    /** Writes value to array context if `Success`; otherwise, writes null if `Failure`. */
    def write(value: Try[JsonValue]): JsonGenerator =
      value.fold(_ => generator.writeNull(), generator.write(_))

    /** Writes value in array context. */
    def write[T](value: T)(implicit writer: ArrayContextWriter[T]): JsonGenerator =
      writer.write(value)(generator)

    /** Writes value in array context or writes null if value is null. */
    def writeNullable[T](value: T)(implicit writer: ArrayContextWriter[T]): JsonGenerator =
      if (value == null) generator.writeNull()
      else writer.write(value)(generator)

    /** Writes value to object context if `Some`; otherwise, writes null if `None`. */
    def write(name: String, value: Option[JsonValue]): JsonGenerator =
      value.fold(generator.writeNull(name)) { generator.write(name, _) }

    /** Writes value to object context if `Success`; otherwise, writes null if `Failure`. */
    def write(name: String, value: Try[JsonValue]): JsonGenerator =
      value.fold(_ => generator.writeNull(name), generator.write(name, _))

    /** Writes value in object context. */
    def write[T](name: String, value: T)(implicit writer: ObjectContextWriter[T]): JsonGenerator =
      writer.write(name, value)(generator)

    /** Writes value in object context or writes null if value is null. */
    def writeNullable[T](name: String, value: T)(implicit writer: ObjectContextWriter[T]): JsonGenerator =
      if (value == null) generator.writeNull(name)
      else writer.write(name, value)(generator)
  }

  /**
   * Provides extension methods to {@code javax.json.stream.JsonParser}.
   *
   * @see [[JsonGeneratorType]]
   */
  implicit class JsonParserType(val parser: JsonParser) extends AnyVal {
    import JsonParser.Event._

    /**
     * Parses next JSON array.
     *
     * Throws {@code JsonException} if next parser state is not start of array.
     */
    def nextArray(): JsonArray =
      parser.next() match {
        case START_ARRAY => getArray()
        case event => throw new JsonException(s"expected START_ARRAY but found $event")
      }

    /**
     * Parses next JSON object.
     *
     * Throws {@code JsonException} if next parser state is not start of object.
     */
    def nextObject(): JsonObject =
      parser.next() match {
        case START_OBJECT => getObject()
        case event => throw new JsonException(s"expected START_OBJECT but found $event")
      }

    /**
     * Gets JSON array.
     *
     * Throws {@code JsonException} if parser enters unexpected state.
     */
    def getArray(): JsonArray = {
      val builder = Json.createArrayBuilder()

      nextUntil(END_ARRAY) {
        case VALUE_STRING => builder.add(parser.getString())
        case VALUE_NUMBER => builder.add(parser.getBigDecimal())
        case VALUE_TRUE   => builder.add(true)
        case VALUE_FALSE  => builder.add(false)
        case VALUE_NULL   => builder.addNull()
        case START_ARRAY  => builder.add(getArray())
        case START_OBJECT => builder.add(getObject())
        case event        => throw new JsonException(s"unexpected parser event: $event")
      }

      builder.build()
    }

    /**
     * Gets JSON object.
     *
     * Throws {@code JsonException} if parser enters unexpected state.
     */
    def getObject(): JsonObject = {
      val builder = Json.createObjectBuilder()

      nextUntil(END_OBJECT) {
        case KEY_NAME =>
          val key = parser.getString()

          parser.next() match {
            case VALUE_STRING => builder.add(key, parser.getString())
            case VALUE_NUMBER => builder.add(key, parser.getBigDecimal())
            case VALUE_TRUE   => builder.add(key, true)
            case VALUE_FALSE  => builder.add(key, false)
            case VALUE_NULL   => builder.addNull(key)
            case START_ARRAY  => builder.add(key, getArray())
            case START_OBJECT => builder.add(key, getObject())
            case event        => throw new JsonException(s"unexpected parser event: $event")
          }

        case event => throw new JsonException(s"expected KEY_NAME but found $event")
      }

      builder.build()
    }

    private def nextUntil(terminal: JsonParser.Event)(f: JsonParser.Event => Unit): Unit = {
      var event = parser.next()

      while (event != terminal) {
        f(event)
        event = parser.next()
      }
    }
  }
}
