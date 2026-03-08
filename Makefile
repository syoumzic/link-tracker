init:
	cp ./templetes/bot-application.conf ./bot/src/main/resources/application.conf
	cp ./templetes/scrapper-application.conf ./scrapper/src/main/resources/application.conf

run:
	sbt "project bot; run" & sbt "project scrapper; run"