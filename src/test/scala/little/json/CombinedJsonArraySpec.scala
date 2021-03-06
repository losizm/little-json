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

import Implicits._
import Test._

class CombinedJsonArraySpec extends org.scalatest.flatspec.AnyFlatSpec {
  it should "append to JSON array" in {
    val arr = Json.arr("a", 2, 3.0, false) ++ Json.arr("x", User(0, "root")) :+ "guest"

    assert(!arr.isEmpty)
    assert(arr.size == 7)
    assert(arr.getString(0) == "a")
    assert(arr.getInt(1) == 2)
    assert(arr.getJsonNumber(2).doubleValue == 3.0)
    assert(!arr.getBoolean(3))
    assert(arr.getString(4) == "x")
    assert(arr.get(5).as[User] == User(0, "root"))
    assert(arr.getString(6) == "guest")
  }

  it should "prepend to JSON array" in {
    val arr = ("guest" +: Json.arr("a", 2, 3.0, false)) ++ Json.arr("x", User(0, "root"))

    assert(!arr.isEmpty)
    assert(arr.size == 7)
    assert(arr.getString(0) == "guest")
    assert(arr.getString(1) == "a")
    assert(arr.getInt(2) == 2)
    assert(arr.getJsonNumber(3).doubleValue == 3.0)
    assert(!arr.getBoolean(4))
    assert(arr.getString(5) == "x")
    assert(arr.get(6).as[User] == User(0, "root"))
  }
}
