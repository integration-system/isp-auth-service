# Version: 0.0.1
FROM anapsix/alpine-java:8

ENV NGINX_CLOJURE_VERSION 0.4.5
ENV NGINX_VERSION 1.13.12
ENV JAR_NAME auth-service-0.1.0-SNAPSHOT

RUN apk add --no-cache tzdata
RUN adduser -h /etc/nginx -D -s /bin/sh nginx
RUN cp /usr/share/zoneinfo/UTC /etc/localtime
RUN echo "UTC" >  /etc/timezone

RUN wget -O - https://sourceforge.net/projects/nginx-clojure/files/nginx-clojure-$NGINX_CLOJURE_VERSION.tar.gz | tar xzf - \
    && mv ./nginx-clojure-$NGINX_CLOJURE_VERSION/* /etc/nginx/ \
    && rm -rf ./nginx-clojure-$NGINX_CLOJURE_VERSION \
    && ln -s /opt/jdk/jre/lib/amd64/server/libjvm.so /opt/jdk/jre/lib/amd64/libjvm.so \
    && chown -R nginx:nginx /etc/nginx

ENV PATH $PATH:/etc/nginx/

#ADD build/libs/$JAR_NAME.jar /etc/nginx/libs/
#ADD nginx.conf /etc/nginx/conf/
#ADD nginx-clojure-$NGINX_CLOJURE_VERSION.jar /etc/nginx/jars/

RUN touch /etc/nginx/logs/app.log \
    #&& ln -sf /dev/stdout /etc/nginx/logs/app.log \
	&& ln -sf /dev/stderr /etc/nginx/logs/error.log \
	&& ln -sf /dev/stdout /etc/nginx/logs/access.log \
    && chown -h nginx:nginx /etc/nginx/logs/*.log

WORKDIR /etc/nginx/

ENTRYPOINT ["nginx-linux-x64", "-g", "daemon off;"]

EXPOSE 5000 8401
