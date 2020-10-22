/*
 * Copyright 2020 Carlos Conyers
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
package little.json.rpc

import javax.json.JsonObject

class JsonRpcParseRequestSpec extends org.scalatest.flatspec.AnyFlatSpec {
  it should "parse request without params" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": "abc",
      "method": "compute"
    }"""

    val req = JsonRpc.parseRequest(text)
    assert(req.version == "2.0")
    assert(req.id.stringValue == "abc")
    assert(req.method == "compute")
    assert(req.params.isEmpty)
  }

  it should "parse request with params" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": 123,
      "method": "compute",
      "params": { "a": 1, "b": 2 }
    }"""

    val req = JsonRpc.parseRequest(text)
    assert(req.version == "2.0")
    assert(req.id.numberValue == 123)
    assert(req.method == "compute")
    assert(
      req.params.exists { 
        case params: JsonObject =>
          params.getInt("a") == 1 && params.getInt("b") == 2
      }
    )
  }

  it should "not parse request as array" in {
    assertThrows[InvalidRequest](JsonRpc.parseRequest("[0, 1, 2]"))
  }

  it should "not parse request with parse error" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": 123
      "method": "compute",
      "params": { "a": 1, "b": 2 }
    }"""
    assertThrows[ParseError](JsonRpc.parseRequest(text))
  }

  it should "not parse request without jsonrpc" in {
    val text = """{
      "id": 123,
      "method": "compute",
      "params": { "a": 1, "b": 2 }
    }"""
    assertThrows[InvalidRequest](JsonRpc.parseRequest(text))
  }

  it should "not parse request with number value for jsonrpc" in {
    val text = """{
      "jsonrpc": 2.0,
      "id": 123,
      "method": "compute",
      "params": { "a": 1, "b": 2 }
    }"""
    assertThrows[InvalidRequest](JsonRpc.parseRequest(text))
  }

  it should "not parse request without method" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": 123,
      "params": { "a": 1, "b": 2 }
    }"""
    assertThrows[InvalidRequest](JsonRpc.parseRequest(text))
  }

  it should "not parse request with array value for method" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": 123,
      "method": [],
      "params": { "a": 1, "b": 2 }
    }"""
    assertThrows[InvalidRequest](JsonRpc.parseRequest(text))
  }

  it should "not parse request with string value for params" in {
    val text = """{
      "jsonrpc": "2.0",
      "id": 123,
      "method": "compute",
      "params": "a"
    }"""
    assertThrows[InvalidRequest](JsonRpc.parseRequest(text))
  }
}
