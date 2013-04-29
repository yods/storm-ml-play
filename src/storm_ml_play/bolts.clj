(ns storm-ml-play.bolts
  "Bolts."
  (:require [backtype.storm [clojure
                             :refer [emit-bolt! defbolt ack! bolt]
                             serialization ISerialization]]
            [clj-commons-exec :as exec]
            [taoensso.carmine :as car])
  (:use [backtype.storm clojure config]))

(def username? 
  (fn [x] (= (str(first x)) "@")))

(def stopwords
  (let [all (slurp (java.io.FileReader. "/home/yodit/storm-ml-play/src/storm_ml_play/stopwords.txt"))]
    (seq (.split all "\n"))))

(def imdb
  (let [words
        (slurp "/home/yodit/storm-ml-play/vwtraining/aclImdb/imdb.vocab")]
    (seq (.split words "\n"))))

(def indexer
  (fn [x]
    (.indexOf imdb x)))
 
(defn seq-contains?
  "Determine whether a sequence contains a stopword"
  [item]
  (if (empty? stopwords)
    false
    (reduce #(or %1 %2) (map #(= %1 item) stopwords))))

(defn remover [xs f]
  (remove f (seq (.split xs " "))))

(def stopremover
  (fn [x]
      (remove seq-contains? x)))

(def existsImdb
  (fn [x]
    (> x -1)))

(defn remove-punctuation [word]
  (clojure.string/replace word #"(?i)[^\w']+" ""))

(defn somet [x]
  (str(first x) ":" (str(second x))))

(defn makeFeatures [x]
  (clojure.string/join " " (cons "|features" (map somet x))))

(defn vw [x]
  (exec/sh-pipe
   ["echo" x]
   ["vw" "-i" "/home/yodit/storm-ml-play/vwtraining/sent.model" "--quiet" "-p"  "/dev/stdout"]))

(defbolt englishTweets ["engtweet"]
  [{tweet :tweet :as tuple} collector]
  (if (= (:lang tweet) "en")
    (emit-bolt! collector [tweet] :anchor tuple))
  (ack! collector tuple))


(defbolt cleanText ["id" "cleantweet"]
  [{engtweet :engtweet :as tuple} collector]
  (let [newtweet
        (update-in (update-in engtweet [:text] clojure.string/lower-case)
                   [:text] remover username?)
        cleantweet (update-in newtweet [:text] stopremover)
        id (:id engtweet)]
    (emit-bolt! collector [id cleantweet] :anchor tuple)
    )
  (ack! collector tuple)  )

(defbolt wordFeatures ["id" "features"]
  [{cleantweet :cleantweet :as tuple} collector]
  (let [text (map remove-punctuation (:text cleantweet))
        index (filter existsImdb (map indexer text)) 
        freq (frequencies index)
        features (makeFeatures freq)
        id (:id cleantweet)]
    (emit-bolt! collector [id features] :anchor tuple)
    )
  (ack! collector tuple))

;vw -i sent.model -t -p /dev/stdout --quiet
(defbolt vwClassifier ["id" "pred"]
  [{features :features :as tuple} collector]
  (let [id (:id tuple)
        output (vw features)
        getoutput (map deref output)
        stpred (clojure.string/trim ((second getoutput) :out))
        pred (read-string stpred)]
    (emit-bolt! collector [id pred] :anchor tuple))
  (ack! collector tuple))

(defn isType? [ff tf]
  (if (or (> ff 4) (> tf 5)) "bot" "human"))

(defn ratio [x y]
  (if (= y 0)
    0 (unchecked-divide-int x y)))

(defbolt predictBot ["id" "type"]
  [{engtweet :engtweet :as tuple} collector]
  (let [id (:id engtweet)
        follower (:followers engtweet)
        friends (:friends engtweet)
        tweets (:tweetcount engtweet)
        ffratio (ratio follower friends)
        tfratio (ratio tweets friends)
        type (isType? ffratio tfratio)]
    (emit-bolt! collector [id type] :anchor tuple))
  (ack! collector tuple))

(defn hasKeys? [x] 
  (and (contains? x :pred) (contains? x :type))
  )

(defbolt aggregate ["type" "pred"] {:prepare true}
  [conf context collector]
  (let [tuples (atom {})]
    (bolt
      (execute [tuple]
        (let [id (:id tuple)
              source (.getSourceComponent tuple)]
              (if (= source "5")
                (let [pred (:pred tuple)]
                  (swap! tuples assoc :pred pred))
                (let [type (:type tuple)]
                  (swap! tuples assoc :type type)))
       (if (hasKeys? @tuples)
        (emit-bolt! collector [id @tuples] :anchor tuple))
      (ack! collector tuple))))))


(def pool (car/make-conn-pool))

(def spec-server1 (car/make-conn-spec))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn publishing [tuple]
  (wcar (car/publish "tweets" tuple)))

(defbolt publisher ["message"]
  [tuple collector]
  (let [id (:id tuple)
        type (:type tuple)
        pred (:pred tuple)
        output [id type pred]]
        emit-bolt! collector [(publishing tuple)])
  
  (ack! collector tuple))