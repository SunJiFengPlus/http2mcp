import groovy.json.JsonSlurper

def raw = exchange.getMessage().getBody(String)
def slurper = new JsonSlurper()
def parsed
try {
  parsed = slurper.parseText(raw)
} catch (Throwable t) {
  // Not JSON; stream raw as a single message
  return [ [ event: 'message', data: [ text: raw ] ] ]
}

// Heuristic mapping to a neutral event schema {event, data, id?}
def events = []

if (parsed instanceof Map) {
  // Common shapes
  if (parsed.containsKey('choices')) {
    parsed.choices.eachWithIndex { ch, idx ->
      def content = ch?.message?.content ?: ch?.delta?.content ?: ch?.text ?: ch?.content
      if (content instanceof List) {
        content.each { part ->
          def text = (part?.text ?: part?.value ?: part)?.toString()
          if (text) events << [ event: 'message', id: "choice-${idx}", data: [ text: text ] ]
        }
      } else if (content != null) {
        events << [ event: 'message', id: "choice-${idx}", data: [ text: content.toString() ] ]
      }
    }
  } else if (parsed.containsKey('data')) {
    events << [ event: 'message', data: parsed.data ]
  } else if (parsed.containsKey('message')) {
    events << [ event: 'message', data: [ text: parsed.message?.toString() ] ]
  } else {
    // generic map -> single event
    events << [ event: 'message', data: parsed ]
  }
} else if (parsed instanceof List) {
  parsed.eachWithIndex { item, i ->
    if (item instanceof Map && item.containsKey('event') && item.containsKey('data')) {
      events << item
    } else if (item instanceof Map) {
      events << [ event: 'message', id: "${i}", data: item ]
    } else {
      events << [ event: 'message', id: "${i}", data: [ text: String.valueOf(item) ] ]
    }
  }
}

if (events.isEmpty()) {
  events << [ event: 'message', data: parsed ]
}

return events

