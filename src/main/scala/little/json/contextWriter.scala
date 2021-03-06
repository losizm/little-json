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

import javax.json.stream.JsonGenerator

/**
 * Writes value in array context to JsonGenerator.
 *
 * @see [[ObjectContextWriter]]
 */
trait ArrayContextWriter[T] {
  /** Writes value in array context. */
  def write(value: T)(implicit generator: JsonGenerator): JsonGenerator
}

/**
 * Writes value in object context to JsonGenerator.
 *
 * @see [[ArrayContextWriter]]
 */
trait ObjectContextWriter[T] {
  /** Writes value in object context. */
  def write(name: String, value: T)(implicit generator: JsonGenerator): JsonGenerator
}

/** Writes value to JsonGenerator. */
trait ContextWriter[T] extends ArrayContextWriter[T] with ObjectContextWriter[T]
