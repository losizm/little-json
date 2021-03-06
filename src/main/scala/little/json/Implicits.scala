/*
 * Copyright 2019 Carlos Conyers
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

import javax.json.{ Json => _, _ }
import javax.json.stream.{ JsonGenerator, JsonParser }

import scala.collection.Iterable
import scala.language.{ higherKinds, implicitConversions }
import scala.util.Try

import CollectionConverters._

/**
 * Provides type classes for `javax.json` and implicit implementations of
 * `JsonOutput` and `JsonInput`.
 */
object Implicits {
  /** Converts value of type T to JsonValue. */
  implicit def asJson[T](value: T)(implicit output: JsonOutput[T]): JsonValue =
    output.writing(value)

  /** Converts Array[String] to JsonValue. */
  implicit def stringArrayAsJson(values: Array[String])(implicit output: JsonOutput[Array[String]]): JsonValue =
    output.writing(values)

  /** Converts M[T] to JsonValue. */
  implicit def containerAsJson[T, M[T]](value: M[T])(implicit output: JsonOutput[M[T]]): JsonValue =
    output.writing(value)

  /** Converts Either[A, B] to JsonValue. */
  implicit def eitherAsJson[A, B](value: Either[A, B])(implicit left: JsonOutput[A], right: JsonOutput[B]): JsonValue =
    value.fold(left.writing, right.writing)

  /** Converts JsonValue to String. */
  implicit val stringJsonInput: JsonInput[String] =
    _.asInstanceOf[JsonString].getString

  /** Converts JsonValue to Boolean. */
  implicit val booleanJsonInput: JsonInput[Boolean] = {
    case JsonValue.TRUE  => true
    case JsonValue.FALSE => false
    case _               => throw new ClassCastException
  }

  /** Converts JsonValue to Short (exact). */
  implicit val shortJsonInput: JsonInput[Short] =
    _.asInstanceOf[JsonNumber].bigDecimalValue.shortValue

  /** Converts JsonValue to Int (exact). */
  implicit val intJsonInput: JsonInput[Int] =
    _.asInstanceOf[JsonNumber].intValueExact

  /** Converts JsonValue to Long (exact). */
  implicit val longJsonInput: JsonInput[Long] =
    _.asInstanceOf[JsonNumber].longValueExact

  /** Converts JsonValue to Float. */
  implicit val floatJsonInput: JsonInput[Float] =
    _.asInstanceOf[JsonNumber].bigDecimalValue.shortValueExact

  /** Converts JsonValue to Double. */
  implicit val doubleJsonInput: JsonInput[Double] =
    _.asInstanceOf[JsonNumber].doubleValue

  /** Converts JsonValue to BigInt (exact). */
  implicit val bigIntJsonInput: JsonInput[BigInt] =
    _.asInstanceOf[JsonNumber].bigIntegerValueExact

  /** Converts JsonValue to BigDecimal. */
  implicit val bigDecimalJsonInput: JsonInput[BigDecimal] =
    _.asInstanceOf[JsonNumber].bigDecimalValue

  /**
   * Creates JsonInput for converting JsonValue to Option.
   *
   * The instance of `JsonInput` returns `Some` if the value is successfully
   * converted, and `None` if the value is JSON null (i.e., `JsonValue.NULL`);
   * otherwise it throws the exception thrown by the implicit `input`.
   *
   * @note This behavior is different from [[JsonValueType.asOption]], which
   * returns `Some` if the value is successfully converted and returns `None` if
   * the value is JSON null or if an exception was thrown during conversion.
   */
  implicit def optionJsonInput[T](implicit input: JsonInput[T]) =
    new JsonInput[Option[T]] {
      def reading(json: JsonValue): Option[T] =
        json == JsonValue.NULL match {
          case true  => None
          case false => Some(input.reading(json))
        }
    }

  /**
   * Creates JsonInput for converting JsonValue to Either.
   *
   * An attempt is made to first convert the value to Right value. If that
   * attempt fails, then an attempt is made to convert it to Left value.
   */
  implicit def eitherJsonInput[A, B](implicit left: JsonInput[A], right: JsonInput[B]) =
    new JsonInput[Either[A, B]] {
      def reading(json: JsonValue): Either[A, B] =
        Try(right.reading(json))
          .map(Right(_))
          .getOrElse(Left(left.reading(json)))
    }

  /** Creates JsonInput for converting JsonValue to Try. */
  implicit def tryJsonInput[T](implicit input: JsonInput[T]) =
    new JsonInput[Try[T]] {
      def reading(json: JsonValue): Try[T] =
        Try(input.reading(json))
    }

  /** Creates JsonInput for converting JsonArray to collection. */
  implicit def collectionJsonInput[T, M[T]](implicit input: JsonInput[T], factory: Factory[T, M[T]]) =
    new JsonInput[M[T]] {
      def reading(json: JsonValue): M[T] =
        CollectionConverters
          .asScala(json.asInstanceOf[JsonArray])
          .map(input.reading)
          .to(factory)
    }

  /** Converts String to JsonValue. */
  implicit val stringJsonOutput: JsonOutput[String] = (value) => new JsonStringImpl(value)

  /** Converts Boolean to JsonValue. */
  implicit val booleanJsonOutput: JsonOutput[Boolean] = (value) => if (value) JsonValue.TRUE else JsonValue.FALSE

  /** Converts Short to JsonValue. */
  implicit val shortJsonOutput: JsonOutput[Short] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Int to JsonValue. */
  implicit val intJsonOutput: JsonOutput[Int] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Long to JsonValue. */
  implicit val longJsonOutput: JsonOutput[Long] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Float to JsonValue. */
  implicit val floatJsonOutput: JsonOutput[Float] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts Double to JsonValue. */
  implicit val doubleJsonOutput: JsonOutput[Double] = (value) => JsonNumberImpl(new java.math.BigDecimal(value))

  /** Converts BigInt to JsonValue. */
  implicit val bigIntJsonOutput: JsonOutput[BigInt] = (value) => JsonNumberImpl(new java.math.BigDecimal(value.bigInteger))

  /** Converts BigDecimal to JsonValue. */
  implicit val bigDecimalJsonOutput: JsonOutput[BigDecimal] = (value) => JsonNumberImpl(value.bigDecimal)

  /** Creates JsonOutput for converting Option to JsonValue. */
  implicit def optionJsonOutput[T, M[T] <: Option[T]](implicit output: JsonOutput[T]) =
    new JsonOutput[M[T]] {
      def writing(value: M[T]): JsonValue =
        value.fold(JsonValue.NULL)(output.writing)
    }

  /** Creates JsonOutput for converting Either to JsonValue. */
  implicit def eitherJsonOutput[A, B, M[A, B] <: Either[A, B]](implicit left: JsonOutput[A], right: JsonOutput[B]) =
    new JsonOutput[M[A, B]] {
      def writing(value: M[A, B]): JsonValue =
        value.fold(left.writing, right.writing)
    }

  /** Creates JsonOutput for converting Try to JsonValue. */
  implicit def tryJsonOutput[T, M[T] <: Try[T]](implicit output: JsonOutput[T]) =
    new JsonOutput[M[T]] {
      def writing(value: M[T]): JsonValue =
        value.fold(_ => JsonValue.NULL, output.writing)
    }

  /** Creates JsonOutput for converting Iterable to JsonArray. */
  implicit def iterableJsonOutput[T, M[T] <: Iterable[T]](implicit output: JsonOutput[T]) =
    new JsonOutput[M[T]] {
      def writing(values: M[T]): JsonValue =
        values.foldLeft(Json.createArrayBuilder())(_.add(_)).build()
    }

  /** Creates JsonOutput for converting Array to JsonArray. */
  implicit def arrayJsonOutput[T](implicit output: JsonOutput[T]) =
    new JsonOutput[Array[T]] {
      def writing(values: Array[T]): JsonValue =
        values.foldLeft(Json.createArrayBuilder())(_.add(_)).build()
    }

  /**
   * Provides extension methods to `javax.json.JsonValue`.
   *
   * @see [[JsonArrayType]], [[JsonObjectType]]
   */
  implicit class JsonValueType(private val json: JsonValue) extends AnyVal {
    /**
     * Gets value in JsonArray.
     *
     * Alias to `get(index)`.
     */
    def \(index: Int): JsonValue =
      get(index)

    /**
     * Gets value in JsonObject.
     *
     * Alias to `get(name)`.
     */
    def \(name: String): JsonValue =
      get(name)

    /**
     * Gets value of all fields with specified name.
     *
     * A lookup is performed in current JsonValue and all descendents.
     */
    def \\(name: String): Seq[JsonValue] =
      json match {
        case arr: JsonArray  => asScala(arr).flatMap(_ \\ name).toSeq
        case obj: JsonObject => Option(obj.get(name)).toSeq ++: asScala(obj.values).flatMap(_ \\ name).toSeq
        case _               => Nil
      }

    /** Gets value in JsonArray. */
    def get(index: Int): JsonValue =
      asArray.get(index)

    /** Gets value in JsonObject. */
    def get(name: String): JsonValue =
      asObject.get(name)

    /** Converts json to requested type. */
    def as[T](implicit input: JsonInput[T]): T =
      input.reading(json)

    /** Optionally converts json to requested type. */
    def asOption[T](implicit input: JsonInput[T]): Option[T] =
      asTry[T].toOption

    /** Tries to convert json to requested type. */
    def asTry[T](implicit input: JsonInput[T]): Try[T] =
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
   * Provides extension methods to `javax.json.JsonArray`.
   *
   * @see [[JsonValueType]], [[JsonObjectType]]
   */
  implicit class JsonArrayType(private val json: JsonArray) extends AnyVal {
    /**
     * Gets value from array and converts it to requested type or returns
     * evaluated default.
     */
    def getOrElse[T](index: Int, default: => T)(implicit input: JsonInput[T]): T =
      getTry[T](index).getOrElse(default)

    /** Optionally gets value from array and converts it to requested type. */
    def getOption[T](index: Int)(implicit input: JsonInput[T]): Option[T] =
      getTry[T](index).toOption

    /** Tries to get value from array and convert it to requested type. */
    def getTry[T](index: Int)(implicit input: JsonInput[T]): Try[T] =
      Try(json.get(index).as[T])

    /** Gets Short from array. */
    def getShort(index: Int): Short =
      json.getJsonNumber(index).bigDecimalValue.shortValue

    /** Gets Short from array or returns default. */
    def getShort(index: Int, default: Short): Short =
      Try(getShort(index)).getOrElse(default)

    /** Gets Long from array. */
    def getLong(index: Int): Long =
      json.getJsonNumber(index).longValue

    /** Gets Long from array or returns default. */
    def getLong(index: Int, default: Long): Long =
      Try(getLong(index)).getOrElse(default)

    /** Gets Float from array. */
    def getFloat(index: Int): Float =
      json.getJsonNumber(index).bigDecimalValue.floatValue

    /** Gets Float from array or returns default. */
    def getFloat(index: Int, default: Float): Float =
      Try(getFloat(index)).getOrElse(default)

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

    /** Creates new JSON array by appending supplied value. */
    def :+(value: JsonValue): JsonArray =
      new CombinedJsonArray(json, Json.arr(value))

    /** Creates new JSON array by prepending supplied value. */
    def +:(value: JsonValue): JsonArray =
      new CombinedJsonArray(Json.arr(value), json)

    /** Creates new JSON array by concatenating supplied array. */
    def ++(other: JsonArray): JsonArray =
      new CombinedJsonArray(json, other)
  }

  /**
   * Provides extension methods to `javax.json.JsonObject`.
   *
   * @see [[JsonValueType]], [[JsonArrayType]]
   */
  implicit class JsonObjectType(private val json: JsonObject) extends AnyVal {
    /**
     * Gets value from object and converts it to requested type or returns
     * evaluated default.
     */
    def getOrElse[T](name: String, default: => T)(implicit input: JsonInput[T]): T =
      getTry[T](name).getOrElse(default)

    /** Optionally gets value from object and converts it to requested type. */
    def getOption[T](name: String)(implicit input: JsonInput[T]): Option[T] =
      getTry[T](name).toOption

    /** Tries to get value from object and convert it to requested type. */
    def getTry[T](name: String)(implicit input: JsonInput[T]): Try[T] =
      Try(json.get(name).as[T])

    /** Gets Short from object. */
    def getShort(name: String): Short =
      json.getJsonNumber(name).bigDecimalValue.shortValue

    /** Gets Short from object or returns default. */
    def getShort(name: String, default: Short): Short =
      Try(getShort(name)).getOrElse(default)

    /** Gets Long from object. */
    def getLong(name: String): Long =
      json.getJsonNumber(name).longValue

    /** Gets Long from object or returns default. */
    def getLong(name: String, default: Long): Long =
      Try(getLong(name)).getOrElse(default)

    /** Gets Float from object. */
    def getFloat(name: String): Float =
      json.getJsonNumber(name).bigDecimalValue.floatValue

    /** Gets Float from object or returns default. */
    def getFloat(name: String, default: Float): Float =
      Try(getFloat(name)).getOrElse(default)

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
     * Creates new JSON object by adding supplied field.
     *
     * @note If field already exists, its value is replaced.
     */
    def +(field: (String, JsonValue)): JsonObject =
      new MergedJsonObject(json, Json.obj(field))

    /**
     * Creates new JSON oject by concatening supplied object.
     *
     * @note If fields already exist, their values are replaced.
     */
    def ++(other: JsonObject): JsonObject =
      new MergedJsonObject(json, other)
  }

  /**
   * Provides extension methods to `javax.json.JsonArrayBuilder`.
   *
   * @see [[JsonObjectBuilderType]]
   */
  implicit class JsonArrayBuilderType(private val builder: JsonArrayBuilder) extends AnyVal {
    /** Adds value to array builder if `Some`; otherwise, adds null if `None`. */
    def add(value: Option[JsonValue]): JsonArrayBuilder =
      value.fold(builder.addNull())(builder.add(_))

    /** Adds value to array builder if `Success`; otherwise, adds null if `Failure`. */
    def add(value: Try[JsonValue]): JsonArrayBuilder =
      value.fold(_ => builder.addNull(), builder.add(_))

    /** Adds value to array builder. */
    def add[T](value: T)(implicit companion: ArrayBuilderCompanion[T]): JsonArrayBuilder =
      companion.add(value)(builder)

    /** Adds value to array builder or adds null if value is null. */
    def addNullable[T](value: T)(implicit companion: ArrayBuilderCompanion[T]): JsonArrayBuilder =
      value == null match {
        case true  => builder.addNull()
        case false => companion.add(value)(builder)
      }
  }

  /**
   * Provides extension methods to `javax.json.JsonObjectBuilder`.
   *
   * @see [[JsonArrayBuilderType]]
   */
  implicit class JsonObjectBuilderType(private val builder: JsonObjectBuilder) extends AnyVal {
    /** Adds value to object builder if `Some`; otherwise, adds null if `None`. */
    def add(name: String, value: Option[JsonValue]): JsonObjectBuilder =
      value.fold(builder.addNull(name))(builder.add(name, _))

    /** Adds value to object builder if `Success`; otherwise, adds null if `Failure`. */
    def add(name: String, value: Try[JsonValue]): JsonObjectBuilder =
      value.fold(_ => builder.addNull(name), builder.add(name, _))

    /** Adds value to object builder. */
    def add[T](name: String, value: T)(implicit companion: ObjectBuilderCompanion[T]): JsonObjectBuilder =
      companion.add(name, value)(builder)

    /** Adds value to object builder or adds null if value is null. */
    def addNullable[T](name: String, value: T)(implicit companion: ObjectBuilderCompanion[T]): JsonObjectBuilder =
      value == null match {
        case true  => builder.addNull(name)
        case false => companion.add(name, value)(builder)
      }
  }

  /**
   * Provides extension methods to `javax.json.stream.JsonGenerator`.
   *
   * @see [[JsonParserType]]
   */
  implicit class JsonGeneratorType(private val generator: JsonGenerator) extends AnyVal {
    /** Writes value to array context if `Some`; otherwise, writes null if `None`. */
    def write(value: Option[JsonValue]): JsonGenerator =
      value.fold(generator.writeNull())(generator.write(_))

    /** Writes value to array context if `Success`; otherwise, writes null if `Failure`. */
    def write(value: Try[JsonValue]): JsonGenerator =
      value.fold(_ => generator.writeNull(), generator.write(_))

    /** Writes value in array context. */
    def write[T](value: T)(implicit writer: ArrayContextWriter[T]): JsonGenerator =
      writer.write(value)(generator)

    /** Writes value in array context or writes null if value is null. */
    def writeNullable[T](value: T)(implicit writer: ArrayContextWriter[T]): JsonGenerator =
      value == null match {
        case true  => generator.writeNull()
        case false => writer.write(value)(generator)
      }

    /** Writes value to object context if `Some`; otherwise, writes null if `None`. */
    def write(name: String, value: Option[JsonValue]): JsonGenerator =
      value.fold(generator.writeNull(name))(generator.write(name, _))

    /** Writes value to object context if `Success`; otherwise, writes null if `Failure`. */
    def write(name: String, value: Try[JsonValue]): JsonGenerator =
      value.fold(_ => generator.writeNull(name), generator.write(name, _))

    /** Writes value in object context. */
    def write[T](name: String, value: T)(implicit writer: ObjectContextWriter[T]): JsonGenerator =
      writer.write(name, value)(generator)

    /** Writes value in object context or writes null if value is null. */
    def writeNullable[T](name: String, value: T)(implicit writer: ObjectContextWriter[T]): JsonGenerator =
      value == null match {
        case true  => generator.writeNull(name)
        case false => writer.write(name, value)(generator)
      }
  }

  /**
   * Provides extension methods to `javax.json.stream.JsonParser`.
   *
   * @see [[JsonGeneratorType]]
   */
  implicit class JsonParserType(private val parser: JsonParser) extends AnyVal {
    import JsonParser.Event._

    /**
     * Parses next JSON array.
     *
     * Throws `JsonException` if next parser state is not start of array.
     */
    def nextArray(): JsonArray =
      parser.next() match {
        case START_ARRAY => getArray()
        case event       => throw new JsonException(s"Expected START_ARRAY and found $event")
      }

    /**
     * Parses next JSON object.
     *
     * Throws `JsonException` if next parser state is not start of object.
     */
    def nextObject(): JsonObject =
      parser.next() match {
        case START_OBJECT => getObject()
        case event        => throw new JsonException(s"Expected START_OBJECT and found $event")
      }

    /**
     * Gets JSON array.
     *
     * Throws `JsonException` if parser enters unexpected state.
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
        case event        => throw new JsonException(s"Unexpected parser event: $event")
      }

      builder.build()
    }

    /**
     * Gets JSON object.
     *
     * Throws `JsonException` if parser enters unexpected state.
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
            case event        => throw new JsonException(s"Unexpected parser event: $event")
          }

        case event =>
          throw new JsonException(s"Expected KEY_NAME and found $event")
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
