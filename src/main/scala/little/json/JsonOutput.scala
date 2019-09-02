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

import javax.json.{ JsonArrayBuilder, JsonObjectBuilder, JsonValue }
import javax.json.stream.JsonGenerator

/**
 * Writes value of type T to JsonValue.
 *
 * {{{
 * import little.json.{ Json, JsonOutput }
 * import little.json.Implicits._
 *
 * case class User(id: Int, name: String)
 *
 * // Define how to write User to JsonValue
 * implicit val userJsonOutput: JsonOutput[User] = { user =>
 *   Json.obj("id" -> user.id, "name" -> user.name)
 * }
 *
 * // Write User to JsonValue
 * val json = Json.toJson(User(0, "root"))
 * }}}
 *
 * @see [[JsonInput]]
 */
trait JsonOutput[T] extends BuilderCompanion[T] with ContextWriter[T] {
  /** Converts T value to JsonValue. */
  def writing(value: T): JsonValue

  /** Converts T value to JsonValue and adds it to array builder. */
  def add(value: T)(implicit builder: JsonArrayBuilder): JsonArrayBuilder =
    builder.add(writing(value))

  /** Converts T value to JsonValue and adds it to object builder. */
  def add(name: String, value: T)(implicit builder: JsonObjectBuilder): JsonObjectBuilder =
    builder.add(name, writing(value))

  /** Converts T value to JsonValue and writes it to array context. */
  def write(value: T)(implicit generator: JsonGenerator): JsonGenerator =
    generator.write(writing(value))

  /** Converts T value to JsonValue and writes it to object context. */
  def write(name: String, value: T)(implicit generator: JsonGenerator): JsonGenerator =
    generator.write(name, writing(value))
}
