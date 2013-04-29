(defproject yodit-storm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [twitter-api "0.7.2"]
                 [org.twitter4j/twitter4j-core "2.2.5"]
                 [org.twitter4j/twitter4j-stream "2.2.5"]
                 [com.taoensso/carmine "1.6.0"]
                 [org.clojars.hozumi/clj-commons-exec "1.0.6"]
                 [aleph "0.3.0-SNAPSHOT"]]
  :aot [yodit-storm.TopologySubmitter]
  ;; include storm dependency only in dev because production storm cluster provides it
  :profiles {:dev {:dependencies [[storm "0.8.1"]]}})
