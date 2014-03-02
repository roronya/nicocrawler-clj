(ns nicocrawler-clj.api
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.xml :as xml])
  (:use [ring.util.codec :only [url-decode]]))

(defn get-thumb [video-id]
  (:body (client/get (str "http://ext.nicovideo.jp/thumb/" video-id))))

(def cs (clj-http.cookies/cookie-store))

(defn login [mail pass]
  (client/post "https://secure.nicovideo.jp/secure/login?site=niconico"
               {:form-params {:mail_tel mail :password pass}
                :cookie-store cs}))

(defn get-flv [video-id]
  (let [result (ref {})]
    (loop
        [urls (re-seq  #"[^=&][^=&]*" (:body (client/post (if (= "nm" (re-find #"[a-z]*" video-id)) (str "http://flapi.nicovideo.jp/api/getflv/" video-id "?as3=1") (str "http://flapi.nicovideo.jp/api/getflv/" video-id)) {:cookie-store cs})))]
      (if (not (= () urls))
        (do
          (dosync
           (ref-set result (assoc @result (keyword (first urls)) (url-decode (second urls)))))
          (recur (rest (rest urls))))
        @result))))

(defn get-relation [page sort order video]
  (:body
   (client/get 
    (str "http://flapi.nicovideo.jp/api/getrelation?page=" page
         "&sort=" sort
         "&order=" order
         "&video=" video))))

(defn get-msg [video-id quantity]
  (let [flvinfo (get-flv video-id)]
    (:body
     (client/get (str (:ms flvinfo) "thread?version=20090904&thread=" (:thread_id flvinfo) "&res_from=" quantity)))))

(defn get-thumbinfo [video-id]
  (let [return (ref {})]
    (reduce merge
            (apply vector
                   (for [x (xml-seq (xml/parse (str "http://ext.nicovideo.jp/api/getthumbinfo/" video-id)))]
                     {(:tag x) (first (:content x))})))))

(defn get-mylist [mylist-id]
  {:mylist-id mylist-id
   :video-ids (apply hash-set
                     (rest 
                      (map #(last (string/split %1 #"/"))
                           (for [x (xml-seq (xml/parse (str "http://www.nicovideo.jp/mylist/" mylist-id "?rss=2.0&lang=ja-jp")))
                                 :when (= (:tag x) :link)]
                             (first (:content x))))))})

(defn get-mylistname [mylist-id]
  (first
   (for [x (xml-seq (xml/parse (str "http://www.nicovideo.jp/mylist/" mylist-id "?rss=2.0&lang=ja-jp")))
         :when (= (:tag x) :title)]
     (first (:content x)))))
