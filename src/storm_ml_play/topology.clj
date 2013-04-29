(ns storm-ml-play.topology
  "Topology"
  (:require [storm-ml-play
             [TwitterSpout :refer [twitterSpout2
                                   twitterSpout]]
             [bolts :refer [englishTweets
                            cleanText
                            wordFeatures
                            vwClassifier
                            predictBot
                            aggregate
                            publisher]]]
            [backtype.storm [clojure :refer [topology spout-spec bolt-spec]] [config :refer :all]])
  (:import [backtype.storm LocalCluster LocalDRPC]))

(defn storm-topology []
  (topology
   {"1" (spout-spec twitterSpout)}
   {"2" (bolt-spec {"1" ["tweet"]}
                   englishTweets
                   :p 10)
   "3" (bolt-spec {"2" ["engtweet"]}
                  cleanText
                  :p 11)
   
   "4" (bolt-spec {"3" ["cleantweet"]}
                  wordFeatures
                  :p 12)
   "5" (bolt-spec {"4" ["features"]}
                  vwClassifier
                  :p 13)
   "6" (bolt-spec {"2" ["engtweet"]}
                  predictBot
                  :p 14)
   "7" (bolt-spec {"6" ["id"] "5" ["id"]}
                  aggregate
                  :p 15)
   "8" (bolt-spec {"7" ["type" "pred"]}
                  publisher
                  :p 16)
   }))

(defn run! [& {debug "debug" workers "workers" :or {debug "true" workers "3"}}]
  (doto (LocalCluster.)
    (.submitTopology "storm-topology"
                     {TOPOLOGY-DEBUG (Boolean/parseBoolean debug)
                      TOPOLOGY-WORKERS (Integer/parseInt workers)}
                     (storm-topology))
    (Thread/sleep 5000) 
    (.shutdown "storm-topology")))