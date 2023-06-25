.PHONY: gen/traces

start/env:
	export TASKS_DAO_USER=postgres
	export TASKS_DAO_PASSWORD=development
	export TASKS_DAO_URL='jdbc:postgresql://localhost:5432/'
	docker-compose -f development-env/docker-compose.yaml up

gen/traces:
	docker run --rm -e JAEGER_AGENT_HOST=jaeger -e JAEGER_AGENT_PORT=6831 --network=trace-source_default jaegertracing/jaeger-tracegen:1.29 --workers=10 --traces=10
