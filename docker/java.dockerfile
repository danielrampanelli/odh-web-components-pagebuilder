FROM maven:3-jdk-8 as base

RUN mkdir -p /code
WORKDIR /code

# Use chromium to create screenshots
# RUN apt-get update && apt-get install --yes chromium chromium-driver

# Or, use firefox to create screenshots
RUN apt-get update && \
	apt-get install -y --no-install-recommends firefox-esr && \
	rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.26.0/geckodriver-v0.26.0-linux64.tar.gz && \
	mv geckodriver-v0.26.0-linux64.tar.gz /usr/bin/ && \
	cd /usr/bin/ && \
	tar xvf geckodriver-v0.26.0-linux64.tar.gz && \
	rm geckodriver-v0.26.0-linux64.tar.gz

# DEV stage ###################################################################
FROM base AS dev

# Not for BASE, because we do not need maven package caches for production
COPY docker/entrypoint-java.sh /entrypoint-java.sh
ENTRYPOINT [ "/entrypoint-java.sh" ]

# BUILD stage #################################################################
FROM base as build

COPY src /code/src
COPY pom.xml /code/pom.xml
COPY src/main/resources/application.users-file.example /var/data/pagebuilder/application.users-file

CMD mkdir /root/.m2 && \
	mvn dependency:copy-dependencies -DoutputDirectory=/root/.m2
