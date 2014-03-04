(defproject nicocrawler "0.2.3"
  :description "ニコニコ動画のマイリストを監視して更新があると勝手にmp3かm4a形式でダウンロードしてきます"
  :url "http://"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main nicocrawler-clj.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.9"]
                 [ring/ring-codec "1.0.0"]
                 [korma "0.3.0-RC5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]])
