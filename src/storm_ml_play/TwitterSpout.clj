(ns storm-ml-play.TwitterSpout
    (:import [twitter4j TwitterStream TwitterStreamFactory Status StatusDeletionNotice StatusListener FilterQuery]
             [twitter4j.conf Configuration ConfigurationBuilder]
             [twitter4j.json DataObjectFactory]
             [backtype.storm StormSubmitter LocalCluster]
        )
    (:use [backtype.storm clojure config tuple])
    (:gen-class))


(def q (new java.util.concurrent.LinkedBlockingQueue))

(defn config-with-password ^Configuration []
  "Build a twitter4j configuration object with a username/password pair"
  (let [cb (ConfigurationBuilder.)]
    (.setUser cb "YOUR_TWITTER_LOGIN")
    (.setPassword cb "YOUR_TWITTER_PASSWORD")
     ; Uncomment if you want access to the raw json
     ;(.setJSONStoreEnabled cb true)
    (.build cb)))


(defn status-listener []
  "Implementation of twitter4j's StatusListener interface"
  (proxy [StatusListener] []
    (onStatus [^twitter4j.Status status]
      (.offer q
              [(.getId (.getUser status))
               (.getFollowersCount (.getUser status))
               (.getFriendsCount (.getUser status))
               (.getStatusesCount (.getUser status))
               (.getLang (.getUser status))
               (.getText status)
               (.isRetweet status)
               (.isFavorited status)])
      
      ;(.getUser status)
      ;(println (DataObjectFactory/getRawJSON status)
      )
    (onException [^java.lang.Exception e] (.printStackTrace e))
    (onDeletionNotice [^twitter4j.StatusDeletionNotice statusDeletionNotice] ())
    (onScrubGeo [userId upToStatusId] ())
    (onTrackLimitationNotice [numberOfLimitedStatuses] ())))

(defn get-twitter-stream-factory[]
  (let [factory (TwitterStreamFactory. (config-with-password))]
    (.getInstance factory)))

(defspout twitterSpout2 ["tweet"]
  [conf context collector]
  (let [listener status-listener
        filter-query (FilterQuery. 0 (long-array []) (into-array String ["test"]))
        stream (.getFilterStream (get-twitter-stream-factory) filter-query)
        ]
    (.next stream (listener))
    (spout
     (nextTuple[]
               (Thread/sleep 50)
               (let [tweet (.poll q)
                     ]
                 (if (= nil tweet)
                   (Thread/sleep 50)
                   (emit-spout! collector [tweet]))))
     (ack [id]))
    ))



(defspout twitterSpout["tweet"]
  [conf context collector]
  (let [listener status-listener
        stream (get-twitter-stream-factory)]
    (.addListener stream (listener))
    (.sample stream)
    (spout
     (nextTuple[]
               (Thread/sleep 50)
               (let [t (.poll q)
                     tweet (zipmap
                      [:id :followers :friends :tweetcount :lang :text :rt? :fav?]
                      t)]
                 (if (= nil tweet)
                   (Thread/sleep 50)
                   (emit-spout! collector [tweet]))))
     (ack [id])
     ))


)