(ns nicocrawler-clj.db
  (:require [clojure.string :as string])
  (:use [korma.db]
        [korma.core]))

(def installpath "/usr/local/bin/nicocrawler")

(defn prepare-db []
  (defdb db (sqlite3 {:db (str installpath "/db/nicocrawler.db")}))
  (defentity mylists)
  (defentity videos))

(defn insert-mylists [notregisterd]
  "notregisterd -> {:mylistid \"mylistid\" :mylistname \"mylistname\" :artist \"artist\" :album \"album\"}"
  (insert mylists (values notregisterd)))

(defn insert-videolist [mylist-id video-ids]
  (doseq [video-id video-ids]
    (insert videos (values {:mylistid mylist-id :videoid video-id}))))

(defn insert-video [mylist-id video-id]
  (insert videos (values {:mylistid mylist-id :videoid video-id})))

(defn get-registered-videos [mylist-id]
   {:mylist-id mylist-id
    :video-ids (apply hash-set
                       (pmap #(:videoid %1)
                             (select videos
                                     (fields :videoid)
                                     (where {:mylistid mylist-id}))))})

(defn get-registered-mylists []
  (apply hash-set
         (pmap #(:mylistid %1)
               (select mylists
                       (fields :mylistid)))))

