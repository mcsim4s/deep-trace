.PHONY: gen/traces

start/env:
	docker-compose -f trace-source/docker-compose.yaml up

gen/traces:
	docker run --rm -e JAEGER_AGENT_HOST=jaeger -e JAEGER_AGENT_PORT=6831 --network=trace-source_default jaegertracing/jaeger-tracegen:1.29 --workers=10 --traces=10
