(ns storm-ml-play.webservice
	(:use [lamina.core]
			[aleph.http]
			[aleph.redis]
			[yodit-storm.bolts])
	(:require [taoensso.carmine :as car])
	)

;a very basic webservice using redis pub sub to pull data out of the Storm topology

(def listerner 
	(car/with-new-pubsub-listener
		spec-server1 {"tweets" (fn f1 [msg] (println "Channel match: " msg))}
		(car/subscribe "tweets")))

(def ch (redis-stream {:host "localhost"}))

(def something (subscribe ch "tweets"))

(defn stream-numbers [ch]
	(future
		(dotimes [i 100]
			(enqueue ch (str i "\n")))
		(close ch)))


(defn handler [request]
	(let [stream (channel)]
		(stream-numbers stream)
		{:status 200
			:headers {"content-type" "text/plain"}
			:body "hello"}))

(def s (start-http-server (wrap-ring-handler handler) {:port 8080}))
