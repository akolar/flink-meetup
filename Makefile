up:
	docker-compose up --build -d

down:
	docker-compose down -v

logs:
	docker-compose logs -f producer consumer

fmt:
	scalafmt ./eventproc/src
