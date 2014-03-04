(ns nicocrawler-clj.core
  (:gen-class)
  (:use [nicocrawler-clj.api]
        [nicocrawler-clj.ffmpeg]
        [nicocrawler-clj.db]
        [clojure.java.io :only (output-stream)]
        [clojure.java.shell :only (sh)]
        [clojure.set :only (difference)])
  (:require [clojure.xml :as xml]
            [clj-http.client :as client]
            [clojure.string :as string]))

(defn replace-slash [str]
  (string/replace str #"/" "／"))

(defn delete-space [str]
  (string/replace str #"\s" ""))

(def conffile (read-string (delete-space (slurp (str installpath "/conf/nicocrawler.conf")))))

(defn rm [filename]
  (sh "sh" "-c" (str "rm -rf \"" filename "\"")))

(defn mv [filename path]
  (sh "sh" "-c" (str "mv \"" filename "\" " path)))

(defn save-video [video-id path]
  (client/get (str "http://www.nicovideo.jp/watch/" video-id) {:cookie-store cs})
  (let [thumbinfo (get-thumbinfo video-id)
        filename (replace-slash (str (:title thumbinfo) "." (:movie_type thumbinfo))) 
        get-flv-url (:url (get-flv video-id))]
    (with-open 
        [os (output-stream filename)]
    (.write os (:body (client/get get-flv-url {:as :byte-array :cookie-store cs}))))
    (mv filename path)))

(defn save-music [video-id path]
  (client/get (str "http://www.nicovideo.jp/watch/" video-id) {:cookie-store cs})
  (let [thumbinfo (get-thumbinfo video-id)
        movie-title (replace-slash (:title thumbinfo))
        movie-path (str installpath "/tmp/" movie-title)
        movie-type (:movie_type thumbinfo)
        filename (str movie-title "." movie-type)
        get-flv-url (:url (get-flv video-id))]

    (with-open
        [os (output-stream (str installpath "/tmp/" filename))]
      (.write os (:body (client/get get-flv-url {:as :byte-array :cookie-store cs}))))

    (cond
     (= movie-type "mp4")
     (do (mp4->m4a movie-path) (mv (str movie-path ".m4a") path))
     (= movie-type "flv")
     (do (flv->mp3 movie-path) (mv (str movie-path ".mp3") path))
     (= movie-type "swf")
     (do (swf->mp3 movie-path) (mv (str movie-path ".mp3") path)))

    (println (str movie-title " is downloaded."))

    (rm (str installpath "/tmp/" filename)))
  video-id)



(defn p-save-music [video-id path]
  (try (save-music video-id path)
       (catch Exception e (do (Thread/sleep 10000) (p-save-music video-id path)))))

(defn p-save-video [video-id path]
  (try (save-music video-id path)
       (catch Exception e (do (Thread/sleep 10000) (p-save-video video-id path)))))

(defn save-all-music [list path]
  (pmap #(p-save-music %1 path) list))

(defn get-notregisterd-mylist []
  (difference (apply hash-set (pmap #(:mylist-id %1) (:mylists conffile))) (get-registered-mylists)))

(defn get-mylist-info [mylist-id]
  (first
   (for [x (:mylists conffile)
         :when (= (:mylist-id x) mylist-id)]
     (hash-map :mylistid mylist-id :mylistname (get-mylistname mylist-id) :artist (:artist x) :album (if (= nil (:album x)) nil (:album x))))))

(defn register-notregisterd-mylist []
  (doseq [mylist-info  (map #(get-mylist-info %1)(get-notregisterd-mylist))]
    (insert-mylists mylist-info)))

(defn check-update []
  (println "check update…")
  (let [writed-mylists (pmap #(:mylist-id %1) (:mylists conffile))
        registerd (pmap get-registered-videos writed-mylists) 
        now (pmap #(get-mylist %1) writed-mylists)]
    (pmap
     #(hash-map :mylist-id (:mylist-id %1) :video-ids (difference (:video-ids %1) (:video-ids %2)))
       now registerd)))

(defn create-path [mylist-id]
  (let [mylist-info (get-mylist-info mylist-id)
        path (str (:path conffile) "/" (:artist mylist-info) "/" (:album mylist-info))]
    (sh "mkdir" "-p" path)
    path))

(defn savefile&updatedb [notregister]
  (insert-video (:mylist-id notregister) (p-save-music (:video-id notregister) (:path notregister))))

(defn -main []
  (print "login … ")
  (login (:mail conffile) (:pass conffile))
  (println "OK")
  (print "prepare database … ")
  (prepare-db)
  (println "OK")
  (register-notregisterd-mylist)
  (doall
   (pmap savefile&updatedb
         (reduce into (pmap (fn [updates] (let [mylist-id (:mylist-id updates)
                                                video-ids (:video-ids updates)
                                                path (create-path mylist-id)]
                                            (pmap #(hash-map :mylist-id mylist-id :video-id %1 :path path) video-ids)))
                            (check-update)))))
  (shutdown-agents))
