// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.grid.distributor;

import static org.openqa.selenium.json.Json.MAP_TYPE;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.remote.RemoteNode;
import org.openqa.selenium.grid.web.CommandHandler;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AddNode implements Predicate<HttpRequest>, CommandHandler {

  private final Distributor distributor;
  private final Json json;
  private final HttpClient.Factory httpFactory;

  public AddNode(Distributor distributor, Json json, HttpClient.Factory httpFactory) {
    this.distributor = Objects.requireNonNull(distributor);
    this.json = Objects.requireNonNull(json);
    this.httpFactory = Objects.requireNonNull(httpFactory);
  }

  @Override
  public boolean test(HttpRequest req) {
    return req.getMethod() == POST && "/se/grid/distributor/node".equals(req.getUri());
  }

  @Override
  public void execute(HttpRequest req, HttpResponse resp) throws IOException {
    Map<String, Object> raw = json.toType(req.getContentString(), MAP_TYPE);

    UUID id = UUID.fromString((String) raw.get("id"));
    URI uri = null;
    try {
      uri = new URI((String) raw.get("uri"));
    } catch (URISyntaxException e) {
      throw new JsonException(e);
    }
    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> rawCaps =
        (Collection<Map<String, Object>>) raw.get("capabilities");

    List<Capabilities> capabilities = rawCaps.stream()
        .map(ImmutableCapabilities::new)
        .collect(Collectors.toList());

    Node node = new RemoteNode(id, uri, capabilities, httpFactory.createClient(uri.toURL()));

    distributor.add(node);
  }
}
