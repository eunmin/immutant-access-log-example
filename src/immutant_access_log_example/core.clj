(ns immutant-access-log-example.core
  (:require [immutant.web :as web]
            [immutant.web.undertow :as undertow])
  (:import [io.undertow.server.handlers.accesslog AccessLogHandler AccessLogReceiver AccessLogHandler$Builder]
           [io.undertow.server HttpHandler]
           [io.undertow Undertow UndertowOptions]
           [io.undertow.server.handlers RequestDumpingHandler$Builder]
           [io.undertow.attribute ExchangeAttributes SubstituteEmptyWrapper ExchangeAttributeWrapper]))

(defn app [request]
  (Thread/sleep (rand-int 10))
  {:status 200 :body "Hello world!"})

(deftype Main [])

(def log-format
  "https://github.com/undertow-io/undertow/blob/master/core/src/main/java/io/undertow/server/handlers/accesslog/AccessLogHandler.java#L45"
  "%h %l %u %t \"%r\" %s %b \"%{i,Referer}\" \"%{i,User-Agent}\" %D")

(def ^AccessLogReceiver log-receiver (proxy [AccessLogReceiver] []
                                       (logMessage [message]
                                         (println message))))

(defn ^AccessLogHandler custom-access-log-handler [^HttpHandler handler]
  (AccessLogHandler. handler log-receiver (or log-format "combined") (.getClassLoader Main)))

(defn -main [& ags]
  (let [^HttpHandler handler (undertow/http-handler app)
        ;; access-log-handler (custom-access-log-handler handler)
        access-log-handler (-> (AccessLogHandler$Builder.)
                               (.build {"format" log-format})
                               (.wrap handler))
        dump-handler (-> (RequestDumpingHandler$Builder.)
                         (.build {})
                         (.wrap access-log-handler))]
    (web/run dump-handler (update (undertow/options {:port 8080})
                                  :configuration
                                  (fn [builder]
                                    (.setServerOption builder UndertowOptions/RECORD_REQUEST_START_TIME true)
                                    builder)))))
