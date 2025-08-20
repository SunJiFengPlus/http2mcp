# Camel MCP Gateway

Bridge legacy HTTP APIs into MCP-friendly streaming outputs (SSE or NDJSON) using Apache Camel. Transformations are customizable with YAML routes and Groovy scripts.

## Quick start

Requirements: JDK 17+, Maven 3.9+

```bash
mvn spring-boot:run
```

- SSE endpoint:
  - `GET http://localhost:8080/mcp/sse?url=http://localhost:8080/legacy`
  - Response: `text/event-stream`
- NDJSON endpoint:
  - `GET http://localhost:8080/mcp/ndjson?url=http://localhost:8080/legacy`
  - Response: `application/x-ndjson`

Optional query parameter `script` can override the transform script resource, e.g. `classpath:scripts/transform.groovy` or `file:/abs/path/custom.groovy`.

## How it works

- YAML route `src/main/resources/routes/transform.yaml` fetches legacy HTTP via `http:` and evaluates a Groovy script (`language:groovy`) to convert the body into event objects.
- The Groovy script returns a list of neutral events `{ event: string, id?: string, data: object }`.
- Controllers expose:
  - `/mcp/sse` streaming as Server-Sent Events
  - `/mcp/ndjson` streaming as newline-delimited JSON

## Customize transformation

Edit `src/main/resources/scripts/transform.groovy` or provide your own via `?script=...`.

The script should return either a single event or a list of events:

```groovy
// single event
[ event: 'message', data: [ text: '...' ] ]

// multiple events
[
  [ event: 'message', id: '1', data: [ text: 'hello' ] ],
  [ event: 'message', data: [ text: 'world' ] ]
]
```

You can also extend the YAML route using Camel steps like `jsonpath`, `setHeader`, `transform`, etc., to pre/post-process the body around the Groovy step.

## Configuration

See `src/main/resources/application.yml`:

- `camel.springboot.routes-include-pattern`: loads YAML routes
- `legacy.baseUrl`: default base URL if `url` is not provided
- `mcp.defaultTransformScript`: default Groovy script resource

## Demo legacy endpoint

A minimal demo endpoint is provided at `GET /legacy` that returns an example JSON payload with `choices`.

## Notes

- SSE delivery uses Spring WebFlux; Camel orchestrates fetching and transformation.
- Headers from the client are forwarded into Camel and can be used in scripts.